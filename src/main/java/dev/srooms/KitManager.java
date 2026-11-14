package dev.srooms;

import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Legacy inventory recovery only. Arena kits were removed in 1.3.5.
 * Existing kit-recovery.yml snapshots must remain restorable after upgrades;
 * no new kit templates or item replacements can be created.
 */
public final class KitManager {
    private final SRoomsPlugin plugin;
    private final File recoveryFile;
    private YamlConfiguration recovery;

    public KitManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        this.recoveryFile = new File(plugin.getDataFolder(), "kit-recovery.yml");
        this.recovery = YamlConfiguration.loadConfiguration(recoveryFile);
    }

    private String playerKey(Player player) {
        return "players." + player.getUniqueId();
    }

    private ItemStack[] items(List<?> list, int size) {
        ItemStack[] result = new ItemStack[size];
        if (list == null) return result;
        for (int i = 0; i < Math.min(list.size(), size); i++)
            if (list.get(i) instanceof ItemStack item) result[i] = item.clone();
        return result;
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

    public boolean pending(Player player) {
        return recovery.isConfigurationSection(playerKey(player));
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
        String modeName = recovery.getString(path + ".game-mode", "SURVIVAL");
        GameMode gameMode;
        try { gameMode = GameMode.valueOf(modeName); }
        catch (IllegalArgumentException ex) { gameMode = GameMode.SURVIVAL; }
        boolean mayFly = recovery.getBoolean(path + ".allow-flight", false);
        boolean wasFlying = recovery.getBoolean(path + ".flying", false);
        double health = recovery.getDouble(path + ".health", player.getHealth());
        int food = recovery.getInt(path + ".food", 20);
        float saturation = (float) recovery.getDouble(path + ".saturation", 5.0);
        float exhaustion = (float) recovery.getDouble(path + ".exhaustion", 0.0);
        int fireTicks = recovery.getInt(path + ".fire-ticks", 0);
        List<PotionEffect> potions = new ArrayList<>();
        for (Object effect : recovery.getList(path + ".potions", List.of())) {
            if (effect instanceof PotionEffect potion) potions.add(potion);
        }

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
        player.setGameMode(gameMode);
        player.setFlying(false);
        player.setAllowFlight(mayFly);
        if (mayFly) player.setFlying(wasFlying);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : potions) player.addPotionEffect(effect);
        player.setFoodLevel(Math.max(0, Math.min(20, food)));
        player.setSaturation(Math.max(0, Math.min(20, saturation)));
        player.setExhaustion(Math.max(0, Math.min(40, exhaustion)));
        player.setFireTicks(Math.max(0, fireTicks));
        if (!player.isDead()) player.setHealth(Math.max(1, Math.min(health, player.getMaxHealth())));
        player.updateInventory();
        if (teleport && location != null && location.getWorld() != null) player.teleport(location);
        return true;
    }

    public void reload() {
        recovery = YamlConfiguration.loadConfiguration(recoveryFile);
    }
}
