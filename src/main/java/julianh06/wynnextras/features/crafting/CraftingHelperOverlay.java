package julianh06.wynnextras.features.crafting;

import julianh06.wynnextras.wynncraft.state.BombState;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import julianh06.wynnextras.utils.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.VcitCompat;
import julianh06.wynnextras.features.crafting.data.recipes.AlchemismRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.CookingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.ScribingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.ChestplateRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.HelmetRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.BraceletRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.NecklaceRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.RingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.tailoring.BootsRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.tailoring.LeggingsRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing.DaggerRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing.SpearRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.BowRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.RelikRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.WandRecipes;
import julianh06.wynnextras.features.crafting.wynnbuilder.DecodedCraft;
import julianh06.wynnextras.features.crafting.wynnbuilder.WynnBuilderDecoder;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.*;

public class CraftingHelperOverlay extends WEMenuExtension {
    private static final int DEFAULT_WIDGET_WIDTH = 165;
    private static final int MIN_WIDGET_WIDTH = 110;
    private static final int RESIZE_GRIP_SIZE = 6;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms
    public static float targetOffset = 0;
    public static float actualOffset = 0;

    static HelperWidget helperWidget;

    SelectionWidget selectionWidget1;
    SelectionWidget selectionWidget2;
    SelectionWidget selectionWidget3;

