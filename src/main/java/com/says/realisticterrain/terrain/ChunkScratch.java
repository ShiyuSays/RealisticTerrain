package com.says.realisticterrain.terrain;

/**
 * 单个区块生成时复用的临时数组。每个线程各自持有一份，避免高频分配。
 *
 * <p>所有 Y 索引都相对世界最低点 {@code minY}，这样 1.18 的负坐标和自定义
 * 高度世界都能共用同一套数组。</p>
 */
final class ChunkScratch {

    static final int GRID = 5;

    final TerrainColumn[] columns = new TerrainColumn[256];
    final int[] tops = new int[256];
    final boolean[] caveAllowed = new boolean[256];
    final boolean[] caveEntrance = new boolean[256];

    final double[] gridHeight = new double[GRID * GRID];
    final double[] gridMountain = new double[GRID * GRID];

    double[] density;
    double[] caveA;
    double[] caveB;
    double[] caveC;
    int densityLevels;
    int caveLevels;
    int minY;
    int maxY;

    void ensure(int minY, int maxY) {
        int needDensity = ((maxY - minY + 7) >> 3) + 1;
        int needCave = ((maxY - minY + 3) >> 2) + 1;
        if (density == null || this.minY != minY || this.maxY != maxY
                || densityLevels != needDensity) {
            this.minY = minY;
            this.maxY = maxY;
            this.densityLevels = needDensity;
            this.caveLevels = needCave;
            this.density = new double[GRID * GRID * needDensity];
            this.caveA = new double[GRID * GRID * needCave];
            this.caveB = new double[GRID * GRID * needCave];
            this.caveC = new double[GRID * GRID * needCave];
        }
    }

    int densityIndex(int gx, int gy, int gz) {
        return (gx * GRID + gz) * densityLevels + gy;
    }

    int caveIndex(int gx, int gy, int gz) {
        return (gx * GRID + gz) * caveLevels + gy;
    }

    double densityAt(int lx, int y, int lz) {
        int relY = y - minY;
        if (relY < 0) {
            relY = 0;
        }
        int gy = relY >> 3;
        if (gy > densityLevels - 2) {
            gy = Math.max(0, densityLevels - 2);
        }
        double ty = (relY - (gy << 3)) * 0.125;
        if (ty < 0.0) {
            ty = 0.0;
        } else if (ty > 1.0) {
            ty = 1.0;
        }

        int gx = lx >> 2;
        int gz = lz >> 2;
        double tx = (lx & 3) * 0.25;
        double tz = (lz & 3) * 0.25;

        int base = densityIndex(gx, gy, gz);
        int stepY = 1;
        int stepZ = densityLevels;
        int stepX = GRID * densityLevels;
        double d000 = density[base];
        double d001 = density[base + stepZ];
        double d010 = density[base + stepY];
        double d011 = density[base + stepY + stepZ];
        double d100 = density[base + stepX];
        double d101 = density[base + stepX + stepZ];
        double d110 = density[base + stepX + stepY];
        double d111 = density[base + stepX + stepY + stepZ];

        double x00 = d000 + (d100 - d000) * tx;
        double x01 = d001 + (d101 - d001) * tx;
        double x10 = d010 + (d110 - d010) * tx;
        double x11 = d011 + (d111 - d011) * tx;
        double y0 = x00 + (x10 - x00) * ty;
        double y1 = x01 + (x11 - x01) * ty;
        return y0 + (y1 - y0) * tz;
    }

    double caveAAt(int lx, int y, int lz) {
        return caveAt(caveA, lx, y, lz);
    }

    double caveBAt(int lx, int y, int lz) {
        return caveAt(caveB, lx, y, lz);
    }

    double caveCAt(int lx, int y, int lz) {
        return caveAt(caveC, lx, y, lz);
    }

    private double caveAt(double[] values, int lx, int y, int lz) {
        int relY = y - minY;
        if (relY < 0) {
            relY = 0;
        }
        int gy = relY >> 2;
        if (gy > caveLevels - 2) {
            gy = Math.max(0, caveLevels - 2);
        }
        double ty = (relY - (gy << 2)) * 0.25;
        if (ty < 0.0) {
            ty = 0.0;
        } else if (ty > 1.0) {
            ty = 1.0;
        }

        int gx = lx >> 2;
        int gz = lz >> 2;
        double tx = (lx & 3) * 0.25;
        double tz = (lz & 3) * 0.25;

        int base = caveIndex(gx, gy, gz);
        int stepZ = caveLevels;
        int stepX = GRID * caveLevels;
        double c000 = values[base];
        double c001 = values[base + stepZ];
        double c010 = values[base + 1];
        double c011 = values[base + 1 + stepZ];
        double c100 = values[base + stepX];
        double c101 = values[base + stepX + stepZ];
        double c110 = values[base + stepX + 1];
        double c111 = values[base + stepX + 1 + stepZ];

        double x00 = c000 + (c100 - c000) * tx;
        double x01 = c001 + (c101 - c001) * tx;
        double x10 = c010 + (c110 - c010) * tx;
        double x11 = c011 + (c111 - c011) * tx;
        double y0 = x00 + (x10 - x00) * ty;
        double y1 = x01 + (x11 - x01) * ty;
        return y0 + (y1 - y0) * tz;
    }
}
