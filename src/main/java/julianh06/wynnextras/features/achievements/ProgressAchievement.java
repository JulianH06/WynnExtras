package julianh06.wynnextras.features.achievements;

public abstract class ProgressAchievement extends Achievement{
    protected int current;
    protected int target;

    @Override
    public float getProgress() {
        return Math.min(1f, (float) current / target);
    }

    public void progress(int progress) {
        if(unlocked) return;

        current += progress;
        if(current >= target) unlock();
    }
}
