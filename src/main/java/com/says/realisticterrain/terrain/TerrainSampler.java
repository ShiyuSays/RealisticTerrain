package com.says.realisticterrain.terrain;

import com.says.realisticterrain.biome.BiomeType;
import com.says.realisticterrain.noise.NoiseFactory;
import com.says.realisticterrain.noise.NoiseMath;
import com.says.realisticterrain.noise.OctaveNoise;

import static com.says.realisticterrain.noise.NoiseMath.clamp;
import static com.says.realisticterrain.noise.NoiseMath.mix;
import static com.says.realisticterrain.noise.NoiseMath.smoothstep;

/**
 * 现代多噪声地形的“气候 + 高度 + 群系”采样器。
 *
 * <p>这里不出现任何 Bukkit 类型，纯数学、线程安全，方便单元测试，也方便
 * Folia 的多个区域线程同时调用。地形由四组场叠加：</p>
 * <ul>
 *   <li><b>大陆度</b>：决定海洋、海岸和内陆；</li>
 *   <li><b>侵蚀度</b>：低侵蚀形成山脉/悬崖，高侵蚀形成平原；</li>
 *   <li><b>山脊</b>：低频脊状噪声，形成连绵山脉而不是孤立尖峰；</li>
 *   <li><b>细节/河流</b>：局部起伏与河谷。</li>
 * </ul>
 */
public final class TerrainSampler {

    private final Settings settings;

    private final OctaveNoise continentNoise;
    private final OctaveNoise erosionNoise;
    private final OctaveNoise ridgeNoise;
    private final OctaveNoise cragNoise;
    private final OctaveNoise detailNoise;
    private final OctaveNoise microNoise;
    private final OctaveNoise temperatureNoise;
    private final OctaveNoise humidityNoise;
    private final OctaveNoise riverNoise;
    private final OctaveNoise warpX;
    private final OctaveNoise warpZ;
    private final OctaveNoise variantNoise;
    private final OctaveNoise mushroomNoise;

    public TerrainSampler(NoiseFactory noise, Settings settings) {
        this.settings = settings;
        double continentScale = settings.continentScale();
        this.continentNoise = noise.octaves("continent", 5, 0.52, 0.00042 / continentScale);
        this.erosionNoise = noise.octaves("erosion", 4, 0.50, 0.00105);
        this.ridgeNoise = noise.octaves("ridge", 4, 0.52, 0.00205);
        this.cragNoise = noise.octaves("crag", 3, 0.55, 0.0062);
        this.detailNoise = noise.octaves("detail", 4, 0.50, 0.0105);
        this.microNoise = noise.octaves("micro", 2, 0.50, 0.047);
        this.temperatureNoise = noise.octaves("temperature", 4, 0.50, 0.00072);
        this.humidityNoise = noise.octaves("humidity", 4, 0.50, 0.00086);
        this.riverNoise = noise.octaves("river", 3, 0.55, 0.00155);
        this.warpX = noise.octaves("warp_x", 3, 0.50, 0.00090);
        this.warpZ = noise.octaves("warp_z", 3, 0.50, 0.00090);
        this.variantNoise = noise.octaves("variant", 3, 0.50, 0.017);
        this.mushroomNoise = noise.octaves("mushroom", 2, 0.50, 0.00055);
    }

