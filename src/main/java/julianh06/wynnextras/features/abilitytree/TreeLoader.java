package julianh06.wynnextras.features.abilitytree;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.handlers.container.type.ContainerContentChangeType;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.character.type.SavableSkillPointSet;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.*;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@WEModule
public class TreeLoader {
    static final int GUI_SETTLE_TICKS = 4;
    static int ticksSinceLastAction = 0;
    static int socketClicksPerformed = 0;

    static boolean inTreeMenu = false;
    static boolean inResetMenu = false;
    static boolean wasStarted = false;
    static boolean treeMenuWasOpened = false;
    static boolean resetMenuWasOpened = false;
    static boolean wasReset = false;
    static int clickedSockets = 0;
    static HandledScreen<?> screen = null;
    static boolean resetTree = false;
    static List<String> abilitiesToClick = new ArrayList<>();
    static List<AbilityMapData.Node> abilitiesToClick2 = null;
    static AbilityTreeData classTree = null;

    static Gson gson = new GsonBuilder()
            .registerTypeAdapter(AbilityMapData.class, new AbilityMapDataDeserializer())
            .registerTypeAdapter(AbilityTreeData.class, new AbilityTreeDataDeserializer())
            .registerTypeAdapter(AbilityMapData.Icon.class, new IconDeserializer())
            .registerTypeAdapter(AbilityMapData.Node.class, new NodeDeserializer())
            .registerTypeAdapter(AbilityTreeData.Icon.class, new IconDeserializer())
            .create();

    private static Command openTreeScreen = new Command(
            "tree",
            "",
            (ctx) -> {
                WEScreen.open(TreeScreen::new);
                return 1;
            },
            null, null
    );

//    secure tree loader
    private static Command treeload = new Command(
        "treeload",
        "",
        (ctx) -> {
            TreeData.loadAll();
            String arg = StringArgumentType.getString(ctx, "tree");
            TreeData tree = TreeData.getTree(arg);
            if(tree == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                return 1;
            }
            resetAll();
            wasStarted = true;
            resetTree = true;
            List<String> abilities = new ArrayList<>(tree.input);
//            abilitiesToClicksecure(abilities);
            return 1;
        },
        null,
        List.of(ClientCommandManager.argument("tree", StringArgumentType.word()))
    );

    private static Command loadtree = new Command(
            "loadtree","",
            (ctx) -> {
                String arg = StringArgumentType.getString(ctx, "tree");
                TreeData tree = TreeData.getTree(arg);
                if(tree == null) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                    return 1;
                }
                resetAll();
                wasStarted = true;
                resetTree = true;
                abilitiesToClick = new ArrayList<>(tree.input);
                return 1;
            },
            null,
            List.of(ClientCommandManager.argument("tree", StringArgumentType.word()))
    );

//    // Command to save ability tree (minimal format)
//    private static Command saveAbilityTree = new Command(
//            "savePlayerAbilityTree",
//            "",
//            (ctx) -> {
//                String playerName = StringArgumentType.getString(ctx, "player");
//                String characterUUID = StringArgumentType.getString(ctx, "characterUUID");
//                savePlayerAbilityTree(playerName, characterUUID, className);
//                String abilityFileName = playerName + "_" + characterUUID + "_abilities.json";
//                AbilityIdConverter.convert(className, abilityFileName); // calls method from your new class with class argument
//                TreeData.loadAll();
//                return 1;
//            },
//            null,
//            List.of(ClientCommandManager.argument("player" ,StringArgumentType.word()), ClientCommandManager.argument("characterUUID" ,StringArgumentType.word()))
//    );
    private static Command convertAbilityFile = new Command(
            "convertAbilityFile",
            "",
            (ctx) -> {
                String playerclass = StringArgumentType.getString(ctx, "playerclass");
                String abilityFileName = StringArgumentType.getString(ctx, "filename");
                AbilityIdConverter.convert(playerclass, abilityFileName); // calls method from your new class with class argument
                TreeData.loadAll();
                return 1;
            },
            null,
            List.of(
                    ClientCommandManager.argument("playerclass", StringArgumentType.word()),
                    ClientCommandManager.argument("filename", StringArgumentType.word())
            )
    );


    static public void resetAll() {
        treeMenuWasOpened = false;
        resetMenuWasOpened = false;
        wasReset = false;
        clickedSockets = 0;
        resetTree = false;
        abilitiesToClick = new ArrayList<>();
        abilitiesToClick2 = null;
        ticksSinceLastAction = 0;
        socketClicksPerformed = 0;
    }

    private static class PendingClick {
        AbilityMapData.Node node;
        String abilityName; // "Unlock <Ability>" bereinigt
        int attempts;
        int ticksWaiting; // Ticks seit letztem Klick
        int expectedPage; // Seite, auf der der Klick ausgeführt wurde
        PendingClick(AbilityMapData.Node node, String abilityName, int expectedPage) {
            this.node = node;
            this.abilityName = abilityName;
            this.attempts = 0;
            this.ticksWaiting = 0;
            this.expectedPage = expectedPage;
        }
    }

    public static boolean loadSkillpoints = false;
    public static SavableSkillPointSet skillPointSet;
    public static boolean loadingSkillpoints = false;

    private static final int CLICK_CONFIRM_TIMEOUT_TICKS = 1;
    private static final int MAX_ATTEMPTS_PER_ABILITY = 15;
    private static final int GUI_SETTLE_TICKS_DEFAULT = GUI_SETTLE_TICKS; // vorhandener Wert
    private static PendingClick pendingClick = null;
    private static int lagTickCounter = 0;
    private static boolean fastMode = true;
    private static String makeNodeKey(AbilityMapData.Node node) {
        if (node == null || node.meta == null || node.coordinates == null) return UUID.randomUUID().toString();
        return node.meta.page + ":" + node.coordinates.x + ":" + (node.coordinates.y % 6);
    }
    private static boolean nodesEqual(AbilityMapData.Node a, AbilityMapData.Node b) {
        if (a == null || b == null || a.meta == null || b.meta == null || a.coordinates == null || b.coordinates == null) return false;
        return a.meta.page == b.meta.page && a.coordinates.x == b.coordinates.x && (a.coordinates.y % 6) == (b.coordinates.y % 6);
    }

    // --- Neue/zusätzliche Felder in deiner Klasse ---
    private static class PendingResetClick {
        String stage; // "socket" oder "confirm"
        int ticksWaiting;
        int attempts;
        PendingResetClick(String stage) {
            this.stage = stage;
            this.ticksWaiting = 0;
            this.attempts = 0;
        }
    }
    private static PendingResetClick pendingReset = null;
    private static final int RESET_CLICK_TIMEOUT = 5;
    private static final int MAX_RESET_ATTEMPTS = 15;

    private static boolean scrolledUp = false;
    private static ItemStack firstNode = null;
    private static long lastResetTryClick = 0;

    private static long finishedTreeTime = 0;

    public static void init() {
        TreeData.loadAll();
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> {
                    dispatcher.register(
                            ClientCommandManager.literal("loadtree")
                                    .then(
                                            ClientCommandManager.argument("tree", StringArgumentType.word())
                                                    .executes(ctx -> {
                                                        String arg = StringArgumentType.getString(ctx, "tree");
                                                        TreeData tree = TreeData.getTree(arg);
                                                        if (tree == null) {
                                                            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                                                            return 1;
                                                        }
                                                        resetAll();
                                                        wasStarted = true;
                                                        resetTree = true;
                                                        abilitiesToClick = new ArrayList<>(tree.input);
                                                        return 1;
                                                    })
                                    )
                    );
// And
//                    dispatcher.register(
//                            ClientCommandManager.literal("savePlayerAbilityTree")
//                                    .then(
//                                            ClientCommandManager.argument("player", StringArgumentType.word())
//                                                    .executes(ctx -> {
//                                                        String playerName = StringArgumentType.getString(ctx, "player");
//                                                        String playerclass = savePlayerAbilityTree(playerName).toLowerCase();
//                                                        String abilityFileName = playerName  + "_abilities.json";
//                                                        AbilityIdConverter.convert(playerclass, abilityFileName); // calls method from your new class with class argument
//                                                        TreeData.loadAll();
//                                                        return 1;
//                                                    })
//                                    )
//                    );
                    dispatcher.register(
                            ClientCommandManager.literal("convertAbilityFile")
                                    .then(
                                            ClientCommandManager.argument("playerclass", StringArgumentType.word())
                                                    .then(
                                                            ClientCommandManager.argument("filename", StringArgumentType.word())
                                                                    .executes(ctx -> {
                                                                        String playerclass = StringArgumentType.getString(ctx, "playerclass");
                                                                        String abilityFileName = StringArgumentType.getString(ctx, "filename");
                                                                        AbilityIdConverter.convert(playerclass, abilityFileName);
                                                                        return 1;
                                                                    })
                                                    )
                                    )
                    );

                }
        );

        // Main tick and automation logic (unchanged)
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            Screen currScreen = client.currentScreen;
            if (currScreen == null) return;

            if (currScreen instanceof HandledScreen) screen = (HandledScreen<?>) currScreen;
            else screen = null;

            String InventoryTitle = currScreen.getTitle().getString();
            boolean oneTrue = inTreeMenu || inResetMenu;
            inTreeMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE000");
            inResetMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE001");
            if (!inTreeMenu && !inResetMenu && !wasStarted && oneTrue) resetAll();
            if (wasStarted && inTreeMenu) wasStarted = false;
        });


