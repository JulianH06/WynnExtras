package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.AbilityTreeClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class SaveBuildScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 154;

    private final WynnBuilderScreen parent;
    private AbilityTreeClass selectedClass;
    private TextFieldWidget nameField;
    private ThemedButton classButton;
    private ThemedButton saveButton;
    private String status = "";

    SaveBuildScreen(WynnBuilderScreen parent, AbilityTreeClass defaultClass) {
        super(Text.literal("Save Build"));
        this.parent = parent;
        this.selectedClass = defaultClass;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        nameField = new TextFieldWidget(
                textRenderer, left + 18, top + 42, PANEL_WIDTH - 36, 20, Text.literal("Build name"));
        nameField.setPlaceholder(Text.literal("Build name"));
        nameField.setMaxLength(64);
        nameField.setChangedListener(value -> saveButton.active = !value.trim().isEmpty());
        addDrawableChild(nameField);

        classButton = new ThemedButton(
                left + 18, top + 72, PANEL_WIDTH - 36, 20,
                classLabel(), this::nextClass);
        addDrawableChild(classButton);

        addDrawableChild(new ThemedButton(
                left + 18, top + PANEL_HEIGHT - 30, 92, 20,
                Text.literal("Cancel"), this::close));
        saveButton = new ThemedButton(
                left + PANEL_WIDTH - 110, top + PANEL_HEIGHT - 30, 92, 20,
                Text.literal("Save"), this::save);
        saveButton.active = false;
        addDrawableChild(saveButton);
        setInitialFocus(nameField);
    }

    private Text classLabel() {
        return Text.literal("Class: " + selectedClass.displayName());
    }

    private void nextClass() {
        AbilityTreeClass[] classes = AbilityTreeClass.values();
        selectedClass = classes[(selectedClass.ordinal() + 1) % classes.length];
        classButton.setMessage(classLabel());
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            status = "Enter a build name.";
            return;
        }
        if (parent.saveBuild(name, selectedClass)) {
            close();
        } else {
            status = "Could not write the saved build file.";
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        UiTheme.drawPanel(context, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        context.drawCenteredTextWithShadow(
                textRenderer, "Save Build", width / 2, top + 14, UiTheme.accentRgb());
        if (!status.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, status, width / 2, top + 98, 0xFFFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
