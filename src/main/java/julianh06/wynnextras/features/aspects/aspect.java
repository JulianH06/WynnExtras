package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;


import java.util.HashMap;
import java.util.List;


public class aspect {
    //TODO: aus tree stuff zeug klauen, auf aspects page gehen, dann ausgeben aspect x:100
    static Screen currScreen = MinecraftClient.getInstance().currentScreen;
    static HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
    static int SearchedPages = 0;
    public static final Map<String, Pair<String, String>> allAspects = new HashMap<>();

    public static void openMenu(MinecraftClient client, PlayerEntity player){
        int currentSlot = player.getInventory().selectedSlot;
        player.getInventory().selectedSlot = 7;
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        player.getInventory().selectedSlot = currentSlot;
//        clickOnNameInInventory("Aspects", screen, client);
        maintracking.setAspectScanreq(true);
    }
    public static Map<String, Pair<String, String>> AspectsInMenu() {
        currScreen = MinecraftClient.getInstance().currentScreen;
        screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        Map<String, Pair<String, String>> result = new HashMap<>();
        if (screen == null) {
            return result;
        } else {
            int[] slotsToRead = {36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53}; // fill slot indices

            int readCount = 0;
            for (int idx = 0; idx < slotsToRead.length; idx++) {
                int i = slotsToRead[idx];
                Slot slot = screen.getScreenHandler().slots.get(i);

                // Stop early if we encounter empty slots before batch is full
                if (!slot.hasStack()) {
                    // If this happens before readCount == 18, stop the batch immediately
                    dummyfunction();
                    break; // Exit loop early, immediately go to next page
                }

                List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
                String name = null;
                String tierLine = null;
                String rarity = "";

                for (Text tooltip : tooltips) {
                    String s = tooltip.getString().replaceAll("§.", "").trim();
                    if (name == null && (s.contains("Aspect") || s.contains("Embodiment"))) {
                        name = s;

                        String hexCode = slot.getStack().getCustomName().getStyle().getColor().getHexCode();
                        if(hexCode.equals("#AA00AA")) {
                            rarity = "Mythic";
                        } else if(hexCode.equals("#FF5555")) {
                            rarity = "Fabled";
                        }else if(hexCode.equals("#55FFFF")) {
                            rarity = "Legendary";
                        }
                    }
                }

                String bestTierLine = null;
                for (Text tooltip : tooltips) {
                    String candidate = extractBestTierLine(tooltip).trim();
                    if (candidate.contains("Tier") &&
                            (candidate.contains(">>>") || candidate.matches(".*\\[\\d+/\\d+\\].*") || candidate.contains("[MAX]"))) {
                        if (bestTierLine == null || candidate.length() > bestTierLine.length()) {
                            bestTierLine = candidate;
                        }
                    }
                }

                if (name != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                    bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                    bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                    result.put(name, new Pair<>(bestTierLine, rarity));
                }

                readCount++;
                if (readCount == 18) {
                    dummyfunction();
                    break; // Done with this page, no need to check further
                }

            }
            if (readCount < 18) {
                int[] extraSlots = {4, 11, 15, 18, 26};
                for (int extra : extraSlots) {
                    // Make sure you don't scan an out-of-bounds slot!
                    if (extra < screen.getScreenHandler().slots.size()) {
                        Slot slot = screen.getScreenHandler().slots.get(extra);
                        if (!slot.hasStack()) continue;
                        List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
                        String name = null, tierLine = null;
                        String rarity = "";
                        for (Text tooltip : tooltips) {
                            String s = tooltip.getString().replaceAll("§.", "").trim();
                            if (name == null && (s.contains("Aspect") || s.contains("Embodiment"))) name = s;

                            String hexCode = slot.getStack().getCustomName().getStyle().getColor().getHexCode();
                            if(hexCode.equals("#AA00AA")) {
                                rarity = "Mythic";
                            } else if(hexCode.equals("#FF5555")) {
                                rarity = "Fabled";
                            }else if(hexCode.equals("#55FFFF")) {
                                rarity = "Legendary";
                            }
                        }
                        String bestTierLine = null;
                        for (Text tooltip : tooltips) {
                            String candidate = extractBestTierLine(tooltip).trim();
                            if (candidate.contains("Tier") &&
                                    (candidate.contains(">>>") || candidate.matches(".*\\[\\d+/\\d+\\].*") || candidate.contains("[MAX]"))) {
                                if (bestTierLine == null || candidate.length() > bestTierLine.length())
                                    bestTierLine = candidate;
                            }
                        }
                        if (name != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                            bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                            bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                            result.put(name, new Pair<>(bestTierLine, rarity));
                        }
                    }
                }
            }
            for (Map.Entry<String, Pair<String, String>> entry : result.entrySet()) {
                allAspects.put(entry.getKey(), entry.getValue());
            }

            maintracking.setAspectScanreq(false);
            if (readCount < 18 && McUtils.mc().currentScreen != null) {
                McUtils.mc().currentScreen.close();
            }
            return result;
        }
    }

