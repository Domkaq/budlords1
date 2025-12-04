package com.budlords.strain;

import com.budlords.effects.StrainEffect;
import com.budlords.effects.StrainEffectType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Strain {

    private final String id;
    private String name;
    private Rarity rarity;
    private int potency;
    private int yield;
    private int packagingQuality;
    private Material iconMaterial;
    private List<StrainEffect> effects;
    
    // Maximum number of effects a strain can have
    public static final int MAX_EFFECTS = 5;

    public Strain(String id, String name, Rarity rarity, int potency, int yield, int packagingQuality) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.potency = potency;
        this.yield = yield;
        this.packagingQuality = packagingQuality;
        this.iconMaterial = Material.GREEN_DYE;
        this.effects = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }

    public int getPotency() {
        return potency;
    }

    public void setPotency(int potency) {
        this.potency = Math.max(1, Math.min(100, potency));
    }

    public int getYield() {
        return yield;
    }

    public void setYield(int yield) {
        this.yield = Math.max(1, Math.min(20, yield));
    }

    public int getPackagingQuality() {
        return packagingQuality;
    }

    public void setPackagingQuality(int packagingQuality) {
        this.packagingQuality = Math.max(1, Math.min(100, packagingQuality));
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public void setIconMaterial(Material iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
    
    // ===== EFFECTS MANAGEMENT =====
    
    /**
     * Gets the list of effects for this strain.
     */
    public List<StrainEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }
    
    /**
     * Sets the effects for this strain.
     */
    public void setEffects(List<StrainEffect> effects) {
        this.effects = new ArrayList<>(effects);
        // Limit to MAX_EFFECTS
        if (this.effects.size() > MAX_EFFECTS) {
            this.effects = new ArrayList<>(this.effects.subList(0, MAX_EFFECTS));
        }
    }
    
    /**
     * Adds an effect to this strain if there's room.
     */
    public boolean addEffect(StrainEffect effect) {
        if (effects.size() >= MAX_EFFECTS) {
            return false;
        }
        // Don't add duplicate effect types
        for (StrainEffect existing : effects) {
            if (existing.getType() == effect.getType()) {
                return false;
            }
        }
        effects.add(effect);
        return true;
    }
    
    /**
     * Removes an effect by type.
     */
    public boolean removeEffect(StrainEffectType type) {
        return effects.removeIf(e -> e.getType() == type);
    }
    
    /**
     * Checks if this strain has a specific effect.
     */
    public boolean hasEffect(StrainEffectType type) {
        return effects.stream().anyMatch(e -> e.getType() == type);
    }
    
    /**
     * Gets a specific effect by type, or null if not present.
     */
    public StrainEffect getEffect(StrainEffectType type) {
        return effects.stream()
            .filter(e -> e.getType() == type)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Clears all effects.
     */
    public void clearEffects() {
        effects.clear();
    }
    
    /**
     * Gets a display string for all effects.
     */
    public String getEffectsDisplay() {
        if (effects.isEmpty()) {
            return "§7None";
        }
        return effects.stream()
            .map(StrainEffect::getCompactDisplay)
            .collect(Collectors.joining("§7, "));
    }
    
    /**
     * Gets lore lines for all effects.
     */
    public List<String> getEffectsLore() {
        List<String> lore = new ArrayList<>();
        if (effects.isEmpty()) {
            lore.add("§7No special effects");
        } else {
            for (StrainEffect effect : effects) {
                lore.add(effect.getLoreDisplay());
            }
        }
        return lore;
    }
    
    /**
     * Checks if this strain has any legendary effects.
     */
    public boolean hasLegendaryEffect() {
        return effects.stream().anyMatch(e -> e.getType().isLegendary());
    }
    
    /**
     * Gets the number of effects.
     */
    public int getEffectCount() {
        return effects.size();
    }
    
    /**
     * Serializes effects to a comma-separated string.
     */
    public String serializeEffects() {
        if (effects.isEmpty()) {
            return "";
        }
        return effects.stream()
            .map(StrainEffect::serialize)
            .collect(Collectors.joining(","));
    }
    
    /**
     * Deserializes effects from a comma-separated string.
     */
    public void deserializeEffects(String data) {
        effects.clear();
        if (data == null || data.isEmpty()) {
            return;
        }
        String[] parts = data.split(",");
        for (String part : parts) {
            StrainEffect effect = StrainEffect.deserialize(part);
            if (effect != null) {
                effects.add(effect);
            }
        }
    }

    public double getBaseValue() {
        double rarityMultiplier = switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.5;
            case LEGENDARY -> 5.0;
        };
        
        // Effects add value
        double effectBonus = 1.0;
        for (StrainEffect effect : effects) {
            effectBonus += 0.1 * effect.getIntensity();
            if (effect.getType().isLegendary()) {
                effectBonus += 0.5; // Legendary effects add significant value
            }
        }
        
        return (potency * 0.5 + packagingQuality * 0.3) * rarityMultiplier * effectBonus;
    }

    public enum Rarity {
        COMMON("§7Common", "§7"),
        UNCOMMON("§aUncommon", "§a"),
        RARE("§9Rare", "§9"),
        LEGENDARY("§6Legendary", "§6");

        private final String displayName;
        private final String colorCode;

        Rarity(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColorCode() {
            return colorCode;
        }

        public Rarity next() {
            Rarity[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
