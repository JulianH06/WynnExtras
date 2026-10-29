package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTree;
import julianh06.wynnextras.features.buildplanner.config.SavedAbilityTreeManager;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDefinition;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeState;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.utils.UI.AbilityTreeWidget;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class AbilityTreeScreen extends PlannerScreen {
    private static final int HEADER_HEIGHT = 42;
    private static final int FOOTER_HEIGHT = 34;
    private static final int TREE_WIDTH = 430;
    private static final int SIDEBAR_WIDTH = 250;
    private static final int ROW_STEP = 40;
    private static final int SECTION_COLOR = 0xFFE5D6A2;
    private static final int PRESET_PANEL_WIDTH = 390;
    private static final int PRESET_PANEL_HEIGHT = 270;
    private static final Identifier TREE_BACKGROUND = Identifier.of(
            "wynnextras", "textures/gui/profileviewer/treetabbackground.png");

    private final Screen parent;
    private final AbilityTreeState state;
    private final int combatLevel;
    private final CharacterClass abilityClass;
    private AbilityTreeDefinition definition;
    private AbilityTreeDefinition.Node hoveredNode;
    private final AbilityTreeWidget abilityWidget;
    private final List<ThemedButton> presetButtons = new ArrayList<>();
    private List<SavedAbilityTree> visiblePresets = List.of();
    private TextFieldWidget presetField;
    private PresetOverlay presetOverlay = PresetOverlay.NONE;
    private String presetText = "";
    private int presetScroll;
    private final SmoothScroll scroll = new SmoothScroll();
    private String status = "";
    private long statusUntil;
    private int workspaceTicks;

    public AbilityTreeScreen(
            Screen parent,
            AbilityTreeState state,
            CharacterClass abilityClass,
            int combatLevel
    ) {
        super(Text.literal("Ability Tree"));
        this.parent = parent;
        this.state = state;
        this.abilityClass = abilityClass;
        this.combatLevel = combatLevel;
        this.abilityWidget = new AbilityTreeWidget(abilityClass.apiName(), 0, 0, 0, 0, 0);
        this.abilityWidget.setNodeClickListener(this::toggleNode);
        addRootWidget(abilityWidget);
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
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        definition = AbilityTreeDatabase.getInstance().get(abilityClass);
        int contentWidth = Math.min(this.width - 16, TREE_WIDTH + SIDEBAR_WIDTH + 8);
        int left = (this.width - contentWidth) / 2;
        int treeWidth = Math.min(TREE_WIDTH, contentWidth - Math.min(SIDEBAR_WIDTH, contentWidth / 3));
        int sidebarX = left + treeWidth + 8;
        int sidebarWidth = contentWidth - treeWidth - 8;
        int top = HEADER_HEIGHT;
        int bottom = this.height - FOOTER_HEIGHT;

        UiTheme.drawPanel(ui, left, top, treeWidth, bottom - top);
        UiTheme.drawPanel(ui, sidebarX, top, sidebarWidth, bottom - top);
        drawTextureRegion(
                context, TREE_BACKGROUND,
                left + 2, top + 2, treeWidth - 4, bottom - top - 4,
                315, 23, 274, 174, 590, 220);

        abilityWidget.setVisible(presetOverlay == PresetOverlay.NONE);
        if (definition == null || AbilityTreeCache.getClassMap(abilityClass.apiName()) == null) {
            renderLoading(context, left, top, treeWidth, bottom);
        } else {
            configureAbilityWidget(left, top, treeWidth, bottom, delta);
            renderSidebar(context, sidebarX, top, sidebarWidth, bottom);
        }
        renderHeader(context);
        if (presetOverlay != PresetOverlay.NONE) {
            renderPresetOverlay(context);
        }
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    private void configureAbilityWidget(int left, int top, int treeWidth, int bottom, float delta) {
        AbilityMapData classMap = AbilityTreeCache.getClassMap(abilityClass.apiName());
        if (classMap == null) return;
        int treeContentWidth = 375;
        int widgetX = (left + Math.max(4, (treeWidth - treeContentWidth) / 2)) * 2 - 925;
        int widgetY = top * 2 - 30;
        int viewportHeight = (bottom - top) * 2;
        abilityWidget.setViewport(widgetX, widgetY, treeWidth * 2, viewportHeight, viewportHeight);
        if (abilityWidget.classTree != classMap) abilityWidget.setClassTree(classMap);
        abilityWidget.setScrollOffset(Math.round(scroll.update(delta) * 2));
        abilityWidget.setSelectedIds(state.selected(abilityClass));
    }

    @Override
    protected void drawPlannerForeground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (presetOverlay != PresetOverlay.NONE || definition == null) return;
        AbilityMapData.Node hovered = abilityWidget.getCurrentHoveredNode();
        hoveredNode = hovered == null || hovered.meta == null
                ? null : definition.nodes().get(hovered.meta.id);
        if (hoveredNode == null) return;
        int contentWidth = Math.min(this.width - 16, TREE_WIDTH + SIDEBAR_WIDTH + 8);
        int left = (this.width - contentWidth) / 2;
        int treeWidth = Math.min(TREE_WIDTH, contentWidth - Math.min(SIDEBAR_WIDTH, contentWidth / 3));
        renderAbilityTooltip(context, mouseX, mouseY, left, HEADER_HEIGHT, treeWidth,
                this.height - FOOTER_HEIGHT);
    }

    private void toggleNode(String nodeId) {
        if (definition == null || presetOverlay != PresetOverlay.NONE) return;
        AbilityTreeState.SelectionResult result = state.toggle(definition, nodeId, combatLevel);
        if (!result.changed()) setStatus(result.message());
        else status = "";
        abilityWidget.setSelectedIds(state.selected(abilityClass));
    }

    private void renderLoading(DrawContext context, int left, int top, int width, int bottom) {
        AbilityTreeDatabase database = AbilityTreeDatabase.getInstance();
        String error = database.error(abilityClass);
        String message = error.isEmpty() ? "Loading live Wynncraft ability tree..." : error;
        ui.drawCenteredTextWithShadow(
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
        ui.drawCenteredTextWithShadow(
                this.textRenderer,
                title,
                this.width / 2, 16, 0xFFFFFFFF);
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            ui.drawCenteredTextWithShadow(
                    this.textRenderer, status, this.width / 2, this.height - 18,
                    status.startsWith("Tree") ? 0xFF55FF55 : 0xFFFF5555);
        }
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
        var ui = new julianh06.wynnextras.utils.UI.UIUtils(context, 1.0, 0, 0);
        ui.drawTexture(
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
        ui.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Archetypes").formatted(Formatting.BOLD),
                textX, y, 0xFFFFFFFF);
        y += 16;
        Map<String, Integer> counts = state.archetypeCounts(definition);
        for (AbilityTreeDefinition.Archetype archetype : definition.archetypes().values()) {
            ui.drawTextWithShadow(
                    this.textRenderer,
                    archetype.name() + ": " + counts.getOrDefault(archetype.id(), 0),
                    textX, y, archetype.color());
            y += 13;
        }
        y += 9;
        ui.drawTextWithShadow(
                this.textRenderer,
                "Use Save to store the current tree.",
                textX, y, 0xFFFFFFFF);
        ui.drawTextWithShadow(
                this.textRenderer,
                "Use Saved to browse presets.",
                textX, y + 13, 0xFFAAAAAA);
        ui.drawTextWithShadow(
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
        ui.fill(0, 0, width, height, 0x99000000);
        UiTheme.drawPanel(ui, panel.x(), panel.y(), panel.width(), panel.height());
        String title = presetOverlay == PresetOverlay.SAVE
                ? "Save Ability Tree" : "Saved Ability Trees";
        ui.drawCenteredTextWithShadow(
                textRenderer,
                title,
                panel.x() + panel.width() / 2,
                panel.y() + 14,
                UiTheme.accentRgb());
        if (presetOverlay == PresetOverlay.SAVE) {
            ui.drawCenteredTextWithShadow(
                    textRenderer,
                    state.selected(abilityClass).size() + " selected abilities",
                    panel.x() + panel.width() / 2,
                    panel.y() + 70,
                    abilityClass.color());
            ui.drawCenteredTextWithShadow(
                    textRenderer,
                    "Example: &aBoss Tree  or  \u00a7dMythic Tree",
                    panel.x() + panel.width() / 2,
                    panel.y() + 89,
                    0xFFAAAAAA);
            if (presetField != null && !presetField.getText().isBlank()) {
                ui.drawCenteredTextWithShadow(
                        textRenderer,
                        MinecraftColorText.parse(presetField.getText()),
                        panel.x() + panel.width() / 2,
                        panel.y() + 110,
                        0xFFFFFFFF);
            }
        } else if (visiblePresets.isEmpty()) {
            ui.drawCenteredTextWithShadow(
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

        ui.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF5101010);
        ui.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, 0xFFE0E0E0);
        ui.fill(tooltipX, tooltipY + tooltipHeight - 1,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFE0E0E0);
        ui.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, 0xFFE0E0E0);
        ui.fill(tooltipX + tooltipWidth - 1, tooltipY,
                tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFFE0E0E0);

        int x = tooltipX + 6;
        int y = tooltipY + 5;
        ui.drawTextWithShadow(
                textRenderer, Text.literal(node.name()).formatted(Formatting.BOLD),
                x, y, node.color());
        y += 16;
        for (ColoredLine line : lines) {
            if (!line.text().isEmpty()) {
                Text text = line.color() == SECTION_COLOR
                        ? Text.literal(line.text()).formatted(Formatting.BOLD)
                        : Text.literal(line.text());
                ui.drawTextWithShadow(textRenderer, text, x, y, line.color());
            }
            y += 11;
        }

        int remaining = state.remaining(definition, combatLevel);
        boolean selected = state.selected(abilityClass).contains(node.id());
        Text points = Text.literal(selected ? "\u2714 " : "\u25CB ")
                .styled(style -> style.withColor(selected ? 0x55FF55 : 0xAAAAAA))
                .append(Text.literal(
                                "Available: " + remaining + "  Cost: " + node.abilityPointCost())
                        .formatted(Formatting.GRAY));
        ui.drawTextWithShadow(
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

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        double logicalX = logicalCoordinate(mouseX);
        double logicalY = logicalCoordinate(mouseY);
        if (presetOverlay == PresetOverlay.SAVED) {
            PresetPanel panel = presetPanel();
            if (logicalX < panel.x()
                    || logicalX >= panel.x() + panel.width()
                    || logicalY < panel.y()
                    || logicalY >= panel.y() + panel.height()) {
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