// --- Ersetzter / verbesserter Reset-Tickhandler ---
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if (!resetTree) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            ClientPlayerEntity player = client.player;

            ticksSinceLastAction++;
            boolean hasTreeManipulation = Models.StatusEffect.getStatusEffects().stream()
                    .anyMatch(effect -> effect.getName().getStringWithoutFormatting().equals("Tree Manipulation"));
            //hasTreeManipulation = false;
            if (ticksSinceLastAction < GUI_SETTLE_TICKS) return;

            // --- PendingClick / Retry logic für Shards + Confirm ---
            if (pendingReset != null && screen != null) {
                pendingReset.ticksWaiting++;
                if (pendingReset.ticksWaiting >= RESET_CLICK_TIMEOUT) {
                    pendingReset.attempts++;
                    if (pendingReset.attempts > MAX_RESET_ATTEMPTS) {
                        System.out.println("Reset click '" + pendingReset.stage + "' failed after retries. Aborting reset.");
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Reset failed due to lag, please retry manually.")));
                        resetAll();
                        pendingReset = null;
                        return;
                    }
                    System.out.println("Retrying reset click stage: " + pendingReset.stage);
                    if (pendingReset.stage.equals("socket")) {
                        clickOnSockets(client, player, screen);
                    } else if (pendingReset.stage.equals("confirm")) {
                        confirmReset(client, player, screen);
                    }
                    pendingReset.ticksWaiting = 0;
                    return;
                }

                // prüfen ob Erfolg erkannt werden kann
                if (pendingReset.stage.equals("socket")) {
                    // Erfolg wenn weniger als 3 Ability Shards in screen
                    if (countOccurences("Ability Shard", screen) < 3) {
                        System.out.println("Shard clicks confirmed.");
                        pendingReset = null;
                        ticksSinceLastAction = 0;
                        return;
                    } else {
                        pendingReset.stage = "confirm";
                    }
                } else if (pendingReset.stage.equals("confirm")) {
                    // Erfolg wenn Tree Menu wieder offen oder reset beendet
                    if (inTreeMenu && !inResetMenu) {
                        System.out.println("Reset confirm acknowledged.");
                        pendingReset = null;
                        resetTree = false;
                        return;
                    }
                }

                // noch warten
                return;
            }

            // --- Normale Reset-Logik ---
            if (!treeMenuWasOpened) {
                openTreeMenu(client, player);
                return;
            }

            if (hasTreeManipulation && inTreeMenu && !resetMenuWasOpened) {
                client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 54 + 4, 1, SlotActionType.QUICK_MOVE, client.player);
                lastResetTryClick = Time.now().timestamp();
//                if (McUtils.player().getInventory().getStack(12).getItem() != Items.AIR) {
//                    client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 54 + 3, 1, SlotActionType.PICKUP, client.player);
//                    client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 54 + 3, 1, SlotActionType.PICKUP, client.player);
//                    return;
//                }
//                scrolledUp = true;
//                firstNode = McUtils.containerMenu().getSlot(4).getStack();
//                client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 4, 1, SlotActionType.PICKUP, client.player);
//                client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 4, 1, SlotActionType.PICKUP, client.player);
                resetMenuWasOpened = true;
                wasReset = true;
                return;
            }

//            if(scrolledUp && firstNode != null) {
//                if(client.interactionManager == null) return;
//                if(screen == null) return;
//                if(screen.getScreenHandler() == null) return;
//
////                if(Time.now().timestamp() - lastResetTryClick < 100) {
////                    McUtils.sendMessageToClient(Text.of("WAITING " + Math.random() * 1000));
////                    McUtils.sendMessageToClient(Text.of("--- "));
////                    return;
////                }
//
//                //McUtils.sendMessageToClient(Text.of("CLICKING AGAIN"));
//
//                //client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 4, 1, SlotActionType.PICKUP, client.player);
//                //lastResetTryClick = Time.now().timestamp();
//
//                if(McUtils.containerMenu().getSlot(4) == null) return;
//                if(McUtils.containerMenu().getSlot(4).getStack() == null) return;
//                if(McUtils.containerMenu().getSlot(4).getStack().getCustomName() == null) return;
//
//                if(McUtils.containerMenu().getSlot(4).getStack().getCustomName().getString().contains("Unlock ")) {
//                //if(ItemStack.areItemsEqual(firstNode, McUtils.containerMenu().getSlot(4).getStack())) {
//                    McUtils.sendMessageToClient(Text.of("DONEEEEE"));
//                    resetMenuWasOpened = true;
//                    wasReset = true;
//                    firstNode = null;
//                    //return;
//                    //client.interactionManager.clickSlot(screen.getScreenHandler().syncId, 4, 1, SlotActionType.PICKUP, client.player);
//                }
//            }

            if (inTreeMenu && !resetMenuWasOpened) {
                openTreeResetMenu(client, player, screen);
                return;
            }

            // Shards einlegen
            if (inResetMenu && !wasReset) {
                int shardCount = countOccurences("Ability Shard", screen);
                if (shardCount < 3) {
                    if (socketClicksPerformed < shardCount + 1) {
                        clickOnSockets(client, player, screen);
                        socketClicksPerformed++;
                        pendingReset = new PendingResetClick("socket");
                        pendingReset.ticksWaiting = 0;
                        return;
                    } else {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You don't have 3 Ability Shards in your Inventory")));
                        resetAll();
                        return;
                    }
                } else {
                    confirmReset(client, player, screen);
                    pendingReset = new PendingResetClick("confirm");
                    pendingReset.ticksWaiting = 0;
                    return;
                }
            }

            if (inTreeMenu && wasReset) {
                resetTree = false;
            }

            if (clickedSockets >= 6) {
                resetAll();
            }
        });


