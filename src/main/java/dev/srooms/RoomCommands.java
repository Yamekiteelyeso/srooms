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
                    "deleteroom", "reopen", "cancel" -> true;
            default -> false;
        };
        if (!sender.hasPermission(admin ? "srooms.admin" : "srooms.use")) {
            plugin.msg(sender, "no-permission");
            return true;
        }

        switch (sub) {
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
                        "seconds", String.valueOf(room.reopenDelay()));
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
                possible.addAll(Arrays.asList("help", "list", "join", "leave", "info"));
            if (sender.hasPermission("srooms.admin"))
                possible.addAll(Arrays.asList("createroom", "nameroom", "roomconfig", "deleteroom", "reopen", "cancel"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("join", "info", "roomconfig", "config", "deleteroom", "reopen").contains(sub)) {
                if (sender.hasPermission(sub.equals("join") || sub.equals("info") ? "srooms.use" : "srooms.admin"))
                    for (Room room : plugin.rooms().all()) possible.add(room.name());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("reopen") && sender.hasPermission("srooms.admin")) {
            possible.addAll(Arrays.asList("0", "5", "10", "15", "30", "60"));
        }
        String start = args[args.length - 1].toLowerCase(Locale.ROOT);
        return possible.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(start)).sorted().toList();
    }
}
