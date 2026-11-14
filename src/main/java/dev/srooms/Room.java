package dev.srooms;

import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Room {
    public static final Map<String, PotionEffectType> EFFECTS = createEffects();

    private static Map<String, PotionEffectType> createEffects() {
        Map<String, PotionEffectType> effects = new LinkedHashMap<>();
        effects.put("speed", PotionEffectType.SPEED);
        effects.put("strength", PotionEffectType.INCREASE_DAMAGE);
        effects.put("resistance", PotionEffectType.DAMAGE_RESISTANCE);
        effects.put("regeneration", PotionEffectType.REGENERATION);
        effects.put("jump", PotionEffectType.JUMP);
        effects.put("fire_resistance", PotionEffectType.FIRE_RESISTANCE);
        effects.put("haste", PotionEffectType.FAST_DIGGING);
        effects.put("night_vision", PotionEffectType.NIGHT_VISION);
        return java.util.Collections.unmodifiableMap(effects);
    }

    private final String name;
    // The new format stores ONE arena region plus its glass selection.
    // Legacy rooms retain the old two regions internally without expanding
    // their bounds or changing saved arena coordinates.
    private final Cuboid region, legacyA, legacyB, gate;
    private boolean keepInventory = false;
    private boolean keepExperience = false;
    private boolean allowBuild = false;
    private boolean lockHunger = false;
    private boolean healOnJoin = false;
    private boolean clearEffects = false;
    private int countdown = 0;
    private int reopenDelay = 0;
    private int duelDuration = 300;
    private boolean walkIn = true;
    private boolean teleportOnJoin = false;
    private boolean openCrystalOnFight = false;
    private boolean blockCommands = false;
    private boolean blockEnderPearls = false;
    private boolean freezeCountdown = false;
    private boolean enabled = true;
    private boolean rewardWinner = false;
    private String barrierMaterial = "GLASS";
    private final Map<String, Integer> effectLevels = new LinkedHashMap<>();

    public Room(String name, Cuboid region, Cuboid gate) {
        this.name = name;
        this.region = region;
        this.legacyA = null;
        this.legacyB = null;
        this.gate = gate;
    }

    /** For rooms.yml files saved before version 1.3.1. */
    public Room(String name, Cuboid a, Cuboid b, Cuboid gate) {
        this.name = name;
        this.region = null;
        this.legacyA = a;
        this.legacyB = b;
        this.gate = gate;
    }

    public String name() { return name; }
    public boolean legacy() { return region == null; }
    public Cuboid region() { return region; }
    // Compatibility accessors for the existing duel/world routines.
    public Cuboid a() { return legacy() ? legacyA : region; }
    public Cuboid b() { return legacy() ? legacyB : region; }
    public Cuboid gate() { return gate; }

    public boolean contains(Location loc) {
        return legacy() ? legacyA.contains(loc) || legacyB.contains(loc) || gate.contains(loc)
                : region.contains(loc);
    }

    private Cuboid[] occupiedRegions() {
        return legacy() ? new Cuboid[]{legacyA, legacyB, gate} : new Cuboid[]{region};
    }

    public boolean overlaps(Room other) {
        for (Cuboid x : occupiedRegions())
            for (Cuboid y : other.occupiedRegions())
                if (x.overlaps(y)) return true;
        return false;
    }

    public boolean validSpawn(Location spawn) {
        return spawn != null && spawn.getWorld() != null
                && contains(spawn) && !gate.contains(spawn);
    }

    private Location automaticSpawn(boolean first) {
        if (legacy()) return (first ? legacyA : legacyB).findSafeSpawn();
        ArenaSplit.Partition partition = ArenaSplit.choose(
                region.minX(), region.maxX(), region.minZ(), region.maxZ(),
                gate.minX(), gate.maxX(), gate.minZ(), gate.maxZ());
        if (!partition.valid() || region.world() == null) return null;
        int minX = partition.alongX() ? (first ? partition.firstMin() : partition.secondMin()) : region.minX();
        int maxX = partition.alongX() ? (first ? partition.firstMax() : partition.secondMax()) : region.maxX();
        int minZ = partition.alongX() ? region.minZ() : (first ? partition.firstMin() : partition.secondMin());
        int maxZ = partition.alongX() ? region.maxZ() : (first ? partition.firstMax() : partition.secondMax());
        Cuboid side = new Cuboid(
                new Location(region.world(), minX, region.minY(), minZ),
                new Location(region.world(), maxX, region.maxY(), maxZ));
        Location position = side.findSafeSpawn();
        return validSpawn(position) ? position : null;
    }

    // Spawns are automatic and only used by the optional teleport toggle.
    // Arena owners never need to set player 1/2 locations manually.
    public Location spawnA() { return automaticSpawn(true); }
    public Location spawnB() { return automaticSpawn(false); }

    public boolean keepInventory() { return keepInventory; }
    public void keepInventory(boolean value) { keepInventory = value; }
    public boolean keepExperience() { return keepExperience; }
    public void keepExperience(boolean value) { keepExperience = value; }
    public boolean allowBuild() { return allowBuild; }
    public void allowBuild(boolean value) { allowBuild = value; }
    public boolean lockHunger() { return lockHunger; }
    public void lockHunger(boolean value) { lockHunger = value; }
    public boolean healOnJoin() { return healOnJoin; }
    public void healOnJoin(boolean value) { healOnJoin = value; }
    public boolean clearEffects() { return clearEffects; }
    public void clearEffects(boolean value) { clearEffects = value; }
    public int countdown() { return countdown; }
    public void countdown(int seconds) { countdown = Math.max(0, Math.min(30, seconds)); }
    public int reopenDelay() { return reopenDelay; }
    public void reopenDelay(int seconds) { reopenDelay = Math.max(0, Math.min(300, seconds)); }
    public int duelDuration() { return duelDuration; }
    public void duelDuration(int value) { duelDuration = Math.max(30, Math.min(1800, value)); }
    public boolean openCrystalOnFight() { return openCrystalOnFight; }
    public void openCrystalOnFight(boolean value) { openCrystalOnFight = value; }
    public boolean teleportOnJoin() { return teleportOnJoin; }
    public void teleportOnJoin(boolean value) { teleportOnJoin = value; }
    public boolean walkIn() { return walkIn; }
    public void walkIn(boolean value) { walkIn = value; }
    public boolean blockCommands() { return blockCommands; }
    public void blockCommands(boolean value) { blockCommands = value; }
    public boolean blockEnderPearls() { return blockEnderPearls; }
    public void blockEnderPearls(boolean value) { blockEnderPearls = value; }
    public boolean freezeCountdown() { return freezeCountdown; }
    public void freezeCountdown(boolean value) { freezeCountdown = value; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean value) { enabled = value; }
    public boolean rewardWinner() { return rewardWinner; }
    public void rewardWinner(boolean value) { rewardWinner = value; }
    public String barrierMaterial() { return barrierMaterial; }
    public void barrierMaterial(String value) {
        org.bukkit.Material material = org.bukkit.Material.matchMaterial(value == null ? "" : value);
        if (material != null && material.isBlock()
                && (material.name().endsWith("GLASS") || material.name().endsWith("STAINED_GLASS")))
            barrierMaterial = material.name();
    }
    public void cycleBarrier() {
        String[] choices = {"GLASS", "RED_STAINED_GLASS", "BLUE_STAINED_GLASS",
            "GREEN_STAINED_GLASS", "BLACK_STAINED_GLASS", "WHITE_STAINED_GLASS", "PURPLE_STAINED_GLASS"};
        int index = java.util.Arrays.asList(choices).indexOf(barrierMaterial);
        barrierMaterial = choices[(index + 1) % choices.length];
    }
    public Map<String, Integer> effects() { return effectLevels; }
    public int effectLevel(String key) { return effectLevels.getOrDefault(key, 0); }
    public void cycleEffect(String key) {
        int next = (effectLevel(key) + 1) % 4;
        if (next == 0) effectLevels.remove(key);
        else effectLevels.put(key, next);
    }
}
