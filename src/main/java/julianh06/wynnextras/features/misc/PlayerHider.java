package julianh06.wynnextras.features.misc;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.core.persisted.config.ConfigManager;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.config.simpleconfig.annotations.Config;
import julianh06.wynnextras.config.simpleconfig.serializer.ConfigSerializer;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static julianh06.wynnextras.features.render.PlayerRenderFilter.*;

public class PlayerHider {
    private static WynnExtrasConfig config;

    //    private static SubCommand subCommandTest = new SubCommand(
//           "subTest",
//           "subTest.",
//           context -> {
//               McUtils.sendMessageToClient(Text.of("subcommand PersonalStorageUtils worked"));
//               return 1;
//           }, null,
//            ClientCommandManager.argument("PersonalStorageUtils", StringArgumentType.greedyString())
//    );
//
//    private static Command testCommand = new Command(
//            "PersonalStorageUtils",
//            "PersonalStorageUtils.",
//            context -> {
//                //String arg = StringArgumentType.getString(context, "PersonalStorageUtils");
//                McUtils.sendMessageToClient(Text.of("PersonalStorageUtils worked"));
//                return 1;
//            },
//            subCommandTest,
//            null
//    );

    private static SubCommand toggleSubCmd;

    private static SubCommand addSubCmd;

    private static SubCommand removeSubCmd;

    private static Command playerhiderCmd;

    static boolean inNotg = false;

    static boolean commandsInitialized = false;

    public static void registerBossPlayerHider() {
        if(config == null) {
            config = SimpleConfig.getInstance(WynnExtrasConfig.class);
        }

        ClientTickEvents.START_CLIENT_TICK.register((tick) -> {

            if(config != null && !commandsInitialized) {
                toggleSubCmd = new SubCommand(
                        "toggle",
                        "",
                        context -> {
                            config.partyMemberHide = !config.partyMemberHide;
                            if(config.partyMemberHide) {
                                McUtils.sendMessageToClient(Text.of("[Wynnextras] Enabled Playerhider"));
                            } else {
                                McUtils.sendMessageToClient(Text.of("[Wynnextras] Disabled Playerhider"));
                            }
                            SimpleConfig.save(WynnExtrasConfig.class);
                            return 1;
                        },
                        null,
                        null
                );

                addSubCmd = new SubCommand(
                        "add",
                        "",
                        context -> {
                            String arg = StringArgumentType.getString(context, "added");
                            if(arg.isEmpty()) {
                                McUtils.sendMessageToClient(Text.of("Name argument is empty! Usage: /WynnExtras playerhider add <player>"));
                                return 1;
                            }
                            config.hiddenPlayers.add(arg);
                            McUtils.sendMessageToClient(Text.of("Added " + arg + " to the player hider list."));
                            SimpleConfig.save(WynnExtrasConfig.class);
                            return 1;
                        },
                        null,
                        ClientCommandManager.argument("player", StringArgumentType.word())
                );

                removeSubCmd = new SubCommand(
                        "remove",
                        "",
                        context -> {
                            String arg = StringArgumentType.getString(context, "removed");
                            if(arg.isEmpty()) {
                                McUtils.sendMessageToClient(Text.of("Name argument is empty! Usage: /WynnExtras playerhider remove <player>"));
                                return 1;
                            }
                            boolean removed = config.hiddenPlayers.remove(arg);
                            if(removed) {
                                McUtils.sendMessageToClient(Text.of("Removed " + arg + " from the player hider list."));
                                SimpleConfig.save(WynnExtrasConfig.class);
                            } else {
                                McUtils.sendMessageToClient(Text.of("Player is not in the player hider list!"));
                            }
                            return 1;
                        },
                        null,
                        ClientCommandManager.argument("player", StringArgumentType.word())
                );

                playerhiderCmd = new Command(
                        "playerhider",
                        "",
                        context -> { return 1; },
                        List.of(
                                addSubCmd,
                                removeSubCmd,
                                toggleSubCmd
                        ),
                        null
                );

                commandsInitialized = true;
            }
            int Distance = SimpleConfig.getInstance(WynnExtrasConfig.class).maxHideDistance;

            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null) { return; }
            ClientPlayerEntity me = client.player;

            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == null) {
                    if(config.printDebugToConsole) {
                        System.out.println("PLAYER == NULL");
                    }
                    return;
                }

                if (player == me) {
                    continue;
                }

                if(!config.partyMemberHide || (config.onlyInNotg && !inNotg)) {
                    if(isHidden(player)) { show(player); }
                    return;
                }

                double distance = player.getPos().distanceTo(me.getPos());
                if(config.printDebugToConsole) {
                    System.out.println("Distance to " + player.getName() + " is: " + distance + " max hide distance is: " + Distance);
                }
                if (distance >= Distance) {
                    if(isHidden(player)) { show(player); }
                    continue;
                }

                if(config.hiddenPlayers.toString().toLowerCase().contains(player.getName().getString().toLowerCase())) {
                    hide(player);
                } else {
                    if(isHidden(player)) { show(player); }
                }
            }
        });
    }

    public static void onRaidStarted(RaidKind raid) {
        if(raid.getAbbreviation().equals("NOG")){
            inNotg = true;
        }
    }

    public static void onRaidEnded(RaidInfo info) {
        if(info.getRaidKind().getAbbreviation().equals("NOG")){
            inNotg = false;
        }
    }
}
