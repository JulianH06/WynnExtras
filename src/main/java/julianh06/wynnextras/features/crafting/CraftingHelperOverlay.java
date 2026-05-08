package julianh06.wynnextras.features.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Time;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.VcitCompat;
import julianh06.wynnextras.features.crafting.data.recipes.AlchemismRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.CookingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.RecipeLoader;
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
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.Pair;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CraftingHelperOverlay extends WEMenuExtension {
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms
    public static float targetOffset = 0;
    public static float actualOffset = 0;

    static HelperWidget helperWidget;

    SelectionWidget selectionWidget1;
    SelectionWidget selectionWidget2;
    SelectionWidget selectionWidget3;


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

    private static final String MAIN_ING_MAP_URL = "https://raw.githubusercontent.com/hppeng-wynn/hppeng-wynn.github.io/HEAD/py_script/ing_map.json";
    private static final String BETA_ING_MAP_URL = "https://raw.githubusercontent.com/wynnbuilder-beta/wynnbuilder-beta.github.io/master/py_script/ing_map.json";
    private static final HttpClient ING_HTTP = HttpClient.newHttpClient();
    private static volatile Map<Integer, String> mainIngMap = null;
    private static volatile Map<Integer, String> betaIngMap = null;
    private static volatile boolean ingMapsLoading = false;

    private static void ensureIngMapsLoaded() {
        if ((mainIngMap != null && betaIngMap != null) || ingMapsLoading) return;
        ingMapsLoading = true;
        CompletableFuture.runAsync(() -> {
            mainIngMap = fetchIngMap(MAIN_ING_MAP_URL);
            betaIngMap = fetchIngMap(BETA_ING_MAP_URL);
            ingMapsLoading = false;
        });
    }

    private static Map<Integer, String> fetchIngMap(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> resp = ING_HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
            Map<Integer, String> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getValue().getAsInt(), entry.getKey());
            }
            return map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static RecipeState state = RecipeState.NONE;

    private final static Map<ProfessionType, Map<RecipeState, Float>> lastOffset = new HashMap<>();
    private final static Map<ProfessionType, RecipeState> lastState = new HashMap<>();

    private static boolean resizingTop = false;
    private static boolean resizingBottom = false;
    private static double resizeDragStartY = 0;
    private static int resizeDragStartBlockHeight = 0;
    private static int resizeDragScreenHeight = 0;
    private static int currentActualBlockH = 0;
    private static int currentBlockTop = 0;
    private static int currentScreenH = 0;
    private static int currentXStart = 0;
    private static int currentWidgetWidth = 200;

    ProfBombWidget profSpeedBombWidget;
    ProfBombWidget profXpBombWidget;

    ActionButtonWidget loadClipboardBtn;
    ActionButtonWidget reuseLastBtn;
    ActionButtonWidget autoStartBtn;

    static ScrollBarWidget scrollBarWidget = null;

    static String statusMessage = "";

    public CraftingHelperOverlay() {
        ensureIngMapsLoaded();
        state = RecipeState.NONE;
        helperWidget = null;
        selectionWidget1 = null;
        selectionWidget2 = null;
        selectionWidget3 = null;
        profSpeedBombWidget = null;
        profXpBombWidget = null;
        actualOffset = 0;

        loadClipboardBtn = new ActionButtonWidget();
        loadClipboardBtn.setOnClick(w -> loadFromWynnBuilder(MinecraftClient.getInstance().keyboard.getClipboard()));
        rootWidgets.add(loadClipboardBtn);

        reuseLastBtn = new ActionButtonWidget();
        reuseLastBtn.setOnClick(w -> reuseLast());
        rootWidgets.add(reuseLastBtn);

        autoStartBtn = new ActionButtonWidget();
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

        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        ProfessionType type = container.getProfessionType();

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
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        if (state == null) state = RecipeState.NONE;

        ProfessionType type = container.getProfessionType();
        lastState.put(type, state);

        int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
        int widgetWidth = 165;
        int screenY = ((HandledScreenAccessor) screen).getY();
        int backgroundHeight = ((HandledScreenAccessor) screen).getBackgroundHeight();

        boolean big = (type == null || type == ProfessionType.ALCHEMISM || type == ProfessionType.COOKING || type == ProfessionType.SCRIBING);
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

        if (profSpeedBombWidget == null) profSpeedBombWidget = new ProfBombWidget(BombType.PROFESSION_SPEED);
        if (profXpBombWidget == null) profXpBombWidget = new ProfBombWidget(BombType.PROFESSION_XP);

        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int menuWidth = ((HandledScreenAccessor) screen).getBackgroundWidth();
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

        boolean dontShowWorldText = profSpeedBombWidget.bomb != null && profSpeedBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName());

        if (profXpBombWidget.bomb != null && profXpBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName()))
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
                    type == ProfessionType.SCRIBING ||
                            type == ProfessionType.ALCHEMISM ||
                            type == ProfessionType.COOKING ||
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
        int leftX = ((HandledScreenAccessor) screen).getX();
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
            String[] words = wbStatusMessage.split(" ");
            StringBuilder line = new StringBuilder();
            int lineY = statusRY;
            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (textRenderer.getWidth(candidate) > wbBtnW && !line.isEmpty()) {
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
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.isEmpty() && !name.contains("Crafted Item Slot")) {
                        hasOutput = true;
                        break;
                    }
                }
            }
            if (lastResultSlotsEmpty && hasOutput) {
                captureCurrentMaterials();
                captureCurrentIngredients();
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
                ui.drawCenteredText(sb1.toString(), ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth() / 2f, statusY, CustomColor.fromHexString("FF0000"), 1f);
                if (!sb2.isEmpty()) ui.drawCenteredText(sb2.toString(), ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth() / 2f, statusY + 10, CustomColor.fromHexString("FF0000"), 1f);
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

        boolean nearBottom = mouseY >= blockTop + actualBlockH - 6 && mouseY <= blockTop + actualBlockH && !resizingTop;
        if (nearBottom || resizingBottom)
            ui.drawRect(xStart, blockTop + actualBlockH - 3, widgetWidth, 3, CustomColor.fromHexString("FFFFFF").withAlpha(0.3f));
    }

    private void setupSelectionWidget(SelectionWidget selectionWidget, ProfessionType type, int i, int maxWidgets, int xStart, int yStart, int widgetWidth) {
        int spacing = 4;

        int totalSpacing = spacing * (maxWidgets - 1);
        int sectionWidth = (widgetWidth - totalSpacing) / maxWidgets;

        int x = xStart + 2 + i * (sectionWidth + spacing);
        int y = yStart - 20;

        selectionWidget.setBounds(x, y, sectionWidth, 18);

        selectionWidget.setText(getSelectorText(type, i));
    }

    private String getSelectorText(ProfessionType type, int i) {
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
            if (x >= currentXStart && x <= currentXStart + currentWidgetWidth) {
                if (y >= bBottom - 6 && y <= bBottom + 3) {
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
        if (button == 0 && (resizingTop || resizingBottom)) {
            resizingTop = false;
            resizingBottom = false;
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

        // Detect beta WynnBuilder from the URL domain (before the # fragment).
        String urlPart = link.substring(0, link.lastIndexOf('#')).toLowerCase();
        boolean isBetaLink = urlPart.contains("wynnbuilder-beta") || urlPart.contains("beta.wynnbuilder");

        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) {
            wbStatusMessage = "Not at a crafting station.";
            return;
        }

        DecodedCraft craft = WynnBuilderDecoder.decode(link);
        if (craft == null) {
            wbStatusMessage = "Invalid WynnBuilder link.";
            return;
        }

        RecipeLoader.RecipeData recipeData = RecipeLoader.getRecipeById(craft.recipeId());
        if (recipeData == null) {
            wbStatusMessage = "Unknown recipe ID: " + craft.recipeId();
            return;
        }

        // Verify correct crafting station
        ProfessionType stationProf = container.getProfessionType();
        if (stationProf != recipeData.skill()) {
            wbStatusMessage = "Wrong station! The recipe needs " + recipeData.skill().getDisplayName() + ", you are at a " + stationProf.getDisplayName() + " station.";
            return;
        }

        // Get material data for this recipe
        IRecipeData materialData = getRecipeDataForType(recipeData.type());
        if (materialData == null) {
            wbStatusMessage = "Could not find material data for " + recipeData.type();
            return;
        }

        List<Pair<IMaterial, Integer>> materials = materialData.getMaterials(recipeData.lvl().x);
        if (materials == null || materials.size() < 2) {
            wbStatusMessage = "Could not determine materials for this recipe.";
            return;
        }

        // Clear queue
        WB_CLICK_QUEUE.clear();

        // Reset all crafting slots first (not counted in progress)
        try {
            for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.contains("Material Slot") && !name.contains("Ingredient Slot") && !name.isEmpty()) {
                        ContainerUtils.clickOnSlot(slot, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                    }
                }
            }
        } catch (Exception ignored) {}

        // Queue material clicks - match by base material name (ignore stars/tiers)
        for (int m = 0; m < 2; m++) {
            Pair<IMaterial, Integer> mat = materials.get(m);
            int amount = mat.getSecond();
            String matName = mat.getFirst().getName();
            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if (slot.getStack().getCustomName() == null) continue;
                    String slotName = slot.getStack().getCustomName().getString();
                    if (slotName.contains(matName)) {
                        for (int i = 0; i < amount; i++) {
                            WB_CLICK_QUEUE.add(slot.id);
                        }
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Queue ingredient clicks using the WynnBuilder ing_map (ID → ingredient name).
        List<String> ingNamesFromLink = new ArrayList<>();
        Map<Integer, String> ingMap = isBetaLink ? betaIngMap : mainIngMap;
        if (ingMap == null) {
            wbStatusMessage = "Loading ingredient data, try again shortly.";
            return;
        }
        for (int id : craft.ingredientIds()) {
            if (WynnBuilderDecoder.isNoIngredient(id)) {
                ingNamesFromLink.add(null);
                continue;
            }
            String ingName = ingMap.get(id);
            if (ingName == null) {
                ingNamesFromLink.add(null);
                continue;
            }
            ingNamesFromLink.add(ingName);
            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if (slot.getStack().getCustomName() == null) continue;
                    String slotName = slot.getStack().getCustomName().getString();
                    if (slotName.contains(ingName)) {
                        WB_CLICK_QUEUE.add(slot.id);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        wbTotalClicks = WB_CLICK_QUEUE.size();
        wbClicksDone = 0;

        // Save for "Reuse Last"
        List<String> matNamesForSave = new ArrayList<>();
        List<Integer> matCountsForSave = new ArrayList<>();
        for (int m = 0; m < 2; m++) {
            matNamesForSave.add(materials.get(m).getFirst().getName());
            matCountsForSave.add(materials.get(m).getSecond());
        }
        saveLastCraft(matNamesForSave, matCountsForSave, ingNamesFromLink);

        if (!isBetaLink) {
            wbStatusMessage = recipeData.type().getDisplayName() + " " + recipeData.lvl().x + "-" + recipeData.lvl().y;
        }

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
                ItemStack craftSlot = McUtils.containerMenu().getSlot(13).getStack();
                String craftName = craftSlot.getCustomName() != null ? craftSlot.getCustomName().getString() : "";
                if (craftName.contains("Incomplete")) {
                    wbStatusMessage = "Missing materials or ingredients!";
                    return;
                }
            } catch (Exception ignored) {}

            wbStatusMessage = "Done!";

            // Auto Start: shift-click the craft button (slot 13) after filling
            if (WynnExtrasConfig.INSTANCE.craftingAutoStart) {
                ContainerUtils.shiftClickOnSlot(13, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                wbStatusMessage = "Crafting!";
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - wbLastClick < 10) return; // 10ms between clicks

        Integer next = WB_CLICK_QUEUE.poll();
        if (next == null) return;

        ContainerUtils.clickOnSlot(next, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
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

    /**
     * Read ingredient names currently in the crafting slots and save them.
     */
    private static void captureCurrentIngredients() {
        if (McUtils.containerMenu() == null) return;
        List<String> ingNames = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : INGREDIENT_SLOTS) {
            try {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
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
        if (McUtils.containerMenu() == null) return;
        List<String> matNames = new ArrayList<>();
        List<Integer> matCounts = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : new int[]{0, 9}) {
            try {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
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

    private void reuseLast() {
        if (McUtils.containerMenu() == null) return;

        if (lastMaterialNames.isEmpty() && lastIngredientNames.isEmpty()) {
            wbStatusMessage = "No previous craft to reuse.";
            return;
        }

        // Clear existing slots and queue
        WB_CLICK_QUEUE.clear();
        wbClicking = false;

        // Reset all crafting slots immediately
        for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
            ContainerUtils.clickOnSlot(slot, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
        }

        // Queue material clicks
        for (int m = 0; m < Math.min(2, lastMaterialNames.size()); m++) {
            String matName = lastMaterialNames.get(m);
            int amount = lastMaterialCounts.get(m);
            if (matName == null || amount <= 0) continue;

            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if(slot.getStack().getCustomName() == null) continue;
                    if (slot.getStack().getCustomName().getString().contains(matName)) {
                        for (int i = 0; i < amount; i++) {
                            WB_CLICK_QUEUE.add(slot.id);
                        }
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Queue ingredient clicks
        for (int i = 0; i < Math.min(6, lastIngredientNames.size()); i++) {
            String ingName = lastIngredientNames.get(i);
            if (ingName == null) continue;

            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if (slot.getStack().getCustomName() == null) continue;
                    String slotName = slot.getStack().getCustomName().getString();
                    if (slotName.contains(ingName)) {
                        WB_CLICK_QUEUE.add(slot.id);
                        break;
                    }
                } catch (Exception ignored) {}
            }
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

    private static IRecipeData getRecipeDataInstance(ProfessionType type) {
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

    private static void drawRecipe(DrawContext ctx, int x, int y, int height, int level,
                                   IRecipeData recipe, UIUtils ui) {
        if (recipe == null) return;

        List<Pair<IMaterial, Integer>> materials = recipe.getMaterials(level);

        if (materials.isEmpty() || materials.size() < 2) return;

        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("080808"));

        drawMaterialIcon(ctx, ui, materials.getFirst().getFirst(), x + 3, y + 2, 14);
        ui.drawText(materials.getFirst().getFirst().getName() + " " + materials.getFirst().getSecond(), x + 20, y + height / 4f + 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 0.85f);

        drawMaterialIcon(ctx, ui, materials.get(1).getFirst(), x + 3, y + 16, 14);
        ui.drawText(materials.get(1).getFirst().getName() + " " + materials.get(1).getSecond(), x + 20, y + 3 * height / 4f - 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 0.85f);
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
        if (McUtils.containerMenu() == null) {
            return null;
        }
        List<Slot> slots = McUtils.containerMenu().slots;
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

            if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
            ProfessionType type = container.getProfessionType();

            if (state == RecipeState.NONE && type != ProfessionType.ALCHEMISM && type != ProfessionType.COOKING && type != ProfessionType.SCRIBING) {
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

            if (recipeWidgets.isEmpty()) {
                int[] levelOrder = {115, 110, 105, 100, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0};
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
            ContainerUtils.clickOnSlot(0, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
            ContainerUtils.clickOnSlot(9, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
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
                drawRecipe(ctx, x, y, height, level, recipeData, ui);
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

                if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;

                checkClick();
            }

            @Override
            protected boolean onClick(int button) {
                if (!helperWidget.hovered) return false;

                if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer))
                    return false;

                statusMessage = "";

                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

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

                List<Slot> slots = McUtils.containerMenu().slots;
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
                if(McUtils.containerMenu().getSlot(0) == null) return;
                if(McUtils.containerMenu().getSlot(9) == null) return;

                ItemStack stackSlot0 = McUtils.containerMenu().getSlot(0).getStack();
                ItemStack stackSlot9 = McUtils.containerMenu().getSlot(9).getStack();

                if (stackSlot0.getCustomName() == null) return;
                if (stackSlot9.getCustomName() == null) return;

                if (stackSlot0.getCustomName().getString() == null || stackSlot9.getCustomName().getString() == null) return;

                if ((!stackSlot0.getCustomName().getString().contains("Material Slot") || !stackSlot9.getCustomName().getString().contains("Material Slot")) && !isClicking)
                    return;

                isClicking = true;
                if (!CLICK_QUEUE.isEmpty() && lastClick < Time.now().timestamp() - 1) {
                    Integer next = CLICK_QUEUE.poll();
                    if (next == null) return;

                    ContainerUtils.clickOnSlot(
                            next,
                            McUtils.containerMenu().syncId,
                            0,
                            McUtils.containerMenu().getStacks()
                    );

                    lastClick = Time.now().timestamp();
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
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, 1f);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            switch (index) {
                case 0 -> {
                    if (state != RecipeState.FIRST) state = RecipeState.FIRST;
                    else state = RecipeState.NONE;
                }
                case 1 -> {
                    if (state != RecipeState.SECOND) state = RecipeState.SECOND;
                    else state = RecipeState.NONE;
                }
                case 2 -> {
                    if (state != RecipeState.THIRD) state = RecipeState.THIRD;
                    else state = RecipeState.NONE;
                }
            }

            helperWidget.recipeData = null;

            if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return true;
            ProfessionType type = container.getProfessionType();

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
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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

        ActionButtonWidget() { super(0, 0, 0, 0); }

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
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled || !contains((int) mx, (int) my)) return false;
            if (isDisabled || isFilling) return true;
            setFocused(true);
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if (onClickCallback != null) onClickCallback.accept(this);
            return true;
        }
    }

    private static class ProfBombWidget extends Widget {
        final BombType type;
        public BombInfo bomb;
        public boolean isActive;
        public String text;

        public ProfBombWidget(BombType type) {
            super(0, 0, 0, 0);
            this.type = type;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            try {
                if (bomb != null) {
                    if (bomb.server().equals(Models.WorldState.getCurrentWorldName())) hovered = false;
                }

                String currentWorld = Models.WorldState.getCurrentWorldName();
                isActive = false;
                bomb = null;

                for (BombInfo bomb : Models.Bomb.getBombBells()) {
                    if (bomb.bomb() == type) {
                        isActive = true;
                        if (bomb.server().equals(currentWorld)) {
                            this.bomb = bomb;
                            break;
                        }
                        if (this.bomb == null || bomb.getRemainingLong() > this.bomb.getRemainingLong()) {
                            this.bomb = bomb;
                        }
                    }
                }

                if (isActive) {
                    String worldColor = bomb.server().equals(currentWorld) ? "§a" : "§f";
                    worldColor += (hovered ? "§n" : "");
                    String bombType = "?";
                    if (type == BombType.PROFESSION_SPEED) bombType = "Speed";
                    if (type == BombType.PROFESSION_XP) bombType = "XP";

                    text = "§6" + (hovered ? "§n" : "") + "Profession " + bombType + " §7" + (hovered ? "§n" : "") + "on " + worldColor + bomb.server() + " §6" + (hovered ? "§n" : "") + "(" + bomb.getRemainingString() + ")";

                    if (bomb.getRemainingLong() < 30000) {
                        long seconds = Time.now().timestamp() / 1000;

                        String color = (seconds % 2 == 0) ? "§c" : "§4";
                        color += (hovered ? "§n" : "");

                        text = color + "Profession " + bombType + " on "
                                + bomb.server()
                                + " (" + bomb.getRemainingString() + ") (EXPIRING SOON)";
                    }

                    ui.drawCenteredText(text, x + width / 2f, y + height / 2f, 1f);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (bomb.server().equals(Models.WorldState.getCurrentWorldName())) return true;

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if (bomb == null) return true;
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                McUtils.setScreen(null);
                client.player.networkHandler.sendChatCommand("switch " + bomb.server());
            }

            return true;
        }
    }
}
//TODO: cant click on item after switching to account bank
//TODO: bug in character bank when character is not known (when restarting game while in raid and joining again wynntils doesnt know which class you are on)
//TODO: wynnbuilder loader: link at the left where you can paste a wynnbuilder link to load a recipe