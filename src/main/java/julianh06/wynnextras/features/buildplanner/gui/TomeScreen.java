package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import julianh06.wynnextras.features.buildplanner.data.AspectDatabase;
import julianh06.wynnextras.features.buildplanner.data.AspectSelection;
import julianh06.wynnextras.features.buildplanner.data.TomeDatabase;
import julianh06.wynnextras.features.buildplanner.data.WynnAspect;
import julianh06.wynnextras.features.buildplanner.data.WynnTome;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TomeScreen extends Screen {
    private static final Slot[] TOME_SLOTS = {
            new Slot("weaponTome1", "Weapon Tome", "weaponTome"),
            new Slot("weaponTome2", "Weapon Tome", "weaponTome"),
            new Slot("armorTome1", "Armor Tome", "armorTome"),
            new Slot("armorTome2", "Armor Tome", "armorTome"),
            new Slot("armorTome3", "Armor Tome", "armorTome"),
            new Slot("armorTome4", "Armor Tome", "armorTome"),
            new Slot("gatherXpTome1", "Marathon Tome", "gatherXpTome"),
            new Slot("gatherXpTome2", "Marathon Tome", "gatherXpTome"),
            new Slot("dungeonXpTome1", "Mysticism Tome", "dungeonXpTome"),
            new Slot("dungeonXpTome2", "Mysticism Tome", "dungeonXpTome"),
            new Slot("mobXpTome1", "Expertise Tome", "mobXpTome"),
            new Slot("mobXpTome2", "Expertise Tome", "mobXpTome"),
            new Slot("guildTome1", "Guild Tome", "guildTome"),
            new Slot("lootrunTome1", "Lootrun Tome", "lootrunTome")
    };
    private static final int PANEL_WIDTH = 600;
    private static final int ASPECT_SLOT_COUNT = 5;
    private static final int ASPECT_ROWS_TOP = 292;
    private static final int ASPECT_ROW_STEP = 34;
    private static final int ICON_SIZE = 24;
    private static final int TOME_ICON_GAP = 36;
    private static final Identifier ASPECT_ICONS =
            Identifier.of("wynnextras", "textures/gui/buildplanner/aspects.png");
    private static final Identifier TOME_ICONS =
            Identifier.of("wynnextras", "textures/gui/buildplanner/tomes.png");

    private final WynnBuilderScreen parent;
    private Slot selectingTome;
    private String selectingAspectSlot;
    private TextFieldWidget searchField;
    private String searchText = "";
    private String status = "";
    private int workspaceTicks;
    private final SmoothScroll resultScroll = new SmoothScroll(16.0F);

    TomeScreen(WynnBuilderScreen parent) {
        super(Text.literal("Tomes & Aspects"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        if (selectingTome != null) {
            initTomeSelector(left, panelWidth);
            return;
        }
        if (selectingAspectSlot != null) {
            initAspectSelector(left, panelWidth);
            return;
        }
        initCombinedMenu(left, panelWidth);
    }

    private void initCombinedMenu(int left, int panelWidth) {
        int slotWidth = (panelWidth - 38) / 2;
        for (int index = 0; index < TOME_SLOTS.length; index++) {
            Slot slot = TOME_SLOTS[index];
            int column = index % 2;
            int row = index / 2;
            WynnTome selected = parent.selectedTome(slot.key());
            String value = selected == null
                    ? "No Tome"
                    : selected.effectSummary().isBlank()
                            ? selected.displayName()
                            : selected.effectSummary();
            int buttonX = left + 12 + column * (slotWidth + 14);
            int labelX = selected == null ? buttonX : buttonX + TOME_ICON_GAP;
            int labelWidth = selected == null ? slotWidth : slotWidth - TOME_ICON_GAP;
            String label = slot.label() + ": " + value;
            String clipped = textRenderer.trimToWidth(label, Math.max(20, labelWidth - 12));
            addDrawableChild(new ThemedButton(
                    labelX, 46 + row * 32,
                    labelWidth, 26,
                    Text.literal(clipped),
                    () -> openTomeSelector(slot)));
        }

        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        if (abilityClass != null) {
            int rowWidth = panelWidth - 24;
            for (int index = 0; index < ASPECT_SLOT_COUNT; index++) {
                String slot = aspectSlot(index);
                AspectSelection selection = parent.selectedAspect(slot);
                int y = ASPECT_ROWS_TOP + index * ASPECT_ROW_STEP;
                String name = selection == null
                        ? "No Aspect"
                        : selection.aspect().displayName();
                addDrawableChild(new ThemedButton(
                        left + 48, y, rowWidth - 182, 28,
                        Text.literal((index + 1) + ". " + textRenderer.trimToWidth(
                                name, rowWidth - 208)),
                        () -> openAspectSelector(slot)));
                String tier = selection == null
                        ? "-"
                        : selection.tier() + "/" + selection.aspect().tiers().size();
                addDrawableChild(new ThemedButton(
                        left + rowWidth - 126, y, 68, 28,
                        Text.literal("Tier " + tier), () -> cycleAspectTier(slot)));
                addDrawableChild(new ThemedButton(
                        left + rowWidth - 54, y, 42, 28,
                        Text.literal("Clear"), () -> {
                            parent.selectAspect(slot, null);
                            init();
                        }));
            }
        }

        addDrawableChild(new ThemedButton(
                left + panelWidth - 90, 22, 78, 20,
                Text.literal("Done"), () -> client.setScreen(parent)));
    }

    private void initTomeSelector(int left, int panelWidth) {
        searchField = new TextFieldWidget(
                textRenderer, left + 12, 44, panelWidth - 108, 20,
                Text.literal("Search tomes"));
        searchField.setPlaceholder(Text.literal("Search " + selectingTome.label() + "..."));
        configureSearchField();
        addDrawableChild(new ThemedButton(
                left + panelWidth - 88, 44, 76, 20, Text.literal("Remove"),
                () -> {
                    parent.selectTome(selectingTome.key(), null);
                    selectingTome = null;
                    init();
                }));
        addBackButton(left);
    }

    private void initAspectSelector(int left, int panelWidth) {
        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        searchField = new TextFieldWidget(
                textRenderer, left + 12, 44, panelWidth - 24, 20,
                Text.literal("Search aspects"));
        searchField.setPlaceholder(Text.literal(
                "Search " + (abilityClass == null ? "" : abilityClass.displayName() + " ")
                        + "aspects..."));
        configureSearchField();
        addBackButton(left);
    }

    private void configureSearchField() {
        searchField.setText(searchText);
        searchField.setChangedListener(value -> {
            searchText = value;
            resultScroll.jump(0.0F);
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);
    }

    private void addBackButton(int left) {
        addDrawableChild(new ThemedButton(
                left + 12, height - 32, 76, 20, Text.literal("Back"),
                () -> {
                    selectingTome = null;
                    selectingAspectSlot = null;
                    status = "";
                    init();
                }));
    }

    private void openTomeSelector(Slot slot) {
        selectingTome = slot;
        selectingAspectSlot = null;
        resetSearch();
        init();
    }

    private void openAspectSelector(String slot) {
        selectingAspectSlot = slot;
        selectingTome = null;
        resetSearch();
        init();
    }

    private void resetSearch() {
        searchText = "";
        status = "";
        resultScroll.jump(0.0F);
    }

    private void cycleAspectTier(String slot) {
        AspectSelection selection = parent.selectedAspect(slot);
        if (selection == null) {
            return;
        }
        int tier = selection.tier() >= selection.aspect().tiers().size()
                ? 1 : selection.tier() + 1;
        parent.selectAspect(slot, new AspectSelection(selection.aspect(), tier));
        init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        UiTheme.drawPanel(context, left, 18, panelWidth, height - 28);

        Text title;
        int titleColor = 0xFFE5D6A2;
        if (selectingTome != null) {
            title = Text.literal("Choose " + selectingTome.label());
        } else if (selectingAspectSlot != null) {
            title = Text.literal("Choose Aspect");
            AbilityTreeClass abilityClass = parent.selectedAbilityClass();
            if (abilityClass != null) {
                titleColor = abilityClass.color();
            }
        } else {
            title = Text.literal("Tomes & Aspects");
        }
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 26, titleColor);

        if (selectingTome == null && selectingAspectSlot == null) {
            renderAspectHeading(context, left, panelWidth);
        }
        super.render(context, mouseX, mouseY, delta);
        if (selectingTome != null) {
            renderTomeResults(context, left, panelWidth, mouseX, mouseY);
        } else if (selectingAspectSlot != null) {
            renderAspectResults(context, left, panelWidth, mouseX, mouseY);
        } else {
            renderSelectedTomeIcons(context, left, panelWidth);
            renderSelectedAspectIcons(context, left, mouseX, mouseY);
        }
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, status, width / 2, height - 30, 0xFFFF5555);
        }
    }

    private void renderAspectHeading(DrawContext context, int left, int panelWidth) {
        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        if (abilityClass == null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    "Equip a weapon to choose aspects.",
                    left + panelWidth / 2,
                    ASPECT_ROWS_TOP + 12,
                    0xFF777777);
            return;
        }
        context.drawCenteredTextWithShadow(
                textRenderer,
                abilityClass.displayName() + " Aspects",
                left + panelWidth / 2,
                ASPECT_ROWS_TOP - 17,
                abilityClass.color());
    }

    private void renderSelectedTomeIcons(
            DrawContext context, int left, int panelWidth
    ) {
        int slotWidth = (panelWidth - 38) / 2;
        for (int index = 0; index < TOME_SLOTS.length; index++) {
            WynnTome tome = parent.selectedTome(TOME_SLOTS[index].key());
            if (tome == null) {
                continue;
            }
            int column = index % 2;
            int row = index / 2;
            int x = left + 16 + column * (slotWidth + 14);
            int y = 47 + row * 32;
            drawGlow(context, x, y, tierColor(tome.tier()));
            drawTomeIcon(context, x, y);
        }
    }

    private void renderSelectedAspectIcons(
            DrawContext context, int left, int mouseX, int mouseY
    ) {
        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        if (abilityClass == null) {
            return;
        }
        AspectSelection hovered = null;
        for (int index = 0; index < ASPECT_SLOT_COUNT; index++) {
            AspectSelection selection = parent.selectedAspect(aspectSlot(index));
            if (selection == null) {
                continue;
            }
            int x = left + 18;
            int y = ASPECT_ROWS_TOP + 2 + index * ASPECT_ROW_STEP;
            int color = rarityColor(selection.aspect().rarity());
            drawGlow(context, x, y, color);
            drawAspectIcon(context, abilityClass, x, y);
            if (mouseX >= x - 3 && mouseX < x + ICON_SIZE + 3
                    && mouseY >= y - 3 && mouseY < y + ICON_SIZE + 3) {
                hovered = selection;
            }
        }
        if (hovered != null) {
            renderAspectTooltip(context, hovered, mouseX, mouseY);
        }
    }

    private void renderTomeResults(
            DrawContext context, int left, int panelWidth, int mouseX, int mouseY
    ) {
        List<WynnTome> results = TomeDatabase.getInstance().search(
                selectingTome.type(), searchField == null ? "" : searchField.getText());
        int top = 72;
        int bottom = height - 38;
        int rowHeight = 24;
        int visibleRows = Math.max(1, (bottom - top) / rowHeight);
        resultScroll.clamp(0.0F, Math.max(0, results.size() - visibleRows));
        float scrollRows = resultScroll.update();
        int firstIndex = (int) Math.floor(scrollRows);
        context.enableScissor(left + 12, top, left + panelWidth - 12, bottom);
        for (int index = firstIndex;
                index < results.size() && index <= firstIndex + visibleRows;
                index++) {
            WynnTome tome = results.get(index);
            int y = top + Math.round((index - scrollRows) * rowHeight);
            if (y + rowHeight <= top || y >= bottom) {
                continue;
            }
            boolean hovered = mouseX >= left + 12 && mouseX < left + panelWidth - 12
                    && mouseY >= y && mouseY < y + rowHeight - 2;
            context.fill(left + 12, y, left + panelWidth - 12, y + rowHeight - 2,
                    hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE);
            context.drawTextWithShadow(
                    textRenderer,
                    textRenderer.trimToWidth(tome.nameWithStats(), panelWidth - 132),
                    left + 20, y + 7, tierColor(tome.tier()));
            context.drawTextWithShadow(
                    textRenderer, "Lv. " + tome.level(),
                    left + panelWidth - 56, y + 7, 0xFFAAAAAA);
        }
        context.disableScissor();
        if (results.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, "No matching tomes", width / 2, top + 14, 0xFFAAAAAA);
        }
    }

    private void renderAspectResults(
            DrawContext context, int left, int panelWidth, int mouseX, int mouseY
    ) {
        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        if (abilityClass == null) {
            return;
        }
        List<WynnAspect> results = AspectDatabase.getInstance().search(
                abilityClass, searchField == null ? "" : searchField.getText());
        int top = 72;
        int bottom = height - 64;
        int rowHeight = 36;
        int visibleRows = Math.max(1, (bottom - top) / rowHeight);
        resultScroll.clamp(0.0F, Math.max(0, results.size() - visibleRows));
        float scrollRows = resultScroll.update();
        int firstIndex = (int) Math.floor(scrollRows);
        context.enableScissor(left + 12, top, left + panelWidth - 12, bottom);
        for (int index = firstIndex;
                index < results.size() && index <= firstIndex + visibleRows;
                index++) {
            WynnAspect aspect = results.get(index);
            int y = top + Math.round((index - scrollRows) * rowHeight);
            if (y + rowHeight <= top || y >= bottom) {
                continue;
            }
            boolean hovered = mouseX >= left + 12 && mouseX < left + panelWidth - 12
                    && mouseY >= y && mouseY < y + rowHeight - 3;
            context.fill(
                    left + 12, y, left + panelWidth - 12, y + rowHeight - 3,
                    hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE);
            context.drawTextWithShadow(
                    textRenderer,
                    textRenderer.trimToWidth(aspect.displayName(), panelWidth - 128),
                    left + 20, y + 5, rarityColor(aspect.rarity()));
            context.drawTextWithShadow(
                    textRenderer,
                    aspect.rarity() + "  T" + aspect.tiers().size(),
                    left + panelWidth - 108, y + 5, 0xFFAAAAAA);
            context.drawTextWithShadow(
                    textRenderer,
                    textRenderer.trimToWidth(
                            aspect.tier(aspect.tiers().size()).description(),
                            panelWidth - 48),
                    left + 20, y + 18, 0xFF999999);
        }
        context.disableScissor();
        if (results.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, "No matching aspects", width / 2, top + 14, 0xFFAAAAAA);
        }
    }

    private void drawGlow(DrawContext context, int x, int y, int color) {
        int transparent = (color & 0x00FFFFFF) | 0x40000000;
        context.fill(x - 3, y - 3, x + ICON_SIZE + 3, y + ICON_SIZE + 3, transparent);
        context.fill(x - 1, y - 1, x + ICON_SIZE + 1, y, color);
        context.fill(x - 1, y + ICON_SIZE, x + ICON_SIZE + 1, y + ICON_SIZE + 1, color);
        context.fill(x - 1, y, x, y + ICON_SIZE, color);
        context.fill(x + ICON_SIZE, y, x + ICON_SIZE + 1, y + ICON_SIZE, color);
    }

    private void drawAspectIcon(
            DrawContext context, AbilityTreeClass abilityClass, int x, int y
    ) {
        int sourceX = switch (abilityClass) {
            case WARRIOR -> 0;
            case ASSASSIN -> 16;
            case MAGE -> 32;
            case ARCHER -> 48;
            case SHAMAN -> 64;
        };
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                ASPECT_ICONS,
                x, y,
                sourceX, 0,
                ICON_SIZE, ICON_SIZE,
                16, 16,
                96, 16);
    }

    private void drawTomeIcon(DrawContext context, int x, int y) {
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TOME_ICONS,
                x, y,
                64, 0,
                ICON_SIZE, ICON_SIZE,
                16, 16,
                80, 16);
    }

    private void renderAspectTooltip(
            DrawContext context, AspectSelection selection, int mouseX, int mouseY
    ) {
        int tooltipWidth = Math.min(330, width - 20);
        List<String> lines = wrap(
                selection.selectedTier().description(), tooltipWidth - 16);
        int tooltipHeight = 34 + lines.size() * 11;
        int x = Math.max(6, Math.min(mouseX + 12, width - tooltipWidth - 6));
        int y = Math.max(6, Math.min(mouseY + 12, height - tooltipHeight - 6));
        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xF5101010);
        context.fill(x, y, x + tooltipWidth, y + 1,
                rarityColor(selection.aspect().rarity()));
        context.drawTextWithShadow(
                textRenderer, selection.aspect().displayName(),
                x + 7, y + 6, rarityColor(selection.aspect().rarity()));
        context.drawTextWithShadow(
                textRenderer, "Tier " + selection.tier(),
                x + 7, y + 18, 0xFFAAAAAA);
        int lineY = y + 30;
        for (String line : lines) {
            context.drawTextWithShadow(textRenderer, line, x + 7, lineY, 0xFFCCCCCC);
            lineY += 11;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && textRenderer.getWidth(candidate) > maxWidth) {
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

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (selectingTome != null && clickTomeResult(click)) {
            return true;
        }
        if (selectingAspectSlot != null && clickAspectResult(click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean clickTomeResult(Click click) {
        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        int top = 72;
        int bottom = height - 38;
        if (click.x() < left + 12 || click.x() >= left + panelWidth - 12
                || click.y() < top || click.y() >= bottom) {
            return false;
        }
        List<WynnTome> results = TomeDatabase.getInstance().search(
                selectingTome.type(), searchField == null ? "" : searchField.getText());
        int index = (int) Math.floor(
                resultScroll.position() + (click.y() - top) / 24.0D);
        if (index < 0 || index >= results.size()) {
            return false;
        }
        parent.selectTome(selectingTome.key(), results.get(index));
        selectingTome = null;
        init();
        return true;
    }

    private boolean clickAspectResult(Click click) {
        AbilityTreeClass abilityClass = parent.selectedAbilityClass();
        if (abilityClass == null) {
            return false;
        }
        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        int left = (width - panelWidth) / 2;
        int top = 72;
        int bottom = height - 64;
        int visibleRows = Math.max(1, (bottom - top) / 36);
        if (click.x() < left + 12 || click.x() >= left + panelWidth - 12
                || click.y() < top || click.y() >= top + visibleRows * 36
                || ((int) click.y() - top) % 36 >= 33) {
            return false;
        }
        List<WynnAspect> results = AspectDatabase.getInstance().search(
                abilityClass, searchField == null ? "" : searchField.getText());
        int index = (int) Math.floor(
                resultScroll.position() + (click.y() - top) / 36.0D);
        if (index < 0 || index >= results.size()) {
            return false;
        }
        WynnAspect aspect = results.get(index);
        String error = parent.selectAspect(
                selectingAspectSlot,
                new AspectSelection(aspect, aspect.tiers().size()));
        if (!error.isEmpty()) {
            status = error;
            return true;
        }
        selectingAspectSlot = null;
        init();
        return true;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontal, double vertical
    ) {
        if (selectingTome != null || selectingAspectSlot != null) {
            int rowHeight = selectingTome != null ? 24 : 36;
            int bottom = selectingTome != null ? height - 38 : height - 64;
            int visibleRows = Math.max(1, (bottom - 72) / rowHeight);
            int resultCount;
            if (selectingTome != null) {
                resultCount = TomeDatabase.getInstance().search(
                        selectingTome.type(),
                        searchField == null ? "" : searchField.getText()).size();
            } else {
                AbilityTreeClass abilityClass = parent.selectedAbilityClass();
                resultCount = abilityClass == null
                        ? 0
                        : AspectDatabase.getInstance().search(
                                abilityClass,
                                searchField == null ? "" : searchField.getText()).size();
            }
            resultScroll.move(
                    vertical < 0 ? 1.0F : -1.0F,
                    0.0F,
                    Math.max(0, resultCount - visibleRows));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void tick() {
        super.tick();
        if (++workspaceTicks % 40 == 0) {
            parent.saveWorkspace();
        }
    }

    @Override
    public void removed() {
        parent.saveWorkspace();
        super.removed();
    }

    @Override
    public void close() {
        if (selectingTome != null || selectingAspectSlot != null) {
            selectingTome = null;
            selectingAspectSlot = null;
            status = "";
            init();
        } else if (client != null) {
            client.setScreen(parent);
        }
    }

    private static int tierColor(String tier) {
        return switch (tier.toLowerCase(java.util.Locale.ROOT)) {
            case "mythic" -> 0xFFAA00AA;
            case "fabled" -> 0xFFFF5555;
            case "legendary" -> 0xFF55FFFF;
            case "rare" -> 0xFFFF55FF;
            case "unique" -> 0xFFFFFF55;
            default -> 0xFFFFFFFF;
        };
    }

    private static int rarityColor(String rarity) {
        return switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "mythic" -> 0xFFAA00AA;
            case "fabled" -> 0xFFFF5555;
            case "legendary" -> 0xFF55FFFF;
            default -> 0xFFFFFFFF;
        };
    }

    static boolean accepts(String slotKey, WynnTome tome) {
        for (Slot slot : TOME_SLOTS) {
            if (slot.key().equals(slotKey) && slot.type().equals(tome.type())) {
                return true;
            }
        }
        return false;
    }

    static boolean validAspectSlot(String slot) {
        return slot != null && slot.matches("aspect[1-5]");
    }

    private static String aspectSlot(int index) {
        return "aspect" + (index + 1);
    }

    private record Slot(String key, String label, String type) {
    }
}
