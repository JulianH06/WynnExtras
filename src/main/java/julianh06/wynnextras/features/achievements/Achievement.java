package julianh06.wynnextras.features.achievements;

import java.time.Instant;

public class Achievement {
    protected String id;
    protected String title;
    protected String description;
    protected boolean secret;
    protected boolean unlocked;
    protected Instant unlockedAt;

    public boolean isUnlocked() {
        return unlocked;
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

    public boolean isSecret() {
        return secret;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public void unlock() {
        if(!unlocked) {
            unlocked = true;
            unlockedAt = Instant.now();
        }
    }

    public float getProgress() {
        return unlocked ? 1f : 0f;
    }
}
