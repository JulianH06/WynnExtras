package julianh06.wynnextras.features.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class ChatManager {
    public enum ChatChannel {
        ALL, PARTY, GUILD
    }

    private static ChatChannel currentChannel = ChatChannel.ALL;

    public static ChatChannel getCurrentChannel() {
        return currentChannel;
    }

    public static void setCurrentChannel(ChatChannel channel) {
        currentChannel = channel;
        sendSystemMessage("[WynnExtras] You are now in the " + channel.name() + " channel");
    }

    private static void sendSystemMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if(player != null) {
            player.sendMessage(Text.literal(message), false);
        }
    }

    /** Fügt Prefix nur bei eigenen Nachrichten hinzu */
    public static String processMessageForSend(String message) {
        return switch(currentChannel) {
            case PARTY -> "/p " + message;
            case GUILD -> "/g " + message;
            default -> message;
        };
    }
}