    public static void AspectsInRaidChest() {
        currScreen = MinecraftClient.getInstance().currentScreen;
        screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        if (screen == null) return;

        if(maintracking.goingBack) {
            if(!screen.getScreenHandler().slots.get(16).getStack().isEmpty()) {
                PrevPageRaid();
            }
        }

        int[] slotsToRead = {11,12,13,14,15};
        List<String> foundNames = new ArrayList<>();

        boolean samePage = true;
        for(int i = 11; i < 16; i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);

            if (slot.getStack().isEmpty()) {
                if (!maintracking.aspectsInChest[i].isEmpty()) {
                    samePage = false;
                    break;
                }
            } else {
                if (!slot.getStack().getCustomName().equals(maintracking.aspectsInChest[i].getCustomName())) {
                    samePage = false;
                    break;
                }
            }
        }

        if(samePage) {
            return;
        }

        maintracking.aspectsInChest = new ItemStack[5];

        if(maintracking.goingBack) {
            if(screen.getScreenHandler().slots.get(10).getStack().isEmpty()) {
                maintracking.scanDone = true;
                maintracking.goingBack = false;
            }
        }

        for(int i = 11; i < 16; i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);

            maintracking.aspectsInChest[i] = slot.getStack().copy();

            // Stop if empty
            if (!slot.hasStack()) break;

            List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
            String name = null;
            for (Text tooltip : tooltips) {
                String s = tooltip.getString().replaceAll("§.", "").trim();
                if (s.contains("Aspect") || s.contains("Embodiment")) {
                    name = s;
                    break;
                }
            }
            // Stop at the first non-aspect, but keep any already found
            if (name == null) break;
            foundNames.add(name);
        }

        // Print all aspects found this page, in order, with duplicates if there are any
        if (MinecraftClient.getInstance().player != null) {
            for (String aspectName : foundNames) {
                McUtils.sendMessageToClient(Text.of("Found aspect: " + aspectName));
            }
        }

        // If all 5 slots contained aspects, go to next page
        if (!screen.getScreenHandler().slots.get(16).getStack().isEmpty()) {
            NextPageRaid();
        } else {
            maintracking.goingBack = true;
        }
    }





    private static String extractBestTierLine(Text t) {
        String self = t.getString().replaceAll("§.", "").trim();
        boolean relevant = !self.isEmpty() && (self.contains("Tier") || self.contains(">>>") ||
                self.matches(".*\\[\\d+/\\d+\\].*") || self.contains("[MAX]"));
        StringBuilder out = new StringBuilder();
        if (relevant) out.append(self);

        StringBuilder deepest = new StringBuilder();
        if (t.getSiblings() != null) {
            for (Text sib : t.getSiblings()) {
                String sub = extractBestTierLine(sib).trim();
                if (sub.length() > deepest.length()) {
                    deepest.setLength(0); deepest.append(sub);
                }
            }
        }
        return deepest.length() > out.length() ? deepest.toString() : out.toString();
    }

    private static void dummyfunction() {
        //System.out.println("dummyfunction called after 18 slots read");
        SearchedPages++;
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        if(SearchedPages<6){
            maintracking.setNextPage(true);
        }
        else{
            System.out.println(allAspects);
            WynncraftApiHandler.processAspects(allAspects);
            resetAllAspects();
            SearchedPages = 0; //TODO
        }
    }
    private static void NextPageRaid() {
        System.out.println("all slots read");
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        //das hat nichts mit dem treeloader direkt zu tun, ist nur ne util funktion
    }
    public static void PrevPageRaid() {
        System.out.println("go back");
        TreeLoader.clickOnNameInInventory("Previous Page",screen,MinecraftClient.getInstance());
    }
    public static void setSearchedPages(int searchedPages) {
        SearchedPages = searchedPages;
    }
    public static void resetAllAspects() {
        allAspects.clear();
    }
}

