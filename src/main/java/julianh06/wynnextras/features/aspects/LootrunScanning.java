package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootrunScanning {
    private static final Map<String, ZonedDateTime> lastLootrunUploadReset = new HashMap<>();

    private static final Map<String, List<LootrunLootPoolData.LootrunItem>> pendingItems = new HashMap<>();
    private static final Map<String, Boolean> pendingUploadAllowed = new HashMap<>();
    private static boolean waitingForPageLoad = false;
    private static boolean waitingForReturn = false;
    private static boolean forceScan = false;
    private static boolean expectingSecondPage = false;
    private static int settleTicks = 0;
    private static String lastTitle = "";

    public static void handleLootrunPreviewChest(HandledScreen<?> screen, String screenTitle) {
        if (screen == null) return;

        String camp = LootrunLootPoolData.getCampFromTitle(screenTitle);
        if (camp == null) {
            return;
        }

        if (!screenTitle.equals(lastTitle)) {
            lastTitle = screenTitle;
            forceScan = true;
            expectingSecondPage = false;
            waitingForPageLoad = false;
            waitingForReturn = false;
            settleTicks = 0;
            pendingItems.remove(camp);
            pendingUploadAllowed.remove(camp);
        }

        if (waitingForPageLoad || waitingForReturn) {
            settleTicks++;
            if (settleTicks >= 5) {
                settleTicks = 0;
                if (waitingForPageLoad) {
                    waitingForPageLoad = false;
                    forceScan = true;
                } else if (waitingForReturn) {
                    waitingForReturn = false;
                }
            }
            return;
        }

        if (!forceScan) {
            return;
        }

        forceScan = false;
        scanLootrunPreviewChest(screen, camp);
    }

    private static void scanLootrunPreviewChest(HandledScreen<?> screen, String camp) {
        try {
            List<LootrunLootPoolData.LootrunItem> items = collectLootrunItems(screen);

            if (items.isEmpty()) {
                return;
            }

            if (expectingSecondPage) {
                List<LootrunLootPoolData.LootrunItem> combined = new ArrayList<>(pendingItems.getOrDefault(camp, new ArrayList<>()));
                combined.addAll(items);
                LootrunLootPoolData.INSTANCE.saveLootPool(camp, combined);

                if (pendingUploadAllowed.getOrDefault(camp, false)) {
                    WynncraftApiHandler.uploadLootrunLootPool(camp, combined);
                    lastLootrunUploadReset.put(camp, getCurrentLootrunReset());
                }

                pendingItems.remove(camp);
                pendingUploadAllowed.remove(camp);
                expectingSecondPage = false;
                clickPreviousPage(screen);
                waitingForReturn = true;
                return;
            }

            LootrunLootPoolData.INSTANCE.saveLootPool(camp, items);

            if (canUploadLootrun(camp)) {
                if (hasNextPage(screen)) {
                    pendingItems.put(camp, new ArrayList<>(items));
                    pendingUploadAllowed.put(camp, true);
                    expectingSecondPage = true;
                    clickNextPage(screen);
                    waitingForPageLoad = true;
                } else {
                    WynncraftApiHandler.uploadLootrunLootPool(camp, items);
                    lastLootrunUploadReset.put(camp, getCurrentLootrunReset());
                }
            }
        } catch (Exception e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError scanning lootrun preview chest: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private static List<LootrunLootPoolData.LootrunItem> collectLootrunItems(HandledScreen<?> screen) {
        List<LootrunLootPoolData.LootrunItem> items = new ArrayList<>();
        int slotCount = screen.getScreenHandler().slots.size();
        int startIndex = 18;
        int endIndex = Math.min(54, slotCount);

        for (int i = startIndex; i < endIndex; i++) {
            Slot slot = screen.getScreenHandler().slots.get(i);
            if (!slot.hasStack()) continue;

            ItemStack stack = slot.getStack();
            List<Text> tooltips = stack.getTooltip(Item.TooltipContext.DEFAULT,
                    MinecraftClient.getInstance().player, TooltipType.BASIC);

            LootrunLootPoolData.LootrunItem item = parseLootrunItem(stack, tooltips);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static LootrunLootPoolData.LootrunItem parseLootrunItem(ItemStack stack, List<Text> tooltips) {
        String name = stack.getName().getString().replaceAll("§.", "").trim();
        if (name.isEmpty()) {
            return null;
        }

        String rarity = detectRarity(stack, tooltips);
        if (rarity == null) {
            return null;
        }

        String shinyStat = extractShinyTracker(tooltips);
        String type = LootrunLootPoolData.LootrunItem.determineType(name);
        if (!shinyStat.isEmpty()) {
            type = "shiny";
        }

        String tooltipText = buildTooltipText(name, tooltips);

        return new LootrunLootPoolData.LootrunItem(name, rarity, type, tooltipText, shinyStat);
    }

    private static String detectRarity(ItemStack stack, List<Text> tooltips) {
        String loreRarity = detectRarityFromLore(stack);
        if (loreRarity != null) {
            return loreRarity;
        }

        for (Text tooltip : tooltips) {
            String line = tooltip.getString().replaceAll("§.", "").trim();
            String rarity = rarityFromLine(line);
            if (rarity != null) {
                return rarity;
            }
        }

        if (stack.getCustomName() != null &&
                stack.getCustomName().getStyle() != null &&
                stack.getCustomName().getStyle().getColor() != null) {
            String hexCode = stack.getCustomName().getStyle().getColor().getHexCode();
            return rarityFromColor(hexCode);
        }

        String rawName = stack.getName().getString();
        if (rawName.contains("§e")) return "Unique";
        if (rawName.contains("§a")) return "Set";

        return null;
    }

    private static String detectRarityFromLore(ItemStack stack) {
        try {
            if (stack.getComponents() == null) return null;
            LoreComponent loreComponent = stack.getComponents().get(DataComponentTypes.LORE);
            if (loreComponent == null) return null;

            for (Text line : loreComponent.lines()) {
                String rarity = rarityFromLine(line.getString().replaceAll("§.", "").trim());
                if (rarity != null) {
                    return rarity;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String rarityFromLine(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("mythic") && (lower.contains("item") || lower.equals("mythic"))) {
            return "Mythic";
        }
        if (lower.contains("fabled") && (lower.contains("item") || lower.equals("fabled"))) {
            return "Fabled";
        }
        if (lower.contains("legendary") && (lower.contains("item") || lower.equals("legendary"))) {
            return "Legendary";
        }
        if (lower.contains("rare") && (lower.contains("item") || lower.equals("rare"))) {
            return "Rare";
        }
        if (lower.contains("set") && (lower.contains("item") || lower.equals("set"))) {
            return "Set";
        }
        if (lower.contains("unique") && (lower.contains("item") || lower.equals("unique"))) {
            return "Unique";
        }
        return null;
    }

    private static String rarityFromColor(String hexCode) {
        if (hexCode == null) return null;
        return switch (hexCode.toUpperCase()) {
            case "#AA00AA" -> "Mythic";
            case "#FF5555" -> "Fabled";
            case "#55FFFF" -> "Legendary";
            case "#FF55FF" -> "Rare";
            case "#55FF55" -> "Set";
            case "#FFFF55" -> "Unique";
            default -> null;
        };
    }

    private static String extractShinyTracker(List<Text> tooltips) {
        for (Text tooltip : tooltips) {
            if (!tooltip.getString().contains("⬡") || !tooltip.getString().contains(":")) {
                continue;
            }
            return tooltip.getString();
        }
        return "";
    }

    private static String buildTooltipText(String name, List<Text> tooltips) {
        StringBuilder builder = new StringBuilder();
        for (Text tooltip : tooltips) {
            String line = tooltip.getString().replaceAll("§.", "").trim();
            if (line.isEmpty() || line.equals(name)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private static boolean hasNextPage(HandledScreen<?> screen) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            String name = slot.getStack().getName().getString().replaceAll("§.", "").trim();
            if (name.equalsIgnoreCase("Next Page")) {
                return true;
            }
        }
        return false;
    }

    private static void clickNextPage(HandledScreen<?> screen) {
        System.out.println("[WynnExtras] Lootrun preview: clicking next page");
        TreeLoader.clickOnNameInInventory("Next Page", screen, MinecraftClient.getInstance());
        settleTicks = 0;
    }

    private static void clickPreviousPage(HandledScreen<?> screen) {
        System.out.println("[WynnExtras] Lootrun preview: clicking previous page");
        TreeLoader.clickOnNameInInventory("Previous Page", screen, MinecraftClient.getInstance());
        settleTicks = 0;
    }

    public static ZonedDateTime getCurrentLootrunReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));

        ZonedDateTime thisFriday =
                now.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
                        .withHour(20).withMinute(0).withSecond(0).withNano(0);

        if (now.isBefore(thisFriday)) {
            thisFriday = thisFriday.minusWeeks(1);
        }

        return thisFriday;
    }

    private static boolean canUploadLootrun(String camp) {
        ZonedDateTime currentReset = getCurrentLootrunReset();
        ZonedDateTime lastUploaded = lastLootrunUploadReset.get(camp);

        return lastUploaded == null || currentReset.isAfter(lastUploaded);
    }
}