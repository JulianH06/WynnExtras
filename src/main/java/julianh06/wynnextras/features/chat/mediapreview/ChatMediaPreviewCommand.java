package julianh06.wynnextras.features.chat.mediapreview;

import com.mojang.brigadier.arguments.StringArgumentType;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.text.Text;

import java.util.List;

@WEModule
public class ChatMediaPreviewCommand {
    private static final List<TestLink> TEST_LINKS = List.of(
            new TestLink("FxTwitter WebP", "https://gif.fxtwitter.com/tweet_video/HNfKdlnWkAAiO5j.webp"),
            new TestLink("7TV AVIF", "https://cdn.7tv.app/emote/01KWY70F4HRW0TNMFF1AD4ZNNE/4x.avif"),
            new TestLink("7TV GIF", "https://cdn.7tv.app/emote/01KWY70F4HRW0TNMFF1AD4ZNNE/4x.gif"),
            new TestLink("Klipy page", "https://klipy.com/gifs/meo-7"),
            new TestLink("Tenor control", "https://tenor.com/view/chud-cat-kitty-silly-cat-gif-13005602971504799451")
    );

    private static final Command mediaPreviewCmd = new Command(
            "mediapreviewtest",
            "",
            ctx -> {
                sendStatus();
                try {
                    String url = StringArgumentType.getString(ctx, "url").trim();
                    sendTestLink("Custom", url);
                    return 1;
                } catch (IllegalArgumentException ignored) { }
                for (TestLink testLink : TEST_LINKS) {
                    sendTestLink(testLink.label, testLink.url);
                }
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("url", StringArgumentType.greedyString()))
    );

    private static void sendTestLink(String label, String url) {
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("§dMedia Preview Test §7(" + label + "): §f" + url)));
    }

    private static void sendStatus() {
        if (WynnExtrasConfig.INSTANCE.chatMediaPreviewEnabled) return;
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of(
                "§eChat Media Preview is disabled. Enable it in /we config > Chat to hover these test links.")));
    }

    private record TestLink(String label, String url) {
    }
}