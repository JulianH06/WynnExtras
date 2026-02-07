package julianh06.wynnextras.features.aspects;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.aspects.pages.AspectsPage;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.List;

@WEModule
public class maintracking {
    // Subcommand: /we aspects scan
    private static SubCommand scanSubCmd = new SubCommand(
            "scan",
            "Manually scan your aspects from the ability tree",
            (ctx) -> {
                AspectScanning.openMenu(MinecraftClient.getInstance(), MinecraftClient.getInstance().player);
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects lootpool
    private static SubCommand lootpoolSubCmd = new SubCommand(
            "lootpool",
            "Open the loot pool page",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.LootPools;
                });
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects gambit
    private static SubCommand gambitSubCmd = new SubCommand(
            "gambit",
            "Open the gambits page",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.LootPools;
                });
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects raidloot
    private static SubCommand raidlootSubCmd = new SubCommand(
            "raidloot",
            "Open the raid loot tracker page",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.RaidLoot;
                });
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects leaderboard
    private static SubCommand leaderboardSubCmd = new SubCommand(
            "leaderboard",
            "View the aspect leaderboard",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.Leaderboard;
                });
                return 1;
            },
            null,
            null
    );

    // Main command with player argument: /we aspects <player>
    private static Command aspectsCmdWithArg = new Command(
            "aspects",
            "Search for a player's aspects",
            (ctx) -> {
                String playerName = StringArgumentType.getString(ctx, "player");
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.Aspects;
                    AspectsPage.performPlayerSearch(playerName);
                });
                return 1;
            },
            List.of(scanSubCmd, lootpoolSubCmd, gambitSubCmd, raidlootSubCmd, leaderboardSubCmd),
            List.of(ClientCommandManager.argument("player", StringArgumentType.word()))
    );

    // Main command without arguments: /we aspects
    private static Command aspectsCmd = new Command(
            "aspects",
            "Aspect tracking and management",
            (ctx) -> {
                // Default: open aspects screen (My Aspects page)
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.Aspects;
                });
                return 1;
            },
            List.of(scanSubCmd, lootpoolSubCmd, gambitSubCmd, raidlootSubCmd, leaderboardSubCmd),
            null
    );

    // Command: /we gambits
    private static Command gambitsCmd = new Command(
            "gambits",
            "View today's gambits and countdown",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.Gambits;
                });
                return 1;
            },
            null,
            null
    );

    // Command: /we lootpool
    private static Command lootpoolCmd = new Command(
            "lootpool",
            "View raid loot pools",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.LootPools;
                });
                return 1;
            },
            null,
            null
    );

    private static Command lootrunlootpoolCmd = new Command(
            "lootruns",
            "View lootrun loot pools",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    WEScreen.open(AspectScreen::new);
                    AspectScreen.currentPage = AspectScreen.Page.Lootruns;
                });
                return 1;
            },
            null,
            null
    );

    //TODO: interfaces tracken und dann zeug aufrufen
    static boolean inTreeMenu = false;
    static boolean AspectScanreq = false;
    static boolean inAspectMenu = false;
    static boolean nextPage = false;
    static boolean inRaidChest = false;
    static boolean inPartyFinder = false;
    static boolean inPreviewChest = false;
    static int GuiSettleTicks = 0;
    public static ItemStack[] aspectsInChest = new ItemStack[5];
    public static Boolean scanDone = false;
    public static boolean returnedToFirstPage = false;
    public static int pagesToGoBack = 0;
    static boolean gambitDetected = false;
    static String lastPreviewChestTitle = "";
    static String lastLootrunPreviewTitle = "";
    static boolean needToClickAbilityTree = false;
    static boolean inCharacterMenu = false;
    static int characterMenuWaitTicks = 0;
    static boolean inLootrunPreviewChest = false;

    public static void init(){
        // Load saved loot pool data
        LootPoolData.INSTANCE.load();
        LootrunLootPoolData.INSTANCE.load();
        GambitData.INSTANCE.load();
        FavoriteAspectsData.INSTANCE.load();

        // Register the screen title debugger
        ScreenTitleDebugger.register();

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }
            Screen currScreen = client.currentScreen;
            HandledScreen<?> screen = null;
            if (currScreen == null) {
                scanDone = false;
                returnedToFirstPage = false;
                nextPage = false;
                aspectsInChest = new ItemStack[5];
                gambitDetected = false;
                lastPreviewChestTitle = "";
                lastLootrunPreviewTitle = "";
                pagesToGoBack = 0;
                AspectScanning.resetRewardAspects();
                // DON'T reset needToClickAbilityTree - it needs to persist across screen changes
                return;
            }

            if (currScreen instanceof HandledScreen) screen = (HandledScreen<?>) currScreen;
            else screen = null;

            String InventoryTitle = currScreen.getTitle().getString();

            inTreeMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE000");
            inAspectMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE002");
            inRaidChest = InventoryTitle.equals("\uDAFF\uDFEA\uE00E");
            inPartyFinder = InventoryTitle.equals("\uDAFF\uDFE1\uE00C");
            // Character menu (opened with right-click on slot 7 without sneaking)
            inCharacterMenu = InventoryTitle.equals("\uDAFF\uDFDC\uE003");

            // Preview chests have different titles for each raid
            inPreviewChest = InventoryTitle.equals("\uDAFF\uDFEA\uE00D\uDAFF\uDF6F\uF00B") || // NOTG
                    InventoryTitle.equals("\uDAFF\uDFEA\uE00D\uDAFF\uDF6F\uF00C") || // NOL
                    InventoryTitle.equals("\uDAFF\uDFEA\uE00D\uDAFF\uDF6F\uF00D") || // TCC
                    InventoryTitle.equals("\uDAFF\uDFEA\uE00D\uDAFF\uDF6F\uF00E");   // TNA

            inLootrunPreviewChest = LootrunLootPoolData.isLootrunChest(InventoryTitle);

            // Character menu: wait 5 ticks then click slot 9 (Ability Tree) to open the tree menu
            if(inCharacterMenu && needToClickAbilityTree){
                ScreenHandler menu = McUtils.containerMenu();
                if(menu == null) return;

                characterMenuWaitTicks++;
                if(characterMenuWaitTicks >= 5){
                    if(menu.slots.size() > 9) {
                        Slot slot = menu.getSlot(9);
                        if(slot != null && slot.getStack() != null && slot.getStack().getName() != null) {
                            String name = slot.getStack().getName().getString();
                            if(name.contains("Ability Tree")) {
                                ContainerUtils.clickOnSlot(9, menu.syncId, 0, menu.getStacks());
                                needToClickAbilityTree = false;
                                characterMenuWaitTicks = 0;
                            }
                        }
                    }
                }
                return;
            }

            if(inTreeMenu && AspectScanreq){
                needToClickAbilityTree = false; // Reset flag since we're now in the tree menu
                TreeLoader.clickOnNameInInventory("Aspects", screen, MinecraftClient.getInstance());
                AspectScanning.setSearchedPages(0);
                GuiSettleTicks = 0; // Reset settle ticks for fresh start
                return;
            }
            if(inAspectMenu && AspectScanreq){
                // Add delay when first entering aspect menu to ensure everything loads
                if(GuiSettleTicks > 5){
                    GuiSettleTicks = 0;
                    AspectScanning.AspectsInMenu();
                } else {
                    GuiSettleTicks++;
                }
                return;
            }
            if(inAspectMenu && nextPage){
                // Use longer delay for first 3 pages to ensure all aspects are loaded
                int requiredTicks = (AspectScanning.getSearchedPages() <= 3) ? 6 : 4;
                if(GuiSettleTicks > requiredTicks){
                    nextPage=false;
                    GuiSettleTicks=0;
                    AspectScanning.AspectsInMenu();
                    return;
                }
                else{
                    GuiSettleTicks++;
                    return;
                }
            }

            if(inPartyFinder && !gambitDetected){
                gambitDetected = true;
                AspectScanning.detectGambit(screen);
                return;
            }

            // Preview chest: scan when title changes (allows switching raids inside the chest)
            if(inPreviewChest){
                String currentTitle = currScreen.getTitle().getString();
                if(!currentTitle.equals(lastPreviewChestTitle)){
                    System.out.println("[WynnExtras] Preview chest detected, title: " + currentTitle);
                    lastPreviewChestTitle = currentTitle;
                    AspectScanning.scanPreviewChest(screen, currentTitle);
                }
                return;
            }

            if(inLootrunPreviewChest){
                String currentTitle = currScreen.getTitle().getString();
                LootrunScanning.handleLootrunPreviewChest(screen, currentTitle);
                return;
            }

            // Reward chest: scan aspects from slots 11-15 and upload
            if(inRaidChest && !(scanDone && returnedToFirstPage)){
                try {
                    AspectScanning.AspectsInRaidChest();
                } catch (Exception e) {
                    System.err.println("[WynnExtras] Error scanning raid chest: " + e.getMessage());
                    e.printStackTrace();
                }
                return;
            }
        });
    }

    public static void setAspectScanreq(boolean value){
        AspectScanreq = value;
    }
    public static void setNextPage(boolean nextPage) {
        maintracking.nextPage = nextPage;
    }
    public static void setNeedToClickAbilityTree(boolean value) {
        needToClickAbilityTree = value;
    }
}
