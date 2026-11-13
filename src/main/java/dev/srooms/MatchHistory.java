package dev.srooms;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, persistent history of completed 1v1 matches. */
public final class MatchHistory {
    public record Entry(String room, String first, String second, String winner, long when) {
        public boolean draw() { return winner == null || winner.isBlank(); }
    }

    private final SRoomsPlugin plugin;
    private final File file;
    private final List<Entry> entries = new ArrayList<>();

    public MatchHistory(SRoomsPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "history.yml");
        load();
    }

    private void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> row : yaml.getMapList("matches")) {
            Object timestamp = row.get("when");
            if (!(timestamp instanceof Number number)) continue;
            String room = String.valueOf(row.getOrDefault("room", ""));
            String first = String.valueOf(row.getOrDefault("first", ""));
            String second = String.valueOf(row.getOrDefault("second", ""));
            String winner = String.valueOf(row.getOrDefault("winner", ""));
            if (room.isBlank() || first.isBlank() || second.isBlank()) continue;
            entries.add(new Entry(room, first, second, winner, number.longValue()));
        }
        trim();
    }

    private void trim() {
        int max = Math.max(20, Math.min(2000, plugin.getConfig().getInt("history.max-entries", 200)));
        while (entries.size() > max) entries.remove(0);
    }

    public List<Entry> recent(String room, int limit) {
        List<Entry> result = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && result.size() < limit; i--) {
            Entry entry = entries.get(i);
            if (room == null || entry.room().equalsIgnoreCase(room)) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    public void record(Room room, String first, String second, String winner) {
        entries.add(new Entry(room.name(), first, second, winner, System.currentTimeMillis()));
        trim();
        save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Entry entry : entries) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("room", entry.room());
            row.put("first", entry.first());
            row.put("second", entry.second());
            row.put("winner", entry.winner() == null ? "" : entry.winner());
            row.put("when", entry.when());
            rows.add(row);
        }
        yaml.set("matches", rows);
        try { yaml.save(file); }
        catch (IOException ex) {
            plugin.getLogger().severe("No se pudo guardar history.yml: " + ex.getMessage());
        }
    }
}
