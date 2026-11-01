package com.says.realisticterrain.command;

import com.says.realisticterrain.RealisticTerrainPlugin;
import com.says.realisticterrain.biome.BiomeType;
import com.says.realisticterrain.compat.Compat;
import com.says.realisticterrain.generator.RealisticChunkGenerator;
import com.says.realisticterrain.terrain.RealisticWorld;
import com.says.realisticterrain.terrain.Settings;
import com.says.realisticterrain.terrain.TerrainColumn;
import com.says.realisticterrain.terrain.WorldProfile;
import com.says.realisticterrain.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * /realisticterrain 命令。全部操作都只读配置或做纯数学采样，
 * 不需要 Bukkit 调度器，在 Folia 的区域线程上也能直接执行。
 */
public final class RealisticCommand implements CommandExecutor, TabCompleter {

    private final RealisticTerrainPlugin plugin;

    public RealisticCommand(RealisticTerrainPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realisticterrain.admin")) {
            sender.sendMessage(Messages.prefix("§c你没有权限使用这个命令。"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info":
                sendInfo(sender);
                return true;
            case "reload":
                plugin.reloadAll();
                sender.sendMessage(Messages.prefix("§a配置已重载。新生成的区块会使用新配置，"
                        + "已经生成的区块保持原样。"));
                return true;
            case "biome":
                return sendBiome(sender, args);
            case "pregen":
                return pregen(sender, args);
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Messages.prefix("§6RealisticTerrain §7—— 1.18+ 的真实地形"));
        sender.sendMessage("§8  /" + label + " info §7- 查看生成器状态");
        sender.sendMessage("§8  /" + label + " biome [x z] §7- 查看指定坐标的群系与地表高度");
        sender.sendMessage("§8  /" + label + " pregen [半径] [x z] §7- 预生成区块（Folia 可用）");
        sender.sendMessage("§8  /" + label + " reload §7- 重载配置（只影响新区块）");
    }

    private void sendInfo(CommandSender sender) {
        Settings settings = plugin.settings();
        sender.sendMessage(Messages.prefix("§6RealisticTerrain §7v"
                + plugin.getDescription().getVersion()));
        sender.sendMessage("§8  服务端: §7" + Compat.serverName()
                + (Compat.isFolia() ? " §a(Folia)" : ""));
        sender.sendMessage("§8  海平面: §7y=" + settings.seaLevel()
                + " §8| 地形最高: §7y=" + settings.maxTerrainY()
                + " §8| 基岩厚度: §7" + settings.bedrockDepth());
        sender.sendMessage("§8  洞穴: §7" + enabled(settings.caves())
                + " §8| 密度: §7" + settings.caveDensity()
                + " §8| 矿物: §7" + enabled(settings.ores())
                + " §8| 原版植被: §7" + enabled(settings.vanillaDecoration())
                + " §8| 原版结构: §7" + enabled(settings.vanillaStructures()));

        List<String> worlds = new ArrayList<>();
        WorldProfile firstProfile = null;
        for (World world : Bukkit.getWorlds()) {
            if (world.getGenerator() instanceof RealisticChunkGenerator) {
                worlds.add(world.getName());
                if (firstProfile == null) {
                    firstProfile = plugin.worldFor(world).profile();
                }
            }
        }
        if (firstProfile != null) {
            sender.sendMessage("§8  实际高度: §7世界 y=" + firstProfile.worldMin()
                    + ".." + firstProfile.worldMax()
                    + "，海平面 y=" + firstProfile.seaLevel()
                    + "，地形上限 y=" + firstProfile.terrainTop()
                    + "，雪线 y=" + firstProfile.snowLine());
        }
        sender.sendMessage("§8  使用真实地形生成器的世界: §7"
                + (worlds.isEmpty() ? "§c无（请在 bukkit.yml 的 worlds 里配置 generator）"
                : String.join(", ", worlds)));
    }

    private boolean sendBiome(CommandSender sender, String[] args) {
        World world = null;
        int x;
        int z;
        if (args.length >= 3) {
            x = parseInt(args[1], Integer.MIN_VALUE);
            z = parseInt(args[2], Integer.MIN_VALUE);
            if (x == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
                sender.sendMessage(Messages.prefix("§c坐标格式不对：/" + args[0]
                        + " biome <x> <z>"));
                return true;
            }
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else if (!Bukkit.getWorlds().isEmpty()) {
                world = Bukkit.getWorlds().get(0);
            }
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            world = player.getWorld();
            x = player.getLocation().getBlockX();
            z = player.getLocation().getBlockZ();
        } else {
            sender.sendMessage(Messages.prefix("§c控制台需要指定坐标：/realisticterrain biome <x> <z>"));
            return true;
        }

        if (world == null) {
            sender.sendMessage(Messages.prefix("§c当前没有可用世界。"));
            return true;
        }
        if (!(world.getGenerator() instanceof RealisticChunkGenerator)) {
            sender.sendMessage(Messages.prefix("§c世界 " + world.getName()
                    + " 没有使用 RealisticTerrain 生成器（generator: RealisticTerrain:realistic）。"));
            return true;
        }

        RealisticWorld realistic = plugin.worldFor(world);
        TerrainColumn column = realistic.columnAt(x, z);
        BiomeType type = column.biome();
        sender.sendMessage(Messages.prefix("§6" + world.getName() + " §7(" + x + ", " + z + ")"));
        sender.sendMessage("§8  群系: §7" + type.id() + " §8-> §a"
                + plugin.biomeMapping().get(type).getKey().getKey());
        sender.sendMessage("§8  地表高度: §7y=" + realistic.surfaceHeight(x, z)
                + " §8| 山脉强度: §7" + String.format(Locale.ROOT, "%.2f", column.mountain()));
        return true;
    }

    private boolean pregen(CommandSender sender, String[] args) {
        World world = sender instanceof Player
                ? ((Player) sender).getWorld()
                : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
        if (world == null) {
            sender.sendMessage(Messages.prefix("§c当前没有可用世界。"));
            return true;
        }
        if (!(world.getGenerator() instanceof RealisticChunkGenerator)) {
            sender.sendMessage(Messages.prefix("§c世界 " + world.getName()
                    + " 没有使用 RealisticTerrain 生成器。"));
            return true;
        }
        int radius = args.length >= 2 ? parseInt(args[1], 8) : 8;
        radius = Math.max(1, Math.min(32, radius));
        int centerX;
        int centerZ;
        if (args.length >= 4) {
            centerX = parseInt(args[2], 0) >> 4;
            centerZ = parseInt(args[3], 0) >> 4;
        } else if (sender instanceof Player) {
            centerX = ((Player) sender).getLocation().getBlockX() >> 4;
            centerZ = ((Player) sender).getLocation().getBlockZ() >> 4;
        } else {
            centerX = 0;
            centerZ = 0;
        }
        int side = radius * 2 + 1;
        int total = side * side;
        AtomicInteger done = new AtomicInteger();
        sender.sendMessage(Messages.prefix("§7开始预生成 §a" + total + " §7个区块（半径 "
                + radius + "），这可能需要一段时间…"));
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = centerX + dx;
                int chunkZ = centerZ + dz;
                plugin.scheduling().loadChunk(world, chunkX, chunkZ, () -> {
                    int completed = done.incrementAndGet();
                    if (completed == total) {
                        plugin.scheduling().global(() -> sender.sendMessage(Messages.prefix(
                                "§a预生成完成：§7" + total + " 个区块。")));
                    }
                });
            }
        }
        return true;
    }

    private static String enabled(boolean value) {
        return value ? "开" : "关";
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias,
                                      String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList(
                    "info", "biome", "pregen", "reload", "help"));
            String prefix = args[0].toLowerCase(Locale.ROOT);
            options.removeIf(option -> !option.startsWith(prefix));
            return options;
        }
        return new ArrayList<>();
    }
}
