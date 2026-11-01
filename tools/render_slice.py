"""Render a vertical x-fixed z-y slice of an Anvil world.

Usage:
    python render_slice.py <world_dir> <x> <center_z> <output.png> [--z-radius 20] [--scale 8]
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render_map  # noqa: E402


def _decode_section(section):
    states = section.get("block_states") or {}
    palette = [render_map.palette_name(entry) for entry in states.get("palette", [])]
    if not palette:
        return None
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
        values = (np.right_shift(unsigned[long_index], shift) & mask).astype(np.uint16)
    names = np.array(palette, dtype=object)[values]
    return names.reshape(16, 16, 16)  # [y][z][x]


def main():
    world = sys.argv[1]
    x = int(sys.argv[2])
    center_z = int(sys.argv[3])
    out = sys.argv[4]
    z_radius = 20
    scale = 8
    if "--z-radius" in sys.argv:
        z_radius = int(sys.argv[sys.argv.index("--z-radius") + 1])
    if "--scale" in sys.argv:
        scale = int(sys.argv[sys.argv.index("--scale") + 1])

    chunk_x = x >> 4
    local_x = x & 15
    z_from = center_z - z_radius
    z_to = center_z + z_radius

    columns = {}
    region_dir = os.path.join(world, "region")
    for name in os.listdir(region_dir):
        if not name.endswith(".mca"):
            continue
        parts = name[2:-4].split(".")
        if len(parts) != 2:
            continue
        rx, rz = int(parts[0]), int(parts[1])
        for local_cx, local_cz, chunk in render_map.parse_region(os.path.join(region_dir, name)):
            cx = rx * 32 + local_cx
            cz = rz * 32 + local_cz
            if cx != chunk_x:
                continue
            if (cz * 16 + 16 < z_from) or (cz * 16 > z_to):
                continue
            for section in chunk.get("sections", []):
                blocks = _decode_section(section)
                if blocks is None:
                    continue
                base_y = section.get("Y", 0) * 16
                for y in range(16):
                    for z in range(16):
                        wz = cz * 16 + z
                        if wz < z_from or wz > z_to:
                            continue
                        columns[(base_y + y, wz)] = blocks[y][z][local_x]

    if not columns:
        print("no data")
        return

    solid = [key[0] for key, name in columns.items()
             if name not in ("minecraft:air", "minecraft:cave_air")]
    if not solid:
        print("nothing solid")
        return
    ys = list(range(min(solid) - 2, max(solid) + 3))
    zs = list(range(z_from, z_to + 1))
    img = np.zeros((len(ys), len(zs), 3), dtype=np.uint8)
    for row, y in enumerate(ys):
        for col, z in enumerate(zs):
            name = columns.get((y, z), "minecraft:air")
            if name in ("minecraft:air", "minecraft:cave_air"):
                img[row, col] = (30, 30, 40)
            else:
                img[row, col] = render_map.block_color(name)

    # ys 是从低到高，图片要上下翻转（天空在上）
    image = Image.fromarray(img[::-1], "RGB")
    image = image.resize((image.width * scale, image.height * scale), Image.NEAREST)
    image.save(out)
    print("saved", out, image.size, "y", ys[0], "-", ys[-1])


if __name__ == "__main__":
    main()
