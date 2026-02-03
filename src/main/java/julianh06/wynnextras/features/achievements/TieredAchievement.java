package julianh06.wynnextras.features.achievements;

import java.util.List;

public abstract class TieredAchievement extends ProgressAchievement {
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
}
