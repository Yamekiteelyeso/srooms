package dev.srooms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RoomCommands implements CommandExecutor, TabCompleter {
    private final SRoomsPlugin plugin;

    public RoomCommands(SRoomsPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!name.equals("room") && !sender.hasPermission("srooms.admin")) {
            plugin.msg(sender, "no-permission"); return true;
        }
        switch (name) {
            case "createroom" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return true; }
                plugin.selections().begin(player);
            }
            case "nameroom" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return true; }
                if (args.length != 1) { sender.sendMessage(SRoomsPlugin.color("&cUso: /nameroom <nombre>")); return true; }
                plugin.selections().name(player, args[0]);
            }
            case "roomconfig" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return true; }
                if (args.length != 1) { sender.sendMessage(SRoomsPlugin.color("&cUso: /roomconfig <nombre>")); return true; }
                Room room = plugin.rooms().get(args[0]);
                if (room == null) plugin.msg(sender, "unknown-room");
                else plugin.menu().open(player, room);
            }
            case "deleteroom" -> {
                if (args.length != 1) { sender.sendMessage(SRoomsPlugin.color("&cUso: /deleteroom <nombre>")); return true; }
                Room room = plugin.rooms().get(args[0]);
                if (room == null) { plugin.msg(sender, "unknown-room"); return true; }
                if (plugin.duels().isOccupied(room)) { plugin.msg(sender, "room-occupied"); return true; }
                plugin.rooms().remove(room);
                plugin.msg(sender, "room-deleted", "room", room.name());
            }
            case "room" -> handleRoom(sender, args);
            default -> { return false; }
        }
        return true;
    }

    private void handleRoom(CommandSender sender, String[] args) {
        if (!sender.hasPermission("srooms.use")) { plugin.msg(sender, "no-permission"); return; }
        if (args.length == 0) {
            sender.sendMessage(SRoomsPlugin.color("&bSRooms &8» &f/room list &7| &f/room join <nombre> &7| &f/room leave &7| &f/room info <nombre>"));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                if (plugin.rooms().all().isEmpty()) {
                    sender.sendMessage(SRoomsPlugin.color("&eNo hay rooms creadas."));
                    return;
                }
                sender.sendMessage(SRoomsPlugin.color("&b&lRooms disponibles:"));
                for (Room room : plugin.rooms().all()) {
                    String status = plugin.duels().state(room);
                    sender.sendMessage(SRoomsPlugin.color("&8- &f" + room.name() + " &7(" +
                            plugin.duels().participants(room) + "/2, &b" + status + "&7)"));
                }
            }
            case "join" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return; }
                if (args.length != 2) { sender.sendMessage(SRoomsPlugin.color("&cUso: /room join <nombre>")); return; }
                Room room = plugin.rooms().get(args[1]);
                if (room == null) plugin.msg(sender, "unknown-room");
                else plugin.duels().join(player, room);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) { plugin.msg(sender, "only-player"); return; }
                plugin.duels().leave(player, false);
            }
            case "info" -> {
                if (args.length != 2) { sender.sendMessage(SRoomsPlugin.color("&cUso: /room info <nombre>")); return; }
                Room room = plugin.rooms().get(args[1]);
                if (room == null) { plugin.msg(sender, "unknown-room"); return; }
                sender.sendMessage(SRoomsPlugin.color("&bRoom &f" + room.name()
                        + " &7| &f" + plugin.duels().participants(room) + "/2 jugadores"
                        + " &7| &fEstado: " + plugin.duels().state(room)
                        + " &7| &fKeep: " + (room.keepInventory() ? "Sí" : "No")
                        + " &7| &fCountdown: " + room.countdown() + "s"));
            }
            default -> sender.sendMessage(SRoomsPlugin.color("&cSubcomandos: list, join, leave, info."));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        List<String> candidates = new ArrayList<>();
        if (cmd.equals("room")) {
            if (args.length == 1) candidates.addAll(Arrays.asList("list", "join", "leave", "info"));
            if (args.length == 2 && (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")))
                for (Room room : plugin.rooms().all()) candidates.add(room.name());
        } else if ((cmd.equals("roomconfig") || cmd.equals("deleteroom")) && sender.hasPermission("srooms.admin") && args.length == 1) {
            for (Room room : plugin.rooms().all()) candidates.add(room.name());
        }
        String partial = args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(partial))
                .sorted().collect(Collectors.toList());
    }
}
