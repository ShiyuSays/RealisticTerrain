package com.says.realisticterrain.terrain;

import com.says.realisticterrain.biome.BiomeType;
import com.says.realisticterrain.noise.NoiseFactory;
import com.says.realisticterrain.noise.SplitMix64;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

/**
 * 一个世界高度配置下的完整地形生成器。
 *
 * <p>实例在构造后不可变；所有可变临时数组都放在 {@link ThreadLocal} 里，
 * 因此可以被 Folia 的多个区域线程同时调用，不需要任何调度器。</p>
 */
public final class RealisticWorld {

    private static final Material AIR = Material.AIR;
    private static final Material STONE = Material.STONE;
    private static final Material DEEPSLATE = Material.DEEPSLATE;
    private static final Material BEDROCK = Material.BEDROCK;
    private static final Material WATER = Material.WATER;
    private static final Material LAVA = Material.LAVA;
    private static final Material ICE = Material.ICE;

    private final long seed;
    private final Settings settings;
    private final WorldProfile profile;
    private final TerrainSampler terrain;
    private final CaveSampler caves;
    private final DensitySampler density;
    private final SurfacePainter surface;
    private final OreGenerator ores;
    private final int lavaY;

    private final ThreadLocal<ChunkScratch> scratch = ThreadLocal.withInitial(ChunkScratch::new);

    public RealisticWorld(long seed, Settings settings, WorldProfile profile) {
        this.seed = seed;
        this.settings = settings;
        this.profile = profile;
        NoiseFactory noise = new NoiseFactory(seed);
        this.terrain = new TerrainSampler(noise, settings);
        this.caves = new CaveSampler(noise);
        this.density = new DensitySampler(noise);
        this.surface = new SurfacePainter(noise);
        this.ores = new OreGenerator(settings, profile);
        this.lavaY = Math.max(profile.worldMin() + 1,
                Math.min(profile.seaLevel() - 8, settings.lavaLevel()));
    }

    public long seed() {
        return seed;
    }

    public Settings settings() {
        return settings;
    }

    public WorldProfile profile() {
        return profile;
    }

    public TerrainColumn columnAt(int x, int z) {
        return terrain.columnAt(x, z, profile);
    }

    public BiomeType biomeAt(int x, int z) {
        return columnAt(x, z).biome();
    }

    public int surfaceHeight(int x, int z) {
        return clampHeight(columnAt(x, z).surfaceY());
    }

    public void generate(int chunkX, int chunkZ, ChunkData data) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        ChunkScratch scratch = this.scratch.get();
        scratch.ensure(profile.worldMin(), profile.terrainTop());