    public TerrainColumn columnAt(int x, int z, WorldProfile profile) {
        // 域扭曲：让海岸线、山脉和河流不是噪声的“圆斑”形状。
        double warpScale = 0.0011;
        double wx = x + warpX.noise2(x * warpScale, z * warpScale) * 82.0;
        double wz = z + warpZ.noise2(x * warpScale + 731.0, z * warpScale - 419.0) * 82.0;

        double continent = clamp(continentNoise.noise2(wx, wz) * 1.70, -1.0, 1.0);
        double erosion = clamp(erosionNoise.noise2(wx, wz) * 1.85, -1.0, 1.0);
        double ridgeRaw = clamp(ridgeNoise.noise2(wx, wz) * 1.25, -1.0, 1.0);
        double cragRaw = clamp(cragNoise.noise2(wx * 1.45, wz * 1.45) * 1.35, -1.0, 1.0);
        double detail = clamp(detailNoise.noise2(wx, wz) * 1.55, -1.0, 1.0);
        double micro = clamp(microNoise.noise2(wx, wz) * 1.60, -1.0, 1.0);
        double temperature = clamp(temperatureNoise.noise2(wx, wz) * 1.85, -1.0, 1.0);
        double humidity = clamp(humidityNoise.noise2(wx, wz) * 1.85, -1.0, 1.0);
        double variant = clamp(variantNoise.noise2(wx, wz) * 1.70, -1.0, 1.0);
        double mushroom = mushroomNoise.noise2(wx, wz);

        double sea = profile.seaLevel();
        double land = smoothstep(-0.30, 0.10, continent);
        double inland = smoothstep(0.02, 0.68, continent);
        double deep = 1.0 - smoothstep(-0.70, -0.32, continent);

        double oceanFloor = sea - 8.0 - settings.oceanDepth() * deep;
        double landBase = sea + 4.0 + inland * 28.0;
        double height = mix(oceanFloor, landBase, land);

        // 海岸带略微压平，形成沙滩和浅海大陆架；压得太多会出现过宽的沙环。
        double coast = (1.0 - smoothstep(0.02, 0.14, Math.abs(continent))) * land;
        height = mix(height, sea + 2.0, coast * 0.20);

        // 侵蚀度决定起伏幅度：平原细腻，山地粗粝。
        double flatness = smoothstep(-0.20, 0.80, erosion);
        double relief = 1.0 - flatness * 0.82;
        double amplitude = 5.0 + 21.0 * relief;
        height += detail * amplitude * (0.25 + 0.75 * land);
        height += micro * 2.8 * (0.30 + 0.70 * land);

        // 山脉：大陆内部 + 低侵蚀 + 脊状噪声。
        double mountainMask = smoothstep(-0.02, 0.58, continent)
                * smoothstep(0.62, -0.42, erosion);
        double ridge = 1.0 - Math.abs(ridgeRaw);
        ridge = Math.pow(clamp(ridge, 0.0, 1.0), 1.62);
        double peak = ridge * ridge * ridge * ridge;
        double mountain = mountainMask * (0.12 + 0.82 * ridge);
        height += mountain * settings.mountainHeight();
        height += peak * mountainMask * settings.mountainHeight() * 0.28;
        // 山脊上的碎裂岩层：给大山加一点短尺度的锯齿，避免变成光滑的雪馒头。
        double crag = 1.0 - Math.abs(cragRaw);
        crag = clamp(crag, 0.0, 1.0);
        height += mountainMask * crag * crag * 14.0;
        // 山脚额外增加一些碎石坡式起伏。
        height += detail * mountainMask * 12.0;

        // 河流：在陆地上切出浅河谷，水面仍由海平面统一填充。
        double river = 0.0;
        if (settings.rivers()) {
            double raw = Math.abs(riverNoise.noise2(wx, wz));
            double width = 0.040 * settings.riverWidth();
            river = 1.0 - smoothstep(0.0, width, raw);
            river *= smoothstep(0.02, 0.35, land);
            river *= 1.0 - smoothstep(sea + 70.0, sea + 130.0, height);
            if (river > 0.01) {
                double bed = sea - 4.0;
                if (height > bed) {
                    height = mix(height, bed, river * 0.88);
                }
            }
        }

        // 以海平面为中心做整体缩放，再做软顶，避免大量山峰被硬切成平台。
        height = sea + (height - sea) * settings.heightScale();
        double softTop = profile.terrainTop() - 12.0;
        if (height > softTop) {
            height = softTop + (height - softTop) * 0.45;
        }
        height = clamp(height, profile.worldMin() + 2.5, profile.terrainTop());

        // 海拔越高越冷；这是雪线和高山群系的基础。
        double altitude = Math.max(0.0, height - sea);
        double localTemperature = clamp(temperature - Math.min(0.90, altitude / 300.0 * 0.78),
                -1.0, 1.0);
        double localHumidity = clamp(humidity - altitude / 900.0, -1.0, 1.0);

        double mountainStrength = clamp(mountain, 0.0, 1.0);
        BiomeType biome = chooseBiome(height, sea, continent, localTemperature, localHumidity,
                erosion, mountainStrength, river, variant, mushroom);

        return new TerrainColumn(x, z, height, biome, continent, erosion, mountainStrength,
                river, localTemperature, localHumidity, 1.0 - land, ridge, variant);
    }

