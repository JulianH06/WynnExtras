package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.guildviewer.GV;
import julianh06.wynnextras.features.profileviewer.PV;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(Screen.class)
public class ChatClickMixin {
//    // Pattern to extract username from various command formats
//    private static final Pattern CMD_PATTERN = Pattern.compile("^/(?:msg|tell|w|p|g|r|party|guild)\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
//
//    // Pattern for Wynncraft hover text showing player info
//    private static final Pattern HOVER_NICKNAME_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16})'s nickname is");
//    private static final Pattern HOVER_PLAYER_PATTERN = Pattern.compile("^([A-Za-z0-9_]{3,16})(?:\\n|$)");
//
//    // Pattern for guild pill hover text: "... [TAG] Guild"
//    private static final Pattern GUILD_PILL_PATTERN = Pattern.compile("\\[([A-Za-z0-9]{3,4})\\] Guild");
//
//    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
//    private void onHandleTextClick(Style style, CallbackInfoReturnable<Boolean> cir) {
//        if (!WynnExtrasConfig.INSTANCE.chatClickOpensPV) return;
//        if (!Models.WorldState.onWorld()) return;
//        if (style == null) return;
//
//        String username = null;
//        String guildTag = null;
//
//        // First, try to get username from ClickEvent
//        ClickEvent clickEvent = style.getClickEvent();
//        if (clickEvent != null && clickEvent.getValue() != null) {
//            String value = clickEvent.getValue();
//            if(!value.contains("join")) {
//                WynnExtras.LOGGER.info("ChatClick - Click value: [" + value + "]");
//
//                if (clickEvent.getAction() == ClickEvent.Action.SUGGEST_COMMAND ||
//                        clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
//                    Matcher matcher = CMD_PATTERN.matcher(value);
//                    if (matcher.find()) {
//                        username = matcher.group(1);
//                    }
//                }
//            }
//        }
//
//        // Try HoverEvent for player nickname or guild pill
//        HoverEvent hoverEvent = style.getHoverEvent();
//        if (hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
//            Text hoverText = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
//            if (hoverText != null) {
//                String hoverString = hoverText.getString();
//                WynnExtras.LOGGER.info("ChatClick - Hover text: [" + hoverString + "]");
//
//                // Check for guild pill: "[TAG] Guild"
//                Matcher guildMatcher = GUILD_PILL_PATTERN.matcher(hoverString);
//                if (guildMatcher.find()) {
//                    guildTag = guildMatcher.group(1);
//                    WynnExtras.LOGGER.info("ChatClick - Found guild tag: " + guildTag);
//                }
//                // Check for player nickname if no guild found
//                else if (username == null) {
//                    // Try nickname pattern: "Username's nickname is Nickname"
//                    Matcher nicknameMatcher = HOVER_NICKNAME_PATTERN.matcher(hoverString);
//                    if (nicknameMatcher.find()) {
//                        username = nicknameMatcher.group(1);
//                    } else {
//                        // Try plain username pattern
//                        Matcher matcher = HOVER_PLAYER_PATTERN.matcher(hoverString);
//                        if (matcher.find()) {
//                            username = matcher.group(1);
//                        }
//                    }
//                }
//            }
//        }
//
//        // If still no username, try insertion text (sometimes contains player name)
//        if (username == null && guildTag == null && style.getInsertion() != null) {
//            String insertion = style.getInsertion();
//
//            if (insertion.matches("^[A-Za-z0-9_]{3,16}$") && !insertion.matches("^[A-Za-z0-9_]{3,16}'$")) {
//                username = insertion;
//            }
//        }
//
//        // Open guild viewer if guild tag found
//        if (guildTag != null && !guildTag.isEmpty()) {
//            WynnExtras.LOGGER.info("ChatClick - Opening GV for: " + guildTag);
//            GV.open(guildTag);
//            cir.setReturnValue(true);
//            return;
//        }
//
//        // Open profile viewer if username found
//        if (username != null && !username.isEmpty()) {
//            WynnExtras.LOGGER.info("ChatClick - Opening PV for: " + username);
//            PV.open(username);
//            cir.setReturnValue(true);
//            return;
//        }
//
//        // No match found, let normal click handling proceed
//    }
}
