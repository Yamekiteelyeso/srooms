package dev.srooms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class RoomCommands implements CommandExecutor, TabCompleter {
    private final SRoomsPlugin plugin;

    public RoomCommands(SRoomsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.msg(sender, "help-title");
            plugin.msg(sender, "help-player");
            if (sender.hasPermission("srooms.admin")) {
                plugin.msg(sender, "help-admin");
                plugin.msg(sender, "help-admin-two");
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean admin = switch (sub) {
            case "createroom", "nameroom", "roomconfig", "config",
                    "deleteroom", "reopen", "cancel", "reload", "duration" -> true;
            default -> false;
        };
        if (!sender.hasPermission(admin ? "srooms.admin" : "srooms.use")) {
            plugin.msg(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "reload" -> {
                plugin.reloadSettings();
                plugin.msg(sender, "reloaded");
            }
            case "duration" -> {
                if (args.length != 3) { plugin.msg(sender, "usage-duration"); return true; }
                Room room = plugin.rooms().get(args[1]);
                if (room == null) { plugin.msg(sender, "unknown-room"); return true; }
                if (plugin.duels().isOccupied(room)) { plugin.msg(sender, "room-occupied"); return true; }
                int seconds;
                try { seconds = Integer.parseInt(args[2]); }
                catch (NumberFormatException ex) { plugin.msg(sender, "invalid-duration"); return true; }
                if (seconds < 30 || seconds > 1800) { plugin.msg(sender, "invalid-duration"); return true; }
                room.duelDuration(seconds);
                plugin.rooms().save();
                plugin.msg(sender, "duration-configured", "room", room.name(), "seconds", String.valueOf(seconds));
            }
            case "stats" -> {
                StatsManager.Record stats;
                if (args.length >= 2) stats = plugin.stats().find(args[1]);
                else if (sender instanceof Player player) stats = plugin.stats().get(player);
                else { plugin.msg(sender, "usage-stats"); return true; }
                if (stats == null) { plugin.msg(sender, "stats-empty"); return true; }
                plugin.msg(sender, "stats-title", "player", stats.name());
                plugin.msg(sender, "stats-line", "wins", String.valueOf(stats.wins()),
                        "losses", String.valueOf(stats.losses()),
                        "streak", String.valueOf(stats.streak()),
                        "best", String.valueOf(stats.bestStreak()),
                        "rate", String.format(Locale.ROOT, "%.1f", stats.winRate()));
            }
            case "top" -> {
                plugin.msg(sender, "top-title");
                int rank = 0;
                for (StatsManager.Record record : plugin.stats().top(10)) {
                    if (record.wins() == 0) continue;
                    plugin.msg(sender, "top-entry", "rank", String.valueOf(++rank),
                            "player", record.name(), "wins", String.valueOf(record.wins()),
                            "streak", String.valueOf(record.streak()));
                }
                if (rank == 0) plugin.msg(sender, "stats-empty");
            }
            case "createroom" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                plugin.selections().begin(player);
            }
            case "cancel" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                plugin.selections().cancel(player, true);
            }
            case "nameroom" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                if (args.length != 2) {
                    plugin.msg(sender, "usage-name");
                    return true;
                }
                plugin.selections().name(player, args[1]);
            }
            case "roomconfig", "config" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                Room room = findRoom(sender, args, "usage-config");
                if (room != null) plugin.menu().open(player, room);
            }
            case "deleteroom" -> {
                Room room = findRoom(sender, args, "usage-delete");
                if (room == null) return true;
                if (plugin.duels().isOccupied(room)) {
                    plugin.msg(sender, "room-occupied");
                    return true;
                }
                plugin.rooms().remove(room);
                plugin.msg(sender, "room-deleted", "room", room.name());
            }
            case "reopen" -> {
                if (args.length != 3) {
                    plugin.msg(sender, "usage-reopen");
                    return true;
                }
                Room room = plugin.rooms().get(args[1]);
                if (room == null) {
                    plugin.msg(sender, "unknown-room");
                    return true;
                }
                if (plugin.duels().isOccupied(room)) {
                    plugin.msg(sender, "room-occupied");
                    return true;
                }
                int seconds;
                try {
                    seconds = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    plugin.msg(sender, "invalid-seconds");
                    return true;
                }
                if (seconds < 0 || seconds > 300) {
                    plugin.msg(sender, "invalid-seconds");
                    return true;
                }
                room.reopenDelay(seconds);
                plugin.rooms().save();
                plugin.msg(sender, "reopen-configured", "room", room.name(), "seconds", String.valueOf(seconds));
            }
            case "list" -> {
                if (plugin.rooms().all().isEmpty()) {
                    plugin.msg(sender, "rooms-empty");
                    return true;
                }
                plugin.msg(sender, "rooms-title");
                for (Room room : plugin.rooms().all()) {
                    plugin.msg(sender, "rooms-entry", "room", room.name(),
                            "players", String.valueOf(plugin.duels().participants(room)),
                            "status", plugin.duels().state(room));
                }
            }
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                Room room = findRoom(sender, args, "usage-join");
                if (room != null) plugin.duels().join(player, room);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player");
                    return true;
                }
                plugin.duels().leave(player, false);
            }
            case "info" -> {
                Room room = findRoom(sender, args, "usage-info");
                if (room == null) return true;
                plugin.msg(sender, "room-info", "room", room.name(),
                        "players", String.valueOf(plugin.duels().participants(room)),
                        "status", plugin.duels().state(room),
                        "keep", room.keepInventory() ? "Sí" : "No",
                        "seconds", String.valueOf(room.reopenDelay()),
                        "duration", String.valueOf(room.duelDuration()));
            }
            default -> plugin.msg(sender, "unknown-command");
        }
        return true;
    }

    private Room findRoom(CommandSender sender, String[] args, String usage) {
        if (args.length != 2) {
            plugin.msg(sender, usage);
            return null;
        }
        Room room = plugin.rooms().get(args[1]);
        if (room == null) plugin.msg(sender, "unknown-room");
        return room;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();
        List<String> possible = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("srooms.use"))
                possible.addAll(Arrays.asList("help", "list", "join", "leave", "info", "stats", "top"));
            if (sender.hasPermission("srooms.admin"))
                possible.addAll(Arrays.asList("createroom", "nameroom", "roomconfig", "deleteroom", "reopen", "duration", "reload", "cancel"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("join", "info", "roomconfig", "config", "deleteroom", "reopen", "duration").contains(sub)) {
                if (sender.hasPermission(sub.equals("join") || sub.equals("info") ? "srooms.use" : "srooms.admin"))
                    for (Room room : plugin.rooms().all()) possible.add(room.name());
            }
            if (sub.equals("stats")) {
                for (Player online : plugin.getServer().getOnlinePlayers()) possible.add(online.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reopen") && sender.hasPermission("srooms.admin")) {
            possible.addAll(Arrays.asList("0", "5", "10", "15", "30", "60"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("duration") && sender.hasPermission("srooms.admin")) {
            possible.addAll(Arrays.asList("60", "120", "180", "300", "600", "900"));
        }
        String start = args[args.length - 1].toLowerCase(Locale.ROOT);
        return possible.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(start)).sorted().toList();
    }
}
