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
    private final Cuboid a, b, gate;
    private Location spawnA, spawnB;
    private boolean keepInventory = true;
    private boolean keepExperience = true;
    private boolean allowBuild = false;
    private boolean lockHunger = false;
    private boolean healOnJoin = true;
    private boolean clearEffects = true;
    private int countdown = 5;
    private int reopenDelay = 10;
    private int duelDuration = 300;
    private boolean walkIn = false;
    private boolean blockCommands = true;
    private boolean blockEnderPearls = true;
    private boolean freezeCountdown = true;
    private boolean enabled = true;
    private boolean rewardWinner = false;
    private boolean kitEnabled = false;
    private String barrierMaterial = "GLASS";
    private final Map<String, Integer> effectLevels = new LinkedHashMap<>();

    public Room(String name, Cuboid a, Cuboid b, Cuboid gate) {
        this.name = name; this.a = a; this.b = b; this.gate = gate;
    }

    public String name() { return name; }
    public Cuboid a() { return a; }
    public Cuboid b() { return b; }
    public Cuboid gate() { return gate; }
    public boolean contains(Location loc) {
        return a.contains(loc) || b.contains(loc) || gate.contains(loc);
    }
    public boolean overlaps(Room other) {
        Cuboid[] first = {a, b, gate};
        Cuboid[] second = {other.a, other.b, other.gate};
        for (Cuboid x : first) for (Cuboid y : second) if (x.overlaps(y)) return true;
        return false;
    }

    public Location spawnA() { return spawnA != null ? spawnA.clone() : a.findSafeSpawn(); }
    public Location spawnB() { return spawnB != null ? spawnB.clone() : b.findSafeSpawn(); }
    public Location configuredSpawnA() { return spawnA == null ? null : spawnA.clone(); }
    public Location configuredSpawnB() { return spawnB == null ? null : spawnB.clone(); }
    public void spawnA(Location value) { spawnA = value == null ? null : value.clone(); }
    public void spawnB(Location value) { spawnB = value == null ? null : value.clone(); }

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
    public void countdown(int seconds) { countdown = Math.max(1, Math.min(30, seconds)); }
    public int reopenDelay() { return reopenDelay; }
    public void reopenDelay(int seconds) { reopenDelay = Math.max(0, Math.min(300, seconds)); }
    public int duelDuration() { return duelDuration; }
    public void duelDuration(int value) { duelDuration = Math.max(30, Math.min(1800, value)); }
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
    public boolean kitEnabled() { return kitEnabled; }
    public void kitEnabled(boolean value) { kitEnabled = value; }
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