    static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Queue<Integer> WB_CLICK_QUEUE = new ArrayDeque<>();
    private static long wbLastClick = 0;
    private static boolean wbClicking = false;
    private static String wbStatusMessage = "";
    private static int wbTotalClicks = 0;
    private static int wbClicksDone = 0;
    private static boolean wbIsReuse = false; // true = filling from Reuse Last, false = from Clipboard
    private static long wbFinishedTime = 0; // timestamp when queue emptied, used to delay completion check
    private static boolean lastResultSlotsEmpty = true; // tracks output slots to detect craft completion
    private static final int[] RESULT_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26};
    private static final int[] INGREDIENT_SLOTS = {2, 3, 11, 12, 20, 21};

    // "Reuse last" - stores item names from the last craft (saved when items are queued for placement)
    private static final List<String> lastMaterialNames = new ArrayList<>();  // material names (from inventory)
    private static final List<Integer> lastMaterialCounts = new ArrayList<>(); // click count per material
    private static final List<String> lastIngredientNames = new ArrayList<>(); // ingredient names (from inventory)
    private static final List<String> preparedMaterialNames = new ArrayList<>();
    private static final List<Integer> preparedMaterialCounts = new ArrayList<>();

    private record ItemRequirement(String type, String name, int amount) {}
    private record MissingRequirement(String type, String name, int amount) {}

    private static RecipeState state = RecipeState.NONE;

    private final static Map<WEProfessionType, Map<RecipeState, Float>> lastOffset = new HashMap<>();
    private final static Map<WEProfessionType, RecipeState> lastState = new HashMap<>();

    private static boolean resizingTop = false;
    private static boolean resizingBottom = false;
    private static boolean resizingRight = false;
    private static double resizeDragStartY = 0;
    private static double resizeDragStartX = 0;
    private static int resizeDragStartBlockHeight = 0;
    private static int resizeDragScreenHeight = 0;
    private static int resizeDragStartWidth = DEFAULT_WIDGET_WIDTH;
    private static int currentActualBlockH = 0;
    private static int currentBlockTop = 0;
    private static int currentScreenH = 0;
    private static int currentXStart = 0;
    private static int currentWidgetWidth = 200;
    private static int currentMaxWidgetWidth = DEFAULT_WIDGET_WIDTH;

    ProfBombWidget profSpeedBombWidget;
    ProfBombWidget profXpBombWidget;

    ActionButtonWidget loadClipboardBtn;
    ActionButtonWidget reuseLastBtn;
    ActionButtonWidget autoStartBtn;

    static ScrollBarWidget scrollBarWidget = null;

    static String statusMessage = "";

    public CraftingHelperOverlay() {
        state = RecipeState.NONE;
        helperWidget = null;
        selectionWidget1 = null;
        selectionWidget2 = null;
        selectionWidget3 = null;
        profSpeedBombWidget = null;
        profXpBombWidget = null;
        actualOffset = 0;

        loadClipboardBtn = new ActionButtonWidget(List.of(Text.of("Copy a WynnBuilder link and"), Text.of("click here to paste it!")));
        loadClipboardBtn.setOnClick(w -> loadFromWynnBuilder(MinecraftClient.getInstance().keyboard.getClipboard()));
        rootWidgets.add(loadClipboardBtn);

        reuseLastBtn = new ActionButtonWidget(List.of(Text.of("Paste the same recipe you"), Text.of("used for your last craft")));
        reuseLastBtn.setOnClick(w -> reuseLast());
        rootWidgets.add(reuseLastBtn);

        autoStartBtn = new ActionButtonWidget(List.of(Text.of("Automatically start crafting when"), Text.of("using one of the buttons above")));
        autoStartBtn.setOnClick(w -> {
            WynnExtrasConfig.INSTANCE.craftingAutoStart = !WynnExtrasConfig.INSTANCE.craftingAutoStart;
            WynnExtrasConfig.save();
        });
        rootWidgets.add(autoStartBtn);
        targetOffset = ui == null ? -10 : -10 / ui.getScaleFactorF();
        statusMessage = "";
        wbStatusMessage = "";
        WB_CLICK_QUEUE.clear();
        wbClicking = false;
        preparedMaterialNames.clear();
        preparedMaterialCounts.clear();

        WEProfessionType type = WynncraftMenuService.currentCraftingProfession().orElse(null);

        if (type == null) return;

        if (lastState.isEmpty()) return;

        state = lastState.get(type);

        Map<RecipeState, Float> offsets = lastOffset.get(type);
        if (offsets == null) return;

        Float offset = offsets.get(state);
        if (offset == null) return;

        actualOffset = offset;
        targetOffset = offset;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) return;
        if (!(MinecraftUtils.screen() instanceof HandledScreen<?> screen)) return;

        if (state == null) state = RecipeState.NONE;

        WEProfessionType type = WynncraftMenuService.currentCraftingProfession().orElse(null);
        lastState.put(type, state);

        int xStart = HandledScreenAccess.x(screen) + HandledScreenAccess.backgroundWidth(screen);
        int widgetWidth = Math.clamp(WynnExtrasConfig.INSTANCE.craftingHelperWidth, MIN_WIDGET_WIDTH, getMaxWidgetWidth(screen, xStart));
        int screenY = HandledScreenAccess.y(screen);
        int backgroundHeight = HandledScreenAccess.backgroundHeight(screen);

        boolean big = (type == null || type == WEProfessionType.ALCHEMISM || type == WEProfessionType.COOKING || type == WEProfessionType.SCRIBING);
        int selBtnHeight = big ? 0 : 20;
        int helperPadding = big ? 18 : 14;
        int maxNineSliceH = 14 * 38 + helperPadding;
        int maxBlockH = maxNineSliceH + selBtnHeight;
        int minNineSliceH = 38 + helperPadding;
        int minBlockH = minNineSliceH + selBtnHeight;

        if (resizingTop || resizingBottom) {
            // Multiply dy by 2: block is center-anchored so each edge moves at half speed;
            // doubling the height delta makes the dragged edge track the mouse 1:1.
            int dy = mouseY - (int) resizeDragStartY;
            int newBlock = resizingTop ? (resizeDragStartBlockHeight - 2 * dy) : (resizeDragStartBlockHeight + 2 * dy);
            float minPct = (float) minBlockH / resizeDragScreenHeight;
            WynnExtrasConfig.INSTANCE.craftingHelperHeightPercent = Math.clamp((float) newBlock / resizeDragScreenHeight, minPct, 1.0f);
        }
        if (resizingRight) {
            int dx = mouseX - (int) resizeDragStartX;
            widgetWidth = Math.clamp(resizeDragStartWidth + dx, MIN_WIDGET_WIDTH, getMaxWidgetWidth(screen, xStart));
            WynnExtrasConfig.INSTANCE.craftingHelperWidth = widgetWidth;
        }

        int desiredBlockH = (int) (screen.height * WynnExtrasConfig.INSTANCE.craftingHelperHeightPercent);
        int actualBlockH = Math.clamp(desiredBlockH, minBlockH, maxBlockH);
        int centerY = screenY + backgroundHeight / 2;
        int blockTop = Math.clamp(centerY - actualBlockH / 2, 0, screen.height - actualBlockH);

        int widgetHeight = actualBlockH - selBtnHeight;
        int yStart = blockTop + selBtnHeight;

        currentActualBlockH = actualBlockH;
        currentBlockTop = blockTop;
        currentScreenH = screen.height;
        currentXStart = xStart;
        currentWidgetWidth = widgetWidth;
        currentMaxWidgetWidth = getMaxWidgetWidth(screen, xStart);

        if (profSpeedBombWidget == null) profSpeedBombWidget = new ProfBombWidget("PROFESSION_SPEED");
        if (profXpBombWidget == null) profXpBombWidget = new ProfBombWidget("PROFESSION_XP");
        profSpeedBombWidget.refresh();
        profXpBombWidget.refresh();

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int menuWidth = HandledScreenAccess.backgroundWidth(screen);
        int speedWidth = textRenderer.getWidth(profSpeedBombWidget.text);
        int xpWidth = textRenderer.getWidth(profXpBombWidget.text);
        int maxBombWidth = Math.max(speedWidth, Math.max(xpWidth,
                !profXpBombWidget.isActive && !profSpeedBombWidget.isActive ? textRenderer.getWidth("There are no active profession bombs.") :
                textRenderer.getWidth("There are no active prof bombs on your world. Click below to switch worlds.")));
        boolean bombYOverlap = blockTop < screenY - 33;
        boolean bombXOverlap = xStart < screen.width / 2 + maxBombWidth / 2;
        int bombCenterX = (bombYOverlap && bombXOverlap) ? xStart - maxBombWidth / 2 - 4 : screen.width / 2;

        profSpeedBombWidget.setBounds(bombCenterX - speedWidth / 2, screenY - 43, speedWidth, 10);
        profXpBombWidget.setBounds(bombCenterX - xpWidth / 2, screenY - 57, xpWidth, 10);

        profSpeedBombWidget.draw(ctx, mouseX, mouseY, delta, ui);
        profXpBombWidget.draw(ctx, mouseX, mouseY, delta, ui);

        boolean dontShowWorldText = profSpeedBombWidget.bomb != null && profSpeedBombWidget.bomb.server().equals(BombState.currentWorld());

        if (profXpBombWidget.bomb != null && profXpBombWidget.bomb.server().equals(BombState.currentWorld()))
            dontShowWorldText = true;

        if ((profXpBombWidget.isActive || profSpeedBombWidget.isActive) && !dontShowWorldText) {
            int currentWorldTextYOffset = profXpBombWidget.isActive ? 67 : 53;
            drawCenteredWrappedUpward(ui, textRenderer, "There are no active profession bombs on your world. Click below to switch worlds.",
                    bombCenterX, screenY - currentWorldTextYOffset, maxBombWidth, CustomColor.fromHexString("FF0000"));
        }

        if (!profXpBombWidget.isActive && !profSpeedBombWidget.isActive) {
            drawCenteredWrappedUpward(ui, textRenderer, "There are no active profession bombs.",
                    bombCenterX, screenY - 40, maxBombWidth, CustomColor.fromHexString("FF0000"));
        }

        if (selectionWidget1 == null) {
            selectionWidget1 = new SelectionWidget(0);
            rootWidgets.add(selectionWidget1);
        }

        if (selectionWidget2 == null) {
            selectionWidget2 = new SelectionWidget(1);
            rootWidgets.add(selectionWidget2);
        }

        if (selectionWidget3 == null) {
            selectionWidget3 = new SelectionWidget(2);
            rootWidgets.add(selectionWidget3);
        }

        switch (type) {
            case JEWELING, WOODWORKING -> {
                setupSelectionWidget(selectionWidget1, type, 0, 3, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget2, type, 1, 3, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget3, type, 2, 3, xStart, yStart, widgetWidth);
            }
            case WEAPONSMITHING, ARMOURING, TAILORING -> {
                setupSelectionWidget(selectionWidget1, type, 0, 2, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget2, type, 1, 2, xStart, yStart, widgetWidth);
                selectionWidget3.setBounds(0, 0, 0, 0);
            }
            case null, default -> {
                selectionWidget1.setBounds(0, 0, 0, 0);
                selectionWidget2.setBounds(0, 0, 0, 0);
                selectionWidget3.setBounds(0, 0, 0, 0);
            }
        }

        ui.drawVanillaPanel(xStart + 1.7f, yStart, widgetWidth, widgetHeight, 4, 7, 7, 6, 6);

        int step = 38;
        int recipeWidgetAmount = 14;

        int contentHeight = recipeWidgetAmount * step;

        int visibleHeight = helperWidget == null ? 0 : helperWidget.getHeight();

        int maxOffset = Math.max(0, contentHeight - visibleHeight);

        if (helperWidget == null) {
            helperWidget = new HelperWidget(maxOffset);
            rootWidgets.add(helperWidget);
        }

        if (helperWidget.recipeData == null) {
            IRecipeData data = getRecipeDataInstance(type);
            if (
                    type == WEProfessionType.SCRIBING ||
                            type == WEProfessionType.ALCHEMISM ||
                            type == WEProfessionType.COOKING ||
                            state != RecipeState.NONE
            ) helperWidget.setRecipeData(data);
        }

        if (scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget(maxOffset);
        }

        helperWidget.maxOffset = maxOffset;
        scrollBarWidget.maxOffset = maxOffset;

        scrollBarWidget.setBounds(xStart + 5 + widgetWidth, yStart, 10, widgetHeight);
        if (maxOffset > 0) {
            scrollBarWidget.draw(ctx, mouseX, mouseY, delta, ui);
        }

        int scissorX1 = xStart;
        int scissorY1 = yStart + (big ? 6 : 7);
        int scissorX2 = xStart + widgetWidth;
        int scissorY2 = yStart + widgetHeight - 7;

        // Buttons (left side of crafting station, right-aligned near GUI)
        int leftX = HandledScreenAccess.x(screen);
        int wbBtnW = (leftX - 10) / 2;
        int wbBtnH = 17;
        int wbBtnX = leftX - wbBtnW - 2;
        int wbBtnY = screenY + 2;
        boolean hasLastCraft = !lastMaterialNames.isEmpty() || !lastIngredientNames.isEmpty();

        loadClipboardBtn.setBounds(wbBtnX, wbBtnY, wbBtnW, wbBtnH);
        loadClipboardBtn.isDisabled = wbClicking;
        loadClipboardBtn.isFilling = wbClicking && !wbIsReuse;
        loadClipboardBtn.fillDone = wbClicksDone;
        loadClipboardBtn.fillTotal = wbTotalClicks;
        loadClipboardBtn.label = "Load from Clipboard";

        reuseLastBtn.setBounds(wbBtnX, wbBtnY + wbBtnH + 2, wbBtnW, wbBtnH);
        reuseLastBtn.isDisabled = wbClicking || !hasLastCraft;
        reuseLastBtn.isFilling = wbClicking && wbIsReuse;
        reuseLastBtn.fillDone = wbClicksDone;
        reuseLastBtn.fillTotal = wbTotalClicks;
        reuseLastBtn.label = "Reuse Last";

        autoStartBtn.setBounds(wbBtnX, wbBtnY + 2 * (wbBtnH + 2), wbBtnW, wbBtnH);
        autoStartBtn.label = "Auto Start: " + (WynnExtrasConfig.INSTANCE.craftingAutoStart ? "§aON" : "§cOFF");

        // Status message below buttons (word-wrapped to button width)
        if (!wbStatusMessage.isEmpty()) {
            CustomColor statusColor = wbStatusMessage.startsWith("Missing") || wbStatusMessage.startsWith("Wrong") || wbStatusMessage.startsWith("Invalid") || wbStatusMessage.startsWith("Unknown") || wbStatusMessage.startsWith("Please") || wbStatusMessage.startsWith("Not") || wbStatusMessage.startsWith("Loading") || wbStatusMessage.startsWith("No craft")
                    ? CustomColor.fromHexString("FF4444") : CustomColor.fromHexString("44FF44");
            int statusRY = wbBtnY + 3 * (wbBtnH + 2) + 3;
            int statusWrapWidth = Math.max(40, wbBtnW - 18);
            String[] words = wbStatusMessage.split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = statusRY;
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textRenderer.getWidth(candidate) > statusWrapWidth && !line.isEmpty()) {
                    ui.drawText(line.toString(), wbBtnX, lineY, statusColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, 1f);
                    line = new StringBuilder(word);
                    lineY += 10;
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) {
                ui.drawText(line.toString(), wbBtnX, lineY, statusColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, 1f);
            }
        }

        // Process WynnBuilder click queue
        processWynnBuilderClicks();

        // Auto-capture craft: when any result slot gets an item, save the current materials + ingredients
        try {
            boolean hasOutput = false;
            for (int slot : RESULT_SLOTS) {
                ItemStack stack = MinecraftUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.isEmpty() && !name.contains("Crafted Item Slot")) {
                        hasOutput = true;
                        break;
                    }
                }
            }
            if (lastResultSlotsEmpty && hasOutput) {
                capturePreparedMaterials();
                captureCurrentIngredients();
            } else if (!hasOutput) {
                updatePreparedMaterialSnapshot();
            }
            lastResultSlotsEmpty = !hasOutput;
        } catch (Exception ignored) {}

        if (!statusMessage.isEmpty()) {
            int statusY = screenY + backgroundHeight + 10;
            if (textRenderer.getWidth(statusMessage) > menuWidth) {
                String[] words = statusMessage.split(" ");
                StringBuilder sb1 = new StringBuilder();
                int wi = 0;
                while (wi < words.length && textRenderer.getWidth((sb1.isEmpty() ? "" : sb1 + " ") + words[wi]) <= menuWidth) {
                    if (!sb1.isEmpty()) sb1.append(" ");
                    sb1.append(words[wi++]);
                }
                StringBuilder sb2 = new StringBuilder();
                while (wi < words.length) {
                    if (!sb2.isEmpty()) sb2.append(" ");
                    sb2.append(words[wi++]);
                }
                ui.drawCenteredText(sb1.toString(), HandledScreenAccess.x(screen) + HandledScreenAccess.backgroundWidth(screen) / 2f, statusY, CustomColor.fromHexString("FF0000"), 1f);
                if (!sb2.isEmpty()) ui.drawCenteredText(sb2.toString(), HandledScreenAccess.x(screen) + HandledScreenAccess.backgroundWidth(screen) / 2f, statusY + 10, CustomColor.fromHexString("FF0000"), 1f);
            } else {
                ui.drawCenteredText(statusMessage, xStart, statusY, CustomColor.fromHexString("FF0000"), 1f);
            }
        }

        selectionWidget1.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget2.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget3.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);

        helperWidget.setBounds(xStart + 2, yStart + (big ? 17 : 7), widgetWidth, widgetHeight + (big ? -18 : -14));
        helperWidget.scissorX1 = scissorX1;
        helperWidget.scissorY1 = scissorY1;
        helperWidget.scissorX2 = scissorX2;
        helperWidget.scissorY2 = scissorY2;

        boolean nearBottom = mouseY >= blockTop + actualBlockH - RESIZE_GRIP_SIZE && mouseY <= blockTop + actualBlockH && !resizingTop;
        if (nearBottom || resizingBottom)
            ui.drawRect(xStart, blockTop + actualBlockH - 3, widgetWidth, 3, CustomColor.fromHexString("FFFFFF").withAlpha(0.3f));

        boolean nearRight = mouseX >= xStart + widgetWidth - RESIZE_GRIP_SIZE && mouseX <= xStart + widgetWidth + 3 &&
                mouseY >= blockTop && mouseY <= blockTop + actualBlockH && !resizingBottom && !resizingTop;
        if (nearRight || resizingRight)
            ui.drawRect(xStart + widgetWidth - 3, blockTop, 3, actualBlockH, CustomColor.fromHexString("FFFFFF").withAlpha(0.3f));
    }

    private static int getMaxWidgetWidth(HandledScreen<?> screen, int xStart) {
        return Math.max(MIN_WIDGET_WIDTH, screen.width - xStart - 20);
    }

    private void setupSelectionWidget(SelectionWidget selectionWidget, WEProfessionType type, int i, int maxWidgets, int xStart, int yStart, int widgetWidth) {
        int spacing = 4;

        int totalSpacing = spacing * (maxWidgets - 1);
        int sectionWidth = (widgetWidth - totalSpacing) / maxWidgets;

        int x = xStart + 2 + i * (sectionWidth + spacing);
        int y = yStart - 20;

        selectionWidget.setBounds(x, y, sectionWidth, 18);

        selectionWidget.setText(getSelectorText(type, i));
    }

    private String getSelectorText(WEProfessionType type, int i) {
        return switch (type) {
            case ARMOURING -> switch (i) {
                case 0 -> "Helmet";
                case 1 -> "Chestplate";
                default -> null;
            };
            case WOODWORKING -> switch (i) {
                case 0 -> "Bow";
                case 1 -> "Wand";
                case 2 -> "Relik";
                default -> null;
            };
            case JEWELING -> switch (i) {
                case 0 -> "Ring";
                case 1 -> "Bracelet";
                case 2 -> "Necklace";
                default -> null;
            };
            case TAILORING -> switch (i) {
                case 0 -> "Pants";
                case 1 -> "Boots";
                default -> null;
            };
            case WEAPONSMITHING -> switch (i) {
                case 0 -> "Spear";
                case 1 -> "Dagger";
                default -> null;
            };
            case null, default -> null;
        };
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    private static void drawCenteredWrappedUpward(UIUtils ui, TextRenderer tr,
                                                   String text, float cx, float bottomY, int maxWidth, CustomColor color) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (tr.getWidth(candidate) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        for (int i = lines.size() - 1; i >= 0; i--) {
            float y = bottomY - (lines.size() - 1 - i) * 10;
            ui.drawCenteredText(lines.get(i), cx, y, color, 1f);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && currentActualBlockH > 0) {
            int bTop = currentBlockTop;
            int bBottom = bTop + currentActualBlockH;
            int right = currentXStart + currentWidgetWidth;
            if (y >= bTop && y <= bBottom && x >= right - RESIZE_GRIP_SIZE && x <= right + 3) {
                resizingRight = true;
                resizeDragStartX = x;
                resizeDragStartWidth = currentWidgetWidth;
                return true;
            }
            if (x >= currentXStart && x <= right) {
                if (y >= bBottom - RESIZE_GRIP_SIZE && y <= bBottom + 3) {
                    resizingBottom = true;
                    resizeDragStartY = y;
                    resizeDragStartBlockHeight = currentActualBlockH;
                    resizeDragScreenHeight = currentScreenH;
                    return true;
                }
            }
        }

        if(scrollBarWidget != null && scrollBarWidget.maxOffset > 0) scrollBarWidget.mouseClicked(x, y, button);
        if(profSpeedBombWidget != null) profSpeedBombWidget.mouseClicked(x, y, button);
        if(profXpBombWidget != null) profXpBombWidget.mouseClicked(x, y, button);

        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && (resizingTop || resizingBottom || resizingRight)) {
            resizingTop = false;
            resizingBottom = false;
            resizingRight = false;
            WynnExtrasConfig.INSTANCE.craftingHelperWidth = Math.clamp(WynnExtrasConfig.INSTANCE.craftingHelperWidth, MIN_WIDGET_WIDTH, currentMaxWidgetWidth);
            WynnExtrasConfig.save();
            return true;
        }
        if (scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        return super.mouseReleased(x, y, button);
    }


    private void loadFromWynnBuilder(String link) {

        // Block re-clicking while already processing
        if (wbClicking) {
            return;
        }

        if (link == null || link.isBlank()) {
            wbStatusMessage = "Clipboard is empty.";
            return;
        }

        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            wbStatusMessage = "Please paste a full WynnBuilder URL.";
            return;
        }

        if (!link.contains("#")) {
            wbStatusMessage = "No craft hash found in URL.";
            return;
        }

        String urlPart = link.substring(0, link.lastIndexOf('#')).toLowerCase();
        String host;
        try {
            host = URI.create(urlPart).getHost();
        } catch (IllegalArgumentException e) {
            wbStatusMessage = "Invalid WynnBuilder URL.";
            return;
        }
        if (host == null || !(host.contains("wynnbuilder") || host.equals("hppeng-wynn.github.io"))) {
            wbStatusMessage = "This is not a WynnBuilder URL.";
            return;
        }

        WynnDataService dataService = WynnDataService.getInstance();
        if (dataService.getState() != WynnDataService.State.READY) {
            wbStatusMessage = dataService.getStatusMessage();
            return;
        }

        if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) {
            wbStatusMessage = "Not at a crafting station.";
            return;
        }

        DecodedCraft craft = WynnBuilderDecoder.decode(link);
        if (craft == null) {
            wbStatusMessage = "Invalid WynnBuilder link.";
            return;
        }
        if (craft.attackSpeed() < 0 || craft.attackSpeed() > 2) {
            wbStatusMessage = "Unsupported WynnBuilder attack-speed encoding.";
            return;
        }

        WynnDataService.RecipeData recipeData = dataService.getRecipeByWynnBuilderId(craft.recipeId());
        if (recipeData == null) {
            wbStatusMessage = "Unknown recipe ID: " + craft.recipeId();
            return;
        }
        if (recipeData.type().isWeapon()) CraftingResultPreviewer.setImportedAttackSpeed(craft.attackSpeed());

        // Verify correct crafting station
        WEProfessionType stationProf = WynncraftMenuService.currentCraftingProfession().orElse(null);
        if (stationProf != recipeData.skill()) {
            wbStatusMessage = "Wrong station! The recipe needs " + recipeData.skill().getDisplayName() + ", you are at a " + stationProf.getDisplayName() + " station.";
            return;
        }

        List<WynnDataService.Material> materials = recipeData.materials();
        if (materials == null || materials.size() < 2) {
            wbStatusMessage = "Could not determine materials for this recipe.";
            return;
        }

        List<ItemRequirement> requirements = new ArrayList<>();
        for (int m = 0; m < 2; m++) {
            WynnDataService.Material mat = materials.get(m);
            requirements.add(new ItemRequirement("materials", normalizeMaterialName(mat.item()), mat.amount()));
        }

        List<String> ingNamesFromLink = new ArrayList<>();
        List<Integer> unknownIngredientIds = new ArrayList<>();
        for (int id : craft.ingredientIds()) {
            if (WynnBuilderDecoder.isNoIngredient(id)) {
                ingNamesFromLink.add(null);
                continue;
            }
            String ingName = dataService.getIngredientNameByWynnBuilderId(id);
            if (ingName == null) {
                ingNamesFromLink.add(null);
                unknownIngredientIds.add(id);
                continue;
            }
            ingNamesFromLink.add(ingName);
            requirements.add(new ItemRequirement("ingredients", ingName, 1));
        }
        if (!unknownIngredientIds.isEmpty()) {
            wbStatusMessage = "Unknown ingredient ID: " + unknownIngredientIds.get(0);
            return;
        }

        // Save for "Reuse Last"
        List<String> matNamesForSave = new ArrayList<>();
        List<Integer> matCountsForSave = new ArrayList<>();
        for (int m = 0; m < 2; m++) {
            matNamesForSave.add(normalizeMaterialName(materials.get(m).item()));
            matCountsForSave.add(materials.get(m).amount());
        }
        saveLastCraft(matNamesForSave, matCountsForSave, ingNamesFromLink);

        List<MissingRequirement> missingRequirements = findMissingRequirements(requirements);
        if (!missingRequirements.isEmpty()) {
            WB_CLICK_QUEUE.clear();
            wbTotalClicks = 0;
            wbClicksDone = 0;
            wbStatusMessage = formatMissingRequirements(missingRequirements);
            return;
        }

        // Clear queue
        WB_CLICK_QUEUE.clear();

        // Reset all crafting slots first (not counted in progress)
        try {
            for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
                ItemStack stack = MinecraftUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.contains("Material Slot") && !name.contains("Ingredient Slot") && !name.isEmpty()) {
                        ContainerUtils.clickOnSlot(slot, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
                    }
                }
            }
        } catch (Exception ignored) {}

        Map<Integer, Integer> queuedBySlot = new HashMap<>();
        for (ItemRequirement requirement : requirements) {
            queueInventoryItem(requirement, queuedBySlot);
        }

        wbTotalClicks = WB_CLICK_QUEUE.size();
        wbClicksDone = 0;

        wbIsReuse = false;
        wbFinishedTime = 0;
        wbClicking = true;
    }

    private static void processWynnBuilderClicks() {
        if (!wbClicking) return;

        if (WB_CLICK_QUEUE.isEmpty()) {
            // Wait 500ms after last click before checking completion
            if (wbFinishedTime == 0) {
                wbFinishedTime = System.currentTimeMillis();
                return;
            }
            if (System.currentTimeMillis() - wbFinishedTime < 500) return;

            wbClicking = false;
            wbFinishedTime = 0;
            if (wbStatusMessage.startsWith("Done!") || wbStatusMessage.startsWith("Crafting!")) return;

            // Check if slot 13 still shows "Incomplete Recipe"
            try {
                ItemStack craftSlot = MinecraftUtils.containerMenu().getSlot(13).getStack();
                String craftName = craftSlot.getCustomName() != null ? craftSlot.getCustomName().getString() : "";
                if (craftName.contains("Incomplete")) {
                    wbStatusMessage = "Missing materials or ingredients!";
                    return;
                }
            } catch (Exception ignored) {}

            wbStatusMessage = "Done!";

            // Auto Start: shift-click the craft button (slot 13) after filling
            if (WynnExtrasConfig.INSTANCE.craftingAutoStart) {
                ContainerUtils.shiftClickOnSlot(13, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
                wbStatusMessage = "Crafting!";
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - wbLastClick < 10) return; // 10ms between clicks

        Integer next = WB_CLICK_QUEUE.poll();
        if (next == null) return;

        ContainerUtils.clickOnSlot(next, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
        wbLastClick = now;
        wbClicksDone++;
    }

    private static void saveLastCraft(List<String> matNames, List<Integer> matCounts, List<String> ingNames) {
        lastMaterialNames.clear();
        lastMaterialNames.addAll(matNames);
        lastMaterialCounts.clear();
        lastMaterialCounts.addAll(matCounts);
        // Only overwrite ingredients if new list has actual entries
        if (ingNames != null && ingNames.stream().anyMatch(Objects::nonNull)) {
            lastIngredientNames.clear();
            lastIngredientNames.addAll(ingNames);
        }
    }

    private static List<MissingRequirement> findMissingRequirements(List<ItemRequirement> requirements) {
        Map<String, ItemRequirement> mergedRequirements = new LinkedHashMap<>();
        for (ItemRequirement requirement : requirements) {
            if (requirement.name() == null || requirement.name().isBlank() || requirement.amount() <= 0) continue;

            String key = requirement.type() + "\u0000" + requirement.name();
            ItemRequirement existing = mergedRequirements.get(key);
            if (existing == null) {
                mergedRequirements.put(key, requirement);
            } else {
                mergedRequirements.put(key, new ItemRequirement(existing.type(), existing.name(), existing.amount() + requirement.amount()));
            }
        }

        List<MissingRequirement> missingRequirements = new ArrayList<>();
        for (ItemRequirement requirement : mergedRequirements.values()) {
            int available = countInventoryItems(requirement.name());
            if (requirement.type().equals("materials")) {
                if (available == 0) {
                    missingRequirements.add(new MissingRequirement(requirement.type(), requirement.name(), 1));
                }
                continue;
            }
            if (available < requirement.amount()) {
                missingRequirements.add(new MissingRequirement(requirement.type(), requirement.name(), requirement.amount() - available));
            }
        }
        return missingRequirements;
    }

    private static int countInventoryItems(String itemName) {
        int count = 0;
        for (Slot slot : MinecraftUtils.containerMenu().slots) {
            try {
                if (!(slot.inventory instanceof PlayerInventory)) continue;
                if (slot.getStack().getCustomName() == null) continue;
                if (slot.getStack().getCustomName().getString().contains(itemName)) {
                    count += slot.getStack().getCount();
                }
            } catch (Exception ignored) {}
        }
        return count;
    }

    private static void queueInventoryItem(ItemRequirement requirement, Map<Integer, Integer> queuedBySlot) {
        int remaining = requirement.amount();
        for (Slot slot : MinecraftUtils.containerMenu().slots) {
            try {
                if (remaining <= 0) return;
                if (!(slot.inventory instanceof PlayerInventory)) continue;
                if (slot.getStack().getCustomName() == null) continue;
                if (!slot.getStack().getCustomName().getString().contains(requirement.name())) continue;

                if (requirement.type().equals("materials")) {
                    for (int i = 0; i < remaining; i++) {
                        WB_CLICK_QUEUE.add(slot.id);
                    }
                    return;
                }

                int alreadyQueued = queuedBySlot.getOrDefault(slot.id, 0);
                int available = slot.getStack().getCount() - alreadyQueued;
                if (available <= 0) continue;
                int clicks = Math.min(remaining, available);
                for (int i = 0; i < clicks; i++) {
                    WB_CLICK_QUEUE.add(slot.id);
                }
                queuedBySlot.put(slot.id, alreadyQueued + clicks);
                remaining -= clicks;
            } catch (Exception ignored) {}
        }
    }

    private static String formatMissingRequirements(List<MissingRequirement> missingRequirements) {
        List<String> items = new ArrayList<>();
        for (MissingRequirement requirement : missingRequirements) {
            items.add(formatMissingRequirement(requirement));
        }
        return "Missing: " + String.join(", ", items) + ".";
    }

    private static String formatMissingRequirement(MissingRequirement requirement) {
        String name = requirement.name().replaceAll("§.", "");
        if (requirement.amount() <= 1) return name;
        return requirement.amount() + "x " + name;
    }

    private static String normalizeMaterialName(String name) {
        if (name == null) return null;
        String normalized = name.startsWith("Refined ") ? name.substring("Refined ".length()) : name;
        if (normalized.endsWith(" Wood")) {
            normalized = normalized.substring(0, normalized.length() - " Wood".length()) + " Plank";
        }
        return normalized;
    }

    /**
     * Read ingredient names currently in the crafting slots and save them.
     */
    private static void captureCurrentIngredients() {
        if (MinecraftUtils.containerMenu() == null) return;
        List<String> ingNames = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : INGREDIENT_SLOTS) {
            try {
                ItemStack stack = MinecraftUtils.containerMenu().getSlot(slot).getStack();
                String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                if (!name.isEmpty() && !name.contains("Ingredient Slot")) {
                    ingNames.add(name);
                    hasAny = true;
                } else {
                    ingNames.add(null);
                }
            } catch (Exception e) {
                ingNames.add(null);
            }
        }
        if (hasAny) {
            lastIngredientNames.clear();
            lastIngredientNames.addAll(ingNames);
        }
    }

    /**
     * Read material names currently in the crafting slots and save them.
     */
    private static void captureCurrentMaterials() {
        if (MinecraftUtils.containerMenu() == null) return;
        List<String> matNames = new ArrayList<>();
        List<Integer> matCounts = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : new int[]{0, 9}) {
            try {
                ItemStack stack = MinecraftUtils.containerMenu().getSlot(slot).getStack();
                String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                if (!name.isEmpty() && !name.contains("Material Slot")) {
                    matNames.add(name);
                    matCounts.add(stack.getCount());
                    hasAny = true;
                } else {
                    matNames.add(null);
                    matCounts.add(0);
                }
            } catch (Exception e) {
                matNames.add(null);
                matCounts.add(0);
            }
        }
        if (hasAny) {
            lastMaterialNames.clear();
            lastMaterialNames.addAll(matNames);
            lastMaterialCounts.clear();
            lastMaterialCounts.addAll(matCounts);
        }
    }

    private static void updatePreparedMaterialSnapshot() {
        if (MinecraftUtils.containerMenu() == null) return;
        List<String> matNames = new ArrayList<>();
        List<Integer> matCounts = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : new int[]{0, 9}) {
            try {
                ItemStack stack = MinecraftUtils.containerMenu().getSlot(slot).getStack();
                String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                if (!name.isEmpty() && !name.contains("Material Slot")) {
                    matNames.add(name);
                    matCounts.add(stack.getCount());
                    hasAny = true;
                } else {
                    matNames.add(null);
                    matCounts.add(0);
                }
            } catch (Exception e) {
                matNames.add(null);
                matCounts.add(0);
            }
        }

        if (hasAny) {
            preparedMaterialNames.clear();
            preparedMaterialCounts.clear();
            preparedMaterialNames.addAll(matNames);
            preparedMaterialCounts.addAll(matCounts);
        }
    }

    private static void capturePreparedMaterials() {
        if (preparedMaterialNames.stream().noneMatch(Objects::nonNull)) {
            captureCurrentMaterials();
            return;
        }

        lastMaterialNames.clear();
        lastMaterialNames.addAll(preparedMaterialNames);
        lastMaterialCounts.clear();
        lastMaterialCounts.addAll(preparedMaterialCounts);
    }

    private void reuseLast() {
        if (MinecraftUtils.containerMenu() == null) return;

        if (lastMaterialNames.isEmpty() && lastIngredientNames.isEmpty()) {
            wbStatusMessage = "No previous craft to reuse.";
            return;
        }

        List<ItemRequirement> requirements = new ArrayList<>();
        for (int m = 0; m < Math.min(2, lastMaterialNames.size()); m++) {
            String matName = lastMaterialNames.get(m);
            int amount = lastMaterialCounts.get(m);
            if (matName == null || amount <= 0) continue;
            requirements.add(new ItemRequirement("materials", normalizeMaterialName(matName), amount));
        }

        for (int i = 0; i < Math.min(6, lastIngredientNames.size()); i++) {
            String ingName = lastIngredientNames.get(i);
            if (ingName == null) continue;
            requirements.add(new ItemRequirement("ingredients", ingName, 1));
        }

        List<MissingRequirement> missingRequirements = findMissingRequirements(requirements);
        if (!missingRequirements.isEmpty()) {
            WB_CLICK_QUEUE.clear();
            wbClicking = false;
            wbTotalClicks = 0;
            wbClicksDone = 0;
            wbStatusMessage = formatMissingRequirements(missingRequirements);
            return;
        }

        // Clear existing slots and queue
        WB_CLICK_QUEUE.clear();
        wbClicking = false;

        // Reset all crafting slots immediately
        for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
            ContainerUtils.clickOnSlot(slot, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
        }

        Map<Integer, Integer> queuedBySlot = new HashMap<>();
        for (ItemRequirement requirement : requirements) {
            queueInventoryItem(requirement, queuedBySlot);
        }

        wbTotalClicks = WB_CLICK_QUEUE.size();
        wbClicksDone = 0;
        wbStatusMessage = "Reusing last craft...";

        wbIsReuse = true;
        wbFinishedTime = 0;
        wbClicking = true;
    }

    private static IRecipeData getRecipeDataForType(CraftableType type) {
        return switch (type) {
            case HELMET -> HelmetRecipes.INSTANCE;
            case CHESTPLATE -> ChestplateRecipes.INSTANCE;
            case LEGGINGS -> LeggingsRecipes.INSTANCE;
            case BOOTS -> BootsRecipes.INSTANCE;
            case SPEAR -> SpearRecipes.INSTANCE;
            case DAGGER -> DaggerRecipes.INSTANCE;
            case BOW -> BowRecipes.INSTANCE;
            case WAND -> WandRecipes.INSTANCE;
            case RELIK -> RelikRecipes.INSTANCE;
            case RING -> RingRecipes.INSTANCE;
            case BRACELET -> BraceletRecipes.INSTANCE;
            case NECKLACE -> NecklaceRecipes.INSTANCE;
            case POTION -> AlchemismRecipes.INSTANCE;
            case SCROLL -> ScribingRecipes.INSTANCE;
            case FOOD -> CookingRecipes.INSTANCE;
        };
    }

    private static IRecipeData getRecipeDataInstance(WEProfessionType type) {
        if (state == null) return null;

        return switch (type) {
            case WEAPONSMITHING -> switch (state) {
                case FIRST -> SpearRecipes.INSTANCE;
                case SECOND -> DaggerRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case ARMOURING -> switch (state) {
                case FIRST -> HelmetRecipes.INSTANCE;
                case SECOND -> ChestplateRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case WOODWORKING -> switch (state) {
                case FIRST -> BowRecipes.INSTANCE;
                case SECOND -> WandRecipes.INSTANCE;
                case THIRD -> RelikRecipes.INSTANCE;
                case NONE -> null;
            };
            case JEWELING -> switch (state) {
                case FIRST -> RingRecipes.INSTANCE;
                case SECOND -> BraceletRecipes.INSTANCE;
                case THIRD -> NecklaceRecipes.INSTANCE;
                case NONE -> null;
            };
            case ALCHEMISM -> AlchemismRecipes.INSTANCE;
            case SCRIBING -> ScribingRecipes.INSTANCE;
            case COOKING -> CookingRecipes.INSTANCE;
            case TAILORING -> switch (state) {
                case FIRST -> LeggingsRecipes.INSTANCE;
                case SECOND -> BootsRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case null, default -> null;
        };
    }

    private static void drawRecipe(DrawContext ctx, int x, int y, int width, int height, int level,
                                   IRecipeData recipe, UIUtils ui) {
        if (recipe == null) return;

        List<Pair<IMaterial, Integer>> materials = recipe.getMaterials(level);

        if (materials.isEmpty() || materials.size() < 2) return;

        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("080808"));

        float separatorX = x + width * 0.8f;
        int materialTextWidth = Math.max(0, (int) (separatorX - (x + 20) - 4));

        drawMaterialIcon(ctx, ui, materials.getFirst().getFirst(), x + 3, y + 2, 14);
        String firstText = formatMaterialText(materials.getFirst().getFirst().getName(), materials.getFirst().getSecond(), materialTextWidth, 0.85f);
        ui.drawText(firstText, x + 20, y + height / 4f + 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 0.85f);

        drawMaterialIcon(ctx, ui, materials.get(1).getFirst(), x + 3, y + 16, 14);
        String secondText = formatMaterialText(materials.get(1).getFirst().getName(), materials.get(1).getSecond(), materialTextWidth, 0.85f);
        ui.drawText(secondText, x + 20, y + 3 * height / 4f - 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 0.85f);
    }

    private static String formatMaterialText(String name, int amount, int maxWidth, float textScale) {
        String suffix = " x" + amount;
        String fullText = name + suffix;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (getScaledTextWidth(textRenderer, fullText, textScale) <= maxWidth) return fullText;
        if (getScaledTextWidth(textRenderer, suffix, textScale) > maxWidth) return truncateToWidth(fullText, maxWidth, textScale);

        int nameWidth = maxWidth - getScaledTextWidth(textRenderer, suffix, textScale);
        String truncatedName = truncateToWidth(name, nameWidth, textScale);
        if (truncatedName.isEmpty()) return suffix.trim();
        return truncatedName + suffix;
    }

    private static String truncateToWidth(String text, int maxWidth, float textScale) {
        if (text == null || maxWidth <= 0) return "";
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (getScaledTextWidth(textRenderer, text, textScale) <= maxWidth) return text;

        String suffix = "..";
        if (getScaledTextWidth(textRenderer, suffix, textScale) > maxWidth) return "";

        int maxLen = text.length();
        while (maxLen > suffix.length()) {
            String truncated = text.substring(0, maxLen - suffix.length()) + suffix;
            if (getScaledTextWidth(textRenderer, truncated, textScale) <= maxWidth) return truncated;
            maxLen--;
        }
        return suffix;
    }

    private static int getScaledTextWidth(TextRenderer textRenderer, String text, float textScale) {
        return (int) Math.ceil(textRenderer.getWidth(text) * textScale);
    }

    private static void drawMaterialIcon(DrawContext ctx, UIUtils ui, IMaterial material, float x, float y, float size) {
        ItemStack stack = buildMaterialStack(material);
        if (shouldUseVcit(stack)) {
            drawItemScaled(ctx, ui, stack, size);
            return;
        }
        ui.drawImage(material.getTexture(), x, y, size, size);
    }

    private static ItemStack buildMaterialStack(IMaterial material) {
        ItemStack inventoryMatch = findInventoryMaterial(material);
        if (inventoryMatch != null && !inventoryMatch.isEmpty()) {
            return inventoryMatch;
        }
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Refined " + material.getName() + " "));
        return stack;
    }

    private static ItemStack findInventoryMaterial(IMaterial material) {
        if (MinecraftUtils.containerMenu() == null) {
            return null;
        }
        List<Slot> slots = MinecraftUtils.containerMenu().slots;
        for (Slot slot : slots) {
            try {
                if (!(slot.inventory instanceof PlayerInventory)) {
                    continue;
                }
                ItemStack stack = slot.getStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Text name = stack.getCustomName();
                if (name != null && name.getString().contains(material.getName())) {
                    return stack;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean shouldUseVcit(ItemStack stack) {
        if (!WynnExtrasConfig.INSTANCE.craftingDynamicTextures) {
            return false;
        }
        return VcitCompat.hasModel(stack);
    }

    private static void drawItemScaled(DrawContext ctx, UIUtils ui, ItemStack stack, float size) {
        float scale = (float) ui.sw(size) / 16.0f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().popMatrix();
    }


    private enum RecipeState {
        NONE,
        FIRST,
        SECOND,
        THIRD
    }

    private static class HelperWidget extends Widget {
        IRecipeData recipeData;
        List<RecipeWidget> recipeWidgets = new ArrayList<>();
        private static final Queue<Integer> CLICK_QUEUE = new ArrayDeque<>();
        public int maxOffset;
        private static long lastClick = 0;
        private boolean reverseOrder = WynnExtrasConfig.INSTANCE.craftingHelperReverseOrder;
        int scissorX1, scissorY1, scissorX2, scissorY2;

        public HelperWidget(int maxOffset) {
            super(0, 0, 0, 0);
            this.maxOffset = maxOffset;
            recipeData = null;

            if (MinecraftClient.getInstance().currentScreen == null) return;
            ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                    screen,
                    mX,
                    mY,
                    horizontalAmount,
                    verticalAmount,
                    consumed
            ) -> {
                long now = System.currentTimeMillis();
                if (now - lastScrollTime < scrollCooldown) {
                    return true;
                }
                lastScrollTime = now;

                if (hovered) {
                    if (verticalAmount > 0) {
                        targetOffset -= 38f;
                    } else /*if(canScrollFurther)*/ {
                        targetOffset += 38f;
                    }
                }
                return true;
            });
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

            WEProfessionType type = WynncraftMenuService.currentCraftingProfession().orElse(null);
            if (type == null) return;

            if (state == RecipeState.NONE && type != WEProfessionType.ALCHEMISM && type != WEProfessionType.COOKING && type != WEProfessionType.SCRIBING) {
                ui.drawCenteredText("Select the type", x + width / 2f, y + height / 2f - 10, CustomColor.fromHexString("FF0000"), 1.5f);
                ui.drawCenteredText("you want to craft.", x + width / 2f, y + height / 2f + 10, CustomColor.fromHexString("FF0000"), 1.5f);
            }

            if (recipeData == null) return;

            float snapValue = 0.5f;

            int widgetHeight = 34;
            int widgetAmount = 14;

            targetOffset = ui == null ? 0 : Math.clamp(targetOffset, 0, maxOffset);

            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if (Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle)
                actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            Map<RecipeState, Float> map = lastOffset.get(type) == null ? new HashMap<>() : lastOffset.get(type);
            map.put(state, actualOffset);
            lastOffset.put(type, map);

            if (reverseOrder != WynnExtrasConfig.INSTANCE.craftingHelperReverseOrder) {
                reverseOrder = WynnExtrasConfig.INSTANCE.craftingHelperReverseOrder;
                recipeWidgets.clear();
                children.clear();
                targetOffset = 0;
                actualOffset = 0;
            }

            if (recipeWidgets.isEmpty()) {
                int[] levelOrder = reverseOrder
                        ? new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 105, 110, 115}
                        : new int[]{115, 110, 105, 100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0};
                for (int i = 0; i < widgetAmount; i++) {
                    int level = levelOrder[i];

                    RecipeWidget recipeWidget = new RecipeWidget(recipeData, i, level);

                    recipeWidgets.add(recipeWidget);
                    addChild(recipeWidget);
                }
            }

            for (int i = 0; i < widgetAmount; i++) {
                int baseY = y + 3 + 38 * i;
                int drawY = baseY - (int) actualOffset;

                recipeWidgets.get(i).setBounds(
                        x + 12,
                        drawY,
                        width - 24,
                        widgetHeight
                );
            }
        }

        @Override
        protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ctx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (contains((int) mx, (int) my) && recipeData != null && !recipeWidgets.isEmpty()) {
                resetMaterialSlots();
            }

            return super.mouseClicked(mx, my, button);
        }

        public void setRecipeData(IRecipeData recipeData) {
            boolean hadRecipe = this.recipeData != null;
            this.recipeData = recipeData;
            recipeWidgets.clear();
            children.clear();
            if (hadRecipe) resetMaterialSlots();
        }

        private static void resetMaterialSlots() {
            ContainerUtils.clickOnSlot(0, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
            ContainerUtils.clickOnSlot(9, MinecraftUtils.containerMenu().syncId, 0, MinecraftUtils.containerMenu().getStacks());
            CLICK_QUEUE.clear();
        }

        private static class RecipeWidget extends Widget {
            final IRecipeData recipeData;
            final int index;
            final int level;
            boolean isClicking;

            public RecipeWidget(IRecipeData recipeData, int index, int level) {
                super(0, 0, 0, 0);
                this.recipeData = recipeData;
                this.index = index;
                this.level = level;
                isClicking = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHexString("FF0000") : CustomColor.fromHexString("FFFFFF"));
                ui.drawButton(x, y, width, height, hovered && helperWidget.hovered);
                drawRecipe(ctx, x, y, width, height, level, recipeData, ui);
                ui.drawLine(x + width * 0.8f, y + 2, x + width * 0.8f, y + height - 3, 1f, UIUtils.getVanillaSeparatorColor(hovered && helperWidget.hovered));
                if (level < 100) {
                    ui.drawCenteredText(String.valueOf(Math.max(1, level)), x + width * 0.9f, y + height / 4f + 1, 0.85f);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f, 0.85f);
                    ui.drawCenteredText(String.valueOf(level + 9), x + width * 0.9f, y + 3 * height / 4f - 1, 0.85f);
                } else {
                    ui.drawCenteredText(String.valueOf(level), x + width * 0.9f, y + height / 4f + 1, 0.85f);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f, 0.85f);
                    ui.drawCenteredText(String.valueOf(level + 4), x + width * 0.9f, y + 3 * height / 4f - 1, 0.85f);
                }

                if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION)) return;

                checkClick();
            }

            @Override
            protected boolean onClick(int button) {
                if (!helperWidget.hovered) return false;

                if (!WynncraftMenuService.isCurrent(MenuType.CRAFTING_STATION))
                    return false;

                statusMessage = "";

                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

                List<Pair<IMaterial, Integer>> materials = recipeData.getMaterials(this.level);

                if (materials.isEmpty() || materials.size() < 2) return true;

                clickMaterial(materials.getFirst());
                clickMaterial(materials.get(1));

                // Save for "Reuse Last" (recipe clicks only place materials, no ingredients)
                List<String> matNames = new ArrayList<>();
                List<Integer> matCounts = new ArrayList<>();
                for (Pair<IMaterial, Integer> mat : materials) {
                    matNames.add(mat.getFirst().getName());
                    matCounts.add(mat.getSecond());
                }
                saveLastCraft(matNames, matCounts, new ArrayList<>());

                return true;
            }

            private void clickMaterial(Pair<IMaterial, Integer> material) {
                int materialAmount = material.getSecond();

                List<Slot> slots = MinecraftUtils.containerMenu().slots;
                int available = 0;

                boolean canClick = false;
                for (Slot slot : slots) {
                    try {
                        if (!(slot.inventory instanceof PlayerInventory)) continue;
                        if(slot.getStack().getCustomName() == null) continue;
                        if (slot.getStack().getCustomName().getString().contains(material.getFirst().getName())) {
                            canClick = true;
                            for (int i = 0; i < materialAmount; i++) {
                                CLICK_QUEUE.add(slot.id);
                            }
                            break;
                        }

                        if (available >= materialAmount) break;
                    } catch (Exception ignored) {
                    }
                }

                if (!canClick) {
                    statusMessage = "You don't have the required materials to craft this.";
                }
            }

            private void checkClick() {
                if(MinecraftUtils.containerMenu().getSlot(0) == null) return;
                if(MinecraftUtils.containerMenu().getSlot(9) == null) return;

                ItemStack stackSlot0 = MinecraftUtils.containerMenu().getSlot(0).getStack();
                ItemStack stackSlot9 = MinecraftUtils.containerMenu().getSlot(9).getStack();

                if (stackSlot0.getCustomName() == null) return;
                if (stackSlot9.getCustomName() == null) return;

                if (stackSlot0.getCustomName().getString() == null || stackSlot9.getCustomName().getString() == null) return;

                if ((!stackSlot0.getCustomName().getString().contains("Material Slot") || !stackSlot9.getCustomName().getString().contains("Material Slot")) && !isClicking)
                    return;

                isClicking = true;
                if (!CLICK_QUEUE.isEmpty() && lastClick < System.currentTimeMillis() - 1) {
                    Integer next = CLICK_QUEUE.poll();
                    if (next == null) return;

                    ContainerUtils.clickOnSlot(
                            next,
                            MinecraftUtils.containerMenu().syncId,
                            0,
                            MinecraftUtils.containerMenu().getStacks()
                    );

                    lastClick = System.currentTimeMillis();
                } else if (CLICK_QUEUE.isEmpty()) isClicking = false;
            }
        }
    }

    private static class SelectionWidget extends Widget {
        final int index;

        String text;

        int scissorX1, scissorY1, scissorX2, scissorY2;

        public SelectionWidget(int index) {
            super(0, 0, 0, 0);
            this.index = index;
        }

        public void setScissorBounds(int scissorX1, int scissorY1, int scissorX2, int scissorY2) {
            this.scissorX1 = scissorX1;
            this.scissorX2 = scissorX2;
            this.scissorY1 = scissorY1;
            this.scissorY2 = scissorY2;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (state == null) return;
            ui.drawButton(x, y - 2, width + 2, height + 3, hovered);
            if (index == state.ordinal() - 1)
                ui.drawRectBorders(x + 2, y - 1, x + width, y + height - 1, CustomColor.fromHexString("FFFF00"));
            ui.drawCenteredText(truncateToWidth(text, Math.max(0, width - 6), 1f), x + width / 2f, y + height / 2f, 1f);
        }

        @Override
        protected boolean onClick(int button) {
            RecipeState clickedState = switch (index) {
                case 0 -> RecipeState.FIRST;
                case 1 -> RecipeState.SECOND;
                case 2 -> RecipeState.THIRD;
                default -> RecipeState.NONE;
            };

            if (state == clickedState) return true;

            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            state = clickedState;

            helperWidget.recipeData = null;

            WEProfessionType type = WynncraftMenuService.currentCraftingProfession().orElse(null);
            if (type == null) return true;

            targetOffset = 0;

            if (type == null) return true;

            Map<RecipeState, Float> offsets = lastOffset.get(type);
            if (offsets == null) return true;

            Float offset = offsets.get(state);
            if (offset == null) return true;

            targetOffset = offset;

            return true;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private static class ScrollBarWidget extends Widget {
        ScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseY = 0;
        public int maxOffset;

        public ScrollBarWidget(int maxOffset) {
            super(0, 0, 0, 0);
            this.scrollBarButtonWidget = new ScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
            this.maxOffset = maxOffset;
        }

        private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
            float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.clamp(relativeY, -1.15f, scrollAreaHeight);

            float scrollPercent = relativeY / scrollAreaHeight;

            targetOffset = scrollPercent * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderBackground(x, y, width, height);

            int buttonHeight = 17;
            int scrollAreaHeight = height - buttonHeight;

            if (scrollBarButtonWidget.isHeld) {
                setOffset(mouseY, maxOffset, scrollAreaHeight);
                actualOffset = targetOffset;
            }

            float percent = maxOffset == 0 ? 0 : actualOffset / maxOffset;
            percent = Math.clamp(percent, 0f, 1f);

            int yPos = y + (int) (scrollAreaHeight * percent);

            scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            int buttonHeight = 17;
            int scrollAreaHeight = height - buttonHeight;

            setOffset(currentMouseY, maxOffset, scrollAreaHeight);

            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarButtonWidget.mouseReleased(mx, my, button);
            return true;
        }

        private static class ScrollBarButtonWidget extends Widget {
            public boolean isHeld;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHeld = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, hovered || isHeld);
            }

            @Override
            protected boolean onClick(int button) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                isHeld = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                isHeld = false;
                return true;
            }
        }
    }

    private static class ActionButtonWidget extends Widget {
        String label = "";
        boolean isDisabled = false;
        boolean isFilling = false;
        int fillDone = 0, fillTotal = 0;
        final List<Text> tooltipText;

        ActionButtonWidget(List<Text> tooltipText) {
            super(0, 0, 0, 0);
            this.tooltipText = tooltipText;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered && !isDisabled && !isFilling);
            if (isFilling) {
                int progress = fillTotal > 0 ? fillDone * width / fillTotal : 0;
                ui.drawRect(x, y, progress, height, CustomColor.fromHexString("2a7a2a").withAlpha(0.5f));
                ui.drawCenteredText("Filling... " + fillDone + "/" + fillTotal, x + width / 2f, y + height / 2f, 1f);
            } else {
                CustomColor color = isDisabled ? CustomColor.fromHexString("666666") : CustomColor.fromHexString("FFFFFF");
                ui.drawCenteredText(label, x + width / 2f, y + height / 2f, color, 1f);
            }

            if (hovered && tooltipText != null && !tooltipText.isEmpty()) {
                ctx.drawTooltip(mc.textRenderer, tooltipText, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled || !contains((int) mx, (int) my)) return false;
            if (isDisabled || isFilling) return true;
            setFocused(true);
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if (onClickCallback != null) onClickCallback.accept(this);
            return true;
        }
    }

    private static class ProfBombWidget extends Widget {
        final String type;
        public BombState.Bomb bomb;
        public boolean isActive;
        public String text = "";

        public ProfBombWidget(String type) {
            super(0, 0, 0, 0);
            this.type = type;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (isActive) ui.drawCenteredText(text, x + width / 2f, y + height / 2f, 1f);
        }

        @Override
        protected void updateValues() {
            refresh();
        }

        public void refresh() {
            try {
                if (bomb != null) {
                    if (bomb.server().equals(BombState.currentWorld())) hovered = false;
                }

                String currentWorld = BombState.currentWorld();
                isActive = false;
                bomb = null;
                text = "";

                for (BombState.Bomb bomb : BombState.bombs()) {
                    if (bomb.type().equals(type)) {
                        isActive = true;
                        if (bomb.server().equals(currentWorld)) {
                            this.bomb = bomb;
                            break;
                        }
                        if (this.bomb == null || bomb.remainingLong() > this.bomb.remainingLong()) {
                            this.bomb = bomb;
                        }
                    }
                }

                if (isActive) {
                    String worldColor = bomb.server().equals(currentWorld) ? "§a" : "§f";
                    worldColor += (hovered ? "§n" : "");
                    String bombType = "?";
                    if (type.equals("PROFESSION_SPEED")) bombType = "Speed";
                    if (type.equals("PROFESSION_XP")) bombType = "XP";

                    text = "§6" + (hovered ? "§n" : "") + "Profession " + bombType + " §7" + (hovered ? "§n" : "") + "on " + worldColor + bomb.server() + " §6" + (hovered ? "§n" : "") + "(" + bomb.remainingString() + ")";

                    if (bomb.remainingLong() < 30000) {
                        long seconds = System.currentTimeMillis() / 1000;

                        String color = (seconds % 2 == 0) ? "§c" : "§4";
                        color += (hovered ? "§n" : "");

                        text = color + "Profession " + bombType + " on "
                                + bomb.server()
                                + " (" + bomb.remainingString() + ") (EXPIRING SOON)";
                    }
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (bomb == null) return true;
            if (bomb.server().equals(BombState.currentWorld())) return true;

            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                MinecraftUtils.setScreen(null);
                client.player.networkHandler.sendChatCommand("switch " + bomb.server());
            }

            return true;
        }
    }
}
