package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.TooltipUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.loader.SkillPointLoader;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.wynncraft.state.SkillPoint;
import julianh06.wynnextras.wynncraft.state.SkillPointState;
import julianh06.wynnextras.wynncraft.item.ItemCategory;
import julianh06.wynnextras.wynncraft.item.WynnItemData;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;


public class CompassMenuOverlay extends WEMenuExtension {
    AutoAssignButton autoAssignButton;
    List<ItemWidget> itemWidgets = new ArrayList<>();
    static ItemStack hoveredItem = Items.AIR.getDefaultStack();

    static boolean selectingWeapon = false;
    static ItemStack selectedWeapon = null;

    public CompassMenuOverlay() {
        for (int i = 0; i < 4; i++) {
            ItemWidget itemWidget = new ItemWidget();
            itemWidgets.add(itemWidget);
            rootWidgets.add(itemWidget);
        }

        autoAssignButton = new AutoAssignButton();
        rootWidgets.add(autoAssignButton);
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        hoveredItem = Items.AIR.getDefaultStack();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) return true;

        if (!selectingWeapon) return false;
        if (!(MinecraftUtils.screen() instanceof HandledScreen<?> screen)) return false;

        Slot focused = HandledScreenAccess.focusedSlot(screen);
        if (focused == null || !focused.hasStack()) return false;

