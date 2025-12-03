package com.budlords.joint;

import com.budlords.quality.StarRating;

import java.util.UUID;

/**
 * Represents an ongoing joint rolling session for a player.
 * Tracks progress through the 4 stages of rolling.
 */
public class JointRollingSession {

    private final UUID playerId;
    private final String strainId;
    private final String strainName;
    private final StarRating budRating;
    private final int potency;
    private final long startTime;
    
    private RollingStage currentStage;
    private int stageScore;      // Score achieved in current stage (0-100)
    private int totalScore;      // Combined score from all completed stages
    private int completedStages; // Number of stages completed
    
    // Minigame state
    private boolean minigameActive;
    private long minigameStartTime;
    private int minigameProgress;
    private int targetProgress;

    public JointRollingSession(UUID playerId, String strainId, String strainName, 
                                StarRating budRating, int potency) {
        this.playerId = playerId;
        this.strainId = strainId;
        this.strainName = strainName;
        this.budRating = budRating;
        this.potency = potency;
        this.startTime = System.currentTimeMillis();
        this.currentStage = RollingStage.PAPER_PULL;
        this.stageScore = 0;
        this.totalScore = 0;
        this.completedStages = 0;
        this.minigameActive = false;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getStrainId() {
        return strainId;
    }

    public String getStrainName() {
        return strainName;
    }

    public StarRating getBudRating() {
        return budRating;
    }

    public int getPotency() {
        return potency;
    }

    public long getStartTime() {
        return startTime;
    }

    public RollingStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(RollingStage currentStage) {
        this.currentStage = currentStage;
        this.stageScore = 0;
        this.minigameActive = false;
        this.minigameProgress = 0;
    }

    public int getStageScore() {
        return stageScore;
    }

    public void setStageScore(int stageScore) {
        this.stageScore = Math.max(0, Math.min(100, stageScore));
    }

    public void addStageScore(int amount) {
        this.stageScore = Math.max(0, Math.min(100, this.stageScore + amount));
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getCompletedStages() {
        return completedStages;
    }

    public boolean isMinigameActive() {
        return minigameActive;
    }

    public void setMinigameActive(boolean active) {
        this.minigameActive = active;
        if (active) {
            this.minigameStartTime = System.currentTimeMillis();
        }
    }

    public long getMinigameStartTime() {
        return minigameStartTime;
    }

    public int getMinigameProgress() {
        return minigameProgress;
    }

    public void setMinigameProgress(int progress) {
        this.minigameProgress = progress;
    }

    public void addMinigameProgress(int amount) {
        this.minigameProgress += amount;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(int targetProgress) {
        this.targetProgress = targetProgress;
    }

    /**
     * Completes the current stage and moves to the next one.
     * Returns true if all stages are complete.
     */
    public boolean completeStage() {
        totalScore += stageScore;
        completedStages++;
        
        RollingStage nextStage = currentStage.getNext();
        if (nextStage == null) {
            return true; // All stages complete
        }
        
        setCurrentStage(nextStage);
        return false;
    }

    /**
     * Calculates the final quality rating based on all stage scores.
     */
    public StarRating calculateFinalRating() {
        // Average score from all 4 stages (0-100 each, total 0-400, average 0-100)
        double averageScore = (double) totalScore / 4.0;
        
        // Combine with bud rating
        double budBonus = budRating.getStars() * 5; // 5-25 bonus
        double finalScore = averageScore + budBonus;
        
        // Convert to star rating
        if (finalScore >= 95) return StarRating.FIVE_STAR;
        if (finalScore >= 80) return StarRating.FOUR_STAR;
        if (finalScore >= 60) return StarRating.THREE_STAR;
        if (finalScore >= 40) return StarRating.TWO_STAR;
        return StarRating.ONE_STAR;
    }

    /**
     * Gets the percentage of completion across all stages.
     */
    public double getOverallProgress() {
        return (completedStages + (stageScore / 100.0)) / 4.0;
    }

    /**
     * Gets how long the session has been active in milliseconds.
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * The four stages of rolling a joint.
     */
    public enum RollingStage {
        PAPER_PULL("§f✦ Paper Pull", "Pull out the rolling paper!", "paper_pull"),
        TOBACCO_ROLL("§6✦ Tobacco Rolling", "Roll the tobacco evenly!", "tobacco_roll"),
        GRIND("§a✦ Ganja Grinding", "Grind the bud perfectly!", "grind"),
        FINAL_ROLL("§e✦ Final Roll", "Roll it all together!", "final_roll");

        private final String displayName;
        private final String description;
        private final String id;

        RollingStage(String displayName, String description, String id) {
            this.displayName = displayName;
            this.description = description;
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getId() {
            return id;
        }

        public RollingStage getNext() {
            RollingStage[] values = values();
            int nextIndex = this.ordinal() + 1;
            return nextIndex < values.length ? values[nextIndex] : null;
        }

        public int getStageNumber() {
            return ordinal() + 1;
        }
    }
}
