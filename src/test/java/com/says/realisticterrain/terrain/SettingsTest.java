package com.says.realisticterrain.terrain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsTest {

    @Test
    public void defaultsAreSane() {
        Settings settings = Settings.builder().build();
        assertEquals(96, settings.seaLevel());
        assertEquals(304, settings.maxTerrainY());
        assertEquals(0.35, settings.caveDensity(), 0.0001);
        assertTrue(settings.ores());
        assertTrue(settings.vanillaDecoration());
    }

    @Test
    public void valuesAreClamped() {
        Settings settings = Settings.builder()
                .seaLevel(-500)
                .maxTerrainY(10)
                .bedrockDepth(99)
                .heightScale(-2)
                .caveDensity(9)
                .oreDensity(-3)
                .build();
        assertEquals(0, settings.seaLevel());
        assertTrue(settings.maxTerrainY() >= 128);
        assertEquals(5, settings.bedrockDepth());
        assertEquals(0.25, settings.heightScale(), 0.0001);
        assertEquals(1.5, settings.caveDensity(), 0.0001);
        assertEquals(0.0, settings.oreDensity(), 0.0001);
    }

    @Test
    public void worldProfileMatchesEighteenDefaults() {
        WorldProfile profile = WorldProfile.of(-64, 320, Settings.builder().build());
        assertEquals(-64, profile.worldMin());
        assertEquals(320, profile.worldMax());
        assertEquals(96, profile.seaLevel());
        assertEquals(304, profile.terrainTop());
        assertEquals(266, profile.snowLine());
        assertEquals(208, profile.rockLine());
    }

    @Test
    public void worldProfileClampsToSmallWorld() {
        WorldProfile profile = WorldProfile.of(0, 256, Settings.builder().build());
        assertEquals(240, profile.terrainTop());
        assertTrue(profile.seaLevel() < profile.terrainTop());
        assertTrue(profile.snowLine() < profile.terrainTop());
    }
}
