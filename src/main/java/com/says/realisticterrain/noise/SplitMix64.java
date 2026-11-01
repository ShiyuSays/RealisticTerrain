package com.says.realisticterrain.noise;

/**
 * 可复现的 SplitMix64 随机数生成器。
 *
 * <p>只依赖 {@code long} 运算，不依赖 {@link java.util.Random} 的实现细节，
 * 因此同一个种子在 Java 8 ~ Java 25 上产出的序列完全一致。</p>
 */
public final class SplitMix64 {

    private long state;

    public SplitMix64(long seed) {
        this.state = seed;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** 返回 {@code [0, bound)} 之间的整数。 */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long value = nextLong() >>> 1;
        return (int) (value % bound);
    }

    /** 返回 {@code [0, 1)} 之间的双精度浮点数。 */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** 返回 {@code [min, max)} 之间的双精度浮点数。 */
    public double nextDouble(double min, double max) {
        return min + nextDouble() * (max - min);
    }

    /** 混合两个种子，用于从世界种子派生互不相关的子噪声。 */
    public static long mix(long seed, long salt) {
        long z = seed + salt * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}

