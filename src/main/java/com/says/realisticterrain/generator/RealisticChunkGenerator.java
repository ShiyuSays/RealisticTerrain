package com.says.realisticterrain.generator;

import com.says.realisticterrain.RealisticTerrainPlugin;
import com.says.realisticterrain.terrain.RealisticWorld;
import com.says.realisticterrain.terrain.Settings;
import org.bukkit.HeightMap;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bukkit 区块生成器。
 *
 * <p>地形、水、洞穴、地表、基岩、矿物全部在 {@link #generateNoise} 里一次完成，
 * 其余阶段返回 false。这个方法在 Paper/Folia 中本来就会在区块工作线程上调用，
 * 内部不访问任何 Bukkit 世界对象，也不调用调度器，因此天然适配 Folia。</p>
 */
@SuppressWarnings("deprecation")
public final class RealisticChunkGenerator extends ChunkGenerator {

    private final RealisticTerrainPlugin plugin;
    private final AtomicLong generatedChunks = new AtomicLong();
    private final AtomicLong totalNanos = new AtomicLong();

    public RealisticChunkGenerator(RealisticTerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ,
                              ChunkData chunkData) {
        Settings settings = plugin.settings();
        long start = settings.debug() ? System.nanoTime() : 0L;
        RealisticWorld world = plugin.worldFor(worldInfo);
        world.generate(chunkX, chunkZ, chunkData);
        if (settings.debug()) {
            long elapsed = System.nanoTime() - start;
            long chunks = generatedChunks.incrementAndGet();
            long total = totalNanos.addAndGet(elapsed);
            if (chunks <= 5) {
                plugin.getLogger().info(String.format("生成区块 (%d, %d)，用时 %.2f ms",
                        chunkX, chunkZ, elapsed / 1_000_000.0));
            }
            if (chunks % 200 == 0) {
                plugin.getLogger().info(String.format(
                        "已生成 %d 个区块，平均 %.2f ms/区块（最近 200 个）",
                        chunks, total / 200.0 / 1_000_000.0));
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new RealisticBiomeProvider(plugin);
    }

    @Override
    public int getBaseHeight(WorldInfo worldInfo, Random random, int x, int z, HeightMap heightMap) {
        int height = plugin.worldFor(worldInfo).surfaceHeight(x, z) + 1;
        int min = worldInfo.getMinHeight() + 1;
        int max = worldInfo.getMaxHeight() - 1;
        return Math.max(min, Math.min(height, max));
    }

    @Override
    public boolean isParallelCapable() {
        return true;
    }

    /**
     * 注意：在 Paper/CraftBukkit 里，这个方法的含义是“是否让原版噪声生成器
     * 也跑一遍”。返回 false 才是完全接管地形；{@link #generateNoise} 仍然会被调用。
     */
    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return plugin.settings().vanillaDecoration();
    }

    @Override
    public boolean shouldGenerateDecorations(WorldInfo worldInfo, Random random,
                                             int chunkX, int chunkZ) {
        return plugin.settings().vanillaDecoration();
    }

    @Override
    public boolean shouldGenerateMobs() {
        return plugin.settings().vanillaMobs();
    }

    @Override
    public boolean shouldGenerateMobs(WorldInfo worldInfo, Random random, int chunkX, int chunkZ) {
        return plugin.settings().vanillaMobs();
    }

    @Override
    public boolean shouldGenerateStructures() {
        return plugin.settings().vanillaStructures();
    }

    @Override
    public boolean shouldGenerateStructures(WorldInfo worldInfo, Random random,
                                            int chunkX, int chunkZ) {
        return plugin.settings().vanillaStructures();
    }
}
