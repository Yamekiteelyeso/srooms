package dev.srooms;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SelectionManager {
    public enum Stage { ROOM, GATE, NAME }
    public static final class Draft {
        private Stage stage = Stage.ROOM;
        private Location point1, point2;
        private Cuboid room, gate;
        public Stage stage() { return stage; }
    }

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{3,24}");
    private final SRoomsPlugin plugin;
    private final NamespacedKey toolKey;
    private final Map<UUID, Draft> drafts = new HashMap<>();

    public SelectionManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        toolKey = new NamespacedKey(plugin, "selection");
    }

    public boolean isTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
    }

    private String toolKind(ItemStack item) {
        if (!isTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    private ItemStack axe(Stage stage) {
        boolean gate = stage == Stage.GATE;
        ItemStack item = new ItemStack(gate ? Material.DIAMOND_AXE : Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SRoomsPlugin.color(gate ? "&b&lSRooms &8» &fCristal" : "&b&lSRooms &8» &fRoom"));
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Izquierdo: punto 1",
                ChatColor.GRAY + "Derecho: punto 2",
                ChatColor.DARK_GRAY + "Herramienta de seleccion"));
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, gate ? "gate" : "zone");
        item.setItemMeta(meta);
        return item;
    }

    private boolean give(Player player, Stage stage) {
        int empty = player.getInventory().firstEmpty();
        if (empty < 0) return false;
        player.getInventory().setItem(empty, axe(stage));
        return true;
    }

    private void removeTools(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (isTool(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, null);
        }
        if (isTool(player.getInventory().getItemInOffHand()))
            player.getInventory().setItemInOffHand(null);
    }

    public void begin(Player player) {
        if (plugin.duels().isPlaying(player)) {
            plugin.msg(player, "already-in-room");
            return;
        }
        cancel(player, false);
        Draft draft = new Draft();
        drafts.put(player.getUniqueId(), draft);
        if (!give(player, Stage.ROOM)) {
            drafts.remove(player.getUniqueId());
            plugin.msg(player, "selection-invalid", "reason", "Libera un espacio en el inventario.");
            return;
        }
        plugin.msg(player, "selection-room");
    }

    public void cancel(Player player, boolean notify) {
        Draft removed = drafts.remove(player.getUniqueId());
        removeTools(player);
        if (notify && removed != null) plugin.msg(player, "selection-ended");
    }

    public void shutdown() {
        for (UUID uuid : drafts.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) removeTools(player);
        }
        drafts.clear();
    }

    public boolean isSelecting(Player player) { return drafts.containsKey(player.getUniqueId()); }

    public void click(Player player, Action action, Block block, ItemStack item) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null || draft.stage == Stage.NAME) return;
        String requiredKind = draft.stage == Stage.GATE ? "gate" : "zone";
        if (!requiredKind.equals(toolKind(item))) {
            plugin.msg(player, "selection-invalid", "reason", "Usa el hacha de selección correspondiente.");
            return;
        }
        if (action == Action.LEFT_CLICK_BLOCK) draft.point1 = block.getLocation();
        else if (action == Action.RIGHT_CLICK_BLOCK) draft.point2 = block.getLocation();
        else return;

        plugin.msg(player, "selection-point",
                "point", action == Action.LEFT_CLICK_BLOCK ? "1" : "2",
                "stage", draft.stage == Stage.ROOM ? "ROOM" : "CRISTAL",
                "coords", block.getX() + ", " + block.getY() + ", " + block.getZ());
        if (draft.point1 == null || draft.point2 == null) return;
        Cuboid cube;
        try {
            cube = new Cuboid(draft.point1, draft.point2);
        } catch (IllegalArgumentException ex) {
            plugin.msg(player, "selection-invalid", "reason", ex.getMessage());
            draft.point2 = null;
            return;
        }
        long max = draft.stage == Stage.GATE
                ? plugin.getConfig().getLong("selection.max-gate-volume", 4096)
                : plugin.getConfig().getLong("selection.max-zone-volume", 250000);
        if (cube.volume() > max) {
            plugin.msg(player, "selection-invalid", "reason", "La region es demasiado grande (max: " + max + " bloques).");
            draft.point2 = null;
            return;
        }
        draft.point1 = null;
        draft.point2 = null;
        switch (draft.stage) {
            case ROOM -> {
                draft.room = cube;
                draft.stage = Stage.GATE;
                removeTools(player);
                give(player, Stage.GATE);
                plugin.msg(player, "selection-gate");
            }
            case GATE -> {
                if (!draft.room.contains(cube)) {
                    plugin.msg(player, "selection-invalid", "reason",
                            "El cristal debe quedar completamente dentro de la room. Selecciona sus dos puntos otra vez.");
                    return;
                }
                draft.gate = cube;
                draft.stage = Stage.NAME;
                removeTools(player);
                plugin.msg(player, "selection-name");
            }
            default -> { }
        }
    }

    public void name(Player player, String name) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null || draft.stage != Stage.NAME) {
            plugin.msg(player, "selection-incomplete");
            return;
        }
        if (!VALID_NAME.matcher(name).matches()) {
            plugin.msg(player, "selection-invalid", "reason", "Usa de 3 a 24 caracteres: letras, numeros, _ o -.");
            return;
        }
        if (plugin.rooms().get(name) != null) {
            plugin.msg(player, "selection-invalid", "reason", "Ese nombre ya existe.");
            return;
        }
        Room room = new Room(name, draft.room, draft.gate);
        String problem = plugin.rooms().validate(room);
        if (problem != null) {
            plugin.msg(player, "selection-invalid", "reason", problem);
            return;
        }
        plugin.rooms().add(room);
        drafts.remove(player.getUniqueId());
        plugin.msg(player, "room-created", "room", name);
        if (!room.validSpawn(room.spawnA()) || !room.validSpawn(room.spawnB())) {
            plugin.msg(player, "room-spawns-needed", "room", name);
        }
        player.sendMessage(SRoomsPlugin.color("&7Configura esta room con &b/srooms roomconfig " + name));
    }
}
