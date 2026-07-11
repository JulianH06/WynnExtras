package julianh06.wynnextras.features.chat.mediapreview;

import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import net.minecraft.text.Text;

import java.util.List;

@WEModule
public class ChatMediaPreviewCommand {
    private static final String TEST_GIF = "https://tenor.com/view/chud-cat-kitty-silly-cat-gif-13005602971504799451";

    private static final Command mediaPreviewCmd = new Command(
            "mediapreviewtest",
            "",
            ctx -> {
                sendStatus();
                sendTestLink("GIF", TEST_GIF);
                return 1;
            },
            null,
            null
    );

    private static void sendTestLink(String label, String url) {
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§dMedia Preview Test §7(" + label + "): §f" + url)));
    }

    private static void sendStatus() {
        if (WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) return;
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(
                "§eChat Media Preview is disabled. Enable it in /we config > Chat to hover these test links.")));
    }
}