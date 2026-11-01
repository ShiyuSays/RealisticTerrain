package com.says.realisticterrain.noise;

/** 噪声与地形计算共用的小工具。 */
public final class NoiseMath {

    private NoiseMath() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /**
     * 标准 smoothstep：{@code edge0 < edge1} 时从 0 平滑升到 1；
     * 两个边界可以相等（此时退化为阶跃）。
     */
    public static double smoothstep(double edge0, double edge1, double value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0 : 1.0;
        }
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** 把值从 {@code [inMin, inMax]} 映射到 {@code [outMin, outMax]}。 */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        if (inMin == inMax) {
            return outMin;
        }
        double t = clamp((value - inMin) / (inMax - inMin), 0.0, 1.0);
        return outMin + t * (outMax - outMin);
    }

    public static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }
}

