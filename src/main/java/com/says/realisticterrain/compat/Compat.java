package com.says.realisticterrain.compat;

/**
 * 运行环境探测。整个插件不直接引用 Folia 的类，避免在普通 Spigot/Paper
 * 上加载时出现 NoClassDefFoundError。
 */
public final class Compat {

    private Compat() {
    }

    /** 当前是否运行在 Folia（或兼容 Folia 的区域化服务端）上。 */
    public static boolean isFolia() {
        return classExists("io.papermc.paper.threadedregions.RegionizedServer")
                || classExists("io.papermc.paper.threadedregions.TickRegionScheduler");
    }

    public static boolean hasRegionScheduler() {
        return isFolia() && classExists("io.papermc.paper.threadedregions.RegionizedServer");
    }

    public static boolean classExists(String name) {
        try {
            Class.forName(name, false, Compat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    public static String serverName() {
        try {
            String version = org.bukkit.Bukkit.getVersion();
            return version == null ? "unknown" : version;
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
