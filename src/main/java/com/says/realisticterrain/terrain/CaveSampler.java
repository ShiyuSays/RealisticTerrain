package com.says.realisticterrain.terrain;

import com.says.realisticterrain.noise.NoiseFactory;
import com.says.realisticterrain.noise.OctaveNoise;

/**
 * 稀疏洞穴场。
 *
 * <p>和 1.18 原版“到处都是洞穴”不同，这里先用一个低频区域噪声圈定
 * “有洞穴的岩层”，再用两条窄噪声带相交生成隧道、用第三个噪声生成少量
 * 大厅。默认 {@code cave-density: 0.35} 时刻意收窄阈值，洞穴总量大约是
 * 常规密度的三分之一到二分之一，并且不会把地表挖成筛子。</p>
 */
public final class CaveSampler {

    private final OctaveNoise tunnelA;
    private final OctaveNoise tunnelB;
    private final OctaveNoise cavern;
    private final OctaveNoise region;
    private final OctaveNoise entrance;

    public CaveSampler(NoiseFactory noise) {
        this.tunnelA = noise.octaves("cave_tunnel_a", 2, 0.50, 0.0125);
        this.tunnelB = noise.octaves("cave_tunnel_b", 2, 0.50, 0.0125);
        this.cavern = noise.octaves("cave_cavern", 3, 0.50, 0.0075);
        this.region = noise.octaves("cave_region", 2, 0.50, 0.0017);
        this.entrance = noise.octaves("cave_entrance", 3, 0.50, 0.018);
    }

    public double tunnelA(int x, int y, int z) {
        return tunnelA.noise3(x, y * 2.05, z);
    }

    public double tunnelB(int x, int y, int z) {
        return tunnelB.noise3(x + 613.0, y * 2.05, z - 887.0);
    }

    public double cavern(int x, int y, int z) {
        return cavern.noise3(x, y * 1.30, z);
    }

    /** 低频的“洞穴富集区”掩码。 */
    public double regionAt(int x, int z) {
        return region.noise2(x, z);
    }

    /** 允许在地表附近开口的列。 */
    public boolean entranceAllowed(int x, int z) {
        return entrance.noise2(x, z) > 0.36;
    }

    public boolean regionAllowed(int x, int z) {
        return regionAt(x, z) > -0.18;
    }

    /**
     * 两条窄带同时接近 0 时形成隧道。
     *
     * @param density 0 ~ 1.5 的洞穴密度
     */
    public boolean isTunnel(double a, double b, double density) {
        double thickness = 0.070 * (0.42 + 0.58 * density);
        return Math.abs(a) < thickness && Math.abs(b) < thickness;
    }

    /**
     * 大型洞窟。阈值随密度线性变化，默认密度下非常稀有。
     */
    public boolean isCavern(double value, double density) {
        // 3 层 Perlin 归一化后的实际范围约 -0.58 ~ 0.58，所以阈值在 0.5 附近。
        double threshold = 0.490 - 0.030 * density;
        return value > threshold;
    }
}
