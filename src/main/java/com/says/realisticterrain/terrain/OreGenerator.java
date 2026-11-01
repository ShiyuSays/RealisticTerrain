package com.says.realisticterrain.terrain;

import com.says.realisticterrain.noise.SplitMix64;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.ArrayList;
import java.util.List;

/**
 * 确定性的矿脉与地质团块。
 *
 * <p>每个区块用“世界种子 ^ 区块坐标”作为随机种子，因此同一个种子、
 * 同一个坐标生成的矿物完全可复现。只替换石头/深板岩/凝灰岩，不会把矿
 * 嵌进空气、水或洞穴里。</p>
 */
public final class OreGenerator {

    private static final Material STONE = Material.STONE;
    private static final Material DEEPSLATE = Material.DEEPSLATE;
    private static final Material TUFF = Material.TUFF;
    private static final Material ANDESITE = Material.ANDESITE;
    private static final Material DIORITE = Material.DIORITE;
    private static final Material GRANITE = Material.GRANITE;

    private final List<Vein> veins = new ArrayList<>();

    public OreGenerator(Settings settings, WorldProfile profile) {
        if (!settings.ores()) {
            return;
        }
        double density = settings.oreDensity();
        int min = profile.worldMin();
        int sea = profile.seaLevel();
        int top = profile.terrainTop();

        add("COAL_ORE", "DEEPSLATE_COAL_ORE", scale(9, density), 10, min + 24, sea + 80);
        add("COPPER_ORE", "DEEPSLATE_COPPER_ORE", scale(6, density), 8, min + 24, sea + 36);
        add("IRON_ORE", "DEEPSLATE_IRON_ORE", scale(8, density), 8, min + 12, sea + 24);
        add("GOLD_ORE", "DEEPSLATE_GOLD_ORE", scale(3, density), 7, min + 6, sea - 28);
        add("REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE", scale(5, density), 8, min + 4, sea - 38);
        add("LAPIS_ORE", "DEEPSLATE_LAPIS_ORE", scale(2, density), 7, min + 6, sea - 30);
        add("DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", scale(2, density), 7, min + 3, min + 58);
        add("EMERALD_ORE", "DEEPSLATE_EMERALD_ORE", scale(2, density), 4, sea + 78, top - 8,
                true);

        if (settings.geology()) {
            add("GRANITE", "GRANITE", scale(4, density), 26, min + 8, sea + 40);
            add("DIORITE", "DIORITE", scale(4, density), 26, min + 8, sea + 40);
            add("ANDESITE", "ANDESITE", scale(4, density), 26, min + 8, sea + 40);
            add("TUFF", "TUFF", scale(3, density), 24, min + 2, min + 70);
            add("GRAVEL", "GRAVEL", scale(3, density), 22, min + 8, sea + 30);
            add("DIRT", "DIRT", scale(3, density), 22, min + 8, sea + 30);
        }
    }

    private static int scale(int base, double density) {
        return Math.max(0, (int) Math.round(base * density));
    }

    private void add(String material, String deepslateMaterial, int count, int size,
                     int minY, int maxY) {
        add(material, deepslateMaterial, count, size, minY, maxY, false);
    }

    private void add(String material, String deepslateMaterial, int count, int size,
                     int minY, int maxY, boolean mountainOnly) {
        if (count <= 0 || maxY <= minY) {
            return;
        }
        Material normal = Blocks.get(material, STONE);
        Material deep = Blocks.get(deepslateMaterial, normal);
        veins.add(new Vein(normal, deep, count, size, minY, maxY, mountainOnly));
    }

    public void generate(ChunkData data, int chunkX, int chunkZ, long worldSeed,
                         WorldProfile profile, TerrainColumn[] columns) {
        if (veins.isEmpty()) {
            return;
        }
        long seed = worldSeed
                ^ (chunkX * 0x9E3779B97F4A7C15L)
                ^ (chunkZ * 0xC2B2AE3D27D4EB4FL)
                ^ 0x5DEECE66DL;
        SplitMix64 random = new SplitMix64(seed);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;

        for (Vein vein : veins) {
            int count = vein.count + random.nextInt(3) - 1;
            for (int i = 0; i < count; i++) {
                int localX = 2 + random.nextInt(12);
                int localZ = 2 + random.nextInt(12);
                int upper = Math.min(vein.maxY, profile.terrainTop() - 4);
                if (upper <= vein.minY) {
                    continue;
                }
                int y = vein.minY + random.nextInt(upper - vein.minY + 1);
                if (vein.mountainOnly) {
                    TerrainColumn column = columns[(localX << 4) + localZ];
                    // 绿宝石只出现在山地；用 hill 高度再兜一层底。
                    if (!column.biome().isMountain() || column.height() < profile.seaLevel() + 60) {
                        continue;
                    }
                }
                placeVein(data, random, vein, localX, y, localZ, profile);
            }
        }
    }

    private static void placeVein(ChunkData data, SplitMix64 random, Vein vein, int startX,
                                  int startY, int startZ, WorldProfile profile) {
        int x = startX;
        int y = startY;
        int z = startZ;
        int steps = vein.size + random.nextInt(4);
        for (int i = 0; i < steps; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16
                    && y > profile.worldMin() + 1 && y < profile.terrainTop() - 1) {
                Material current = data.getType(x, y, z);
                if (current == STONE || current == DEEPSLATE || current == TUFF
                        || current == ANDESITE || current == DIORITE || current == GRANITE) {
                    data.setBlock(x, y, z, current == DEEPSLATE ? vein.deepslate : vein.material);
                }
            }
            x += random.nextInt(3) - 1;
            y += random.nextInt(3) - 1;
            z += random.nextInt(3) - 1;
        }
    }

    private static final class Vein {
        private final Material material;
        private final Material deepslate;
        private final int count;
        private final int size;
        private final int minY;
        private final int maxY;
        private final boolean mountainOnly;

        private Vein(Material material, Material deepslate, int count, int size,
                     int minY, int maxY, boolean mountainOnly) {
            this.material = material;
            this.deepslate = deepslate;
            this.count = count;
            this.size = size;
            this.minY = minY;
            this.maxY = maxY;
            this.mountainOnly = mountainOnly;
        }
    }
}
