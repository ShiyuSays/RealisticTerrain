package com.says.realisticterrain.terrain;

import com.says.realisticterrain.noise.NoiseFactory;
import com.says.realisticterrain.noise.OctaveNoise;

import static com.says.realisticterrain.noise.NoiseMath.smoothstep;

/**
 * 三维密度场：值大于 0 的位置是实心岩石。
 *
 * <p>基础项 {@code (列高 - y)} 保证地表位置正确，三维噪声只在靠近地表的
 * 十几格内生效，用来制造悬崖、山脊和局部悬垂；远离地表后噪声淡出，
 * 避免出现成片浮空岛。</p>
 */
public final class DensitySampler {

    private final OctaveNoise shape;
    private final OctaveNoise broad;

    public DensitySampler(NoiseFactory noise) {
        this.shape = noise.octaves("density_shape", 4, 0.50, 0.0130);
        this.broad = noise.octaves("density_broad", 3, 0.50, 0.0045);
    }

    public double densityAt(int x, int y, int z, double columnHeight, double mountain,
                            Settings settings) {
        double distance = columnHeight - y;
        double gradient = distance * 0.085;

        double local = shape.noise3(x, y * 1.65, z) * 1.25;
        double large = broad.noise3(x * 0.7, y * 0.85, z * 0.7) * 0.55;

        // 只在贴近地表的范围内允许噪声“推翻”列高，形成悬崖/悬垂。
        double fade = 1.0 - smoothstep(14.0, 30.0, Math.abs(distance));
        double overhang = settings.roughness() * (0.55 + 0.45 * mountain);
        return gradient + (local + large) * overhang * fade;
    }
}
