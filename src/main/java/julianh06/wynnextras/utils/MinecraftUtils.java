package julianh06.wynnextras.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class MinecraftUtils {
    public static ClientPlayerEntity localPlayer() {
        ClientPlayerEntity localPlayer = localPlayerOrNull();
        if (localPlayer == null) {
            throw new IllegalStateException("Local player is null, this should not happen!");
        }
        return localPlayer;
    }

    public static ClientPlayerEntity localPlayerOrNull() {
        return MinecraftClient.getInstance().player;
    }

    public static boolean isLocalPlayer(Entity entity) {
        return entity != null && entity.equals(localPlayerOrNull());
    }

    public static boolean localPlayerExists() {
        return localPlayerOrNull() != null;
    }

    public static ClientWorld localWorld() {
        ClientWorld localWorld = localWorldOrNull();
        if (localWorld == null) {
            throw new IllegalStateException("Local world is null, this should not happen!");
        }
        return localWorld;
    }

    public static ClientWorld localWorldOrNull() {
        return MinecraftClient.getInstance().world;
    }

    public static boolean localWorldExists() {
        return localWorldOrNull() != null;
    }

    public static boolean isOnWynncraft() {
        MinecraftClient client = mc();
        if (client.player == null || client.world == null || client.getCurrentServerEntry() == null) return false;
        String address = client.getCurrentServerEntry().address;
        int portSeparator = address.indexOf(':');
        if (portSeparator >= 0) address = address.substring(0, portSeparator);
        String host = address.toLowerCase(java.util.Locale.ROOT);
        return host.equals("wynncraft.com") || host.endsWith(".wynncraft.com");
    }

    public static boolean isControlDown() {
        long window = mc().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    // TODO: This is temporary and should be replaced with a wrapper around the network handler
    public static ClientPlayNetworkHandler localNetworkHandler() {
        ClientPlayNetworkHandler localNetworkHandler = localNetworkHandlerOrNull();
        if (localNetworkHandler == null) {
            throw new IllegalStateException("Local network handler is null, this should not happen!");
        }
        return localNetworkHandler;
    }

    public static ClientPlayNetworkHandler localNetworkHandlerOrNull() {
        return MinecraftClient.getInstance().getNetworkHandler();
    }

    // TODO: This is temporary and should be replaced with a wrapper around the MinecraftClient instance
    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static ClientPlayerEntity player() {
        return localPlayer();
    }

    public static String playerName() {
        return player().getGameProfile().name();
    }

    public static ScreenHandler containerMenu() {
        return player().currentScreenHandler;
    }

    public static PlayerInventory inventory() {
        return player().getInventory();
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

    public static void sendMessageToClient(Text message) {
        ClientPlayerEntity player = localPlayerOrNull();
        if (player != null) player.sendMessage(message, false);
    }

    public static void sendChat(String message) {
        ClientPlayNetworkHandler handler = localNetworkHandlerOrNull();
        if (handler != null) handler.sendChatMessage(message);
    }

    public static void playSoundUI(SoundEvent sound) {
        playSoundUI(sound, 1f);
    }

    public static void playSoundUI(SoundEvent sound, float volume) {
        ClientPlayerEntity player = localPlayerOrNull();
        if (player != null) player.playSound(sound, volume, 1f);
    }

    public static void playSoundUI(SoundEvent sound, float volume, float pitch) {
        mc().getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
    }

    public static void playSoundAmbient(SoundEvent sound) {
        playSoundAmbient(sound, 1f, 1f);
    }

    public static void playSoundAmbient(SoundEvent sound, float volume, float pitch) {
        ClientPlayerEntity player = localPlayerOrNull();
        if (player != null) player.playSound(sound, volume, pitch);
    }
}
