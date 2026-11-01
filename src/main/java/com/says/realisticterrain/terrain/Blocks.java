package com.says.realisticterrain.terrain;

import org.bukkit.Material;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块名解析缓存。某个方块在当前服务端不存在时退回安全方块，
 * 避免整个生成器因为一个名字而崩溃。
 */
public final class Blocks {

    private static final ConcurrentHashMap<String, Material> CACHE = new ConcurrentHashMap<>();

    private Blocks() {
    }

    public static Material get(String name, Material fallback) {
        Material cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        Material resolved = Material.matchMaterial(name);
        if (resolved == null) {
            resolved = fallback;
        }
        CACHE.putIfAbsent(name, resolved);
        return resolved;
    }

    public static Material get(String name, String fallback) {
        return get(name, get(fallback, Material.STONE));
    }
}
