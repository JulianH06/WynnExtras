package julianh06.wynnextras.features.chat;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
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
    private static WynnExtrasConfig config;

    private static Command testCmd = new Command(
            "notifiertest",
            "",
            context -> {
//                CustomColor textColor = CustomColor.fromHexString(config.TextColor);
                ChatUtils.displayTitle("test", "", config.TextDurationInMs/50, Formatting.byName(config.TextColor));
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.Sound)), config.SoundVolume, config.SoundPitch);
//                System.out.println(Formatting.byColorIndex(textColor.asInt()));
                return 1;
            },
            null,
            null
    );

    @SubscribeEvent
    void recieveMessageGame(ChatEvent event) {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }
        notify(event.message);
    }

    private static void notify(Text message) {
        if(message.getString().equals("You feel like thousands of eyes")) RaidChatNotifier.disableChiropUntil = Time.now().timestamp() + 90_000;

        if(config == null) return;
        for(String notificator : config.notifierWords) {
            if(!notificator.contains("|")) return;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                ChatUtils.displayTitle(parts[1], "", config.TextDurationInMs/50, Formatting.byName(config.TextColor));
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.Sound)), config.SoundVolume, config.SoundPitch);
            }
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class) == null) return;

        WynnExtrasConfig.NotificationConfig notificationConfig = SimpleConfig.getInstance(WynnExtrasConfig.class).notificationConfig;
        if(notificationConfig == null) return;

        notificationConfig.syncPremades();

        for(Map.Entry<String, Boolean> entry : notificationConfig.premades.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            if(parts.length != 2) continue;
            String trigger = parts[0];
            String display = parts[1];
            boolean enabled = entry.getValue();

            if(!enabled) continue;

            if(message.getString().toLowerCase().contains(trigger.toLowerCase())) {
                ChatUtils.displayTitle(display, "", config.TextDurationInMs/50, Formatting.byName(config.TextColor));
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.Sound)), config.SoundVolume, config.SoundPitch);
            }
        }
    }
}