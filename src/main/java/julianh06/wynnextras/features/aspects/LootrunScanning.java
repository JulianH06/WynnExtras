package julianh06.wynnextras.features.aspects;

import com.wynntils.models.gear.type.GearTier;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.ResetTimeConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.utils.ItemUtils;
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

import java.time.ZonedDateTime;
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
                    lastLootrunUploadReset.put(camp, ResetTimeConfig.INSTANCE.getCurrentLootrunReset());
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
                    lastLootrunUploadReset.put(camp, ResetTimeConfig.INSTANCE.getCurrentLootrunReset());
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

    private static Text getDisplayName(ItemStack stack) {
        if (stack.getCustomName() != null) return stack.getCustomName();
        return stack.get(DataComponentTypes.ITEM_NAME);
    }

    private static String cleanName(ItemStack stack) {
        Text nameText = getDisplayName(stack);
        if (nameText == null) return "";
        String raw = nameText.getString();
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);

            if (c >= 0x20 && c < 0xD800) {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("§.", "").trim();
    }

    private static LootrunLootPoolData.LootrunItem parseLootrunItem(ItemStack stack, List<Text> tooltips) {
        String name = cleanName(stack);
        if (name.isEmpty()) {
            return null;
        }

        String rarity = detectRarity(stack, tooltips);
        if (rarity == null) {
            rarity = "Unknown";
        }

        String shinyStat = extractShinyTracker(stack);
        String type = LootrunLootPoolData.LootrunItem.determineType(name);
        if (!shinyStat.isEmpty()) {
            type = "shiny";
        }

        String tooltipText = buildTooltipText(name, tooltips);

        return new LootrunLootPoolData.LootrunItem(name, rarity, type, tooltipText, shinyStat);
    }

    private static String detectRarity(ItemStack stack, List<Text> tooltips) {
        GearTier tier = ItemUtils.getTier(stack);
        if (tier != null) {
            return switch (tier) {
                case MYTHIC -> "Mythic";
                case FABLED -> "Fabled";
                case LEGENDARY -> "Legendary";
                case RARE -> "Rare";
                case SET -> "Set";
                case UNIQUE -> "Unique";
                default -> null;
            };
        }

        return null;
    }

    private static String cleanString(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 0x20 && c < 0xD800) sb.append(c);
        }
        return sb.toString().replaceAll("§.", "").trim();
    }

    private static final java.util.regex.Pattern TRACKER_PATTERN =
        java.util.regex.Pattern.compile("^([A-Za-z][A-Za-z ]+?)\\s*(\\d[\\d,]*)$");

    private static String extractShinyTracker(ItemStack stack) {
        try {
            LoreComponent lore = stack.getComponents().get(DataComponentTypes.LORE);
            if (lore == null) return "";
            for (Text line : lore.lines()) {
                String stripped = cleanString(line.getString());
                if (stripped.isEmpty()) continue;
                if (stripped.contains("+") || stripped.contains("%") || stripped.contains("/") ||
                    stripped.contains("-") || stripped.contains("(") || stripped.contains(":")) continue;
                String lower = stripped.toLowerCase();
                if (lower.contains("combat level") || lower.contains("class type") ||
                    lower.contains("dps") || lower.contains("item") || lower.contains("hits")) continue;
                java.util.regex.Matcher m = TRACKER_PATTERN.matcher(stripped);
                if (m.matches()) {
                    return m.group(1).trim() + ": " + m.group(2).trim();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String buildTooltipText(String name, List<Text> tooltips) {
        StringBuilder builder = new StringBuilder();
        for (Text tooltip : tooltips) {
            String line = tooltip.getString();
            if (line.isEmpty() || line.equals(name)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    private static boolean hasNextPage(HandledScreen<?> screen) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.hasStack()) continue;
            String name = cleanName(slot.getStack());
            if (name.equalsIgnoreCase("Next Page")) {
                return true;
            }
        }
        return false;
    }

    private static void clickNextPage(HandledScreen<?> screen) {
        WynnExtras.LOGGER.info("[WynnExtras] Lootrun preview: clicking next page");
        TreeLoader.clickOnNameInInventory("Next Page", screen, MinecraftClient.getInstance());
        settleTicks = 0;
    }

    private static void clickPreviousPage(HandledScreen<?> screen) {
        WynnExtras.LOGGER.info("[WynnExtras] Lootrun preview: clicking previous page");
        TreeLoader.clickOnNameInInventory("Previous Page", screen, MinecraftClient.getInstance());
        settleTicks = 0;
    }

    private static boolean canUploadLootrun(String camp) {
        ZonedDateTime currentReset = ResetTimeConfig.INSTANCE.getCurrentLootrunReset();
        ZonedDateTime lastUploaded = lastLootrunUploadReset.get(camp);

        return lastUploaded == null || currentReset.isAfter(lastUploaded);
    }
}