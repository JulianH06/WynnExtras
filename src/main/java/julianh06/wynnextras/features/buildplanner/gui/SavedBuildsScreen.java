package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.features.buildplanner.config.SavedBuildManager;
import julianh06.wynnextras.features.buildplanner.config.SavedCraft;
import julianh06.wynnextras.features.buildplanner.config.SavedCraftManager;
import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.AspectDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDatabase;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeDefinition;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.data.TomeDatabase;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.features.buildplanner.data.WynnTome;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

final class SavedBuildsScreen extends PlannerScreen {
    private static final int BUILD_CARD_HEIGHT = 242;
    private static final int CRAFT_CARD_HEIGHT = 190;
    private static final int GAP = 10;
    private static final List<String> ARMOR_SLOTS = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");
    private static final List<String> ACCESSORY_SLOTS = List.of("RING_1", "RING_2", "BRACELET", "NECKLACE");
    private static final List<String> SLOTS = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "RING_1", "RING_2", "BRACELET", "NECKLACE", "WEAPON");
    private static final Identifier TOME_ICON =
            Identifier.of("wynnextras", "textures/gui/buildplanner/tomes.png");
    private static final Identifier ASPECT_ICON =
            Identifier.of("wynnextras", "textures/gui/buildplanner/aspects.png");
    private static final Identifier[] SKILL_ICONS = {
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/strength.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/dexterity.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/intelligence.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/defence.png"),
            Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/agility.png")
    };
    private static final int[] SKILL_COLORS = {
            0xFF55FF55, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFFFFFF
    };
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());
    private final Screen parent;
    private final Screen workspaceParent;
    private final SmoothScroll scroll = new SmoothScroll();
    private final PlannerScrollbar scrollbar = new PlannerScrollbar(scroll, 20);
    private final List<LibraryCard> cards = new ArrayList<>();
    private final List<LibraryCard> visibleCards = new ArrayList<>();
    private final List<ThemedButton> actions = new ArrayList<>();
    private final List<TextFieldWidget> nameFields = new ArrayList<>();
    private Map<Integer, CraftedItemCodec.Ingredient> ingredientChoices = Map.of();
    private Map<Integer, CraftedItemCodec.Recipe> recipeChoices = Map.of();
    private TextFieldWidget search;
    private boolean crafts;
    private String filter = "All";
    private boolean alphabetical;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int viewportTop;
    private int viewportBottom;
    private int columns;
    private int cardWidth;
    private int maxScroll;
    private long itemRevision;

    SavedBuildsScreen(BuilderScreen parent) {
        this(parent, parent.workspaceParent(), false);
    }

    SavedBuildsScreen(Screen parent, Screen workspaceParent, boolean crafts) {
        super(Text.literal("Saved Builds and Crafts"));
        this.parent = parent;
        this.workspaceParent = workspaceParent;
        this.crafts = crafts;
    }

    @Override
    protected void init() {
        String query = search == null ? "" : search.getText();
        super.init();
        clearChildren();
        actions.clear();
        panelWidth = Math.min(1060, width - 40);
        panelHeight = Math.min(760, height - 40);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        viewportTop = top + 76;
        viewportBottom = top + panelHeight - 42;
        columns = Math.max(1, (panelWidth - 32) / 310);
        cardWidth = (panelWidth - 40 - (columns - 1) * GAP) / columns;
        addButton(new ThemedButton(left + 16, top + 12, 142, 22,
                Text.literal("Builds (" + SavedBuildManager.getInstance().getAll().size() + ")"),
                () -> switchType(false)).setSelected(!crafts).setSelectedBorderHighlight());
        addButton(new ThemedButton(left + 164, top + 12, 142, 22,
                Text.literal("Crafts (" + SavedCraftManager.getInstance().getAll().size() + ")"),
                () -> switchType(true)).setSelected(crafts).setSelectedBorderHighlight());
        search = new TextFieldWidget(textRenderer, left + 16, top + 42, panelWidth - 344, 22,
                Text.literal("Search..."));
        search.setMaxLength(64);
        search.setPlaceholder(Text.literal("Search..."));
        search.setText(query);
        addDrawableChild(search);
        addButton(new ThemedButton(left + panelWidth - 320, top + 42, 170, 22,
                Text.literal((crafts ? "Type: " : "Class: ")
                        + (crafts ? readableMaterial(filter) : filter)), this::nextFilter));
        addButton(new ThemedButton(left + panelWidth - 144, top + 42, 128, 22,
                Text.literal(alphabetical ? "Sort: A-Z" : "Sort: Newest"), () -> {
                    alphabetical = !alphabetical;
                    scroll.jump(0);
                    init();
                }));
        addButton(new ThemedButton(left + panelWidth - 100, top + panelHeight - 30, 84, 20,
                Text.literal("Back"), this::close));
        loadCards();
        search.setChangedListener(value -> {
            scroll.jump(0);
            filterCards();
        });
    }

    private void switchType(boolean crafts) {
        if (this.crafts == crafts) return;
        this.crafts = crafts;
        filter = "All";
        scroll.jump(0);
        init();
    }

    private void nextFilter() {
        List<String> filters = new ArrayList<>(List.of("All"));
        if (crafts) cards.stream().map(card -> card.category).distinct().sorted().forEach(filters::add);
        else for (CharacterClass value : CharacterClass.playableValues()) filters.add(value.displayName());
        filter = filters.get((filters.indexOf(filter) + 1) % filters.size());
        scroll.jump(0);
        init();
    }

    private void loadCards() {
        cards.clear();
        if (crafts) {
            Map<Integer, CraftedItemCodec.Ingredient> choices = new LinkedHashMap<>();
            Map<Integer, CraftedItemCodec.Recipe> recipes = new LinkedHashMap<>();
            if (!SavedCraftManager.getInstance().getAll().isEmpty()) {
                try {
                    for (var ingredient : CraftedItemCodec.ingredientChoices()) choices.put(ingredient.id(), ingredient);
                    for (var recipe : CraftedItemCodec.recipeChoices()) recipes.put(recipe.id(), recipe);
                } catch (IllegalArgumentException | IllegalStateException ignored) {}
            }
            ingredientChoices = choices;
            recipeChoices = recipes;
            for (SavedCraft craft : SavedCraftManager.getInstance().getAll()) cards.add(new LibraryCard(null, craft));
        } else {
            for (SavedBuild build : SavedBuildManager.getInstance().getAll()) cards.add(new LibraryCard(build, null));
        }
        itemRevision = ItemDatabase.getInstance().revision();
        filterCards();
    }

    private void filterCards() {
        actions.forEach(this::removeButton);
        actions.clear();
        nameFields.forEach(this::remove);
        nameFields.clear();
        visibleCards.clear();
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        cards.stream().filter(card -> filter.equals("All") || filter.equals(card.category))
                .filter(card -> card.searchText.toLowerCase(Locale.ROOT).contains(query))
                .sorted(alphabetical ? Comparator.comparing(card -> card.name, String.CASE_INSENSITIVE_ORDER)
                        : Comparator.comparingLong((LibraryCard card) -> card.savedAt).reversed())
                .forEach(visibleCards::add);
        for (LibraryCard card : visibleCards) {
            card.open = addButton(new ThemedButton(0, 0, 72, 22, Text.literal("Open"), () -> open(card)));
            card.remove = addButton(new ThemedButton(0, 0, 60, 22, Text.literal("Delete"), () -> remove(card)));
            card.nameField = new TextFieldWidget(textRenderer, 0, 0, 100, 20, Text.literal("Saved name"));
            card.nameField.setMaxLength(64);
            card.nameField.setText(card.name);
            card.nameField.setCursor(0, false);
            card.nameField.setChangedListener(value -> rename(card, value));
            addDrawableChild(card.nameField);
            nameFields.add(card.nameField);
            actions.add(card.open);
            actions.add(card.remove);
        }
        int rows = (visibleCards.size() + columns - 1) / columns;
        maxScroll = Math.max(0, rows * (cardHeight() + GAP) - GAP - (viewportBottom - viewportTop));
        scroll.clamp(0, maxScroll);
        scrollbar.configure(left + panelWidth - 22, viewportTop, viewportBottom - viewportTop, maxScroll);
        positionCards();
    }

    private void positionCards() {
        for (int i = 0; i < visibleCards.size(); i++) {
            LibraryCard card = visibleCards.get(i);
            int x = left + 16 + i % columns * (cardWidth + GAP);
            int y = viewportTop + i / columns * (cardHeight() + GAP) - Math.round(scroll.position());
            card.setBounds(x, y, cardWidth, cardHeight());
            card.nameField.setPosition(x + 10, y + 9);
            card.nameField.setWidth(Math.max(80,
                    cardWidth - 30 - textRenderer.getWidth(card.headerInfo())));
            card.open.setPosition(x + 12, y + cardHeight() - 32);
            card.remove.setPosition(x + cardWidth - 72, y + cardHeight() - 32);
            updateCardControlVisibility(card);
        }
    }

    private void updateCardControlVisibility(LibraryCard card) {
        card.open.setVisible(intersectsViewport(card.open.getY(), card.open.getHeight()));
        card.remove.setVisible(intersectsViewport(card.remove.getY(), card.remove.getHeight()));
        card.nameField.visible = intersectsViewport(card.nameField.getY(), card.nameField.getHeight());
    }

    private boolean intersectsViewport(int y, int height) {
        return y < viewportBottom && y + height > viewportTop;
    }

    private void hideCardControls() {
        for (LibraryCard card : visibleCards) {
            card.open.setVisible(false);
            card.remove.setVisible(false);
            card.nameField.visible = false;
        }
    }

    private void restoreCardControlVisibility() {
        visibleCards.forEach(this::updateCardControlVisibility);
    }

    private int cardHeight() {
        return crafts ? CRAFT_CARD_HEIGHT : BUILD_CARD_HEIGHT;
    }

    private void rename(LibraryCard card, String name) {
        String cleaned = name.trim();
        if (cleaned.isEmpty() || cleaned.equals(card.name)) return;
        if (card.build != null) {
            SavedBuild renamed = SavedBuildManager.getInstance().rename(card.build, cleaned);
            if (renamed == null) {
                restoreName(card);
                return;
            }
            card.build = renamed;
        } else {
            SavedCraft renamed = SavedCraftManager.getInstance().rename(card.craft, cleaned);
            if (renamed == null) {
                restoreName(card);
                return;
            }
            card.craft = renamed;
        }
        card.name = cleaned;
        card.updateSearchText();
    }

    private void restoreName(LibraryCard card) {
        card.nameField.setText(card.name);
        card.nameField.setCursor(0, false);
    }

    private void open(LibraryCard card) {
        try {
            var workspace = ToolWorkspaceManager.getInstance();
            if (card.build != null) {
                String problem = buildProblem(card.build);
                if (!problem.isEmpty()) return;
                String id = workspace.add(ToolWorkspaceManager.Type.BUILDER);
                workspace.updateBuild(id, card.build);
            } else {
                var craft = CraftedItemCodec.readCraft(card.craft.code());
                CraftedItemCodec.preview(craft);
                String id = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
                workspace.updateCraft(id, craft);
                String name = card.name;
                int number = 2;
                while (hasTabName(workspace, id, name)) name = card.name.substring(0, Math.min(54, card.name.length()))
                        + " (" + number++ + ")";
                workspace.rename(id, name);
            }
            client.setScreen(WorkspaceTabs.open(workspaceParent));
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    private static boolean hasTabName(ToolWorkspaceManager workspace, String id, String name) {
        return workspace.tabs().stream().anyMatch(tab -> !tab.id().equals(id) && tab.name().equalsIgnoreCase(name));
    }

    private String buildProblem(SavedBuild build) {
        List<String> items = new ArrayList<>(build.equipment().values());
        build.offhands().forEach(offhand -> items.add(offhand.item()));
        for (String reference : items) {
            if (!CraftedItemCodec.isReference(reference) && !ItemDatabase.getInstance().isReady()) {
                return "The item database is still loading.";
            }
            if (ItemDatabase.getInstance().getItem(reference) == null) return "Missing item: " + reference;
        }
        for (var entry : build.tomes().entrySet()) {
            var tome = TomeDatabase.getInstance().get(entry.getValue());
            if (tome == null || !TomeScreen.accepts(entry.getKey(), tome)) return "Missing tome: " + entry.getValue();
        }
        for (var entry : build.aspects().entrySet()) {
            if (!TomeScreen.validAspectSlot(entry.getKey())
                    || AspectDatabase.getInstance().get(classFor(build.characterClass()), entry.getValue().name()) == null) {
                return "Missing aspect: " + entry.getValue().name();
            }
        }
        return "";
    }

    private void remove(LibraryCard card) {
        client.setScreen(new ThemedConfirmScreen(confirmed -> {
            if (confirmed) {
                if (card.build != null) SavedBuildManager.getInstance().remove(card.build);
                else SavedCraftManager.getInstance().remove(card.craft);
            }
            client.setScreen(this);
        }, Text.literal("Delete saved " + (card.build == null ? "craft?" : "build?")),
                Text.literal("Delete \"" + card.name + "\"?")));
    }

    @Override
    public void tick() {
        super.tick();
        if (itemRevision != ItemDatabase.getInstance().revision()) loadCards();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll > 0 && vertical != 0) {
            scroll.move((float) -vertical * 48, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Click logical = logicalClick(click);
        if (scrollbar.mouseClicked(logical.x(), logical.y(), logical.button())) return true;
        positionCards();
        if (logical.y() >= viewportTop && logical.y() < viewportBottom) {
            return super.mouseClicked(click, doubled);
        }
        hideCardControls();
        try {
            return super.mouseClicked(click, doubled);
        } finally {
            restoreCardControlVisibility();
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Click logical = logicalClick(click);
        if (scrollbar.mouseDragged(logical.x(), logical.y(), logical.button(),
                logicalCoordinate(deltaX), logicalCoordinate(deltaY))) return true;
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Click logical = logicalClick(click);
        if (scrollbar.mouseReleased(logical.x(), logical.y(), logical.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        UiTheme.drawPanel(context, ui, left, top, panelWidth, panelHeight);
        scroll.update(delta);
        positionCards();
        UiTheme.enableScissor(context, ui, left + 16, viewportTop, left + panelWidth - 26, viewportBottom);
        try {
            for (LibraryCard card : visibleCards) {
                if (card.getY() + cardHeight() > viewportTop && card.getY() < viewportBottom) {
                    card.draw(context, mouseX, mouseY, delta, ui);
                }
            }
        } finally {
            ui.disableScissor();
        }
        scrollbar.draw(context, mouseX, mouseY, delta, ui);
        if (visibleCards.isEmpty()) {
            ui.drawCenteredTextWithShadow(textRenderer, cards.isEmpty()
                    ? "No saved " + (crafts ? "crafts" : "builds") + " yet." : "No matches for these filters.",
                    width / 2, viewportTop + 70, UiTheme.TEXT);
            ui.drawCenteredTextWithShadow(textRenderer, cards.isEmpty()
                    ? "Use Save in the " + (crafts ? "Crafter" : "Builder") + " to start your collection."
                    : "Try another search or choose All.", width / 2, viewportTop + 90, UiTheme.MUTED);
        }
        hideCardControls();
        renderVanillaChildren(context, mouseX, mouseY, delta);
        restoreCardControlVisibility();
        UiTheme.enableScissor(context, ui, left + 16, viewportTop, left + panelWidth - 26, viewportBottom);
        try {
            renderVanillaChildren(context, mouseX, mouseY, delta);
        } finally {
            ui.disableScissor();
        }
    }

    @Override
    public void close() { client.setScreen(parent); }

    private String fit(String text, int width) {
        return textRenderer.getWidth(text) <= width ? text : textRenderer.trimToWidth(text, Math.max(0, width - 10)) + "...";
    }

    private static CharacterClass classFor(String name) {
        for (CharacterClass value : CharacterClass.playableValues()) {
            if (value.apiName().equalsIgnoreCase(name)) return value;
        }
        return CharacterClass.ARCHER;
    }

    private static int aspectSourceX(CharacterClass abilityClass) {
        return switch (abilityClass) {
            case WARRIOR -> 0;
            case ASSASSIN -> 16;
            case MAGE -> 32;
            case ARCHER, UNKNOWN -> 48;
            case SHAMAN -> 64;
        };
    }

    private static String readableMaterial(String name) {
        if (name == null || name.isBlank()) return "Unknown material";
        return java.util.Arrays.stream(name.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private final class LibraryCard extends Widget {
        private SavedBuild build;
        private SavedCraft craft;
        private String name;
        private final long savedAt;
        private final Map<String, WynnItem> equipment = new LinkedHashMap<>();
        private final List<CraftedItemCodec.Ingredient> ingredients = new ArrayList<>();
        private WynnItem result;
        private CraftedItemCodec.Craft recipe;
        private CraftedItemCodec.Recipe recipeDefinition;
        private String category;
        private String searchText;
        private String previewError = "";
        private ThemedButton open;
        private ThemedButton remove;
        private TextFieldWidget nameField;

        private LibraryCard(SavedBuild build, SavedCraft craft) {
            this.build = build;
            this.craft = craft;
            name = build != null ? build.name() : craft.name();
            savedAt = build != null ? build.savedAt() : craft.savedAt();
            category = build != null ? classFor(build.characterClass()).displayName() : "Unavailable";
            if (build != null) {
                for (String slot : SLOTS) {
                    String reference = build.equipment().get(slot);
                    WynnItem item = ItemDatabase.getInstance().getItem(reference);
                    equipment.put(slot, item);
                }
            } else {
                try {
                    recipe = CraftedItemCodec.readCraft(craft.code());
                    result = CraftedItemCodec.preview(recipe);
                    recipeDefinition = recipeChoices.get(recipe.recipeId());
                    category = result.subType();
                    for (int id : recipe.ingredients()) {
                        var ingredient = ingredientChoices.get(id);
                        ingredients.add(ingredient);
                    }
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    previewError = "Recipe unavailable: " + exception.getMessage();
                }
            }
            updateSearchText();
        }

        private void updateSearchText() {
            StringBuilder value = new StringBuilder(name).append(' ').append(category);
            for (Map.Entry<String, WynnItem> entry : equipment.entrySet()) {
                String reference = build == null ? null : build.equipment().get(entry.getKey());
                if (reference != null) value.append(' ').append(entry.getValue() == null
                        ? reference : entry.getValue().displayName());
            }
            for (var ingredient : ingredients) if (ingredient != null) value.append(' ').append(ingredient.name());
            if (result != null) value.append(' ').append(result.displayName());
            searchText = value.toString();
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean inside = mouseY >= viewportTop && mouseY < viewportBottom && contains(mouseX, mouseY);
            UiTheme.drawRoundedBox(context, ui, x, y, width, height, UiTheme.SURFACE,
                    inside ? UiTheme.accent() : UiTheme.BORDER);
            int color = build == null ? 0xFF55DDCC : 0xFF000000 | classFor(build.characterClass()).color();
            String subtitle = headerInfo();
            ui.drawTextWithShadow(textRenderer, subtitle, x + width - 10 - textRenderer.getWidth(subtitle), y + 15, color);
            if (build != null) drawBuild(context, mouseX, mouseY, inside);
            else drawCraft(context, mouseX, mouseY, inside);
            ui.drawTextWithShadow(textRenderer, DATE.format(Instant.ofEpochMilli(savedAt)),
                    x + 12, y + height - 48, UiTheme.DISABLED);
        }

        private String headerInfo() {
            return readableMaterial(category) + (build == null && result != null
                    ? ", Level " + result.stat("lvlLow") + "-" + result.stat("lvl")
                    : build != null ? ", Level " + build.level() : "");
        }

        private void drawBuild(DrawContext context, int mouseX, int mouseY, boolean inside) {
            int offhandCount = Math.min(3, build.offhands().size());
            int offhandWidth = offhandCount == 3 ? 64 : offhandCount > 0 ? 30 : 0;
            int loadoutWidth = 34 + 8 + 132 + (offhandCount > 0 ? 8 + offhandWidth : 0);
            int loadoutX = x + (width - loadoutWidth) / 2;
            drawEquipmentSlot(context, "WEAPON", equipment.get("WEAPON"), build.powders().get("WEAPON"),
                    loadoutX, y + 59, 34, inside, mouseX, mouseY);
            int gearX = loadoutX + 42;
            for (int index = 0; index < ARMOR_SLOTS.size(); index++) {
                String slot = ARMOR_SLOTS.get(index);
                drawEquipmentSlot(context, slot, equipment.get(slot), build.powders().get(slot),
                        gearX + index * 34, y + 44, 30, inside, mouseX, mouseY);
            }
            for (int index = 0; index < ACCESSORY_SLOTS.size(); index++) {
                String slot = ACCESSORY_SLOTS.get(index);
                drawEquipmentSlot(context, slot, equipment.get(slot), build.powders().get(slot),
                        gearX + index * 34, y + 78, 30, inside, mouseX, mouseY);
            }
            drawOffhands(context, gearX + 140, inside, mouseX, mouseY);
            int summaryX = x + (width - 126) / 2;
            drawBuildSummaryIcon(context, 0, summaryX, y + 118, inside, mouseX, mouseY);
            drawBuildSummaryIcon(context, 1, summaryX + 44, y + 118, inside, mouseX, mouseY);
            drawBuildSummaryIcon(context, 2, summaryX + 88, y + 118, inside, mouseX, mouseY);
            int[] skills = build.assignedSkills();
            int skillSize = 24;
            int skillGap = 10;
            int skillGroupWidth = skills.length * skillSize + (skills.length - 1) * skillGap;
            int skillX = x + (width - skillGroupWidth) / 2;
            for (int i = 0; i < skills.length; i++) {
                int centerX = skillX + i * (skillSize + skillGap) + skillSize / 2;
                ui.drawTexture(RenderPipelines.GUI_TEXTURED, SKILL_ICONS[i], centerX - skillSize / 2, y + 152,
                        0, 0, skillSize, skillSize, 16, 16, 16, 16);
                ui.drawCenteredTextWithShadow(textRenderer, Integer.toString(skills[i]), centerX, y + 179, SKILL_COLORS[i]);
            }
        }

        private void drawEquipmentSlot(DrawContext context, String slot, WynnItem item, String powders,
                int slotX, int slotY, int size, boolean inside, int mouseX, int mouseY) {
            UiTheme.drawRoundedBox(context, ui, slotX, slotY, size, size, UiTheme.PANEL,
                    item == null ? UiTheme.BORDER : ToolItemCards.color(item.tier()));
            if (item != null) {
                ItemStack stack = PlannerItemIcons.create(slot, item);
                if (stack.isEmpty()) {
                    EquipmentSprites.draw(context, item.subType(), slotX + 3, slotY + 3, size - 6);
                } else {
                    ui.pushMatrix();
                    ui.translate(slotX + 3, slotY + 3);
                    ui.scale((size - 6) / 16.0F, (size - 6) / 16.0F);
                    ui.drawItem(stack, 0, 0);
                    ui.popMatrix();
                }
            } else {
                ui.drawCenteredTextWithShadow(textRenderer, "-", slotX + size / 2, slotY + size / 2 - 4, UiTheme.DISABLED);
            }
            if (!inside || mouseX < slotX || mouseX >= slotX + size || mouseY < slotY || mouseY >= slotY + size) return;
            String reference = slot.startsWith("OFFHAND_")
                    ? build.offhands().get(Integer.parseInt(slot.substring(8)) - 1).item()
                    : build.equipment().get(slot);
            if (item != null) {
                WynnItemTooltip.render(SavedBuildsScreen.this, context, textRenderer, item, mouseX, mouseY,
                        classFor(build.characterClass()), build.level(), build.assignedSkills(), false);
            } else if (reference != null) {
                renderPlannerTooltip(context, List.of(Text.literal(reference).formatted(Formatting.RED),
                        Text.literal("Item data unavailable").formatted(Formatting.GRAY)), mouseX, mouseY);
            }
        }

        private void drawOffhands(DrawContext context, int offhandX, boolean inside, int mouseX, int mouseY) {
            int count = Math.min(3, build.offhands().size());
            for (int index = 0; index < count; index++) {
                int slotX = count == 3 && index == 2 ? offhandX + 34 : offhandX;
                int slotY = count == 1 || count == 3 && index == 2
                        ? y + 61 : y + 44 + index * 34;
                SavedBuild.SavedOffhand offhand = build.offhands().get(index);
                drawEquipmentSlot(context, "OFFHAND_" + (index + 1),
                        ItemDatabase.getInstance().getItem(offhand.item()), offhand.powders(),
                        slotX, slotY, 30, inside, mouseX, mouseY);
            }
        }

        private void drawBuildSummaryIcon(DrawContext context, int type, int iconX, int iconY,
                boolean inside, int mouseX, int mouseY) {
            UiTheme.drawRoundedBox(context, ui, iconX, iconY, 38, 28, UiTheme.PANEL, UiTheme.BORDER);
            int count;
            if (type == 0) {
                count = build.tomes().size();
                ui.drawTexture(RenderPipelines.GUI_TEXTURED, TOME_ICON, iconX + 4, iconY + 6,
                        64, 0, 16, 16, 16, 16, 80, 16);
            } else if (type == 1) {
                count = build.aspects().size();
                int sourceX = aspectSourceX(classFor(build.characterClass()));
                ui.drawTexture(RenderPipelines.GUI_TEXTURED, ASPECT_ICON, iconX + 4, iconY + 6,
                        sourceX, 0, 16, 16, 16, 16, 96, 16);
            } else {
                count = selectedTreeNodes().size();
                ui.drawItem(new ItemStack(Items.OAK_SAPLING), iconX + 4, iconY + 4);
            }
            ui.drawTextWithShadow(textRenderer, Integer.toString(count), iconX + (count > 9 ? 21 : 24), iconY + 10, UiTheme.TEXT);
            if (inside && mouseX >= iconX && mouseX < iconX + 38 && mouseY >= iconY && mouseY < iconY + 28) {
                renderPlannerTooltip(context, type == 0 ? tomeSummary() : type == 1 ? aspectSummary() : treeSummary(), mouseX, mouseY);
            }
        }

        private List<Text> tomeSummary() {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("Tomes " + build.tomes().size()).formatted(Formatting.AQUA, Formatting.BOLD));
            if (build.tomes().isEmpty()) {
                lines.add(Text.literal("No active tomes").formatted(Formatting.DARK_GRAY));
                return lines;
            }
            lines.add(Text.empty());
            build.tomes().forEach((slot, name) -> {
                WynnTome tome = TomeDatabase.getInstance().get(name);
                int color = tome == null ? UiTheme.TEXT : ToolItemCards.color(tome.tier());
                lines.add(Text.literal(name).styled(style -> style.withColor(color)));
            });
            return lines;
        }

        private List<Text> aspectSummary() {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("Aspects " + build.aspects().size()).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            if (build.aspects().isEmpty()) {
                lines.add(Text.literal("No active aspects").formatted(Formatting.DARK_GRAY));
                return lines;
            }
            lines.add(Text.empty());
            CharacterClass abilityClass = classFor(build.characterClass());
            build.aspects().forEach((slot, selection) -> {
                var aspect = AspectDatabase.getInstance().get(abilityClass, selection.name());
                int color = aspect == null ? UiTheme.TEXT : ToolItemCards.color(aspect.rarity());
                lines.add(Text.literal(selection.name()).styled(style -> style.withColor(color))
                        .append(Text.literal(" T" + selection.tier()).formatted(Formatting.GRAY)));
            });
            return lines;
        }

        private Set<String> selectedTreeNodes() {
            return build.abilityTrees().getOrDefault(classFor(build.characterClass()).apiName(), Set.of());
        }

        private List<Text> treeSummary() {
            List<Text> lines = new ArrayList<>();
            CharacterClass abilityClass = classFor(build.characterClass());
            lines.add(Text.literal("Ability Tree " + selectedTreeNodes().size())
                    .styled(style -> style.withColor(abilityClass.color()).withBold(true)));
            AbilityTreeDefinition definition = AbilityTreeDatabase.getInstance().getCachedOrLoadAsync(abilityClass);
            Set<String> selected = selectedTreeNodes();
            if (definition == null) {
                lines.add(Text.literal("Tree details are loading...").formatted(Formatting.DARK_GRAY));
                return lines;
            }
            lines.add(Text.empty());
            for (AbilityTreeDefinition.Archetype archetype : definition.archetypes().values()) {
                long count = selected.stream().map(definition.nodes()::get).filter(java.util.Objects::nonNull)
                        .filter(node -> archetype.id().equals(node.archetype())).count();
                lines.add(Text.literal(archetype.name()).styled(style -> style.withColor(archetype.color()))
                        .append(Text.literal(" " + count).formatted(Formatting.WHITE)));
            }
            List<String> red = activeSpecialNodes(definition, selected, false);
            List<String> ultimates = activeSpecialNodes(definition, selected, true);
            if (!red.isEmpty() || !ultimates.isEmpty()) {
                lines.add(Text.empty());
            }
            red.forEach(name -> lines.add(Text.literal(name).formatted(Formatting.RED)));
            ultimates.forEach(name -> lines.add(Text.literal(name).formatted(Formatting.DARK_PURPLE)));
            return lines;
        }

        private List<String> activeSpecialNodes(AbilityTreeDefinition definition, Set<String> selected, boolean ultimate) {
            return selected.stream().map(definition.nodes()::get).filter(java.util.Objects::nonNull)
                    .filter(node -> node.iconName().toLowerCase(Locale.ROOT).contains("ultimate") == ultimate)
                    .filter(node -> ultimate || node.color() == 0xFFFF5555)
                    .map(AbilityTreeDefinition.Node::name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }

        private void drawCraft(DrawContext context, int mouseX, int mouseY, boolean inside) {
            if (result == null) {
                ui.drawTextWithShadow(textRenderer, "Preview unavailable", x + 12, y + 54, 0xFFFFAA55);
                ui.drawTextWithShadow(textRenderer, "Saved recipe is kept.", x + 12, y + 72, UiTheme.MUTED);
                if (inside) renderPlannerTooltip(context, List.of(Text.literal(previewError)), mouseX, mouseY);
                return;
            }
            EquipmentSprites.draw(context, result.subType(), x + 12, y + 44, 30);
            String lifetime = result.type().equals("consumable")
                    ? "Duration " + result.stat("durationMin") + "-" + result.stat("durationMax")
                            + "s   " + result.stat("charges") + " charges"
                    : "Durability " + result.stat("durabilityMin") + "-" + result.stat("durabilityMax");
            ui.drawTextWithShadow(textRenderer, fit(lifetime, width - 64), x + 50, y + 46, UiTheme.TEXT);
            ui.drawTextWithShadow(textRenderer, fit(materialSummary(), width - 64), x + 50, y + 62, UiTheme.MUTED);
            if (inside && mouseX < x + 44 && mouseY >= y + 44 && mouseY < y + 76) {
                WynnItemTooltip.render(SavedBuildsScreen.this, context, textRenderer, result, mouseX, mouseY,
                        CharacterClass.ARCHER, 121, new int[5], false);
            }
            int cellWidth = (width - 24) / 2;
            for (int i = 0; i < ingredients.size(); i++) {
                var ingredient = ingredients.get(i);
                int cellX = x + 12 + i % 2 * cellWidth;
                int cellY = y + 82 + i / 2 * 18;
                UiTheme.drawRoundedBox(context, ui, cellX, cellY, cellWidth - 4, 16, UiTheme.PANEL, UiTheme.BORDER);
                String label = ingredient == null ? "Unavailable" : ingredient.id() == 4000 ? "Empty" : ingredient.name();
                ui.drawTextWithShadow(textRenderer, fit(label, cellWidth - 12), cellX + 4, cellY + 4,
                        ingredient == null ? UiTheme.DISABLED : ToolItemCards.color(Integer.toString(ingredient.tier())));
                if (inside && ingredient != null && mouseX >= cellX && mouseX < cellX + cellWidth - 4
                        && mouseY >= cellY && mouseY < cellY + 16) {
                    WynnItemTooltip.renderIngredient(SavedBuildsScreen.this, context, textRenderer, ingredient, mouseX, mouseY);
                }
            }
        }

        private String materialSummary() {
            if (recipeDefinition == null || recipeDefinition.materials().size() < 2) return "Materials unavailable";
            return materialName(recipeDefinition.materials().get(0).name()) + " T" + recipe.material1()
                    + " / " + materialName(recipeDefinition.materials().get(1).name()) + " T" + recipe.material2();
        }

        private String materialName(String name) {
            return readableMaterial(name == null ? null : name.replaceFirst("(?i)^refined\\s+", ""));
        }

    }
}
