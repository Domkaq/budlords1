package com.budlords.effects;

import java.util.Objects;

/**
 * Represents a single effect attached to a strain.
 * Contains the effect type and any additional configuration.
 */
public class StrainEffect {
    
    private final StrainEffectType type;
    private int intensity;  // 1-5, affects strength of effect
    private double chance;  // 0.0-1.0, chance of triggering
    
    public StrainEffect(StrainEffectType type) {
        this.type = type;
        this.intensity = 3;  // Default medium intensity
        this.chance = 1.0;   // Default always triggers
    }
    
    public StrainEffect(StrainEffectType type, int intensity) {
        this.type = type;
        this.intensity = Math.max(1, Math.min(5, intensity));
        this.chance = 1.0;
    }
    
    public StrainEffect(StrainEffectType type, int intensity, double chance) {
        this.type = type;
        this.intensity = Math.max(1, Math.min(5, intensity));
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }
    
    public StrainEffectType getType() {
        return type;
    }
    
    public int getIntensity() {
        return intensity;
    }
    
    public void setIntensity(int intensity) {
        this.intensity = Math.max(1, Math.min(5, intensity));
    }
    
    public double getChance() {
        return chance;
    }
    
    public void setChance(double chance) {
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }
    
    /**
     * Gets a display string for the intensity (stars).
     */
    public String getIntensityDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < intensity) {
                sb.append("§e★");
            } else {
                sb.append("§7☆");
            }
        }
        return sb.toString();
    }
    
    /**
     * Gets a formatted lore line for this effect.
     */
    public String getLoreDisplay() {
        return type.getCategoryColor() + type.getSymbol() + " " + type.getDisplayName() + " " + getIntensityDisplay();
    }
    
    /**
     * Gets a compact display for chat messages.
     */
    public String getCompactDisplay() {
        return type.getSymbol() + type.getCategoryColor() + type.getDisplayName();
    }
    
    /**
     * Calculates the effect duration multiplier based on intensity.
     */
    public double getDurationMultiplier() {
        return 0.5 + (intensity * 0.25); // 0.75x to 1.75x
    }
    
    /**
     * Calculates the particle count multiplier based on intensity.
     */
    public double getParticleMultiplier() {
        return 0.5 + (intensity * 0.3); // 0.8x to 2.0x
    }
    
    /**
     * Calculates the potion effect amplifier based on intensity.
     */
    public int getPotionAmplifier() {
        return Math.max(0, intensity - 2); // 0-3
    }
    
    /**
     * Serializes this effect to a string for storage.
     */
    public String serialize() {
        return type.name() + ":" + intensity + ":" + chance;
    }
    
    /**
     * Deserializes an effect from a stored string.
     */
    public static StrainEffect deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        String[] parts = data.split(":");
        if (parts.length < 1) {
            return null;
        }
        
        try {
            StrainEffectType type = StrainEffectType.valueOf(parts[0]);
            int intensity = parts.length > 1 ? Integer.parseInt(parts[1]) : 3;
            double chance = parts.length > 2 ? Double.parseDouble(parts[2]) : 1.0;
            return new StrainEffect(type, intensity, chance);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Creates a copy of this effect with potentially modified intensity.
     */
    public StrainEffect copy() {
        return new StrainEffect(type, intensity, chance);
    }
    
    /**
     * Creates a mutated copy with slightly altered intensity.
     */
    public StrainEffect mutate() {
        int newIntensity = intensity + (Math.random() < 0.5 ? -1 : 1);
        newIntensity = Math.max(1, Math.min(5, newIntensity));
        return new StrainEffect(type, newIntensity, chance);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrainEffect that = (StrainEffect) o;
        return type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
    
    @Override
    public String toString() {
        return type.getDisplayName() + " (Intensity: " + intensity + ")";
    }
}
