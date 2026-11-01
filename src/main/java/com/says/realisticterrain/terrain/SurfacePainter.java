package com.says.realisticterrain.terrain;

import com.says.realisticterrain.biome.BiomeType;
import com.says.realisticterrain.biome.SurfaceProfile;
import com.says.realisticterrain.noise.NoiseFactory;
import com.says.realisticterrain.noise.OctaveNoise;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.EnumMap;
import java.util.Map;

/**
 * 按群系、海拔、坡度和水下状态铺设地表。
 *
 * <p>顺序很重要：先判断水下/沙滩，再判断高山裸岩和雪线，最后才铺群系
 * 模板。这样同一套模板可以在不同海拔呈现草甸、碎石坡和雪峰。</p>
 */
public final class SurfacePainter {

    private static final Material AIR = Material.AIR;
    private static final Material STONE = Material.STONE;
    private static final Material DEEPSLATE = Material.DEEPSLATE;
    private static final Material TUFF = Material.TUFF;
    private static final Material GRAVEL = Material.GRAVEL;
    private static final Material SAND = Material.SAND;
    private static final Material RED_SAND = Material.RED_SAND;
    private static final Material SANDSTONE = Material.SANDSTONE;
    private static final Material CLAY = Material.CLAY;
    private static final Material SNOW = Material.SNOW;
    private static final Material SNOW_BLOCK = Material.SNOW_BLOCK;
    private static final Material COARSE_DIRT = Material.COARSE_DIRT;
    private static final Material PODZOL = Material.PODZOL;
    private static final Material MYCELIUM = Material.MYCELIUM;
    private static final Material TERRACOTTA = Material.TERRACOTTA;
    private static final Material WHITE_TERRACOTTA = Material.WHITE_TERRACOTTA;
    private static final Material ORANGE_TERRACOTTA = Material.ORANGE_TERRACOTTA;
    private static final Material YELLOW_TERRACOTTA = Material.YELLOW_TERRACOTTA;
    private static final Material BROWN_TERRACOTTA = Material.BROWN_TERRACOTTA;
    private static final Material RED_TERRACOTTA = Material.RED_TERRACOTTA;
    private static final Material LIGHT_GRAY_TERRACOTTA = Material.LIGHT_GRAY_TERRACOTTA;

    private final Map<SurfaceProfile, ProfileBlocks> profiles = new EnumMap<>(SurfaceProfile.class);
    private final OctaveNoise patchNoise;

    public SurfacePainter(NoiseFactory noise) {
        this.patchNoise = noise.octaves("surface_patch", 2, 0.5, 0.055);
        for (SurfaceProfile profile : SurfaceProfile.values()) {
            profiles.put(profile, new ProfileBlocks(
                    Blocks.get(profile.top(), STONE),
                    Blocks.get(profile.filler(), Material.DIRT),
                    Blocks.get(profile.underwaterTop(), SAND),
                    Blocks.get(profile.underwaterFiller(), GRAVEL)));
        }
    }

