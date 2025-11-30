package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
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
    private static Command Scanaspects = new Command(
            "ScanAspects",
            "Command to manually scan your Aspects",
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
    static boolean Raiddone = true;
    static boolean PrevPageRaid = false;
    static int GuiSettleTicks = 0;
    static int counter = 0;
    static boolean NextPageRaid = false;
    static Screen currScreen = MinecraftClient.getInstance().currentScreen;
    static HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
    public static ItemStack[] aspectsInChest = new ItemStack[5];
    public static Boolean scanDone = false;
    public static Boolean goingBack = false;

    public static void init(){
        ClientTickEvents.END_CLIENT_TICK.register((tick) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }
            Screen currScreen = client.currentScreen;
            if (currScreen == null) {
                scanDone = false;
                goingBack = false;
                nextPage = false;
                aspectsInChest = new ItemStack[5];
                return;
            }

            if (currScreen instanceof HandledScreen) screen = (HandledScreen<?>) currScreen;
            else screen = null;

            String InventoryTitle = currScreen.getTitle().getString();
            inTreeMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE000");
            inAspectMenu = InventoryTitle.equals("\uDAFF\uDFEA\uE002");
            inRaidChest = InventoryTitle.equals("\uDAFF\uDFEA\uE00E");

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
}
