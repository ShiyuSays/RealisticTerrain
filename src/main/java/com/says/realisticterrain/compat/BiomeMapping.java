package com.says.realisticterrain.compat;

import com.says.realisticterrain.biome.BiomeType;
import org.bukkit.block.Biome;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 内部群系 -> 当前服务端 Bukkit {@link Biome} 的反射映射。
 *
 * <p>1.18 到 26.x 之间群系名发生过多次改名，26.x 还把 Biome 从 enum
 * 改成了接口。这里按候选名依次查找公共静态字段，不依赖枚举的 ordinal，
 * 也不依赖任何 NMS 类。</p>
 */
public final class BiomeMapping {

    private final Map<BiomeType, Biome> mappings = new EnumMap<>(BiomeType.class);
    private final List<Biome> all;

    public BiomeMapping(Logger logger) {
        Set<Biome> unique = new LinkedHashSet<>();
        Biome fallback = resolve("PLAINS");
        for (BiomeType type : BiomeType.values()) {
            Biome biome = resolve(type.biomeNames());
            if (biome == null) {
                biome = fallback;
                if (fallback == null) {
                    throw new IllegalStateException("当前服务端连 PLAINS 群系都解析不到");
                }
                logger.warning("无法解析群系 " + type.name() + "（候选名 "
                        + String.join(", ", type.biomeNames()) + "），已退回 "
                        + fallback.getKey().getKey() + "。");
            }
            mappings.put(type, biome);
            unique.add(biome);
        }
        this.all = Collections.unmodifiableList(new ArrayList<>(unique));
    }

    public Biome get(BiomeType type) {
        Biome biome = mappings.get(type);
        return biome == null ? mappings.get(BiomeType.PLAINS) : biome;
    }

    public List<Biome> all() {
        return all;
    }

    private static Biome resolve(String... names) {
        for (String name : names) {
            Biome value = resolve(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Biome resolve(String name) {
        try {
            Field field = Biome.class.getField(name);
            Object value = field.get(null);
            if (value instanceof Biome) {
                return (Biome) value;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 该版本没有这个字段，继续尝试下一个候选名。
        }
        return null;
    }
}
