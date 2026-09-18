package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTree;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTreeManager;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDefinition;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class AbilityTreeScreen extends Screen {
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 34;
    private static final int TREE_WIDTH = 430;
    private static final int SIDEBAR_WIDTH = 250;
    private static final int NODE_SIZE = 36;
    private static final int NODE_TEXTURE_SIZE = 60;
    private static final int CONNECTOR_TEXTURE_SIZE = 68;
    private static final int COLUMN_STEP = 40;
    private static final int ROW_STEP = 40;
    private static final int SECTION_COLOR = 0xFFE5D6A2;
    private static final int PRESET_PANEL_WIDTH = 390;
    private static final int PRESET_PANEL_HEIGHT = 270;
    private static final Identifier TREE_BACKGROUND = texture("treetabbackground.png");
    private static final Identifier PAGE_LINE = texture("pageline.png");

    private final Screen parent;
    private final AbilityTreeState state;
    private final int combatLevel;
    private final AbilityTreeClass abilityClass;
    private AbilityTreeDefinition definition;
    private AbilityTreeDefinition.Node hoveredNode;
    private final List<ThemedButton> presetButtons = new ArrayList<>();
    private List<SavedAbilityTree> visiblePresets = List.of();
    private TextFieldWidget presetField;
    private PresetOverlay presetOverlay = PresetOverlay.NONE;
    private String presetText = "";
    private int presetScroll;
    private final SmoothScroll scroll = new SmoothScroll(14.0F);
    private String status = "";
    private long statusUntil;
    private int workspaceTicks;

    public AbilityTreeScreen(
            Screen parent,
            AbilityTreeState state,
            AbilityTreeClass abilityClass,
            int combatLevel
    ) {
        super(Text.literal("Ability Tree"));
        this.parent = parent;
        this.state = state;
        this.abilityClass = abilityClass;
        this.combatLevel = combatLevel;
    }

    @Override
    protected void init() {
        clearChildren();
        presetButtons.clear();
        super.init();
        AbilityTreeDatabase.getInstance().loadAsync(abilityClass);
        SidebarBounds sidebar = sidebarBounds();
        int presetButtonY = this.height - FOOTER_HEIGHT - 28;
        int presetButtonWidth = (sidebar.width() - 24) / 2;
        if (presetOverlay == PresetOverlay.NONE) {
            this.addDrawableChild(new ThemedButton(
                    sidebar.x() + 10, presetButtonY, presetButtonWidth, 20,
                    Text.literal("Save"), () -> openPresetOverlay(PresetOverlay.SAVE)));
            this.addDrawableChild(new ThemedButton(
                    sidebar.x() + 14 + presetButtonWidth, presetButtonY, presetButtonWidth, 20,
                    Text.literal("Saved"), () -> openPresetOverlay(PresetOverlay.SAVED)));
            this.addDrawableChild(new ThemedButton(
                    10, this.height - 27, 64, 20, Text.literal("Reset"), () -> {
                        state.reset(abilityClass);
                        setStatus("Tree reset");
                    }));
            this.addDrawableChild(new ThemedButton(
                    78, this.height - 27, 64, 20, Text.literal("Copy"), this::copyTree))
                    .setTooltip(Tooltip.of(Text.literal(
                            "Copy a WynnBuilder tree code. Select at least the root ability.")));
            this.addDrawableChild(new ThemedButton(
                    146, this.height - 27, 64, 20, Text.literal("Paste"), this::pasteTree))
                    .setTooltip(Tooltip.of(Text.literal(
                            "Paste a WynnBuilder code for " + abilityClass.displayName()
                                    + " (2.2.3.0). Codes contain no class/version. Old WynnQOL codes also work.")));
            this.addDrawableChild(new ThemedButton(
                    this.width - 74, this.height - 27, 64, 20,
                    Text.literal("Done"), this::close));
        } else {
            initPresetOverlay();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        definition = AbilityTreeDatabase.getInstance().get(abilityClass);
        int contentWidth = Math.min(this.width - 16, TREE_WIDTH + SIDEBAR_WIDTH + 8);
        int left = (this.width - contentWidth) / 2;
        int treeWidth = Math.min(TREE_WIDTH, contentWidth - Math.min(SIDEBAR_WIDTH, contentWidth / 3));
        int sidebarX = left + treeWidth + 8;
        int sidebarWidth = contentWidth - treeWidth - 8;
        int top = HEADER_HEIGHT;
        int bottom = this.height - FOOTER_HEIGHT;

        UiTheme.drawPanel(context, left, top, treeWidth, bottom - top);
        UiTheme.drawPanel(context, sidebarX, top, sidebarWidth, bottom - top);
        drawTextureRegion(
                context, TREE_BACKGROUND,
                left + 2, top + 2, treeWidth - 4, bottom - top - 4,
                315, 23, 274, 174, 590, 220);

        hoveredNode = null;
        if (definition == null) {
            renderLoading(context, left, top, treeWidth, bottom);
        } else {
            renderTree(context, mouseX, mouseY, left, top, treeWidth, bottom);
            renderSidebar(context, sidebarX, top, sidebarWidth, bottom);
        }
        renderHeader(context);
        if (presetOverlay != PresetOverlay.NONE) {
            renderPresetOverlay(context);
        }
        super.render(context, mouseX, mouseY, delta);
        if (presetOverlay == PresetOverlay.NONE
                && definition != null
                && hoveredNode != null) {
            renderAbilityTooltip(context, mouseX, mouseY, left, top, treeWidth, bottom);
        }
    }

    private void renderLoading(DrawContext context, int left, int top, int width, int bottom) {
        AbilityTreeDatabase database = AbilityTreeDatabase.getInstance();
        String error = database.error(abilityClass);
        String message = error.isEmpty() ? "Loading live Wynncraft ability tree..." : error;
        context.drawCenteredTextWithShadow(
                this.textRenderer, message, left + width / 2, (top + bottom) / 2,
                error.isEmpty() ? 0xFFAAAAAA : 0xFFFF5555);
    }

    private void renderHeader(DrawContext context) {
        String points = definition == null
                ? ""
                : "  AP " + state.spent(definition) + "/"
                        + AbilityTreeState.abilityPointsForLevel(combatLevel);
        Text title = Text.literal(abilityClass.displayName() + " Ability Tree" + points)
                .styled(style -> style.withColor(abilityClass.color()));
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                title,
                this.width / 2, 16, 0xFFFFFFFF);
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer, status, this.width / 2, this.height - 18,
                    status.startsWith("Tree") ? 0xFF55FF55 : 0xFFFF5555);
        }
    }

    private void renderTree(
            DrawContext context,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int width,
            int bottom
    ) {
        context.enableScissor(left + 1, top + 1, left + width - 1, bottom - 1);
        int originX = left + Math.max(6, (width - (8 * COLUMN_STEP + NODE_SIZE)) / 2);
        int originY = top + 12 - Math.round(scroll.update());

        renderPageDividers(context, left, width, originY);

        for (AbilityTreeDefinition.Connector connector : definition.connectors()) {
            int centerX = originX + (connector.x() - 1) * COLUMN_STEP + NODE_SIZE / 2;
            int centerY = originY + (connector.y() - 1) * ROW_STEP + NODE_SIZE / 2;
            drawConnector(context, connector, centerX, centerY);
        }

        List<AbilityTreeDefinition.Node> ordered = definition.nodes().values().stream()
                .sorted(Comparator.comparingInt(AbilityTreeDefinition.Node::y)
                        .thenComparingInt(AbilityTreeDefinition.Node::x))
                .toList();
        Set<String> selected = state.selected(abilityClass);
        for (AbilityTreeDefinition.Node node : ordered) {
            int x = originX + (node.x() - 1) * COLUMN_STEP;
            int y = originY + (node.y() - 1) * ROW_STEP;
            if (y + NODE_SIZE < top || y > bottom) {
                continue;
            }
            boolean isSelected = selected.contains(node.id());
            boolean available = isAvailable(node);
            drawAbilityNode(context, x, y, node, isSelected, available);
            if (mouseX >= x && mouseX < x + NODE_SIZE
                    && mouseY >= y && mouseY < y + NODE_SIZE
                    && mouseY >= top && mouseY < bottom) {
                hoveredNode = node;
            }
        }
        context.disableScissor();
    }

    private void renderPageDividers(DrawContext context, int left, int width, int originY) {
        Map<Integer, Integer> pageRows = new TreeMap<>();
        for (AbilityTreeDefinition.Node node : definition.nodes().values()) {
            pageRows.merge(node.page(), node.y(), Math::min);
        }
        for (Map.Entry<Integer, Integer> page : pageRows.entrySet()) {
            int y = originY + (page.getValue() - 1) * ROW_STEP - 13;
            String label = "Page " + page.getKey();
            int labelX = left + 10;
            context.drawTextWithShadow(this.textRenderer, label, labelX, y - 4, 0xFF434654);
            int lineX = labelX + this.textRenderer.getWidth(label) + 7;
            drawTexture(
                    context, PAGE_LINE,
                    lineX, y - 3, Math.max(1, left + width - 10 - lineX), 8,
                    730, 16);
        }
    }

    private void drawAbilityNode(
            DrawContext context,
            int x,
            int y,
            AbilityTreeDefinition.Node node,
            boolean selected,
            boolean available
    ) {
        String iconName = node.iconName();
        String key = standardNodeTextureKey(iconName);
        int textureX = x - (NODE_TEXTURE_SIZE - NODE_SIZE) / 2;
        int textureY = y - (NODE_TEXTURE_SIZE - NODE_SIZE) / 2;
        if (key != null) {
            drawTexture(
                    context,
                    texture("node/" + key + (selected ? "_active" : "") + ".png"),
                    textureX, textureY, NODE_TEXTURE_SIZE, NODE_TEXTURE_SIZE,
                    32, 32);
            return;
        }
        if (iconName.startsWith("abilityTree.ultimate")) {
            drawTexture(
                    context,
                    texture("node/" + (selected ? "selected.png" : "node.png")),
                    textureX, textureY, NODE_TEXTURE_SIZE, NODE_TEXTURE_SIZE,
                    32, 32);
            String archetype = camelToSnake(
                    iconName.substring("abilityTree.ultimate".length()));
            drawTexture(
                    context,
                    texture("node/" + abilityClass.apiName() + "/" + archetype
                            + (selected ? "_selected" : "") + ".png"),
                    textureX, textureY, NODE_TEXTURE_SIZE, NODE_TEXTURE_SIZE,
                    32, 32);
        }
    }

    private void drawConnector(
            DrawContext context,
            AbilityTreeDefinition.Connector connector,
            int centerX,
            int centerY
    ) {
        Set<String> selected = state.selected(abilityClass);
        boolean active = connector.paths().stream()
                .anyMatch(path -> selected.contains(path.first()) && selected.contains(path.second()));
        String key = connectorTextureKey(connector.iconName());
        if (key == null) return;
        drawTexture(
                context,
                texture("connector/" + key + (active ? "_active" : "") + ".png"),
                centerX - CONNECTOR_TEXTURE_SIZE / 2,
                centerY - CONNECTOR_TEXTURE_SIZE / 2,
                CONNECTOR_TEXTURE_SIZE, CONNECTOR_TEXTURE_SIZE,
                32, 32);
    }

    private static String standardNodeTextureKey(String iconName) {
        return switch (iconName) {
            case "abilityTree.nodeWarrior" -> "warrior";
            case "abilityTree.nodeShaman" -> "shaman";
            case "abilityTree.nodeArcher" -> "archer";
            case "abilityTree.nodeMage" -> "mage";
            case "abilityTree.nodeAssassin" -> "assassin";
            case "abilityTree.nodeWhite" -> "white";
            case "abilityTree.nodeYellow" -> "yellow";
            case "abilityTree.nodeBlue" -> "blue";
            case "abilityTree.nodePurple" -> "purple";
            case "abilityTree.nodeRed" -> "red";
            default -> null;
        };
    }

    private static String connectorTextureKey(String iconName) {
        return switch (iconName) {
            case "connector_up_down" -> "vertical";
            case "connector_right_left" -> "horizontal";
            case "connector_down_left" -> "down_left";
            case "connector_right_down" -> "right_down";
            case "connector_right_down_left" -> "right_down_left";
            case "connector_up_right_down" -> "up_right_down";
            case "connector_up_down_left" -> "up_down_left";
            case "connector_up_right_left" -> "up_right_left";
            case "connector_up_right_down_left" -> "up_right_down_left";
            default -> null;
        };
    }

    private static String camelToSnake(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private static Identifier texture(String path) {
        return Identifier.of("wynnextras", "textures/gui/buildplanner/abilitytree/" + path);
    }

    private static void drawTexture(
            DrawContext context,
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x, y,
                0.0f, 0.0f,
                width, height,
                textureWidth, textureHeight,
                textureWidth, textureHeight);
    }

    private static void drawTextureRegion(
            DrawContext context,
            Identifier texture,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight,
            int textureWidth,
            int textureHeight
    ) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x, y,
                sourceX, sourceY,
                width, height,
                sourceWidth, sourceHeight,
                textureWidth, textureHeight);
    }

    private void renderSidebar(DrawContext context, int x, int top, int width, int bottom) {
        int textX = x + 10;
        int y = top + 10;
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Archetypes").formatted(Formatting.BOLD),
                textX, y, 0xFFFFFFFF);
        y += 16;
        Map<String, Integer> counts = state.archetypeCounts(definition);
        for (AbilityTreeDefinition.Archetype archetype : definition.archetypes().values()) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    archetype.name() + ": " + counts.getOrDefault(archetype.id(), 0),
                    textX, y, archetype.color());
            y += 13;
        }
        y += 9;
        context.drawTextWithShadow(
                this.textRenderer,
                "Use Save to store the current tree.",
                textX, y, 0xFFFFFFFF);
        context.drawTextWithShadow(
                this.textRenderer,
                "Use Saved to browse presets.",
                textX, y + 13, 0xFFAAAAAA);
        context.drawTextWithShadow(
                this.textRenderer,
                state.selected(abilityClass).size() + " selected abilities",
                textX, y + 31, abilityClass.color());
    }

    private void openPresetOverlay(PresetOverlay overlay) {
        presetOverlay = overlay;
        presetText = "";
        presetScroll = 0;
        init();
        setInitialFocus(presetField);
    }

    private void initPresetOverlay() {
        PresetPanel panel = presetPanel();
        presetField = new TextFieldWidget(
                textRenderer,
                panel.x() + 16,
                panel.y() + 38,
                panel.width() - 32,
                20,
                Text.literal(presetOverlay == PresetOverlay.SAVE
                        ? "Tree preset name" : "Search saved trees"));
        presetField.setPlaceholder(Text.literal(
                presetOverlay == PresetOverlay.SAVE
                        ? "Name (&a and \u00a7a colors supported)..."
                        : "Search saved trees..."));
        presetField.setMaxLength(64);
        presetField.setText(presetText);
        presetField.setChangedListener(value -> {
            presetText = value;
            presetScroll = 0;
            rebuildPresetButtons();
        });
        addDrawableChild(presetField);

        addDrawableChild(new ThemedButton(
                panel.x() + 16, panel.y() + panel.height() - 32, 80, 20,
                Text.literal("Back"), this::closePresetOverlay));
        if (presetOverlay == PresetOverlay.SAVE) {
            addDrawableChild(new ThemedButton(
                    panel.x() + panel.width() - 96,
                    panel.y() + panel.height() - 32,
                    80,
                    20,
                    Text.literal("Save"),
                    this::savePreset));
        } else {
            rebuildPresetButtons();
        }
    }

    private void closePresetOverlay() {
        presetOverlay = PresetOverlay.NONE;
        presetText = "";
        presetScroll = 0;
        init();
    }

    private void savePreset() {
        String name = presetField == null ? "" : presetField.getText().trim();
        if (MinecraftColorText.plain(name).isBlank()) {
            setStatus("Enter a preset name");
            return;
        }
        boolean saved = SavedAbilityTreeManager.getInstance().save(new SavedAbilityTree(
                name,
                abilityClass.apiName(),
                state.selected(abilityClass),
                System.currentTimeMillis()));
        if (!saved) {
            setStatus("Could not save tree preset");
            return;
        }
        closePresetOverlay();
        setStatus("Tree preset saved");
    }

    private void rebuildPresetButtons() {
        presetButtons.forEach(this::remove);
        presetButtons.clear();
        if (presetOverlay != PresetOverlay.SAVED || presetField == null) {
            visiblePresets = List.of();
            return;
        }
        String query = MinecraftColorText.plain(
                presetField.getText()).trim().toLowerCase(Locale.ROOT);
        visiblePresets = SavedAbilityTreeManager.getInstance().getAll(abilityClass).stream()
                .filter(tree -> query.isEmpty()
                        || MinecraftColorText.plain(tree.name())
                                .toLowerCase(Locale.ROOT).contains(query))
                .toList();
        PresetPanel panel = presetPanel();
        int top = panel.y() + 68;
        int bottom = panel.y() + panel.height() - 40;
        int rowHeight = 24;
        int visibleRows = Math.max(1, (bottom - top) / rowHeight);
        int maxScroll = Math.max(0, visiblePresets.size() - visibleRows);
        presetScroll = Math.min(presetScroll, maxScroll);
        int count = Math.min(visibleRows, visiblePresets.size() - presetScroll);
        for (int index = 0; index < count; index++) {
            SavedAbilityTree preset = visiblePresets.get(index + presetScroll);
            int y = top + index * rowHeight;
            Text label = MinecraftColorText.parse(preset.name())
                    .copy()
                    .append(Text.literal("  (" + preset.selectedNodes().size() + ")")
                            .formatted(Formatting.GRAY));
            ThemedButton equip = new ThemedButton(
                    panel.x() + 16, y, panel.width() - 66, rowHeight - 3,
                    label,
                    () -> equipPreset(preset));
            ThemedButton remove = new ThemedButton(
                    panel.x() + panel.width() - 42, y, 26, rowHeight - 3,
                    Text.literal("X").formatted(Formatting.RED),
                    () -> removePreset(preset));
            presetButtons.add(addDrawableChild(equip));
            presetButtons.add(addDrawableChild(remove));
        }
    }

    private void equipPreset(SavedAbilityTree preset) {
        if (definition == null) {
            setStatus("Ability tree is still loading");
            return;
        }
        String encoded = "wq-atree:1:" + abilityClass.apiName() + ":"
                + preset.selectedNodes().stream().sorted().collect(Collectors.joining(","));
        AbilityTreeState.SelectionResult result =
                state.importTree(definition, encoded, combatLevel);
        setStatus(result.changed() ? "Tree preset equipped" : result.message());
        if (result.changed()) {
            closePresetOverlay();
        }
    }

    private void removePreset(SavedAbilityTree preset) {
        if (!SavedAbilityTreeManager.getInstance().remove(preset)) {
            setStatus("Could not remove tree preset");
        }
        rebuildPresetButtons();
    }

    private void renderPresetOverlay(DrawContext context) {
        PresetPanel panel = presetPanel();
        context.fill(0, 0, width, height, 0x99000000);
        UiTheme.drawPanel(context, panel.x(), panel.y(), panel.width(), panel.height());
        String title = presetOverlay == PresetOverlay.SAVE
                ? "Save Ability Tree" : "Saved Ability Trees";
        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                panel.x() + panel.width() / 2,
                panel.y() + 14,
                UiTheme.accentRgb());
        if (presetOverlay == PresetOverlay.SAVE) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    state.selected(abilityClass).size() + " selected abilities",
                    panel.x() + panel.width() / 2,
                    panel.y() + 70,
                    abilityClass.color());
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    "Example: &aBoss Tree  or  \u00a7dMythic Tree",
                    panel.x() + panel.width() / 2,
                    panel.y() + 89,
                    0xFFAAAAAA);
            if (presetField != null && !presetField.getText().isBlank()) {
                context.drawCenteredTextWithShadow(
                        textRenderer,
                        MinecraftColorText.parse(presetField.getText()),
                        panel.x() + panel.width() / 2,
                        panel.y() + 110,
                        0xFFFFFFFF);
            }
        } else if (visiblePresets.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    presetText.isBlank()
                            ? "No saved trees yet."
                            : "No saved trees match this search.",
                    panel.x() + panel.width() / 2,
                    panel.y() + 110,
                    0xFFAAAAAA);
        }
    }

    private PresetPanel presetPanel() {
        int panelWidth = Math.min(PRESET_PANEL_WIDTH, width - 30);
        int panelHeight = Math.min(PRESET_PANEL_HEIGHT, height - 40);
        return new PresetPanel(
                (width - panelWidth) / 2,
                (height - panelHeight) / 2,
                panelWidth,
                panelHeight);
    }

    private SidebarBounds sidebarBounds() {
        int contentWidth = Math.min(this.width - 16, TREE_WIDTH + SIDEBAR_WIDTH + 8);
        int left = (this.width - contentWidth) / 2;
        int treeWidth = Math.min(
                TREE_WIDTH, contentWidth - Math.min(SIDEBAR_WIDTH, contentWidth / 3));
        int sidebarX = left + treeWidth + 8;
        return new SidebarBounds(sidebarX, contentWidth - treeWidth - 8);
    }

    private void renderAbilityTooltip(
            DrawContext context,
            int mouseX,
            int mouseY,
            int treeLeft,
            int treeTop,
            int treeWidth,
            int treeBottom
    ) {
        AbilityTreeDefinition.Node node = hoveredNode;
        int tooltipWidth = Math.min(416, treeWidth - 12);
        int textWidth = tooltipWidth - 12;
        List<ColoredLine> lines = organizedDescription(node, textWidth);
        appendTooltipRequirements(lines, node, textWidth);
        while (!lines.isEmpty() && lines.getLast().text().isEmpty()) {
            lines.removeLast();
        }

        int tooltipHeight = 45 + lines.size() * 11;
        int tooltipX = Math.max(
                treeLeft + 6,
                Math.min(mouseX + 12, treeLeft + treeWidth - tooltipWidth - 6));
        int tooltipY = mouseY + 14;
        if (tooltipY + tooltipHeight > treeBottom - 4) {
            tooltipY = mouseY - tooltipHeight - 10;
        }
        tooltipY = Math.max(treeTop + 4, tooltipY);

        context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF5101010);
        context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, 0xFFE0E0E0);
        context.fill(tooltipX, tooltipY + tooltipHeight - 1,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFE0E0E0);
        context.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, 0xFFE0E0E0);
        context.fill(tooltipX + tooltipWidth - 1, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFE0E0E0);

        int x = tooltipX + 6;
        int y = tooltipY + 5;
        context.drawTextWithShadow(
                textRenderer, Text.literal(node.name()).formatted(Formatting.BOLD),
                x, y, node.color());
        y += 16;
        for (ColoredLine line : lines) {
            if (!line.text().isEmpty()) {
                Text text = line.color() == SECTION_COLOR
                        ? Text.literal(line.text()).formatted(Formatting.BOLD)
                        : Text.literal(line.text());
                context.drawTextWithShadow(textRenderer, text, x, y, line.color());
            }
            y += 11;
        }

        int remaining = state.remaining(definition, combatLevel);
        boolean available = state.selected(abilityClass).contains(node.id()) || isAvailable(node);
        Text points = Text.literal(available ? "\u2714 " : "\u2718 ")
                .styled(style -> style.withColor(available ? 0x55FF55 : 0xFF5555))
                .append(Text.literal(
                                "Available: " + remaining + "  Cost: " + node.abilityPointCost())
                        .formatted(Formatting.GRAY));
        context.drawTextWithShadow(
                textRenderer, points, x, tooltipY + tooltipHeight - 14, 0xFFFFFFFF);
    }

    private List<ColoredLine> organizedDescription(
            AbilityTreeDefinition.Node node, int width
    ) {
        List<String> casting = new ArrayList<>();
        List<String> description = new ArrayList<>();
        List<String> stats = new ArrayList<>();
        boolean inStats = false;
        for (String rawLine : node.description()) {
            String line = rawLine.trim();
            if (line.isEmpty()
                    || startsWithAny(
                            line,
                            "Ability Points:",
                            "Required Ability:",
                            "Archetype Requirement:",
                            "Unlocking will block:")
                    || (!node.locks().isEmpty() && line.startsWith("- "))) {
                continue;
            }
            if (line.startsWith("Click Combo:")) {
                casting.add(line);
                continue;
            }
            if (isStatLine(line) || (inStats && line.startsWith("("))) {
                inStats = true;
                stats.add(line);
                continue;
            }
            description.add(line);
        }

        List<ColoredLine> lines = new ArrayList<>();
        appendSection(lines, "Casting", casting, width);
        appendSection(lines, "Description", description, width);
        appendSection(lines, "Stats", stats, width);
        return lines;
    }

    private void appendSection(
            List<ColoredLine> target,
            String heading,
            List<String> values,
            int width
    ) {
        if (values.isEmpty()) {
            return;
        }
        if (!target.isEmpty()) {
            target.add(new ColoredLine("", 0xFFCCCCCC));
        }
        target.add(new ColoredLine(heading, SECTION_COLOR));
        for (String value : values) {
            for (String line : wrap(value, width)) {
                target.add(new ColoredLine(line, descriptionColor(line)));
            }
        }
    }

    private static boolean isStatLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("mana cost:")
                || lower.contains("damage:")
                || lower.contains("range:")
                || lower.contains("area of effect:")
                || lower.contains("duration:")
                || lower.contains("healing:")
                || lower.contains("speed:")
                || lower.contains("knockback:")
                || lower.contains("charges:")
                || lower.contains("resistance:")
                || lower.contains("defense:");
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void appendTooltipRequirements(
            List<ColoredLine> lines,
            AbilityTreeDefinition.Node node,
            int width
    ) {
        List<ColoredLine> requirements = new ArrayList<>();
        if (node.combatLevel() > 0) {
            requirements.add(new ColoredLine("Combat Level: " + node.combatLevel(), 0xFFFFFF55));
        }
        if (!node.requiredNodes().isEmpty()) {
            for (String id : node.requiredNodes()) {
                AbilityTreeDefinition.Node required = definition.nodes().get(id);
                requirements.add(new ColoredLine(
                        "Required Ability: " + (required == null ? id : required.name()),
                        required == null ? 0xFFFFFF55 : required.color()));
            }
        }
        if (node.archetypeRequirement() != null) {
            AbilityTreeDefinition.ArchetypeRequirement required = node.archetypeRequirement();
            AbilityTreeDefinition.Archetype archetype = definition.archetypes().get(required.archetype());
            requirements.add(new ColoredLine(
                    "Archetype Requirement: " + required.amount() + " "
                            + (archetype == null ? required.archetype() : archetype.name()),
                    archetype == null ? 0xFFFFFF55 : archetype.color()));
        }
        for (String id : node.locks()) {
            AbilityTreeDefinition.Node locked = definition.nodes().get(id);
            requirements.add(new ColoredLine(
                    "Blocks Ability: " + (locked == null ? id : locked.name()),
                    0xFFFF5555));
        }
        if (requirements.isEmpty()) {
            return;
        }
        lines.add(new ColoredLine("", 0xFFCCCCCC));
        lines.add(new ColoredLine("Requirements", SECTION_COLOR));
        for (ColoredLine requirement : requirements) {
            for (String line : wrap(requirement.text(), width)) {
                lines.add(new ColoredLine(line, requirement.color()));
            }
        }
    }

    private int descriptionColor(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (AbilityTreeDefinition.Archetype archetype : definition.archetypes().values()) {
            if (lower.contains(archetype.name().toLowerCase(Locale.ROOT))) {
                return archetype.color();
            }
        }
        if (lower.contains("thunder") || lower.contains("dexterity")) {
            return 0xFFFFFF55;
        }
        if (lower.contains("earth") || lower.contains("strength")) {
            return 0xFF55FF55;
        }
        if (lower.contains("water") || lower.contains("intelligence")) {
            return 0xFF55FFFF;
        }
        if (lower.contains("fire") || lower.contains("defense")) {
            return 0xFFFF5555;
        }
        if (lower.contains("air") || lower.contains("agility")) {
            return 0xFFFFFFFF;
        }
        if (lower.startsWith("ability points:") || lower.startsWith("cost:")) {
            return 0xFF55FFFF;
        }
        return 0xFFCCCCCC;
    }

    private List<String> wrap(String text, int width) {
        int safeWidth = Math.max(40, width);
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && this.textRenderer.getWidth(candidate) > safeWidth) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private record ColoredLine(String text, int color) {
    }

    private record SidebarBounds(int x, int width) {
    }

    private record PresetPanel(int x, int y, int width, int height) {
    }

    private enum PresetOverlay {
        NONE,
        SAVE,
        SAVED
    }

    private boolean isAvailable(AbilityTreeDefinition.Node node) {
        if (definition == null || state.selected(abilityClass).contains(node.id())) {
            return true;
        }
        if (combatLevel < node.combatLevel()
                || state.remaining(definition, combatLevel) < node.abilityPointCost()) {
            return false;
        }
        Set<String> selected = state.selected(abilityClass);
        boolean lockConflict = node.locks().stream().anyMatch(selected::contains)
                || selected.stream()
                        .map(definition.nodes()::get)
                        .filter(selectedNode -> selectedNode != null)
                        .anyMatch(selectedNode -> selectedNode.locks().contains(node.id()));
        if (!selected.containsAll(node.requiredNodes()) || lockConflict) {
            return false;
        }
        AbilityTreeDefinition.ArchetypeRequirement required = node.archetypeRequirement();
        if (required != null
                && state.archetypeCounts(definition).getOrDefault(required.archetype(), 0) < required.amount()) {
            return false;
        }
        return node.id().equals(definition.rootId())
                || definition.adjacency().getOrDefault(node.id(), Set.of()).stream()
                        .filter(selected::contains)
                        .map(definition.nodes()::get)
                        .anyMatch(parent -> parent != null && parent.y() <= node.y());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (presetOverlay != PresetOverlay.NONE) {
            return super.mouseClicked(click, doubled);
        }
        if (definition != null && hoveredNode != null && click.button() == 0) {
            AbilityTreeState.SelectionResult result = state.toggle(
                    definition, hoveredNode.id(), combatLevel);
            if (!result.changed()) {
                setStatus(result.message());
            } else {
                status = "";
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (presetOverlay == PresetOverlay.SAVED) {
            PresetPanel panel = presetPanel();
            if (mouseX < panel.x()
                    || mouseX >= panel.x() + panel.width()
                    || mouseY < panel.y()
                    || mouseY >= panel.y() + panel.height()) {
                return true;
            }
            int visibleRows = Math.max(
                    1,
                    (panel.height() - 108) / 24);
            int maxScroll = Math.max(0, visiblePresets.size() - visibleRows);
            if (maxScroll > 0) {
                presetScroll = Math.max(
                        0,
                        Math.min(
                                maxScroll,
                                presetScroll - (int) Math.signum(verticalAmount)));
                rebuildPresetButtons();
            }
            return true;
        }
        if (presetOverlay != PresetOverlay.NONE) {
            return true;
        }
        if (definition == null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int viewportHeight = this.height - HEADER_HEIGHT - FOOTER_HEIGHT;
        int contentHeight = definition.maxY() * ROW_STEP + 24;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scroll.move((float) -Math.signum(verticalAmount) * 36.0F, 0.0F, maxScroll);
        return true;
    }

    private void setStatus(String message) {
        status = message;
        statusUntil = System.currentTimeMillis() + 3500;
    }

    private void copyTree() {
        if (this.client == null || definition == null) {
            return;
        }
        try {
            this.client.keyboard.setClipboard(state.exportTree(definition));
            setStatus("WynnBuilder tree copied");
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage());
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Failed to copy WynnBuilder ability tree.", exception);
            setStatus("Could not load WynnBuilder tree data. See the log.");
        }
    }

    private void pasteTree() {
        if (this.client == null || definition == null) {
            return;
        }
        try {
            AbilityTreeState.SelectionResult result = state.importTree(
                    definition, this.client.keyboard.getClipboard(), combatLevel);
            setStatus(result.changed() ? abilityClass.displayName() + " tree imported" : result.message());
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Failed to paste WynnBuilder ability tree.", exception);
            setStatus("Could not load WynnBuilder tree data. See the log.");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++workspaceTicks % 40 == 0 && parent instanceof WynnBuilderScreen builder) {
            builder.saveWorkspace();
        }
    }

    @Override
    public void removed() {
        if (parent instanceof WynnBuilderScreen builder) {
            builder.saveWorkspace();
        }
        super.removed();
    }

    @Override
    public void close() {
        if (presetOverlay != PresetOverlay.NONE) {
            closePresetOverlay();
            return;
        }
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
