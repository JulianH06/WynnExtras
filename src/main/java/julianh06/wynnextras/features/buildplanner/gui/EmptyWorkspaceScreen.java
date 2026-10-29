package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

final class EmptyWorkspaceScreen extends PlannerScreen {
    private final Screen parent;
    private final ToolWorkspaceManager.Type type;
    private final List<ThemedButton> controls = new ArrayList<>();

    EmptyWorkspaceScreen(Screen parent, ToolWorkspaceManager.Type type) {
        super(Text.literal(WorkspaceTabs.toolName(type)));
        this.parent = parent;
        this.type = type;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        rootWidgets.clear();
        controls.clear();
        WorkspaceTabs.addEmpty(parent, type, 12, 8, width - 24, this::control);
        addRootWidget(new EmptyMessage(type, width, height));
        control(new ThemedButton(
                (width - 130) / 2, height / 2 + 30, 130, 20,
                Text.literal("Create " + singular(type)), () -> WorkspaceNavigation.add(parent, type)));
    }

    private void control(ThemedButton button) {
        controls.add(addDrawableChild(button));
    }

    @Override
    protected void drawPlannerContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.fill(0, 0, width, height, UiTheme.BACKGROUND);
        for (ThemedButton control : controls) control.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private static String singular(ToolWorkspaceManager.Type type) {
        return switch (type) {
            case BUILDER -> "Build";
            case CRAFTER -> "Craft";
            case ATLAS -> "Atlas tab";
        };
    }

    private static final class EmptyMessage extends Widget {
        private final ToolWorkspaceManager.Type type;

        private EmptyMessage(ToolWorkspaceManager.Type type, int width, int height) {
            super(0, 0, width * 2, height * 2);
            this.type = type;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            float centerX = width / 2.0F;
            float centerY = height / 2.0F;
            ui.drawCenteredText("No " + WorkspaceTabs.toolName(type) + " tabs are open.",
                    centerX, centerY - 24, CustomColor.fromInt(UiTheme.TEXT), 3.0F);
            ui.drawCenteredText("Use + or the button below to start a new one.",
                    centerX, centerY + 4, CustomColor.fromInt(UiTheme.MUTED), 2.0F);
        }
    }
}
