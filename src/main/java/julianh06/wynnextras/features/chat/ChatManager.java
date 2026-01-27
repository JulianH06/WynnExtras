package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.loader.WELoader;
import julianh06.wynnextras.event.ChatEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import org.lwjgl.glfw.GLFW;

@WEModule
public class ChatManager implements WELoader {

    public enum ChatChannel { ALL, PARTY, GUILD }

    public static ChatChannel currentChannel = ChatChannel.ALL;
    private static boolean awaitingRawInput = false;

    public static ChatChannel getCurrentChannel() { return currentChannel; }

    public static void setCurrentChannel(ChatChannel channel) {
        currentChannel = channel;

        String channelColor;
        switch (channel) {
            case PARTY -> channelColor = "§e";
            case GUILD -> channelColor = "§b";
            default -> channelColor = "§f";
        }

        McUtils.sendMessageToClient(
             WynnExtras.addWynnExtrasPrefix(Text.of("§dYou are now in the " + channelColor + channel.name() + "§d channel"))
        );
    }

    private static long rawInputExpireTime = 0;

    @SubscribeEvent
    public void onChatMessage(ChatEvent event) {
       String msg = event.message.getString().toLowerCase();

       boolean containsCancel = msg.contains("cancel") || msg.contains("clear");
       if (currentChannel != ChatChannel.ALL && msg.contains("type") && containsCancel) {
           awaitingRawInput = true;
           rawInputExpireTime = System.currentTimeMillis() + 30_000;
       }
    }

    @SubscribeEvent
    public void onEscape(KeyInputEvent event) {
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            awaitingRawInput = false;
        }
    }

    public static String processMessageForSend(String message) {

       if (awaitingRawInput) {
           if (System.currentTimeMillis() > rawInputExpireTime) {
               awaitingRawInput = false;
               return switch(currentChannel) {
                   case PARTY -> "/p " + message;
                   case GUILD -> "/g " + message;
                   default -> message;
               };
           }
           awaitingRawInput = false;
           return message;
       }

       return switch(currentChannel) {
           case PARTY -> "/p " + message;
           case GUILD -> "/g " + message;
           default -> message;
       };
    }
}
