package dev.srooms;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class SRoomsPlugin extends JavaPlugin {
    private FileConfiguration messages;
    private RoomRegistry registry;
    private SelectionManager selections;
    private DuelManager duels;
    private ConfigMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        registry = new RoomRegistry(this);
        selections = new SelectionManager(this);
        duels = new DuelManager(this);
        menu = new ConfigMenu(this);
        RoomCommands commands = new RoomCommands(this);
        for (String name : new String[]{"srooms"}) {
            Objects.requireNonNull(getCommand(name), name + " missing in plugin.yml").setExecutor(commands);
            Objects.requireNonNull(getCommand(name)).setTabCompleter(commands);
        }
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(menu, this);
        getLogger().info("SRooms iniciado con " + registry.all().size() + " rooms.");
    }

    @Override
    public void onDisable() {
        if (duels != null) duels.shutdown();
        if (selections != null) selections.shutdown();
        if (registry != null) registry.save();
    }

    public RoomRegistry rooms() { return registry; }
    public SelectionManager selections() { return selections; }
    public DuelManager duels() { return duels; }
    public ConfigMenu menu() { return menu; }

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
