package julianh06.wynnextras.features.achievements;

public class ProgressAchievement extends Achievement{
    protected int current;
    protected int target;

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

    public void progress(int progress) {
        if(unlocked) return;

        current += progress;
        if(current >= target) unlock();
    }

    /**
     * Sets the absolute progress count (instead of incrementing), unlocking the achievement if the
     * target is met. Used when syncing a value straight from an API/scan rather than counting events.
     */
    public void setCurrentAbsolute(int amount) {
        if (amount < 0) amount = 0;
        current = amount;
        if (current >= target) unlock();
    }
}
