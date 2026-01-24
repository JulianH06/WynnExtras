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
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;


@WEModule
public class ChatNotificator {
    private static WynnExtrasConfig config;

    private static Command testCmd = new Command(
            "notifiertest",
            "",
            context -> {
                ChatUtils.displayTitle("test", "", config.textDurationInMs/50, config.textColor.getFormatting());
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.notificationSound.getSoundId())), config.soundVolume, config.soundPitch);
                return 1;
            },
            null,
            null
    );

    @SubscribeEvent
    void recieveMessageGame(ChatEvent event) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
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
                ChatUtils.displayTitle(parts[1], "", config.textDurationInMs/50, config.textColor.getFormatting());
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.notificationSound.getSoundId())), config.soundVolume, config.soundPitch);
            }
        }
    }
}