package julianh06.wynnextras.features.misc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.core.components.Models;
import com.wynntils.models.raid.raids.RaidKind;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static julianh06.wynnextras.features.render.PlayerRenderFilter.*;

public class PlayerHider {
    private static Set<String> hiddenPlayersLower = new HashSet<>();
    private static int lastHiddenListSize = -1;

    private static void rebuildHiddenSet() {
        List<String> list = WynnExtrasConfig.INSTANCE.hiddenPlayers;
        if (list.size() != lastHiddenListSize) {
            hiddenPlayersLower = new HashSet<>();
            for (String name : list) hiddenPlayersLower.add(name.toLowerCase());
            lastHiddenListSize = list.size();
        }
    }

    private static SubCommand toggleSubCmd;

    private static SubCommand toggleWarSubCmd;

    private static SubCommand addSubCmd;

    private static SubCommand removeSubCmd;

    private static SubCommand hideAllSubCmd;

    private static SubCommand hideAllInWarSubCmd;

    private static Command playerhiderCmd;

    static boolean inNotg = false;

    static boolean commandsInitialized = false;

    public static void registerBossPlayerHider() {
        ClientTickEvents.START_CLIENT_TICK.register((tick) -> {

            if(WynnExtrasConfig.INSTANCE != null && !commandsInitialized) {
                toggleSubCmd = new SubCommand(
                        "toggle",
                        "",
                        context -> {
                            WynnExtrasConfig.INSTANCE.playerHiderToggle = !WynnExtrasConfig.INSTANCE.playerHiderToggle;
                            if(WynnExtrasConfig.INSTANCE.playerHiderToggle) {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Enabled Playerhider")));
                            } else {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Disabled Playerhider")));
                            }
                            WynnExtrasConfig.save();
                            return 1;
                        },
                        null,
                        null
                );

                toggleWarSubCmd = new SubCommand(
                        "war",
                        "",
                        context -> {
                            WynnExtrasConfig.INSTANCE.hideAllPlayersInWar = !WynnExtrasConfig.INSTANCE.hideAllPlayersInWar;
                            if(WynnExtrasConfig.INSTANCE.hideAllPlayersInWar) {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("All players will now be hidden in a war.")));
                            } else {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Other players will now be visible again in wars.")));
                            }
                            WynnExtrasConfig.save();
                            return 1;
                        },
                        null,
                        null
                );

                addSubCmd = new SubCommand(
                        "add",
                        "",
                        context -> {
                            String arg = StringArgumentType.getString(context, "player");
                            if(arg.isEmpty()) {
                                McUtils.sendMessageToClient(Text.of("Name argument is empty! Usage: /WynnExtras playerhider add <player>"));
                                return 1;
                            }
                            WynnExtrasConfig.INSTANCE.hiddenPlayers.add(arg);
                            McUtils.sendMessageToClient(Text.of("Added " + arg + " to the player hider list."));
                            WynnExtrasConfig.save();
                            return 1;
                        },
                        null,
                        List.of(ClientCommandManager.argument("player", StringArgumentType.word()))
                );

                removeSubCmd = new SubCommand(
                        "remove",
                        "",
                        context -> {
                            String arg = StringArgumentType.getString(context, "player");
                            if(arg.isEmpty()) {
                                McUtils.sendMessageToClient(Text.of("Name argument is empty! Usage: /WynnExtras playerhider remove <player>"));
                                return 1;
                            }
                            boolean removed = WynnExtrasConfig.INSTANCE.hiddenPlayers.remove(arg);
                            if(removed) {
                                McUtils.sendMessageToClient(Text.of("Removed " + arg + " from the player hider list."));
                                WynnExtrasConfig.save();
                            } else {
                                McUtils.sendMessageToClient(Text.of("Player is not in the player hider list!"));
                            }
                            return 1;
                        },
                        null,
                        List.of(ClientCommandManager.argument("player", StringArgumentType.word()))
                );

                hideAllSubCmd = new SubCommand(
                        "hideall",
                        "",
                        context -> {
                            WynnExtrasConfig.INSTANCE.hideAllPlayers = !WynnExtrasConfig.INSTANCE.hideAllPlayers;
                            if(WynnExtrasConfig.INSTANCE.hideAllPlayers) {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Enabled Hide All Players (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")")));
                            } else {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Disabled Hide All Players")));
                            }
                            WynnExtrasConfig.save();
                            return 1;
                        },
                        null,
                        null
                );

                hideAllInWarSubCmd = new SubCommand(
                        "hideallinwar",
                        "",
                        context -> {
                            WynnExtrasConfig.INSTANCE.hideAllPlayersInWar = !WynnExtrasConfig.INSTANCE.hideAllPlayersInWar;
                            if(WynnExtrasConfig.INSTANCE.hideAllPlayersInWar) {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Enabled Hide All Players in Wars (range: " + WynnExtrasConfig.INSTANCE.maxHideDistance + ")")));
                            } else {
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Disabled Hide All Players in Wars")));
                            }
                            WynnExtrasConfig.save();
                            return 1;
                        },
                        null,
                        null
                );

                playerhiderCmd = new Command(
                        "playerhider",
                        "",
                        context -> { return 1; },
                        List.of(
                                addSubCmd,
                                removeSubCmd,
                                toggleSubCmd,
                                toggleWarSubCmd,
                                hideAllSubCmd,
                                hideAllInWarSubCmd
                        ),
                        null
                );

                commandsInitialized = true;
            }
            int Distance = WynnExtrasConfig.INSTANCE.maxHideDistance;

            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player == null || client.world == null) { return; }
            ClientPlayerEntity me = client.player;

            for (PlayerEntity player : client.world.getPlayers()) {
                if (player == null) {
                    continue;
                }

                if (player == me) {
                    continue;
                }

                if(!WynnExtrasConfig.INSTANCE.playerHiderToggle) {
                    if(isHidden(player)) { show(player); }
                    continue;
                }

                double distance = player.getBlockPos().toBottomCenterPos().distanceTo(me.getBlockPos().toBottomCenterPos());
                if (distance >= Distance) {
                    if(isHidden(player)) { show(player); }
                    continue;
                }

                boolean inWarAndHiding = WynnExtrasConfig.INSTANCE.hideAllPlayersInWar && Models.War.isWarActive();

                rebuildHiddenSet();
                String playerName = player.getName().getString().toLowerCase();
                boolean inList = hiddenPlayersLower.contains(playerName);
                if(WynnExtrasConfig.INSTANCE.hideAllPlayers || inWarAndHiding || inList) {
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