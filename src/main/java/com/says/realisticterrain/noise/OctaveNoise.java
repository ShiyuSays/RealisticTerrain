package com.says.realisticterrain.noise;

/**
 * 分形叠加噪声：多个不同频率的 Perlin 噪声按 {@code persistence} 衰减叠加。
 *
 * <p>频段从 {@code baseFrequency} 开始，每层乘 {@code lacunarity}（默认 2）；
 * 振幅每层乘 {@code persistence}（默认 0.5），最后按振幅总和归一化，
 * 所以输出范围仍然约为 {@code [-1, 1]}。</p>
 */
public final class OctaveNoise {

    private final PerlinNoise[] layers;
    private final double[] frequencies;
    private final double[] amplitudes;
    private final double normalization;

    public OctaveNoise(long seed, int octaves, double persistence, double baseFrequency) {
        this(seed, octaves, persistence, baseFrequency, 2.0);
    }

    public OctaveNoise(long seed, int octaves, double persistence, double baseFrequency, double lacunarity) {
        if (octaves < 1) {
            throw new IllegalArgumentException("octaves must be >= 1");
        }
        this.layers = new PerlinNoise[octaves];
        this.frequencies = new double[octaves];
        this.amplitudes = new double[octaves];

        double amplitude = 1.0;
        double frequency = baseFrequency;
        double sum = 0.0;
        for (int i = 0; i < octaves; i++) {
            this.layers[i] = new PerlinNoise(SplitMix64.mix(seed, 0x51ED270BEF592C5DL + i));
            this.frequencies[i] = frequency;
            this.amplitudes[i] = amplitude;
            sum += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        this.normalization = 1.0 / sum;
    }

    public double noise2(double x, double z) {
        double sum = 0.0;
        for (int i = 0; i < layers.length; i++) {
            double f = frequencies[i];
            sum += layers[i].noise2(x * f, z * f) * amplitudes[i];
        }
        return sum * normalization;
    }

    public double noise3(double x, double y, double z) {
        double sum = 0.0;
        for (int i = 0; i < layers.length; i++) {
            double f = frequencies[i];
            sum += layers[i].noise3(x * f, y * f, z * f) * amplitudes[i];
        }
        return sum * normalization;
    }

    /**
     * 在 {@code [0, 1]} 内归一化的二维噪声，旧版地形里常用于"距离赤道/海洋程度"。
     */
    public double normalized2(double x, double z) {
        return NoiseMath.clamp((noise2(x, z) + 0.7) / 1.4, 0.0, 1.0);
    }
}