        // 1. 16×16 列：群系、基础高度、洞穴区域。
        for (int lx = 0; lx < 16; lx++) {
            int worldX = originX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int index = lx * 16 + lz;
                TerrainColumn column = terrain.columnAt(worldX, originZ + lz, profile);
                scratch.columns[index] = column;
                if (settings.caves()) {
                    scratch.caveAllowed[index] = caves.regionAllowed(worldX, originZ + lz);
                    scratch.caveEntrance[index] = caves.entranceAllowed(worldX, originZ + lz);
                }
            }
        }

        // 2. 5×5 高度网格，供三维密度插值使用。
        for (int gx = 0; gx < ChunkScratch.GRID; gx++) {
            int worldX = originX + gx * 4;
            for (int gz = 0; gz < ChunkScratch.GRID; gz++) {
                TerrainColumn column = terrain.columnAt(worldX, originZ + gz * 4, profile);
                int index = gx * ChunkScratch.GRID + gz;
                scratch.gridHeight[index] = column.height();
                scratch.gridMountain[index] = column.mountain();
            }
        }

        // 3. 三维密度网格。
        for (int gx = 0; gx < ChunkScratch.GRID; gx++) {
            int worldX = originX + gx * 4;
            for (int gz = 0; gz < ChunkScratch.GRID; gz++) {
                int index = gx * ChunkScratch.GRID + gz;
                double height = scratch.gridHeight[index];
                double mountain = scratch.gridMountain[index];
                for (int gy = 0; gy < scratch.densityLevels; gy++) {
                    int y = profile.worldMin() + gy * 8;
                    if (y > profile.terrainTop()) {
                        y = profile.terrainTop();
                    }
                    scratch.density[scratch.densityIndex(gx, gy, gz)] =
                            density.densityAt(worldX, y, originZ + gz * 4, height, mountain, settings);
                }
            }
        }

        // 4. 洞穴噪声网格（4×4×4）。
        if (settings.caves()) {
            for (int gx = 0; gx < ChunkScratch.GRID; gx++) {
                int worldX = originX + gx * 4;
                for (int gz = 0; gz < ChunkScratch.GRID; gz++) {
                    int worldZ = originZ + gz * 4;
                    for (int gy = 0; gy < scratch.caveLevels; gy++) {
                        int y = profile.worldMin() + gy * 4;
                        int index = scratch.caveIndex(gx, gy, gz);
                        scratch.caveA[index] = caves.tunnelA(worldX, y, worldZ);
                        scratch.caveB[index] = caves.tunnelB(worldX, y, worldZ);
                        scratch.caveC[index] = caves.cavern(worldX, y, worldZ);
                    }
                }
            }
        }

        // 5. 填充石头/深板岩。
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                fillColumn(data, scratch, lx, lz);
            }
        }

        // 6. 挖洞穴。放在地表铺设之前，洞穴口会得到正确的地表材质。
        if (settings.caves()) {
            carveCaves(data, scratch);
        }

        // 7. 重新扫描实际最高实心方块（洞穴可能改变了它）。
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int index = lx * 16 + lz;
                TerrainColumn column = scratch.columns[index];
                int max = Math.min(profile.terrainTop(), column.surfaceY() + 24);
                int top = profile.worldMin();
                for (int y = max; y > profile.worldMin(); y--) {
                    if (isSolid(data.getType(lx, y, lz))) {
                        top = y;
                        break;
                    }
                }
                scratch.tops[index] = top;
            }
        }

        // 8. 填水、结冰。
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int index = lx * 16 + lz;
                int top = scratch.tops[index];
                if (top < profile.seaLevel()) {
                    int from = Math.max(profile.worldMin() + 1, top + 1);
                    if (from < 0) {
                        // 与负 Y 的石头填充同理，部分 Paper 版本会丢弃负 Y 的 setRegion。
                        for (int y = from; y <= profile.seaLevel(); y++) {
                            data.setBlock(lx, y, lz, WATER);
                        }
                    } else {
                        data.setRegion(lx, from, lz, lx + 1, profile.seaLevel() + 1, lz + 1, WATER);
                    }
                    if (scratch.columns[index].biome().isFrozen()
                            && data.getType(lx, profile.seaLevel(), lz) == WATER) {
                        data.setBlock(lx, profile.seaLevel(), lz, ICE);
                    }
                }
            }
        }

        // 9. 地表材质。
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int index = lx * 16 + lz;
                int top = scratch.tops[index];
                if (top <= profile.worldMin()) {
                    continue;
                }
                int slope = slopeAt(scratch, lx, lz);
                surface.paint(data, lx, lz, originX + lx, originZ + lz, top,
                        scratch.columns[index], slope, profile);
            }
        }

        // 10. 基岩。
        SplitMix64 bedrock = new SplitMix64(seed
                ^ ((long) chunkX << 32)
                ^ (chunkZ & 0xFFFFFFFFL)
                ^ 0x5DEECE66DL);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                data.setBlock(lx, profile.worldMin(), lz, BEDROCK);
                for (int i = 1; i < profile.bedrockDepth(); i++) {
                    if (bedrock.nextDouble() < 0.72 - i * 0.18) {
                        data.setBlock(lx, profile.worldMin() + i, lz, BEDROCK);
                    }
                }
            }
        }

        // 11. 矿脉与地质团块。
        if (settings.ores()) {
            ores.generate(data, chunkX, chunkZ, seed, profile, scratch.columns);
        }
    }

    /**
     * 按密度场填充一列。密度大于 0 的连续段用 setRegion 一次写入，
     * 比逐方块 setBlock 快很多。
     */
    private void fillColumn(ChunkData data, ChunkScratch scratch, int lx, int lz) {
        TerrainColumn column = scratch.columns[lx * 16 + lz];
        int top = clampHeight(column.surfaceY());
        int fillTop = Math.min(profile.terrainTop(), top + 24);

        // 负 Y 用逐方块写入。部分 Paper 版本对 setRegion 的负 Y 区域会
        // 静默丢弃，逐方块 setBlock 在所有 1.18+ 服务端上都可靠。
        int negativeEnd = Math.min(fillTop, -1);
        for (int y = profile.floor(); y <= negativeEnd; y++) {
            if (scratch.densityAt(lx, y, lz) > 0.0) {
                data.setBlock(lx, y, lz, DEEPSLATE);
            }
        }

        int start = Math.max(profile.floor(), 0);
        int runStart = -1;
        for (int y = start; y <= fillTop; y++) {
            boolean solid = scratch.densityAt(lx, y, lz) > 0.0;
            if (solid) {
                if (runStart < 0) {
                    runStart = y;
                }
            } else if (runStart >= 0) {
                data.setRegion(lx, runStart, lz, lx + 1, y, lz + 1, STONE);
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            data.setRegion(lx, runStart, lz, lx + 1, fillTop + 1, lz + 1, STONE);
        }
    }

    private void carveCaves(ChunkData data, ChunkScratch scratch) {
        double caveDensity = settings.caveDensity();
        if (caveDensity <= 0.0) {
            return;
        }
        int sea = profile.seaLevel();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int index = lx * 16 + lz;
                if (!scratch.caveAllowed[index]) {
                    continue;
                }
                int top = scratch.columns[index].surfaceY();
                if (top <= profile.worldMin() + 4) {
                    continue;
                }
                boolean entrance = scratch.caveEntrance[index];
                for (int y = profile.floor() + 1; y <= top - 2; y++) {
                    if (y > top - 10) {
                        // 地表附近只在“入口列”开口，避免整片地表被挖穿。
                        if (!entrance || top < sea + 1) {
                            continue;
                        }
                    }
                    double a = scratch.caveAAt(lx, y, lz);
                    double b = scratch.caveBAt(lx, y, lz);
                    boolean carve = caves.isTunnel(a, b, caveDensity);
                    if (!carve && y < sea - 18 && y < top - 12) {
                        carve = caves.isCavern(scratch.caveCAt(lx, y, lz), caveDensity);
                    }
                    if (!carve) {
                        continue;
                    }
                    Material current = data.getType(lx, y, lz);
                    if (!isCarvable(current)) {
                        continue;
                    }
                    if (settings.lavaLakes() && y <= lavaY) {
                        data.setBlock(lx, y, lz, LAVA);
                    } else {
                        data.setBlock(lx, y, lz, AIR);
                    }
                }
            }
        }
    }

    private static int slopeAt(ChunkScratch scratch, int lx, int lz) {
        int index = lx * 16 + lz;
        int height = scratch.columns[index].surfaceY();
        int slope = 0;
        if (lx > 0) {
            slope = Math.max(slope, Math.abs(height - scratch.columns[index - 16].surfaceY()));
        }
        if (lx < 15) {
            slope = Math.max(slope, Math.abs(height - scratch.columns[index + 16].surfaceY()));
        }
        if (lz > 0) {
            slope = Math.max(slope, Math.abs(height - scratch.columns[index - 1].surfaceY()));
        }
        if (lz < 15) {
            slope = Math.max(slope, Math.abs(height - scratch.columns[index + 1].surfaceY()));
        }
        return slope;
    }

    private int clampHeight(int height) {
        if (height < profile.floor()) {
            return profile.floor();
        }
        return Math.min(height, profile.terrainTop());
    }

    private static boolean isSolid(Material material) {
        return material != AIR && material != WATER && material != LAVA;
    }

    private static boolean isCarvable(Material material) {
        return material != AIR && material != WATER && material != LAVA
                && material != BEDROCK && material != ICE;
    }
}
