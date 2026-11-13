package julianh06.wynnextras.features.buildplanner.gui;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import julianh06.wynnextras.compat.wynntils.WynntilsCustomModelAdapter;
import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.BuildCalculator;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.Craft;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.Ingredient;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec.Recipe;
import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.CraftingPageLayout;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CrafterScreen extends PlannerScreen {
    private final Screen parent;
    private final String toolTabId;
    private final ToolWorkspaceManager workspace = ToolWorkspaceManager.getInstance();
    private final List<ThemedButton> tabButtons = new ArrayList<>();
    private final List<Recipe> recipes = CraftedItemCodec.recipeChoices();
    private final Map<Integer, Ingredient> ingredients = CraftedItemCodec.ingredientChoices().stream()
            .collect(Collectors.toMap(Ingredient::id, ingredient -> ingredient));
    private final List<ThemedButton> footerButtons = new ArrayList<>();
    private final List<PageControl> controls = new ArrayList<>();
    private final List<WynnItemTooltip.Card> ingredientCards = new ArrayList<>();
    private final Map<Integer, ItemStack> ingredientIcons = new HashMap<>();
    private final Map<String, ItemStack> materialIcons = new HashMap<>();
    private final Deque<Craft> undoHistory = new ArrayDeque<>();
    private final Deque<Craft> redoHistory = new ArrayDeque<>();
    private final SmoothScroll pageScroll = new SmoothScroll();
    private final PlannerScrollbar pageScrollbar = new PlannerScrollbar(pageScroll, 16);
    private CraftingPageLayout.Page page;
    private WynnItemTooltip.Card resultCard;
    private float resultCardScale = CARD_SCALE;
    private TextInputWidget hashField;
    private TextInputWidget tabNameField;
    private String hashInput;
    private int scrollOffset;
    private int maxScroll;
    private Craft craft;
    private WynnItem preview;
    private String error = "";
    private String notice = "";
    private int draggedIngredient = -1;
    private double ingredientDragX;
    private double ingredientDragY;
    private boolean ingredientMoved;
    private int hoveredIngredient = -1;
    private int copiedIngredient = -1;
    private int ticks;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int viewportTop;
    private int viewportBottom;
    private static final int CONTROL_BOX_HEIGHT = 383;
    private static final int CONTROLS_HEIGHT = 383;
    private static final float CARD_SCALE = 1.25F;
    private static final float CONTROL_TEXT_SCALE = 1.25F;
    private static final float BUTTON_TEXT_SCALE = 1.2F;

    public CrafterScreen(Screen parent) {
        this(parent, ToolWorkspaceManager.getInstance().add(ToolWorkspaceManager.Type.CRAFTER));
    }

    CrafterScreen(Screen parent, String toolTabId) {
        super(Text.literal("Crafter"));
        this.parent = parent;
        this.toolTabId = toolTabId;
        craft = CraftedItemCodec.readCraft(workspace.find(toolTabId).craftCode());
    }

    @Override
    protected int getMinLogicalWidth() {
        return (int) Math.round(1400 * scaleFactor);
    }

    @Override
    protected void init() {
        super.init();
        if (hashField != null) removeRootWidget(hashField);
        if (tabNameField != null) removeRootWidget(tabNameField);
        clearChildren();
        tabButtons.clear();
        footerButtons.clear();
        controls.clear();
        ingredientCards.clear();
        panelWidth = width - 20;
        panelHeight = height - 74;
        left = (width - panelWidth) / 2;
        top = 64;
        WorkspaceTabs.add(parent, toolTabId, 8, 2, width - 16,
                button -> tabButtons.add(addButton(button)));
        viewportTop = top + 6;
        viewportBottom = top + panelHeight - 58;
        updatePreview();
        for (int i = 0; i < 6; i++) {
            Ingredient ingredient = ingredients.get(craft.ingredients().get(i));
            ingredientCards.add(WynnItemTooltip.ingredientCard(textRenderer, ingredient));
        }
        updateCardLayout();
        var columns = CraftingPageLayout.columns(Math.max(280, panelWidth - 28));
        int w = columns.form();
        int typeWidth = (w - 16) * 45 / 100;
        tabNameField = builderTextInput(0, 0, w - 16, 28, "Tab name");
        tabNameField.setMaxLength(64);
        tabNameField.setTextScale(2.5F);
        tabNameField.setTextOffset(8, 17);
        tabNameField.setInputAndMoveCursorToEnd(workspace.find(toolTabId).name());
        tabNameField.setOnChange(value -> {
            try {
                if (workspace.rename(toolTabId, value)) updateCardLayout();
            } catch (IllegalArgumentException ignored) {
            }
        });
        addPageControl(tabNameField, 8, 7, true, 2);
        pageButton(46, 41, typeWidth - 42, 26, title(recipe().type()), this::chooseType, true);
        pageButton(46, 79, typeWidth - 42, 26,
                recipe().name().substring(recipe().name().indexOf('-') + 1), this::chooseLevel, true);
        int speedWidth = (w - typeWidth - 16) / 3;
        if (isWeapon()) {
            for (int i = 0; i < 3; i++) {
                final int speed = i;
                ThemedButton button = pageButton(typeWidth + 6 + i * speedWidth, 79, speedWidth - 3, 26,
                        List.of("Slow", "Normal", "Fast").get(i), () -> change(new Craft(craft.recipeId(),
                                craft.ingredients(), craft.material1(), craft.material2(), speed)), true);
                if (craft.speed() == i) {
                    button.setMessage(button.getMessage().copy().formatted(Formatting.GREEN, Formatting.BOLD));
                }
            }
        }
        hashField = builderTextInput(0, 0, w - 100, 28, "Crafted recipe hash or link");
        hashField.setMaxLength(2048);
        hashField.setTextScale(2.5F);
        hashField.setTextOffset(8, 17);
        hashField.setInputAndMoveCursorToEnd(hashInput == null ? CraftedItemCodec.encode(craft) : hashInput);
        hashField.setOnChange(value -> hashInput = value);
        addPageControl(hashField, 46, 117, true, 2);
        pageButton(w - 50, 117, 42, 28, "Load", () -> importCode(hashField.getInput()), true);
        int cellWidth = (w - 22) / 2;
        for (int i = 0; i < 2; i++) {
            final int index = i;
            var material = recipe().materials().get(i);
            int cellX = 8 + i * (cellWidth + 6);
            ThemedButton button = pageButton(cellX, 173, cellWidth, 38, "", () -> cycleMaterial(index), true);
            button.setHoverText(Text.literal("Click to change " + material.name() + " tier"));
        }
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            int cellX = 8 + (i % 2) * (cellWidth + 6);
            int cellY = 221 + (i / 2) * 40;
            pageButton(cellX, cellY, cellWidth, 36, "", () -> selectSlot(slot), true);
        }
        List<String> copies = List.of("Reset", "Copy Hash", "Copy Short", "Copy Long");
        int copyWidth = (w - 60) / 4;
        for (int i = 0; i < copies.size(); i++) {
            String action = copies.get(i);
            pageButton(8 + i * (copyWidth + 4), 347, copyWidth, 28, action,
                    () -> footerAction(action), i == 0 || preview != null);
        }
        pageButton(24 + copyWidth * 4, 347, 28, 28, "i", () -> {}, true)
                .setTextScale(1.5F).setTextOffset(0.5F, 0.0F).setHoverLines(List.of(
                Text.literal("Controls").formatted(Formatting.UNDERLINE),
                Text.literal("Click: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Choose ingredient").formatted(Formatting.GRAY)),
                Text.literal("Drag: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Swap ingredients").formatted(Formatting.GRAY)),
                Text.literal("Ctrl + Drag: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Copy ingredient").formatted(Formatting.GRAY)),
                Text.literal("Shift + 1-6: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Swap hovered ingredient with slot").formatted(Formatting.GRAY)),
                Text.literal("Ctrl + 1-6: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Copy hovered ingredient to slot").formatted(Formatting.GRAY)),
                Text.literal("Ctrl + C/V: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Copy/paste hovered ingredient").formatted(Formatting.GRAY)),
                Text.literal("Ctrl + Z/Y: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Undo/redo craft changes").formatted(Formatting.GRAY)),
                Text.literal("Right-click: ").formatted(Formatting.WHITE)
                        .append(Text.literal("Clear slot").formatted(Formatting.GRAY))));
        List<String> labels = new ArrayList<>(List.of("Back", "Save", "Saved", "Paste", "Browser"));
        var destination = workspace.craftTarget(toolTabId);
        if (destination != null) labels.add("Equip");
        int buttonWidth = (panelWidth - 24 - (labels.size() - 1) * 5) / labels.size();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            ThemedButton button = new ThemedButton(left + 12 + i * (buttonWidth + 5), top + panelHeight - 30,
                    buttonWidth, 22, Text.literal(label), () -> footerAction(label))
                    .setTextScale(BUTTON_TEXT_SCALE);
            if (label.equals("Save")) button.setEnabled(preview != null);
            if (label.equals("Equip")) {
                button.setEnabled(preview != null && CraftedItemCodec.matchesSlot(preview, destination.slotType()));
                button.setHoverText(Text.literal("Equip in the original builder slot. Consumables cannot be equipped."));
            }
            footerButtons.add(addButton(button));
        }
    }

    private void updateCardLayout() {
        if (ingredientCards.size() != 6) return;
        List<ItemInspection.Row> resultRows = previewRows();
        resultCard = preview == null
                ? WynnItemTooltip.messageCard(textRenderer, "Invalid recipe", resultRows)
                : WynnItemTooltip.itemCard(textRenderer, preview, workspace.find(toolTabId).name());
        var columns = CraftingPageLayout.columns(Math.max(280, panelWidth - 28));
        resultCardScale = Math.max(CARD_SCALE,
                Math.min(1.4F, (columns.result() - 8) / (float) resultCard.width()));
        page = CraftingPageLayout.arrange(columns, CONTROLS_HEIGHT, resultCard.height(resultCardScale),
                ingredientCards.stream().map(card -> card.height(CARD_SCALE)).toList());
        maxScroll = Math.max(0, page.height() - viewportHeight());
        pageScroll.clamp(0, maxScroll);
        scrollOffset = Math.round(pageScroll.position());
    }

    private ThemedButton pageButton(int x, int y, int width, int height, String label, Runnable action, boolean enabled) {
        int textWidth = Math.round((width - 8) / BUTTON_TEXT_SCALE);
        String fitted = textRenderer.getWidth(label) <= textWidth ? label
                : textRenderer.trimToWidth(label, Math.max(0, textWidth - textRenderer.getWidth("..."))) + "...";
        ThemedButton button = new ThemedButton(0, 0, width, height, Text.literal(fitted), action)
                .setTextScale(BUTTON_TEXT_SCALE);
        addPageControl(button, x, y, enabled);
        if (!label.isBlank()) button.setHoverText(Text.literal(label));
        return button;
    }

    private TextInputWidget builderTextInput(int x, int y, int width, int height, String placeholder) {
        TextInputWidget field = new TextInputWidget(
                x * 2, y * 2, width * 2, height * 2, 8, 10, 2.0F);
        field.setPlaceholder(placeholder);
        field.setBackgroundColor(CustomColor.fromInt(UiTheme.SURFACE));
        field.setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
        field.setTextColor(CustomColor.fromInt(UiTheme.TEXT));
        field.setPlaceholderColor(CustomColor.fromInt(UiTheme.MUTED));
        field.setCursorColor(CustomColor.fromInt(UiTheme.accent()));
        return field;
    }

    private void addPageControl(Widget widget, int x, int y, boolean enabled) {
        addPageControl(widget, x, y, enabled, 1);
    }

    private void addPageControl(Widget widget, int x, int y, boolean enabled, int coordinateScale) {
        widget.setPosition((left + 12 + x) * coordinateScale,
                (viewportTop + y - scrollOffset) * coordinateScale);
        controls.add(new PageControl(widget, y, enabled, coordinateScale));
        if (widget instanceof ThemedButton button) addButton(button);
        else addRootWidget(widget);
        widget.setEnabled(enabled && widget.getY() / coordinateScale >= viewportTop
                && (widget.getY() + widget.getHeight()) / coordinateScale <= viewportBottom);
    }

    private void change(Craft changed) {
        if (changed.equals(craft)) return;
        undoHistory.push(craft);
        if (undoHistory.size() > 100) undoHistory.removeLast();
        redoHistory.clear();
        applyCraft(changed);
    }

    private void applyCraft(Craft changed) {
        craft = changed;
        notice = "";
        hashInput = CraftedItemCodec.encode(craft);
        saveWorkspace();
        init();
    }

    private void updatePreview() {
        preview = null;
        error = "";
        try {
            preview = CraftedItemCodec.preview(craft);
        } catch (IllegalArgumentException exception) {
            error = exception.getMessage();
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Could not calculate Crafter preview.", exception);
            error = "Crafting data could not be loaded. See the log.";
        }
    }

    private Recipe recipe() {
        return recipes.stream().filter(recipe -> recipe.id() == craft.recipeId()).findFirst().orElseThrow();
    }

    private boolean isWeapon() {
        return List.of("bow", "spear", "wand", "dagger", "relik").contains(recipe().type());
    }

    private void chooseType() {
        List<String> types = recipes.stream().map(Recipe::type).distinct().toList();
        List<CraftingChoiceScreen.Choice> choices = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            choices.add(new CraftingChoiceScreen.Choice(i, title(type), "Craft " + type,
                    recipeIcon(type), List.of(Text.literal("Craft " + title(type)).formatted(Formatting.GRAY))));
        }
        client.setScreen(new CraftingChoiceScreen(this, "Recipe type", choices, selected -> {
            String type = types.get(selected.id());
            String suffix = recipe().name().substring(recipe().name().indexOf('-'));
            Recipe target = recipes.stream().filter(recipe -> recipe.type().equals(type))
                    .min(Comparator.comparingInt(recipe -> recipe.name().endsWith(suffix) ? -1
                            : Math.abs(recipe.levelHigh() - recipe().levelHigh()))).orElseThrow();
            change(craft.withRecipe(target.id()));
        }));
    }

    private void chooseLevel() {
        List<CraftingChoiceScreen.Choice> choices = recipes.stream().filter(entry -> entry.type().equals(recipe().type()))
                .sorted(Comparator.comparingInt(Recipe::levelLow).reversed())
                .map(entry -> new CraftingChoiceScreen.Choice(entry.id(), recipeLabel(entry),
                        title(entry.profession()),
                        recipeIcon(entry.type()), recipeTooltip(entry))).toList();
        client.setScreen(new CraftingChoiceScreen(this, "Recipe level", choices,
                selected -> change(craft.withRecipe(selected.id()))));
    }

    private void selectSlot(int slot) {
        List<CraftingChoiceScreen.Choice> choices = ingredients.values().stream()
                .filter(ingredient -> ingredient.id() != 4000 && ingredient.supports(recipe()))
                .sorted(Comparator.comparing(Ingredient::name, String.CASE_INSENSITIVE_ORDER))
                .map(ingredient -> new CraftingChoiceScreen.Choice(ingredient.id(),
                        ingredient.name() + " " + "✫".repeat(3) + " [Level " + ingredient.level() + "]",
                        ingredientDescription(ingredient), ingredient)).toList();
        Runnable remove = craft.ingredients().get(slot) == 4000
                ? null : () -> change(craft.withIngredient(slot, 4000));
        client.setScreen(new CraftingChoiceScreen(this, "Ingredients", choices,
                selected -> change(craft.withIngredient(slot, selected.id())), remove));
    }

    private void cycleMaterial(int index) {
        int tier = index == 0 ? craft.material1() : craft.material2();
        change(new Craft(craft.recipeId(), craft.ingredients(),
                index == 0 ? tier % 3 + 1 : craft.material1(),
                index == 1 ? tier % 3 + 1 : craft.material2(), craft.speed()));
    }

    private String ingredientDescription(Ingredient ingredient) {
        return String.join(", ", ingredient.professions()) + "\n" + String.join("\n", ingredient.details());
    }

    private static String recipeLabel(Recipe recipe) {
        String materials = recipe.materials().stream()
                .map(material -> material.name().replaceFirst("(?i)^Refined\\s+", ""))
                .collect(Collectors.joining(" + "));
        return title(recipe.type()) + " " + recipe.levelLow() + "-" + recipe.levelHigh() + ", " + materials;
    }

    private static List<Text> recipeTooltip(Recipe recipe) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(title(recipe.type()) + " Recipe").formatted(Formatting.GOLD));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Profession: ").formatted(Formatting.GRAY)
                .append(Text.literal(title(recipe.profession())).formatted(Formatting.WHITE)));
        tooltip.add(Text.literal("Crafting Level: ").formatted(Formatting.GRAY)
                .append(Text.literal(recipe.levelLow() + "-" + recipe.levelHigh()).formatted(Formatting.WHITE)));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Materials").formatted(Formatting.YELLOW));
        for (var material : recipe.materials()) {
            tooltip.add(Text.literal("  " + material.amount() + "x ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(material.name().replaceFirst("(?i)^Refined\\s+", ""))
                            .formatted(Formatting.WHITE)));
        }
        return tooltip;
    }

    private void footerAction(String action) {
        switch (action) {
            case "Back" -> close();
            case "Save" -> client.setScreen(new SaveCraftScreen(this, workspace.find(toolTabId).name(),
                    CraftedItemCodec.encode(craft)));
            case "Saved" -> client.setScreen(new SavedBuildsScreen(this, parent, true));
            case "Reset" -> client.setScreen(new ThemedConfirmScreen(confirmed -> {
                if (confirmed) change(CraftedItemCodec.emptyCraft(recipe().type()));
                client.setScreen(this);
            }, Text.literal("Reset this craft?"), Text.literal("This clears all six ingredients and resets materials.")));
            case "Paste" -> importCode(client.keyboard.getClipboard());
            case "Browser" -> {
                LinkUtils.openLink("https://wynnbuilder.github.io/crafter/#"
                        + CraftedItemCodec.encode(craft).substring(3));
                notice = "Opened current recipe in WynnBuilder.";
            }
            case "Copy Hash", "Copy Short", "Copy Long" -> {
                String code = CraftedItemCodec.encode(craft);
                client.keyboard.setClipboard(action.equals("Copy Hash") ? code
                        : action.equals("Copy Long") ? CraftedItemCodec.shareText(craft)
                        : "https://wynnbuilder.github.io/crafter/#" + code.substring(3));
                notice = action.equals("Copy Hash") ? "WynnCrafter code copied."
                        : action.equals("Copy Long") ? "Recipe link and ingredient list copied." : "Recipe link copied.";
            }
            case "Equip" -> {
                try {
                    var destination = workspace.craftTarget(toolTabId);
                    if (destination == null) throw new IllegalStateException("The destination build is no longer open.");
                    WynnItem item = CraftedItemCodec.decodeForSlot(CraftedItemCodec.encode(craft), destination.slotType());
                    WorkspaceTabs.equipCraft(parent, toolTabId, item);
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    workspace.select(toolTabId);
                    client.setScreen(this);
                    notice = exception.getMessage();
                }
            }
            default -> throw new IllegalArgumentException("Unknown crafting action");
        }
    }

    private void importCode(String text) {
        try {
            Craft imported = CraftedItemCodec.readCraft(text);
            CraftedItemCodec.preview(imported);
            pageScroll.jump(0);
            change(imported);
            notice = "Recipe imported.";
        } catch (IllegalArgumentException exception) {
            notice = "Import failed: " + exception.getMessage();
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Could not import WynnCrafter recipe.", exception);
            notice = "Crafting data could not be loaded. See the log.";
        }
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.fill(0, 0, width, height, UiTheme.BACKGROUND);
        UiTheme.drawPanel(context, ui, left, top, panelWidth, panelHeight);
        scrollOffset = Math.round(pageScroll.update(delta));
        hoveredIngredient = mouseY >= viewportTop && mouseY < viewportBottom
                ? ingredientSlotAt(mouseX, mouseY) : -1;
        for (PageControl control : controls) {
            control.setY(viewportTop + control.y - scrollOffset);
            boolean enabled = control.enabled && control.getY() >= viewportTop
                    && control.getY() + control.getHeight() <= viewportBottom;
            control.setEnabled(enabled);
            if (control.is(hashField) || control.is(tabNameField)) {
                control.setVisible(enabled);
                if (!enabled) control.setFocused(false);
            }
        }
        ui.enableScissor(left + 3, viewportTop, left + panelWidth - 8, viewportBottom);
        try {
            int x = left + 12;
            int y = viewportTop - scrollOffset;
            UiTheme.drawPanel(context, ui, x, y, page.controls().width(), CONTROL_BOX_HEIGHT);
            renderVanillaChildren(context, mouseX, mouseY, delta);
            renderControls(context, x, y);
            resultCard.render(context, textRenderer,
                    x + page.result().x() + (page.result().width() - resultCard.width(resultCardScale)) / 2,
                    y + page.result().y(), resultCardScale);
            int ingredientCardWidth = ingredientCardWidth();
            int scaledIngredientCardWidth = Math.round(ingredientCardWidth * CARD_SCALE);
            int dragTarget = ingredientMoved ? ingredientSlotAt(mouseX, mouseY) : -1;
            for (int i = 0; i < 6; i++) {
                var bounds = page.ingredients().get(i);
                var card = ingredientCards.get(i);
                int cardX = x + bounds.x() + (bounds.width() - scaledIngredientCardWidth) / 2;
                int cardY = y + bounds.y();
                card.render(context, textRenderer,
                        cardX, cardY, CARD_SCALE, ingredientCardWidth);
                if (dragTarget == i && dragTarget != draggedIngredient) {
                    int cardHeight = card.height(CARD_SCALE);
                    int accent = UiTheme.accent();
                    ui.fill(cardX, cardY, cardX + scaledIngredientCardWidth, cardY + 2, accent);
                    ui.fill(cardX, cardY + cardHeight - 2, cardX + scaledIngredientCardWidth, cardY + cardHeight, accent);
                    ui.fill(cardX, cardY, cardX + 2, cardY + cardHeight, accent);
                    ui.fill(cardX + scaledIngredientCardWidth - 2, cardY,
                            cardX + scaledIngredientCardWidth, cardY + cardHeight, accent);
                }
            }
        } finally {
            ui.disableScissor();
        }
        for (ThemedButton button : footerButtons) button.draw(context, mouseX, mouseY, delta, ui);
        for (ThemedButton button : tabButtons) button.draw(context, mouseX, mouseY, delta, ui);
        pageScrollbar.configure(left + panelWidth - 7, viewportTop, viewportHeight(), maxScroll);
        pageScrollbar.draw(context, mouseX, mouseY, delta, ui);
        if (ingredientMoved && draggedIngredient >= 0) {
            int cellWidth = (page.controls().width() - 22) / 2;
            renderIngredientTile(ingredients.get(craft.ingredients().get(draggedIngredient)), draggedIngredient,
                    mouseX + 7, mouseY + 7, cellWidth, true);
        }

    }

    private List<ItemInspection.Row> previewRows() {
        List<ItemInspection.Row> content = new ArrayList<>();
        if (!error.isEmpty()) {
            content.add(row(error, "", ItemInspection.Kind.WARNING));
            content.add(row("Replace incompatible ingredients; current slots are preserved.", "", ItemInspection.Kind.NOTE));
        } else if (preview != null) {
            content.addAll(ItemInspection.rows(preview, "", true));
            if ("consumable".equals(preview.type())) {
                content.add(row("Consumable preview only; not an equipment slot.", "", ItemInspection.Kind.NOTE));
            } else if (!preview.ingredientPowders().isEmpty()) {
                content.add(row("After ingredient powders:", "", ItemInspection.Kind.INFO));
                var damage = BuildCalculator.calculate(121, new int[5],
                        List.of(new BuildCalculator.EquippedItem(preview, "", true))).weaponDamage();
                String[] elements = {"Neutral", "Earth", "Thunder", "Water", "Fire", "Air"};
                for (int i = 0; i < damage.length; i++) {
                    if (damage[i].min() != 0 || damage[i].max() != 0) {
                        content.add(row(elements[i], String.format(Locale.ROOT, "%.2f-%.2f",
                                damage[i].min(), damage[i].max()), ItemInspection.Kind.INLINE));
                    }
                }
            }
        }
        return content;
    }

    private void renderControls(DrawContext context, int x, int y) {
        int w = page.controls().width();
        int typeWidth = (w - 16) * 45 / 100;
        drawScaledText("Type:", x + 8, y + 54, 0xFFFFFFFF);
        drawScaledText("Lv:", x + 8, y + 92, 0xFFFFFFFF);
        drawScaledText("Hash:", x + 8, y + 131, 0xFFFFFFFF);
        if (isWeapon()) {
            drawScaledCenteredText("Attack Speed",
                    x + typeWidth + (w - typeWidth) / 2, y + 54, 0xFFFFFFFF);
        }
        drawScaledCenteredText("Crafting Materials", x + w / 2, y + 160, 0xFFFFFFFF);
        int cellWidth = (w - 22) / 2;
        for (int i = 0; i < 2; i++) {
            var material = recipe().materials().get(i);
            int cellX = x + 8 + i * (cellWidth + 6);
            drawMaterialIcon(material.name(), cellX + 6, y + 178, 28);
            Text label = Text.literal(material.amount() + "x " + material.name().replaceFirst("^Refined ", "") + " ")
                    .formatted(Formatting.WHITE)
                    .append(Text.literal("[").formatted(Formatting.GOLD))
                    .append(Text.literal("\u272B".repeat(i == 0 ? craft.material1() : craft.material2()))
                            .formatted(Formatting.YELLOW))
                    .append(Text.literal("]").formatted(Formatting.GOLD));
            drawScaledText(label, cellX + 40, y + 192, 0xFFFFFFFF);
        }
        for (int i = 0; i < 6; i++) {
            int cellX = x + 8 + (i % 2) * (cellWidth + 6);
            int cellY = y + 221 + (i / 2) * 40;
            Ingredient ingredient = ingredients.get(craft.ingredients().get(i));
            renderIngredientTile(ingredient, i, cellX, cellY, cellWidth, false);
        }
    }

    private void renderIngredientTile(Ingredient ingredient, int slot, int x, int y, int width, boolean floating) {
        if (floating) {
            UiTheme.drawRoundedBox(drawContext, ui, x, y, width, 36, UiTheme.hoverBackground(), UiTheme.accent());
        }
        drawIngredientIcon(ingredient, x + 6, y + 5, 26);
        String name = ingredient.name();
        int textWidth = Math.round((width - 43) / CONTROL_TEXT_SCALE);
        if (textRenderer.getWidth(name) > textWidth) {
            name = textRenderer.trimToWidth(name, Math.max(0, textWidth - textRenderer.getWidth("..."))) + "...";
        }
        drawScaledText(name, x + 40, y + 9, ingredientColor(ingredient));
        int effectiveness = preview == null ? 0 : preview.stat("ingredientEffectiveness" + slot);
        drawScaledText(preview == null ? "[--]" : "[" + effectiveness + "%]",
                x + 40, y + 24, preview == null ? 0xFFAAAAAA
                        : effectiveness < 0 ? 0xFFFF5555 : effectiveness == 0 ? 0xFFFFFFFF : 0xFF55FF55);
    }

    private void drawScaledText(String text, float x, float centerY, int color) {
        drawScaledText(Text.literal(text), x, centerY, color);
    }

    private void drawScaledText(Text text, float x, float centerY, int color) {
        ui.pushMatrix();
        ui.translate(x, centerY);
        ui.scale(CONTROL_TEXT_SCALE, CONTROL_TEXT_SCALE);
        ui.drawTextWithShadow(textRenderer, text, 0, -4, color);
        ui.popMatrix();
    }

    private void drawScaledCenteredText(String text, float x, float centerY, int color) {
        ui.pushMatrix();
        ui.translate(x, centerY);
        ui.scale(CONTROL_TEXT_SCALE, CONTROL_TEXT_SCALE);
        ui.drawCenteredTextWithShadow(textRenderer, text, 0, -4, color);
        ui.popMatrix();
    }

    private void drawMaterialIcon(String name, int x, int y, int size) {
        ItemStack stack = materialIcons.computeIfAbsent(name, CrafterScreen::materialIcon);
        ui.pushMatrix();
        try {
            ui.translate(x, y);
            ui.scale(size / 16.0F, size / 16.0F);
            ui.drawItem(stack, 0, 0);
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not render crafting material icon for {}.", name, exception);
            ItemStack fallback = new ItemStack(Items.BARRIER);
            materialIcons.put(name, fallback);
            ui.drawItem(fallback, 0, 0);
        } finally {
            ui.popMatrix();
        }
    }

    private static ItemStack materialIcon(String name) {
        try {
            var item = WynnDataService.getInstance().findByInternalName(name).stream().findFirst()
                    .or(() -> WynnDataService.getInstance().findByDisplayName(name.replaceFirst("^Refined ", ""))
                            .stream().findFirst()).orElse(null);
            if (item == null) return new ItemStack(Items.BARRIER);
            return itemIcon(item.icon().id(), item.icon().model(), item.icon().customModelData(), Items.BARRIER);
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not create crafting material icon for {}.", name, exception);
            return new ItemStack(Items.BARRIER);
        }
    }

    private void drawIngredientIcon(Ingredient ingredient, int x, int y, int size) {
        ItemStack stack = ingredientIcons.computeIfAbsent(ingredient.id(), ignored -> ingredientIcon(ingredient));
        ui.pushMatrix();
        try {
            ui.translate(x, y);
            ui.scale(size / 16.0F, size / 16.0F);
            ui.drawItem(stack, 0, 0);
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not render ingredient icon for {} ({}).",
                    ingredient.name(), ingredient.id(), exception);
            ItemStack fallback = ingredientFallback();
            ingredientIcons.put(ingredient.id(), fallback);
            ui.drawItem(fallback, 0, 0);
        } finally {
            ui.popMatrix();
        }
    }

    static ItemStack ingredientIcon(Ingredient ingredient) {
        try {
            var icon = ingredient.icon();
            if (!icon.skinHash().isBlank()) {
                ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
                String texture = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":"
                        + "\"https://textures.minecraft.net/texture/" + icon.skinHash() + "\"}}}")
                        .getBytes(StandardCharsets.UTF_8));
                Property property = new Property("textures", texture);
                GameProfile profile = new GameProfile(
                        UUID.nameUUIDFromBytes(icon.skinHash().getBytes(StandardCharsets.UTF_8)), "Ingredient",
                        new PropertyMap(ImmutableMultimap.of("textures", property)));
                stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile));
                return stack;
            }
            if (icon.itemId().isBlank()) return ingredientFallback();
            return itemIcon(icon.itemId(), icon.model(), icon.customModelData(), ingredientFallback().getItem());
        } catch (RuntimeException exception) {
            PlannerLog.LOGGER.warn("Could not create ingredient icon for {} ({}).",
                    ingredient.name(), ingredient.id(), exception);
            return ingredientFallback();
        }
    }

    static ItemStack ingredientFallback() {
        return new ItemStack(Items.BARRIER);
    }

    static ItemStack recipeIcon(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "bow", "dagger", "relik", "spear", "wand" ->
                    itemIcon("minecraft:potion", type.toLowerCase(Locale.ROOT) + ".basicWood", 0, Items.BARRIER);
            case "helmet" -> new ItemStack(Items.LEATHER_HELMET);
            case "chestplate" -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case "leggings" -> new ItemStack(Items.LEATHER_LEGGINGS);
            case "boots" -> new ItemStack(Items.LEATHER_BOOTS);
            case "ring" -> new ItemStack(Items.EMERALD);
            case "bracelet" -> new ItemStack(Items.IRON_NUGGET);
            case "necklace" -> new ItemStack(Items.GOLD_NUGGET);
            case "food" -> new ItemStack(Items.COOKED_BEEF);
            case "scroll" -> new ItemStack(Items.PAPER);
            default -> new ItemStack(Items.BARRIER);
        };
    }

    private static ItemStack itemIcon(String itemId, String model, int customModelData, net.minecraft.item.Item fallbackItem) {
        ItemStack fallback = new ItemStack(fallbackItem);
        if (itemId == null || itemId.isBlank()) return fallback;
        try {
            ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.of(itemId)));
            if (stack.isEmpty()) return fallback;
            var resolvedModelData = WynntilsCustomModelAdapter.getFloat(model);
            if (model != null && !model.isBlank() && resolvedModelData.isEmpty() && customModelData <= 0) {
                return fallback;
            }
            float modelData = resolvedModelData.orElse((float) customModelData);
            if (modelData > 0) {
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                        List.of(modelData), List.of(), List.of(), List.of()));
            }
            return stack;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static ItemInspection.Row row(String label, String value, ItemInspection.Kind kind) {
        return new ItemInspection.Row(label, value, kind, -1, false);
    }

    private static int ingredientColor(Ingredient ingredient) {
        return ToolItemCards.color(Integer.toString(ingredient.tier()));
    }

    private int viewportHeight() {
        return Math.max(1, viewportBottom - viewportTop);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (WorkspaceTabs.scroll(parent, toolTabId, 8, 2, width - 16,
                logicalCoordinate(mouseX), logicalCoordinate(mouseY), horizontal, vertical)) return true;
        pageScroll.move((float) -vertical * 30, 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        clearUiFocus();
        Click logical = logicalClick(click);
        if (pageScrollbar.mouseClicked(logical.x(), logical.y(), logical.button())) return true;
        if ((logical.button() == 0 || logical.button() == 1)
                && logical.y() >= viewportTop && logical.y() < viewportBottom) {
            int slot = ingredientSlotAt(logical.x(), logical.y());
            if (slot >= 0) {
                if (logical.button() == 1) {
                    if (craft.ingredients().get(slot) != 4000) {
                        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        change(craft.withIngredient(slot, 4000));
                    }
                    return true;
                }
                draggedIngredient = slot;
                ingredientDragX = logical.x();
                ingredientDragY = logical.y();
                ingredientMoved = false;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Click logical = logicalClick(click);
        if (pageScrollbar.mouseDragged(logical.x(), logical.y(), logical.button(),
                deltaX / matrixScale, deltaY / matrixScale)) return true;
        if (draggedIngredient >= 0 && click.button() == 0) {
            if (Math.hypot(logical.x() - ingredientDragX, logical.y() - ingredientDragY) >= 3.0) {
                ingredientMoved = true;
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click logical = logicalClick(click);
        if (pageScrollbar.mouseReleased(logical.x(), logical.y(), logical.button())) return true;
        if (draggedIngredient >= 0 && click.button() == 0) {
            int source = draggedIngredient;
            int target = ingredientSlotAt(logical.x(), logical.y());
            draggedIngredient = -1;
            if (ingredientMoved) {
                if (target >= 0 && target != source) {
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    change(MinecraftUtils.isControlDown()
                            ? craft.withIngredient(target, craft.ingredients().get(source))
                            : craft.swap(source, target));
                }
            } else {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                selectSlot(source);
            }
            ingredientMoved = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    private int ingredientSlotAt(double mouseX, double mouseY) {
        double x = mouseX - left - 12;
        double y = mouseY - viewportTop + scrollOffset;
        int cellWidth = (page.controls().width() - 22) / 2;
        for (int i = 0; i < 6; i++) {
            int cellX = 8 + (i % 2) * (cellWidth + 6);
            int cellY = 221 + (i / 2) * 40;
            if (x >= cellX && x < cellX + cellWidth && y >= cellY && y < cellY + 36) return i;
            var bounds = page.ingredients().get(i);
            if (x >= bounds.x() && x < bounds.right() && y >= bounds.y() && y < bounds.bottom()) return i;
        }
        return -1;
    }

    private int ingredientCardWidth() {
        return Math.max((int) Math.ceil(page.ingredients().getFirst().width() / CARD_SCALE),
                ingredientCards.stream().mapToInt(WynnItemTooltip.Card::width).max().orElse(1));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (hashField.isFocused() && (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            importCode(hashField.getInput());
            return true;
        }
        int target = numberedIngredientSlot(input.key());
        boolean shift = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean control = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (control && matchesPrintableKey(input, GLFW.GLFW_KEY_Z, "z")) {
            if (!undoHistory.isEmpty()) {
                redoHistory.push(craft);
                applyCraft(undoHistory.pop());
            }
            return true;
        }
        if (control && matchesPrintableKey(input, GLFW.GLFW_KEY_Y, "y")) {
            if (!redoHistory.isEmpty()) {
                undoHistory.push(craft);
                applyCraft(redoHistory.pop());
            }
            return true;
        }
        if (control && hoveredIngredient >= 0 && input.key() == GLFW.GLFW_KEY_C) {
            copiedIngredient = craft.ingredients().get(hoveredIngredient);
            notice = "Copied " + ingredients.get(copiedIngredient).name() + ".";
            return true;
        }
        if (control && hoveredIngredient >= 0 && input.key() == GLFW.GLFW_KEY_V) {
            if (copiedIngredient < 0) {
                notice = "No ingredient copied.";
            } else {
                int ingredient = copiedIngredient;
                if (craft.ingredients().get(hoveredIngredient) != ingredient) {
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    change(craft.withIngredient(hoveredIngredient, ingredient));
                }
                notice = "Pasted " + ingredients.get(ingredient).name() + ".";
            }
            return true;
        }
        if (hoveredIngredient >= 0 && target >= 0 && shift != control) {
            if (target != hoveredIngredient) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                change(control
                        ? craft.withIngredient(target, craft.ingredients().get(hoveredIngredient))
                        : craft.swap(hoveredIngredient, target));
            }
            return true;
        }
        return super.keyPressed(input);
    }

    private static boolean matchesPrintableKey(KeyInput input, int fallbackKey, String name) {
        String keyName = GLFW.glfwGetKeyName(input.key(), input.scancode());
        return keyName == null ? input.key() == fallbackKey : keyName.equalsIgnoreCase(name);
    }

    private static int numberedIngredientSlot(int key) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_6) return key - GLFW.GLFW_KEY_1;
        if (key >= GLFW.GLFW_KEY_KP_1 && key <= GLFW.GLFW_KEY_KP_6) return key - GLFW.GLFW_KEY_KP_1;
        return -1;
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks >= 40) {
            saveWorkspace();
            ticks = 0;
        }
    }

    @Override
    public void removed() {
        saveWorkspace();
        super.removed();
    }

    private void saveWorkspace() {
        if (workspace.find(toolTabId) != null) workspace.updateCraft(toolTabId, craft);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static String title(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private static final class PageControl {
        private final Widget widget;
        private final int y;
        private final boolean enabled;
        private final int coordinateScale;

        private PageControl(Widget widget, int y, boolean enabled, int coordinateScale) {
            this.widget = widget;
            this.y = y;
            this.enabled = enabled;
            this.coordinateScale = coordinateScale;
        }

        private void setY(int value) { widget.setPosition(widget.getX(), value * coordinateScale); }
        private int getY() { return widget.getY() / coordinateScale; }
        private int getHeight() { return widget.getHeight() / coordinateScale; }
        private void setEnabled(boolean value) { widget.setEnabled(value); }
        private void setVisible(boolean value) { widget.setVisible(value); }
        private void setFocused(boolean value) { widget.setFocused(value); }
        private boolean is(Widget widget) { return this.widget == widget; }
    }
}
