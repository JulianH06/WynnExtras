package julianh06.wynnextras.features.buildplanner.gui;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class WynnCrafterScreen extends Screen {
    private final Screen parent;
    private final String toolTabId;
    private final ToolWorkspaceManager workspace = ToolWorkspaceManager.getInstance();
    private final List<ThemedButton> tabButtons = new ArrayList<>();
    private final List<Recipe> recipes = CraftedItemCodec.recipeChoices();
    private final Map<Integer, Ingredient> ingredients = CraftedItemCodec.ingredientChoices().stream()
            .collect(Collectors.toMap(Ingredient::id, ingredient -> ingredient));
    private final List<ThemedButton> footerButtons = new ArrayList<>();
    private final List<PageControl> controls = new ArrayList<>();
    private final List<ItemInspectionCard.Embedded> ingredientCards = new ArrayList<>();
    private final SmoothScroll pageScroll = new SmoothScroll(16.0F);
    private CraftingPageLayout.Page page;
    private ItemInspectionCard.Embedded resultCard;
    private TextFieldWidget hashField;
    private String hashInput;
    private int scrollOffset;
    private int maxScroll;
    private Craft craft;
    private WynnItem preview;
    private String error = "";
    private String notice = "";
    private int swapFirst = -1;
    private boolean swapping;
    private int ticks;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int viewportTop;
    private int viewportBottom;
    private static final int CONTROLS_HEIGHT = 251;

    public WynnCrafterScreen(Screen parent) {
        this(parent, ToolWorkspaceManager.getInstance().add(ToolWorkspaceManager.Type.CRAFTER));
    }

    WynnCrafterScreen(Screen parent, String toolTabId) {
        super(Text.literal("WynnCrafter"));
        this.parent = parent;
        this.toolTabId = toolTabId;
        craft = CraftedItemCodec.readCraft(workspace.find(toolTabId).craftCode());
    }

    @Override
    protected void init() {
        clearChildren();
        tabButtons.clear();
        footerButtons.clear();
        controls.clear();
        ingredientCards.clear();
        panelWidth = Math.min(1100, width - 20);
        panelHeight = height - 52;
        left = (width - panelWidth) / 2;
        top = 42;
        WorkspaceTabs.add(this, parent, toolTabId, 12, 10, width - 24,
                button -> tabButtons.add(addDrawableChild(button)));
        viewportTop = top + 32;
        viewportBottom = top + panelHeight - 58;
        updatePreview();
        var columns = CraftingPageLayout.columns(Math.max(280, panelWidth - 28));
        for (int i = 0; i < 6; i++) {
            Ingredient ingredient = ingredients.get(craft.ingredients().get(i));
            ingredientCards.add(ToolItemCards.ingredient(textRenderer, ingredient, columns.ingredientCard()));
        }
        List<ItemInspection.Row> resultRows = previewRows();
        resultCard = ItemInspectionCard.embedded(textRenderer,
                Text.literal(preview == null ? "Invalid recipe" : preview.craftedCode()),
                resultRows, columns.result(), 0xFF00AAAA, preview != null);
        int summaryHeight = 234;
        page = CraftingPageLayout.arrange(columns, CONTROLS_HEIGHT, summaryHeight, resultCard.height(),
                ingredientCards.stream().map(ItemInspectionCard.Embedded::height).toList());
        maxScroll = Math.max(0, page.height() - viewportHeight());
        pageScroll.clamp(0, maxScroll);
        scrollOffset = Math.round(pageScroll.update());
        int w = columns.controls();
        int half = (w - 20) / 2;
        int typeWidth = (w - 16) * 45 / 100;
        pageButton(40, 7, typeWidth - 36, 18, title(recipe().type()), this::chooseType, true);
        pageButton(40, 30, typeWidth - 36, 18,
                recipe().name().substring(recipe().name().indexOf('-') + 1), this::chooseLevel, true);
        int speedWidth = (w - typeWidth - 16) / 3;
        for (int i = 0; i < 3; i++) {
            final int speed = i;
            ClickableWidget button = pageButton(typeWidth + 6 + i * speedWidth, 30, speedWidth - 3, 18,
                    List.of("Slow", "Normal", "Fast").get(i),
                    () -> change(new Craft(craft.recipeId(), craft.ingredients(), craft.material1(), craft.material2(), speed)),
                    isWeapon());
            if (craft.speed() == i) button.setMessage(button.getMessage().copy().formatted(Formatting.GREEN, Formatting.BOLD));
        }
        hashField = new TextFieldWidget(textRenderer, 0, 0, w - 94, 18, Text.literal("Crafted recipe hash or link"));
        hashField.setMaxLength(2048);
        hashField.setText(hashInput == null ? CraftedItemCodec.encode(craft) : hashInput);
        hashField.setChangedListener(value -> hashInput = value);
        addPageControl(hashField, 40, 57, true);
        pageButton(w - 48, 57, 40, 18, "Load", () -> importCode(hashField.getText()), true);
        for (int i = 0; i < 2; i++) {
            final int index = i;
            var material = recipe().materials().get(i);
            int tier = i == 0 ? craft.material1() : craft.material2();
            int cellX = 8 + i * (half + 4);
            int tierWidth = 17;
            for (int j = 1; j <= 3; j++) {
                final int chosen = j;
                var button = pageButton(cellX + half - 51 + (j - 1) * tierWidth, 90, tierWidth - 1, 18, Integer.toString(j),
                        () -> change(new Craft(craft.recipeId(), craft.ingredients(),
                                index == 0 ? chosen : craft.material1(), index == 1 ? chosen : craft.material2(), craft.speed())),
                        true);
                if (tier == j) button.setMessage(button.getMessage().copy().formatted(Formatting.GREEN, Formatting.BOLD));
                button.setTooltip(Tooltip.of(Text.literal(material.amount() + " x " + material.name() + ", tier " + j)));
            }
        }
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            int cellX = 8 + (i % 2) * (half + 4);
            int cellY = 146 + (i / 2) * 25;
            Ingredient ingredient = ingredients.get(craft.ingredients().get(i));
            var button = pageButton(cellX + 31, cellY, half - 52, 19,
                    (swapFirst == i ? "> " : "") + ingredient.name(), () -> selectSlot(slot), true);
            button.setTooltip(Tooltip.of(Text.literal(ingredient.name() + "\n" + ingredientDescription(ingredient))));
            pageButton(cellX + half - 18, cellY, 18, 19, "X", () -> change(craft.withIngredient(slot, 4000)), true);
        }
        List<String> copies = List.of("Reset", "Copy Hash", "Copy Short", "Copy Long");
        int copyWidth = (w - 28) / 4;
        for (int i = 0; i < copies.size(); i++) {
            String action = copies.get(i);
            pageButton(8 + i * (copyWidth + 4), 225, copyWidth, 18, action,
                    () -> footerAction(action), i == 0 || preview != null);
        }
        List<String> labels = new ArrayList<>(List.of("Back", "Swap slots", "Paste"));
        var destination = workspace.craftTarget(toolTabId);
        if (destination != null) labels.add("Equip");
        int buttonWidth = (panelWidth - 24 - (labels.size() - 1) * 5) / labels.size();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            ThemedButton button = new ThemedButton(left + 12 + i * (buttonWidth + 5), top + panelHeight - 30,
                    buttonWidth, 20, Text.literal(label), () -> footerAction(label));
            if (label.equals("Equip")) {
                button.active = preview != null && CraftedItemCodec.matchesSlot(preview, destination.slotType());
                button.setTooltip(Tooltip.of(Text.literal("Equip in the original builder slot. Consumables cannot be equipped.")));
            }
            footerButtons.add(addDrawableChild(button));
        }
    }

    private ThemedButton pageButton(int x, int y, int width, int height, String label, Runnable action, boolean enabled) {
        String fitted = textRenderer.getWidth(label) <= width - 8 ? label
                : textRenderer.trimToWidth(label, Math.max(0, width - 20)) + "...";
        ThemedButton button = new ThemedButton(0, 0, width, height, Text.literal(fitted), action);
        addPageControl(button, x, y, enabled);
        button.setTooltip(Tooltip.of(Text.literal(label)));
        return button;
    }

    private void addPageControl(ClickableWidget widget, int x, int y, boolean enabled) {
        widget.setX(left + 12 + x);
        widget.setY(viewportTop + y - scrollOffset);
        controls.add(new PageControl(addDrawableChild(widget), y, enabled));
        widget.active = enabled && widget.getY() >= viewportTop && widget.getY() + widget.getHeight() <= viewportBottom;
    }

    private void change(Craft changed) {
        craft = changed;
        notice = "";
        hashInput = CraftedItemCodec.encode(craft);
        workspace.updateCraft(toolTabId, craft);
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
            PlannerLog.LOGGER.error("Could not calculate WynnCrafter preview.", exception);
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
            choices.add(new CraftingChoiceScreen.Choice(i, title(types.get(i)), "Craft " + types.get(i)));
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
                .map(entry -> new CraftingChoiceScreen.Choice(entry.id(), entry.name(),
                        entry.profession() + ", ingredient level limit " + entry.levelHigh())).toList();
        client.setScreen(new CraftingChoiceScreen(this, "Recipe level", choices,
                selected -> change(craft.withRecipe(selected.id()))));
    }

    private void selectSlot(int slot) {
        if (swapping) {
            if (swapFirst < 0) {
                swapFirst = slot;
                init();
            } else {
                Craft changed = craft.swap(swapFirst, slot);
                swapFirst = -1;
                swapping = false;
                change(changed);
            }
            return;
        }
        List<CraftingChoiceScreen.Choice> choices = ingredients.values().stream()
                .filter(ingredient -> ingredient.supports(recipe()))
                .sorted(Comparator.comparingInt((Ingredient ingredient) -> ingredient.id() == 4000 ? -1 : 0)
                        .thenComparing(Ingredient::name, String.CASE_INSENSITIVE_ORDER))
                .map(ingredient -> new CraftingChoiceScreen.Choice(ingredient.id(),
                        ingredient.name() + (ingredient.id() == 4000 ? "" : " [" + ingredient.tier()
                                + "* / Lv. " + ingredient.level() + "]"), ingredientDescription(ingredient))).toList();
        client.setScreen(new CraftingChoiceScreen(this, "Ingredient " + (slot + 1), choices,
                selected -> change(craft.withIngredient(slot, selected.id()))));
    }

    private String ingredientDescription(Ingredient ingredient) {
        return String.join(", ", ingredient.professions()) + "\n" + String.join("\n", ingredient.details());
    }

    private void footerAction(String action) {
        switch (action) {
            case "Back" -> close();
            case "Swap slots" -> {
                swapping = !swapping;
                swapFirst = -1;
                notice = swapping ? "Click the first ingredient slot, then the second." : "";
                init();
            }
            case "Reset" -> client.setScreen(new ThemedConfirmScreen(confirmed -> {
                if (confirmed) change(CraftedItemCodec.emptyCraft(recipe().type()));
                client.setScreen(this);
            }, Text.literal("Reset this craft?"), Text.literal("This clears all six ingredients and resets materials.")));
            case "Paste" -> importCode(client.keyboard.getClipboard());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, UiTheme.BACKGROUND);
        UiTheme.drawPanel(context, left, top, panelWidth, panelHeight);
        context.drawCenteredTextWithShadow(textRenderer, "WynnCrafter", width / 2, top + 10, UiTheme.accentRgb());
        scrollOffset = Math.round(pageScroll.update());
        for (PageControl control : controls) {
            var widget = control.widget();
            widget.setY(viewportTop + control.y() - scrollOffset);
            widget.active = control.enabled() && widget.getY() >= viewportTop
                    && widget.getY() + widget.getHeight() <= viewportBottom;
            if (!widget.active && widget == hashField) widget.setFocused(false);
        }
        context.enableScissor(left + 3, viewportTop, left + panelWidth - 8, viewportBottom);
        try {
            int x = left + 12;
            int y = viewportTop - scrollOffset;
            renderControls(context, x, y);
            renderRecipeStats(context, x + page.summary().x(), y + page.summary().y(), page.summary().width());
            resultCard.render(context, textRenderer, x + page.result().x(), y + page.result().y(),
                    (iconX, iconY, size) -> drawRecipeIcon(context, iconX, iconY, size));
            context.drawCenteredTextWithShadow(textRenderer, "Ingredients",
                    x + page.ingredientLeft() + page.ingredientWidth() / 2, y + page.headingY(), 0xFFFFFFFF);
            for (int i = 0; i < 6; i++) {
                var bounds = page.ingredients().get(i);
                ingredientCards.get(i).render(context, textRenderer, x + bounds.x(), y + bounds.y(),
                        (iconX, iconY, size) -> {});
            }
            super.render(context, mouseX, mouseY, delta);
        } finally {
            context.disableScissor();
        }
        for (ThemedButton button : footerButtons) button.render(context, mouseX, mouseY, delta);
        for (ThemedButton button : tabButtons) button.render(context, mouseX, mouseY, delta);
        if (maxScroll > 0) {
            int thumb = Math.max(16, viewportHeight() * viewportHeight() / page.height());
            int thumbY = viewportTop + Math.round(scrollOffset / (float) maxScroll * (viewportHeight() - thumb));
            context.fill(left + panelWidth - 5, viewportTop, left + panelWidth - 3, viewportBottom, UiTheme.BORDER);
            context.fill(left + panelWidth - 5, thumbY, left + panelWidth - 3, thumbY + thumb, UiTheme.accent());
        }
        String status = !workspace.error().isEmpty() ? workspace.error() : notice;
        int statusY = viewportBottom + 5;
        for (OrderedText line : textRenderer.wrapLines(Text.literal(status), panelWidth - 24)) {
            if (statusY + 9 > top + panelHeight - 32) break;
            context.drawTextWithShadow(textRenderer, line, left + 12, statusY, 0xFFFFAA00);
            statusY += 10;
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
        int half = (w - 20) / 2;
        int typeWidth = (w - 16) * 45 / 100;
        UiTheme.drawRoundedBox(context, x, y, w, CONTROLS_HEIGHT, UiTheme.SURFACE, UiTheme.BORDER);
        context.drawTextWithShadow(textRenderer, "Type:", x + 8, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Lv:", x + 8, y + 35, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Hash:", x + 8, y + 62, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Attack Speed",
                x + typeWidth + (w - typeWidth) / 2, y + 12, 0xFFFFFFFF);
        for (int i = 0; i < 2; i++) {
            int cellX = x + 8 + i * (half + 4);
            String name = recipe().materials().get(i).name().replaceFirst("^Refined ", "") + " Tier:";
            int labelWidth = half - 55;
            float scale = Math.min(1F, labelWidth / (float) Math.max(1, textRenderer.getWidth(name)));
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(cellX, y + 95);
            context.getMatrices().scale(scale, scale);
            context.drawTextWithShadow(textRenderer, name, 0, 0, 0xFFFFFFFF);
            context.getMatrices().popMatrix();
        }
        for (int i = 0; i < 6; i++) {
            context.drawTextWithShadow(textRenderer, "Ing " + (i + 1) + ":", x + 8 + (i % 2) * (half + 4),
                    y + 151 + (i / 2) * 25, swapFirst == i ? UiTheme.accent() : 0xFFFFFFFF);
        }
    }

    private void renderRecipeStats(DrawContext context, int x, int y, int w) {
        UiTheme.drawRoundedBox(context, x, y, w, page.summary().height(), UiTheme.SURFACE, UiTheme.BORDER);
        context.drawCenteredTextWithShadow(textRenderer, "Recipe Stats", x + w / 2, y + 10, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Crafting Materials:", x + 8, y + 27, 0xFFFFFFFF);
        for (int i = 0; i < 2; i++) {
            var material = recipe().materials().get(i);
            Text name = Text.literal("- " + material.amount() + "x " + material.name().replaceFirst("^Refined ", "") + " ")
                    .formatted(Formatting.WHITE)
                    .append(Text.literal("[").formatted(Formatting.GOLD))
                    .append(Text.literal("\u272B".repeat(i == 0 ? craft.material1() : craft.material2())).formatted(Formatting.YELLOW))
                    .append(Text.literal("]").formatted(Formatting.GOLD));
            List<OrderedText> lines = textRenderer.wrapLines(name, w - 16);
            for (int line = 0; line < Math.min(2, lines.size()); line++) {
                context.drawTextWithShadow(textRenderer, lines.get(line), x + 8, y + 40 + i * 23 + line * 11, 0xFFFFFFFF);
            }
        }
        int cellWidth = (w - 22) / 2;
        for (int i = 0; i < 6; i++) {
            int cellX = x + 8 + (i % 2) * (cellWidth + 6);
            int cellY = y + 94 + (i / 2) * 45;
            Ingredient ingredient = ingredients.get(craft.ingredients().get(i));
            UiTheme.drawRoundedBox(context, cellX, cellY, cellWidth, 41, UiTheme.SURFACE, UiTheme.BORDER);
            String name = ingredient.name();
            if (textRenderer.getWidth(name) > cellWidth - 10) {
                name = textRenderer.trimToWidth(name, cellWidth - 22) + "...";
            }
            context.drawTextWithShadow(textRenderer, name, cellX + 5, cellY + 5, ingredientColor(ingredient));
            int effectiveness = preview == null ? 0 : preview.stat("ingredientEffectiveness" + i);
            context.drawTextWithShadow(textRenderer, preview == null ? "[--]" : "[" + effectiveness + "%]",
                    cellX + 5, cellY + 25, preview == null ? 0xFFAAAAAA
                            : effectiveness < 0 ? 0xFFFF5555 : effectiveness == 0 ? 0xFFFFFFFF : 0xFF55FF55);
        }
    }

    private void drawRecipeIcon(DrawContext context, int x, int y, int size) {
        EquipmentSprites.draw(context, recipe().type(), x, y, size);
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
        if (WorkspaceTabs.scroll(parent, toolTabId, 12, 10, width - 24,
                mouseX, mouseY, horizontal, vertical)) return true;
        pageScroll.move((float) -vertical * 30, 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0 && click.y() >= viewportTop && click.y() < viewportBottom) {
            double x = click.x() - left - 12;
            double y = click.y() - viewportTop + scrollOffset;
            for (int i = 0; i < 6; i++) {
                var bounds = page.ingredients().get(i);
                if (x >= bounds.x() && x < bounds.right() && y >= bounds.y() && y < bounds.y() + 32) {
                    selectSlot(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (hashField.isFocused() && (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            importCode(hashField.getText());
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks >= 40) {
            workspace.updateCraft(toolTabId, craft);
            ticks = 0;
        }
    }

    @Override
    public void removed() {
        workspace.updateCraft(toolTabId, craft);
        super.removed();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static String title(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private record PageControl(ClickableWidget widget, int y, boolean enabled) {}
}
