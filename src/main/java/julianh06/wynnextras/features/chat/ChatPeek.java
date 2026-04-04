package julianh06.wynnextras.features.chat;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.KeyInputEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;


@WEModule
public class ChatPeek {
    public static boolean isPeeking = false;

    @SubscribeEvent
    public void onKey(KeyInputEvent event) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.chatPeekEnabled) {
            isPeeking = false;
            return;
        }

        int boundKey = config.chatPeekKey;
        if (event.getKey() != boundKey) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;

        if (config.chatPeekToggle) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                isPeeking = !isPeeking;
            }
        } else {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                isPeeking = true;
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                isPeeking = false;
            }
        }
    }

    public static boolean onScroll(double verticalAmount) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!isPeeking) return false;
        if (!config.chatPeekEnabled) return false;
        if (config.chatPeekAllowVanillaScroll) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return false;

        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (verticalAmount > 0) {
            chatHud.scroll(1);
        } else if (verticalAmount < 0) {
            chatHud.scroll(-1);
        }
        return true;
    }
}