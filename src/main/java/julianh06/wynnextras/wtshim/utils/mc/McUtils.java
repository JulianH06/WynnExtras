// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — reimplementation of McUtils helpers.
 * Yarn-mapped wrappers around MinecraftClient.
 */
package julianh06.wynnextras.wtshim.utils.mc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

public final class McUtils {
    private McUtils() {}

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static ClientPlayerEntity player() {
        return mc().player;
    }

    public static String playerName() {
        ClientPlayerEntity p = player();
        return p == null ? "" : p.getName().getString();
    }

    public static PlayerInventory inventory() {
        ClientPlayerEntity p = player();
        return p == null ? null : p.getInventory();
    }

    public static ScreenHandler containerMenu() {
        ClientPlayerEntity p = player();
        return p == null ? null : p.currentScreenHandler;
    }

    public static ScreenHandler inventoryMenu() {
        ClientPlayerEntity p = player();
        return p == null ? null : p.playerScreenHandler;
    }

    public static Screen screen() {
        return mc().currentScreen;
    }

    public static void setScreen(Screen screen) {
        mc().setScreen(screen);
    }

    public static double guiScale() {
        return mc().getWindow().getScaleFactor();
    }

    public static void sendMessageToClient(Text text) {
        if (text == null) return;
        ClientPlayerEntity p = player();
        if (p != null) {
            p.sendMessage(text, false);
        }
    }

    public static void sendChat(String message) {
        ClientPlayerEntity p = player();
        if (p != null) {
            p.networkHandler.sendChatMessage(message);
        }
    }

    public static void playSoundUI(SoundEvent sound) {
        playSoundUI(sound, 1.0f, 1.0f);
    }

    public static void playSoundUI(SoundEvent sound, float pitch, float volume) {
        if (sound == null) return;
        mc().getSoundManager().play(buildMasterSound(sound, pitch, volume));
    }

    public static void playSoundAmbient(SoundEvent sound) {
        playSoundAmbient(sound, 1.0f, 1.0f);
    }

    public static void playSoundAmbient(SoundEvent sound, float pitch, float volume) {
        if (sound == null || player() == null) return;
        mc().getSoundManager().play(buildMasterSound(sound, pitch, volume));
    }

    /**
     * Builds a master-category PositionedSoundInstance directly via its public constructor.
     * Avoids the static {@code master(...)} factory whose signature varies across yarn snapshots.
     */
    private static PositionedSoundInstance buildMasterSound(SoundEvent sound, float pitch, float volume) {
        return new PositionedSoundInstance(
                sound.id(), SoundCategory.MASTER,
                volume, pitch,
                Random.create(),
                false, 0,
                SoundInstance.AttenuationType.NONE,
                0.0, 0.0, 0.0,
                true);
    }
}
