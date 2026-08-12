package julianh06.wynnextras.features.crafting;

import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.features.crafting.model.GearAttackSpeed;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.joml.Vector2i;

import java.util.List;
import java.util.Optional;
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

    private final static Pattern craftingPattern = Pattern.compile(" - ([A-Za-z]+) \\[Lv\\. (\\d+)(?:\\.0)? to (\\d+)(?:\\.0)?]");

    private static DefaultedList<ItemStack> stacks = DefaultedList.of();
    private static CraftingResult result = null;
    private static GearAttackSpeed importedAttackSpeed = GearAttackSpeed.NORMAL;

    public static void setImportedAttackSpeed(int encodedAttackSpeed) {
        importedAttackSpeed = switch (encodedAttackSpeed) {
            case 0 -> GearAttackSpeed.SLOW;
            case 1 -> GearAttackSpeed.NORMAL;
            case 2 -> GearAttackSpeed.FAST;
            default -> GearAttackSpeed.NORMAL;
        };
    }

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
        if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) return;

        if (!WynnExtrasConfig.INSTANCE.craftingPreviewOverlay) return;

        loadConfig();
        WynnDataService dataService = WynnDataService.getInstance();
        if (dataService.getState() != WynnDataService.State.READY) {
            Text status = Text.literal(dataService.getStatusMessage());
            context.drawText(MinecraftClient.getInstance().textRenderer, status, xPos, yPos, 0xFFFF5555, true);
            currentWidth = MinecraftClient.getInstance().textRenderer.getWidth(status);
            currentHeight = 11;
            result = null;
            return;
        }
        if (result != null) {
            List<Text> lines = result.getTooltip();

            int width = getOverlayWidth(lines);
            int height = getOverlayHeight(lines);
            int bgColor = 0xCC1A1A1A;

            if (WynnExtrasConfig.INSTANCE.craftingPreviewBackground) {
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

        if (MinecraftUtils.player() == null) return;
        DefaultedList<ItemStack> stacks = MinecraftUtils.containerMenu().getStacks();
        if (sameStacks(stacks, CraftingResultPreviewer.stacks))
            return; // probably a slot changed even but i dont wanna find it
        CraftingResultPreviewer.stacks = copyStacks(stacks);
        update();
    }

    private static DefaultedList<ItemStack> copyStacks(DefaultedList<ItemStack> source) {
        DefaultedList<ItemStack> copy = DefaultedList.ofSize(source.size(), ItemStack.EMPTY);
        for (int i = 0; i < source.size(); i++) {
            copy.set(i, source.get(i).copy());
        }
        return copy;
    }

    private static boolean sameStacks(DefaultedList<ItemStack> left, DefaultedList<ItemStack> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            ItemStack a = left.get(i);
            ItemStack b = right.get(i);
            if (a.getCount() != b.getCount()) return false;
            if (!ItemStack.areItemsAndComponentsEqual(a, b)) return false;
        }
        return true;
    }

    private static void update() {
        List<Text> checkmarkTooltip = getTooltip(stacks, 13);
        if (checkmarkTooltip.isEmpty() || !checkmarkTooltip.getFirst().getString().contains("Craft")) {
            result = null;
            return;
        }

        Matcher matcher = findCraftingLine(checkmarkTooltip);
        if (matcher != null) {
            String typeStr = matcher.group(1);
            int minLvl = Integer.parseInt(matcher.group(2));
            int maxLvl = Integer.parseInt(matcher.group(3));

            CraftableType type = CraftableType.fromCraftingName(typeStr);
            if (type == null) {
                result = null;
                return;
            }
            Vector2i lvl = new Vector2i(minLvl, maxLvl);

            int mat1Tier = parseMaterialTier(getTooltip(stacks, 0));
            int mat1Count = stacks.getFirst().getCount();

            int mat2Tier = parseMaterialTier(getTooltip(stacks, 9));
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
            if (type.isWeapon()) recipe.setAttackSpeed(importedAttackSpeed);
            result = recipe.craft();
        }
    }

    private static Matcher findCraftingLine(List<Text> tooltip) {
        for (Text line : tooltip) {
            Matcher matcher = craftingPattern.matcher(stripFormatting(line.getString()));
            if (matcher.find()) {
                return matcher;
            }
        }
        return null;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private static List<Text> getTooltip(DefaultedList<ItemStack> stacks, int slot) {
        return stacks.get(slot).getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
    }

    private static String getIngName(DefaultedList<ItemStack> stacks, int slot) {
        String name = getTooltip(stacks, slot).getFirst().getString();
        return name.replace("\uDAFC\uDC00", "").trim();
    }

    private static int parseMaterialTier(List<Text> tooltip) {
        String tierStr = "";
        for (Text line : tooltip) {
            Optional<String> result = line.visit((style, string) -> {
                if (style.getFont() instanceof StyleSpriteSource.Font(Identifier id)) {
                    if (Identifier.ofVanilla("banner/symbol").equals(id)) {
                        return Optional.of(string);
                    }
                }
                return Optional.empty();
            }, Style.EMPTY);

            if (result.isPresent()) {
                tierStr = result.get();
                break;
            }
        }
        return switch (tierStr) {
            case "\uE060\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE062\uDAFF\uDFF7" -> 1;
            case "\uE060\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE062\uDAFF\uDFF0" -> 2;
            case "\uE060\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE001\uDAFF\uDFFF\uE062\uDAFF\uDFE9" -> 3;
            default -> -1;
        };
    }

    public static void handleClick(double mouseX, double mouseY, int button, int action) {
        if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) return;
        if (currentWidth == 0 || currentHeight == 0) return;

        loadConfig();

        boolean inBounds =
                mouseX >= xPos && mouseX <= xPos + currentWidth &&
                        mouseY >= yPos && mouseY <= yPos + currentHeight;

        // Drag Release
        if (action == 0 && button == 0 && isDragging) {
            isDragging = false;
            saveConfig();
            return;
        }

        if (!inBounds) return;

        // Drag Start (Right Click)
        if (action == 1 && button == 0) {
            isDragging = true;
            dragOffsetX = (int) mouseX - xPos;
            dragOffsetY = (int) mouseY - yPos;
        }

    }

    public static boolean isDragging() {
        return isDragging;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (!isDragging) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        xPos = (int) mouseX - dragOffsetX;
        yPos = (int) mouseY - dragOffsetY;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        xPos = Math.clamp(xPos, 0, screenW - currentWidth);
        yPos = Math.clamp(yPos, 0, screenH - currentHeight);
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
