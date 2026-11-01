package com.says.realisticterrain.config;

import com.says.realisticterrain.terrain.Settings;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 把 config.yml 读成不可变的 {@link Settings}。所有数值都会在
 * {@link Settings.Builder#build()} 里做范围钳制，写错不会让生成器抛异常。
 */
public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static Settings load(FileConfiguration config) {
        return Settings.builder()
                .seaLevel(config.getInt("world.sea-level", 96))
                .maxTerrainY(config.getInt("world.max-terrain-y", 304))
                .bedrockDepth(config.getInt("world.bedrock-depth", 3))
                .continentScale(config.getDouble("terrain.continent-scale", 1.0))
                .heightScale(config.getDouble("terrain.height-scale", 1.0))
                .mountainHeight(config.getDouble("terrain.mountain-height", 170.0))
                .oceanDepth(config.getDouble("terrain.ocean-depth", 58.0))
                .roughness(config.getDouble("terrain.roughness", 1.0))
                .rivers(config.getBoolean("terrain.rivers", true))
                .riverWidth(config.getDouble("terrain.river-width", 1.0))
                .caves(config.getBoolean("caves.enabled", true))
                .caveDensity(config.getDouble("caves.density", 0.35))
                .lavaLevel(config.getInt("caves.lava-level", -54))
                .lavaLakes(config.getBoolean("caves.lava-lakes", true))
                .ores(config.getBoolean("ores.enabled", true))
                .oreDensity(config.getDouble("ores.density", 1.0))
                .geology(config.getBoolean("ores.geology", true))
                .vanillaDecoration(config.getBoolean("features.vanilla-decoration", true))
                .vanillaStructures(config.getBoolean("features.vanilla-structures", false))
                .vanillaMobs(config.getBoolean("features.vanilla-mobs", true))
                .debug(config.getBoolean("debug", false))
                .build();
    }
}
