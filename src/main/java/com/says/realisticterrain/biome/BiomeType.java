package com.says.realisticterrain.biome;

/**
 * 生成器内部使用的生物群系表。
 *
 * <p>每个枚举的第一个名字是 1.18+ 的 Bukkit 群系字段名，后面是可以回退的
 * 旧名字/别名。运行期通过反射按顺序查找，所以 1.18 的枚举、
 * 1.21+ 的新群系以及 26.x 把 {@code Biome} 改成接口之后都能解析。</p>
 */
public enum BiomeType {

    OCEAN("ocean", SurfaceProfile.OCEAN, false, false, false, false, false,
            "OCEAN"),
    DEEP_OCEAN("deep_ocean", SurfaceProfile.DEEP, false, false, false, false, false,
            "DEEP_OCEAN"),
    FROZEN_OCEAN("frozen_ocean", SurfaceProfile.DEEP, false, false, false, true, true,
            "FROZEN_OCEAN", "DEEP_FROZEN_OCEAN"),
    WARM_OCEAN("warm_ocean", SurfaceProfile.OCEAN, false, false, false, false, false,
            "WARM_OCEAN"),
    LUKEWARM_OCEAN("lukewarm_ocean", SurfaceProfile.OCEAN, false, false, false, false, false,
            "LUKEWARM_OCEAN"),
    RIVER("river", SurfaceProfile.RIVER, false, true, false, false, false,
            "RIVER"),
    FROZEN_RIVER("frozen_river", SurfaceProfile.RIVER, false, true, false, true, false,
            "FROZEN_RIVER"),

    BEACH("beach", SurfaceProfile.BEACH, false, false, true, false, false,
            "BEACH"),
    SNOWY_BEACH("snowy_beach", SurfaceProfile.SNOWY_BEACH, false, false, true, true, false,
            "SNOWY_BEACH"),
    STONY_SHORE("stony_shore", SurfaceProfile.STONY, false, false, true, false, false,
            "STONY_SHORE", "STONE_SHORE"),

    PLAINS("plains", SurfaceProfile.GRASS, false, false, false, false, false,
            "PLAINS"),
    SUNFLOWER_PLAINS("sunflower_plains", SurfaceProfile.GRASS, false, false, false, false, false,
            "SUNFLOWER_PLAINS"),
    FOREST("forest", SurfaceProfile.GRASS, false, false, false, false, false,
            "FOREST"),
    FLOWER_FOREST("flower_forest", SurfaceProfile.GRASS, false, false, false, false, false,
            "FLOWER_FOREST"),
    BIRCH_FOREST("birch_forest", SurfaceProfile.GRASS, false, false, false, false, false,
            "BIRCH_FOREST"),
    DARK_FOREST("dark_forest", SurfaceProfile.GRASS, false, false, false, false, false,
            "DARK_FOREST", "ROOFED_FOREST"),
    MEADOW("meadow", SurfaceProfile.MEADOW, false, false, false, false, true,
            "MEADOW"),
    GROVE("grove", SurfaceProfile.SNOWY, false, false, false, true, true,
            "GROVE"),
    CHERRY_GROVE("cherry_grove", SurfaceProfile.GRASS, false, false, false, false, true,
            "CHERRY_GROVE", "BIRCH_FOREST"),

    TAIGA("taiga", SurfaceProfile.TAIGA, false, false, false, false, false,
            "TAIGA"),
    OLD_GROWTH_PINE_TAIGA("old_growth_pine_taiga", SurfaceProfile.TAIGA, false, false, false,
            false, false, "OLD_GROWTH_PINE_TAIGA", "GIANT_TREE_TAIGA"),
    OLD_GROWTH_SPRUCE_TAIGA("old_growth_spruce_taiga", SurfaceProfile.TAIGA, false, false, false,
            false, false, "OLD_GROWTH_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA"),
    SNOWY_TAIGA("snowy_taiga", SurfaceProfile.SNOWY, false, false, false, true, false,
            "SNOWY_TAIGA"),

    SNOWY_PLAINS("snowy_plains", SurfaceProfile.SNOWY, false, false, false, true, false,
            "SNOWY_PLAINS", "SNOWY_TUNDRA"),
    ICE_SPIKES("ice_spikes", SurfaceProfile.SNOWY, false, false, false, true, false,
            "ICE_SPIKES"),

