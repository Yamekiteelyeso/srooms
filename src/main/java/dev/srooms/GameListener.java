package dev.srooms;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GameListener implements Listener {
    private final SRoomsPlugin plugin;
    private final Map<UUID, Long> intruderWarnings = new HashMap<>();
    public GameListener(SRoomsPlugin plugin) { this.plugin = plugin; }

    private void warnIntruder(Player player, Room room) {
        long now = System.currentTimeMillis();
        long last = intruderWarnings.getOrDefault(player.getUniqueId(), 0L);
        if (now - last >= 2500L) {
            intruderWarnings.put(player.getUniqueId(), now);
            plugin.msg(player, "intruder-ejected", "room", room.name());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSelect(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (!plugin.selections().isTool(item)) return;
        event.setCancelled(true);
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        plugin.selections().click(event.getPlayer(), event.getAction(), event.getClickedBlock(), item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Room room = plugin.duels().playerRoom(event.getPlayer());
        if (plugin.selections().isTool(event.getItemDrop().getItemStack())
                || room != null && room.kitEnabled()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            Room room = plugin.duels().playerRoom(player);
            if (room != null && room.kitEnabled()) event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Room room = plugin.duels().playerRoom(player);
        if (room != null && room.kitEnabled()
                && plugin.getConfig().getBoolean("kits.block-containers", true)
                && event.getInventory().getType() != InventoryType.CRAFTING
                && event.getInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            plugin.msg(player, "kit-containers-blocked");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player)
            plugin.duels().recordProjectile(player, event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.selections().isTool(event.getCurrentItem())
                || plugin.selections().isTool(event.getCursor())
                || event.getHotbarButton() >= 0
                && plugin.selections().isTool(player.getInventory().getItem(event.getHotbarButton())))
            event.setCancelled(true);
    }

    private boolean canEdit(Player player, Location position) {
        Room room = plugin.rooms().at(position);
        if (room == null) return true;
        if (plugin.duels().isOccupied(room)) {
            return room.allowBuild()
                    && plugin.duels().playerRoom(player) == room
                    && plugin.duels().isFighting(player)
                    && !room.gate().contains(position);
        }
        return player.hasPermission("srooms.admin");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.selections().isTool(player.getInventory().getItemInMainHand())
                || !canEdit(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.duels().playerRoom(player) != null) {
            if (!plugin.duels().recordWorldEdit(player, event.getBlock(), event.getBlock().getBlockData())) {
                event.setCancelled(true);
                plugin.msg(player, "build-limit");
                return;
            }
            event.setDropItems(false);
            event.setExpToDrop(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (plugin.rooms().at(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation()) != null)
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!canEdit(player, event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.duels().playerRoom(player) != null
                && !plugin.duels().recordWorldEdit(player, event.getBlockPlaced(),
                        event.getBlockReplacedState().getBlockData())) {
            event.setCancelled(true);
            plugin.msg(player, "build-limit");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        if (plugin.rooms().at(event.getToBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null
                || plugin.rooms().at(event.getBlock().getRelative(event.getDirection()).getLocation()) != null
                || event.getBlocks().stream().anyMatch(block ->
                    plugin.rooms().at(block.getLocation()) != null
                            || plugin.rooms().at(block.getRelative(event.getDirection()).getLocation()) != null))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null
                || plugin.rooms().at(event.getBlock().getRelative(event.getDirection()).getLocation()) != null
                || event.getBlocks().stream().anyMatch(block ->
                    plugin.rooms().at(block.getLocation()) != null
                            || plugin.rooms().at(block.getRelative(event.getDirection().getOppositeFace()).getLocation()) != null))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (plugin.rooms().at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.rooms().at(block.getLocation()) != null);
    }

    /**
     * Only the two registered duelists may occupy arena space. Administrators
     * can visit an idle room to configure spawns and regions.
     */
    private boolean shouldDenyEntry(Player player, Room room) {
        if (room == null || plugin.duels().playerRoom(player) == room) return false;
        if (plugin.duels().isOccupied(room)) return true;
        if (player.hasPermission("srooms.admin")) return false;
        return plugin.getConfig().getBoolean("access.protect-empty-rooms", true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null || plugin.duels().isJoining(player)) return;
        Room duringCountdown = plugin.duels().playerRoom(player);
        if (duringCountdown != null && duringCountdown.freezeCountdown()
                && plugin.duels().isCountingDown(player)
                && event.getFrom().getWorld().equals(event.getTo().getWorld())
                && event.getFrom().distanceSquared(event.getTo()) > 0.00001) {
            Location frozen = event.getFrom().clone();
            frozen.setYaw(event.getTo().getYaw());
            frozen.setPitch(event.getTo().getPitch());
            event.setTo(frozen);
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getWorld() == event.getTo().getWorld()) return;
        Room participating = plugin.duels().playerRoom(player);
        if (participating != null) {
            if (!participating.contains(event.getTo())) {
                // Waiting players can walk back out; during countdown and
                // combat the arena remains closed to avoid invalid results.
                if (plugin.getConfig().getBoolean("access.walk-out-while-waiting", true)
                        && !plugin.duels().isCountingDown(player)
                        && !plugin.duels().isFighting(player)) {
                    plugin.duels().leave(player, false);
                    event.setTo(player.getLocation());
                } else event.setTo(event.getFrom());
            }
            return;
        }
        Room entered = plugin.rooms().at(event.getTo());
        if (entered != null && !plugin.selections().isSelecting(player)) {
            boolean crossedBoundary = !entered.contains(event.getFrom());
            // Staff can crouch to inspect/setup an idle arena without joining.
            boolean editing = player.hasPermission("srooms.admin") && player.isSneaking();
            // Guests who were already inside a room when it was configured
            // or reloaded can join by moving, without leaving and re-entering.
            boolean entryMovement = crossedBoundary || !player.hasPermission("srooms.admin");
            if (entryMovement && !editing) {
                // Walking inside is always the way to join (even when older
                // rooms.yml files had walk-in=false).
                if (entered.enabled() && player.hasPermission("srooms.use")
                        && plugin.duels().participants(entered) < 2
                        && plugin.duels().reopenRemaining(entered) == 0) {
                    plugin.duels().join(player, entered);
                    if (plugin.duels().playerRoom(player) == entered)
                        event.setTo(player.getLocation());
                    else event.setTo(event.getFrom());
                } else {
                    event.setTo(event.getFrom());
                    warnIntruder(player, entered);
                }
                return;
            }
        }
        if (shouldDenyEntry(player, entered)) {
            if (entered.contains(event.getFrom())) {
                Location exit = plugin.duels().exitLocation(entered);
                if (exit != null) event.setTo(exit);
                else event.setTo(event.getFrom());
            } else event.setTo(event.getFrom());
            warnIntruder(player, entered);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (plugin.duels().isJoining(player)) return;
        Room room = plugin.duels().playerRoom(player);
        if (room != null && (event.getTo() == null || !room.contains(event.getTo())
                || (room.blockEnderPearls() && (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT)))) {
            event.setCancelled(true);
            return;
        }
        if (room == null && event.getTo() != null) {
            Room destination = plugin.rooms().at(event.getTo());
            if (shouldDenyEntry(player, destination)) {
                event.setCancelled(true);
                warnIntruder(player, destination);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommands(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Room room = plugin.duels().playerRoom(player);
        if (room == null || !room.blockCommands() || player.hasPermission("srooms.admin")) return;
        String typed = event.getMessage().trim().toLowerCase(java.util.Locale.ROOT);
        if (typed.startsWith("/")) typed = typed.substring(1);
        String[] split = typed.split("\\s+");
        if (split.length == 0) return;
        String label = split[0];
        if (label.contains(":")) label = label.substring(label.indexOf(':') + 1);
        String sub = split.length > 1 ? split[1] : "";
        boolean allowed = (label.equals("srooms") && sub.equals("leave"));
        for (String entry : plugin.getConfig().getStringList("restrictions.allowed-commands")) {
            String token = entry.toLowerCase(java.util.Locale.ROOT).trim();
            if (token.startsWith("/")) token = token.substring(1);
            if (token.equals(label) || token.equals(label + " " + sub)) allowed = true;
        }
        if (!allowed) {
            event.setCancelled(true);
            plugin.msg(player, "commands-locked");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPearl(PlayerInteractEvent event) {
        Room room = plugin.duels().playerRoom(event.getPlayer());
        if (room == null || !room.blockEnderPearls()) return;
        ItemStack used = event.getItem();
        if (used != null && (used.getType() == Material.ENDER_PEARL || used.getType() == Material.CHORUS_FRUIT)) {
            event.setCancelled(true);
            plugin.msg(event.getPlayer(), "pearls-locked");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.kits().pending(player)) {
            if (plugin.kits().restore(player, true))
                plugin.msg(player, "kit-recovered");
            else plugin.msg(player, "kit-recovery-pending");
        }
        Room occupied = plugin.rooms().at(player.getLocation());
        if (shouldDenyEntry(player, occupied)) {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && shouldDenyEntry(player, occupied)) {
                    Location exit = plugin.duels().exitLocation(occupied);
                    if (exit != null) player.teleport(exit);
                    warnIntruder(player, occupied);
                }
            });
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (plugin.duels().isPlaying(event.getPlayer())) plugin.duels().leave(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player p ? p : null;
        Player attacker = sourcePlayer(event.getDamager());
        boolean involved = (victim != null && plugin.duels().isPlaying(victim))
                || (attacker != null && plugin.duels().isPlaying(attacker));
        if (!involved) return;
        if (attacker == null || victim == null || !plugin.duels().allowPvp(attacker, victim))
            event.setCancelled(true);
    }

    private Player sourcePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.duels().isPlaying(player) && !plugin.duels().isFighting(player))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Room room = plugin.duels().playerRoom(player);
        if (room != null && room.lockHunger()) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(plugin.selections()::isTool);
        plugin.selections().cancel(event.getEntity(), false);
        plugin.duels().handleDeath(event.getEntity(), event);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.duels().handleRespawn(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        intruderWarnings.remove(event.getPlayer().getUniqueId());
        plugin.selections().cancel(event.getPlayer(), false);
        plugin.duels().leave(event.getPlayer(), true);
    }
}
