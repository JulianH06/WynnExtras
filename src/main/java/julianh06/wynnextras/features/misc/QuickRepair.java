package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickRepair extends WEMenuExtension {

    private static final String BLACKSMITH_TITLE = "󏿸";
    private static final String REPAIR_TITLE = "󏿸";
    private static final int SLOT_REPAIR_ITEMS = 18;
    private static final int[] SLOT_ITEMS = {11, 12, 13, 14, 15};
    private static final int SLOT_NEXT_PAGE = 23;
    private static final int EMPTY_CLOSE_THRESHOLD = 6;
    private static final int BTN_W = 90, BTN_H = 16;
    private static final Pattern DURABILITY_LORE_PATTERN = Pattern.compile("\\[(\\d+)\\s*/\\s*(\\d+)\\s+durability]", Pattern.CASE_INSENSITIVE);

    private static boolean repairing = false;
    private static int spamCooldown = 0;
    private static int emptySlotTicks = 0;
    private static boolean keyWasDown = false;
    private static String lastNextPageSignature = null;

    private RepairButtonWidget repairButton = null;

    public static void startRepair() {
        repairing = true;
        spamCooldown = 0;
        emptySlotTicks = 0;
        lastNextPageSignature = null;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return;
            if (client.player == null) return;

            String title = getCurrentTitle(client);

            if (title == null || (!title.equals(BLACKSMITH_TITLE) && !title.equals(REPAIR_TITLE))) {
                repairing = false;
                spamCooldown = 0;
                emptySlotTicks = 0;
                keyWasDown = false;
                lastNextPageSignature = null;
                return;
            }

            long window = client.getWindow().getHandle();
            int key = WynnExtrasConfig.INSTANCE.quickRepairKey;
            boolean keyDown = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (keyDown && !keyWasDown && title.equals(BLACKSMITH_TITLE)) {
                startRepair();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRepairing..."));
            }
            keyWasDown = keyDown;

            if (!repairing) return;

            if (spamCooldown > 0) { spamCooldown--; return; }

            ScreenHandler menu = McUtils.containerMenu();
            if (menu == null) return;

            if (title.equals(BLACKSMITH_TITLE)) {
                if (menu.slots.size() > SLOT_REPAIR_ITEMS) {
                    ItemStack stack = menu.getSlot(SLOT_REPAIR_ITEMS).getStack();
                    if (!stack.isEmpty()) {
                        ContainerUtils.clickOnSlot(SLOT_REPAIR_ITEMS, menu.syncId, 0, menu.getStacks());
                        spamCooldown = 4;
                    }
                }
            } else if (title.equals(REPAIR_TITLE)) {
                handleRepairScreen(client, menu);
            }
        });
    }

    private static void handleRepairScreen(MinecraftClient client, ScreenHandler menu) {
        for (int slot : SLOT_ITEMS) {
            if (menu.slots.size() <= slot) continue;

            ItemStack stack = menu.getSlot(slot).getStack();
            if (isEmptyRepairSlot(stack)) continue;
            List<Text> tooltip = getTooltip(stack);
            if (cannotAffordRepair(tooltip)) continue;

            Integer durability = getDurabilityPercentage(stack, tooltip);
            if (durability == null || durability <= WynnExtrasConfig.INSTANCE.quickRepairDurabilityThreshold) {
                emptySlotTicks = 0;
                lastNextPageSignature = null;
                ContainerUtils.clickOnSlot(slot, menu.syncId, 0, menu.getStacks());
                spamCooldown = 2;
                return;
            }
        }

        if (hasNextPage(menu)) {
            String signature = getPageSignature(menu);
            if (!signature.equals(lastNextPageSignature)) {
                emptySlotTicks = 0;
                lastNextPageSignature = signature;
                ContainerUtils.clickOnSlot(SLOT_NEXT_PAGE, menu.syncId, 0, menu.getStacks());
                spamCooldown = 4;
                return;
            }
        }

        emptySlotTicks++;
        if (emptySlotTicks >= EMPTY_CLOSE_THRESHOLD) {
            repairing = false;
            emptySlotTicks = 0;
            lastNextPageSignature = null;
            client.execute(() -> client.player.closeHandledScreen());
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aAll items repaired!"));
        }
    }

    private static boolean hasNextPage(ScreenHandler menu) {
        if (menu.slots.size() <= SLOT_NEXT_PAGE) return false;
        return !isEmptyRepairSlot(menu.getSlot(SLOT_NEXT_PAGE).getStack());
    }

    private static boolean isEmptyRepairSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        String itemName = getCleanItemName(stack);
        return itemName.contains("empty");
    }

    private static String getPageSignature(ScreenHandler menu) {
        StringBuilder signature = new StringBuilder();
        for (int slot : SLOT_ITEMS) {
            if (menu.slots.size() <= slot) continue;
            ItemStack stack = menu.getSlot(slot).getStack();
            List<Text> tooltip = getTooltip(stack);
            signature.append(slot).append(':')
                    .append(getCleanItemName(stack)).append(':')
                    .append(cannotAffordRepair(tooltip)).append(':')
                    .append(getDurabilityPercentage(stack, tooltip)).append('|');
        }
        return signature.toString();
    }

    private static String getCleanItemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return stack.getName().getString().replaceAll("§[0-9a-fk-or]", "").toLowerCase();
    }

    private static Integer getDurabilityPercentage(ItemStack stack, List<Text> tooltip) {
        Integer loreDurability = getLoreDurabilityPercentage(tooltip);
        if (loreDurability != null) return loreDurability;

        try {
            Optional<WynnItem> wynnItem = Models.Item.getWynnItem(stack);
            if (wynnItem.isPresent() && wynnItem.get() instanceof DurableItemProperty durable) {
                return durable.getDurability().getPercentageInt();
            }
        } catch (Exception ignored) {}

        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) return null;

        int remaining = stack.getMaxDamage() - stack.getDamage();
        return Math.clamp(Math.round(remaining * 100f / stack.getMaxDamage()), 0, 100);
    }

    private static List<Text> getTooltip(ItemStack stack) {
        try {
            return stack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
        } catch (Exception ignored) {}
        return List.of();
    }

    private static boolean cannotAffordRepair(List<Text> tooltip) {
        for (Text line : tooltip) {
            if (line.getString().toLowerCase().contains("cannot afford to repair")) return true;
        }
        return false;
    }

    private static Integer getLoreDurabilityPercentage(List<Text> tooltip) {
        for (Text line : tooltip) {
            Matcher matcher = DURABILITY_LORE_PATTERN.matcher(line.getString());
            if (!matcher.find()) continue;

            int current = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            if (max <= 0) return null;
            return Math.clamp(Math.round(current * 100f / max), 0, 100);
        }
        return null;
    }

    private static String getCurrentTitle(MinecraftClient mc) {
        if (mc.currentScreen == null) return null;
        return mc.currentScreen.getTitle().getString();
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return;
        if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return;
        if (!screen.getTitle().getString().equals(BLACKSMITH_TITLE)) return;

        if (repairButton == null) {
            repairButton = new RepairButtonWidget();
            rootWidgets.add(repairButton);
        }

        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int bx = acc.getX() + acc.getBackgroundWidth() / 2 - BTN_W / 2;
        int by = acc.getY() + acc.getBackgroundWidth() - BTN_H / 4;
        repairButton.setBounds(bx, by, BTN_W, BTN_H);
        repairButton.setVisible(true);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (repairButton != null && !WynnExtrasConfig.INSTANCE.quickRepairEnabled)
            repairButton.setVisible(false);
    }

    private static class RepairButtonWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String keyName = GLFW.glfwGetKeyName(WynnExtrasConfig.INSTANCE.quickRepairKey, 0);
            if (keyName == null) keyName = "?";
            String label = "Quick Repair [" + keyName.toUpperCase() + "]";

            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f, 1f);
        }

        @Override
        protected boolean onClick(int button) {
            startRepair();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRepairing..."));
            return true;
        }
    }
}
