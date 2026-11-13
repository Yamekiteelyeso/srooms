package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RoomsBrowser implements Listener {
    private static final int PAGE_SIZE = 45;
    private final SRoomsPlugin plugin;

    private static final class BrowserHolder implements InventoryHolder {
        final int page;
        final List<String> names;
        Inventory inventory;
        BrowserHolder(int page, List<String> names) {
            this.page = page; this.names = names;
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    public RoomsBrowser(SRoomsPlugin plugin) { this.plugin = plugin; }

    private ItemStack item(Material material, String title, String... lines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SRoomsPlugin.color(title));
        meta.setLore(java.util.Arrays.stream(lines).map(SRoomsPlugin::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player, int requestedPage) {
        List<Room> rooms = plugin.rooms().all().stream()
                .sorted(Comparator.comparing(Room::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int maxPage = rooms.isEmpty() ? 0 : (rooms.size() - 1) / PAGE_SIZE;
        int page = Math.min(Math.max(0, requestedPage), maxPage);
        List<String> names = new ArrayList<>();
        BrowserHolder holder = new BrowserHolder(page, names);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                SRoomsPlugin.color("&8SRooms &7• &f1v1 &8| &b" + (page + 1) + "/" + (maxPage + 1)));
        holder.inventory = inventory;

        int offset = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && offset + i < rooms.size(); i++) {
            Room room = rooms.get(offset + i);
            names.add(room.name());
            int size = plugin.duels().participants(room);
            boolean available = room.enabled() && size < 2 && plugin.duels().reopenRemaining(room) == 0;
            Material icon = !room.enabled() ? Material.BARRIER : available
                    ? Material.LIME_STAINED_GLASS : Material.RED_STAINED_GLASS;
            inventory.setItem(i, item(icon, "&b&l" + room.name(),
                    "&7Jugadores: &f" + size + "/2",
                    "&7Estado: &f" + plugin.duels().state(room),
                    "&7Duración: &f" + room.duelDuration() + " segundos",
                    "&7Modo: " + (room.ranked() ? "&6Ranked" : "&fCasual"),
                    "&7Equipamiento: " + (room.kitEnabled() ? "&bKit de arena" : "&fPropio"),
                    available ? "&aClic para entrar" : "&cNo disponible"));
        }

        if (page > 0)
            inventory.setItem(45, item(Material.ARROW, "&ePágina anterior"));
        if (page < maxPage)
            inventory.setItem(53, item(Material.ARROW, "&ePágina siguiente"));
        inventory.setItem(49, item(Material.BARRIER, "&cCerrar"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof BrowserHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        int slot = event.getRawSlot();
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 45 && holder.page > 0) { open(player, holder.page - 1); return; }
        if (slot == 53) { open(player, holder.page + 1); return; }
        if (slot >= holder.names.size()) return;
        Room room = plugin.rooms().get(holder.names.get(slot));
        if (room == null) { open(player, holder.page); return; }
        player.closeInventory();
        plugin.duels().join(player, room);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BrowserHolder)
            event.setCancelled(true);
    }
}