//        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
//            if (!resetTree) return;
//            MinecraftClient client = MinecraftClient.getInstance();
//            if (client.player == null || client.world == null) return;
//            ClientPlayerEntity player = client.player;
//            ticksSinceLastAction++;
//            boolean hasTreeManipulation = Models.StatusEffect.getStatusEffects().stream()
//                    .anyMatch(effect -> effect.getName().getStringWithoutFormatting().equals("Tree Manipulation"));
//            hasTreeManipulation = false;
//            if (ticksSinceLastAction < GUI_SETTLE_TICKS) return;
//            if (!treeMenuWasOpened) openTreeMenu(client, player);
//            else if (hasTreeManipulation && inTreeMenu && !resetMenuWasOpened) {
//                if(McUtils.player().getInventory().getStack(12).getItem() != Items.AIR) {
//                    System.out.println("GO UP");
//                    client.interactionManager.clickSlot(
//                            screen.getScreenHandler().syncId,
//                            54 + 3,
//                            1,
//                            SlotActionType.PICKUP,
//                            client.player
//                    );
//                    client.interactionManager.clickSlot(
//                            screen.getScreenHandler().syncId,
//                            54 + 3,
//                            1,
//                            SlotActionType.PICKUP,
//                            client.player
//                    );
//                    return;
//                }
//                System.out.println("HAS TREE MANIPULATION EFFECT");
//                if(client.interactionManager == null) return;
//                client.interactionManager.clickSlot(
//                        screen.getScreenHandler().syncId,
//                        4,
//                        1,
//                        SlotActionType.PICKUP,
//                        client.player
//                );
//                resetMenuWasOpened = true;
//            }
//            //else if(<player has the ability tree buff where you can freely change stuff>) <right click the very first node to reset the whole tree>
//            else if (inTreeMenu && !resetMenuWasOpened) openTreeResetMenu(client, player, screen);
//            else if (inResetMenu && !wasReset) {
//                if (countOccurences("Ability Shard", screen) < 3)
//                    if (socketClicksPerformed < countOccurences("Ability Shard", screen) + 1) {
//                        clickOnSockets(client, player, screen);
//                        socketClicksPerformed++;
//                    } else {
//                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("You don't have 3 Ability Shards in your Inventory")));
//                        resetAll();
//                    }
//                else
//                    confirmReset(client, player, screen);
//            } else if (inTreeMenu && wasReset) resetTree = false;
//            if (clickedSockets >= 6) resetAll();
//        });

        // Ability Selection
        int[] abilityClickTicks = {0};
        int[] currentPage = {1};
        AtomicInteger failCycles = new AtomicInteger(); // How many times we've cycled the list
        final int MAX_FAIL_CYCLES = 30;
