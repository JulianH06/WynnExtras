package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CharacterInfoContainer;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.UnknownGearItem;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Pair;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.loader.SkillPointLoader;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
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
        if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return false;

        Slot focused = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (focused == null || !focused.hasStack()) return false;

        ItemStack clicked = focused.getStack();
        Optional<WynnItem> wynnItemOpt = BankOverlay2.asWynnItem(clicked);

        if (wynnItemOpt.isEmpty() || !(wynnItemOpt.get() instanceof GearItem)) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
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
        if(!(Models.Container.getCurrentContainer() instanceof CharacterInfoContainer)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

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
            ItemStack item = McUtils.player().getEquippedStack(EquipmentSlot.FROM_INDEX.apply(4 - i));
            itemWidgets.get(i).setBounds((int) (itemXStart + i * backgroundWidth / 3f), (int) itemYStart, itemWidth, itemHeight);
            itemWidgets.get(i).setItem(item);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(hoveredItem.isEmpty()) return;

        ctx.drawItemTooltip(MinecraftClient.getInstance().textRenderer, hoveredItem, mouseX, mouseY);
    }

    private static class ItemWidget extends Widget {
        ItemStack item;
        WynnItem wynnItem = null;
        GearRequirements requirements = null;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(item == null) return;

            ctx.drawItem(item, (int) (x / ui.getScaleFactor()), (int) (y / ui.getScaleFactor()));

            if(hovered) {
                hoveredItem = item;
                ui.drawRect(x - 0.5f, y - 0.25f, width, height, CustomColor.fromHexString("FFFFFF").withAlpha(0.25f));
            }

            if(requirements == null) return;

            if(true) return;

            List<Pair<Skill, Integer>> skills = requirements.skills();

            if(skills == null) return;

            int textY = y + 64;

            for(Pair<Skill, Integer> skill : skills) {
                String skillName = skill.a().getColorCode().toString() + skill.a().getDisplayName().substring(0, 3) + ": §r";
                int skillReq = skill.b();

                int skillCurrent = Models.SkillPoint.getAssignedSkillPoints(skill.a());

                ui.drawCenteredText(skillName + skillCurrent + "/" + skillReq, x + width / 2f, textY);
                textY += 40;
            }
        }

        public void setItem(ItemStack item) {
            this.item = item;

            Optional<WynnItem> wynnItemOpt = BankOverlay2.asWynnItem(item);

            if(wynnItemOpt.isEmpty()) return;

            wynnItem = wynnItemOpt.get();

            if(!(wynnItem instanceof GearItem gearItem)) return;

            if(gearItem.getItemInfo() == null) return;

            requirements = gearItem.getItemInfo().requirements();
        }
    }

    private static class AutoAssignButton extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!(Models.Container.getCurrentContainer() instanceof CharacterInfoContainer)) return;
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
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("Click a weapon in your inventory, or skip weapon selection.")));
            return true;
        }
    }

    private static void startAssignment() {
        int[] required = calculateRequiredSkillPoints(selectedWeapon);
        if (required == null) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    Text.of("No skill point requirements found.")));
            return;
        }

        boolean alreadySatisfied =
                Models.SkillPoint.getAssignedSkillPoints(Skill.STRENGTH)     >= required[0] &&
                        Models.SkillPoint.getAssignedSkillPoints(Skill.DEXTERITY)    >= required[1] &&
                        Models.SkillPoint.getAssignedSkillPoints(Skill.INTELLIGENCE) >= required[2] &&
                        Models.SkillPoint.getAssignedSkillPoints(Skill.DEFENCE)      >= required[3] &&
                        Models.SkillPoint.getAssignedSkillPoints(Skill.AGILITY)      >= required[4];

        if (alreadySatisfied) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
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
            ItemStack stack = McUtils.player().getEquippedStack(EquipmentSlot.FROM_INDEX.apply(4 - i));
            SolvableItem si = toSolvableItem(stack);
            if (si != null) nonWeaponItems.add(si);
        }

        //Accessories
        for (int i = 9; i < 13; i++) {
            ItemStack stack = McUtils.player().getInventory().getStack(i);
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
        Optional<WynnItem> wynnOpt = BankOverlay2.asWynnItem(stack);
        if (wynnOpt.isEmpty()) {
            try {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4Warning: The following item is not recognized and ignored in the calculation: " + stack.getCustomName().getString()));
            } catch (Exception ignored) {}
            return null;
        }

        WynnItem wynnItem = wynnOpt.get();

        if(wynnItem instanceof UnknownGearItem) {
            try {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4Warning: The following item is not recognized and ignored in the calculation: " + stack.getCustomName().getString()));
            } catch (Exception ignored) {}
            return null;
        }

        SolvableItem si = new SolvableItem();

        if (wynnItem instanceof GearItem gearItem) {
            if (gearItem.getItemInfo() == null) return null;

            GearRequirements req = gearItem.getItemInfo().requirements();
            if (req != null && req.skills() != null) {
                for (Pair<Skill, Integer> pair : req.skills()) {
                    int idx = skillToIndex(pair.a());
                    if (idx >= 0) si.reqs[idx] = pair.b();
                }
            }

            for (Pair<StatType, StatPossibleValues> statPair : gearItem.getItemInfo().variableStats()) {
                if (!(statPair.a() instanceof SkillStatType skillStat)) continue;
                int idx = skillToIndex(skillStat.getSkill());
                if (idx >= 0) si.bonuses[idx] = statPair.b().baseValue();
            }

            si.name = gearItem.getName();
        } else if (wynnItem instanceof CraftedGearItem craftedItem) {
            if (craftedItem.getRequirements() == null) return null;

            WynnExtras.LOGGER.info("[WE-CRAFT] class=" + craftedItem.getClass().getName());
            for (var method : craftedItem.getClass().getMethods()) {
                if (method.getName().toLowerCase().contains("req") ||
                        method.getName().toLowerCase().contains("skill") ||
                        method.getName().toLowerCase().contains("stat") ||
                        method.getName().toLowerCase().contains("info")) {
                    WynnExtras.LOGGER.info("[WE-CRAFT] method: " + method.getName()
                            + " → " + method.getReturnType().getSimpleName());
                }
            }

            GearRequirements req = craftedItem.getRequirements();
            if (req != null && req.skills() != null) {
                for (Pair<Skill, Integer> pair : req.skills()) {
                    int idx = skillToIndex(pair.a());
                    if (idx >= 0) si.reqs[idx] = pair.b();
                }
            }

            si.name = craftedItem.getName();
        } else {
            try {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§4Warning: The following item is not recognized and ignored in the calculation: " + stack.getCustomName().getString()));
            } catch (Exception ignored) { }
            return null;
        }

        return si;
    }

    private static int skillToIndex(Skill skill) {
        return switch (skill) {
            case STRENGTH     -> 0;
            case DEXTERITY    -> 1;
            case INTELLIGENCE -> 2;
            case DEFENCE      -> 3;
            case AGILITY      -> 4;
            default           -> -1;
        };
    }

    public static boolean isSelectingWeapon() {
        return selectingWeapon;
    }

    public static void setSelectingWeapon(boolean selectingWeapon) {
        CompassMenuOverlay.selectingWeapon = selectingWeapon;
    }
}