    private static BiomeType chooseBiome(double height, double sea, double continent,
                                         double temperature, double humidity, double erosion,
                                         double mountain, double river, double variant,
                                         double mushroom) {
        double altitude = height - sea;

        if (river > 0.45 && altitude < 32.0) {
            return temperature < -0.45 ? BiomeType.FROZEN_RIVER : BiomeType.RIVER;
        }

        if (height < sea - 1.0) {
            if (temperature < -0.50) {
                return BiomeType.FROZEN_OCEAN;
            }
            if (sea - height > 38.0) {
                return BiomeType.DEEP_OCEAN;
            }
            if (temperature > 0.55 && sea - height < 22.0) {
                return BiomeType.WARM_OCEAN;
            }
            if (temperature > 0.15) {
                return BiomeType.LUKEWARM_OCEAN;
            }
            return BiomeType.OCEAN;
        }

        if (height <= sea + 1.8 && continent < 0.065) {
            if (temperature < -0.45) {
                return BiomeType.SNOWY_BEACH;
            }
            if (erosion < -0.28 || mountain > 0.40) {
                return BiomeType.STONY_SHORE;
            }
            return BiomeType.BEACH;
        }

        if (mushroom > 0.76 && continent > -0.05 && height < sea + 30.0) {
            return BiomeType.MUSHROOM_FIELDS;
        }

        // 高山：雪峰、裸岩、草甸、雪坡。
        if (altitude > 128.0 || (mountain > 0.55 && altitude > 70.0)) {
            if (altitude > 188.0 || (temperature < -0.45 && altitude > 150.0)) {
                if (temperature < -0.70) {
                    return BiomeType.FROZEN_PEAKS;
                }
                if (temperature < -0.45) {
                    return BiomeType.JAGGED_PEAKS;
                }
                return BiomeType.STONY_PEAKS;
            }
            if (altitude > 96.0) {
                if (temperature < -0.45) {
                    return BiomeType.SNOWY_SLOPES;
                }
                if (humidity > 0.20 && temperature < 0.35) {
                    return BiomeType.GROVE;
                }
                if (humidity > 0.02) {
                    return BiomeType.MEADOW;
                }
                return BiomeType.WINDSWEPT_HILLS;
            }
            if (humidity > 0.18) {
                return BiomeType.WINDSWEPT_FOREST;
            }
            if (erosion < -0.18) {
                return BiomeType.WINDSWEPT_GRAVELLY_HILLS;
            }
            return BiomeType.WINDSWEPT_HILLS;
        }

        // 低地：先按温度带，再按湿度分。
        if (temperature < -0.55) {
            if (humidity > 0.10) {
                return BiomeType.SNOWY_TAIGA;
            }
            if (variant > 0.62) {
                return BiomeType.ICE_SPIKES;
            }
            return BiomeType.SNOWY_PLAINS;
        }
        if (temperature < -0.22) {
            if (humidity > 0.42 && variant > 0.25) {
                return BiomeType.OLD_GROWTH_SPRUCE_TAIGA;
            }
            if (humidity > 0.18 && variant > 0.45) {
                return BiomeType.OLD_GROWTH_PINE_TAIGA;
            }
            return BiomeType.TAIGA;
        }

        if (temperature < 0.22) {
            if (humidity > 0.44 && altitude < 12.0 && erosion > 0.10) {
                return BiomeType.SWAMP;
            }
            if (humidity > 0.30 && temperature > 0.02 && variant > 0.78) {
                return BiomeType.CHERRY_GROVE;
            }
            if (humidity > 0.28) {
                return variant > 0.42 ? BiomeType.DARK_FOREST : BiomeType.BIRCH_FOREST;
            }
            if (humidity > 0.02) {
                return variant > 0.55 ? BiomeType.FLOWER_FOREST : BiomeType.FOREST;
            }
            if (humidity > -0.22) {
                return variant > 0.72 ? BiomeType.SUNFLOWER_PLAINS : BiomeType.PLAINS;
            }
            return BiomeType.PLAINS;
        }

        if (temperature < 0.55) {
            if (humidity < -0.30) {
                return BiomeType.DESERT;
            }
            if (humidity < -0.04) {
                return variant > 0.55 ? BiomeType.SAVANNA_PLATEAU : BiomeType.SAVANNA;
            }
            if (humidity < 0.25) {
                return variant > 0.65 ? BiomeType.FOREST : BiomeType.PLAINS;
            }
            if (humidity < 0.50) {
                return BiomeType.SPARSE_JUNGLE;
            }
            return BiomeType.JUNGLE;
        }

        // 炎热带
        if (humidity < -0.35) {
            return BiomeType.DESERT;
        }
        if (humidity < -0.05) {
            if (erosion < -0.15) {
                return BiomeType.ERODED_BADLANDS;
            }
            return variant > 0.45 ? BiomeType.WOODED_BADLANDS : BiomeType.BADLANDS;
        }
        if (humidity < 0.28) {
            return variant > 0.55 ? BiomeType.WINDSWEPT_SAVANNA : BiomeType.SAVANNA;
        }
        if (humidity < 0.55) {
            return BiomeType.SPARSE_JUNGLE;
        }
        if (altitude < 8.0 && humidity > 0.62 && temperature > 0.45) {
            return BiomeType.MANGROVE_SWAMP;
        }
        return variant > 0.60 ? BiomeType.BAMBOO_JUNGLE : BiomeType.JUNGLE;
    }
}
