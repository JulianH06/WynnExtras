package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.SavedCraft;
import julianh06.wynnextras.features.buildplanner.config.SavedCraftManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class SaveCraftScreen extends PlannerScreen {
    private final Screen parent;
    private final String code;
    private String name;
    private String status = "Saved recipes stay available after closing their tabs.";
    private TextFieldWidget nameField;
    private ThemedButton saveButton;

    SaveCraftScreen(Screen parent, String name, String code) {
        super(Text.literal("Save Crafted"));
        this.parent = parent;
        this.name = name;
        this.code = code;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        int left = (width - 380) / 2;
        int top = (height - 144) / 2;
        nameField = new TextFieldWidget(textRenderer, left + 16, top + 42, 348, 20, Text.literal("Craft name"));
        nameField.setMaxLength(64);
        nameField.setText(name);
        addDrawableChild(nameField);
        addButton(new ThemedButton(left + 16, top + 108, 90, 20, Text.literal("Cancel"), this::close));
        saveButton = addButton(new ThemedButton(left + 274, top + 108, 90, 20, Text.literal("Save"), () -> {
            if (SavedCraftManager.getInstance().contains(name)) {
                client.setScreen(new ThemedConfirmScreen(confirmed -> {
                    client.setScreen(this);
                    if (confirmed) save();
                }, Text.literal("Replace saved crafted?"), Text.literal("Replace \"" + name.trim() + "\"?")));
            } else save();
        }));
        saveButton.setEnabled(!name.isBlank());
        nameField.setChangedListener(value -> {
            name = value;
            saveButton.setEnabled(!value.isBlank());
        });
        setInitialFocus(nameField);
    }

    private void save() {
        if (SavedCraftManager.getInstance().save(new SavedCraft(name, code, System.currentTimeMillis()))) close();
        else status = "Kept in memory only. Could not write saved-crafts.json.";
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - 380) / 2;
        int top = (height - 144) / 2;
        UiTheme.drawPanel(context, ui, left, top, 380, 144);
        ui.drawCenteredTextWithShadow(textRenderer, "Save Crafted", width / 2, top + 16, UiTheme.accent());
        ui.drawCenteredTextWithShadow(textRenderer, status, width / 2, top + 80, UiTheme.MUTED);
        renderVanillaChildren(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() { client.setScreen(parent); }
}
