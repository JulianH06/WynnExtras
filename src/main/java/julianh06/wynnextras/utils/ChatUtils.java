package julianh06.wynnextras.utils;

import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

// TODO: fail proof sendMessage for when localPlayer is null
public class ChatUtils {
    public static void sendMessage(Text message) {
        MinecraftUtils.localPlayer().sendMessage(message, false);
    }

    public static void sendMessage(String message) {
        sendMessage(message, Formatting.RESET);
    }

    public static void sendMessage(String message, Formatting... formats) {
        sendGenericMessage(message, false, formats);
    }

    public static void sendActionbarMessage(String message) {
        sendActionbarMessage(message, Formatting.RESET);
    }

    public static void sendActionbarMessage(String message, Formatting... formats) {
        sendGenericMessage(message, true, formats);
    }

    public static void displayTitle(String title, int duration) {
        displayTitle(title, "", duration, 10, 10, Formatting.RESET);
    }

    public static void displayTitle(String title, String subtitle, int duration) {
        displayTitle(title, subtitle, duration, 10, 10, Formatting.RESET);
    }

    public static void displayTitle(String title, String subtitle, int duration, int fadeInTicks, int fadeOutTicks, Formatting... formats) {
        MutableText titleComponent = Text.literal(title);
        MutableText subtitleComponent = Text.literal(subtitle);

        titleComponent = titleComponent.formatted(formats);
        subtitleComponent = subtitleComponent.formatted(formats);

        MinecraftUtils.localNetworkHandler().onTitleFade(new TitleFadeS2CPacket(fadeInTicks, duration, fadeOutTicks));

        if (!subtitle.isEmpty()) {
            MinecraftUtils.localNetworkHandler().onSubtitle(new SubtitleS2CPacket(subtitleComponent));
        }
        if (!title.isEmpty()) {
            MinecraftUtils.localNetworkHandler().onTitle(new TitleS2CPacket(titleComponent));
        }
    }

    public static void displayTitle(String title, String subtitle, int duration, Formatting... formats) {
        displayTitle(title, subtitle, duration, 10, 10, formats);
    }

    private static void sendGenericMessage(String message, Boolean actionbar, Formatting... formats) {
        MutableText component = Text.literal(message);

        component = component.formatted(formats);

        MinecraftUtils.localPlayer().sendMessage(component, actionbar);
    }

    public static MutableText concatComponents(Iterable<? extends Text> components) {
        return concatComponents("", components);
    }

    public static MutableText concatComponents(String separator, Iterable<? extends Text> components) {
        MutableText result = Text.empty();
        for (Text component : components) {
            if (!result.getString().isEmpty()) {
                result.append(separator);
            }
            result.append(component);
        }

        return result;
    }
}