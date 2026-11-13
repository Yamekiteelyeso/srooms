package dev.srooms;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public final class GameListener implements Listener {
    private final SRoomsPlugin plugin;
    public GameListener(SRoomsPlugin plugin) { this.plugin = plugin; }

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
        if (plugin.selections().isTool(event.getItemDrop().getItemStack())) event.setCancelled(true);
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
        if (plugin.selections().isTool(event.getPlayer().getInventory().getItemInMainHand())
                || !canEdit(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!canEdit(event.getPlayer(), event.getBlockPlaced().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.rooms().at(block.getLocation()) != null);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Room room = plugin.duels().playerRoom(player);
        if (room == null || event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getWorld() == event.getTo().getWorld()) return;
        if (!room.contains(event.getTo())) event.setTo(event.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Room room = plugin.duels().playerRoom(event.getPlayer());
        if (room != null && (event.getTo() == null || !room.contains(event.getTo())))
            event.setCancelled(true);
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
        plugin.selections().cancel(event.getPlayer(), false);
        plugin.duels().leave(event.getPlayer(), true);
    }
}
