package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DuelManager {
    public enum Phase { WAITING, COUNTDOWN, FIGHT }
    private static final class Snapshot {
        final Location location;
        final double health;
        final int food, fireTicks;
        final float saturation, exhaustion;
        final Collection<PotionEffect> potions;
        Snapshot(Player player) {
            location = player.getLocation().clone();
            health = player.getHealth();
            food = player.getFoodLevel();
            fireTicks = player.getFireTicks();
            saturation = player.getSaturation();
            exhaustion = player.getExhaustion();
            potions = new ArrayList<>(player.getActivePotionEffects());
        }
    }
    private static final class Match {
        final Room room;
        final UUID first;
        UUID second;
        Phase phase = Phase.WAITING;
        BukkitTask countdownTask;
        Match(Room room, Player first) { this.room = room; this.first = first.getUniqueId(); }
    }

    private final SRoomsPlugin plugin;
    private final Map<String, Match> matches = new HashMap<>();
    private final Map<UUID, Match> players = new HashMap<>();
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();
    private final Map<UUID, Snapshot> pendingRespawns = new HashMap<>();
    private final File recoveryFile;

    public DuelManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        recoveryFile = new File(plugin.getDataFolder(), "gate-recovery.yml");
        restoreAllRecovery();
    }

    public boolean isPlaying(Player player) { return players.containsKey(player.getUniqueId()); }
    public Room playerRoom(Player player) {
        Match match = players.get(player.getUniqueId());
        return match == null ? null : match.room;
    }
    public boolean isOccupied(Room room) { return matches.containsKey(room.name().toLowerCase(java.util.Locale.ROOT)); }
    public int participants(Room room) {
        Match match = matches.get(room.name().toLowerCase(java.util.Locale.ROOT));
        return match == null ? 0 : match.second == null ? 1 : 2;
    }
    public String state(Room room) {
        Match match = matches.get(room.name().toLowerCase(java.util.Locale.ROOT));
        return match == null ? "LIBRE" : match.phase.name();
    }
    public boolean isFighting(Player player) {
        Match match = players.get(player.getUniqueId());
        return match != null && match.phase == Phase.FIGHT;
    }
    public boolean allowPvp(Player attacker, Player victim) {
        Match match = players.get(attacker.getUniqueId());
        return match != null && match == players.get(victim.getUniqueId())
                && match.phase == Phase.FIGHT;
    }

    public void join(Player player, Room room) {
        if (isPlaying(player)) { plugin.msg(player, "already-in-room"); return; }
        String key = room.name().toLowerCase(java.util.Locale.ROOT);
        Match match = matches.get(key);
        if (match != null && match.second != null) { plugin.msg(player, "room-busy"); return; }
        Location spawn = match == null ? room.spawnA() : room.spawnB();
        if (spawn == null || spawn.getWorld() == null || !room.contains(spawn) || room.gate().contains(spawn)) {
            plugin.msg(player, "selection-invalid", "reason", "Spawn invalido. Configuralo con /roomconfig.");
            return;
        }
        if (match == null) {
            if (plugin.rooms().hasTileEntities(room)) {
                plugin.msg(player, "gate-unsafe");
                return;
            }
            if (!closeGate(room)) {
                plugin.msg(player, "selection-invalid", "reason", "No se pudo guardar la barrera. Comprueba el disco.");
                return;
            }
            match = new Match(room, player);
            matches.put(key, match);
        }
        Snapshot beforeTeleport = new Snapshot(player);
        if (!player.teleport(spawn)) {
            if (match.second == null && !players.containsKey(match.first)) {
                matches.remove(key);
                restoreGate(room);
            }
            plugin.msg(player, "selection-invalid", "reason", "No fue posible teletransportarte al spawn.");
            return;
        }
        snapshots.put(player.getUniqueId(), beforeTeleport);
        players.put(player.getUniqueId(), match);
        applyRoomSettings(player, room);
        if (match.first.equals(player.getUniqueId())) {
            plugin.msg(player, "joined-wait", "room", room.name());
        } else {
            match.second = player.getUniqueId();
            plugin.msg(player, "joined-start");
            Player first = Bukkit.getPlayer(match.first);
            if (first != null) plugin.msg(first, "joined-start");
            startCountdown(match);
        }
    }

    private void applyRoomSettings(Player player, Room room) {
        if (room.clearEffects()) {
            for (PotionEffect existing : player.getActivePotionEffects()) player.removePotionEffect(existing.getType());
        }
        if (room.healOnJoin()) {
            player.setHealth(Math.max(1.0, player.getMaxHealth()));
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.setFireTicks(0);
        }
        for (Map.Entry<String, Integer> effect : room.effects().entrySet()) {
            PotionEffectType type = Room.EFFECTS.get(effect.getKey());
            if (type != null && effect.getValue() > 0)
                player.addPotionEffect(new PotionEffect(type, 20 * 60 * 60, effect.getValue() - 1, false, false, true));
        }
    }

    private void startCountdown(Match match) {
        match.phase = Phase.COUNTDOWN;
        match.countdownTask = new BukkitRunnable() {
            int remaining = match.room.countdown();
            @Override
            public void run() {
                if (!matches.containsValue(match)) { cancel(); return; }
                Player a = Bukkit.getPlayer(match.first);
                Player b = Bukkit.getPlayer(match.second);
                if (a == null || b == null) {
                    if (a == null) leaveById(match.first);
                    else leaveById(match.second);
                    cancel();
                    return;
                }
                if (remaining == 0) {
                    match.phase = Phase.FIGHT;
                    openGate(match.room);
                    plugin.msg(a, "duel-start");
                    plugin.msg(b, "duel-start");
                    a.sendTitle(SRoomsPlugin.color("&a&l¡PELEA!"), "", 0, 30, 10);
                    b.sendTitle(SRoomsPlugin.color("&a&l¡PELEA!"), "", 0, 30, 10);
                    cancel();
                    return;
                }
                String title = SRoomsPlugin.color("&b&l" + remaining);
                a.sendTitle(title, SRoomsPlugin.color("&7Prepárate"), 0, 24, 0);
                b.sendTitle(title, SRoomsPlugin.color("&7Prepárate"), 0, 24, 0);
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private Material gateMaterial() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("duel.gate-material", "GLASS"));
        return material != null && material.isBlock() && material.name().endsWith("GLASS")
                ? material : Material.GLASS;
    }

    private boolean closeGate(Room room) {
        Cuboid gate = room.gate();
        World world = gate.world();
        if (world == null) return false;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(recoveryFile);
        String path = "gates." + room.name();
        yaml.set(path + ".world", world.getName());
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    yaml.set(path + ".blocks." + x + "_" + y + "_" + z, block.getBlockData().getAsString());
                }
        if (!saveRecovery(yaml)) return false;
        Material material = gateMaterial();
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++)
                    world.getBlockAt(x, y, z).setType(material, false);
        return true;
    }

    private void openGate(Room room) {
        Cuboid gate = room.gate();
        World world = gate.world();
        if (world == null) return;
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++)
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
    }

    private boolean saveRecovery(YamlConfiguration yaml) {
        try { yaml.save(recoveryFile); return true; }
        catch (IOException ex) { plugin.getLogger().severe("No se pudo guardar la barrera: " + ex.getMessage()); return false; }
    }

    private void restoreAllRecovery() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(recoveryFile);
        ConfigurationSection gates = yaml.getConfigurationSection("gates");
        if (gates == null) return;
        for (String roomName : new ArrayList<>(gates.getKeys(false))) restoreGateFrom(yaml, roomName);
        saveRecovery(yaml);
    }

    private void restoreGate(Room room) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(recoveryFile);
        restoreGateFrom(yaml, room.name());
        saveRecovery(yaml);
    }

    private void restoreGateFrom(YamlConfiguration yaml, String roomName) {
        String path = "gates." + roomName;
        ConfigurationSection data = yaml.getConfigurationSection(path + ".blocks");
        if (data == null) return;
        World world = Bukkit.getWorld(yaml.getString(path + ".world", ""));
        if (world == null) {
            plugin.getLogger().warning("Barrera sin restaurar (mundo no cargado): " + roomName);
            return;
        }
        boolean success = true;
        for (String coords : data.getKeys(false)) {
            try {
                String[] values = coords.split("_");
                if (values.length != 3) throw new IllegalArgumentException("Coordenadas invalidas");
                int x = Integer.parseInt(values[0]), y = Integer.parseInt(values[1]), z = Integer.parseInt(values[2]);
                world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(data.getString(coords, "minecraft:air")), false);
            } catch (Exception ex) {
                success = false;
                plugin.getLogger().warning("Error restaurando cristal " + roomName + " " + coords + ": " + ex.getMessage());
            }
        }
        if (success) yaml.set(path, null);
    }

    private void leaveById(UUID uuid) {
        Match match = players.get(uuid);
        if (match != null) finish(match, uuid, false);
    }

    public void leave(Player player, boolean quitting) {
        Match match = players.get(player.getUniqueId());
        if (match == null) { if (!quitting) plugin.msg(player, "not-in-room"); return; }
        boolean contested = match.second != null;
        finish(match, contested ? player.getUniqueId() : null, false);
        if (!quitting && !contested) plugin.msg(player, "left-room");
    }

    public void handleDeath(Player dead, PlayerDeathEvent event) {
        Match match = players.get(dead.getUniqueId());
        if (match == null) return;
        Room room = match.room;
        event.setDeathMessage(null);
        event.setKeepInventory(room.keepInventory());
        if (room.keepInventory()) {
            event.getDrops().clear();
        }
        event.setKeepLevel(room.keepExperience());
        if (room.keepExperience()) {
            event.setDroppedExp(0);
        }
        finish(match, dead.getUniqueId(), true);
    }

    private void finish(Match match, UUID loser, boolean died) {
        String key = match.room.name().toLowerCase(java.util.Locale.ROOT);
        if (matches.get(key) != match) return;
        matches.remove(key);
        if (match.countdownTask != null) match.countdownTask.cancel();
        restoreGate(match.room);
        UUID winner = loser == null ? null : loser.equals(match.first) ? match.second : match.first;
        if (loser != null && winner != null) {
            Player win = Bukkit.getPlayer(winner), lose = Bukkit.getPlayer(loser);
            String winnerName = win == null ? "rival" : win.getName();
            String loserName = lose == null ? "rival" : lose.getName();
            if (win != null) plugin.msg(win, "duel-win", "loser", loserName);
            if (lose != null && !died) plugin.msg(lose, "duel-lose", "winner", winnerName);
        }
        for (UUID id : new UUID[]{match.first, match.second}) {
            if (id == null) continue;
            players.remove(id);
            Snapshot snapshot = snapshots.remove(id);
            if (snapshot == null) continue;
            if (died && id.equals(loser)) {
                pendingRespawns.put(id, snapshot);
                continue;
            }
            Player online = Bukkit.getPlayer(id);
            if (online != null) restorePlayer(online, snapshot, true);
        }
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        Snapshot snapshot = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        event.setRespawnLocation(snapshot.location);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) restorePlayer(event.getPlayer(), snapshot, false);
        });
    }

    private void restorePlayer(Player player, Snapshot snapshot, boolean teleport) {
        if (teleport) player.teleport(snapshot.location);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        for (PotionEffect effect : snapshot.potions) player.addPotionEffect(effect);
        player.setFoodLevel(snapshot.food);
        player.setSaturation(snapshot.saturation);
        player.setExhaustion(snapshot.exhaustion);
        player.setFireTicks(snapshot.fireTicks);
        if (!player.isDead()) player.setHealth(Math.max(1, Math.min(snapshot.health, player.getMaxHealth())));
    }

    public void shutdown() {
        for (Match match : new ArrayList<>(matches.values())) finish(match, null, false);
        restoreAllRecovery();
    }

}