//
//        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
//            if(abilitiesToClick2 == null) {
//                abilityClickTicks[0] = 0;
//                failCycles.set(0);
//                return;
//            }
//            if(abilitiesToClick2.isEmpty()) {
//                abilityClickTicks[0] = 0;
//                failCycles.set(0);
//                return;
//            }
//            if (resetTree) {
//                abilityClickTicks[0] = 0;
//                currentPage[0] = 1;
//                failCycles.set(0);
//                return;
//            }
//            if (!inTreeMenu) {
//                abilityClickTicks[0] = 0;
//                return;
//            }
//
//            MinecraftClient client = MinecraftClient.getInstance();
//            if (client.player == null || client.world == null) {
//                abilityClickTicks[0] = 0;
//                return;
//            }
//            ClientPlayerEntity player = client.player;
//            if (client.currentScreen == null) {
//                abilityClickTicks[0] = 0;
//                return;
//            }
//            HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;
//
//            abilityClickTicks[0]++;
//            if (abilityClickTicks[0] < GUI_SETTLE_TICKS) return;
//
//            if (failCycles.get() >= MAX_FAIL_CYCLES) {
//                System.out.println("Reached max cycles without unlocking abilities. Aborting!");
//                abilitiesToClick2 = null; // Or handle differently
//                abilityClickTicks[0] = 0;
//                failCycles.set(0);
//                return;
//            }
//
//            AbilityMapData.Node ability1 = abilitiesToClick2.getFirst();
//            System.out.println(ability1.meta.page + " " + currentPage[0]);
//            int pageOffset = ability1.meta.page - currentPage[0];
//
//            // Navigation logic
//            if (pageOffset > 0) {
//                clickOnAbility(client, player, "Next Page", screen);
//                currentPage[0]++;
//                abilityClickTicks[0] = 0;
//                return;
//            }
//            if (pageOffset < 0) {
//                clickOnAbility(client, player, "Previous Page", screen);
//                currentPage[0]--;
//                abilityClickTicks[0] = 0;
//                return;
//            }
//
//            // Only click if "Unlock <Ability>" is present as substring
//            if(classTree == null) return;
//            AbilityTreeData.Ability abilityFromNode = getAbilityFromNode(ability1, classTree);
//            if(abilityFromNode == null) return;
//            String name = extractAbilityNameFromHtml(abilityFromNode.name);
//            System.out.println(name);
//            if (hasUnlockPrefix(name, screen)) {
//                System.out.println("Clicking ability: Unlock " + ability1);
//                clickOnAbility(client, player, name, screen);
//                abilitiesToClick2.removeFirst();
//                if(abilitiesToClick2.isEmpty()) {
//                    System.out.println("FINISHED"); //TODO: FIX THAT IT BREAKS WHEN LAG (E.G WHEN PLAYING ON ASIA SERVERS)
//                }
//                failCycles.set(0); // Success: reset fail counter
//            } else {
//                System.out.println("cant click");
//                // If not present, move ability one spot down in the list and try unlocking the next one first
//                if (abilitiesToClick2.size() > 1) {
//                    abilitiesToClick2.add(Math.min(failCycles.get(), abilitiesToClick2.size() - 1), abilitiesToClick2.removeFirst());
//                    failCycles.set(failCycles.get() + 1); // Count a cycle only if list is requeued
//                }
//            }
//
//            abilityClickTicks[0] = 0;
//        });

        AtomicBoolean pendingPageSwitch = new AtomicBoolean(false);
        AtomicReference<List<ItemStack>> prevPageStacks = new AtomicReference<>(new ArrayList<>());
        final int PAGE_SWITCH_TIMEOUT = 40; // ~2 Sekunden
        AtomicInteger pageSwitchTicks = new AtomicInteger();


        // --- Ersetzter/verbesserter Tick-Handler (Ability Selection) ---
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            // Basisprüfungen (unverändert)
            if(Time.now().timestamp() - lastResetTryClick < 1000) return; //wait a second after resetting the tree

            if (abilitiesToClick2 == null || abilitiesToClick2.isEmpty()) {
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                pendingClick = null;
                return;
            }
            if (resetTree) {
                abilityClickTicks[0] = 0;
                currentPage[0] = 1;
                failCycles.set(0);
                pendingClick = null;
                return;
            }
            if (!inTreeMenu) {
                abilityClickTicks[0] = 0;
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.currentScreen == null) {
                abilityClickTicks[0] = 0;
                return;
            }
            ClientPlayerEntity player = client.player;
            HandledScreen<?> screen = (HandledScreen<?>) client.currentScreen;

            abilityClickTicks[0]++;
            if (abilityClickTicks[0] < GUI_SETTLE_TICKS_DEFAULT) return;

            // Wenn gerade ein Page-Switch läuft: warten bis Inventar anders aussieht
            if (pendingPageSwitch.get()) {
                pageSwitchTicks.incrementAndGet();

                List<ItemStack> inv = new ArrayList<>(McUtils.containerMenu().getStacks());
                boolean changed = false;
                int i = 0;
                for(ItemStack stack : prevPageStacks.get()) {
                    if(stack == null) { i++; continue; }
                    if(stack.getItem().equals(Items.AIR)) { i++; continue; }

                    ItemStack invStack = inv.get(i);
                    if(invStack == null) { i++; continue; }
                    if(invStack.getItem().equals(Items.AIR)) { i++; continue; }
                    i++;

                    if(!ItemStack.areItemsEqual(invStack, stack)) {
                        //McUtils.sendMessageToClient(Text.of("CHANGED:" + invStack.getCustomName() + " AAAA "
                        //+ stack.getCustomName()));
                        //McUtils.sendMessageToClient(Text.of("---------"));
                        changed = true;
                        break;
                    }
                }

                if (changed) {
                    System.out.println("Page switch confirmed after " + pageSwitchTicks + " ticks.");
                    pendingPageSwitch.set(false);
                    abilityClickTicks[0] = 0;
                    prevPageStacks.get().clear();
                    return;
                }

                if (pageSwitchTicks.get() > PAGE_SWITCH_TIMEOUT) {
                    System.out.println("Page switch timed out! Retrying...");
                    pendingPageSwitch.set(false);
                    prevPageStacks.get().clear();
                    return;
                }

                // Solange der Wechsel nicht fertig ist → alles andere aussetzen
                return;
            }


            if (failCycles.get() >= MAX_FAIL_CYCLES) {
                System.out.println("Reached max cycles without unlocking abilities. Aborting!");
                resetAll();
                if(McUtils.mc().currentScreen != null) McUtils.mc().currentScreen.close();
                McUtils.sendMessageToClient(Text.of("Something went wrong! Try again"));
                abilitiesToClick2 = null;
                abilityClickTicks[0] = 0;
                failCycles.set(0);
                pendingClick = null;
                return;
            }

            // Wenn ein Klick pending ist: prüfen ob bestätigt oder timeout -> retry oder skip
            if (pendingClick != null) {
                pendingClick.ticksWaiting++;
                //System.out.println(pendingClick.ticksWaiting);

                boolean stillHasUnlock = hasUnlockPrefix(pendingClick.abilityName, screen);

                // Fast Confirm: Wenn Button verschwindet → sofort weiter
                if (!stillHasUnlock) {
                    McUtils.sendMessageToClient(Text.of("UNLOCKED: " + pendingClick.abilityName));
                    System.out.println("Fast confirm: " + pendingClick.abilityName);
                    abilitiesToClick2.removeFirst();
                    failCycles.set(0);
                    pendingClick = null;
                    abilityClickTicks[0] = 0;
                    fastMode = true; // zurück in fast mode
                } else {
                    System.out.println("LAG");
                    // Wenn Unlock noch da ist
                    if (pendingClick.ticksWaiting >= 2) { // nur 2 Ticks warten für schnelles Feedback
                        // Wir nehmen an, es laggt → wechsel in "slow mode"
                        fastMode = false;
                        lagTickCounter++;
                        if (lagTickCounter > CLICK_CONFIRM_TIMEOUT_TICKS) {
                            System.out.println("Lag detected -> retrying " + pendingClick.abilityName);
                            clickOnAbility(client, player, pendingClick.abilityName, screen);
                            pendingClick.ticksWaiting = 0;
                            lagTickCounter = 0;
                        }
                        return;
                    }
                }
            }
            // end pendingClick handling
            if(abilitiesToClick2 == null) return;
            if(abilitiesToClick2.isEmpty()) {
                resetAll();
                if(McUtils.mc().currentScreen != null) McUtils.mc().currentScreen.close();
                McUtils.sendMessageToClient(Text.of("Done! Skillpoints? " + loadSkillpoints));
                if(skillPointSet != null) {
                    int currentSlot = player.getInventory().selectedSlot;
                    player.getInventory().selectedSlot = 7;
                    client.interactionManager.interactItem(player, Hand.MAIN_HAND);
                    player.getInventory().selectedSlot = currentSlot;
                    loadingSkillpoints = true;
                    finishedTreeTime = Time.now().timestamp();
//                    loadSkillpoints(skillPointSet);
                }
                return;
            }

            AbilityMapData.Node abilityNode = abilitiesToClick2.getFirst();
            AbilityTreeData.Ability abilityFromNode = getAbilityFromNode(abilityNode, classTree);
            if (abilityFromNode == null) return; //{ abilitiesToClick2.removeFirst(); return; }
            String abilityName = extractAbilityNameFromHtml(abilityFromNode.name);
            if (abilityName == null) return; //{ abilitiesToClick2.removeFirst(); return; }

            //TODO: auf as1 werden die ersten zwei abilities unlocked und die dritte nicht und dann pfeil hoch/runter bis abbruch
            //TODO: ich vermute, dass es lagt, dadurch hat die dritte ability noch nicht den unlock prefix, dadurch wird es hinter geschoben,
            //TODO: dadurch wird die ability auf der naechsten seite versucht, es wird next page geklickt, was wiederrum lagt, weshalb
            //TODO: der loader nicht mehr weiß auf welcher page er ist :steamhappy:
            //wenn page switch: pfeil pending machen, itemstacks speichern, clicken, stacks durch iterieren, sobald einer anders ist: page switch hat geklappt
            int pageOffset = abilityNode.meta.page - currentPage[0];
            if (pageOffset != 0 && !pendingPageSwitch.get()) {
                List<ItemStack> inv = new ArrayList<>(McUtils.containerMenu().getStacks());
                prevPageStacks.set(inv);

                String direction = pageOffset > 0 ? "Next Page" : "Previous Page";
                clickOnAbility(client, player, direction, screen);
                currentPage[0] += pageOffset > 0 ? 1 : -1;

                pendingPageSwitch.set(true);
                pageSwitchTicks.set(0);
                System.out.println("Initiated page switch: " + direction);
                return;
            }

            if (hasUnlockPrefix(abilityName, screen)) {
                clickOnAbility(client, player, abilityName, screen);
                pendingClick = new PendingClick(abilityNode, abilityName, currentPage[0]);
                pendingClick.ticksWaiting = 0;

                // Fast mode → sofort nächste Node vorbereiten (nicht warten)
                if (fastMode) {
                    abilityClickTicks[0] = 0; // keine Pause
                } else {
                    abilityClickTicks[0] = 1; // minimal delay
                }
                return;
            } else {
                if (abilitiesToClick2.size() > 1) {
                    AbilityMapData.Node removed = abilitiesToClick2.removeFirst();
                    abilitiesToClick2.add(Math.min(failCycles.get(), abilitiesToClick2.size() - 1), removed);
                    failCycles.set(failCycles.get() + 1); // Count a cycle only if list is requeued
                    McUtils.sendMessageToClient(Text.of(abilityName + " Es muss hinter geschoben werden"));
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            if(!loadingSkillpoints || skillPointSet == null) return;

            if(Time.now().timestamp() - finishedTreeTime < 600) {
                MinecraftClient.getInstance().interactionManager.clickSlot(screen.getScreenHandler().syncId, 4, 0, SlotActionType.QUICK_MOVE,McUtils.player());

                return;
            }

            int finishedSkillPoints = 0;
            int[] pointArray = skillPointSet.getSkillPointsAsArray();
            for (int i = 0; i < 5; i++) {
                int remainingPoints = pointArray[i];
                if(remainingPoints == 0) {
                    finishedSkillPoints++;
                    continue;
                }

                if(remainingPoints % 5 == 0) {
                    //11
                    //MinecraftClient.getInstance().player.networkHandler.sendPacket(new ClientCommandC2SPacket(McUtils.player(), ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    MinecraftClient.getInstance().interactionManager.clickSlot(screen.getScreenHandler().syncId, 11 + i, 0, SlotActionType.QUICK_MOVE,McUtils.player());
                    //clickSlotHelper(11 + i, screen, MinecraftClient.getInstance());

                    //McUtils.containerMenu().onSlotClick(11 + i, 0, SlotActionType.QUICK_MOVE, McUtils.player());
                    //MinecraftClient.getInstance().player.networkHandler.sendPacket(new ClientCommandC2SPacket(McUtils.player(), ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

                    pointArray[i] -= 5;
                    break;
                }

                clickSlotHelper(11 + i, screen, MinecraftClient.getInstance());
                //McUtils.containerMenu().onSlotClick(11 + i, 0, SlotActionType.PICKUP, McUtils.player());
                pointArray[i]--;
                break;
            }

            if(finishedSkillPoints == 5) {
                McUtils.sendMessageToClient(Text.of("Finished assigning skill points"));
                skillPointSet = null;
                loadingSkillpoints = false;
                return;
            }

            skillPointSet = new SavableSkillPointSet(pointArray);
        });
    }

    public static String extractAbilityNameFromHtml(String html) {
        if (html == null) return null;
        // 1) Entferne HTML-Tags, ersetze sie durch ein Leerzeichen damit Worte nicht zusammenlaufen
        String plain = html.replaceAll("<[^>]+>", " ");
        // 2) Entferne Minecraft-Farb-/Formatcodes (§x)
        plain = plain.replaceAll("§.", "");
        // 3) Unescape einiger häufiger Entities
        plain = plain.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&#39;", "'")
                .replace("&quot;", "\"");
        // 4) Normalisiere typographische Apostrophe auf ASCII-Apostroph
        plain = plain.replace('\u2019', '\'').replace('\u2018', '\'');
        // 5) Entferne Steuerzeichen, collapse multiple whitespaces, trim
        plain = plain.replaceAll("[\\p{C}]+", " ").replaceAll("\\s+", " ").trim();
        // 6) Falls der Name in Klammern oder mit vorangestellten/trailenden Satzzeichen bleibt, säubere Ränder
        plain = plain.replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("[\\p{Punct}\\s]+$", "");
        return plain.isEmpty() ? null : plain;
    }


    public static AbilityMapData.Node getNodeFromAbility(AbilityTreeData.Ability ability, AbilityMapData treeData) {
        int page = ability.page;

        for(AbilityMapData.Node node : treeData.pages.get(page)) {
            if(ability.coordinates.x != node.coordinates.x) {
                //System.out.println("skipped because x");
                continue;
            }
            if(ability.coordinates.y != node.coordinates.y % 6) {
                //System.out.println("skipped because y " + ability.name);
                continue;
            }
            return node;
        }

        return null;
    }


    public static AbilityTreeData.Ability getAbilityFromNode(AbilityMapData.Node node, AbilityTreeData treeData) {
        if(treeData == null) return null;
        Map<String, AbilityTreeData.Ability> page = treeData.pages.get(node.meta.page);
        for(AbilityTreeData.Ability ability : page.values()) {
            if(ability.coordinates.x != node.coordinates.x) {
                //System.out.println("skipped because x");
                continue;
            }
            if(ability.coordinates.y != node.coordinates.y % 6) {
                //System.out.println("skipped because y " + ability.name);
                continue;
            }
            return ability;
        }

        return null;
    }

    public static List<AbilityMapData.Node> convertNodeMapToList(AbilityMapData treeMap) {
        List<AbilityMapData.Node> result = new ArrayList<>();

        if(treeMap == null) return result;
        if(treeMap.pages == null) return result;

        for(List<AbilityMapData.Node> page : treeMap.pages.values()) {
            result.addAll(page);
        }

        return result;
    }

    public static List<AbilityTreeData.Ability> convertNodeTreeToList(AbilityTreeData treeData) {
        List<AbilityTreeData.Ability> result = new ArrayList<>();

        if(treeData == null) return result;
        if(treeData.pages == null) return result;

        for(Map<String, AbilityTreeData.Ability> page : treeData.pages.values()) {
            result.addAll(page.values());
        }

        return result;
    }

    // Hilfsfunktionen oben in deiner Klasse

    private static String normalizeArchetypeKey(String display) {
        if (display == null) return null;
        return (display + " Archetype").toLowerCase();
    }

    private static Optional<String> extractArchetypeInfo(List<String> description) {
        if (description == null) return Optional.empty();
        Pattern archetypeLine = Pattern.compile("(.+?)\\s+Archetype", Pattern.CASE_INSENSITIVE);
        for (String line : description) {
            if (line == null) continue;
            String plain = line.replaceAll("<[^>]+>", "").replaceAll("§.", "").trim();
            Matcher m = archetypeLine.matcher(plain);
            if (m.find()) {
                return Optional.of(m.group(1).trim()); // z.B. "Paladin" (ohne das Wort "Archetype")
            }
        }
        return Optional.empty();
    }

    public static Optional<Integer> extractCountFromComponentsString(String componentsToString) {
        if (componentsToString == null) return Optional.empty();
        String plain = componentsToString.replaceAll("§.", ""); // alle Farb-/Formatcodes entfernen
        Pattern p = Pattern.compile("\\b(\\d+)\\/(\\d+)\\b");
        Matcher m = p.matcher(plain);
        if (m.find()) {
            int max = Integer.parseInt(m.group(2));
            return Optional.of(max);
        }
        return Optional.empty();
    }

    public static Map<String, Integer> getArchetypeCounts(Map<String, AbilityTreeData.Archetype> archetypes) {
        Map<String, Integer> result = new HashMap<>();
        if (archetypes == null) return result;
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            String internalKey = e.getKey(); // z.B. your internal id like "monk" or similar
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;

            ItemStack archetypeItem;
            try {
                archetypeItem = McUtils.inventory().getStack(at.slot);
            } catch (Exception ex) {
                continue;
            }
            if (archetypeItem == null) continue;

            String displayName = null;
            try {
                Object cn = archetypeItem.getCustomName();
                if (cn != null) displayName = String.valueOf(cn).trim(); //TODO: maybe remove the trim
            } catch (Exception ignored) {}

            String lore = null;
            try {
                if (archetypeItem.getComponents() != null) lore = archetypeItem.getComponents().toString();
            } catch (Exception ignored) {}

            Optional<Integer> res = lore == null ? Optional.empty() : extractCountFromComponentsString(lore);
            if (res.isEmpty()) continue;

            int count = res.get();

            // mapKey: if the displayed name exists, prefer that, else fallback to internal archetype key
            String mapKey;
            if (displayName != null && !"null".equalsIgnoreCase(displayName) && !displayName.isEmpty()) {
                mapKey = normalizeArchetypeKey(displayName);
            } else {
                // fallback: use the provided map key (internal) but normalize to the same format
                // If your archetypes keys are already like "Paladin Archetype", adapt accordingly.
                mapKey = (internalKey == null ? "" : internalKey.toLowerCase());
            }

            if (!mapKey.isEmpty()) result.put(mapKey, count);
        }
        return result;
    }

    public static List<AbilityTreeData.Ability> calculateNodeOrder(
            Map<String, AbilityTreeData.Archetype> archetypes,
            List<AbilityMapData.Node> nodez,
            List<String> unlockedNodes,
            AbilityTreeData treeData) {

        List<AbilityTreeData.Ability> result = new ArrayList<>();
        if (nodez == null || nodez.isEmpty()) return result;

        List<AbilityTreeData.Ability> nodes = new ArrayList<>();
        for(AbilityMapData.Node node : nodez) {
            nodes.add(getAbilityFromNode(node, treeData));
        }

        // Map: lowercased name -> Ability
        Map<String, AbilityTreeData.Ability> byName = new HashMap<>();
        for (AbilityTreeData.Ability a : nodes) {
            if (a == null || a.name == null) continue;
            if(a.name.toLowerCase().contains("bak") || a.name.toLowerCase().contains("half")) {
                System.out.println("BAK OR HALF");
            }
            byName.put(a.name.toLowerCase(), a);
        }

        // Normalisiere initial unlocked (lowercase)
        Set<String> unlocked = new HashSet<>();
        if (unlockedNodes != null) {
            for (String s : unlockedNodes) if (s != null) unlocked.add(s.toLowerCase());
            // keep unlockedNodes normalized as well
            unlockedNodes.clear();
            unlockedNodes.addAll(unlocked);
        }

        // Status für DFS / Toposort
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();

        // Archetype-Counts (öffnet Knoten wenn genug Archetype-Punkte vorhanden)
        Map<String, Integer> archetypeCounts = getArchetypeCounts(archetypes);
        // Ensure archetypeCounts keys are normalized (already done in getArchetypeCounts)

        Deque<AbilityTreeData.Ability> stack = new ArrayDeque<>();

        class Resolver {
            // returns true if node is resolved/unlocked (either already unlocked or added to stack)
            boolean resolve(String name) {
                if (name == null || name.isEmpty()) return true;
                String key = name.toLowerCase();
                if (unlocked.contains(key)) return true;
                if (visited.contains(key)) return true;
                if (visiting.contains(key)) {
                    throw new IllegalStateException("Cycle detected in requirements at: " + key);
                }

                AbilityTreeData.Ability node = byName.get(key);
                if (node == null) {
                    // Unknown requirement: treat as already unlocked to allow progress
                    unlocked.add(key);
                    if (unlockedNodes != null && !unlockedNodes.contains(key)) unlockedNodes.add(key);
                    return true;
                }

                // Archetype requirement check
                AbilityTreeData.ArchetypeRequirement arReq = null;
                if (node.requirements != null) arReq = node.requirements.ARCHETYPE;
                if (arReq != null) {
                    String arcName = arReq.name == null ? null : arReq.name.trim();
                    String arcKey = arcName == null ? null : normalizeArchetypeKey(arcName);
                    int need = arReq.amount;
                    int have = arcKey == null ? 0 : archetypeCounts.getOrDefault(arcKey, 0);
                    if (have < need) {
                        return false; // hier wird bei denen dann returned
                    }
                }

                // Optional: check ABILITY_POINTS requirement here if needed
                // if (node.requirements != null && node.requirements.ABILITY_POINTS != null) { ... }

                visiting.add(key);

                // NODE requirement is a single node name
                if (node.requirements != null && node.requirements.NODE != null) {
                    String req = node.requirements.NODE.trim();
                    if (!req.isEmpty()) {
                        boolean ok = resolve(req);
                        if (!ok) {
                            visiting.remove(key);
                            return false;
                        }
                    }
                }

                visiting.remove(key);
                visited.add(key);

                if (!unlocked.contains(key)) {
                    stack.push(node);
                    unlocked.add(key);

                    // Archetype-Extraction: falls die Ability eine Archetype-Anzeigezeile hat, erhöhe den passenden Counter
                    try {
                        Optional<String> optDisplay = extractArchetypeInfo(node.description);
                        if (optDisplay.isPresent()) {
                            String display = optDisplay.get();
                            String internalArchetypeName = getInternalName(display, archetypes);
                            String mapKey = normalizeArchetypeKey(internalArchetypeName);
                            archetypeCounts.put(mapKey, archetypeCounts.getOrDefault(mapKey, 0) + 1);

                            //System.out.println(mapKey + " now has this many points: " + archetypeCounts.get(mapKey));
                        }
                    } catch (Exception ignored) {
                    }

                    if (unlockedNodes != null) {
                        if (!unlockedNodes.contains(key)) unlockedNodes.add(key);
                    }
                }
                return true;
            }
        }

        Resolver resolver = new Resolver();

        // Versuche alle Knoten zu lösen; Knoten, die wegen Archetype-Requirements nicht gelöst werden können, werden später erneut geprüft
        for (AbilityTreeData.Ability a : nodes) {
            if (a == null || a.name == null) continue;
            String key = a.name.toLowerCase();
            if (unlocked.contains(key) || visited.contains(key)) continue;
            resolver.resolve(key);
        }

        // Wiederholte Versuche falls Archetype-Abhängigkeiten später erfüllt werden
        boolean progress;
        do {
            progress = false;
            for (AbilityTreeData.Ability a : nodes) {
                if (a == null || a.name == null) continue;
                String key = a.name.toLowerCase();
                if (visited.contains(key)) continue;
                boolean resolved = resolver.resolve(key);
                if (resolved) progress = true;
            }
        } while (progress);

        // Stack in richtige Reihenfolge umwandeln (first resolved -> first in list)
        while (!stack.isEmpty()) result.add(stack.removeLast());

        return result;
    }

    public static String getInternalName(String displayName, Map<String, AbilityTreeData.Archetype> archetypes) {
        if (displayName == null || archetypes == null) return null;
        String target = normalizeDisplay(displayName);

        // 1) exact match against cleaned archetype.name field
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;
            String raw = at.name;
            String cand = normalizeDisplay(raw);
            if (cand.equals(target)) return e.getKey();
        }

        // 2) contains / word-match (handles cases where target is a substring)
        for (Map.Entry<String, AbilityTreeData.Archetype> e : archetypes.entrySet()) {
            AbilityTreeData.Archetype at = e.getValue();
            if (at == null) continue;
            String cand = normalizeDisplay(at.name);
            if (!cand.isEmpty() && (cand.contains(target) || target.contains(cand))) return e.getKey();
        }
        return null;
    }

    // Hilfsmethode: bereinigt HTML/Farbcodes, normalisiert Apostrophe/Hyphen und collapsed whitespace
    private static String normalizeDisplay(String s) {
        if (s == null) return "";
        // remove tags -> replace with space so words don't merge
        String plain = s.replaceAll("<[^>]+>", " ");
        // remove minecraft color codes like §a
        plain = plain.replaceAll("§.", "");
        // unescape common entities
        plain = plain.replace("&nbsp;", " ").replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
        // normalize typographic apostrophes to ASCII
        plain = plain.replace('\u2019', '\'').replace('\u2018', '\'');
        // remove control chars, collapse spaces, trim and lowercase
        plain = plain.replaceAll("[\\p{C}]+", " ").replaceAll("\\s+", " ").trim().toLowerCase();
        return plain;
    }



