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
        // One-time upgrade: previous versions shipped several options ON
        // by default. Leave existing individual room settings untouched,
        // but switch the template for NEW rooms to all-off as requested.
        if (!getConfig().getBoolean("migration.defaults-1-3-3", false)) {
            for (String key : new String[]{
                    "keep-inventory", "keep-experience", "allow-build",
                    "lock-hunger", "heal-on-join", "clear-effects",
                    "block-commands", "block-ender-pearls",
                    "freeze-countdown", "reward-winner"}) {
                getConfig().set("defaults." + key, false);
            }
            getConfig().set("defaults.walk-in", true);
            getConfig().set("defaults.reopen-delay-seconds", 0);
            getConfig().set("migration.defaults-1-3-3", true);
        }
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
            // Update only exact bundled 1.3.4 messages. Owners who wrote
            // custom text keep it unchanged.
            Map<String, String> previousDefaults = new LinkedHashMap<>();
            previousDefaults.put("help-player", "&aPara entrar a una room simplemente camina dentro de ella. &7/srooms gui &8• &fmenú &8| &7/srooms leave &8• &fsalir &8| &7/srooms stats &8• &fstats &8| &7/srooms top elo &8• &franking");
            previousDefaults.put("help-admin-two", "&7/srooms kit save <room> &8• &fguardar kit &8| &7/srooms setexit &8• &fsalida &8| &7/srooms reset <room> &8• &freiniciar &8| &7/srooms check <room> &8• &fcomprobar &8| &7/srooms reload &8• &frecargar");
            previousDefaults.put("room-info", "&b%room% &8» &7%players%/2 &8| &7%status% &8| &7Keep: &f%keep% &8| &7Reabre: &f%seconds%s &8| &7Límite: &f%duration%s &8| &7Ranked: &f%ranked% &8| &7Kit: &f%kit%");
            previousDefaults.put("stats-line", "&aVictorias: &f%wins% &8| &cDerrotas: &f%losses% &8| &eEmpates: &f%draws% &8| &6ELO: &f%elo% &8| &eRacha: &f%streak% &8| &7Mejor: &f%best% &8| &bWinrate: &f%rate%%");
            previousDefaults.put("room-check-ok", "&aLa room &f%room% &apasó las comprobaciones: región y cristal (spawns solo si hay TP).");
            previousDefaults.put("room-spawns-invalid", "&cLa room &f%room% &cno tiene dos spawns válidos y separados. &7Configúralos en &f/srooms roomconfig %room%&7.");
            for (Map.Entry<String, String> entry : previousDefaults.entrySet()) {
                if (entry.getValue().equals(messages.getString(entry.getKey())))
                    messages.set(entry.getKey(), newDefaults.getString(entry.getKey()));
            }
            for (String removed : new String[]{
                    "usage-kit", "kit-saved", "kit-cleared", "kit-info",
                    "kit-missing", "kit-save-failed", "kit-apply-failed",
                    "kit-containers-blocked", "top-elo-title", "top-elo-entry",
                    "room-spawns-needed", "room-walk-in"}) {
                messages.set(removed, null);
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
