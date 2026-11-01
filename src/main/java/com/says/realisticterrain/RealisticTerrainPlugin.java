package com.says.realisticterrain;

import com.says.realisticterrain.command.RealisticCommand;
import com.says.realisticterrain.compat.BiomeMapping;
import com.says.realisticterrain.compat.Compat;
import com.says.realisticterrain.compat.Scheduling;
import com.says.realisticterrain.config.ConfigLoader;
import com.says.realisticterrain.generator.RealisticChunkGenerator;
import com.says.realisticterrain.terrain.RealisticWorld;
import com.says.realisticterrain.terrain.Settings;
import com.says.realisticterrain.terrain.WorldProfile;
import org.bukkit.command.PluginCommand;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RealisticTerrain 主类。
 *
 * <p>插件只做三件事：读配置、按世界种子缓存 {@link RealisticWorld}、
 * 把生成器交给 Bukkit/Paper/Folia。区块生成路径没有主线程交互，
 * 也没有调度器调用。</p>
 */
public final class RealisticTerrainPlugin extends JavaPlugin {

    private final ConcurrentHashMap<String, RealisticWorld> worlds = new ConcurrentHashMap<>();
    private final AtomicInteger revision = new AtomicInteger();

    private volatile Settings settings;
    private volatile BiomeMapping biomeMapping;
    private volatile Scheduling scheduling;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.biomeMapping = new BiomeMapping(getLogger());
        this.settings = ConfigLoader.load(getConfig());
        this.scheduling = new Scheduling(this);
        this.revision.incrementAndGet();

        RealisticCommand executor = new RealisticCommand(this);
        PluginCommand command = getCommand("realisticterrain");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("真实地形生成器已启用（" + (Compat.isFolia() ? "Folia" : "常规服务端")
                + "），海平面 y=" + settings.seaLevel()
                + "，地形最高 y=" + settings.maxTerrainY()
                + "，洞穴密度 " + settings.caveDensity() + "。");
        getLogger().info("在新世界的 bukkit.yml 中设置 generator: RealisticTerrain:realistic 才会生效。");
    }

    @Override
    public void onDisable() {
        worlds.clear();
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        String generatorId = id == null ? "" : id.trim().toLowerCase();
        if (generatorId.isEmpty() || generatorId.equals("realistic")
                || generatorId.equals("modern") || generatorId.equals("terrain")
                || generatorId.equals("default")) {
            return new RealisticChunkGenerator(this);
        }
        return null;
    }

    /** 重新读取 config.yml；只影响之后新生成的区块。 */
    public void reloadAll() {
        reloadConfig();
        this.settings = ConfigLoader.load(getConfig());
        this.revision.incrementAndGet();
        worlds.clear();
    }

    public Settings settings() {
        return settings;
    }

    public int revision() {
        return revision.get();
    }

    public BiomeMapping biomeMapping() {
        return biomeMapping;
    }

    public Scheduling scheduling() {
        return scheduling;
    }

    /**
     * 取得某个世界对应的生成器实例。相同种子、相同世界高度共享噪声；
     * reload 后 revision 变化，会创建新实例。
     */
    public RealisticWorld worldFor(WorldInfo info) {
        String key = info.getSeed() + ":" + revision.get() + ":"
                + info.getMinHeight() + ":" + info.getMaxHeight();
        RealisticWorld world = worlds.get(key);
        if (world != null) {
            return world;
        }
        WorldProfile profile = WorldProfile.of(info, settings);
        RealisticWorld created = new RealisticWorld(info.getSeed(), settings, profile);
        RealisticWorld previous = worlds.putIfAbsent(key, created);
        if (worlds.size() > 64) {
            worlds.clear();
            worlds.put(key, created);
        }
        return previous == null ? created : previous;
    }
}
