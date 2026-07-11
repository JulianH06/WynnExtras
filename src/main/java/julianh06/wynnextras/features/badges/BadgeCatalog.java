package julianh06.wynnextras.features.badges;

import com.wynntils.utils.colors.WynncraftShaderColor;
import julianh06.wynnextras.features.achievements.AchievementId;
import julianh06.wynnextras.features.achievements.AchievementTracking;
import julianh06.wynnextras.features.achievements.Achievements;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class BadgeCatalog {
    public static final String DEFAULT_ICON_ID = "spark";
    public static final String DEFAULT_COLOR_ID = "orange";

    private static final List<BadgeIcon> ICONS = List.of(
            new BadgeIcon(DEFAULT_ICON_ID, "\u2726", "Spark", null, null),
            new BadgeIcon("skull", "\u2620", "Skull", null, null),
            new BadgeIcon("star", "\u2605", "Star", null, null),
            new BadgeIcon("heart", "\u2665", "Heart", null, null),
            new BadgeIcon("note", "\u266A", "Note", null, null),
            new BadgeIcon("notes", "\u266B", "Notes", null, null),
            new BadgeIcon("notg", "\u2737", "Idk what to call this", AchievementId.RAID_NOTG, 3),
            new BadgeIcon("hollow_spark", "\u2727", "Hollow spark", AchievementId.RAID_NOL, 3),
            new BadgeIcon("comet", "\u2604", "Comet", AchievementId.RAID_TCC, 3),
            new BadgeIcon("void", "\u2205", "Void", AchievementId.RAID_TNA, 3),
            new BadgeIcon("crown", "\u265B", "Crown", AchievementId.RAID_TWP, 3),
            new BadgeIcon("fire", "\u2739", "Fire", AchievementId.ASPECT_MAX_ALL, 4),
            new BadgeIcon("cross", "\u2716", "Cross", AchievementId.ASPECT_MAX_ALL_MYTHIC, 4),
            new BadgeIcon("staff", "\u269A", "Staff", AchievementId.ASPECT_MAX_ALL_FABLED, 4),
            new BadgeIcon("snowflake", "\u2747", "Snowflake", AchievementId.ASPECT_MAX_ALL_LEGENDARY, 4),
            new BadgeIcon("cloud", "\u2601", "Cloud", AchievementId.ASPECT_MAX_ALL_SHAMAN, 2),
            new BadgeIcon("umbrella", "\u2602", "Umbrella", AchievementId.ASPECT_MAX_ALL_SHAMAN, 3),
            new BadgeIcon("snowman", "\u2603", "Snowman", AchievementId.ASPECT_MAX_ALL_SHAMAN, 4),
            new BadgeIcon("lightning", "\u26A1", "Lightning", AchievementId.ASPECT_MAX_ALL_SHAMAN, 5),
            new BadgeIcon("atom", "\u269B", "Atom", AchievementId.ASPECT_MAX_ALL_ASSASSIN, 2),
            new BadgeIcon("shamrock", "\u2618", "Shamrock", AchievementId.ASPECT_MAX_ALL_ASSASSIN, 3),
            new BadgeIcon("check", "\u2611", "Check", AchievementId.ASPECT_MAX_ALL_ASSASSIN, 4),
            new BadgeIcon("warning", "\u26A0", "Warning", AchievementId.ASPECT_MAX_ALL_ASSASSIN, 5),
            new BadgeIcon("anchor", "\u2693", "Anchor", AchievementId.ASPECT_MAX_ALL_WARRIOR, 2),
            new BadgeIcon("broken_heart", "\u2694", "Broken heart", AchievementId.ASPECT_MAX_ALL_WARRIOR, 3),
            new BadgeIcon("scales", "\u2696", "Scales", AchievementId.ASPECT_MAX_ALL_WARRIOR, 4),
            new BadgeIcon("ball", "\u26BD", "Ball", AchievementId.ASPECT_MAX_ALL_WARRIOR, 5),
            new BadgeIcon("triangle_up", "\u25B2", "Triangle Up", AchievementId.ASPECT_MAX_ALL_ARCHER, 2),
            new BadgeIcon("triangle_down", "\u25BC", "Triangle Down", AchievementId.ASPECT_MAX_ALL_ARCHER, 3),
            new BadgeIcon("triangle_left", "\u25C0", "Triangle Left", AchievementId.ASPECT_MAX_ALL_ARCHER, 4),
            new BadgeIcon("triangle_right", "\u25B6", "Triangle Right", AchievementId.ASPECT_MAX_ALL_ARCHER, 5),
            new BadgeIcon("sum", "\u2211", "Sum", AchievementId.ASPECT_MAX_ALL_MAGE, 2),
            new BadgeIcon("lambda", "\u03BB", "Lambda", AchievementId.ASPECT_MAX_ALL_MAGE, 3),
            new BadgeIcon("omega", "\u03A9", "Omega", AchievementId.ASPECT_MAX_ALL_MAGE, 4),
            new BadgeIcon("sun", "\u2734", "Sun", AchievementId.ASPECT_MAX_ALL_MAGE, 5),
            new BadgeIcon("tick", "\u2714", "Tick", AchievementId.CLASS_LEVEL_120, 3),
            new BadgeIcon("infinity", "\u221E", "Infinity", AchievementId.CLASS_LEVEL_121, 5),
            new BadgeIcon("star_2", "\u272F", "Star 2", AchievementId.CONTENT_COMPLETION, null),
            new BadgeIcon("flower", "\u273F", "Flower", AchievementId.PROF_GATHER_100, 4),
            new BadgeIcon("plant", "\u2724", "Plant", AchievementId.PROF_GATHER_115, 3),
            new BadgeIcon("pinwheel", "\u273D", "Pinwheel", AchievementId.PROF_GATHER_132, 2),
            new BadgeIcon("hollow_square", "\u25A1", "Hollow Square", AchievementId.PROF_CRAFT_100, 3),
            new BadgeIcon("square", "\u25A0", "Square", AchievementId.PROF_CRAFT_100, 4),
            new BadgeIcon("hollow_circle", "\u25CB", "Hollow Circle", AchievementId.PROF_CRAFT_115, 3),
            new BadgeIcon("circle", "\u25CF", "Circle", AchievementId.PROF_CRAFT_115, 4),
            new BadgeIcon("hollow_diamond", "\u25C7", "Hollow Diamond", AchievementId.PROF_CRAFT_132, 2),
            new BadgeIcon("diamond", "\u25C6", "Diamond", AchievementId.PROF_CRAFT_132, 3)
    );

    private static final List<BadgeColor> COLORS = List.of(
            new BadgeColor(DEFAULT_COLOR_ID, 0xFF8800, "Orange", null, null),
            new BadgeColor("red", 0xFF3333, "Red", null, null),
            new BadgeColor("pink", 0xFF69B4, "Pink", null, null),
            new BadgeColor("sapphire", 0x0F52BA, "Sapphire", null, null),
            new BadgeColor("dark_green", 0x228B22, "Dark Green", null, null),
            new BadgeColor("gray", 0xAAAAAA, "Gray", null, null),
            new BadgeColor("white", 0xFFFFFF, "White", null, null),
            new BadgeColor("rainbow", WynncraftShaderColor.RAINBOW.color.asInt(), "Rainbow", AchievementId.CONTENT_COMPLETION, null),
            new BadgeColor("shine", WynncraftShaderColor.SHINE.color.asInt(), "Shine", AchievementId.ASPECT_MAX_ALL, 6),
            new BadgeColor("gradient", WynncraftShaderColor.GRADIENT.color.asInt(), "Gradient", AchievementId.PROF_CRAFT_132, 1),
            new BadgeColor("crimson", WynncraftShaderColor.GRADIENT_2.color.asInt(), "Crimson", AchievementId.RAID_TWP, 4),
            new BadgeColor("black", 0x111111, "Black", AchievementId.PROF_GATHER_132, 1),
            new BadgeColor("silver", 0xD8D8D8, "Silver", AchievementId.RAID_TCC, 4),
            new BadgeColor("rose_gold", 0xE6A7A1, "Rose Gold", AchievementId.PROF_CRAFT_115, 2),
            new BadgeColor("bronze", 0xD98235, "Bronze", AchievementId.PROF_CRAFT_100, 2),
            new BadgeColor("gold", 0xFFBF00, "Gold", AchievementId.ASPECT_MAX_ALL_MAGE, 1),
            new BadgeColor("yellow", 0xFFD700, "Yellow", AchievementId.RAID_NOL, 4),
            new BadgeColor("lime", 0xB6FF3F, "Lime", AchievementId.RAID_NOTG, 4),
            new BadgeColor("green", 0x55DD66, "Green", AchievementId.PROF_GATHER_100, 3),
            new BadgeColor("mint", 0x6DFFB8, "Mint", AchievementId.PROF_GATHER_115, 2),
            new BadgeColor("aqua", 0x00FFFF, "Aqua", AchievementId.ASPECT_MAX_ALL_SHAMAN, 1),
            new BadgeColor("light_blue", 0x9BD8FF, "Light Blue", AchievementId.CLASS_LEVEL_121, 1),
            new BadgeColor("navy", 0x1E2A78, "Navy Blue", AchievementId.ASPECT_MAX_ALL_LEGENDARY, 3),
            new BadgeColor("void", 0x2A163A, "Void", AchievementId.RAID_TNA, 4),
            new BadgeColor("indigo", 0x4B0082, "Indigo", AchievementId.ASPECT_MAX_ALL_MYTHIC, 3),
            new BadgeColor("violet", 0x8F00FF, "Violet", AchievementId.CLASS_LEVEL_120, 1),
            new BadgeColor("purple", 0xC95CFF, "Purple", AchievementId.ASPECT_MAX_ALL_ASSASSIN, 1),
            new BadgeColor("magenta", 0xFF00FF, "Magenta", AchievementId.ASPECT_MAX_ALL_ARCHER, 1),
            new BadgeColor("dark_red", 0xBB1111, "Dark Red", AchievementId.ASPECT_MAX_ALL_FABLED, 3),
            new BadgeColor("darker_red", 0x660000, "Darker Red", AchievementId.ASPECT_MAX_ALL_WARRIOR, 1)
    );

    private BadgeCatalog() {}

    public static List<BadgeIcon> icons() {
        return ICONS;
    }

    public static List<BadgeColor> colors() {
        return COLORS;
    }

    public static BadgeIcon icon(String id) {
        for (BadgeIcon icon : ICONS) {
            if (icon.id().equals(id)) return icon;
        }
        return defaultIcon();
    }

    public static BadgeColor color(String id) {
        for (BadgeColor color : COLORS) {
            if (color.id().equals(id)) return color;
        }
        return defaultColor();
    }

    public static BadgeIcon defaultIcon() {
        return ICONS.getFirst();
    }

    public static BadgeColor defaultColor() {
        return COLORS.getFirst();
    }

    public static boolean isKnownIcon(String id) {
        return ICONS.stream().anyMatch(icon -> icon.id().equals(id));
    }

    public static boolean isKnownColor(String id) {
        return COLORS.stream().anyMatch(color -> color.id().equals(id));
    }

    public static boolean isUnlocked(BadgeIcon icon) {
        return isUnlocked(icon.achievement(), icon.minTier());
    }

    public static boolean isUnlocked(BadgeColor color) {
        return isUnlocked(color.achievement(), color.minTier());
    }

    public static String requirement(AchievementId achievement, Integer minTier) {
        if (achievement == null) return "Unlocked by default";
        String suffix = minTier == null ? "" : " tier " + minTier;
        return "Requires " + achievement.id() + suffix;
    }

    public static List<BadgeReward> rewards() {
        List<BadgeReward> rewards = new ArrayList<>();
        for (BadgeIcon icon : ICONS) {
            if (icon.achievement() != null) rewards.add(new BadgeReward(icon.achievement(), icon.minTier(), "Icon: " + icon.displayName()));
        }
        for (BadgeColor color : COLORS) {
            if (color.achievement() != null) rewards.add(new BadgeReward(color.achievement(), color.minTier(), "Color: " + color.displayName()));
        }
        return rewards;
    }

    public static Text badgeText(String iconId, String colorId) {
        BadgeIcon icon = icon(iconId);
        BadgeColor color = color(colorId);
        return Text.literal(icon.glyph()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.rgb())));
    }

    public static Text colorPreviewText(String colorId) {
        BadgeColor color = color(colorId);
        return Text.literal("\u25CF").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.rgb())));
    }

    private static boolean isUnlocked(AchievementId achievement, Integer minTier) {
        if (achievement == null) return true;
        Achievements achievements = AchievementTracking.achievements;
        if (achievements == null) return false;
        if (minTier != null) {
            Integer tier = achievements.getTier(achievement.id());
            return tier != null && tier >= minTier;
        }
        return achievements.isUnlocked(achievement.id());
    }

    public record BadgeIcon(String id, String glyph, String displayName, AchievementId achievement, Integer minTier) {}
    public record BadgeColor(String id, int rgb, String displayName, AchievementId achievement, Integer minTier) {}
    public record BadgeReward(AchievementId achievement, Integer minTier, String rewardName) {}
}