package com.says.realisticterrain.biome;

/**
 * 地表材质模板。
 *
 * <p>方块名以字符串保存，由地形层统一解析为 {@code Material}，这样群系/地表
 * 逻辑可以脱离服务端单独测试，也不会因为某个版本移除方块而加载失败。</p>
 */
public enum SurfaceProfile {

    /** 普通温带草地。 */
    GRASS("GRASS_BLOCK", "DIRT", "SAND", "GRAVEL", 3, false, false),

    /** 雪地：草方块 + 泥土，上面额外铺一层雪。 */
    SNOWY("GRASS_BLOCK", "DIRT", "SAND", "GRAVEL", 3, true, false),

    /** 针叶林：草地，夹杂灰化土。 */
    TAIGA("GRASS_BLOCK", "PODZOL", "SAND", "GRAVEL", 3, false, false),

    /** 沙滩。 */
    BEACH("SAND", "SANDSTONE", "SAND", "SANDSTONE", 3, false, false),

    /** 雪沙滩。 */
    SNOWY_BEACH("SAND", "SANDSTONE", "SAND", "SANDSTONE", 3, true, false),

    /** 石岸 / 陡峭海岸。 */
    STONY("STONE", "GRAVEL", "GRAVEL", "STONE", 2, false, false),

    /** 沙漠：沙子 + 砂岩。 */
    SANDY("SAND", "SANDSTONE", "SAND", "SANDSTONE", 4, false, false),

    /** 恶地：红沙 + 陶瓦，下面有彩色条带。 */
    TERRACOTTA("RED_SAND", "TERRACOTTA", "RED_SAND", "TERRACOTTA", 3, false, true),

    /** 高山裸岩：草很薄，很快露出石头。 */
    MOUNTAIN("STONE", "STONE", "GRAVEL", "STONE", 1, false, false),

    /** 砂砾山地。 */
    GRAVELLY("GRAVEL", "STONE", "GRAVEL", "STONE", 2, false, false),

    /** 海洋底部：沙与砂砾。 */
    OCEAN("SAND", "GRAVEL", "SAND", "GRAVEL", 3, false, false),

    /** 深海底部：砂砾与石头。 */
    DEEP("GRAVEL", "STONE", "GRAVEL", "STONE", 3, false, false),

    /** 河床。 */
    RIVER("SAND", "GRAVEL", "SAND", "GRAVEL", 3, false, false),

    /** 沼泽：草 + 黏土。 */
    SWAMP("GRASS_BLOCK", "DIRT", "CLAY", "GRAVEL", 3, false, false),

    /** 蘑菇岛。 */
    MYCELIUM("MYCELIUM", "DIRT", "SAND", "GRAVEL", 3, false, false),

    /** 热带草原：草 + 粗泥。 */
    SAVANNA("GRASS_BLOCK", "COARSE_DIRT", "SAND", "GRAVEL", 3, false, false),

    /** 丛林。 */
    JUNGLE("GRASS_BLOCK", "DIRT", "SAND", "GRAVEL", 3, false, false),

    /** 山地草甸。 */
    MEADOW("GRASS_BLOCK", "DIRT", "SAND", "GRAVEL", 3, false, false);

    private final String top;
    private final String filler;
    private final String underwaterTop;
    private final String underwaterFiller;
    private final int depth;
    private final boolean snow;
    private final boolean terracottaBands;

    SurfaceProfile(String top, String filler, String underwaterTop, String underwaterFiller,
                   int depth, boolean snow, boolean terracottaBands) {
        this.top = top;
        this.filler = filler;
        this.underwaterTop = underwaterTop;
        this.underwaterFiller = underwaterFiller;
        this.depth = depth;
        this.snow = snow;
        this.terracottaBands = terracottaBands;
    }

    public String top() {
        return top;
    }

    public String filler() {
        return filler;
    }

    public String underwaterTop() {
        return underwaterTop;
    }

    public String underwaterFiller() {
        return underwaterFiller;
    }

    /** 表层厚度（不含最上面一格）。 */
    public int depth() {
        return depth;
    }

    public boolean snow() {
        return snow;
    }

    public boolean terracottaBands() {
        return terracottaBands;
    }
}
