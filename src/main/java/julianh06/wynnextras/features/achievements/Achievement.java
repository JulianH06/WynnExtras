package julianh06.wynnextras.features.achievements;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class Achievement {
    protected String id;
    protected String title;
    protected String description;
    protected AchievementCategory category;
    protected boolean secret;
    protected boolean unlocked;
    protected Instant unlockedAt;
    protected List<String> hints = new ArrayList<>();

    public Achievement() {}

    public Achievement(String id, String title, String description, AchievementCategory category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.secret = false;
        this.unlocked = false;
    }

    public Achievement(String id, String title, String description, AchievementCategory category, boolean secret) {
        this(id, title, description, category);
        this.secret = secret;
    }

    public Achievement(String id, String title, String description, AchievementCategory category, boolean secret, List<String> hints) {
        this(id, title, description, category, secret);
        this.hints = hints != null ? hints : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public AchievementCategory getCategory() {
        return category;
    }

    public boolean isSecret() {
        return secret;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public List<String> getHints() {
        return hints;
    }

    public void unlock() {
        if (!unlocked) {
            unlocked = true;
            unlockedAt = Instant.now();
        }
    }

    public void setUnlocked(boolean unlocked, Instant unlockedAt) {
        this.unlocked = unlocked;
        this.unlockedAt = unlockedAt;
    }

    public abstract float getProgress();

    public String getDisplayTitle() {
        if (secret && !unlocked) {
            return "§k?????????§r"; // Obfuscated text for hidden achievements
        }
        return title;
    }

    public String getDisplayDescription() {
        if (secret && !unlocked) {
            if (!hints.isEmpty()) {
                return hints.get(0);
            }
            return "§k???????????????????§r"; // Obfuscated text
        }
        return description;
    }
}
