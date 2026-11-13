package dev.srooms;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class SRoomsPlugin extends JavaPlugin {
    private FileConfiguration messages;
    private RoomRegistry registry;
    private SelectionManager selections;
    private DuelManager duels;
    private ConfigMenu menu;
    private StatsManager stats;
    private KitManager kits;
    private MatchHistory history;
    private RoomsBrowser browser;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(getResource("messages.yml")), StandardCharsets.UTF_8)) {
            YamlConfiguration newDefaults = YamlConfiguration.loadConfiguration(reader);
            Map<String, String> legacyMessages = new LinkedHashMap<>();
            legacyMessages.put("prefix", "&8[&bSRooms&8] &7");
            legacyMessages.put("no-permission", "&cNo tienes permiso.");
            legacyMessages.put("only-player", "&cSolo jugadores.");
            legacyMessages.put("unknown-room", "&cEsa room no existe.");
            legacyMessages.put("already-in-room", "&cYa estas en una room.");
            legacyMessages.put("room-busy", "&cEsta room ya esta llena.");
            legacyMessages.put("room-created", "&aRoom &f%room% &acreada.");
            legacyMessages.put("room-deleted", "&cRoom &f%room% &celiminada.");
            legacyMessages.put("room-occupied", "&cLa room esta ocupada.");
            legacyMessages.put("selection-a", "&aMarca los 2 puntos de la zona A.");
            legacyMessages.put("selection-b", "&aZona A lista. Marca la zona B.");
            legacyMessages.put("selection-gate", "&aZona B lista. Marca la barrera de cristal con la nueva hacha.");
            legacyMessages.put("selection-name", "&aCristal listo. Escribe &f/nameroom <nombre>");
            legacyMessages.put("selection-point", "&7Punto %point% en %stage%: &b%coords%");
            legacyMessages.put("selection-incomplete", "&cTermina antes la seleccion.");
            legacyMessages.put("selection-invalid", "&c%reason%");
            legacyMessages.put("selection-ended", "&7Seleccion cancelada.");
            legacyMessages.put("joined-wait", "&eEsperando rival en &f%room%&e...");
            legacyMessages.put("joined-start", "&a¡Rival encontrado!");
            legacyMessages.put("duel-start", "&a¡PELEA!");
            legacyMessages.put("duel-win", "&aGanaste contra &f%loser%&a.");
            legacyMessages.put("duel-lose", "&cPerdiste contra &f%winner%&c.");
            legacyMessages.put("left-room", "&eSaliste de la room.");
            legacyMessages.put("not-in-room", "&cNo estas en una room.");
            legacyMessages.put("gate-unsafe", "&cLa barrera contiene cofres, carteles u otros bloques con datos.");

            // Upgrade unchanged 1.0 texts, but preserve anything edited by the server owner.
            for (Map.Entry<String, String> old : legacyMessages.entrySet()) {
                if (old.getValue().equals(messages.getString(old.getKey())))
                    messages.set(old.getKey(), newDefaults.getString(old.getKey()));
            }
            messages.setDefaults(newDefaults);
            messages.options().copyDefaults(true);
            messages.save(messagesFile);
        } catch (IOException ex) {
            getLogger().warning("No se pudieron actualizar los mensajes: " + ex.getMessage());
        }
        registry = new RoomRegistry(this);
        stats = new StatsManager(this);
        kits = new KitManager(this);
        history = new MatchHistory(this);
        selections = new SelectionManager(this);
        duels = new DuelManager(this);
        menu = new ConfigMenu(this);
        browser = new RoomsBrowser(this);
        RoomCommands commands = new RoomCommands(this);
        for (String name : new String[]{"srooms"}) {
            Objects.requireNonNull(getCommand(name), name + " missing in plugin.yml").setExecutor(commands);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(commands);
        }
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(browser, this);
        getLogger().info("SRooms iniciado con " + registry.all().size() + " rooms.");
    }

    @Override
    public void onDisable() {
        if (duels != null) duels.shutdown();
        if (selections != null) selections.shutdown();
        if (registry != null) registry.save();
        if (stats != null) stats.save();
        if (history != null) history.save();
    }

    public RoomRegistry rooms() { return registry; }
    public SelectionManager selections() { return selections; }
    public DuelManager duels() { return duels; }
    public ConfigMenu menu() { return menu; }
    public StatsManager stats() { return stats; }
    public KitManager kits() { return kits; }
    public MatchHistory history() { return history; }
    public RoomsBrowser browser() { return browser; }

    public void reloadSettings() {
        reloadConfig();
        if (kits != null) kits.reload();
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(getResource("messages.yml")), StandardCharsets.UTF_8)) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (IOException ex) {
            getLogger().warning("No se pudieron recargar los mensajes: " + ex.getMessage());
        }
    }

    public void playSound(org.bukkit.entity.Player player, String setting) {
        if (!getConfig().getBoolean("sounds.enabled", true)) return;
        String name = getConfig().getString("sounds." + setting, "");
        if (name == null || name.isBlank()) return;
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            player.playSound(player.getLocation(), sound,
                    (float) Math.max(0.0, Math.min(2.0, getConfig().getDouble("sounds.volume", 0.85))),
                    (float) Math.max(0.5, Math.min(2.0, getConfig().getDouble("sounds.pitch", 1.0))));
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Sonido desconocido: " + name);
        }
    }

    public void msg(CommandSender target, String key, String... replacements) {
        String value = messages.getString(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2)
            value = value.replace("%" + replacements[i] + "%", replacements[i + 1]);
        target.sendMessage(color(messages.getString("prefix", "&8[&bSRooms&8] &7") + value));
    }

    public String raw(String key, String... replacements) {
        String value = messages.getString(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2)
            value = value.replace("%" + replacements[i] + "%", replacements[i + 1]);
        return color(value);
    }

    public void broadcast(String key, String... replacements) {
        getServer().broadcastMessage(color(messages.getString("prefix", "&8[&bSRooms&8] &7")) + raw(key, replacements));
    }

    public static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
