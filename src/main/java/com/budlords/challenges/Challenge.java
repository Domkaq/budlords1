package com.budlords.challenges;

import java.util.UUID;

/**
 * Represents a challenge that players can complete for rewards.
 */
public class Challenge {

    private final String id;
    private final String name;
    private final String description;
    private final ChallengeType type;
    private final ChallengeDifficulty difficulty;
    private final int targetAmount;
    private final double rewardMoney;
    private final int rewardXP;
    private final String rewardItem;
    private final long expirationTime;

    public Challenge(String id, String name, String description, ChallengeType type,
                     ChallengeDifficulty difficulty, int targetAmount, double rewardMoney,
                     int rewardXP, String rewardItem, long expirationTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.difficulty = difficulty;
        this.targetAmount = targetAmount;
        this.rewardMoney = rewardMoney;
        this.rewardXP = rewardXP;
        this.rewardItem = rewardItem;
        this.expirationTime = expirationTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ChallengeType getType() {
        return type;
    }

    public ChallengeDifficulty getDifficulty() {
        return difficulty;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public double getRewardMoney() {
        return rewardMoney;
    }

    public int getRewardXP() {
        return rewardXP;
    }

    public String getRewardItem() {
        return rewardItem;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getDifficultyColor() {
        return switch (difficulty) {
            case EASY -> "§a";
            case MEDIUM -> "§e";
            case HARD -> "§c";
            case LEGENDARY -> "§6";
        };
    }

    public String getDisplayName() {
        return getDifficultyColor() + name;
    }

    /**
     * Types of challenges available.
     */
    public enum ChallengeType {
        HARVEST_PLANTS("Harvest plants", "plants-harvested"),
        HARVEST_STRAIN("Harvest specific strain", "strain-harvested"),
        SELL_PRODUCTS("Sell products", "products-sold"),
        EARN_MONEY("Earn money", "money-earned"),
        ROLL_JOINTS("Roll joints", "joints-rolled"),
        PERFECT_HARVESTS("Get perfect harvests", "perfect-harvests"),
        FIVE_STAR_BUDS("Harvest 5-star buds", "five-star-buds"),
        LEGENDARY_HARVESTS("Harvest legendary strains", "legendary-harvests"),
        SUCCESSFUL_TRADES("Complete successful trades", "successful-trades"),
        CROSSBREED_STRAINS("Crossbreed strains", "crossbreeds"),
        USE_FERTILIZER("Use fertilizer", "fertilizer-used"),
        WATER_PLANTS("Water plants", "plants-watered");

        private final String displayName;
        private final String trackingKey;

        ChallengeType(String displayName, String trackingKey) {
            this.displayName = displayName;
            this.trackingKey = trackingKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTrackingKey() {
            return trackingKey;
        }
    }

    /**
     * Difficulty levels for challenges.
     */
    public enum ChallengeDifficulty {
        EASY(1.0, "§a✦ Easy"),
        MEDIUM(1.5, "§e✦✦ Medium"),
        HARD(2.5, "§c✦✦✦ Hard"),
        LEGENDARY(5.0, "§6✦✦✦✦ Legendary");

        private final double rewardMultiplier;
        private final String display;

        ChallengeDifficulty(double rewardMultiplier, String display) {
            this.rewardMultiplier = rewardMultiplier;
            this.display = display;
        }

        public double getRewardMultiplier() {
            return rewardMultiplier;
        }

        public String getDisplay() {
            return display;
        }
    }

    /**
     * Tracks a player's progress on a specific challenge.
     */
    public static class PlayerChallengeProgress {
        private final UUID playerId;
        private final String challengeId;
        private int currentProgress;
        private boolean completed;
        private boolean claimed;

        public PlayerChallengeProgress(UUID playerId, String challengeId) {
            this.playerId = playerId;
            this.challengeId = challengeId;
            this.currentProgress = 0;
            this.completed = false;
            this.claimed = false;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getChallengeId() {
            return challengeId;
        }

        public int getCurrentProgress() {
            return currentProgress;
        }

        public void addProgress(int amount) {
            this.currentProgress += amount;
        }

        public void setProgress(int progress) {
            this.currentProgress = progress;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean isClaimed() {
            return claimed;
        }

        public void setClaimed(boolean claimed) {
            this.claimed = claimed;
        }
    }
}