//    public static List<AbilityTreeData.Ability> calculateNodeOrder(List<AbilityTreeData.Ability> nodes, List<String> unlockedNodes) {
//        List<AbilityTreeData.Ability> result = new ArrayList<>();
//
//        if(nodes == null) return result;
//        if(nodes.isEmpty()) return result;
//
//        List<AbilityTreeData.Ability> copy = new ArrayList<>(nodes);
//        for(AbilityTreeData.Ability node : nodes) {
//            if(unlockedNodes.contains(node.name)) continue; //if its already added, e.g. when its required for another node earlier in the list
//            if(node.requirements != null && node.requirements.NODE != null) {
//                if(!unlockedNodes.contains(node.requirements.NODE.toLowerCase())) {
//                    List<AbilityTreeData.Ability> required = getRequiredNodes(node.requirements.NODE.toLowerCase(), copy, unlockedNodes);
//                    //for the line above it may be better to recursively call this function instead of another function but that would
//                    //require this function to be changed a little bit
//
//                    result.addAll(required);
//                    for(AbilityTreeData.Ability ability : required) {
//                        unlockedNodes.add(ability.name);
//                    }
//                    copy.removeAll(required);
//                } else {
//                    result.add(node);
//                    unlockedNodes.add(node.name);
//                    copy.remove(node);
//                }
//            } else {
//                result.add(node);
//                unlockedNodes.add(node.name.toLowerCase());
//                copy.remove(node);
//            }
//            //missing: check for archetype requirement, maybe add a queue that nodes get added to if the archetype requirement is not met
//            //and the total amount of nodes per archetype is tracked and if the needed amount is reached the element in the queue is unlocked
//            //maybe the queue can be sorted with the lowest amount needed being the first node
//        }
//
//        return result;
//    }

