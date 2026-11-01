"""把 Anvil 区域文件渲染成俯视 PNG，用于检查地形观感。

用法：
    python render_map.py <world_dir> <output.png> [--from-chunk X Z] [--to-chunk X Z]

只读取 region/ 下的 .mca 文件，不依赖服务端。
"""
import glob
import gzip
import os
import struct
import sys
import zlib

import numpy as np
from PIL import Image

AIR_BLOCKS = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}


def palette_name(entry):
    """1.20 用 {"Name": ...}，26.x 用 {"": ...} / {"id": ..., "properties": ...}。"""
    if isinstance(entry, str):
        return entry
    if isinstance(entry, dict):
        for key in ("Name", "id", "", "name", "block"):
            value = entry.get(key)
            if isinstance(value, str):
                return value
    return "minecraft:air"


class NBTReader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def u1(self):
        value = self.data[self.pos]
        self.pos += 1
        return value

    def i2(self):
        value = struct.unpack(">h", self.data[self.pos:self.pos + 2])[0]
        self.pos += 2
        return value

    def i4(self):
        value = struct.unpack(">i", self.data[self.pos:self.pos + 4])[0]
        self.pos += 4
        return value

    def i8(self):
        value = struct.unpack(">q", self.data[self.pos:self.pos + 8])[0]
        self.pos += 8
        return value

    def string(self):
        length = struct.unpack(">H", self.data[self.pos:self.pos + 2])[0]
        self.pos += 2
        text = self.data[self.pos:self.pos + length].decode("utf-8", "replace")
        self.pos += length
        return text, length

    def payload(self, tag_type):
        if tag_type == 1:
            value = self.u1()
            return value - 256 if value > 127 else value
        if tag_type == 2:
            return self.i2()
        if tag_type == 3:
            return self.i4()
        if tag_type == 4:
            return self.i8()
        if tag_type == 5:
            value = struct.unpack(">f", self.data[self.pos:self.pos + 4])[0]
            self.pos += 4
            return value
        if tag_type == 6:
            value = struct.unpack(">d", self.data[self.pos:self.pos + 8])[0]
            self.pos += 8
            return value
        if tag_type == 7:
            length = self.i4()
            value = self.data[self.pos:self.pos + length]
            self.pos += length
            return value
        if tag_type == 8:
            text, _length = self.string()
            return text
        if tag_type == 9:
            element_type = self.u1()
            length = self.i4()
            return [self.payload(element_type) for _ in range(length)]
        if tag_type == 10:
            result = {}
            while True:
                child_type = self.u1()
                if child_type == 0:
                    return result
                name, _ = self.string()
                result[name] = self.payload(child_type)
        if tag_type == 11:
            length = self.i4()
            value = list(struct.unpack(">%di" % length, self.data[self.pos:self.pos + 4 * length]))
            self.pos += 4 * length
            return value
        if tag_type == 12:
            length = self.i4()
            value = list(struct.unpack(">%dq" % length, self.data[self.pos:self.pos + 8 * length]))
            self.pos += 8 * length
            return value
        raise ValueError("unknown NBT tag %d" % tag_type)

    def root(self):
        tag_type = self.u1()
        if tag_type != 10:
            raise ValueError("root is not a compound")
        self.string()
        return self.payload(10)


def parse_region(path):
    with open(path, "rb") as handle:
        data = handle.read()
    if len(data) < 8192:
        return
    for index in range(1024):
        entry = struct.unpack(">I", data[index * 4:index * 4 + 4])[0]
        offset = entry >> 8
        if offset == 0:
            continue
        position = offset * 4096
        length = struct.unpack(">I", data[position:position + 4])[0]
        compression = data[position + 4]
        payload = data[position + 5:position + 4 + length]
        try:
            if compression == 1:
                payload = gzip.decompress(payload)
            elif compression == 2:
                payload = zlib.decompress(payload)
        except Exception:
            continue
        try:
            yield index % 32, index // 32, NBTReader(payload).root()
        except Exception as exc:  # 坏区块直接跳过
            print("skip corrupted chunk:", exc)


