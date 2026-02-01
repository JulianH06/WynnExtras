package julianh06.wynnextras.features.achievements;

import java.util.List;

public class TieredAchievement extends Achievement {
    public enum TierLevel {
        NONE("None", 0xAAAAAA, ""),
        BRONZE("Bronze", 0xCD7F32, "\uD83E\uDD49"),
        SILVER("Silver", 0xC0C0C0, "\uD83E\uDD48"),
        GOLD("Gold", 0xFFD700, "\uD83E\uDD47");

        private final String name;
        private final int color;
        private final String emoji;

        TierLevel(String name, int color, String emoji) {
            this.name = name;
            this.color = color;
            this.emoji = emoji;
        }

        public String getName() {
            return name;
        }

        public int getColor() {
            return color;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    protected int current;
    protected int currentTierIndex;
    protected List<Integer> tierTargets;

    public TieredAchievement() {
        super();
    }

    public TieredAchievement(String id, String title, String description, AchievementCategory category, List<Integer> tierTargets) {
        super(id, title, description, category);
        this.tierTargets = tierTargets;
        this.current = 0;
        this.currentTierIndex = 0;
    }

    public TieredAchievement(String id, String title, String description, AchievementCategory category, List<Integer> tierTargets, boolean secret) {
        super(id, title, description, category, secret);
        this.tierTargets = tierTargets;
        this.current = 0;
        this.currentTierIndex = 0;
    }

    public TieredAchievement(String id, String title, String description, AchievementCategory category, List<Integer> tierTargets, boolean secret, List<String> hints) {
        super(id, title, description, category, secret, hints);
        this.tierTargets = tierTargets;
        this.current = 0;
        this.currentTierIndex = 0;
    }

    @Override
    public float getProgress() {
        if (unlocked) return 1f;
        if (currentTierIndex >= tierTargets.size()) return 1f;
        return (float) current / tierTargets.get(currentTierIndex);
    }

    public float getTotalProgress() {
        if (tierTargets == null || tierTargets.isEmpty()) return 0f;
        int maxTarget = tierTargets.get(tierTargets.size() - 1);
        return Math.min(1f, (float) current / maxTarget);
    }

    public int getCurrent() {
        return current;
    }

    public int getCurrentTierTarget() {
        if (currentTierIndex >= tierTargets.size()) {
            return tierTargets.get(tierTargets.size() - 1);
        }
        return tierTargets.get(currentTierIndex);
    }

    public int getCurrentTierIndex() {
        return currentTierIndex;
    }

    public List<Integer> getTierTargets() {
        return tierTargets;
    }

    public TierLevel getCurrentTier() {
        if (currentTierIndex == 0 && current < tierTargets.get(0)) {
            return TierLevel.NONE;
        }
        if (currentTierIndex >= tierTargets.size() || unlocked) {
            return TierLevel.GOLD;
        }
        switch (currentTierIndex) {
            case 1: return TierLevel.BRONZE;
            case 2: return TierLevel.SILVER;
            default: return TierLevel.NONE;
        }
    }

    public TierLevel getHighestUnlockedTier() {
        int completedTiers = 0;
        for (int i = 0; i < tierTargets.size(); i++) {
            if (current >= tierTargets.get(i)) {
                completedTiers = i + 1;
            }
        }
        switch (completedTiers) {
            case 1: return TierLevel.BRONZE;
            case 2: return TierLevel.SILVER;
            case 3: return TierLevel.GOLD;
            default: return TierLevel.NONE;
        }
    }

    public void setCurrent(int current) {
        this.current = current;
        updateTierIndex();
    }

    public void setCurrentTierIndex(int index) {
        this.currentTierIndex = index;
    }

    private void updateTierIndex() {
        for (int i = 0; i < tierTargets.size(); i++) {
            if (current < tierTargets.get(i)) {
                currentTierIndex = i;
                return;
            }
        }
        currentTierIndex = tierTargets.size();
        if (!unlocked) {
            unlock();
        }
    }

    public TierLevel progress(int amount) {
        if (unlocked) return null;

        TierLevel previousTier = getHighestUnlockedTier();
        current += amount;
        updateTierIndex();
        TierLevel newTier = getHighestUnlockedTier();

        if (newTier != previousTier && newTier != TierLevel.NONE) {
            return newTier;
        }
        return null;
    }

    public String getProgressText() {
        if (unlocked || currentTierIndex >= tierTargets.size()) {
            return current + " / " + tierTargets.get(tierTargets.size() - 1) + " (Complete)";
        }
        return current + " / " + tierTargets.get(currentTierIndex);
    }

    public String getTierProgressText() {
        TierLevel tier = getHighestUnlockedTier();
        if (tier == TierLevel.GOLD) {
            return tier.getEmoji() + " Gold";
        } else if (tier == TierLevel.NONE) {
            return "0 / " + tierTargets.get(0) + " (Bronze)";
        }
        int nextTierIndex = tier == TierLevel.BRONZE ? 1 : 2;
        if (nextTierIndex < tierTargets.size()) {
            TierLevel nextTier = TierLevel.values()[nextTierIndex + 1];
            return tier.getEmoji() + " " + current + " / " + tierTargets.get(nextTierIndex) + " (" + nextTier.getName() + ")";
        }
        return tier.getEmoji() + " " + tier.getName();
    }

    /**
     * Get description showing the next tier target
     */
    @Override
    public String getDisplayDescription() {
        if (secret && !unlocked) {
            if (!hints.isEmpty()) {
                return hints.get(0);
            }
            return "§k???????????????????§r";
        }

        // Show what to do next based on current tier
        TierLevel tier = getHighestUnlockedTier();
        if (tier == TierLevel.GOLD) {
            return description;
        }

        int nextTarget;
        String tierName;
        if (tier == TierLevel.NONE) {
            nextTarget = tierTargets.get(0);
            tierName = "Bronze";
        } else if (tier == TierLevel.BRONZE) {
            nextTarget = tierTargets.get(1);
            tierName = "Silver";
        } else {
            nextTarget = tierTargets.get(2);
            tierName = "Gold";
        }

        // For profession achievements (level-based), show "Reach level X"
        if (id.startsWith("profession_gathering") || id.startsWith("profession_crafting")) {
            String profType = id.contains("gathering") ? "gathering" : "crafting";
            return "Reach level " + nextTarget + " in a " + profType + " profession";
        }

        // For other tiered achievements, show next target
        return description + " (next: " + nextTarget + ")";
    }
}
