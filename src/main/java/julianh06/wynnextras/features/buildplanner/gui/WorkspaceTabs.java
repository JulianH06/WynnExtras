package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import java.util.function.Consumer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class WorkspaceTabs {
    private static final int TAB_GAP = 3;
    private static final int TAB_WIDTH = 118;
    private static final int CONTROL_HEIGHT = 22;
    private static final int CONTROL_GAP = 4;
    private static final int TAB_HEIGHT = CONTROL_HEIGHT;
    private static final int TAB_STRIP_HEIGHT = TAB_HEIGHT + 4;
    private static final float CONTROL_TEXT_SCALE = 1.25F;
    private static final float TAB_TEXT_SCALE = 1.2F;
    private static final Map<ToolWorkspaceManager.Type, SmoothScroll> SCROLL =
            new EnumMap<>(ToolWorkspaceManager.Type.class);

    private WorkspaceTabs() {}

    public static Screen open(Screen parent) {
        var workspace = ToolWorkspaceManager.getInstance();
        var tab = workspace.activeTab();
        if (tab == null) return new EmptyWorkspaceScreen(parent, workspace.activeType());
        return switch (tab.type()) {
            case BUILDER -> new BuilderScreen(parent, tab.id());
            case CRAFTER -> new CrafterScreen(parent, tab.id());
            case ATLAS -> new AtlasScreen(parent, tab.id());
        };
    }

    public static Screen open(Screen parent, ToolWorkspaceManager.Type type) {
        ToolWorkspaceManager.getInstance().showType(type);
        return open(parent);
    }

    static String builderId() {
        var workspace = ToolWorkspaceManager.getInstance();
        var builder = workspace.activeTab(ToolWorkspaceManager.Type.BUILDER);
        return builder == null ? workspace.add(ToolWorkspaceManager.Type.BUILDER) : builder.id();
    }

    static void openCrafter(BuilderScreen source, String slot, WynnItem current) {
        var workspace = ToolWorkspaceManager.getInstance();
        String id = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        String type = source.crafterType(slot, current);
        workspace.updateCraft(id, current != null && current.isCrafted()
                ? CraftedItemCodec.readCraft(current.craftedCode()) : CraftedItemCodec.emptyCraft(type));
        workspace.bindCraft(id, source.toolTabId(), slot);
        MinecraftClient.getInstance().setScreen(open(source.workspaceParent()));
    }

    static void equipCraft(Screen parent, String id, WynnItem item) {
        var workspace = ToolWorkspaceManager.getInstance();
        var target = workspace.craftTarget(id);
        if (target == null || workspace.find(target.buildId()) == null) {
            throw new IllegalStateException("The destination build was closed. Copy this craft into another build.");
        }
        workspace.select(target.buildId());
        BuilderScreen builder = new BuilderScreen(parent, target.buildId());
        MinecraftClient.getInstance().setScreen(builder);
        builder.equipFromTool(target.slot(), item);
    }

    static void add(Screen parent, String id, int x, int y, int width, Consumer<ThemedButton> add) {
        var workspace = ToolWorkspaceManager.getInstance();
        var current = workspace.find(id);
        if (current == null) return;
        add(parent, current.type(), id, x, y, width, add);
    }

    static void addEmpty(Screen parent, ToolWorkspaceManager.Type type,
            int x, int y, int width, Consumer<ThemedButton> add) {
        add(parent, type, null, x, y, width, add);
    }

    private static void add(Screen parent, ToolWorkspaceManager.Type currentType, String activeId,
            int x, int y, int width, Consumer<ThemedButton> add) {
        var workspace = ToolWorkspaceManager.getInstance();

        int historyWidth = 45;
        ThemedButton back = new ThemedButton(x, y, 21, CONTROL_HEIGHT, Text.literal("<"),
                () -> WorkspaceNavigation.back(parent)).setTextScale(CONTROL_TEXT_SCALE);
        back.setEnabled(WorkspaceNavigation.canGoBack());
        back.setHoverText(Text.literal("Back"));
        add.accept(back);
        ThemedButton forward = new ThemedButton(x + 24, y, 21, CONTROL_HEIGHT, Text.literal(">"),
                () -> WorkspaceNavigation.forward(parent)).setTextScale(CONTROL_TEXT_SCALE);
        forward.setEnabled(WorkspaceNavigation.canGoForward());
        forward.setHoverText(Text.literal("Forward"));
        add.accept(forward);

        int toolX = x + historyWidth + 6;
        int controlsWidth = 22;
        int controls = x + width - controlsWidth;
        int toolWidth = Math.max(45, (controls - toolX - 3)
                / ToolWorkspaceManager.Type.values().length);
        int buttonX = toolX;
        for (var type : ToolWorkspaceManager.Type.values()) {
            ThemedButton tool = new ThemedButton(buttonX, y, toolWidth - 3, CONTROL_HEIGHT,
                    Text.literal(shortToolName(type)).withColor(type == currentType
                            ? UiTheme.accentRgb() : UiTheme.TEXT & 0xFFFFFF), () -> {
                        var active = workspace.activeTab(type);
                        if (active == null) WorkspaceNavigation.show(parent, type);
                        else WorkspaceNavigation.open(parent, active.id());
                    }).setTextScale(CONTROL_TEXT_SCALE);
            add.accept(tool);
            buttonX += toolWidth;
        }
        ThemedButton info = new ThemedButton(controls, y, 21, CONTROL_HEIGHT, Text.literal("i"), () -> {})
                .setTextScale(1.5F).setTextOffset(0.5F, 0.0F);
        info.setHoverLines(List.of(
                Text.literal("Info"),
                Text.literal("This Menu is heavily inspired by WynnBuilder, WynnCrafter and WynnAtlas and it could not exist without their amazing work"),
                Text.literal("You can find their website at https://wynnbuilder.github.io or by clicking the \"Browser\" button in the bottom right"),
                Text.literal("Thanks to hppeng, ferricles, reschan and blankman and all WynnBuilder contributors for making this possible")
        ));
        add.accept(info);

        int tabY = y + CONTROL_HEIGHT + CONTROL_GAP;
        var tabs = workspace.tabs(currentType);
        int stripWidth = width - controlsWidth - 3;
        SmoothScroll scroll = SCROLL.computeIfAbsent(currentType, ignored -> new SmoothScroll());
        int maxScroll = Math.max(0, tabs.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP - stripWidth);
        scroll.clamp(0, maxScroll);
        int active = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id().equals(activeId)) active = i;
        if (!tabs.isEmpty()) {
            int activeLeft = active * (TAB_WIDTH + TAB_GAP);
            if (activeLeft < scroll.position()) scroll.jump(activeLeft);
            else if (activeLeft + TAB_WIDTH > scroll.position() + stripWidth) {
                scroll.jump(Math.min(maxScroll, activeLeft + TAB_WIDTH - stripWidth));
            }
            add.accept(new TabStrip(parent, activeId, x, tabY, stripWidth, tabs, scroll, maxScroll, active));
        }

        ThemedButton plus = new ThemedButton(controls, tabY, 21, CONTROL_HEIGHT, Text.literal("+"),
                () -> WorkspaceNavigation.add(parent, currentType))
                .setTextScale(1.5F).setTextOffset(0.5F, 0.0F);
        plus.setHoverText(Text.literal("New " + toolName(currentType) + " tab"));
        add.accept(plus);
    }

    static boolean scroll(Screen parent, String id, int x, int y, int width,
            double mouseX, double mouseY, double horizontal, double vertical) {
        double amount = vertical != 0 ? vertical : horizontal;
        int tabY = y + CONTROL_HEIGHT + CONTROL_GAP;
        if (amount == 0 || mouseX < x || mouseX >= x + width - 25
                || mouseY < tabY || mouseY >= tabY + TAB_STRIP_HEIGHT) {
            return false;
        }
        var workspace = ToolWorkspaceManager.getInstance();
        var current = workspace.find(id);
        if (current == null) return false;
        List<ToolWorkspaceManager.Tab> tabs = workspace.tabs(current.type());
        int stripWidth = width - 25;
        int maxScroll = Math.max(0, tabs.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP - stripWidth);
        SCROLL.computeIfAbsent(current.type(), ignored -> new SmoothScroll())
                .move((float) -amount * 54.0F, 0, maxScroll);
        return true;
    }

    static TextFieldWidget nameField(String id, int x, int y, int width, Consumer<String> renamed) {
        var client = MinecraftClient.getInstance();
        var workspace = ToolWorkspaceManager.getInstance();
        TextFieldWidget field = new TextFieldWidget(client.textRenderer, x, y, width, 20, Text.literal("Tab name"));
        field.setMaxLength(64);
        field.setText(workspace.find(id).name());
        field.setChangedListener(value -> {
            try {
                if (workspace.rename(id, value)) renamed.accept(value.trim());
            } catch (IllegalArgumentException ignored) {
            }
        });
        return field;
    }

    private static final class TabStrip extends ThemedButton {
        private final Screen parent;
        private final String activeId;
        private final List<ToolWorkspaceManager.Tab> tabs;
        private final SmoothScroll scroll;
        private final int maxScroll;
        private int keyboardIndex;

        private TabStrip(Screen parent, String activeId, int x, int y, int width,
                List<ToolWorkspaceManager.Tab> tabs, SmoothScroll scroll, int maxScroll, int active) {
            super(x, y, width, TAB_STRIP_HEIGHT, Text.empty(), () -> {});
            this.parent = parent;
            this.activeId = activeId;
            this.tabs = tabs;
            this.scroll = scroll;
            this.maxScroll = maxScroll;
            this.keyboardIndex = active;
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            setHoverText(null);
            float offset = scroll.update(delta);
            ui.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
            try {
                for (int i = 0; i < tabs.size(); i++) {
                    var tab = tabs.get(i);
                    int tabX = getX() + Math.round(i * (TAB_WIDTH + TAB_GAP) - offset);
                    boolean active = tab.id().equals(activeId);
                    boolean focused = isFocused() && i == keyboardIndex;
                    int background = active ? UiTheme.hoverBackground() : UiTheme.SURFACE;
                    int border = focused ? 0xFFFFFFFF : active ? UiTheme.accent() : UiTheme.BORDER;
                    UiTheme.drawRoundedBox(context, ui, tabX, getY(), TAB_WIDTH, TAB_HEIGHT,
                            background, border);
                    var current = ToolWorkspaceManager.getInstance().find(tab.id());
                    String name = current == null ? tab.name() : current.name();
                    String label = abbreviate(name, Math.round((TAB_WIDTH - 30) / TAB_TEXT_SCALE));
                    if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH
                            && mouseY >= getY() && mouseY < getY() + TAB_HEIGHT && !label.equals(name)) {
                        setHoverText(Text.literal(name));
                    }
                    ui.pushMatrix();
                    ui.translate(tabX + 7, getY() + TAB_HEIGHT / 2.0F);
                    ui.scale(TAB_TEXT_SCALE, TAB_TEXT_SCALE);
                    ui.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label,
                            0, -4, active ? UiTheme.accent() : UiTheme.TEXT);
                    ui.popMatrix();
                    ui.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "x",
                            tabX + TAB_WIDTH - 10, getY() + 7, active ? 0xFFFFFFFF : UiTheme.MUTED);
                }
            } finally {
                ui.disableScissor();
            }
            if (maxScroll > 0) {
                int thumbWidth = Math.max(18, getWidth() * getWidth()
                        / (tabs.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP));
                int thumbX = getX() + Math.round(offset / maxScroll * (getWidth() - thumbWidth));
                ui.fill(getX(), getY() + 24, getX() + getWidth(), getY() + 26, UiTheme.BORDER);
                ui.fill(thumbX, getY() + 23, thumbX + thumbWidth, getY() + 26, UiTheme.accent());
            }
        }

        private String abbreviate(String name, int width) {
            var font = MinecraftClient.getInstance().textRenderer;
            if (font.getWidth(name) <= width) return name;
            String suffix = "...";
            return font.trimToWidth(name, Math.max(0, width - font.getWidth(suffix))) + suffix;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isVisible() || !isEnabled() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || !contains((int) mouseX, (int) mouseY)) return false;
            setFocused(true);
            if (mouseY >= getY() + TAB_HEIGHT) {
                if (maxScroll > 0) {
                    moveScrollbar(mouseX);
                    return true;
                }
                return false;
            }
            int index = (int) ((mouseX - getX() + scroll.position()) / (TAB_WIDTH + TAB_GAP));
            if (index >= 0 && index < tabs.size()) {
                int tabX = getX() + Math.round(index * (TAB_WIDTH + TAB_GAP) - scroll.position());
                if (mouseX < tabX || mouseX >= tabX + TAB_WIDTH) return false;
                if (mouseX >= tabX + TAB_WIDTH - 20) {
                    playClickSound();
                    ToolWorkspaceManager.getInstance().close(tabs.get(index).id());
                    MinecraftClient.getInstance().setScreen(open(parent));
                    return true;
                }
                playClickSound();
                keyboardIndex = index;
                WorkspaceNavigation.open(parent, tabs.get(index).id());
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!isVisible() || !isEnabled() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || maxScroll <= 0 || mouseY < getY() + TAB_HEIGHT) return false;
            moveScrollbar(mouseX);
            return true;
        }

        private void moveScrollbar(double mouseX) {
            float ratio = (float) ((mouseX - getX()) / getWidth());
            scroll.jump(Math.clamp(ratio * maxScroll, 0, maxScroll));
        }

    }

    static String toolName(ToolWorkspaceManager.Type type) {
        return switch (type) { case BUILDER -> "Builder"; case CRAFTER -> "Crafter"; case ATLAS -> "Atlas"; };
    }

    private static String shortToolName(ToolWorkspaceManager.Type type) {
        return switch (type) { case BUILDER -> "Builder"; case CRAFTER -> "Crafter"; case ATLAS -> "Atlas"; };
    }
}
