package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.utils.ChatUtils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;


@WEModule
public class ChatNotificator {
    private static Command testCmd = new Command(
            "notifiertest",
            "",
            context -> {
                displayAndPlaySound("test");
                return 1;
            },
            null,
            null
    );

    @SubscribeEvent
    void recieveMessageGame(ChatEvent event) {
        notify(event.message);
    }

    private static void notify(Text message) {
        if(message.getString().contains("You feel like thousands of eyes")) RaidChatNotifier.disableChiropUntil = Time.now().timestamp() + 90_000;

        for(String notificator : WynnExtrasConfig.INSTANCE.notifierWords) {
            if(!notificator.contains("|")) return;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                displayAndPlaySound(parts[1]);
            }
        }

        WynnExtrasConfig.INSTANCE.syncPremades();

        for(Map.Entry<String, Boolean> entry : WynnExtrasConfig.INSTANCE.premades.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            if(parts.length != 2) continue;
            String trigger = parts[0];
            String display = parts[1];
            boolean enabled = entry.getValue();

            if(!enabled) continue;

            if(message.getString().toLowerCase().contains(trigger.toLowerCase())) {
                displayAndPlaySound(display);
            }
        }
    }

    private static void displayAndPlaySound(String display) {
        ChatUtils.displayTitle(display, "", WynnExtrasConfig.INSTANCE.textDurationInMs / 50, WynnExtrasConfig.INSTANCE.textColor.getFormatting());
        McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.notificationSound.getSoundId())), WynnExtrasConfig.INSTANCE.soundVolume / 100, WynnExtrasConfig.INSTANCE.soundPitch / 100);
    }
}