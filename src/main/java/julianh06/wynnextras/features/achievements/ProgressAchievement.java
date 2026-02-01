package julianh06.wynnextras.features.achievements;

import java.util.List;

public class ProgressAchievement extends Achievement {
    protected int current;
    protected int target;

    public ProgressAchievement() {
        super();
    }

    public ProgressAchievement(String id, String title, String description, AchievementCategory category, int target) {
        super(id, title, description, category);
        this.target = target;
        this.current = 0;
    }

    public ProgressAchievement(String id, String title, String description, AchievementCategory category, int target, boolean secret) {
        super(id, title, description, category, secret);
        this.target = target;
        this.current = 0;
    }

    public ProgressAchievement(String id, String title, String description, AchievementCategory category, int target, boolean secret, List<String> hints) {
        super(id, title, description, category, secret, hints);
        this.target = target;
        this.current = 0;
    }

    @Override
    public float getProgress() {
        return Math.min(1f, (float) current / target);
    }

    public int getCurrent() {
        return current;
    }

    public int getTarget() {
        return target;
    }

    public void setCurrent(int current) {
        this.current = current;
        if (this.current >= target && !unlocked) {
            unlock();
        }
    }

    public void progress(int amount) {
        if (unlocked) return;
        current += amount;
        if (current >= target) {
            unlock();
        }
    }

    public String getProgressText() {
        return current + " / " + target;
    }
}
