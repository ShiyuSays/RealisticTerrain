package com.says.realisticterrain.terrain;

/**
 * 生成配置的不可变快照。
 *
 * <p>每次 reload 都创建一个新实例；已经开始生成的区块继续使用旧快照，
 * 新生成的区块使用新快照，避免配置切换时地形在区块中间“变脸”。</p>
 */
public final class Settings {

    private final int seaLevel;
    private final int maxTerrainY;
    private final int bedrockDepth;
    private final double heightScale;
    private final double continentScale;
    private final double mountainHeight;
    private final double oceanDepth;
    private final double roughness;
    private final boolean rivers;
    private final double riverWidth;
    private final boolean caves;
    private final double caveDensity;
    private final int lavaLevel;
    private final boolean lavaLakes;
    private final boolean ores;
    private final double oreDensity;
    private final boolean geology;
    private final boolean vanillaDecoration;
    private final boolean vanillaStructures;
    private final boolean vanillaMobs;
    private final boolean debug;

    private Settings(Builder builder) {
        this.seaLevel = builder.seaLevel;
        this.maxTerrainY = builder.maxTerrainY;
        this.bedrockDepth = builder.bedrockDepth;
        this.heightScale = builder.heightScale;
        this.continentScale = builder.continentScale;
        this.mountainHeight = builder.mountainHeight;
        this.oceanDepth = builder.oceanDepth;
        this.roughness = builder.roughness;
        this.rivers = builder.rivers;
        this.riverWidth = builder.riverWidth;
        this.caves = builder.caves;
        this.caveDensity = builder.caveDensity;
        this.lavaLevel = builder.lavaLevel;
        this.lavaLakes = builder.lavaLakes;
        this.ores = builder.ores;
        this.oreDensity = builder.oreDensity;
        this.geology = builder.geology;
        this.vanillaDecoration = builder.vanillaDecoration;
        this.vanillaStructures = builder.vanillaStructures;
        this.vanillaMobs = builder.vanillaMobs;
        this.debug = builder.debug;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int seaLevel() {
        return seaLevel;
    }

    public int maxTerrainY() {
        return maxTerrainY;
    }

    public int bedrockDepth() {
        return bedrockDepth;
    }

    public double heightScale() {
        return heightScale;
    }

    public double continentScale() {
        return continentScale;
    }

    public double mountainHeight() {
        return mountainHeight;
    }

    public double oceanDepth() {
        return oceanDepth;
    }

    public double roughness() {
        return roughness;
    }

    public boolean rivers() {
        return rivers;
    }

    public double riverWidth() {
        return riverWidth;
    }

    public boolean caves() {
        return caves;
    }

    public double caveDensity() {
        return caveDensity;
    }

    public int lavaLevel() {
        return lavaLevel;
    }

    public boolean lavaLakes() {
        return lavaLakes;
    }

    public boolean ores() {
        return ores;
    }

    public double oreDensity() {
        return oreDensity;
    }

    public boolean geology() {
        return geology;
    }

    public boolean vanillaDecoration() {
        return vanillaDecoration;
    }

    public boolean vanillaStructures() {
        return vanillaStructures;
    }

    public boolean vanillaMobs() {
        return vanillaMobs;
    }

    public boolean debug() {
        return debug;
    }

    public static final class Builder {

        private int seaLevel = 96;
        private int maxTerrainY = 304;
        private int bedrockDepth = 3;
        private double heightScale = 1.0;
        private double continentScale = 1.0;
        private double mountainHeight = 170.0;
        private double oceanDepth = 58.0;
        private double roughness = 1.0;
        private boolean rivers = true;
        private double riverWidth = 1.0;
        private boolean caves = true;
        private double caveDensity = 0.35;
        private int lavaLevel = -54;
        private boolean lavaLakes = true;
        private boolean ores = true;
        private double oreDensity = 1.0;
        private boolean geology = true;
        private boolean vanillaDecoration = true;
        private boolean vanillaStructures = false;
        private boolean vanillaMobs = true;
        private boolean debug = false;

        public Builder seaLevel(int value) {
            this.seaLevel = value;
            return this;
        }

        public Builder maxTerrainY(int value) {
            this.maxTerrainY = value;
            return this;
        }

        public Builder bedrockDepth(int value) {
            this.bedrockDepth = value;
            return this;
        }

        public Builder heightScale(double value) {
            this.heightScale = value;
            return this;
        }

        public Builder continentScale(double value) {
            this.continentScale = value;
            return this;
        }

        public Builder mountainHeight(double value) {
            this.mountainHeight = value;
            return this;
        }

        public Builder oceanDepth(double value) {
            this.oceanDepth = value;
            return this;
        }

        public Builder roughness(double value) {
            this.roughness = value;
            return this;
        }

        public Builder rivers(boolean value) {
            this.rivers = value;
            return this;
        }

        public Builder riverWidth(double value) {
            this.riverWidth = value;
            return this;
        }

        public Builder caves(boolean value) {
            this.caves = value;
            return this;
        }

        public Builder caveDensity(double value) {
            this.caveDensity = value;
            return this;
        }

        public Builder lavaLevel(int value) {
            this.lavaLevel = value;
            return this;
        }

        public Builder lavaLakes(boolean value) {
            this.lavaLakes = value;
            return this;
        }

        public Builder ores(boolean value) {
            this.ores = value;
            return this;
        }

        public Builder oreDensity(double value) {
            this.oreDensity = value;
            return this;
        }

        public Builder geology(boolean value) {
            this.geology = value;
            return this;
        }

        public Builder vanillaDecoration(boolean value) {
            this.vanillaDecoration = value;
            return this;
        }

        public Builder vanillaStructures(boolean value) {
            this.vanillaStructures = value;
            return this;
        }

        public Builder vanillaMobs(boolean value) {
            this.vanillaMobs = value;
            return this;
        }

        public Builder debug(boolean value) {
            this.debug = value;
            return this;
        }

        public Settings build() {
            this.seaLevel = clamp(this.seaLevel, 0, 400);
            this.maxTerrainY = clamp(this.maxTerrainY, 128, 512);
            if (this.maxTerrainY < this.seaLevel + 48) {
                this.maxTerrainY = this.seaLevel + 48;
            }
            this.bedrockDepth = clamp(this.bedrockDepth, 1, 5);
            this.heightScale = clamp(this.heightScale, 0.25, 3.0);
            this.continentScale = clamp(this.continentScale, 0.25, 4.0);
            this.mountainHeight = clamp(this.mountainHeight, 0.0, 280.0);
            this.oceanDepth = clamp(this.oceanDepth, 10.0, 120.0);
            this.roughness = clamp(this.roughness, 0.0, 2.5);
            this.riverWidth = clamp(this.riverWidth, 0.3, 3.0);
            this.caveDensity = clamp(this.caveDensity, 0.0, 1.5);
            this.lavaLevel = clamp(this.lavaLevel, -256, 300);
            this.oreDensity = clamp(this.oreDensity, 0.0, 3.0);
            return new Settings(this);
        }

        private static int clamp(int value, int min, int max) {
            return value < min ? min : Math.min(value, max);
        }

        private static double clamp(double value, double min, double max) {
            if (Double.isNaN(value)) {
                return min;
            }
            return value < min ? min : Math.min(value, max);
        }
    }
}
