package com.says.realisticterrain.terrain;

import com.says.realisticterrain.biome.BiomeType;

/**
 * 一个方块列的地形采样结果。不可变，可安全地跨线程传递。
 */
public final class TerrainColumn {

    private final int x;
    private final int z;
    private final double height;
    private final BiomeType biome;
    private final double continent;
    private final double erosion;
    private final double mountain;
    private final double river;
    private final double temperature;
    private final double humidity;
    private final double ocean;
    private final double ridge;
    private final double detail;

    public TerrainColumn(int x, int z, double height, BiomeType biome, double continent,
                         double erosion, double mountain, double river, double temperature,
                         double humidity, double ocean, double ridge, double detail) {
        this.x = x;
        this.z = z;
        this.height = height;
        this.biome = biome;
        this.continent = continent;
        this.erosion = erosion;
        this.mountain = mountain;
        this.river = river;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ocean = ocean;
        this.ridge = ridge;
        this.detail = detail;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    /** 地表最高实心方块的绝对 Y（未取整）。 */
    public double height() {
        return height;
    }

    public int surfaceY() {
        return (int) Math.round(height);
    }

    public BiomeType biome() {
        return biome;
    }

    public double continent() {
        return continent;
    }

    public double erosion() {
        return erosion;
    }

    /** 0 ~ 1 的山地强度。 */
    public double mountain() {
        return mountain;
    }

    /** 0 ~ 1 的河流强度。 */
    public double river() {
        return river;
    }

    public double temperature() {
        return temperature;
    }

    public double humidity() {
        return humidity;
    }

    /** 0 ~ 1，1 表示深海。 */
    public double ocean() {
        return ocean;
    }

    public double ridge() {
        return ridge;
    }

    public double detail() {
        return detail;
    }

    @Override
    public String toString() {
        return "TerrainColumn{" + x + "," + z + " y=" + String.format("%.1f", height)
                + " " + biome.id() + "}";
    }
}
