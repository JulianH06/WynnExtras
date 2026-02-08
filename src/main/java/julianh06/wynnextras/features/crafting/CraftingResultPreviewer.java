package julianh06.wynnextras.features.crafting;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
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
    private static int xPos = 20;
    private static int yPos = 20;

    private static int currentWidth = 0;
    private static int currentHeight = 0;

    private static boolean isDragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private static boolean configLoaded = false;

    private final static Pattern craftingPattern = Pattern.compile("§7 - §f(\\w+) §7\\[Lv\\. (\\d+)\\.0 to (\\d+)\\.0]");

    private static DefaultedList<ItemStack> stacks = DefaultedList.of();
    private static CraftingResult result = null;

    private static void loadConfig() {
        if (configLoaded) return;

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        xPos = config.craftingPreviewOverlayX;
        yPos = config.craftingPreviewOverlayY;

        configLoaded = true;
    }

    private static void saveConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.craftingPreviewOverlayX = xPos;
        config.craftingPreviewOverlayY = yPos;
        WynnExtrasConfig.save();
    }

    public static void onRender(DrawContext context) {
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;

        if(!WynnExtrasConfig.INSTANCE.craftingPreviewOverlay) return;

        loadConfig();
        if (result != null) {
            loadConfig();

            List<Text> lines = result.getTooltip();

            int width = getOverlayWidth(lines);
            int height = getOverlayHeight(lines);

            int bgColor = 0xCC1A1A1A;

            if(WynnExtrasConfig.INSTANCE.craftingPreviewBackground) {
                drawBackground(
                        context,
                        xPos - 4,
                        yPos - 3,
                        xPos + width + 4,
                        yPos + height + 5,
                        bgColor
                );
            }

            Text pillWithTitle = WynnExtras.addWynnExtrasPrefix(Text.literal("Crafting preview").styled(s -> s.withColor(CustomColor.fromHexString("FFAA00").asInt())));
            context.drawText(MinecraftClient.getInstance().textRenderer, pillWithTitle, xPos, yPos, 0xFFFFFFFF, true);

            int y = yPos + 11;
            for (Text line : lines) {
                context.drawText(MinecraftClient.getInstance().textRenderer, line, xPos, y, 0xFFFFFFFF, true);
                y += 11;
            }

            currentWidth = width;
            currentHeight = height;
        }

        DefaultedList<ItemStack> stacks = McUtils.containerMenu().getStacks();
        if (stacks.equals(CraftingResultPreviewer.stacks))
            return; // probably a slot changed even but i dont wanna find it
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

            Recipe.Materials mats = new Recipe.Materials(mat1Tier, mat1Count, mat2Tier, mat2Count);

            String ing1 = getIngName(stacks, 2);
            String ing2 = getIngName(stacks, 3);
            String ing3 = getIngName(stacks, 11);
            String ing4 = getIngName(stacks, 12);
            String ing5 = getIngName(stacks, 20);
            String ing6 = getIngName(stacks, 21);

            String[] ingredients = {ing1, ing2, ing3, ing4, ing5, ing6};

            Recipe recipe = new Recipe(
                    ingredients,
                    mats,
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

    public static boolean handleClick(double mouseX, double mouseY, int button, int action) {
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer))
            return false;

        loadConfig();

        if (currentWidth == 0 || currentHeight == 0)
            return false;

        boolean inBounds =
                mouseX >= xPos && mouseX <= xPos + currentWidth &&
                        mouseY >= yPos && mouseY <= yPos + currentHeight;

        // Drag Release
        if (action == 0 && button == 0 && isDragging) {
            isDragging = false;
            saveConfig();
            return true;
        }

        if (!inBounds) return false;

        // Drag Start (Right Click)
        if (action == 1 && button == 0) {
            isDragging = true;
            dragOffsetX = (int) mouseX - xPos;
            dragOffsetY = (int) mouseY - yPos;
            return true;
        }

        return true;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (!isDragging) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        xPos = (int) mouseX - dragOffsetX;
        yPos = (int) mouseY - dragOffsetY;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        xPos = Math.max(0, Math.min(xPos, screenW - currentWidth));
        yPos = Math.max(0, Math.min(yPos, screenH - currentHeight));
    }

    private static int getOverlayWidth(List<Text> lines) {
        float max = 0;
        for (Text t : lines) {
            float w = FontRenderer.getInstance()
                    .getFont()
                    .getWidth(t.getString());
            max = Math.max(max, w);
        }

        Text pillWithTitle = WynnExtras.addWynnExtrasPrefix(Text.literal("Crafting preview"));
        max = Math.max(max, FontRenderer.getInstance().getFont().getWidth(pillWithTitle));

        return (int) max + 10;
    }

    private static int getOverlayHeight(List<Text> lines) {
        int lineHeight = 11;
        return lines.size() * lineHeight + 8;
    }

    private static void drawBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int r = 3;
        context.fill(x1 + r, y1, x2 - r, y2, color);
        context.fill(x1, y1 + r, x1 + r, y2 - r, color);
        context.fill(x2 - r, y1 + r, x2, y2 - r, color);

        context.fill(x1 + 1, y1 + 1, x1 + r, y1 + r, color);
        context.fill(x2 - r, y1 + 1, x2 - 1, y1 + r, color);
        context.fill(x1 + 1, y2 - r, x1 + r, y2 - 1, color);
        context.fill(x2 - r, y2 - r, x2 - 1, y2 - 1, color);
    }

}
