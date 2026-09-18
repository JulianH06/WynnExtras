package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.SavedBuild;
import julianh06.wynnextras.features.buildplanner.config.SavedBuildManager;
import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.AspectDatabase;
import julianh06.wynnextras.features.buildplanner.data.ItemDatabase;
import julianh06.wynnextras.features.buildplanner.data.TomeDatabase;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class SavedBuildsScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int ROW_HEIGHT = 25;

    private final WynnBuilderScreen parent;
    private final List<ThemedButton> rowButtons = new ArrayList<>();
    private List<SavedBuild> visibleBuilds = List.of();
    private AbilityTreeClass filter;
    private ThemedButton filterButton;
    private TextFieldWidget searchField;
    private int scrollOffset;
    private String status = "";

    SavedBuildsScreen(WynnBuilderScreen parent) {
        super(Text.literal("Saved Builds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String previousSearch = searchField == null ? "" : searchField.getText();
        super.init();
        int left = (width - PANEL_WIDTH) / 2;
        int panelHeight = panelHeight();
        int top = (height - panelHeight) / 2;

        filterButton = new ThemedButton(
                left + 16, top + 34, 150, 20, filterLabel(), this::nextFilter);
        addDrawableChild(filterButton);
        searchField = new TextFieldWidget(
                textRenderer, left + 174, top + 34, PANEL_WIDTH - 190, 20,
                Text.literal("Search saved builds"));
        searchField.setPlaceholder(Text.literal("Search..."));
        searchField.setMaxLength(64);
        searchField.setText(previousSearch);
        searchField.setChangedListener(value -> {
            scrollOffset = 0;
            status = "";
            rebuildRows();
        });
        addDrawableChild(searchField);
        addDrawableChild(new ThemedButton(
                left + PANEL_WIDTH - 96, top + panelHeight - 30, 80, 20,
                Text.literal("Back"), this::close));
        rebuildRows();
    }

    private Text filterLabel() {
        return Text.literal("Class: " + (filter == null ? "All" : filter.displayName()));
    }

    private void nextFilter() {
        AbilityTreeClass[] classes = AbilityTreeClass.values();
        filter = filter == null ? classes[0]
                : filter.ordinal() == classes.length - 1 ? null : classes[filter.ordinal() + 1];
        filterButton.setMessage(filterLabel());
        scrollOffset = 0;
        status = "";
        rebuildRows();
    }

    private void rebuildRows() {
        rowButtons.forEach(this::remove);
        rowButtons.clear();

        visibleBuilds = SavedBuildManager.getInstance().getAll().stream()
                .filter(build -> filter == null
                        || filter.apiName().equalsIgnoreCase(build.characterClass()))
                .filter(build -> searchField == null || searchField.getText().isBlank()
                        || build.name().toLowerCase(java.util.Locale.ROOT)
                                .contains(searchField.getText().trim().toLowerCase(java.util.Locale.ROOT)))
                .toList();
        int visibleRows = visibleRows();
        int maxScroll = Math.max(0, visibleBuilds.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - panelHeight()) / 2 + 64;
        int count = Math.min(visibleRows, visibleBuilds.size() - scrollOffset);
        for (int i = 0; i < count; i++) {
            SavedBuild build = visibleBuilds.get(i + scrollOffset);
            AbilityTreeClass abilityClass = classForBuild(build);
            String fittedName = textRenderer.trimToWidth(build.name(), PANEL_WIDTH - 155);
            Text label = Text.literal(fittedName).formatted(Formatting.WHITE)
                    .append(Text.literal("  [" + abilityClass.displayName() + "]")
                            .styled(style -> style.withColor(abilityClass.color())));
            ThemedButton open = new ThemedButton(
                    left + 16, top + i * ROW_HEIGHT, PANEL_WIDTH - 64, ROW_HEIGHT - 3,
                    label, () -> open(build));
            ThemedButton remove = new ThemedButton(
                    left + PANEL_WIDTH - 42, top + i * ROW_HEIGHT, 26, ROW_HEIGHT - 3,
                    Text.literal("X").formatted(Formatting.RED), () -> remove(build));
            rowButtons.add(addDrawableChild(open));
            rowButtons.add(addDrawableChild(remove));
        }
    }

    private void open(SavedBuild build) {
        if (build.equipment().values().stream().anyMatch(value -> !CraftedItemCodec.isReference(value))
                && !ItemDatabase.getInstance().isReady()) {
            status = "The item database is still loading.";
            return;
        }
        List<String> missing = build.equipment().values().stream()
                .filter(name -> ItemDatabase.getInstance().getItem(name) == null)
                .toList();
        if (!missing.isEmpty()) {
            status = "Missing item: " + textRenderer.trimToWidth(missing.getFirst(), 220);
            return;
        }
        List<String> missingTomes = build.tomes().entrySet().stream()
                .filter(entry -> {
                    var tome = TomeDatabase.getInstance().get(entry.getValue());
                    return tome == null || !TomeScreen.accepts(entry.getKey(), tome);
                })
                .map(java.util.Map.Entry::getValue)
                .toList();
        if (!missingTomes.isEmpty()) {
            status = "Missing tome: " + textRenderer.trimToWidth(missingTomes.getFirst(), 220);
            return;
        }
        AbilityTreeClass abilityClass = classFor(build.characterClass());
        List<String> missingAspects = build.aspects().entrySet().stream()
                .filter(entry -> !TomeScreen.validAspectSlot(entry.getKey())
                        || AspectDatabase.getInstance().get(
                                abilityClass, entry.getValue().name()) == null)
                .map(entry -> entry.getValue().name())
                .toList();
        if (!missingAspects.isEmpty()) {
            status = "Missing aspect: "
                    + textRenderer.trimToWidth(missingAspects.getFirst(), 210);
            return;
        }
        parent.loadBuild(build);
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void remove(SavedBuild build) {
        client.setScreen(new ThemedConfirmScreen(confirmed -> {
            if (confirmed) {
                status = SavedBuildManager.getInstance().remove(build)
                        ? "" : "Could not update the saved build file.";
            }
            client.setScreen(this);
        }, Text.literal("Delete saved build?"),
                Text.literal("Delete \"" + build.name() + "\"? This cannot be undone. Open tabs are not deleted.")));
    }

    private static AbilityTreeClass classFor(String name) {
        for (AbilityTreeClass abilityClass : AbilityTreeClass.values()) {
            if (abilityClass.apiName().equalsIgnoreCase(name)) {
                return abilityClass;
            }
        }
        return AbilityTreeClass.ARCHER;
    }

    private static AbilityTreeClass classForBuild(SavedBuild build) {
        String weaponName = build.equipment().get("WEAPON");
        var weapon = ItemDatabase.getInstance().getItem(weaponName);
        return weapon == null
                ? classFor(build.characterClass())
                : AbilityTreeClass.fromWeaponSubtype(weapon.subType());
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontalAmount, double verticalAmount
    ) {
        int maxScroll = Math.max(0, visibleBuilds.size() - visibleRows());
        if (maxScroll > 0) {
            scrollOffset = Math.max(
                    0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
            rebuildRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - PANEL_WIDTH) / 2;
        int panelHeight = panelHeight();
        int top = (height - panelHeight) / 2;
        UiTheme.drawPanel(context, left, top, PANEL_WIDTH, panelHeight);
        context.drawCenteredTextWithShadow(
                textRenderer, "Saved Builds", width / 2, top + 14, UiTheme.accentRgb());
        if (visibleBuilds.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    searchField != null && !searchField.getText().isBlank()
                            ? "No saved builds match this search."
                            : "No builds match this class.",
                    width / 2, top + 138, 0xFFAAAAAA);
        }
        if (!status.isEmpty()) {
            context.drawTextWithShadow(
                    textRenderer, status, left + 16, top + panelHeight - 24, 0xFFFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private int visibleRows() {
        return Math.max(3, Math.min(8, (height - 120) / ROW_HEIGHT));
    }

    private int panelHeight() {
        return 104 + visibleRows() * ROW_HEIGHT;
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
