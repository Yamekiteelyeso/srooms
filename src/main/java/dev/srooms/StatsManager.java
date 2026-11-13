package dev.srooms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class StatsManager {
    public static final class Record {
        private final UUID id;
        private String name;
        private int wins, losses, draws, streak, bestStreak;
        private int rating = 1000;

        private Record(UUID id, String name) {
            this.id = id; this.name = name;
        }

        public UUID id() { return id; }
        public String name() { return name; }
        public int wins() { return wins; }
        public int losses() { return losses; }
        public int draws() { return draws; }
        public int rating() { return rating; }
        public int streak() { return streak; }
        public int bestStreak() { return bestStreak; }
        public int fights() { return wins + losses + draws; }
        public double winRate() { return fights() == 0 ? 0.0 : 100.0 * wins / fights(); }
    }

    private final SRoomsPlugin plugin;
    private final File file;
    private final Map<UUID, Record> players = new HashMap<>();

    public StatsManager(SRoomsPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private Record entry(UUID uuid, String name) {
        Record record = players.computeIfAbsent(uuid, key -> new Record(uuid, name));
        if (name != null && !name.isBlank()) record.name = name;
        return record;
    }

    public Record find(String playerName) {
        for (Record record : players.values()) {
            if (record.name.equalsIgnoreCase(playerName)) return record;
        }
        return null;
    }

    public Record get(Player player) { return entry(player.getUniqueId(), player.getName()); }

    public List<Record> top(int limit) {
        return players.values().stream()
                .sorted(Comparator.comparingInt(Record::wins).reversed()
                        .thenComparing(Comparator.comparingInt(Record::bestStreak).reversed())
                        .thenComparing(Record::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit).toList();
    }

    public List<Record> topRating(int limit) {
        return players.values().stream()
                .sorted(Comparator.comparingInt(Record::rating).reversed()
                        .thenComparing(Comparator.comparingInt(Record::wins).reversed())
                        .thenComparing(Record::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit).toList();
    }

    public void result(UUID winnerId, String winnerName, UUID loserId, String loserName, boolean ranked) {
        if (!plugin.getConfig().getBoolean("stats.enabled", true)) return;
        Record winner = entry(winnerId, winnerName);
        Record loser = entry(loserId, loserName);
        if (ranked) {
            int k = Math.max(1, Math.min(128, plugin.getConfig().getInt("ranked.k-factor", 32)));
            int delta = EloRating.winDelta(winner.rating, loser.rating, k);
            winner.rating = EloRating.clamp(winner.rating + delta);
            loser.rating = EloRating.clamp(loser.rating - delta);
        }
        winner.wins++;
        winner.streak++;
        winner.bestStreak = Math.max(winner.bestStreak, winner.streak);
        loser.losses++;
        loser.streak = 0;
        save();
    }

    public void draw(UUID firstId, String firstName, UUID secondId, String secondName, boolean ranked) {
        if (!plugin.getConfig().getBoolean("stats.enabled", true)) return;
        Record first = entry(firstId, firstName);
        Record second = entry(secondId, secondName);
        first.draws++;
        second.draws++;
        first.streak = 0;
        second.streak = 0;
        if (ranked) {
            int k = Math.max(1, Math.min(128, plugin.getConfig().getInt("ranked.k-factor", 32)));
            int delta = EloRating.expectedDelta(first.rating, second.rating, k, 0.5);
            first.rating = EloRating.clamp(first.rating + delta);
            second.rating = EloRating.clamp(second.rating - delta);
        }
        save();
    }

    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String base = "players." + key + ".";
                Record record = new Record(id, yaml.getString(base + "name", key));
                record.wins = Math.max(0, yaml.getInt(base + "wins"));
                record.losses = Math.max(0, yaml.getInt(base + "losses"));
                record.draws = Math.max(0, yaml.getInt(base + "draws"));
                record.rating = EloRating.clamp(yaml.getInt(base + "rating", 1000));
                record.streak = Math.max(0, yaml.getInt(base + "streak"));
                record.bestStreak = Math.max(record.streak, yaml.getInt(base + "best-streak"));
                players.put(id, record);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Registro estadístico inválido: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Record record : players.values()) {
            String base = "players." + record.id + ".";
            yaml.set(base + "name", record.name);
            yaml.set(base + "wins", record.wins);
            yaml.set(base + "losses", record.losses);
            yaml.set(base + "draws", record.draws);
            yaml.set(base + "rating", record.rating);
            yaml.set(base + "streak", record.streak);
            yaml.set(base + "best-streak", record.bestStreak);
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("No se pudieron guardar stats.yml: " + ex.getMessage());
        }
    }
}
