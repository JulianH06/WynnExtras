package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.core.command.SubCommand;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.utils.MinecraftUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WEModule
public class maintracking {

    // Subcommand: /we aspects scan
    private static SubCommand scanSubCmd = new SubCommand(
            "scan",
            "Manually scan your aspects from the ability tree",
            (ctx) -> {
                if (WynncraftApiHandler.INSTANCE.API_KEY == null) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§bYou need to set your api-key to use this feature. Run \"/we apikey\" for more information."));
                    return 0;
                }
                aspect.openMenu(MinecraftClient.getInstance(), MinecraftClient.getInstance().player);
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects debug
    private static SubCommand debugSubCmd = new SubCommand(
            "debug",
            "Toggle screen title debugging (for finding menu identifiers)",
            (ctx) -> {
                ScreenTitleDebugger.toggleDebug();
                return 1;
            },
            null,
            null
    );

    // Subcommand: /we aspects raiddebug
    private static SubCommand raidDebugSubCmd = new SubCommand(
            "raiddebug",
            "Toggle raid slot debugging (shows which slots are clicked)",
            (ctx) -> {
                AspectScreenSimple.toggleDebug();
                return 1;
            },
            null,
            null
    );

    // Main command: /we aspects
    private static Command aspectsCmd = new Command(
            "aspects",
            "Aspect tracking and management",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    AspectScreenSimple.open();
                });
                return 1;
            },
            List.of(scanSubCmd, debugSubCmd, raidDebugSubCmd),
            null
    );

    // Command: /we gambits
    private static Command gambitsCmd = new Command(
            "gambits",
            "View today's gambits and countdown",
            (ctx) -> {
                MinecraftUtils.mc().send(() -> {
                    AspectScreenSimple.openGambits();
                });
                return 1;
            },
            null,
            null
    );

    // Legacy command for backwards compatibility
    private static Command Scanaspects = new Command(
            "ScanAspects",
            "Command to manually scan your Aspects (legacy, use /we aspects scan)",
            (ctx)->{
                if(WynncraftApiHandler.INSTANCE.API_KEY == null) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§bYou need to set your api-key to use this feature.Run \"/we apikey\" for more information."));
                    return 0;
                }
                aspect.openMenu(MinecraftClient.getInstance(),MinecraftClient.getInstance().player);
                return 0;
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
    static boolean Raiddone = true;
    static boolean PrevPageRaid = false;
    static int GuiSettleTicks = 0;
    static int counter = 0;
    static boolean NextPageRaid = false;
    public static ItemStack[] aspectsInChest = new ItemStack[5];
    public static Boolean scanDone = false;
    public static Boolean goingBack = false;
    static boolean gambitDetected = false;
    static String lastPreviewChestTitle = "";
    static boolean needToClickAbilityTree = false;
    static boolean inCharacterMenu = false;

    public static void init(){
        // Load saved loot pool data
        LootPoolData.INSTANCE.load();
        GambitData.INSTANCE.load();
        FavoriteAspectsData.INSTANCE.load();

        // Register the screen title debugger
        ScreenTitleDebugger.register();

        // Initialize gambit reset notifications
        GambitNotifier.init();

        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }
            Screen currScreen = client.currentScreen;
            HandledScreen<?> screen = null;
            if (currScreen == null) {
                scanDone = false;
                goingBack = false;
                nextPage = false;
                aspectsInChest = new ItemStack[5];
                gambitDetected = false;
                lastPreviewChestTitle = "";
                needToClickAbilityTree = false;
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

            // Character menu: click on "Ability Tree" to get to the tree menu
            if(inCharacterMenu && needToClickAbilityTree){
                needToClickAbilityTree = false;
                // Debug: print all slot names to find Ability Tree
                if (screen != null) {
                    for (int i = 0; i < screen.getScreenHandler().slots.size(); i++) {
                        var slot = screen.getScreenHandler().slots.get(i);
                        if (slot.hasStack()) {
                            String name = slot.getStack().getName().getString().replaceAll("§.", "");
                            if (!name.isEmpty() && !name.equals("Air")) {
                                System.out.println("[WynnExtras] Slot " + i + ": " + name);
                            }
                        }
                    }
                }
                TreeLoader.clickOnNameInInventory("Ability Tree", screen, MinecraftClient.getInstance());
                return;
            }

            if(inTreeMenu && AspectScanreq){
                TreeLoader.clickOnNameInInventory("Aspects", screen, MinecraftClient.getInstance());
                aspect.setSearchedPages(0);
                return;
            }
            if(inAspectMenu && AspectScanreq){
                aspect.AspectsInMenu();
                return;
            }
            if(inAspectMenu && nextPage){
                if(GuiSettleTicks>7){
                    nextPage=false;
                    GuiSettleTicks=0;
                    aspect.AspectsInMenu();
                    return;
                }
                else{
                    GuiSettleTicks++;
                    return;
                }
            }

            if(inPartyFinder && !gambitDetected){
                gambitDetected = true;
                aspect.detectGambit(screen);
                return;
            }

            // Preview chest: scan when title changes (allows switching raids inside the chest)
            if(inPreviewChest){
                String currentTitle = currScreen.getTitle().getString();
                if(!currentTitle.equals(lastPreviewChestTitle)){
                    System.out.println("[WynnExtras] Preview chest detected, title: " + currentTitle);
                    lastPreviewChestTitle = currentTitle;
                    aspect.scanPreviewChest(screen, currentTitle);
                }
                return;
            }

//            if(inRaidChest && !scanDone){
//                try {
//                    aspect.AspectsInRaidChest();
//                } catch (Exception e) {
//                    McUtils.sendMessageToClient(Text.of("crash"));
//                }
//                return;
//            }

//            if(inRaidChest && Raiddone){
//                Raiddone = false;
//                aspect.AspectsInRaidChest();
//                return;
//            }
//            if(inRaidChest && NextPageRaid){
//                if(GuiSettleTicks>7){
//                    NextPageRaid=false;
//                    GuiSettleTicks=0;
//                    aspect.AspectsInRaidChest();
//                    return;
//                } else{
//                    GuiSettleTicks++;
//                    return;
//                }
//            }
//            if(inRaidChest && PrevPageRaid){
//                if(GuiSettleTicks>7){
//                    if(counter == 4) {
//                        PrevPageRaid = false;
//                        GuiSettleTicks = 0;
//                        counter = 0;
//                    }
//                    aspect.PrevPageRaid();
//                    counter++;
//                }
//                else{
//                    GuiSettleTicks++;
//                }
//            }
        });
    }
    public static boolean getinTreeMenu(){
        return inTreeMenu;
    }
    public static void setAspectScanreq(boolean value){
        AspectScanreq = value;
    }
    public static void setNextPage(boolean nextPage) {
        maintracking.nextPage = nextPage;
    }
    public static void setRaiddone(boolean value){
        Raiddone = value;
    }
    public static void setNextPageRaid(boolean nextPage) {
        maintracking.NextPageRaid = nextPage;
    }
    public static void setPrevPageRaid(boolean nextPage) {
        maintracking.PrevPageRaid = nextPage;
    }
    public static void setNeedToClickAbilityTree(boolean value) {
        needToClickAbilityTree = value;
    }
}
