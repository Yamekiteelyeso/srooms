package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.TileState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RoomRegistry {
    private final SRoomsPlugin plugin;
    private final File file;
    private final Map<String, Room> rooms = new LinkedHashMap<>();

    public RoomRegistry(SRoomsPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "rooms.yml");
        load();
    }

    public Room get(String name) { return rooms.get(name.toLowerCase(Locale.ROOT)); }
    public Collection<Room> all() { return Collections.unmodifiableCollection(rooms.values()); }

    public Room at(Location location) {
        for (Room room : rooms.values()) if (room.contains(location)) return room;
        return null;
    }

    public void add(Room room) {
        room.walkIn(plugin.getConfig().getBoolean("defaults.walk-in", false));
        room.blockCommands(plugin.getConfig().getBoolean("defaults.block-commands", true));
        room.blockEnderPearls(plugin.getConfig().getBoolean("defaults.block-ender-pearls", true));
        room.freezeCountdown(plugin.getConfig().getBoolean("defaults.freeze-countdown", true));
        room.duelDuration(plugin.getConfig().getInt("defaults.duel-duration-seconds", 300));
        room.barrierMaterial(plugin.getConfig().getString("duel.gate-material", "GLASS"));
        rooms.put(room.name().toLowerCase(Locale.ROOT), room);
        save();
    }

    public void remove(Room room) {
        rooms.remove(room.name().toLowerCase(Locale.ROOT));
        save();
    }

    public String validate(Room room) {
        if (!room.legacy()) {
            if (!room.region().worldName().equals(room.gate().worldName()))
                return "La room y el cristal deben estar en el mismo mundo.";
            if (!room.region().contains(room.gate()))
                return "El cristal debe estar completamente dentro de la región de la room.";
            if (room.region().volume() > plugin.getConfig().getLong("selection.max-zone-volume", 250000L))
                return "La room supera el máximo de bloques permitido.";
        } else {
            if (!room.a().worldName().equals(room.b().worldName())
                    || !room.a().worldName().equals(room.gate().worldName()))
                return "Las regiones guardadas están en mundos distintos.";
            if (room.a().overlaps(room.b()))
                return "La room anterior contiene regiones superpuestas.";
            if (room.a().volume() > plugin.getConfig().getLong("selection.max-zone-volume", 250000L)
                    || room.b().volume() > plugin.getConfig().getLong("selection.max-zone-volume", 250000L))
                return "Una región antigua supera el máximo de bloques.";
        }
        if (room.gate().volume() > plugin.getConfig().getLong("selection.max-gate-volume", 4096L))
            return "El cristal supera el volumen maximo.";
        if (room.a().world() == null) return "El mundo no esta cargado.";
        for (Room other : all()) {
            if (other == room) continue;
            if (other.overlaps(room)) return "Se solapa con la room " + other.name() + ".";
        }
        if (hasTileEntities(room)) return "El cristal contiene cofres, carteles u otros bloques con datos.";
        return null;
    }

    public boolean hasTileEntities(Room room) {
        Cuboid gate = room.gate();
        if (gate.world() == null) return true;
        for (int x = gate.minX(); x <= gate.maxX(); x++)
            for (int y = gate.minY(); y <= gate.maxY(); y++)
                for (int z = gate.minZ(); z <= gate.maxZ(); z++)
                    if (gate.world().getBlockAt(x, y, z).getState() instanceof TileState) return true;
        return false;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("rooms");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) continue;
                Cuboid gate = Cuboid.read(s.getConfigurationSection("gate"));
                Room room;
                if (s.isConfigurationSection("region")) {
                    room = new Room(key, Cuboid.read(s.getConfigurationSection("region")), gate);
                } else {
                    // Read-only backwards compatibility: keep old A/B geometry exactly.
                    room = new Room(key, Cuboid.read(s.getConfigurationSection("a")),
                            Cuboid.read(s.getConfigurationSection("b")), gate);
                }
                room.keepInventory(s.getBoolean("keep-inventory", true));
                room.keepExperience(s.getBoolean("keep-experience", true));
                room.allowBuild(s.getBoolean("allow-build", false));
                room.lockHunger(s.getBoolean("lock-hunger", false));
                room.healOnJoin(s.getBoolean("heal-on-join", true));
                room.clearEffects(s.getBoolean("clear-effects", true));
                room.countdown(s.getInt("countdown", 5));
                room.reopenDelay(s.getInt("reopen-delay", 10));
                room.duelDuration(s.getInt("duel-duration", plugin.getConfig().getInt("defaults.duel-duration-seconds", 300)));
                room.walkIn(s.getBoolean("walk-in", plugin.getConfig().getBoolean("defaults.walk-in", false)));
                room.blockCommands(s.getBoolean("block-commands", plugin.getConfig().getBoolean("defaults.block-commands", true)));
                room.blockEnderPearls(s.getBoolean("block-ender-pearls", plugin.getConfig().getBoolean("defaults.block-ender-pearls", true)));
                room.freezeCountdown(s.getBoolean("freeze-countdown", plugin.getConfig().getBoolean("defaults.freeze-countdown", true)));
                room.enabled(s.getBoolean("enabled", true));
                room.rewardWinner(s.getBoolean("reward-winner", false));
                room.kitEnabled(s.getBoolean("kit-enabled", false));
                room.ranked(s.getBoolean("ranked", false));
                room.barrierMaterial(s.getString("barrier-material", plugin.getConfig().getString("duel.gate-material", "GLASS")));
                Location spawnA = s.getLocation("spawn-a");
                Location spawnB = s.getLocation("spawn-b");
                if (room.validSpawn(spawnA)) room.spawnA(spawnA);
                if (room.validSpawn(spawnB)) room.spawnB(spawnB);
                ConfigurationSection effects = s.getConfigurationSection("effects");
                if (effects != null) for (String effect : effects.getKeys(false)) {
                    int level = effects.getInt(effect);
                    if (Room.EFFECTS.containsKey(effect) && level > 0 && level <= 3)
                        room.effects().put(effect, level);
                }
                if (room.a().world() != null) rooms.put(key.toLowerCase(Locale.ROOT), room);
                else plugin.getLogger().warning("Se omite room '" + key + "': mundo sin cargar.");
            } catch (Exception ex) {
                plugin.getLogger().warning("Room invalida '" + key + "': " + ex.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Room room : all()) {
            String base = "rooms." + room.name();
            if (room.legacy()) {
                room.a().write(yaml, base + ".a");
                room.b().write(yaml, base + ".b");
            } else room.region().write(yaml, base + ".region");
            room.gate().write(yaml, base + ".gate");
            yaml.set(base + ".spawn-a", room.configuredSpawnA());
            yaml.set(base + ".spawn-b", room.configuredSpawnB());
            yaml.set(base + ".keep-inventory", room.keepInventory());
            yaml.set(base + ".keep-experience", room.keepExperience());
            yaml.set(base + ".allow-build", room.allowBuild());
            yaml.set(base + ".lock-hunger", room.lockHunger());
            yaml.set(base + ".heal-on-join", room.healOnJoin());
            yaml.set(base + ".clear-effects", room.clearEffects());
            yaml.set(base + ".countdown", room.countdown());
            yaml.set(base + ".reopen-delay", room.reopenDelay());
            yaml.set(base + ".duel-duration", room.duelDuration());
            yaml.set(base + ".walk-in", room.walkIn());
            yaml.set(base + ".block-commands", room.blockCommands());
            yaml.set(base + ".block-ender-pearls", room.blockEnderPearls());
            yaml.set(base + ".freeze-countdown", room.freezeCountdown());
            yaml.set(base + ".enabled", room.enabled());
            yaml.set(base + ".reward-winner", room.rewardWinner());
            yaml.set(base + ".kit-enabled", room.kitEnabled());
            yaml.set(base + ".ranked", room.ranked());
            yaml.set(base + ".barrier-material", room.barrierMaterial());
            for (Map.Entry<String, Integer> effect : room.effects().entrySet())
                yaml.set(base + ".effects." + effect.getKey(), effect.getValue());
        }
        try { yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("No se pudieron guardar las rooms: " + ex.getMessage()); }
    }
}
