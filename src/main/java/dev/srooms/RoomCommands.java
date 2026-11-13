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
                    "deleteroom", "reopen", "cancel", "reload", "duration", "check", "repair", "kit", "setexit", "reset" -> true;
            default -> false;
        };
        if (!sender.hasPermission(admin ? "srooms.admin" : "srooms.use")) {
            plugin.msg(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "kit" -> {
                if (args.length != 3 || !(args[1].equalsIgnoreCase("save")
                        || args[1].equalsIgnoreCase("clear") || args[1].equalsIgnoreCase("info"))) {
                    plugin.msg(sender, "usage-kit");
                    return true;
                }
                Room room = plugin.rooms().get(args[2]);
                if (room == null) { plugin.msg(sender, "unknown-room"); return true; }
                if (args[1].equalsIgnoreCase("info")) {
                    plugin.msg(sender, "kit-info", "room", room.name(),
                            "saved", plugin.kits().hasKit(room) ? "Sí" : "No",
                            "enabled", room.kitEnabled() ? "Sí" : "No");
                    return true;
                }
                if (plugin.duels().isOccupied(room)) {
                    plugin.msg(sender, "room-occupied");
                    return true;
                }
                if (args[1].equalsIgnoreCase("save")) {
                    if (!(sender instanceof Player player)) {
                        plugin.msg(sender, "only-player");
                        return true;
                    }
                    if (plugin.duels().isPlaying(player)) {
                        plugin.msg(sender, "already-in-room");
                        return true;
                    }
                    if (plugin.kits().saveKit(room, player))
                        plugin.msg(sender, "kit-saved", "room", room.name());
                    else plugin.msg(sender, "kit-save-failed");
                } else {
                    if (plugin.kits().clearKit(room))
                        plugin.msg(sender, "kit-cleared", "room", room.name());
                    else plugin.msg(sender, "kit-save-failed");
                }
            }
            case "setexit" -> {
                if (!(sender instanceof Player player)) {
                    plugin.msg(sender, "only-player"); return true;
                }
                for (Room room : plugin.rooms().all()) {
                    if (room.contains(player.getLocation())) {
                        plugin.msg(sender, "exit-inside-room"); return true;
                    }
                }
                plugin.getConfig().set("access.exit-spawn", player.getLocation().clone());
                plugin.saveConfig();
                plugin.msg(sender, "exit-saved");
            }
            case "reset" -> {
                Room room = findRoom(sender, args, "usage-reset");
                if (room == null) return true;
                if (plugin.duels().forceReset(room))
                    plugin.msg(sender, "room-reset", "room", room.name());
                else plugin.msg(sender, "room-already-idle", "room", room.name());
            }
            case "check" -> {
                Room room = findRoom(sender, args, "usage-check");
                if (room == null) return true;
                String validation = plugin.rooms().validate(room);
                if (validation == null) {
                    org.bukkit.Location a = room.spawnA();
                    org.bukkit.Location b = room.spawnB();
                    if (a == null || b == null) validation = "Falta un spawn seguro (A o B).";
                    else if (!room.a().contains(a) || room.gate().contains(a)
                            || !room.b().contains(b) || room.gate().contains(b))
                        validation = "Los spawns deben estar en sus zonas y fuera del cristal.";
                }
                if (validation == null && room.kitEnabled() && !plugin.kits().hasKit(room))
                    validation = "El kit está activado, pero no existe un kit guardado.";
                if (validation == null) plugin.msg(sender, "room-check-ok", "room", room.name());
                else plugin.msg(sender, "room-check-error", "room", room.name(),
                        "reason", validation);
            }
            case "repair" -> {
                Room room = findRoom(sender, args, "usage-repair");
                if (room == null) return true;
                if (plugin.duels().isOccupied(room)) {
                    plugin.msg(sender, "room-occupied");
                    return true;
                }
                boolean restored = plugin.duels().repairGate(room);
                plugin.msg(sender, restored ? "room-repaired" : "room-no-recovery",
                        "room", room.name());
            }
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
                        "draws", String.valueOf(stats.draws()),
                        "elo", String.valueOf(stats.rating()),
                        "streak", String.valueOf(stats.streak()),
                        "best", String.valueOf(stats.bestStreak()),
                        "rate", String.format(Locale.ROOT, "%.1f", stats.winRate()));
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return true; }
                plugin.browser().open(player, 0);
            }
            case "top" -> {
                boolean elo = args.length >= 2 && args[1].equalsIgnoreCase("elo");
                plugin.msg(sender, elo ? "top-elo-title" : "top-title");
                int rank = 0;
                List<StatsManager.Record> top = elo
                        ? plugin.stats().topRating(10) : plugin.stats().top(10);
                for (StatsManager.Record record : top) {
                    if (record.fights() == 0) continue;
                    plugin.msg(sender, elo ? "top-elo-entry" : "top-entry",
                            "rank", String.valueOf(++rank),
                            "player", record.name(), "elo", String.valueOf(record.rating()),
                            "wins", String.valueOf(record.wins()),
                            "streak", String.valueOf(record.streak()));
                }
                if (rank == 0) plugin.msg(sender, "stats-empty");
            }
            case "history" -> {
                if (args.length > 2) { plugin.msg(sender, "usage-history"); return true; }
                List<MatchHistory.Entry> history = plugin.history()
                        .recent(args.length == 2 ? args[1] : null, 10);
                if (history.isEmpty()) { plugin.msg(sender, "history-empty"); return true; }
                plugin.msg(sender, "history-title");
                for (MatchHistory.Entry result : history) {
                    String time = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
                            .format(java.time.Instant.ofEpochMilli(result.when())
                                    .atZone(java.time.ZoneId.systemDefault()));
                    plugin.msg(sender, result.draw() ? "history-draw" : "history-win",
                            "room", result.room(), "a", result.first(),
                            "b", result.second(), "winner", result.winner(), "time", time);
                }
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
                if (plugin.kits().hasKit(room) && !plugin.kits().clearKit(room)) {
                    plugin.msg(sender, "kit-save-failed");
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
                        "duration", String.valueOf(room.duelDuration()),
                        "ranked", room.ranked() ? "Sí" : "No",
                        "kit", room.kitEnabled() ? "Sí" : "No");
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
                possible.addAll(Arrays.asList("help", "list", "join", "leave", "info", "stats", "top", "history", "gui"));
            if (sender.hasPermission("srooms.admin"))
                possible.addAll(Arrays.asList("createroom", "nameroom", "roomconfig", "deleteroom", "reopen", "duration", "reload", "check", "repair", "reset", "setexit", "kit", "cancel"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("join", "info", "roomconfig", "config", "deleteroom", "reopen", "duration", "check", "repair", "reset").contains(sub)) {
                if (sender.hasPermission(sub.equals("join") || sub.equals("info") ? "srooms.use" : "srooms.admin"))
                    for (Room room : plugin.rooms().all()) possible.add(room.name());
            }
            if (sub.equals("stats")) {
                for (Player online : plugin.getServer().getOnlinePlayers()) possible.add(online.getName());
            }
            if (sub.equals("top")) possible.addAll(Arrays.asList("elo", "wins"));
            if (sub.equals("history")) {
                for (Room room : plugin.rooms().all()) possible.add(room.name());
            }
            if (sub.equals("kit") && sender.hasPermission("srooms.admin")) {
                possible.addAll(Arrays.asList("save", "clear", "info"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("kit")
                && sender.hasPermission("srooms.admin")) {
            for (Room room : plugin.rooms().all()) possible.add(room.name());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reopen") && sender.hasPermission("srooms.admin")) {
            possible.addAll(Arrays.asList("0", "5", "10", "15", "30", "60"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("duration") && sender.hasPermission("srooms.admin")) {
            possible.addAll(Arrays.asList("60", "120", "180", "300", "600", "900"));
        }
        String start = args[args.length - 1].toLowerCase(Locale.ROOT);
        return possible.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(start)).sorted().toList();
    }
}
