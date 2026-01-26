package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.chat.ChatManager;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final Pattern CMD_PATTERN = Pattern.compile("^/(?:msg|tell|w|p|g|r|party|guild)\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOVER_NICKNAME_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16})'s (?:real username|nickname) is");
    private static final Pattern HOVER_PLAYER_PATTERN = Pattern.compile("^([A-Za-z0-9_]{3,16})(?:\\n|$)");
    private static final Pattern GUILD_PILL_PATTERN = Pattern.compile("\\[([A-Za-z0-9]{3,4})\\] Guild");

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (message == null || message.isEmpty() || ChatManager.currentChannel == ChatManager.ChatChannel.ALL) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if(message.matches("^/a\\s.*")) {
            message = message.substring(2);
            player.networkHandler.sendChatMessage(message);
            mc.inGameHud.getChatHud().addToMessageHistory(message);
            ci.cancel();
            return;
        }
        if(message.matches("^/ac\\s.*")) {
            message = message.substring(3);
            player.networkHandler.sendChatMessage(message);
            mc.inGameHud.getChatHud().addToMessageHistory(message);
            ci.cancel();
            return;
        }

        if (message.startsWith("/")) return;

        String processed = ChatManager.processMessageForSend(message);
        player.networkHandler.sendChatMessage(processed);

        if (addToHistory) {
            mc.inGameHud.getChatHud().addToMessageHistory(message);
        }

        ci.cancel();
    }

    @Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
    private void onHandleClickEvent(Style style, boolean insert, CallbackInfoReturnable<Boolean> cir) {
        if (!WynnExtrasConfig.INSTANCE.chatClickOpensPV) return;
        if (!Models.WorldState.onWorld()) return;
        if (style == null) return;

        String username = null;
        String guildTag = null;

        // Check ClickEvent for username
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent != null) {
            String value = null;

            if (clickEvent instanceof ClickEvent.SuggestCommand suggestCommand) {
                value = suggestCommand.command();
            } else if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
                value = runCommand.command();
            }

            if (value != null) {
                Matcher matcher = CMD_PATTERN.matcher(value);
                if (matcher.find()) {
                    username = matcher.group(1);
                }
            }
        }

        // Check HoverEvent for guild pill or username
        HoverEvent hoverEvent = style.getHoverEvent();
        if (hoverEvent instanceof HoverEvent.ShowText showText) {
            Text hoverText = showText.value();
            if (hoverText != null) {
                String hoverString = hoverText.getString();

                // Check for guild pill FIRST - short hover text like "[SEQ] Guild"
                Matcher guildMatcher = GUILD_PILL_PATTERN.matcher(hoverString);
                if (guildMatcher.find() && hoverString.length() < 50) {
                    guildTag = guildMatcher.group(1);
                }
                // Only look for username from hover if no guild tag and no username from command
                else if (username == null) {
                    Matcher nicknameMatcher = HOVER_NICKNAME_PATTERN.matcher(hoverString);
                    if (nicknameMatcher.find()) {
                        username = nicknameMatcher.group(1);
                    } else {
                        Matcher matcher = HOVER_PLAYER_PATTERN.matcher(hoverString);
                        if (matcher.find()) {
                            username = matcher.group(1);
                        }
                    }
                }
            }
        }

        // Try insertion text as fallback
        if (username == null && guildTag == null && style.getInsertion() != null) {
            String insertion = style.getInsertion();
            if (insertion.matches("^[A-Za-z0-9_]{3,16}$")) {
                username = insertion;
            }
        }

        // Open guild viewer if guild pill clicked (priority)
        if (guildTag != null && !guildTag.isEmpty()) {
            GV.open(guildTag);
            cir.setReturnValue(true);
            return;
        }

        // Open profile viewer if username found
        if (username != null && !username.isEmpty()) {
            PV.open(username);
            cir.setReturnValue(true);
        }
    }
}
