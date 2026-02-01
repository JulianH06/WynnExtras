package julianh06.wynnextras.features.achievements;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AchievementToast {

    public static void showUnlock(Achievement achievement) {
        if (!WynnExtrasConfig.INSTANCE.achievementToastsEnabled) return;

        String title = "Achievement Unlocked!";
        String subtitle = achievement.getTitle();

        // Must run on main thread for UI operations
        MinecraftClient.getInstance().execute(() -> {
            ChatUtils.displayTitle(title, subtitle, 60, 10, 10, Formatting.GOLD, Formatting.BOLD);
            playSound();
        });
    }

    public static void showTierUp(TieredAchievement achievement, TieredAchievement.TierLevel tier) {
        if (!WynnExtrasConfig.INSTANCE.achievementToastsEnabled) return;

        String emoji = tier.getEmoji();
        String tierName = tier.getName();
        String title = emoji + " " + tierName + " Tier!";
        String subtitle = achievement.getTitle();

        Formatting color = switch (tier) {
            case BRONZE -> Formatting.GOLD;
            case SILVER -> Formatting.GRAY;
            case GOLD -> Formatting.YELLOW;
            default -> Formatting.WHITE;
        };

        // Must run on main thread for UI operations
        MinecraftClient.getInstance().execute(() -> {
            ChatUtils.displayTitle(title, subtitle, 60, 10, 10, color, Formatting.BOLD);
            playSound();
        });
    }

    public static void showProgress(Achievement achievement, int oldProgress, int newProgress) {
        // Optional: Show progress milestones (e.g., 50%, 75%)
        // For now, we'll skip this to avoid spam
    }

    private static void playSound() {
        McUtils.playSoundAmbient(
            SoundEvent.of(Identifier.of("entity.player.levelup")),
            0.5f,
            1.0f
        );
    }
}