        ItemStack clicked = focused.getStack();
        WynnItemData item = WynnItemParser.parse(clicked).orElse(null);
        if (item == null || item.category() != ItemCategory.GEAR || !item.gearType().isWeapon()) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("That's not a valid weapon. Click a weapon item.")));
            return true;
        }

        selectedWeapon = clicked;
        selectingWeapon = false;
        startAssignment();
        return true;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;
        if(!(MinecraftUtils.screen() instanceof HandledScreen<?> screen)) return;

        float xStart = hsX(screen);
        float yStart = hsY(screen) + hsHeight(screen);
        float backgroundWidth = hsWidth(screen);
        int buttonWidth = 133;

        int itemWidth = 17;
        int itemHeight = itemWidth;
        float itemXStart = xStart + 8;
        float itemYStart = yStart + 20;

        autoAssignButton.setBounds((int) (xStart + (backgroundWidth - buttonWidth) / 2f), (int) (itemYStart + 22), buttonWidth, 17);

        ui.drawCenteredText(WynnExtras.addWynnExtrasPrefix("§6Skillpoint helper:"), xStart + backgroundWidth / 2f, yStart + 8, CustomColor.fromHexString("FFFFFF"), 1f);
        ui.drawCenteredText(Text.of("§7This is an experimental feature, new items"), xStart + backgroundWidth / 2f, (float) (itemYStart + (selectingWeapon ? 57 : 43)), CustomColor.fromHexString("FFFFFF"), 0.67f);
        ui.drawCenteredText(Text.of("§7and crafteds might not be recognized yet"), xStart + backgroundWidth / 2f, (float) (itemYStart + (selectingWeapon ? 63 : 50)), CustomColor.fromHexString("FFFFFF"), 0.67f);
        if(selectingWeapon) ui.drawCenteredText(Text.of("§eClick on a weapon if you want to include it in the calculation."), xStart + backgroundWidth / 2f, (float) (itemYStart + 47), CustomColor.fromHexString("FFFFFF"), 0.8f);

        backgroundWidth -= 32;
        for(int i = 0; i < 4; i++) {
            ItemStack item = MinecraftUtils.player().getEquippedStack(EquipmentSlot.FROM_INDEX.apply(4 - i));
            itemWidgets.get(i).setBounds((int) (itemXStart + i * backgroundWidth / 3f), (int) itemYStart, itemWidth, itemHeight);
            itemWidgets.get(i).setItem(item);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderHoveredTooltip(ctx, mouseX, mouseY);
    }

    public void renderHoveredTooltip(DrawContext ctx, int mouseX, int mouseY) {
        if(hoveredItem.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        List<Text> tooltip = hoveredItem.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC);
        drawTooltip(mc.textRenderer, TooltipUtils.getClientTooltipComponent(tooltip), mouseX + 14, mouseY, ctx);
    }

    private static void drawTooltip(net.minecraft.client.font.TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, DrawContext context) {
        if (components.isEmpty()) return;

        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        TooltipComponent component;
        for (Iterator<TooltipComponent> iterator = components.iterator(); iterator.hasNext(); height += component.getHeight(textRenderer)) {
            component = iterator.next();
            width = Math.max(width, component.getWidth(textRenderer));
        }

        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        if (x + width > screenWidth) x = Math.max(4, screenWidth - width - 4);
        if (y + height > screenHeight) y = Math.max(4, screenHeight - height - 4);

        TooltipBackgroundRenderer.render(context, x, y, width, height, null);

        int textY = y;
        for (int i = 0; i < components.size(); i++) {
            component = components.get(i);
            component.drawText(context, textRenderer, x, textY);
            textY += component.getHeight(textRenderer) + (i == 0 ? 2 : 0);
        }

        int itemY = y;
        for (int i = 0; i < components.size(); i++) {
            component = components.get(i);
            component.drawItems(textRenderer, x, itemY, width, height, context);
            itemY += component.getHeight(textRenderer) + (i == 0 ? 2 : 0);
        }
    }

    private static class ItemWidget extends Widget {
        ItemStack item;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(item == null) return;

            ctx.drawItem(item, (int) (x / ui.getScaleFactor()), (int) (y / ui.getScaleFactor()));

            if(hovered) {
                hoveredItem = item;
                ui.drawRect(x - 0.5f, y - 0.25f, width, height, CustomColor.fromHexString("FFFFFF").withAlpha(0.25f));
            }

        }

        public void setItem(ItemStack item) {
            this.item = item;
        }
    }

    private static class AutoAssignButton extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (!WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;
            ui.drawButton(x, y, width, height, hovered);
            if (selectingWeapon) {
                ui.drawCenteredText("Skip weapon selection", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 1f);
            } else {
                ui.drawCenteredText("Auto assign skill points", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 1f);
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (selectingWeapon) {
                selectedWeapon = null;
                selectingWeapon = false;
                startAssignment();
                return true;
            }

            selectedWeapon = null;
            selectingWeapon = true;
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Click a weapon in your inventory, or skip weapon selection.")));
            return true;
        }
    }

    private static void startAssignment() {
        int[] required = calculateRequiredSkillPoints(selectedWeapon);
        if (required == null) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("No skill point requirements found.")));
            return;
        }

        boolean alreadySatisfied =
                SkillPointState.assigned(SkillPoint.STRENGTH) >= required[0] &&
                        SkillPointState.assigned(SkillPoint.DEXTERITY) >= required[1] &&
                        SkillPointState.assigned(SkillPoint.INTELLIGENCE) >= required[2] &&
                        SkillPointState.assigned(SkillPoint.DEFENCE) >= required[3] &&
                        SkillPointState.assigned(SkillPoint.AGILITY) >= required[4];

        if (alreadySatisfied) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Requirements already satisfied.")));
            return;
        }

        if(required.length < 5) return;

        SkillPointLoader.getInstance().load(required[0], required[1], required[2], required[3], required[4]);
    }

    private static int[] calculateRequiredSkillPoints(ItemStack weapon) {
        List<SolvableItem> nonWeaponItems = new ArrayList<>();

        //Armor
        for (int i = 0; i < 4; i++) {
            ItemStack stack = MinecraftUtils.player().getEquippedStack(EquipmentSlot.FROM_INDEX.apply(4 - i));
            SolvableItem si = toSolvableItem(stack);
            if (si != null) nonWeaponItems.add(si);
        }

        //Accessories
        for (int i = 9; i < 13; i++) {
            ItemStack stack = MinecraftUtils.player().getInventory().getStack(i);
            SolvableItem si = toSolvableItem(stack);
            if (si != null) nonWeaponItems.add(si);
        }

        SolvableItem weaponItem = (weapon != null && !weapon.isEmpty()) ? toSolvableItem(weapon) : null;

        if (nonWeaponItems.isEmpty() && weaponItem == null) return null;

        int[] best = null;
        List<SolvableItem> bestOrder = null;

        int n = nonWeaponItems.size();
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) indices[i] = i;

        List<List<Integer>> permutations = new ArrayList<>();
        generatePermutations(indices, 0, permutations);

        for (List<Integer> perm : permutations) {
            List<SolvableItem> order = new ArrayList<>();
            for (int idx : perm) order.add(nonWeaponItems.get(idx));

            int[] candidate = evaluateOrder(order, weaponItem);

            if (best == null || isBetter(candidate, best)) {
                best = candidate;
                bestOrder = new ArrayList<>(order);
            }
        }

        if (best == null) best = new int[5];
        //logBestOrder(bestOrder != null ? bestOrder : List.of(), weaponItem, best);
        return best;
    }

    private static int[] evaluateOrder(List<SolvableItem> order, SolvableItem weapon) {
        int[] current = new int[5];
        int[] assigned = new int[5];

        for (SolvableItem entry : order) {
            for (int i = 0; i < 5; i++) {
                if (entry.reqs[i] <= 0) continue;
                if (current[i] < entry.reqs[i]) {
                    int diff = entry.reqs[i] - current[i];
                    assigned[i] += diff;
                    current[i] += diff;
                }
            }
            for (int i = 0; i < 5; i++) {
                if (entry.bonuses[i] != 0) {
                    current[i] += entry.bonuses[i];
                }
            }
        }

        if (weapon != null) {
            for (int i = 0; i < 5; i++) {
                if (weapon.reqs[i] <= 0) continue;
                if (current[i] < weapon.reqs[i]) {
                    int diff = weapon.reqs[i] - current[i];
                    assigned[i] += diff;
                    current[i] += diff;
                }
            }
        }

        for (SolvableItem entry : order) {
            for (int i = 0; i < 5; i++) {
                if (entry.reqs[i] <= 0) continue;
                int effectiveReq = entry.reqs[i] + Math.max(0, entry.bonuses[i]);
                if (current[i] < effectiveReq) {
                    int diff = effectiveReq - current[i];
                    assigned[i] += diff;
                    current[i] += diff;
                }
            }
        }

        for (int i = 0; i < 5; i++) {
            assigned[i] = Math.min(150, Math.max(0, assigned[i]));
        }

        return assigned;
    }

    private static void logBestOrder(List<SolvableItem> order, SolvableItem weapon, int[] assigned) {
        String[] skillNames = {"STR", "DEX", "INT", "DEF", "AGI"};
        WynnExtras.LOGGER.info("[WE-BEST] === Best order found ===");
        for (SolvableItem si : order) {
            WynnExtras.LOGGER.info("[WE-BEST]   " + si.name
                    + " reqs=" + Arrays.toString(si.reqs)
                    + " bonuses=" + Arrays.toString(si.bonuses));
        }
        if (weapon != null) WynnExtras.LOGGER.info("[WE-BEST]   WEAPON " + weapon.name);
        WynnExtras.LOGGER.info("[WE-BEST]   result=" + Arrays.toString(assigned)
                + " total=" + (assigned[0]+assigned[1]+assigned[2]+assigned[3]+assigned[4]));
    }

    private static boolean isBetter(int[] candidate, int[] best) {
        int sumC = 0, sumB = 0;
        for (int i = 0; i < 5; i++) { sumC += candidate[i]; sumB += best[i]; }
        return sumC < sumB;
    }

    private static void generatePermutations(int[] arr, int start, List<List<Integer>> result) {
        if (start == arr.length) {
            List<Integer> perm = new ArrayList<>();
            for (int v : arr) perm.add(v);
            result.add(perm);
            return;
        }
        for (int i = start; i < arr.length; i++) {
            int tmp = arr[start]; arr[start] = arr[i]; arr[i] = tmp;
            generatePermutations(arr, start + 1, result);
            tmp = arr[start]; arr[start] = arr[i]; arr[i] = tmp;
        }
    }

    private static class SolvableItem {
        String name = "?";
        int[] reqs = new int[5];
        int[] bonuses = new int[5];
    }

    private static SolvableItem toSolvableItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Optional<WynnItemData> data = WynnItemParser.parse(stack);
        if (data.isEmpty()) {
            try {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4Warning: The following item is not recognized and ignored in the calculation: " + stack.getCustomName().getString()));
            } catch (Exception ignored) {}
            return null;
        }

        SolvableItem si = new SolvableItem();
        si.reqs = data.get().requirementsArray();
        si.bonuses = data.get().bonusesArray();
        si.name = data.get().name();

        return si;
    }

    public static boolean isSelectingWeapon() {
        return selectingWeapon;
    }

    public static void setSelectingWeapon(boolean selectingWeapon) {
        CompassMenuOverlay.selectingWeapon = selectingWeapon;
    }
}