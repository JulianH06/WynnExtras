package julianh06.wynnextras.features.chat;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.utils.ChatUtils;
import me.shedaniel.math.Color;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;


@WEModule
public class ChatNotificator {
    private static WynnExtrasConfig config;

    private static Command testCmd;

    @SubscribeEvent
    void recieveMessageGame(ChatEvent event) {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }
        if(testCmd == null) {
            testCmd = new Command(
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
        }
        notify(event.message);
    }

    public static void onPlayerChatReceived(Text message) {
        notify(message);
        RaidChatNotifier.handleMessage(message);
    }

    private static void notify(Text message) {
        for(String notificator : config.notifierWords) {
            if(!notificator.contains("|")) return;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                //CustomColor textColor = CustomColor.fromHexString(config.TextColor);
                ChatUtils.displayTitle(parts[1], "", config.TextDurationInMs/50, Formatting.byName(config.TextColor));
                McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(config.Sound)), config.SoundVolume, config.SoundPitch);
            }
        }
    }
}