    public void paint(ChunkData data, int localX, int localZ, int worldX, int worldZ,
                      int top, TerrainColumn column, int slope, WorldProfile profile) {
        if (top < profile.worldMin() + 1 || top >= profile.worldMax()) {
            return;
        }
        BiomeType biome = column.biome();
        SurfaceProfile surface = biome.profile();
        ProfileBlocks blocks = profiles.get(surface);
        if (blocks == null) {
            blocks = profiles.get(SurfaceProfile.GRASS);
        }

        boolean underwater = top < profile.seaLevel();
        int depth = surface.depth();
        Material topMaterial;
        Material fillerMaterial;

        if (underwater) {
            topMaterial = blocks.underwaterTop;
            fillerMaterial = blocks.underwaterFiller;
            depth = Math.min(depth, 3);
            // 浅海/河口的沙质海底更亮一些。
            if (top >= profile.seaLevel() - 4 && surface != SurfaceProfile.DEEP) {
                topMaterial = SAND;
            }
        } else {
            topMaterial = blocks.top;
            fillerMaterial = blocks.filler;
        }

        double patch = patchNoise.noise2(worldX, worldZ);

        // 沙漠/恶地的沙层更厚；针叶林和黑森林夹灰化土；热带草原夹粗泥。
        if (!underwater) {
            if (surface == SurfaceProfile.SANDY) {
                depth = 4;
            } else if (surface == SurfaceProfile.TAIGA && patch > 0.30) {
                topMaterial = PODZOL;
            } else if (surface == SurfaceProfile.SAVANNA && patch > 0.42) {
                topMaterial = COARSE_DIRT;
            } else if (surface == SurfaceProfile.MYCELIUM) {
                topMaterial = MYCELIUM;
            }
        }

        // 不同山峰的雪线略有差异：石峰更干、雪峰更低，形成岩石与积雪交错的山顶。
        int effectiveSnowLine = profile.snowLine();
        if (biome == BiomeType.STONY_PEAKS || biome == BiomeType.WINDSWEPT_GRAVELLY_HILLS) {
            effectiveSnowLine += 26;
        } else if (biome == BiomeType.JAGGED_PEAKS || biome == BiomeType.FROZEN_PEAKS) {
            effectiveSnowLine -= 8;
        } else if (biome == BiomeType.SNOWY_SLOPES) {
            effectiveSnowLine -= 16;
        }

        // 陡坡/高山裸岩：不再保留草皮。
        boolean steep = slope >= 5 || (column.erosion() < -0.35 && column.mountain() > 0.20);
        if (!underwater && (steep || top >= profile.rockLine())) {
            topMaterial = STONE;
            fillerMaterial = STONE;
            depth = 1;
            if (surface == SurfaceProfile.GRAVELLY && top < profile.snowLine()) {
                topMaterial = GRAVEL;
            }
        }

        // 高山雪线：整块雪，外加上面一层薄雪；陡峭岩壁偶尔露出裸岩。
        boolean snowLine = !underwater && top >= effectiveSnowLine;
        if (snowLine && steep && patch < -0.08) {
            snowLine = false;
        }
        if (snowLine) {
            topMaterial = SNOW_BLOCK;
            fillerMaterial = STONE;
            depth = 1;
        }

        // 恶地彩色条带。
        if (!underwater && surface.terracottaBands()) {
            topMaterial = RED_SAND;
            fillerMaterial = TERRACOTTA;
            depth = 3;
        }

        // 如果洞穴刚好切掉了这一列，最高实心方块可能不是原来的地表，
        // 此时仍然按“石头”处理，避免把草铺到洞穴顶板上。
        Material currentTop = data.getType(localX, top, localZ);
        if (!isGround(currentTop)) {
            return;
        }

        data.setBlock(localX, top, localZ, topMaterial);
        for (int i = 1; i <= depth; i++) {
            int y = top - i;
            if (y <= profile.worldMin()) {
                break;
            }
            Material current = data.getType(localX, y, localZ);
            if (!isGround(current)) {
                continue;
            }
            data.setBlock(localX, y, localZ, fillerMaterial);
        }

        if (!underwater && surface.terracottaBands()) {
            paintBands(data, localX, localZ, top - depth - 1, patch, profile);
        }

        // 雪层：雪原、雪林，以及雪线附近的山地。
        boolean snowyBiome = surface.snow() && !underwater;
        boolean justBelowSnow = snowyBiome && top >= effectiveSnowLine - 8;
        if ((snowyBiome || justBelowSnow) && top + 1 < profile.worldMax()
                && data.getType(localX, top + 1, localZ) == AIR) {
            data.setBlock(localX, top + 1, localZ, SNOW);
        }

        // 冰面由 RealisticWorld 在水体填充后统一处理；这里只处理雪。
    }

    private static void paintBands(ChunkData data, int localX, int localZ, int startY,
                                   double patch, WorldProfile profile) {
        for (int i = 0; i < 9; i++) {
            int y = startY - i;
            if (y <= profile.worldMin() + 1 || y >= profile.worldMax()) {
                break;
            }
            Material current = data.getType(localX, y, localZ);
            if (!isGround(current)) {
                continue;
            }
            data.setBlock(localX, y, localZ, bandColor(y, patch));
        }
    }

    private static Material bandColor(int y, double patch) {
        int band = Math.floorMod(y * 2 + (int) Math.round(patch * 3.0), 7);
        switch (band) {
            case 0:
                return WHITE_TERRACOTTA;
            case 1:
                return ORANGE_TERRACOTTA;
            case 2:
                return YELLOW_TERRACOTTA;
            case 3:
                return BROWN_TERRACOTTA;
            case 4:
                return RED_TERRACOTTA;
            case 5:
                return LIGHT_GRAY_TERRACOTTA;
            default:
                return TERRACOTTA;
        }
    }

    private static boolean isGround(Material material) {
        return material == STONE || material == DEEPSLATE || material == TUFF
                || material == Material.ANDESITE || material == Material.DIORITE
                || material == Material.GRANITE || material == Material.DIRT
                || material == Material.GRAVEL || material == Material.SAND
                || material == Material.RED_SAND || material == Material.SANDSTONE
                || material == Material.CLAY || material == Material.TERRACOTTA;
    }

    private static final class ProfileBlocks {
        private final Material top;
        private final Material filler;
        private final Material underwaterTop;
        private final Material underwaterFiller;

        private ProfileBlocks(Material top, Material filler, Material underwaterTop,
                              Material underwaterFiller) {
            this.top = top;
            this.filler = filler;
            this.underwaterTop = underwaterTop;
            this.underwaterFiller = underwaterFiller;
        }
    }
}
