package com.says.realisticterrain.terrain;

import org.bukkit.generator.WorldInfo;

/**
 * 世界高度参数的适配层。
 *
 * <p>配置里的海平面、最高地形都是“绝对 Y”。这里根据当前世界的
 * {@code minHeight / maxHeight} 做一次钳制，保证同一个 jar 用在
 * 1.18 默认世界（-64 ~ 320）或自定义高度世界时都不会越界。</p>
 */
public final class WorldProfile {

    private final int worldMin;
    private final int worldMax;
    private final int seaLevel;
    private final int terrainTop;
    private final int bedrockDepth;
    private final int snowLine;
    private final int rockLine;

    private WorldProfile(int worldMin, int worldMax, int seaLevel, int terrainTop, int bedrockDepth) {
        this.worldMin = worldMin;
        this.worldMax = worldMax;
        this.seaLevel = seaLevel;
        this.terrainTop = terrainTop;
        this.bedrockDepth = bedrockDepth;
        this.snowLine = Math.min(terrainTop - 8, seaLevel + 170);
        this.rockLine = Math.min(terrainTop - 24, seaLevel + 112);
    }

    public static WorldProfile of(WorldInfo info, Settings settings) {
        return of(info.getMinHeight(), info.getMaxHeight(), settings);
    }

    /** 纯数值版本，方便在不启动服务端的情况下做单元测试。 */
    public static WorldProfile of(int min, int max, Settings settings) {
        // 顶部留 16 格给树木、建筑和天空；1.18 默认世界得到 304。
        int top = Math.min(settings.maxTerrainY(), max - 16);
        top = Math.max(top, min + 64);
        if (top > max - 1) {
            top = max - 1;
        }

        int sea = clamp(settings.seaLevel(), min + 16, top - 32);
        if (sea > top - 24) {
            sea = top - 24;
        }
        int bedrock = clamp(settings.bedrockDepth(), 1, 5);
        bedrock = Math.min(bedrock, Math.max(1, sea - min - 8));
        return new WorldProfile(min, max, sea, top, bedrock);
    }

    public int worldMin() {
        return worldMin;
    }

    public int worldMax() {
        return worldMax;
    }

    public int seaLevel() {
        return seaLevel;
    }

    public int terrainTop() {
        return terrainTop;
    }

    public int bedrockDepth() {
        return bedrockDepth;
    }

    /** 高山雪线（绝对 Y）。 */
    public int snowLine() {
        return snowLine;
    }

    /** 高山岩石线（绝对 Y）。 */
    public int rockLine() {
        return rockLine;
    }

    /** 地形填充的最低一层（基岩之上）。 */
    public int floor() {
        return worldMin + 1;
    }

    public int heightRange() {
        return terrainTop - worldMin;
    }

    private static int clamp(int value, int min, int max) {
        if (min > max) {
            return min;
        }
        return value < min ? min : Math.min(value, max);
    }
}
