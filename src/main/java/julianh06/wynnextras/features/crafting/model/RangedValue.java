package julianh06.wynnextras.features.crafting.model;

public record RangedValue(int low, int high) {
    public static RangedValue of(int low, int high) {
        return new RangedValue(low, high);
    }

    public boolean inRange(int value) {
        return value >= low && value <= high;
    }
}