def section_block_names(section):
    states = section.get("block_states") or {}
    palette = [entry.get("Name", "minecraft:air") for entry in states.get("palette", [])]
    if not palette:
        return None
    if len(palette) == 1:
        return palette[0]
    longs = states.get("data")
    if not longs:
        return palette[0]
    bits = max(4, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    block_count = 4096
    values = np.empty(block_count, dtype=np.uint16)
    unsigned = np.array([value & 0xFFFFFFFFFFFFFFFF for value in longs], dtype=np.uint64)
    indices = np.arange(block_count)
    long_index = indices // per_long
    shift = (indices % per_long) * bits
    mask = (1 << bits) - 1
    values[:] = ((unsigned[long_index] >> shift) & mask).astype(np.uint16)
    return [palette[value] if value < len(palette) else "minecraft:air" for value in values]


def chunk_heightmap(chunk):
    """返回 (16,16) 的最高非空气方块名与高度，索引 [z, x]。"""
    names = np.full((16, 16), "", dtype=object)
    height = np.full((16, 16), -9999, dtype=np.int32)
    for section in chunk.get("sections", []):
        section_y = section.get("Y", 0)
        states = section.get("block_states") or {}
        palette = [palette_name(entry) for entry in states.get("palette", [])]
        if not palette:
            continue
        data = states.get("data")
        if len(palette) == 1 or not data:
            values = np.zeros(4096, dtype=np.uint16)
        else:
            bits = max(4, (len(palette) - 1).bit_length())
            per_long = 64 // bits
            unsigned = np.array([value & 0xFFFFFFFFFFFFFFFF for value in data], dtype=np.uint64)
            indices = np.arange(4096)
            long_index = indices // per_long
            shift = ((indices % per_long) * bits).astype(np.uint64)
            mask = np.uint64((1 << bits) - 1)
            values = np.right_shift(unsigned[long_index], shift) & mask
            values = values.astype(np.uint16)
        palette_names = np.array(palette, dtype=object)
        block_names = palette_names[values].reshape(16, 16, 16)  # [y][z][x]
        solid = np.ones((16, 16, 16), dtype=bool)
        for air in AIR_BLOCKS:
            solid &= block_names != air
        for y in range(16):
            layer = solid[y]
            if not layer.any():
                continue
            world_y = section_y * 16 + y
            update = layer & (world_y > height)
            if update.any():
                height[update] = world_y
                names[update] = block_names[y][update]
    return names, height


def block_color(name):
    name = name.replace("minecraft:", "")
    if name in ("air",):
        return (0, 0, 0)
    if "water" in name or name == "bubble_column":
        return (49, 92, 170)
    if name in ("ice", "frosted_ice", "packed_ice", "blue_ice"):
        return (156, 203, 232)
    if "lava" in name or "magma" in name:
        return (226, 110, 30)
    if "snow" in name:
        return (242, 245, 248)
    if "grass_block" in name or name == "grass":
        return (92, 148, 63)
    if "mycelium" in name:
        return (130, 116, 126)
    if "podzol" in name:
        return (104, 78, 45)
    if "coarse_dirt" in name:
        return (115, 84, 53)
    if "dirt" in name:
        return (121, 88, 56)
    if "red_sand" in name:
        return (186, 104, 50)
    if "sandstone" in name:
        return (205, 190, 142)
    if "sand" in name:
        return (218, 205, 155)
    if "terracotta" in name or "hardened_clay" in name:
        return (171, 95, 64)
    if "gravel" in name:
        return (125, 121, 120)
    if "clay" in name:
        return (158, 164, 176)
    if "stone" in name or "andesite" in name or "diorite" in name or "granite" in name or "deepslate" in name or "tuff" in name:
        return (126, 126, 126)
    if "cobblestone" in name or "bedrock" in name:
        return (84, 84, 84)
    if "leaves" in name:
        return (60, 120, 45)
    if "log" in name or "wood" in name or "planks" in name:
        return (90, 66, 42)
    if "mushroom" in name:
        return (150, 110, 110)
    if "coal" in name:
        return (70, 70, 70)
    if "iron" in name:
        return (180, 150, 130)
    if "gold" in name:
        return (220, 190, 70)
    if "diamond" in name:
        return (90, 200, 200)
    if "redstone" in name:
        return (160, 40, 40)
    if "lapis" in name:
        return (40, 60, 160)
    if "emerald" in name:
        return (40, 160, 80)
    if "copper" in name:
        return (190, 120, 90)
    if "obsidian" in name:
        return (30, 20, 45)
    return (120, 120, 120)


def render(world_dir, output, chunk_from=None, chunk_to=None):
    region_dir = os.path.join(world_dir, "region")
    files = sorted(glob.glob(os.path.join(region_dir, "*.mca")))
    if not files:
        print("no region files in", region_dir)
        return

    chunks = {}
    min_cx = min_cz = 10 ** 9
    max_cx = max_cz = -10 ** 9
    for path in files:
        base = os.path.basename(path)
        parts = base[2:-4].split(".")
        if len(parts) != 2:
            continue
        rx, rz = int(parts[0]), int(parts[1])
        for local_x, local_z, chunk in parse_region(path):
            cx = rx * 32 + local_x
            cz = rz * 32 + local_z
            if chunk_from and (cx < chunk_from[0] or cz < chunk_from[1]):
                continue
            if chunk_to and (cx > chunk_to[0] or cz > chunk_to[1]):
                continue
            chunks[(cx, cz)] = chunk
            min_cx, max_cx = min(min_cx, cx), max(max_cx, cx)
            min_cz, max_cz = min(min_cz, cz), max(max_cz, cz)

    if not chunks:
        print("no chunks matched")
        return

    width = (max_cx - min_cx + 1) * 16
    depth = (max_cz - min_cz + 1) * 16
    colors = np.zeros((depth, width, 3), dtype=np.uint8)
    heights = np.full((depth, width), -9999, dtype=np.int32)

    for (cx, cz), chunk in chunks.items():
        names, height = chunk_heightmap(chunk)
        ox = (cx - min_cx) * 16
        oz = (cz - min_cz) * 16
        for x in range(16):
            for z in range(16):
                name = names[z, x]
                color = block_color(name)
                colors[oz + z, ox + x] = color
                heights[oz + z, ox + x] = height[z, x]

    # 按高度做明暗，模拟简单的山体阴影
    valid = heights > -9990
    if valid.any():
        low = np.percentile(heights[valid], 2)
        high = np.percentile(heights[valid], 98)
        span = max(1, high - low)
        shade = np.clip((heights - low) / span, 0, 1)
        colors = (colors.astype(np.float32) * (0.72 + 0.38 * shade[..., None])).clip(0, 255).astype(np.uint8)
        grad_z = np.gradient(heights.astype(np.float32), axis=0)
        grad_x = np.gradient(heights.astype(np.float32), axis=1)
        light = np.clip(0.85 + 0.06 * (-grad_x + grad_z), 0.65, 1.15)
        colors = (colors.astype(np.float32) * light[..., None]).clip(0, 255).astype(np.uint8)

    image = Image.fromarray(colors, "RGB")
    image.save(output)
    print("saved", output, image.size, "chunks:", len(chunks))
    print("height range:", int(heights[valid].min()) if valid.any() else None,
          "to", int(heights[valid].max()) if valid.any() else None)

    # 简单统计，便于快速判断地形是否合理
    flat_names = []
    for (cx, cz), chunk in list(chunks.items())[:1]:
        pass
    water = (colors[:, :, 2].astype(int) > 150) & (colors[:, :, 0].astype(int) < 90)
    print("water-ish pixels: %.1f%%" % (100.0 * water.sum() / water.size))


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    world = sys.argv[1]
    out = sys.argv[2]
    from_chunk = None
    to_chunk = None
    if "--from-chunk" in sys.argv:
        index = sys.argv.index("--from-chunk")
        from_chunk = (int(sys.argv[index + 1]), int(sys.argv[index + 2]))
    if "--to-chunk" in sys.argv:
        index = sys.argv.index("--to-chunk")
        to_chunk = (int(sys.argv[index + 1]), int(sys.argv[index + 2]))
    render(world, out, from_chunk, to_chunk)
