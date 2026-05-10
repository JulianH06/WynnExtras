package julianh06.wynnextras.features.guildviewer;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ClickEvent;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.data.Guild;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.mixin.Accessor.BannerBlockEntityAccessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.registry.entry.RegistryEntry;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@WEModule
public class GV {
    public static boolean inGV = false;
    static boolean commandsInitialized = false;
    private static Command gvCmd;
    private static Command gvCmdNoArgs;

    public static String currentGuild = "";
    public static GuildData currentGuildData;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register((client) -> {
            if(commandsInitialized) return;

            gvCmd = new Command(
                    "gv",
                    "",
                    context -> {
                        String arg = StringArgumentType.getString(context, "guild");
                        open(arg);
                        return 1;
                    },
                    null,
                    List.of(ClientCommandManager.argument("guild", StringArgumentType.word()))
            );

            gvCmdNoArgs = new Command(
                    "gv",
                    "",
                    context -> {
                        openOwnGuild();
                        return 1;
                    },
                    null,
                    null
            );
            commandsInitialized = true;
        });
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if(inGV) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new GVScreen(currentGuild)));
            inGV = false;
        }
    }

    @SubscribeEvent
    void onInput(KeyInputEvent event) {
        if(event.getKey() != GLFW.GLFW_KEY_ENTER || event.getAction() != GLFW.GLFW_PRESS) return;
        if(GVScreen.searchBar != null) {
            open(GVScreen.searchBar.getInput());
        }
    }

    public static void open(String guild) {
        currentGuildData = null;
        WynncraftApiHandler.fetchGuildData(guild).thenAccept(guildData -> {
            currentGuildData = guildData;

            BlockState state = Blocks.WHITE_BANNER.getDefaultState();

            GVScreen.bannerBlockEntity = new BannerBlockEntity(
                    MinecraftClient.getInstance().player.getBlockPos().add(-1,0,0),
                    state,
                    GVScreen.dyeColorFromName(currentGuildData.banner.base)
            );

            GVScreen.bannerBlockEntity.setWorld(MinecraftClient.getInstance().world);

            BannerPatternsComponent.Builder builder = new BannerPatternsComponent.Builder();

            for (GuildData.BannerLayer layer : GV.currentGuildData.banner.layers) {
                RegistryEntry<BannerPattern> entry =
                        GVScreen.resolvePatternEntry(layer.pattern.toUpperCase());
                if (entry != null) {
                    builder.add(entry, GVScreen.dyeColorFromName(layer.colour));
                }
            }

            ((BannerBlockEntityAccessor) GVScreen.bannerBlockEntity)
                    .setPatterns(builder.build());
        }).exceptionally(ex -> {
            WynnExtras.LOGGER.error("Error while getting the data: " + ex.getMessage());
            return null;
        });

        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(null));
        currentGuild = guild;
        inGV = true;
    }

    public static void openOwnGuild() {
        WynncraftApiHandler.fetchPlayerData(McUtils.playerName()).thenAccept(playerData -> {
            if (playerData == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Could not load your player data. Try again or use /gv [guild prefix]."));
                return;
            }

            Guild guild = playerData.getGuild();
            if (guild == null || guild.getPrefix() == null || guild.getPrefix().isBlank()) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("You are not in a guild. Usage: /gv [guild prefix]"));
                return;
            }

            open(guild.getPrefix());
        }).exceptionally(ex -> {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Could not determine your guild. Usage: /gv [guild prefix]"));
            WynnExtras.LOGGER.error("Error while getting own guild data: " + ex.getMessage());
            return null;
        });
    }

    @SubscribeEvent
    void onClick(ClickEvent event) {
        //GVScreen.onClick();
    }
}
