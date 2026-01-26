package julianh06.wynnextras.features.crafting;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.joml.Vector2i;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CraftingResultPreviewer {
    private final static Pattern craftingPattern = Pattern.compile("§7 - §f(\\w+) §7\\[Lv\\. (\\d+)\\.0 to (\\d+)\\.0]");

    private static DefaultedList<ItemStack> stacks = DefaultedList.of();
    private static CraftingResult result = null;

    public static void onRender(DrawContext context, HandledScreen<?> screen) {
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;

        if (result != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            List<Text> tooltip = result.getTooltip();
            int tooltipWidth = tooltip.stream().mapToInt(textRenderer::getWidth).max().orElse(0) + 16;

            int x = ((screenWidth - 176) / 2) - (tooltipWidth + 32);
            int y = (screenHeight - 166) / 2;

            context.drawTooltip(textRenderer, tooltip, x, y);
        }

        DefaultedList<ItemStack> stacks = McUtils.containerMenu().getStacks();
        if (stacks.equals(CraftingResultPreviewer.stacks)) return; // probably a slot changed even but i dont wanna find it
        CraftingResultPreviewer.stacks = stacks;
        update();
    }

    private static void update() {
        List<Text> checkmarkTooltip = getTooltip(stacks, 13);
        if (!checkmarkTooltip.getFirst().getString().contains("Craft")) {
            result = null;
            return;
        }

        Matcher matcher = craftingPattern.matcher(checkmarkTooltip.get(2).getString());
        if (matcher.find()) {
            String typeStr = matcher.group(1);
            int minLvl = Integer.parseInt(matcher.group(2));
            int maxLvl = Integer.parseInt(matcher.group(3));

            CraftableType type = CraftableType.fromCraftingName(typeStr);
            if (type == null) {
                result = null;
                return;
            }
            Vector2i lvl = new Vector2i(minLvl, maxLvl);

            String mat1 = getTooltip(stacks, 0).getFirst().getString();
            int mat1Tier = parseMaterialTier(mat1);
            int mat1Count = stacks.getFirst().getCount();

            String mat2 = getTooltip(stacks, 9).getFirst().getString();
            int mat2Tier = parseMaterialTier(mat2);
            int mat2Count = stacks.get(9).getCount();

            int sMatTier = mat1Tier;
            int lMatTier = mat2Tier;
            if (mat1Count > mat2Count) {
                sMatTier = mat2Tier;
                lMatTier = mat1Tier;
            }

            String ing1 = getIngName(stacks, 2);
            String ing2 = getIngName(stacks, 3);
            String ing3 = getIngName(stacks, 11);
            String ing4 = getIngName(stacks, 12);
            String ing5 = getIngName(stacks, 20);
            String ing6 = getIngName(stacks, 21);

            String[] ingredients = {ing1, ing2, ing3, ing4, ing5, ing6};

            Recipe recipe = new Recipe(
                    ingredients,
                    sMatTier,
                    lMatTier,
                    lvl,
                    type
            );
            result = recipe.craft();
        }
    }

    private static List<Text> getTooltip(DefaultedList<ItemStack> stacks, int slot) {
        return stacks.get(slot).getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
    }

    private static String getIngName(DefaultedList<ItemStack> stacks, int slot) {
        String name = getTooltip(stacks, slot).getFirst().getString();
        int index = name.indexOf("[");
        if (index == -1) return null;
        return name.substring(0, index).trim();
    }

    private static int parseMaterialTier(String name) {
        int startIdx = name.indexOf("[");
        int endIdx = name.indexOf("]", startIdx);

        if (startIdx == -1 || endIdx == -1) return -1;

        String tierIndicator = name.substring(startIdx + 1, endIdx);
        return switch (tierIndicator) {
            case "§e✫§8✫✫§6" -> 1;
            case "§e✫✫§8✫§6" -> 2;
            case "§e✫✫✫§6" -> 3;
            default -> {
                WynntilsMod.warn("Cannot parse tier from: " + tierIndicator);
                yield 1;
            }
        };
    }
}
