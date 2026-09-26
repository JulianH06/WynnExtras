package julianh06.wynnextras.features.buildplanner.data;

public record Identification(int min, int raw, int max) {
    public boolean isRollable() {
        return min != max;
    }

    public double getRollPercent(int actual) {
        if (max == min) {
            return 100.0D;
        }

        double percent = ((double) actual - min) / (max - min) * 100.0D;
        return Math.max(0.0D, Math.min(100.0D, percent));
    }
}
