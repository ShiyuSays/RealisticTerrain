package com.says.realisticterrain.generator;

import com.says.realisticterrain.RealisticTerrainPlugin;
import com.says.realisticterrain.terrain.RealisticWorld;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

/**
 * 把地形采样得到的群系交给服务端。植被、雪线、生物生成都会读取这里的结果。
 */
public final class RealisticBiomeProvider extends BiomeProvider {

    private final RealisticTerrainPlugin plugin;
    private volatile RealisticWorld world;
    private volatile int revision = -1;

    public RealisticBiomeProvider(RealisticTerrainPlugin plugin) {
        this.plugin = plugin;
    }

    private RealisticWorld world(WorldInfo info) {
        int current = plugin.revision();
        RealisticWorld cached = world;
        if (cached == null || revision != current) {
            cached = plugin.worldFor(info);
            world = cached;
            revision = current;
        }
        return cached;
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return plugin.biomeMapping().get(world(worldInfo).biomeAt(x, z));
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return plugin.biomeMapping().all();
    }
}
