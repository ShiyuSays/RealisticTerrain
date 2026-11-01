package com.says.realisticterrain.noise;

/**
 * 经典的三维 Perlin 噪声（Ken Perlin 的 improved noise）。
 *
 * <p>排列表由种子确定性地洗牌，同一个种子在任何服务端版本、任何线程上
 * 都会得到完全相同的结果 —— 这是地形可复现的基础。</p>
 *
 * <p>返回值大致落在 {@code [-1, 1]}，单层噪声实际范围约 {@code [-0.7, 0.7]}，
 * 上层使用 {@link OctaveNoise} 做归一化叠加。</p>
 */
public final class PerlinNoise {

    /** 12 个经典梯度向量，按 (x, y, z) 三连存放。 */
    private static final int[] GRAD = {
            1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1, 0,
            1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, -1,
            0, 1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1
    };

    private final int[] permutation = new int[512];

    public PerlinNoise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        SplitMix64 random = new SplitMix64(seed);
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }
        for (int i = 0; i < 512; i++) {
            permutation[i] = p[i & 255];
        }
    }

    /** 二维噪声。 */
    public double noise2(double x, double y) {
        int xi = fastFloor(x) & 255;
        int yi = fastFloor(y) & 255;
        double xf = x - fastFloor(x);
        double yf = y - fastFloor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = permutation[permutation[xi] + yi];
        int ab = permutation[permutation[xi] + yi + 1];
        int ba = permutation[permutation[xi + 1] + yi];
        int bb = permutation[permutation[xi + 1] + yi + 1];

        double x1 = lerp(v, grad2(aa, xf, yf), grad2(ab, xf, yf - 1));
        double x2 = lerp(v, grad2(ba, xf - 1, yf), grad2(bb, xf - 1, yf - 1));
        return lerp(u, x1, x2);
    }

    /** 三维噪声。 */
    public double noise3(double x, double y, double z) {
        int xi = fastFloor(x) & 255;
        int yi = fastFloor(y) & 255;
        int zi = fastFloor(z) & 255;
        double xf = x - fastFloor(x);
        double yf = y - fastFloor(y);
        double zf = z - fastFloor(z);

        double u = fade(xf);
        double v = fade(yf);
        double w = fade(zf);

        int a = permutation[xi] + yi;
        int aa = permutation[a] + zi;
        int ab = permutation[a + 1] + zi;
        int b = permutation[xi + 1] + yi;
        int ba = permutation[b] + zi;
        int bb = permutation[b + 1] + zi;

        return lerp(w,
                lerp(v,
                        lerp(u, grad3(permutation[aa], xf, yf, zf),
                                grad3(permutation[ba], xf - 1, yf, zf)),
                        lerp(u, grad3(permutation[ab], xf, yf - 1, zf),
                                grad3(permutation[bb], xf - 1, yf - 1, zf))),
                lerp(v,
                        lerp(u, grad3(permutation[aa + 1], xf, yf, zf - 1),
                                grad3(permutation[ba + 1], xf - 1, yf, zf - 1)),
                        lerp(u, grad3(permutation[ab + 1], xf, yf - 1, zf - 1),
                                grad3(permutation[bb + 1], xf - 1, yf - 1, zf - 1))));
    }

    private static double grad2(int hash, double x, double y) {
        int h = (hash % 12) * 3;
        return GRAD[h] * x + GRAD[h + 1] * y;
    }

    private static double grad3(int hash, double x, double y, double z) {
        int h = (hash % 12) * 3;
        return GRAD[h] * x + GRAD[h + 1] * y + GRAD[h + 2] * z;
    }

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}

