package julianh06.wynnextras.features.achievements;

import java.util.List;

public class TieredAchievement extends ProgressAchievement {
    protected int currentLevel;
    protected List<Integer> levelTargets;

    @Override
    public float getProgress() {
        if(unlocked) return 1;

        return (float) current / levelTargets.get(currentLevel);
    }

    @Override
    public void progress(int progress) {
        if(unlocked) return;

        current += progress;

        int currentTarget = levelTargets.get(currentLevel);

        if(current >= currentTarget) {
            currentLevel++;
            if(currentLevel >= levelTargets.size()) {
                unlock();
            }
        };
    }

    /**
     * Sets the absolute progress count and recomputes the current tier level from scratch,
     * correctly handling jumps across multiple tiers at once (unlike {@link #progress(int)},
     * which only advances a single tier per call). Used when syncing a raw completion count
     * straight from the Wynncraft API.
     */
    public void setCurrent(int count) {
        if (count < 0) count = 0;
        current = count;
        if (unlocked) return;

        int level = 0;
        while (level < levelTargets.size() && current >= levelTargets.get(level)) {
            level++;
        }

        currentLevel = level;
        if (currentLevel >= levelTargets.size()) {
            unlock();
        }
    }

    public int getCurrent() {
        return current;
    }
}
