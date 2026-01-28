package julianh06.wynnextras.features.aspects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
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
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.HashMap;
import java.util.List;


public class aspect {
    //TODO: aus tree stuff zeug klauen, auf aspects page gehen, dann ausgeben aspect x:100
    static int SearchedPages = 0;
    public static final Map<String, Pair<String, String>> allAspects = new HashMap<>();

    // Upload throttling for preview chests (once per minute per raid, unless it's reset time)
    private static final Map<String, Long> lastUploadTime = new HashMap<>();
    private static final long UPLOAD_COOLDOWN_MS = 60000; // 60 seconds

    // Reward chest aspect collection
    private static final List<String> collectedRewardAspects = new ArrayList<>();

    // Reward chest coordinates for raid detection
    private static final Map<String, double[]> REWARD_CHEST_COORDS = Map.of(
            "NOTG", new double[]{10342, 41, 3111},
            "NOL",  new double[]{11005, 58, 2909},
            "TCC",  new double[]{10817, 45, 3901},
            "TNA",  new double[]{24489, 8, -23878}
    );

    public static void openMenu(MinecraftClient client, PlayerEntity player){
        int currentSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(7);

        // Just click normally - this opens the character menu
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);

        player.getInventory().setSelectedSlot(currentSlot);

        // Set flag to click on "Ability Tree" when menu opens
        maintracking.setNeedToClickAbilityTree(true);
        maintracking.setAspectScanreq(true);
    }
    public static Map<String, Pair<String, String>> AspectsInMenu() {
        Screen currScreen = MinecraftClient.getInstance().currentScreen;
        HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        Map<String, Pair<String, String>> result = new HashMap<>();
        if (screen == null) {
            return result;
        } else {
            // On the first page (SearchedPages == 0), scan the 5 active aspects in center slots FIRST
            if (SearchedPages == 0) {
                int[] centerSlots = {4, 11, 15, 18, 26};
                for (int slotIdx : centerSlots) {
                    if (slotIdx >= screen.getScreenHandler().slots.size()) continue;
                    Slot slot = screen.getScreenHandler().slots.get(slotIdx);
                    if (!slot.hasStack()) continue;

                    List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
                    String name = null;
                    String rarity = "";

                    for (Text tooltip : tooltips) {
                        String s = tooltip.getString().replaceAll("§.", "").trim();
                        if (name == null && (s.contains("Aspect") || s.contains("Embodiment"))) {
                            name = s;

                            // Get rarity from color
                            if (slot.getStack().getCustomName() != null &&
                                slot.getStack().getCustomName().getStyle() != null &&
                                slot.getStack().getCustomName().getStyle().getColor() != null) {
                                String hexCode = slot.getStack().getCustomName().getStyle().getColor().getHexCode();
                                if(hexCode.equals("#AA00AA")) rarity = "Mythic";
                                else if(hexCode.equals("#FF5555")) rarity = "Fabled";
                                else if(hexCode.equals("#55FFFF")) rarity = "Legendary";
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
                }

                // Add active aspects to global map immediately
                for (Map.Entry<String, Pair<String, String>> entry : result.entrySet()) {
                    allAspects.put(entry.getKey(), entry.getValue());
                }

                // Clear result map so we don't add these aspects twice
                result.clear();

                // Now continue to scan the regular slots on this page (don't return early)
            }

            int[] slotsToRead = {36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53}; // fill slot indices

            int readCount = 0;
            for (int idx = 0; idx < slotsToRead.length; idx++) {
                int i = slotsToRead[idx];
                Slot slot = screen.getScreenHandler().slots.get(i);

                // Stop early if we encounter empty slots before batch is full
                if (!slot.hasStack()) {
                    // If this happens before readCount == 18, stop the batch immediately
                    dummyfunction(screen);
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

                        // Safe null checking for rarity detection
                        if (slot.getStack().getCustomName() != null &&
                            slot.getStack().getCustomName().getStyle() != null &&
                            slot.getStack().getCustomName().getStyle().getColor() != null) {
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
                    // Add current page aspects to global map BEFORE calling dummyfunction
                    // This ensures page 6 aspects are included in the upload
                    for (Map.Entry<String, Pair<String, String>> entry : result.entrySet()) {
                        allAspects.put(entry.getKey(), entry.getValue());
                    }
                    dummyfunction(screen);
                    break; // Done with this page, no need to check further
                }

            }

            // Add remaining aspects to global map (for pages with < 18 aspects)
            for (Map.Entry<String, Pair<String, String>> entry : result.entrySet()) {
                allAspects.put(entry.getKey(), entry.getValue());
            }

            maintracking.setAspectScanreq(false);

            // Don't close screen early - let the normal pagination complete
            // Only close if we've searched all pages (0-6 = 7 pages total)
            if (SearchedPages > 6 && McUtils.mc().currentScreen != null) {
                McUtils.mc().currentScreen.close();
            }
            return result;
        }
    }

    public static void AspectsInRaidChest() {
        Screen currScreen = MinecraftClient.getInstance().currentScreen;
        HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        if (screen == null) return;

        int[] slotsToRead = {11,12,13,14,15};
        List<String> foundNames = new ArrayList<>();

        boolean samePage = true;
        for(int i = 11; i < 16; i++) {
            int arrayIndex = i - 11; // Map slot 11-15 to array index 0-4
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack cachedStack = maintracking.aspectsInChest[arrayIndex];

            // If cached is null, definitely not same page
            if (cachedStack == null) {
                samePage = false;
                break;
            }

            if (slot.getStack().isEmpty()) {
                if (!cachedStack.isEmpty()) {
                    samePage = false;
                    break;
                }
            } else {
                // Safe null check for customName comparison
                Text currentName = slot.getStack().getCustomName();
                Text cachedName = cachedStack.getCustomName();
                if (currentName == null || cachedName == null || !currentName.equals(cachedName)) {
                    samePage = false;
                    break;
                }
            }
        }

        if(samePage) {
            return;
        }

        maintracking.aspectsInChest = new ItemStack[5];

        for(int i = 11; i < 16; i++) {
            int arrayIndex = i - 11; // Map slot 11-15 to array index 0-4
            Slot slot = screen.getScreenHandler().slots.get(i);

            maintracking.aspectsInChest[arrayIndex] = slot.getStack().copy();

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

        // Collect all aspects found this page
        collectedRewardAspects.addAll(foundNames);

        // Print found aspects to chat
        if (MinecraftClient.getInstance().player != null && !foundNames.isEmpty()) {
            for (String aspectName : foundNames) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Found aspect: §e" + aspectName));
            }
        }

        // Check if there's a next page (slot 16)
        if (!screen.getScreenHandler().slots.get(16).getStack().isEmpty()) {
            System.out.println("[WynnExtras] Next page available, total collected so far: " + collectedRewardAspects.size());
            NextPageRaid(screen);
        } else {
            // Last page - upload now
            System.out.println("[WynnExtras] Last page reached, total aspects found: " + collectedRewardAspects.size());
            maintracking.scanDone = true;

            if (!collectedRewardAspects.isEmpty()) {
                uploadCollectedAspects();
            }
        }
    }

    private static void uploadCollectedAspects() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Detect which raid based on player position
        String currentRaid = detectRaidFromPosition();

        if (currentRaid.equals("UNKNOWN")) {
            System.err.println("[WynnExtras] Cannot determine raid type for aspect upload");
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cCannot detect raid type - too far from reward chest"));
            collectedRewardAspects.clear();
            return;
        }

        System.out.println("[WynnExtras] Uploading " + collectedRewardAspects.size() + " aspects from " + currentRaid);
        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Uploading §e" + collectedRewardAspects.size() + " §7aspect(s) from §6" + currentRaid + "§7..."));

        // Upload to API
        WynncraftApiHandler.uploadRewardedAspects(currentRaid, new ArrayList<>(collectedRewardAspects));

        // Clear the collection for next raid
        collectedRewardAspects.clear();
    }

    private static String detectRaidFromPosition() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "UNKNOWN";

        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        String closest = "UNKNOWN";
        double minDist = Double.MAX_VALUE;

        for (Map.Entry<String, double[]> entry : REWARD_CHEST_COORDS.entrySet()) {
            double[] pos = entry.getValue();
            double dist = Math.sqrt(Math.pow(px - pos[0], 2) + Math.pow(py - pos[1], 2) + Math.pow(pz - pos[2], 2));
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getKey();
            }
        }

        // Sanity check: if player is too far from any known chest, return UNKNOWN
        return minDist < 100 ? closest : "UNKNOWN";
    }

    /**
     * Reset collected reward aspects (called when screen closes)
     */
    public static void resetRewardAspects() {
        if (!collectedRewardAspects.isEmpty()) {
            System.out.println("[WynnExtras] Clearing " + collectedRewardAspects.size() + " collected reward aspects");
            collectedRewardAspects.clear();
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

    private static void dummyfunction(HandledScreen<?> screen) {
        //System.out.println("dummyfunction called after 18 slots read");
        SearchedPages++;
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        if(SearchedPages<=6){
            maintracking.setNextPage(true);
            // Show progress message matching UI page numbers (don't show for page 0 which is active aspects)
            if(SearchedPages > 0) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Scanning page " + (SearchedPages + 1) + "/7..."));
            }
        }
        else{
            System.out.println(allAspects);
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aScanned " + allAspects.size() + " aspects! §7Uploading..."));
            // Create a copy of the map to avoid it being cleared before async upload completes
            Map<String, Pair<String, String>> aspectsCopy = new HashMap<>(allAspects);
            WynncraftApiHandler.processAspects(aspectsCopy);
            resetAllAspects();
            SearchedPages = 0; //TODO
        }
    }
    private static void NextPageRaid(HandledScreen<?> screen) {
        System.out.println("[WynnExtras] Clicking next page in reward chest");
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        maintracking.NextPageRaid = true;
        maintracking.GuiSettleTicks = 0;
    }
    public static void PrevPageRaid(HandledScreen<?> screen) {
        System.out.println("[WynnExtras] Clicking previous page in reward chest");
        TreeLoader.clickOnNameInInventory("Previous Page",screen,MinecraftClient.getInstance());
        maintracking.PrevPageRaid = true;
        maintracking.GuiSettleTicks = 0;
    }
    public static void setSearchedPages(int searchedPages) {
        SearchedPages = searchedPages;
    }
    public static int getSearchedPages() {
        return SearchedPages;
    }
    public static void resetAllAspects() {
        allAspects.clear();
    }

    /**
     * Check if it's currently loot pool reset time (Friday 19:00 CET, ±30 min window)
     * During reset time, upload throttling is disabled
     */
    private static boolean isResetTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));

        // Only on Friday
        if (now.getDayOfWeek() != DayOfWeek.FRIDAY) {
            return false;
        }

        // Check if within 30 minutes of 19:00 (18:30 - 19:30)
        int hour = now.getHour();
        int minute = now.getMinute();

        if (hour == 19 && minute <= 30) {
            return true; // 19:00 - 19:30
        }
        if (hour == 18 && minute >= 30) {
            return true; // 18:30 - 19:00
        }

        return false;
    }

    /**
     * Check if enough time has passed since last upload for this raid
     * Returns true if upload is allowed
     */
    private static boolean canUpload(String raidType) {
        // Always allow during reset time
        if (isResetTime()) {
            return true;
        }

        Long lastUpload = lastUploadTime.get(raidType);
        if (lastUpload == null) {
            return true; // Never uploaded before
        }

        long timeSinceLastUpload = System.currentTimeMillis() - lastUpload;
        return timeSinceLastUpload >= UPLOAD_COOLDOWN_MS;
    }

    /**
     * Detects and displays all 4 daily gambits from the Party Finder menu
     * Gambit format: "Name Gambit" or "Name Name's Gambit"
     * Example: "Glutton's Gambit", "Dull Blade's Gambit"
     * Description is between "Refreshes in X hours" and "Rewards for enabling"
     * Also saves to GambitData for the GUI
     */
    public static void detectGambit(HandledScreen<?> screen) {
        if (screen == null) return;

        try {
            List<String> gambitsForChat = new ArrayList<>();
            List<GambitData.GambitEntry> gambitsForSave = new ArrayList<>();

            // Search through inventory slots for all gambit items
            for (Slot slot : screen.getScreenHandler().slots) {
                if (!slot.hasStack()) continue;

                ItemStack stack = slot.getStack();
                if (stack.getCustomName() == null) continue;

                String itemName = stack.getCustomName().getString().replaceAll("§.", "").trim();

                // Check if item name contains "Gambit"
                if (itemName.contains("Gambit")) {
                    // Get the tooltip for description
                    List<Text> tooltips = stack.getTooltip(Item.TooltipContext.DEFAULT,
                        MinecraftClient.getInstance().player, TooltipType.BASIC);

                    StringBuilder description = new StringBuilder();
                    boolean inDescriptionSection = false;

                    for (Text tooltip : tooltips) {
                        String line = tooltip.getString().replaceAll("§.", "").trim();

                        // Start collecting after "Refreshes in" line
                        if (line.contains("Refreshes in")) {
                            inDescriptionSection = true;
                            continue;
                        }

                        // Stop collecting at "Rewards for enabling" or "Rewards for Enabling"
                        if (line.contains("Rewards for enabling") || line.contains("Rewards for Enabling")) {
                            break;
                        }

                        // Collect description lines
                        if (inDescriptionSection && !line.isEmpty()) {
                            if (description.length() > 0) description.append(" ");
                            description.append(line);
                        }
                    }

                    // Only add if we have both name and description
                    if (description.length() > 0) {
                        gambitsForChat.add("§e" + itemName + " §7- " + description.toString());
                        gambitsForSave.add(new GambitData.GambitEntry(itemName, description.toString()));
                    }
                }
            }

            // Save gambits to data storage
            if (!gambitsForSave.isEmpty()) {
                GambitData.INSTANCE.saveGambits(gambitsForSave);
            }

            // Upload to crowdsourcing API (don't spam chat)
            if (!gambitsForSave.isEmpty()) {
                WynncraftApiHandler.uploadGambits(gambitsForSave);
            }

        } catch (Exception e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError detecting gambit: " + e.getMessage()));
        }
    }

    /**
     * Scans the raid preview chest for aspects and detects which raid is selected
     * Raid is detected from the screen title itself (last character changes per raid)
     * Also extracts user's aspect tier and progress for each aspect
     * Saves loot pool data locally for the GUI
     */
    public static void scanPreviewChest(HandledScreen<?> screen, String screenTitle) {
        if (screen == null) return;

        System.out.println("[WynnExtras] scanPreviewChest called with title: " + screenTitle);

        try {
            // Detect raid from the last character of the title
            String selectedRaid = "Unknown";
            if (screenTitle.endsWith("\uF00B")) {
                selectedRaid = "NOTG";
            } else if (screenTitle.endsWith("\uF00C")) {
                selectedRaid = "NOL";
            } else if (screenTitle.endsWith("\uF00D")) {
                selectedRaid = "TCC";
            } else if (screenTitle.endsWith("\uF00E")) {
                selectedRaid = "TNA";
            }

            Map<String, Pair<String, String>> foundAspects = new HashMap<>();
            List<LootPoolData.AspectEntry> lootPoolDataFull = new ArrayList<>(); // For saving with full data

            // Collect all aspects with their progress
            for (Slot slot : screen.getScreenHandler().slots) {
                if (!slot.hasStack()) continue;

                ItemStack stack = slot.getStack();
                List<Text> tooltips = stack.getTooltip(Item.TooltipContext.DEFAULT,
                    MinecraftClient.getInstance().player, TooltipType.BASIC);

                String aspectName = null;
                String tierLine = null;
                String rarity = "";
                StringBuilder description = new StringBuilder();
                boolean foundName = false;
                boolean foundTier = false;

                for (Text tooltip : tooltips) {
                    String line = tooltip.getString().replaceAll("§.", "").trim();

                    // Find aspect name
                    if (aspectName == null && (line.contains("Aspect") || line.contains("Embodiment"))) {
                        aspectName = line;
                        foundName = true;

                        // Detect rarity from color with safe null checking
                        if (stack.getCustomName() != null &&
                            stack.getCustomName().getStyle() != null &&
                            stack.getCustomName().getStyle().getColor() != null) {
                            String hexCode = stack.getCustomName().getStyle().getColor().getHexCode();
                            if (hexCode.equals("#AA00AA")) {
                                rarity = "Mythic";
                            } else if (hexCode.equals("#FF5555")) {
                                rarity = "Fabled";
                            } else if (hexCode.equals("#55FFFF")) {
                                rarity = "Legendary";
                            }
                        }
                        continue;
                    }

                    // Check if we've reached tier info
                    if (foundName && (line.contains("Tier") || line.contains("[") || line.contains(">") || line.contains("Class Req:"))) {
                        foundTier = true;
                    }

                    // Collect all description lines (between name and tier info)
                    if (foundName && !foundTier && !line.isEmpty() &&
                        !line.contains("Aspect") && !line.contains("Embodiment")) {
                        if (description.length() > 0) {
                            description.append("\n");
                        }
                        description.append(line);
                    }
                }

                // Find best tier line
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

                if (aspectName != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                    bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                    bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                    foundAspects.put(aspectName, new Pair<>(bestTierLine, rarity));

                    // Save for loot pool data with full info
                    if (!rarity.isEmpty()) {
                        lootPoolDataFull.add(new LootPoolData.AspectEntry(aspectName, rarity, bestTierLine, description.toString()));
                    }
                }
            }

            // Save loot pool data to local storage with full info
            if (!selectedRaid.equals("Unknown") && !lootPoolDataFull.isEmpty()) {
                LootPoolData.INSTANCE.saveLootPoolFull(selectedRaid, lootPoolDataFull);
            }

            // Upload aspects (don't spam chat with list)
            if (!foundAspects.isEmpty()) {
                // Upload with throttling (once per minute per raid, unless reset time)
                if (canUpload(selectedRaid)) {
                    System.out.println("[WynnExtras] Found " + foundAspects.size() + " aspects, uploading...");

                    // Upload personal aspects WITH progress (requires API key)
                    WynncraftApiHandler.processAspects(foundAspects);

                    // Also upload to crowdsourcing (loot pool WITHOUT personal progress, no API key)
                    WynncraftApiHandler.uploadLootPool(selectedRaid, lootPoolDataFull);

                    lastUploadTime.put(selectedRaid, System.currentTimeMillis());
                } else {
                    long timeSinceLastUpload = System.currentTimeMillis() - lastUploadTime.get(selectedRaid);
                    long secondsRemaining = (UPLOAD_COOLDOWN_MS - timeSinceLastUpload) / 1000;
                    System.out.println("[WynnExtras] Upload throttled for " + selectedRaid + ", wait " + secondsRemaining + "s");
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Upload cooldown: wait " + secondsRemaining + "s"));
                }
            }

        } catch (Exception e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError scanning preview chest: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}

