package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import java.util.HashSet;
import java.util.Set;
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
        final GameMode gameMode;
        final boolean allowFlight, flying;
        final float saturation, exhaustion;
        final Collection<PotionEffect> potions;
        Snapshot(Player player) {
            location = player.getLocation().clone();
            gameMode = player.getGameMode();
            allowFlight = player.getAllowFlight();
            flying = player.isFlying();
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
        final String firstName;
        UUID second;
        String secondName;
        Phase phase = Phase.WAITING;
        BukkitTask waitingTask;
        BukkitTask countdownTask;
        BukkitTask combatTask;
        BossBar timerBar;
        final Map<String, BlockData> changedBlocks = new LinkedHashMap<>();
        final Set<UUID> projectiles = new HashSet<>();
        Match(Room room, Player first) {
            this.room = room;
            this.first = first.getUniqueId();
            this.firstName = first.getName();
        }
    }

    private final SRoomsPlugin plugin;
    private final Map<String, Match> matches = new HashMap<>();
    private final Map<UUID, Match> players = new HashMap<>();
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();
    private final Map<UUID, Snapshot> pendingRespawns = new HashMap<>();
    private final Map<String, Long> reopeningAt = new HashMap<>();
    private final Map<String, BukkitTask> reopeningTasks = new HashMap<>();
    private final Set<UUID> joining = new HashSet<>();
    private final File recoveryFile;

    public DuelManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        recoveryFile = new File(plugin.getDataFolder(), "gate-recovery.yml");
        restoreAllRecovery();
    }

    public boolean recordWorldEdit(Player player, Block block, BlockData original) {
        Match match = players.get(player.getUniqueId());
        if (match == null || match.phase != Phase.FIGHT || !match.room.allowBuild()
                || !match.room.contains(block.getLocation()) || match.room.gate().contains(block.getLocation())
                || block.getState() instanceof TileState) return false;
        String key = block.getX() + "_" + block.getY() + "_" + block.getZ();
        if (!match.changedBlocks.containsKey(key)) {
            int maximum = Math.max(1, plugin.getConfig().getInt("protection.max-edited-blocks", 4096));
            if (match.changedBlocks.size() >= maximum) return false;
            match.changedBlocks.put(key, original.clone());
        }
        return true;
    }

    private void restoreWorldEdits(Match match) {
        if (match.changedBlocks.isEmpty()) return;
        World world = match.room.a().world();
        if (world == null) return;
        for (Map.Entry<String, BlockData> edit : match.changedBlocks.entrySet()) {
            try {
                String[] xyz = edit.getKey().split("_");
                world.getBlockAt(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]),
                        Integer.parseInt(xyz[2])).setBlockData(edit.getValue(), false);
            } catch (Exception ex) {
                plugin.getLogger().warning("No se pudo restaurar bloque en " + edit.getKey() + ": " + ex.getMessage());
            }
        }
        match.changedBlocks.clear();
    }

    public void recordProjectile(Player player, Projectile projectile) {
        Match match = players.get(player.getUniqueId());
        if (match != null) match.projectiles.add(projectile.getUniqueId());
    }

    private void clearProjectiles(Match match) {
        World world = match.room.a().world();
        if (world == null) return;
        for (UUID projectileId : match.projectiles) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(projectileId);
            if (entity != null && entity.getWorld().equals(world)) entity.remove();
        }
        match.projectiles.clear();
    }

    public boolean forceReset(Room room) {
        String key = roomKey(room);
        Match match = matches.get(key);
        if (match != null) {
            match.phase = Phase.WAITING; // A forced reset must never reward a winner or broadcast a draw.
            finish(match, null, false);
            return true;
        }
        BukkitTask task = reopeningTasks.remove(key);
        boolean reopening = reopeningAt.remove(key) != null;
        if (task != null) task.cancel();
        if (reopening) {
            restoreGate(room);
            return true;
        }
        return repairGate(room);
    }

    public boolean isPlaying(Player player) { return players.containsKey(player.getUniqueId()); }
    public boolean isJoining(Player player) { return joining.contains(player.getUniqueId()); }
    public Room playerRoom(Player player) {
        Match match = players.get(player.getUniqueId());
        return match == null ? null : match.room;
    }
    public boolean isOccupied(Room room) { return matches.containsKey(roomKey(room)) || reopeningAt.containsKey(roomKey(room)); }
    private String roomKey(Room room) { return room.name().toLowerCase(java.util.Locale.ROOT); }
    public int reopenRemaining(Room room) {
        Long until = reopeningAt.get(roomKey(room));
        return until == null ? 0 : Math.max(1, (int) ((until - System.currentTimeMillis() + 999L) / 1000L));
    }
    public int participants(Room room) {
        Match match = matches.get(room.name().toLowerCase(java.util.Locale.ROOT));
        return match == null ? 0 : match.second == null ? 1 : 2;
    }
    public String state(Room room) {
        Match match = matches.get(room.name().toLowerCase(java.util.Locale.ROOT));
        if (reopeningAt.containsKey(roomKey(room))) return "REAPERTURA " + reopenRemaining(room) + "s";
        return match == null ? "LIBRE" : match.phase.name();
    }
    public boolean isCountingDown(Player player) {
        Match match = players.get(player.getUniqueId());
        return match != null && match.phase == Phase.COUNTDOWN;
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
        if (!room.enabled()) { plugin.msg(player, "room-disabled", "room", room.name()); return; }
        if (isPlaying(player)) { plugin.msg(player, "already-in-room"); return; }
        if (plugin.kits().pending(player)) {
            plugin.msg(player, "kit-recovery-pending");
            return;
        }
        if (room.kitEnabled() && !plugin.kits().hasKit(room)) {
            plugin.msg(player, "kit-missing", "room", room.name());
            return;
        }
        String key = roomKey(room);
        if (reopeningAt.containsKey(key)) {
            plugin.msg(player, "room-reopening", "room", room.name(),
                    "seconds", String.valueOf(reopenRemaining(room)));
            return;
        }
        Match match = matches.get(key);
        if (match != null && match.second != null) {
            plugin.msg(player, "room-busy", "room", room.name());
            return;
        }
        Location spawn = match == null ? room.spawnA() : room.spawnB();
        if (spawn == null || spawn.getWorld() == null || !room.contains(spawn) || room.gate().contains(spawn)) {
            plugin.msg(player, "selection-invalid", "reason", "Spawn inválido. Configúralo con /srooms roomconfig.");
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
        joining.add(player.getUniqueId());
        boolean teleported;
        try {
            teleported = player.teleport(spawn);
        } finally {
            joining.remove(player.getUniqueId());
        }
        if (!teleported) {
            if (match.second == null && !players.containsKey(match.first)) {
                matches.remove(key);
                restoreGate(room);
            }
            plugin.msg(player, "selection-invalid", "reason", "No fue posible teletransportarte al spawn.");
            return;
        }
        boolean kitApplied;
        try {
            kitApplied = plugin.kits().apply(player, room, beforeTeleport.location);
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("Error aplicando kit de " + room.name() + ": " + ex.getMessage());
            plugin.kits().restore(player, false);
            kitApplied = false;
        }
        if (!kitApplied) {
            player.teleport(beforeTeleport.location);
            if (match.first.equals(player.getUniqueId()) && !players.containsKey(match.first)) {
                matches.remove(key);
                restoreGate(room);
            }
            plugin.msg(player, "kit-apply-failed");
            return;
        }
        snapshots.put(player.getUniqueId(), beforeTeleport);
        players.put(player.getUniqueId(), match);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        plugin.playSound(player, "join");
        applyRoomSettings(player, room);
        ejectIntruders(room);
        if (match.first.equals(player.getUniqueId())) {
            plugin.msg(player, "joined-wait", "room", room.name());
            startWaitingTimer(match);
        } else {
            match.second = player.getUniqueId();
            match.secondName = player.getName();
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

    private void startWaitingTimer(Match match) {
        int seconds = Math.max(0, Math.min(3600,
                plugin.getConfig().getInt("duel.waiting-timeout-seconds", 120)));
        if (seconds == 0) return;
        match.waitingTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (matches.get(roomKey(match.room)) != match || match.second != null) return;
            Player first = Bukkit.getPlayer(match.first);
            if (first != null) plugin.msg(first, "waiting-expired", "room", match.room.name());
            finish(match, null, false);
        }, seconds * 20L);
    }

    private void startCountdown(Match match) {
        if (match.waitingTask != null) match.waitingTask.cancel();
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
                    startCombatTimer(match);
                    plugin.msg(a, "duel-start");
                    plugin.msg(b, "duel-start");
                    plugin.playSound(a, "start");
                    plugin.playSound(b, "start");
                    a.sendTitle(SRoomsPlugin.color("&a&l¡PELEA!"), "", 0, 30, 10);
                    b.sendTitle(SRoomsPlugin.color("&a&l¡PELEA!"), "", 0, 30, 10);
                    cancel();
                    return;
                }
                String title = SRoomsPlugin.color("&b&l" + remaining);
                a.sendTitle(title, SRoomsPlugin.color("&7Prepárate"), 0, 24, 0);
                b.sendTitle(title, SRoomsPlugin.color("&7Prepárate"), 0, 24, 0);
                plugin.playSound(a, "countdown");
                plugin.playSound(b, "countdown");
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startCombatTimer(Match match) {
        int max = match.room.duelDuration();
        if (plugin.getConfig().getBoolean("duel.bossbar.enabled", true)) {
            BossBar bar = Bukkit.createBossBar(plugin.raw("timer-title",
                            "seconds", String.valueOf(max)),
                    BarColor.BLUE, BarStyle.SOLID);
            match.timerBar = bar;
            Player first = Bukkit.getPlayer(match.first);
            Player second = Bukkit.getPlayer(match.second);
            if (first != null) bar.addPlayer(first);
            if (second != null) bar.addPlayer(second);
        }
        match.combatTask = new BukkitRunnable() {
            int remaining = max;
            @Override
            public void run() {
                if (matches.get(roomKey(match.room)) != match || match.phase != Phase.FIGHT) {
                    cancel();
                    return;
                }
                if (remaining <= 0) {
                    cancel();
                    finish(match, null, false);
                    return;
                }
                if (match.timerBar != null) {
                    match.timerBar.setTitle(plugin.raw("timer-title", "seconds", String.valueOf(remaining)));
                    match.timerBar.setProgress(Math.max(0.0, Math.min(1.0, (double) remaining / max)));
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private Material gateMaterial(Room room) {
        Material material = Material.matchMaterial(room.barrierMaterial());
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
        Material material = gateMaterial(room);
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++)
                    world.getBlockAt(x, y, z).setType(material, false);
        return true;
    }

    private void sealGateForReopen(Room room) {
        Cuboid gate = room.gate();
        World world = gate.world();
        if (world == null) return;
        Material glass = gateMaterial(room);
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++)
                    world.getBlockAt(x, y, z).setType(glass, false);
    }

    private void beginReopen(Room room) {
        int seconds = room.reopenDelay();
        if (seconds <= 0) {
            restoreGate(room);
            if (plugin.getConfig().getBoolean("announcements.broadcast-ready", true))
                plugin.broadcast("room-ready", "room", room.name());
            return;
        }
        String key = roomKey(room);
        sealGateForReopen(room);
        reopeningAt.put(key, System.currentTimeMillis() + seconds * 1000L);
        if (plugin.getConfig().getBoolean("announcements.broadcast-ready", true))
            plugin.broadcast("room-cooldown", "room", room.name(), "seconds", String.valueOf(seconds));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reopeningTasks.remove(key);
            reopeningAt.remove(key);
            restoreGate(room);
            if (plugin.getConfig().getBoolean("announcements.broadcast-ready", true))
                plugin.broadcast("room-ready", "room", room.name());
        }, seconds * 20L);
        reopeningTasks.put(key, task);
    }

    public void ejectIntruders(Room room) {
        if (!isOccupied(room)) return;
        World world = room.a().world();
        if (world == null) return;
        for (Player online : world.getPlayers()) {
            if (room.contains(online.getLocation()) && playerRoom(online) != room) {
                online.teleport(exitLocation(room));
                plugin.msg(online, "intruder-ejected", "room", room.name());
            }
        }
    }

    public Location exitLocation(Room room) {
        World world = room.a().world();
        if (world == null) return null;
        Location configured = plugin.getConfig().getLocation("access.exit-spawn");
        if (configured != null && configured.getWorld() != null && !room.contains(configured))
            return configured;
        Location spawn = world.getSpawnLocation();
        if (!room.contains(spawn)) return spawn;
        int x = Math.max(room.a().maxX(), Math.max(room.b().maxX(), room.gate().maxX())) + 12;
        int z = Math.max(room.a().maxZ(), Math.max(room.b().maxZ(), room.gate().maxZ())) + 12;
        int y = world.getHighestBlockYAt(x, z) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
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

    /**
     * Restore the recorded original glass-area blocks after a previous crash.
     * This never changes an active fight or its cooldown.
     */
    public boolean repairGate(Room room) {
        if (isOccupied(room)) return false;
        YamlConfiguration recovery = YamlConfiguration.loadConfiguration(recoveryFile);
        String path = "gates." + room.name();
        if (!recovery.isConfigurationSection(path + ".blocks")) return false;
        restoreGateFrom(recovery, room.name());
        if (!saveRecovery(recovery)) return false;
        return !recovery.isConfigurationSection(path + ".blocks");
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
        boolean keep = room.keepInventory() || room.kitEnabled();
        event.setKeepInventory(keep);
        if (keep) {
            event.getDrops().clear();
        }
        event.setKeepLevel(room.keepExperience() || room.kitEnabled());
        if (room.keepExperience() || room.kitEnabled()) {
            event.setDroppedExp(0);
        }
        finish(match, dead.getUniqueId(), true);
    }

    private void finish(Match match, UUID loser, boolean died) {
        String key = roomKey(match.room);
        if (matches.get(key) != match) return;
        matches.remove(key);
        if (match.waitingTask != null) match.waitingTask.cancel();
        if (match.countdownTask != null) match.countdownTask.cancel();
        if (match.combatTask != null) match.combatTask.cancel();
        if (match.timerBar != null) {
            match.timerBar.removeAll();
            match.timerBar = null;
        }
        boolean played = match.phase == Phase.FIGHT;
        UUID winner = DuelOutcome.winner(played, match.first, match.second, loser);
        if (loser != null && winner != null) {
            Player win = Bukkit.getPlayer(winner), lose = Bukkit.getPlayer(loser);
            String winnerName = winner.equals(match.first) ? match.firstName : match.secondName;
            String loserName = loser.equals(match.first) ? match.firstName : match.secondName;
            if (played) {
                plugin.stats().result(winner, winnerName, loser, loserName, match.room.ranked());
                rewardWinner(match.room, winnerName, loserName);
            }
            if (plugin.getConfig().getBoolean("announcements.broadcast-results", true))
                plugin.broadcast("duel-result", "room", match.room.name(),
                        "winner", winnerName, "loser", loserName);
            if (win != null) {
                plugin.msg(win, "duel-win", "loser", loserName);
                plugin.playSound(win, "win");
                win.sendTitle(plugin.raw("victory-title"), plugin.raw("victory-subtitle",
                        "loser", loserName), 5, 50, 10);
            }
            if (lose != null && !died) {
                plugin.msg(lose, "duel-lose", "winner", winnerName);
                plugin.playSound(lose, "lose");
                lose.sendTitle(plugin.raw("defeat-title"), plugin.raw("defeat-subtitle",
                        "winner", winnerName), 5, 50, 10);
            }
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
            if (online != null) {
                restorePlayer(online, snapshot, true);
                if (plugin.kits().pending(online)) {
                    if (!plugin.kits().restore(online, false))
                        plugin.msg(online, "kit-recovery-pending");
                }
            }
        }
        restoreWorldEdits(match);
        clearProjectiles(match);
        if (winner == null && played) {
            plugin.stats().draw(match.first, match.firstName, match.second, match.secondName,
                    match.room.ranked());
            if (plugin.getConfig().getBoolean("announcements.broadcast-results", true))
                plugin.broadcast("duel-draw", "room", match.room.name(),
                        "a", match.firstName, "b", match.secondName);
            beginReopen(match.room);
        } else if (winner == null) {
            restoreGate(match.room);
            for (UUID id : new UUID[]{match.first, match.second}) {
                if (id == null) continue;
                Player online = Bukkit.getPlayer(id);
                if (online != null && online.isOnline() && !online.isDead()) {
                    plugin.msg(online, "duel-cancelled");
                }
            }
        } else beginReopen(match.room);
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        Snapshot snapshot = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (snapshot == null) return;
        event.setRespawnLocation(snapshot.location);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                restorePlayer(event.getPlayer(), snapshot, false);
                if (plugin.kits().pending(event.getPlayer()))
                    plugin.kits().restore(event.getPlayer(), false);
            }
        });
    }

    private void rewardWinner(Room room, String winner, String loser) {
        if (!room.rewardWinner() || !plugin.getConfig().getBoolean("rewards.enabled", false)) return;
        int count = 0;
        for (String template : plugin.getConfig().getStringList("rewards.winner-commands")) {
            if (++count > 20) break;
            String command = template.replace("%winner%", winner)
                    .replace("%loser%", loser).replace("%room%", room.name()).trim();
            if (command.startsWith("/")) command = command.substring(1);
            if (!command.isEmpty()) {
                try { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command); }
                catch (Exception ex) { plugin.getLogger().warning("Recompensa fallida: " + ex.getMessage()); }
            }
        }
    }

    private void restorePlayer(Player player, Snapshot snapshot, boolean teleport) {
        if (teleport) player.teleport(snapshot.location);
        player.setGameMode(snapshot.gameMode);
        player.setAllowFlight(snapshot.allowFlight);
        if (snapshot.allowFlight) player.setFlying(snapshot.flying);
        for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
        for (PotionEffect effect : snapshot.potions) player.addPotionEffect(effect);
        player.setFoodLevel(snapshot.food);
        player.setSaturation(snapshot.saturation);
        player.setExhaustion(snapshot.exhaustion);
        player.setFireTicks(snapshot.fireTicks);
        if (!player.isDead()) player.setHealth(Math.max(1, Math.min(snapshot.health, player.getMaxHealth())));
    }

    public void shutdown() {
        for (BukkitTask task : reopeningTasks.values()) task.cancel();
        reopeningTasks.clear();
        reopeningAt.clear();
        for (Match match : new ArrayList<>(matches.values())) finish(match, null, false);
        restoreAllRecovery();
    }

}
