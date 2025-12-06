package com.budlords.strain;

import org.bukkit.Material;

public class Strain {

    private final String id;
    private String name;
    private Rarity rarity;
    private int potency;
    private int yield;
    private int packagingQuality;
    private Material iconMaterial;

    public Strain(String id, String name, Rarity rarity, int potency, int yield, int packagingQuality) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.potency = potency;
        this.yield = yield;
        this.packagingQuality = packagingQuality;
        this.iconMaterial = Material.GREEN_DYE;
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

    public double getBaseValue() {
        double rarityMultiplier = switch (rarity) {
            case COMMON -> 1.0;
            case UNCOMMON -> 1.5;
            case RARE -> 2.5;
            case LEGENDARY -> 5.0;
        };
        return (potency * 0.5 + packagingQuality * 0.3) * rarityMultiplier;
    }

    public enum Rarity {
        COMMON("§7Common"),
        UNCOMMON("§aUncommon"),
        RARE("§9Rare"),
        LEGENDARY("§6Legendary");

        private final String displayName;

        Rarity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Rarity next() {
            Rarity[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