    DESERT("desert", SurfaceProfile.SANDY, false, false, false, false, false,
            "DESERT"),
    BADLANDS("badlands", SurfaceProfile.TERRACOTTA, false, false, false, false, true,
            "BADLANDS", "MESA"),
    ERODED_BADLANDS("eroded_badlands", SurfaceProfile.TERRACOTTA, false, false, false, false,
            true, "ERODED_BADLANDS", "MESA_PLATEAU_F"),
    WOODED_BADLANDS("wooded_badlands", SurfaceProfile.TERRACOTTA, false, false, false, false,
            true, "WOODED_BADLANDS", "WOODED_BADLANDS_PLATEAU"),

    SAVANNA("savanna", SurfaceProfile.SAVANNA, false, false, false, false, true,
            "SAVANNA"),
    SAVANNA_PLATEAU("savanna_plateau", SurfaceProfile.SAVANNA, false, false, false, false,
            true, "SAVANNA_PLATEAU"),
    WINDSWEPT_SAVANNA("windswept_savanna", SurfaceProfile.SAVANNA, false, false, false, false,
            true, "WINDSWEPT_SAVANNA", "SHATTERED_SAVANNA"),

    JUNGLE("jungle", SurfaceProfile.JUNGLE, false, false, false, false, true,
            "JUNGLE"),
    SPARSE_JUNGLE("sparse_jungle", SurfaceProfile.JUNGLE, false, false, false, false, true,
            "SPARSE_JUNGLE", "JUNGLE_EDGE"),
    BAMBOO_JUNGLE("bamboo_jungle", SurfaceProfile.JUNGLE, false, false, false, false, true,
            "BAMBOO_JUNGLE"),

    SWAMP("swamp", SurfaceProfile.SWAMP, false, false, false, false, false,
            "SWAMP", "SWAMPLAND"),
    MANGROVE_SWAMP("mangrove_swamp", SurfaceProfile.SWAMP, false, false, false, false, false,
            "MANGROVE_SWAMP", "SWAMP"),

    MUSHROOM_FIELDS("mushroom_fields", SurfaceProfile.MYCELIUM, false, false, false, false,
            true, "MUSHROOM_FIELDS", "MUSHROOM_ISLAND"),

    WINDSWEPT_HILLS("windswept_hills", SurfaceProfile.MOUNTAIN, true, false, false, false,
            false, "WINDSWEPT_HILLS", "MOUNTAINS"),
    WINDSWEPT_FOREST("windswept_forest", SurfaceProfile.MOUNTAIN, true, false, false, false,
            false, "WINDSWEPT_FOREST", "WOODED_MOUNTAINS"),
    WINDSWEPT_GRAVELLY_HILLS("windswept_gravelly_hills", SurfaceProfile.GRAVELLY, true, false,
            false, false, false, "WINDSWEPT_GRAVELLY_HILLS", "GRAVELLY_MOUNTAINS"),

    JAGGED_PEAKS("jagged_peaks", SurfaceProfile.GRAVELLY, true, false, false, true, true,
            "JAGGED_PEAKS"),
    FROZEN_PEAKS("frozen_peaks", SurfaceProfile.MOUNTAIN, true, false, false, true, true,
            "FROZEN_PEAKS"),
    STONY_PEAKS("stony_peaks", SurfaceProfile.MOUNTAIN, true, false, false, false, true,
            "STONY_PEAKS"),
    SNOWY_SLOPES("snowy_slopes", SurfaceProfile.SNOWY, true, false, false, true, true,
            "SNOWY_SLOPES");

    private final String id;
    private final SurfaceProfile profile;
    private final boolean mountain;
    private final boolean river;
    private final boolean beach;
    private final boolean frozen;
    private final boolean snowy;
    private final String[] biomeNames;

    BiomeType(String id, SurfaceProfile profile, boolean mountain, boolean river, boolean beach,
              boolean frozen, boolean snowy, String... biomeNames) {
        this.id = id;
        this.profile = profile;
        this.mountain = mountain;
        this.river = river;
        this.beach = beach;
        this.frozen = frozen;
        this.snowy = snowy;
        this.biomeNames = biomeNames;
    }

    public String id() {
        return id;
    }

    public SurfaceProfile profile() {
        return profile;
    }

    public boolean isMountain() {
        return mountain;
    }

    public boolean isRiver() {
        return river;
    }

    public boolean isBeach() {
        return beach;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isSnowy() {
        return snowy || profile.snow();
    }

    public boolean isOcean() {
        return this == OCEAN || this == DEEP_OCEAN || this == FROZEN_OCEAN
                || this == WARM_OCEAN || this == LUKEWARM_OCEAN;
    }

    public boolean isBadlands() {
        return this == BADLANDS || this == ERODED_BADLANDS || this == WOODED_BADLANDS;
    }

    public String[] biomeNames() {
        return biomeNames.clone();
    }
}
