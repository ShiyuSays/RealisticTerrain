package com.says.realisticterrain.terrain;

import com.says.realisticterrain.biome.BiomeType;
import com.says.realisticterrain.noise.NoiseFactory;
import org.junit.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TerrainSamplerTest {

    private static final Settings SETTINGS = Settings.builder().build();
    private static final WorldProfile PROFILE = WorldProfile.of(-64, 320, SETTINGS);

    @Test
    public void terrainStaysInsideWorldAndIsDeterministic() {
        TerrainSampler a = new TerrainSampler(new NoiseFactory(20261101L), SETTINGS);
        TerrainSampler b = new TerrainSampler(new NoiseFactory(20261101L), SETTINGS);
        for (int x = -1200; x <= 1200; x += 80) {
            for (int z = -1200; z <= 1200; z += 80) {
                TerrainColumn first = a.columnAt(x, z, PROFILE);
                TerrainColumn second = b.columnAt(x, z, PROFILE);
                assertEquals(first.height(), second.height(), 0.000001);
                assertTrue("height NaN at " + x + "," + z, !Double.isNaN(first.height()));
                assertTrue(first.height() >= PROFILE.worldMin() + 2);
                assertTrue(first.height() <= PROFILE.terrainTop());
            }
        }
    }

    @Test
    public void terrainHasOceansLowlandsAndMountains() {
        TerrainSampler sampler = new TerrainSampler(new NoiseFactory(20261101L), SETTINGS);
        int samples = 0;
        int oceans = 0;
        int mountains = 0;
        int high = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        Map<BiomeType, Integer> biomes = new EnumMap<>(BiomeType.class);

        for (int x = -2000; x <= 2000; x += 24) {
            for (int z = -2000; z <= 2000; z += 24) {
                TerrainColumn column = sampler.columnAt(x, z, PROFILE);
                samples++;
                min = Math.min(min, column.height());
                max = Math.max(max, column.height());
                if (column.height() < PROFILE.seaLevel() - 1) {
                    oceans++;
                }
                if (column.biome().isMountain()) {
                    mountains++;
                }
                if (column.height() > 240) {
                    high++;
                }
                biomes.merge(column.biome(), 1, Integer::sum);
            }
        }

        System.out.println("terrain stats: min=" + min + " max=" + max
                + " ocean=" + (100.0 * oceans / samples) + "%"
                + " mountain=" + (100.0 * mountains / samples) + "%"
                + " high=" + high);
        for (Map.Entry<BiomeType, Integer> entry : biomes.entrySet()) {
            System.out.println("  " + entry.getKey().id() + ": " + entry.getValue());
        }

        assertTrue("ocean too rare", oceans > samples * 0.10);
        assertTrue("ocean too common", oceans < samples * 0.70);
        assertTrue("no mountains", mountains > samples * 0.02);
        assertTrue("terrain never gets high", high > 0);
        assertTrue("terrain max too low: " + max, max > 260);
        assertTrue("terrain min too high: " + min, min < 65);
        assertTrue("too few biome types: " + biomes.size(), biomes.size() >= 8);
        assertTrue(biomes.containsKey(BiomeType.OCEAN) || biomes.containsKey(BiomeType.DEEP_OCEAN));
        Set<BiomeType> land = EnumSet.noneOf(BiomeType.class);
        for (BiomeType type : biomes.keySet()) {
            if (!type.isOcean() && !type.isRiver()) {
                land.add(type);
            }
        }
        assertFalse("no land biomes", land.isEmpty());
    }

    @Test
    public void sparseCavesAreActuallySparser() {
        Settings settings = Settings.builder().build();
        CaveSampler caves = new CaveSampler(new NoiseFactory(7L));
        int sparse = 0;
        int dense = 0;
        int tunnels = 0;
        int caverns = 0;
        int total = 0;
        for (int x = -128; x <= 128; x += 4) {
            for (int z = -128; z <= 128; z += 4) {
                if (!caves.regionAllowed(x, z)) {
                    continue;
                }
                for (int y = -60; y <= 200; y += 4) {
                    total++;
                    double a = caves.tunnelA(x, y, z);
                    double b = caves.tunnelB(x, y, z);
                    double c = caves.cavern(x, y, z);
                    boolean tunnel = caves.isTunnel(a, b, 0.35);
                    boolean cavern = y < 60 && caves.isCavern(c, 0.35);
                    if (tunnel) {
                        tunnels++;
                    }
                    if (cavern) {
                        caverns++;
                    }
                    if (tunnel || cavern) {
                        sparse++;
                    }
                    if (caves.isTunnel(a, b, 1.0) || (y < 60 && caves.isCavern(c, 1.0))) {
                        dense++;
                    }
                }
            }
        }
        System.out.println("cave stats: sparse=" + sparse + " dense=" + dense
                + " tunnels=" + tunnels + " caverns=" + caverns + " total=" + total);
        assertTrue("caves should exist", sparse > 0);
        assertTrue("sparse must be less than dense", sparse < dense);
        assertTrue("sparse caves too common: " + (100.0 * sparse / total) + "%",
                sparse < total * 0.035);
    }
}
