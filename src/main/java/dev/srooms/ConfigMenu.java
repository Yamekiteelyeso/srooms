package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigMenu implements Listener {
    private static final Map<Integer, String> EFFECT_SLOTS = new LinkedHashMap<>();
    static {
        EFFECT_SLOTS.put(19, "speed");
        EFFECT_SLOTS.put(20, "strength");
        EFFECT_SLOTS.put(21, "resistance");
        EFFECT_SLOTS.put(22, "regeneration");
        EFFECT_SLOTS.put(23, "jump");
        EFFECT_SLOTS.put(24, "fire_resistance");
        EFFECT_SLOTS.put(25, "haste");
        EFFECT_SLOTS.put(29, "night_vision");
    }

    private static final class MenuHolder implements InventoryHolder {
        final String roomName;
        Inventory inventory;
        MenuHolder(String roomName) { this.roomName = roomName; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private final SRoomsPlugin plugin;
    public ConfigMenu(SRoomsPlugin plugin) { this.plugin = plugin; }

    private ItemStack item(Material material, String title, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(SRoomsPlugin.color(title));
        meta.setLore(java.util.Arrays.stream(lore).map(SRoomsPlugin::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void toggle(Inventory menu, int slot, Material material, String title, boolean enabled, String explanation) {
        menu.setItem(slot, item(material, "&b" + title, "&7" + explanation,
                enabled ? "&aEstado: ACTIVADO" : "&cEstado: DESACTIVADO", "&eClic para cambiar"));
    }

    public void open(Player player, Room room) {
        MenuHolder holder = new MenuHolder(room.name());
        Inventory menu = Bukkit.createInventory(holder, 54, SRoomsPlugin.color("&8SRooms &7» &f" + room.name()));
        holder.inventory = menu;
        ItemStack border = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45) menu.setItem(slot, border);
        }
        toggle(menu, 10, Material.CHEST, "Keep Inventory", room.keepInventory(), "Conserva objetos al morir");
        toggle(menu, 11, Material.EXPERIENCE_BOTTLE, "Keep XP", room.keepExperience(), "Conserva experiencia al morir");
        toggle(menu, 12, Material.IRON_PICKAXE, "Construcción", room.allowBuild(), "Permite colocar y romper durante pelea");
        toggle(menu, 13, Material.COOKED_BEEF, "Hambre congelada", room.lockHunger(), "No perder puntos de hambre");
        toggle(menu, 14, Material.GOLDEN_APPLE, "Curar al entrar", room.healOnJoin(), "Restaura salud y comida al empezar");
        toggle(menu, 15, Material.MILK_BUCKET, "Limpiar efectos", room.clearEffects(), "Quitar pociones anteriores mientras peleas");
        toggle(menu, 16, Material.OAK_DOOR, "Entrada automática", room.walkIn(), "Entrar caminando a la región");
        toggle(menu, 17, Material.PAPER, "Bloquear comandos", room.blockCommands(), "Desactiva comandos durante el duelo");
        toggle(menu, 27, Material.ENDER_PEARL, "Bloquear teletransporte", room.blockEnderPearls(), "Impide ender pearls y chorus fruit");
        toggle(menu, 28, Material.ICE, "Congelar inicio", room.freezeCountdown(), "Impide moverse en la cuenta atrás");
        toggle(menu, 30, Material.LEVER, "Room habilitada", room.enabled(), "Activa o desactiva nuevos combates");
        toggle(menu, 39, Material.DIAMOND, "Recompensa al ganador", room.rewardWinner(),
                "Ejecuta comandos configurados al ganar");
        menu.setItem(41, item(room.kitEnabled() ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE,
                "&bKit de la arena",
                "&7Kit guardado: " + (plugin.kits().hasKit(room) ? "&aSÍ" : "&cNO"),
                "&7Usar kit: " + (room.kitEnabled() ? "&aACTIVADO" : "&cDESACTIVADO"),
                "&7Guardar: &f/srooms kit save " + room.name(),
                "&eClic para alternar"));
        toggle(menu, 44, Material.NETHERITE_SWORD, "Combate Ranked", room.ranked(),
                "Victorias y empates modifican el ELO");
        Material barrier = Material.matchMaterial(room.barrierMaterial());
        if (barrier == null) barrier = Material.GLASS;
        menu.setItem(31, item(barrier, "&bCristal de cierre", "&7Material actual: &f" + room.barrierMaterial(),
                "&eClic para cambiar entre 7 colores"));
        Material[] icons = {Material.SUGAR, Material.BLAZE_POWDER, Material.SHIELD,
                Material.GHAST_TEAR, Material.RABBIT_FOOT, Material.MAGMA_CREAM,
                Material.GOLDEN_PICKAXE, Material.ENDER_EYE};
        int i = 0;
        for (Map.Entry<Integer, String> entry : EFFECT_SLOTS.entrySet()) {
            int level = room.effectLevel(entry.getValue());
            menu.setItem(entry.getKey(), item(icons[i++], "&bEfecto: " + entry.getValue().replace('_', ' '),
                    "&7Nivel: &f" + (level == 0 ? "DESACTIVADO" : level),
                    "&eClic: 0 > I > II > III > 0"));
        }
        menu.setItem(32, item(Material.RED_STAINED_GLASS_PANE, "&cReducir reapertura",
                "&7Actual: &f" + room.reopenDelay() + " segundos",
                "&eClic: -1 segundo", "&eShift + clic: -5 segundos"));
        menu.setItem(33, item(Material.CLOCK, "&b&lTiempo de reapertura",
                "&7Espera despues de anunciar al ganador.",
                "&7Durante la espera la room esta cerrada.",
                "&7Tiempo: &f" + room.reopenDelay() + " segundos",
                "&8Maximo: 300 segundos"));
        menu.setItem(34, item(Material.LIME_STAINED_GLASS_PANE, "&aAumentar reapertura",
                "&7Actual: &f" + room.reopenDelay() + " segundos",
                "&eClic: +1 segundo", "&eShift + clic: +5 segundos"));
        menu.setItem(35, item(Material.RECOVERY_COMPASS, "&c-30 segundos de combate",
                "&7Límite: &f" + room.duelDuration() + " segundos", "&eClic: -30s"));
        menu.setItem(36, item(Material.CLOCK, "&a+30 segundos de combate",
                "&7Límite: &f" + room.duelDuration() + " segundos", "&eClic: +30s"));
        menu.setItem(38, item(Material.REDSTONE, "&c-1 segundo", "&7Countdown actual: &f" + room.countdown() + " s"));
        menu.setItem(40, item(Material.EMERALD, "&a+1 segundo", "&7Countdown actual: &f" + room.countdown() + " s"));
        menu.setItem(42, item(Material.LIME_BED, "&aSpawn A", "&7Ubícate dentro de la zona A", "&eClic para fijar tu posicion"));
        menu.setItem(43, item(Material.RED_BED, "&cSpawn B", "&7Ubícate dentro de la zona B", "&eClic para fijar tu posicion"));
        menu.setItem(49, item(Material.BARRIER, "&cCerrar menú"));
        player.openInventory(menu);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0 || event.getRawSlot() >= 54) return;
        if (!player.hasPermission("srooms.admin")) {
            player.closeInventory(); return;
        }
        Room room = plugin.rooms().get(holder.roomName);
        if (room == null) { player.closeInventory(); return; }
        if (event.getRawSlot() == 49) { player.closeInventory(); return; }
        if (plugin.duels().isOccupied(room)) { plugin.msg(player, "room-occupied"); return; }

        int slot = event.getRawSlot();
        boolean changed = true;
        switch (slot) {
            case 10 -> room.keepInventory(!room.keepInventory());
            case 11 -> room.keepExperience(!room.keepExperience());
            case 12 -> room.allowBuild(!room.allowBuild());
            case 13 -> room.lockHunger(!room.lockHunger());
            case 14 -> room.healOnJoin(!room.healOnJoin());
            case 15 -> room.clearEffects(!room.clearEffects());
            case 16 -> room.walkIn(!room.walkIn());
            case 17 -> room.blockCommands(!room.blockCommands());
            case 27 -> room.blockEnderPearls(!room.blockEnderPearls());
            case 28 -> room.freezeCountdown(!room.freezeCountdown());
            case 30 -> room.enabled(!room.enabled());
            case 39 -> room.rewardWinner(!room.rewardWinner());
            case 44 -> room.ranked(!room.ranked());
            case 41 -> {
                if (!room.kitEnabled() && !plugin.kits().hasKit(room)) {
                    plugin.msg(player, "kit-missing", "room", room.name());
                    changed = false;
                } else room.kitEnabled(!room.kitEnabled());
            }
            case 31 -> room.cycleBarrier();
            case 35 -> room.duelDuration(room.duelDuration() - 30);
            case 36 -> room.duelDuration(room.duelDuration() + 30);
            case 32 -> room.reopenDelay(room.reopenDelay() - (event.isShiftClick() ? 5 : 1));
            case 34 -> room.reopenDelay(room.reopenDelay() + (event.isShiftClick() ? 5 : 1));
            case 38 -> room.countdown(room.countdown() - 1);
            case 40 -> room.countdown(room.countdown() + 1);
            case 42 -> {
                Location pos = player.getLocation();
                if (!room.a().contains(pos)) {
                    plugin.msg(player, "selection-invalid", "reason", "Debes estar dentro de la zona A.");
                    changed = false;
                } else room.spawnA(pos);
            }
            case 43 -> {
                Location pos = player.getLocation();
                if (!room.b().contains(pos)) {
                    plugin.msg(player, "selection-invalid", "reason", "Debes estar dentro de la zona B.");
                    changed = false;
                } else room.spawnB(pos);
            }
            default -> {
                String effect = EFFECT_SLOTS.get(slot);
                if (effect == null) changed = false;
                else room.cycleEffect(effect);
            }
        }
        if (changed) {
            plugin.rooms().save();
            open(player, room);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }
}
