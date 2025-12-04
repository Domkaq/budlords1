package com.budlords.quality;

/**
 * Represents a 1-6 star quality rating for items in the BudLords system.
 * Higher star ratings provide better bonuses and outcomes.
 * Note: 6-star rating is only achievable through crossbreeding mutations!
 */
public enum StarRating {
    ONE_STAR(1, "§7★☆☆☆☆☆", "§7", 1.0, 0.8),
    TWO_STAR(2, "§e★★☆☆☆☆", "§e", 1.15, 0.9),
    THREE_STAR(3, "§a★★★☆☆☆", "§a", 1.35, 1.0),
    FOUR_STAR(4, "§9★★★★☆☆", "§9", 1.6, 1.15),
    FIVE_STAR(5, "§6★★★★★☆", "§6", 2.0, 1.35),
    SIX_STAR(6, "§d§l★★★★★★", "§d§l", 3.0, 2.0); // LEGENDARY - Only from crossbreeding!

    /** Minimum star rating value */
    public static final int MIN_STARS = 1;
    /** Maximum star rating value for normal items */
    public static final int MAX_STARS = 5;
    /** Maximum star rating value including legendary 6-star (crossbreeding only) */
    public static final int MAX_STARS_LEGENDARY = 6;

    private final int stars;
    private final String display;
    private final String colorCode;
    private final double qualityMultiplier;
    private final double growthSpeedMultiplier;

    StarRating(int stars, String display, String colorCode, double qualityMultiplier, double growthSpeedMultiplier) {
        this.stars = stars;
        this.display = display;
        this.colorCode = colorCode;
        this.qualityMultiplier = qualityMultiplier;
        this.growthSpeedMultiplier = growthSpeedMultiplier;
    }

    public int getStars() {
        return stars;
    }

    public String getDisplay() {
        return display;
    }

    public String getColorCode() {
        return colorCode;
    }

    public double getQualityMultiplier() {
        return qualityMultiplier;
    }

    public double getGrowthSpeedMultiplier() {
        return growthSpeedMultiplier;
    }
    
    /**
     * Checks if this is the legendary 6-star rating.
     */
    public boolean isLegendary() {
        return this == SIX_STAR;
    }

    /**
     * Gets a star rating from an integer value (1-5).
     * Values outside 1-5 are clamped. Use fromValueAllowSixStar for crossbreeding.
     */
    public static StarRating fromValue(int value) {
        if (value <= 1) return ONE_STAR;
        if (value >= 5) return FIVE_STAR;
        return values()[value - 1];
    }
    
    /**
     * Gets a star rating from an integer value (1-6).
     * Only use this for crossbreeding mutations!
     */
    public static StarRating fromValueAllowSixStar(int value) {
        if (value <= 1) return ONE_STAR;
        if (value >= 6) return SIX_STAR;
        return values()[value - 1];
    }

    /**
     * Calculates the average star rating from multiple component ratings.
     * Uses weighted averaging and rounds to nearest star.
     */
    public static StarRating calculateCombined(StarRating... ratings) {
        if (ratings == null || ratings.length == 0) return ONE_STAR;
        
        double total = 0;
        for (StarRating rating : ratings) {
            total += rating.getStars();
        }
        int average = (int) Math.round(total / ratings.length);
        return fromValue(average);
    }

    /**
     * Calculates the final star rating for a harvested bud based on all contributing factors.
     * Each component is weighted differently:
     * - Pot: 20%
     * - Seed: 25%
     * - Lamp: 20%
     * - Fertilizer: 15%
     * - Scissors: 10%
     * - Care quality: 10%
     */
    public static StarRating calculateBudRating(StarRating pot, StarRating seed, StarRating lamp, 
                                                 StarRating fertilizer, StarRating scissors, double careQuality) {
        double weighted = 0;
        
        weighted += (pot != null ? pot.getStars() : 1) * 0.20;
        weighted += (seed != null ? seed.getStars() : 1) * 0.25;
        weighted += (lamp != null ? lamp.getStars() : 1) * 0.20;
        weighted += (fertilizer != null ? fertilizer.getStars() : 1) * 0.15;
        weighted += (scissors != null ? scissors.getStars() : 1) * 0.10;
        weighted += Math.min(5, Math.max(1, careQuality * 5)) * 0.10;
        
        int finalStars = (int) Math.round(weighted);
        return fromValue(finalStars);
    }

    /**
     * Returns a compact star display (just filled stars).
     */
    public String getCompactDisplay() {
        StringBuilder sb = new StringBuilder(colorCode);
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return display;
    }
}
