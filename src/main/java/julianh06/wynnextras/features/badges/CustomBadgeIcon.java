package julianh06.wynnextras.features.badges;

import julianh06.wynnextras.features.achievements.AchievementId;

public enum CustomBadgeIcon {
    STEAMHAPPY("steamhappy", "Steamhappy", "steamhappy.png", 64, null, null);

    private static final int FIRST_CODE_POINT = 0xE100;

    private final String id;
    private final String displayName;
    private final String fileName;
    private final int textureSize;
    private final AchievementId achievement;
    private final Integer minTier;

    CustomBadgeIcon(String id, String displayName, String fileName, int textureSize, AchievementId achievement, Integer minTier) {
        this.id = id;
        this.displayName = displayName;
        this.fileName = fileName;
        this.textureSize = textureSize;
        this.achievement = achievement;
        this.minTier = minTier;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String fileName() {
        return fileName;
    }

    public int textureSize() {
        return textureSize;
    }

    public AchievementId achievement() {
        return achievement;
    }

    public Integer minTier() {
        return minTier;
    }

    public String tintedGlyph() {
        return glyph(FIRST_CODE_POINT + ordinal() * 2);
    }

    public String originalGlyph() {
        return glyph(FIRST_CODE_POINT + ordinal() * 2 + 1);
    }

    private static String glyph(int codePoint) {
        return new String(Character.toChars(codePoint));
    }
}