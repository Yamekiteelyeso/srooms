package dev.srooms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.block.Block;

import java.util.Objects;

public final class Cuboid {
    private final String worldName;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public Cuboid(Location one, Location two) {
        if (one.getWorld() == null || two.getWorld() == null ||
                !one.getWorld().getUID().equals(two.getWorld().getUID())) {
            throw new IllegalArgumentException("Los puntos deben pertenecer al mismo mundo.");
        }
        worldName = one.getWorld().getName();
        minX = Math.min(one.getBlockX(), two.getBlockX());
        maxX = Math.max(one.getBlockX(), two.getBlockX());
        minY = Math.min(one.getBlockY(), two.getBlockY());
        maxY = Math.max(one.getBlockY(), two.getBlockY());
        minZ = Math.min(one.getBlockZ(), two.getBlockZ());
        maxZ = Math.max(one.getBlockZ(), two.getBlockZ());
    }

    private Cuboid(String worldName, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        this.worldName = Objects.requireNonNull(worldName);
        this.minX = minX; this.maxX = maxX;
        this.minY = minY; this.maxY = maxY;
        this.minZ = minZ; this.maxZ = maxZ;
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Coordenadas de cuboide invalidas");
        }
    }

    public static Cuboid read(ConfigurationSection section) {
        if (section == null) throw new IllegalArgumentException("Falta una region.");
        return new Cuboid(section.getString("world", ""),
                section.getInt("min-x"), section.getInt("max-x"),
                section.getInt("min-y"), section.getInt("max-y"),
                section.getInt("min-z"), section.getInt("max-z"));
    }

    public void write(FileConfiguration config, String path) {
        config.set(path + ".world", worldName);
        config.set(path + ".min-x", minX); config.set(path + ".max-x", maxX);
        config.set(path + ".min-y", minY); config.set(path + ".max-y", maxY);
        config.set(path + ".min-z", minZ); config.set(path + ".max-z", maxZ);
    }

    public String worldName() { return worldName; }
    public World world() { return Bukkit.getWorld(worldName); }
    public int minX() { return minX; } public int maxX() { return maxX; }
    public int minY() { return minY; } public int maxY() { return maxY; }
    public int minZ() { return minZ; } public int maxZ() { return maxZ; }

    /**
     * Ground-level corner clicks select a horizontal footprint. For arenas
     * whose selection is only 1–2 blocks tall, grow upward so players'
     * standing coordinates and both automatic spawn locations fit inside.
     * Explicitly taller cuboids keep their exact vertical bounds.
     */
    public Cuboid withMinimumArenaHeight(int requiredHeight) {
        int current = maxY - minY + 1;
        if (current > 2 || requiredHeight <= current) return this;
        World loaded = world();
        if (loaded == null) return this;
        int target = Math.max(3, Math.min(64, requiredHeight));
        int top = Math.min(loaded.getMaxHeight() - 1, minY + target - 1);
        return new Cuboid(worldName, minX, maxX, minY, top, minZ, maxZ);
    }

    public long volume() {
        return ((long) maxX - minX + 1L) * ((long) maxY - minY + 1L)
                * ((long) maxZ - minZ + 1L);
    }

    public boolean contains(Location location) {
        return location != null && location.getWorld() != null
                && worldName.equals(location.getWorld().getName())
                && location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }

    public boolean contains(Cuboid other) {
        return worldName.equals(other.worldName)
                && minX <= other.minX && maxX >= other.maxX
                && minY <= other.minY && maxY >= other.maxY
                && minZ <= other.minZ && maxZ >= other.maxZ;
    }

    public boolean overlaps(Cuboid other) {
        return worldName.equals(other.worldName)
                && minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public Location findSafeSpawn() {
        World w = world();
        if (w == null) return null;
        int x = minX + (maxX - minX) / 2;
        int z = minZ + (maxZ - minZ) / 2;
        for (int y = Math.min(maxY - 1, w.getMaxHeight() - 2); y >= Math.max(minY, w.getMinHeight() + 1); y--) {
            Block ground = w.getBlockAt(x, y - 1, z);
            if (ground.getType().isSolid()
                    && w.getBlockAt(x, y, z).isPassable()
                    && w.getBlockAt(x, y + 1, z).isPassable()) {
                return new Location(w, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }
}
