package com.budlords.packaging;

import com.budlords.strain.Strain;

public class PackagedProduct {

    private final String strainId;
    private final WeightType weight;
    private final double baseValue;
    private final double finalValue;

    public PackagedProduct(String strainId, WeightType weight, double baseValue) {
        this.strainId = strainId;
        this.weight = weight;
        this.baseValue = baseValue;
        this.finalValue = baseValue * weight.getMultiplier();
    }

    public String getStrainId() {
        return strainId;
    }

    public WeightType getWeight() {
        return weight;
    }

    public double getBaseValue() {
        return baseValue;
    }

    public double getFinalValue() {
        return finalValue;
    }

    public enum WeightType {
        ONE_GRAM(1, "1g", 1.0),
        THREE_GRAM(3, "3g", 1.25),
        FIVE_GRAM(5, "5g", 1.5),
        TEN_GRAM(10, "10g", 2.0);

        private final int grams;
        private final String displayName;
        private final double multiplier;

        WeightType(int grams, String displayName, double multiplier) {
            this.grams = grams;
            this.displayName = displayName;
            this.multiplier = multiplier;
        }

        public int getGrams() {
            return grams;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public static WeightType fromGrams(int grams) {
            for (WeightType type : values()) {
                if (type.grams == grams) {
                    return type;
                }
            }
            return null;
        }
    }
}
