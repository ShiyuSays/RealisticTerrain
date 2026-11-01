package com.says.realisticterrain.noise;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按世界种子创建并缓存噪声组。
 *
 * <p>实例是不可变的（内部 map 只做幂等缓存），可以被多个区域线程同时使用。</p>
 */
public final class NoiseFactory {

    private final long seed;
    private final ConcurrentHashMap<String, OctaveNoise> cache = new ConcurrentHashMap<>();

    public NoiseFactory(long seed) {
        this.seed = seed;
    }

    public long seed() {
        return seed;
    }

    /**
     * 取得一组分形噪声。相同参数只会创建一次。
     *
     * @param key           名称，参与种子派生，保证不同用途的噪声互不相关
     * @param octaves       叠加层数
     * @param persistence   每层振幅衰减
     * @param baseFrequency 基础频率（1 / 特征尺度）
     */
    public OctaveNoise octaves(String key, int octaves, double persistence, double baseFrequency) {
        String cacheKey = key + '|' + octaves + '|' + persistence + '|' + baseFrequency;
        return cache.computeIfAbsent(cacheKey, ignored ->
                new OctaveNoise(SplitMix64.mix(seed, key.hashCode()), octaves, persistence, baseFrequency));
    }

    /** 单层噪声，常用于只需要一个大尺度场的场合。 */
    public OctaveNoise single(String key, double baseFrequency) {
        return octaves(key, 1, 0.5, baseFrequency);
    }
}

