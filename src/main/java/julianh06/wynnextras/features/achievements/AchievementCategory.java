package julianh06.wynnextras.features.achievements;

public enum AchievementCategory {
    RAIDING("Raiding", 0xFFAA00),
    ASPECTS("Aspects", 0x00AAFF),
    MISC("Miscellaneous", 0xAAAAAA),
    PROFESSIONS("Professions", 0x55FF55),
    WARS("Wars", 0xFF5555),
    LOOTRUNS("Lootruns", 0xAA00FF);

    private final String displayName;
    private final int color;

    AchievementCategory(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }
}