//    public static List<AbilityTreeData.Ability> getRequiredNodes(String nodeName, List<AbilityTreeData.Ability> nodes, List<String> unlockedNodes) {
//        List<AbilityTreeData.Ability> result = new ArrayList<>();
//
//        if(nodes == null) return result;
//        if(nodes.isEmpty()) return result;
//
//        for(AbilityTreeData.Ability node : nodes) {
//            if(!node.name.equals(nodeName)) continue;
//            if(node.requirements != null && node.requirements.NODE != null) {
//                if(unlockedNodes.contains(node.requirements.NODE.toLowerCase())) {
//
//                }
//            } else {
//                result.add(node);
//                unlockedNodes.add(node.name.toLowerCase());
//                break;
//            }
//        }
//    }



    public static void savePlayerAbilityTree(String playerName, String characterUUID, String className, SkillPoints skillPoints, AbilityMapData classMap, AbilityTreeData classTree, AbilityMapData playerTree) {
        try {
//            String playerApiUrl = "https://api.wynncraft.com/v3/player/" + playerName + "?fullResult";
////            String playerResponse = makeHttpRequest(playerApiUrl);
////            if (playerResponse == null) {
////                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to fetch player data")));
////                return characterClass;
////            }
////            JsonObject playerData = JsonParser.parseString(playerResponse).getAsJsonObject();
////            String characterUuid = null;
////            JsonElement activeCharacterElement = playerData.get("activeCharacter");
////            if (activeCharacterElement != null && !activeCharacterElement.isJsonNull()) {
////                characterUuid = activeCharacterElement.getAsString();
//                if(playerData.has("characters")) {
//                    JsonObject chars = playerData.getAsJsonObject("characters");
//                    if(chars.has(characterUuid)) {
//                        JsonObject charObj = chars.get(characterUuid).getAsJsonObject();
//                        characterClass = charObj.get("type").getAsString();
//                        System.out.println(characterClass);
//                    }
//
//
//                }
//            } else if (playerData.has("characters")) {
//                JsonArray chars = playerData.getAsJsonArray("characters");
//                for (JsonElement charElement : chars) {
//                    JsonObject charObj = charElement.getAsJsonObject();
//                    if (charObj.has("uuid")) {
//                        characterUuid = charObj.get("uuid").getAsString();
//                        break;
//                    }
//                }
//            }
            if (characterUUID == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Player is not online or has no character UUID.")));
                return;
            }
            String abilityApiUrl = "https://api.wynncraft.com/v3/player/" + playerName + "/characters/" + characterUUID + "/abilities";
            String abilityResponse = makeHttpRequest(abilityApiUrl);
            if (abilityResponse == null) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to fetch ability tree data")));
                return;
            }
            JsonArray abilityArr = JsonParser.parseString(abilityResponse).getAsJsonArray();
            List<String> ids = new ArrayList<>();
            for (JsonElement el : abilityArr) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("type") && obj.get("type").getAsString().equals("ability")) {
                    JsonObject meta = obj.getAsJsonObject("meta");
                    if (meta.has("id")) {
                        ids.add(meta.get("id").getAsString());
                    }
                }
            }
            JsonObject out = new JsonObject();
            out.addProperty("name", playerName + "_" + characterUUID);
            out.addProperty("visibleName", "");
            out.addProperty("strength", skillPoints.getStrength());
            out.addProperty("dexterity", skillPoints.getDexterity());
            out.addProperty("intelligence", skillPoints.getIntelligence());
            out.addProperty("defence", Math.max(skillPoints.getDefence(), skillPoints.getDefense()));
            out.addProperty("agility", skillPoints.getAgility());
            String formatted = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
            out.addProperty("className", formatted);
            out.add("playerMap", gson.toJsonTree(playerTree));
            out.add("playerTree", gson.toJsonTree(classTree));
            JsonArray inArr = new JsonArray();
            for (String id : ids) inArr.add(id);
            out.add("input", inArr);

            Path treesDir = FabricLoader.getInstance().getConfigDir().resolve("wynnextras/trees");
            Files.createDirectories(treesDir);
            String fileName = playerName + "_" + characterUUID + ".json";
            Path filePath = treesDir.resolve(fileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                String prettyJson = gson.toJson(out);
                writer.write(prettyJson);
                writer.flush();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Ability tree saved to: " + fileName)));
                TreeData.loadAll();
                return;
            } catch (IOException e) {
                System.err.println("[WynnExtras] Couldn't write ability tree file:");
                e.printStackTrace();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Failed to save ability tree file")));
            }
        } catch (Exception e) {
            System.err.println("[WynnExtras] Error fetching ability tree:");
            e.printStackTrace();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Error fetching ability tree")));
        }
    }

    private static String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "WynnExtras-Mod/1.0");
            connection.setRequestProperty("Authorization", "Bearer " + WynncraftApiHandler.INSTANCE.API_KEY);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else if (responseCode == 403) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Player has hidden their ability tree")));
                return null;
            } else if (responseCode == 401) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("Invalid API key")));
                return null;
            } else {
                System.err.println("[WynnExtras] HTTP Error: " + responseCode);
                return null;
            }
        } catch (IOException e) {
            System.err.println("[WynnExtras] Network error:");
            e.printStackTrace();
            return null;
        }
    }


    static public void openTreeMenu(MinecraftClient client, PlayerEntity player) {
        int currentSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = 7;
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        player.getInventory().selectedSlot = currentSlot;
        treeMenuWasOpened = true;
    }

    static public void openTreeResetMenu(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inTreeMenu) return;
        resetMenuWasOpened = clickOnNameInInventory("Reset", screen, client);
    }

    static public void clickOnSockets(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inResetMenu) return;
        boolean wasClicked = clickOnNameInInventory("Empty Socket", screen, client);
        if (wasClicked) clickedSockets++;
    }

    static public void confirmReset(MinecraftClient client, PlayerEntity player, HandledScreen<?> screen) {
        if (!inResetMenu) return;
        wasReset = clickOnNameInInventory("Confirm", screen, client);
    }

    static public void clickOnAbility(MinecraftClient client, PlayerEntity player, String nameToClick, HandledScreen<?> screen) {
        if (!inTreeMenu) return;
        clickOnNameInInventory(nameToClick, screen, client);
    }
    static public boolean hasUnlockPrefix(String ability, HandledScreen<?> screen) {
        String unlockName = "Unlock " + ability;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();
            if (name.contains(unlockName)) { // Substring match instead of exact match
                return true;
            }
        }
        return false;
    }


    static public int countOccurences(String count, HandledScreen<?> screen){
        int socketsFilled = 0;
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++){
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();
            if(name.contains(count)){
                socketsFilled++;
            }
        }
        return socketsFilled;
    }

    static public boolean clickOnNameInInventory(String nameToClick, HandledScreen<?> screen, MinecraftClient client) {
        for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack() || slot.getStack().getCustomName() == null) continue;
            String name = slot.getStack().getCustomName().getString();

            if (name.contains(nameToClick)) {
                // Print item components (simplified - no NBT)
                ItemStack stack = slot.getStack();
                System.out.println("Clicked item: " + stack.getName().getString());

                clickSlotHelper(i, screen, client);
                return true;
            }
        }
        return false;
    }

    static public void clickSlotHelper(int slotid, HandledScreen<?> screen, MinecraftClient client) {
        client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slotid,
                0,
                SlotActionType.PICKUP,
                client.player
        );
        ticksSinceLastAction = 0;
    }






    //Wynntils Skillpoint loader, slightly changed to work without having to save the points in their ui

    private static final int TOME_SLOT = 8;
    private static final int[] SKILL_POINT_TOTAL_SLOTS = {11, 12, 13, 14, 15};
    private static final int SKILL_POINT_TOME_SLOT = 4;
    private static final int CONTENT_BOOK_SLOT = 62;
    private static final int TOME_MENU_CONTENT_BOOK_SLOT = 89;

    private static Map<Skill, Integer> totalSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> gearSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> craftedSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> tomeSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> statusEffectSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> setBonusSkillPoints = new EnumMap<>(Skill.class);
    private static Map<Skill, Integer> assignedSkillPoints = new EnumMap<>(Skill.class);

    public static void loadSkillpoints(SavableSkillPointSet points) {
        ContainerUtils.closeBackgroundContainer();

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Loading Skill Point Loadout Query")
                .onError(msg -> WynntilsMod.warn("Failed to load skill point loadout: " + msg))
                .then(QueryStep.useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer((container) -> loadSkillPointsOnServer(container, points)))
                .build();
        query.executeQuery();
    }

    private static void loadSkillPointsOnServer(ContainerContent containerContent, SavableSkillPointSet points) {
        // we need to figure out which points we can subtract from first to actually allow assigning for positive points
        Map<Skill, Integer> negatives = new EnumMap<>(Skill.class);
        Map<Skill, Integer> positives = new EnumMap<>(Skill.class);
        for (int i = 0; i < Skill.values().length; i++) {
            int buildTarget = points.getSkillPointsAsArray()[i];
            int difference = buildTarget - getAssignedSkillPoints(Skill.values()[i]);

            // no difference automatically dropped here
            if (difference > 0) {
                positives.put(Skill.values()[i], difference);
            } else if (difference < 0) {
                negatives.put(Skill.values()[i], difference);
            }
        }

        boolean confirmationCompleted = false;
        for (Map.Entry<Skill, Integer> entry : negatives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // confirmation required, force loop to repeat this iteration
                    i--;
                    confirmationCompleted = true;
                }
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // needs to exist in both loops in case of 1s only
                    i--;
                    confirmationCompleted = true;
                }
            }
        }

        for (Map.Entry<Skill, Integer> entry : positives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
        }

        // Server needs 2 ticks, give a couple extra to be safe
        Managers.TickScheduler.scheduleLater(TreeLoader::populateSkillPoints, 4);
    }

    public static int getAssignedSkillPoints(Skill skill) {
        return assignedSkillPoints.getOrDefault(skill, 0);
    }

    public static void populateSkillPoints() {
        ContainerUtils.closeBackgroundContainer();

        Managers.TickScheduler.scheduleNextTick(() -> {
            assignedSkillPoints = new EnumMap<>(Skill.class);
            calculateGearSkillPoints();
            calculateStatusEffectSkillPoints();
            queryTotalAndTomeSkillPoints();
        });
    }

    private static boolean verifyChange(ContainerContent content, Int2ObjectFunction<ItemStack> changes, ContainerContentChangeType changeType, int contentBookSlot) {
        return changeType == ContainerContentChangeType.SET_CONTENT && changes.containsKey(contentBookSlot) && ((ItemStack)content.items().get(contentBookSlot)).getItem() == Items.POTION;
    }

    private static void calculateGearSkillPoints() {
        gearSkillPoints = new EnumMap<>(Skill.class);
        craftedSkillPoints = new EnumMap<>(Skill.class);
        setBonusSkillPoints = new EnumMap<>(Skill.class);

        for (ItemStack itemStack : Models.Inventory.getEquippedItems()) {
            calculateSingleGearSkillPoints(itemStack);
        }

        Models.Set.getUniqueSetNames().forEach(name -> {
            int trueCount = Models.Set.getTrueCount(name);
            Models.Set.getSetInfo(name).getBonusForItems(trueCount).forEach((bonus, value) -> {
                if (bonus instanceof SkillStatType skillStat) {
                    setBonusSkillPoints.merge(skillStat.getSkill(), value, Integer::sum);
                }
            });
        });
    }

    private static void calculateSingleGearSkillPoints(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isEmpty()) return; // Empty slot

        if (wynnItemOptional.get() instanceof GearItem gear) {
            gear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    gearSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });

        } else if (wynnItemOptional.get() instanceof CraftedGearItem craftedGear) {
            craftedGear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    craftedSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse gear: " + LoreUtils.getStringLore(itemStack));
        }
    }

    private static void calculateStatusEffectSkillPoints() {
        statusEffectSkillPoints = new EnumMap<>(Skill.class);
        Models.StatusEffect.getStatusEffects().forEach(statusEffect -> {
            for (Skill skill : Skill.values()) {
                if (statusEffect.getName().contains(skill.getDisplayName())) {
                    statusEffectSkillPoints.merge(
                            skill,
                            Integer.parseInt(statusEffect.getModifier().getStringWithoutFormatting()),
                            Integer::sum);
                }
            }
        });
    }

    private static void queryTotalAndTomeSkillPoints() {
        totalSkillPoints = new EnumMap<>(Skill.class);
        tomeSkillPoints = new EnumMap<>(Skill.class);

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Total and Tome Skill Point Query")
                .onError(msg -> WynntilsMod.warn("Failed to query skill points: " + msg))
                .then(QueryStep.useItemInHotbar(CharacterModel.CHARACTER_INFO_SLOT)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer(TreeLoader::processTotalSkillPoints))
                .conditionalThen(
                        TreeLoader::checkTomesUnlocked,
                        QueryStep.clickOnSlot(TOME_SLOT)
                                .expectContainerTitle(ContainerModel.MASTERY_TOMES_NAME)
                                .verifyContentChange((container, changes, changeType) ->
                                        verifyChange(container, changes, changeType, TOME_MENU_CONTENT_BOOK_SLOT))
                                .processIncomingContainer(TreeLoader::processTomeSkillPoints))
                .execute(TreeLoader::calculateAssignedSkillPoints)
                .build();

        query.executeQuery();
    }

    private static boolean checkTomesUnlocked(ContainerContent content) {
        return LoreUtils.getStringLore(content.items().get(TOME_SLOT)).contains("✔");
    }

    private static void processTotalSkillPoints(ContainerContent content) {
        for (Integer slot : SKILL_POINT_TOTAL_SLOTS) {
            Optional<WynnItem> wynnItemOptional =
                    Models.Item.getWynnItem(content.items().get(slot));
            if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof SkillPointItem skillPoint) {
                totalSkillPoints.merge(skillPoint.getSkill(), skillPoint.getSkillPoints(), Integer::sum);
            } else {
                WynntilsMod.warn("Skill Point Model failed to parse skill point item: "
                        + LoreUtils.getStringLore(content.items().get(slot)));
            }
        }
    }

    private static void processTomeSkillPoints(ContainerContent content) {
        ItemStack itemStack = content.items().get(SKILL_POINT_TOME_SLOT);
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof TomeItem tome) {
            tome.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    tomeSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else if (LoreUtils.getStringLore(itemStack).contains("§6Requirements:")) {
            // no-op, this is a tome that has not been unlocked or is not used by the player
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse tome: "
                    + LoreUtils.getStringLore(content.items().get(SKILL_POINT_TOME_SLOT)));
        }
    }

    private static void calculateAssignedSkillPoints() {
        for (Skill skill : Skill.values()) {
            assignedSkillPoints.put(
                    skill,
                    getTotalSkillPoints(skill)
                            - getGearSkillPoints(skill)
                            - getSetBonusSkillPoints(skill)
                            - getTomeSkillPoints(skill)
                            - getCraftedSkillPoints(skill)
                            - getStatusEffectSkillPoints(skill));
        }
    }

    public static int getTotalSkillPoints(Skill skill) {
        return totalSkillPoints.getOrDefault(skill, 0);
    }

    public static int getGearSkillPoints(Skill skill) {
        return gearSkillPoints.getOrDefault(skill, 0);
    }

    public static int getCraftedSkillPoints(Skill skill) {
        return craftedSkillPoints.getOrDefault(skill, 0);
    }

    public static int getTomeSkillPoints(Skill skill) {
        return tomeSkillPoints.getOrDefault(skill, 0);
    }

    public static int getStatusEffectSkillPoints(Skill skill) {
        return statusEffectSkillPoints.getOrDefault(skill, 0);
    }

    public static int getSetBonusSkillPoints(Skill skill) {
        return setBonusSkillPoints.getOrDefault(skill, 0);
    }
}
