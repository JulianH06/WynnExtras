package julianh06.wynnextras.features.achievements;

import java.time.Instant;

public abstract class Achievement {
    protected String id;
    protected String title;
    protected String description;
    protected boolean unlocked;
    protected Instant unlockedAt;

    public boolean isUnlocked() {
        return unlocked;
    }

    public void unlock() {
        if(!unlocked) {
            unlocked = true;
            unlockedAt = Instant.now();
        }
    }

    public abstract float getProgress();
}
