package com.says.realisticterrain.compat;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * 调度适配层，只用于管理命令（例如预生成），不参与区块生成。
 *
 * <p>普通服务端走 {@link org.bukkit.scheduler.BukkitScheduler}；Folia 没有
 * 主线程，必须把任务投递到拥有目标区块的区域线程。这里全部用反射调用
 * Folia 的 RegionScheduler，因此 jar 里不包含任何 Folia 类引用。</p>
 */
public final class Scheduling {

    private final Plugin plugin;
    private final boolean folia;
    private Object regionScheduler;
    private Object globalScheduler;

    public Scheduling(Plugin plugin) {
        this.plugin = plugin;
        this.folia = Compat.isFolia();
        if (Compat.hasRegionScheduler()) {
            try {
                Method getRegion = Bukkit.class.getMethod("getRegionScheduler");
                regionScheduler = getRegion.invoke(null);
                Method getGlobal = Bukkit.class.getMethod("getGlobalRegionScheduler");
                globalScheduler = getGlobal.invoke(null);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "区域调度器初始化失败，退回普通调度器", t);
                regionScheduler = null;
                globalScheduler = null;
            }
        }
    }

    public boolean isFolia() {
        return folia && regionScheduler != null;
    }

    /** 全局任务：普通服务端 = 主线程；Folia = 全局区域线程。 */
    public void global(Runnable runnable) {
        if (globalScheduler != null) {
            if (invoke(globalScheduler, "execute", new Class<?>[]{Plugin.class, Runnable.class},
                    plugin, runnable)) {
                return;
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    /** 让任务跑在拥有 (chunkX, chunkZ) 的区域线程上。 */
    public void atChunk(World world, int chunkX, int chunkZ, Runnable runnable) {
        if (regionScheduler != null) {
            if (invoke(regionScheduler, "execute",
                    new Class<?>[]{Plugin.class, World.class, int.class, int.class, Runnable.class},
                    plugin, world, chunkX, chunkZ, runnable)) {
                return;
            }
        }
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * 在正确的线程上加载/生成一个区块，然后执行回调。
     *
     * <p>Paper 优先用返回 {@code CompletableFuture} 的异步加载，避免阻塞
     * 主线程；Folia 投递到拥有该区块的区域线程后同步加载。预生成不依赖
     * 仅在部分版本存在的 Consumer 回调重载。</p>
     */
    public void loadChunk(World world, int chunkX, int chunkZ, Runnable done) {
        if (isFolia()) {
            atChunk(world, chunkX, chunkZ, () -> {
                world.getChunkAt(chunkX, chunkZ);
                done.run();
            });
            return;
        }

        // Paper 1.18+：优先使用返回 CompletableFuture 的异步加载，
        // 这样预生成不会占住主线程触发 watchdog。
        try {
            Method method = World.class.getMethod("getChunkAtAsync",
                    int.class, int.class, boolean.class);
            Object result = method.invoke(world, chunkX, chunkZ, true);
            if (result instanceof java.util.concurrent.CompletableFuture) {
                ((java.util.concurrent.CompletableFuture<?>) result)
                        .whenComplete((chunk, error) -> done.run());
                return;
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "异步区块加载不可用，退回主线程", t);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            world.getChunkAt(chunkX, chunkZ);
            done.run();
        });
    }

    private boolean invoke(Object target, String name, Class<?>[] params, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, params);
            try {
                method.setAccessible(true);
            } catch (Throwable ignored) {
                // 公开接口一般不需要
            }
            method.invoke(target, args);
            return true;
        } catch (InvocationTargetException e) {
            plugin.getLogger().log(Level.WARNING, "调度任务执行失败", e.getCause());
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.FINE, "调度方法 " + name + " 调用失败", t);
            return false;
        }
    }
}
