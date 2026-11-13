package dev.srooms;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Optional per-room kit templates. Original player inventories are written to a
 * separate recovery file BEFORE any item replacement, so a server crash does
 * not permanently strand a player with a duel kit.
 */
public final class KitManager {
    private final SRoomsPlugin plugin;
    private final File kitsFile;
    private final File recoveryFile;
    private YamlConfiguration templates;
    private YamlConfiguration recovery;

    public KitManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        recoveryFile = new File(plugin.getDataFolder(), "kit-recovery.yml");
        templates = YamlConfiguration.loadConfiguration(kitsFile);
        recovery = YamlConfiguration.loadConfiguration(recoveryFile);
    }

    private String key(Room room) {
        return "kits." + room.name().toLowerCase(Locale.ROOT);
    }

    private String playerKey(Player player) {
        return "players." + player.getUniqueId();
    }

    private ItemStack[] items(List<?> list, int size) {
        ItemStack[] result = new ItemStack[size];
        if (list == null) return result;
        for (int i = 0; i < Math.min(list.size(), size); i++) {
            if (list.get(i) instanceof ItemStack item) result[i] = item.clone();
        }
        return result;
    }

    private List<ItemStack> copy(ItemStack[] items) {
        List<ItemStack> result = new ArrayList<>(items.length);
        for (ItemStack item : items) result.add(item == null ? null : item.clone());
        return result;
    }

    private void capture(YamlConfiguration yaml, String base, Player player) {
        yaml.set(base + ".storage", copy(player.getInventory().getStorageContents()));
        yaml.set(base + ".armor", copy(player.getInventory().getArmorContents()));
        ItemStack offhand = player.getInventory().getItemInOffHand();
        yaml.set(base + ".offhand", offhand == null ? null : offhand.clone());
        yaml.set(base + ".level", player.getLevel());
        yaml.set(base + ".exp", player.getExp());
        yaml.set(base + ".total-exp", player.getTotalExperience());
    }

    private boolean save(YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("SRooms: no se pudo guardar " + file.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    public boolean hasKit(Room room) {
        return templates.isList(key(room) + ".storage");
    }

    public boolean saveKit(Room room, Player player) {
        if (player == null) return false;
        String path = key(room);
        YamlConfiguration next = YamlConfiguration.loadConfiguration(kitsFile);
        capture(next, path, player);
        next.set(path + ".name", room.name());
        if (!save(next, kitsFile)) return false;
        templates = next;
        return true;
    }

    public boolean clearKit(Room room) {
        YamlConfiguration next = YamlConfiguration.loadConfiguration(kitsFile);
        next.set(key(room), null);
        if (!save(next, kitsFile)) return false;
        templates = next;
        room.kitEnabled(false);
        plugin.rooms().save();
        return true;
    }

    public boolean pending(Player player) {
        return recovery.isConfigurationSection(playerKey(player));
    }

    /**
     * Returns false (without modifying inventory) on failed recovery write.
     * The caller must cancel the join if this happens.
     */
    public boolean apply(Player player, Room room, Location oldLocation) {
        if (!room.kitEnabled()) return true;
        if (!hasKit(room) || pending(player)) return false;

        String path = playerKey(player);
        capture(recovery, path, player);
        recovery.set(path + ".location", oldLocation);
        if (!save(recovery, recoveryFile)) {
            recovery.set(path, null);
            return false;
        }

        String kit = key(room);
        player.getInventory().setStorageContents(
                items(templates.getList(kit + ".storage"), player.getInventory().getStorageContents().length));
        player.getInventory().setArmorContents(items(templates.getList(kit + ".armor"), 4));
        ItemStack offhand = templates.getItemStack(kit + ".offhand");
        player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
        player.updateInventory();
        return true;
    }

    public boolean restore(Player player, boolean teleport) {
        String path = playerKey(player);
        if (!recovery.isConfigurationSection(path)) return false;
        ItemStack[] storage = items(recovery.getList(path + ".storage"),
                player.getInventory().getStorageContents().length);
        ItemStack[] armor = items(recovery.getList(path + ".armor"), 4);
        ItemStack offhand = recovery.getItemStack(path + ".offhand");
        int level = recovery.getInt(path + ".level");
        int total = recovery.getInt(path + ".total-exp");
        float exp = (float) recovery.getDouble(path + ".exp");
        Location location = recovery.getLocation(path + ".location");

        // Clear the journal BEFORE giving anything back. This prevents replay
        // after a crash halfway through restoration and prioritizes no dupe.
        recovery.set(path, null);
        if (!save(recovery, recoveryFile)) {
            // Keep the journal on disk for the next connection.
            recovery = YamlConfiguration.loadConfiguration(recoveryFile);
            return false;
        }
        player.getInventory().setStorageContents(storage);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand == null ? null : offhand.clone());
        player.setTotalExperience(Math.max(0, total));
        player.setLevel(Math.max(0, level));
        player.setExp(Math.max(0f, Math.min(0.9999f, exp)));
        player.updateInventory();
        if (teleport && location != null && location.getWorld() != null) player.teleport(location);
        return true;
    }

    public void reload() {
        templates = YamlConfiguration.loadConfiguration(kitsFile);
    }

    public void removeRoom(Room room) {
        // Preserve kit file on disk if there are old backups; this only removes template.
        clearKit(room);
    }
}
