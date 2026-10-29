package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.UIUtils;
import java.util.function.Consumer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class WorkspaceTabs {
    private static final int TAB_GAP = 3;
    private static final int TAB_WIDTH = 118;
    private static final Map<ToolWorkspaceManager.Type, SmoothScroll> SCROLL =
            new EnumMap<>(ToolWorkspaceManager.Type.class);

    private WorkspaceTabs() {}

    public static Screen open(Screen parent) {
        var workspace = ToolWorkspaceManager.getInstance();
        var tab = workspace.activeTab();
        if (tab == null) return new EmptyWorkspaceScreen(parent, workspace.activeType());
        return switch (tab.type()) {
            case BUILDER -> new WynnBuilderScreen(parent, tab.id());
            case CRAFTER -> new WynnCrafterScreen(parent, tab.id());
            case ATLAS -> new WynnAtlasScreen(parent, tab.id());
        };
    }

    static String builderId() {
        var workspace = ToolWorkspaceManager.getInstance();
        var builder = workspace.activeTab(ToolWorkspaceManager.Type.BUILDER);
        return builder == null ? workspace.add(ToolWorkspaceManager.Type.BUILDER) : builder.id();
    }

    static void openCrafter(WynnBuilderScreen source, String slot, WynnItem current) {
        var workspace = ToolWorkspaceManager.getInstance();
        String id = workspace.add(ToolWorkspaceManager.Type.CRAFTER);
        String type = slot.startsWith("RING_") ? "ring"
                : slot.equals("WEAPON") ? "bow" : slot.toLowerCase(java.util.Locale.ROOT);
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
        WynnBuilderScreen builder = new WynnBuilderScreen(parent, target.buildId());
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
        ThemedButton back = new ThemedButton(x, y, 21, 20, Text.literal("<"),
                () -> WorkspaceNavigation.back(parent));
        back.active = WorkspaceNavigation.canGoBack();
        back.setHoverText(Text.literal("Back"));
        add.accept(back);
        ThemedButton forward = new ThemedButton(x + 24, y, 21, 20, Text.literal(">"),
                () -> WorkspaceNavigation.forward(parent));
        forward.active = WorkspaceNavigation.canGoForward();
        forward.setHoverText(Text.literal("Forward"));
        add.accept(forward);

        int toolX = x + historyWidth + 6;
        int toolWidth = Math.max(45, (width - historyWidth - 6) / ToolWorkspaceManager.Type.values().length);
        int toolIndex = 0;
        for (var type : ToolWorkspaceManager.Type.values()) {
            int buttonX = toolX + toolIndex++ * toolWidth;
            ThemedButton tool = new ThemedButton(buttonX, y, toolWidth - 3, 20,
                    Text.literal(shortToolName(type)).withColor(type == currentType
                            ? UiTheme.accentRgb() : UiTheme.TEXT & 0xFFFFFF), () -> {
                        var active = workspace.activeTab(type);
                        if (active == null) WorkspaceNavigation.show(parent, type);
                        else WorkspaceNavigation.open(parent, active.id());
                    });
            tool.setHoverText(Text.literal(toolName(type)));
            add.accept(tool);
        }

        int tabY = y + 23;
        var tabs = workspace.tabs(currentType);
        int controlsWidth = 22;
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

        int controls = x + width - controlsWidth;
        ThemedButton plus = new ThemedButton(controls, tabY, 21, 20, Text.literal("+"),
                () -> WorkspaceNavigation.add(parent, currentType));
        plus.setHoverText(Text.literal("New " + toolName(currentType) + " tab"));
        add.accept(plus);
    }

    static boolean scroll(Screen parent, String id, int x, int y, int width,
            double mouseX, double mouseY, double horizontal, double vertical) {
        double amount = vertical != 0 ? vertical : horizontal;
        if (amount == 0 || mouseX < x || mouseX >= x + width - 25 || mouseY < y + 23 || mouseY >= y + 43) {
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
            super(x, y, width, 20, Text.empty(), () -> {});
            this.parent = parent;
            this.activeId = activeId;
            this.tabs = tabs;
            this.scroll = scroll;
            this.maxScroll = maxScroll;
            this.keyboardIndex = active;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            UIUtils ui = new UIUtils(context, 1.0, 0, 0);
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
                    UiTheme.drawRoundedBox(ui, tabX, getY(), TAB_WIDTH, 16, background, border);
                    var current = ToolWorkspaceManager.getInstance().find(tab.id());
                    String name = current == null ? tab.name() : current.name();
                    String label = abbreviate(name, TAB_WIDTH - 30);
                    if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH
                            && mouseY >= getY() && mouseY < getY() + 16 && !label.equals(name)) {
                        setHoverText(Text.literal(name));
                    }
                    ui.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, label,
                            tabX + 7, getY() + 4,
                            active ? UiTheme.accent() : UiTheme.TEXT);
                    ui.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "×",
                            tabX + TAB_WIDTH - 10, getY() + 4, active ? 0xFFFFFFFF : UiTheme.MUTED);
                }
            } finally {
                ui.disableScissor();
            }
            if (maxScroll > 0) {
                int thumbWidth = Math.max(18, getWidth() * getWidth()
                        / (tabs.size() * (TAB_WIDTH + TAB_GAP) - TAB_GAP));
                int thumbX = getX() + Math.round(offset / maxScroll * (getWidth() - thumbWidth));
                ui.fill(getX(), getY() + 18, getX() + getWidth(), getY() + 20, UiTheme.BORDER);
                ui.fill(thumbX, getY() + 17, thumbX + thumbWidth, getY() + 20, UiTheme.accent());
            }
        }

        private String abbreviate(String name, int width) {
            var font = MinecraftClient.getInstance().textRenderer;
            if (font.getWidth(name) <= width) return name;
            String suffix = "...";
            return font.trimToWidth(name, Math.max(0, width - font.getWidth(suffix))) + suffix;
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            if (click.y() >= getY() + 16 && maxScroll > 0) {
                moveScrollbar(click.x());
                return;
            }
            int index = (int) ((click.x() - getX() + scroll.position()) / (TAB_WIDTH + TAB_GAP));
            if (index >= 0 && index < tabs.size()) {
                int tabX = getX() + Math.round(index * (TAB_WIDTH + TAB_GAP) - scroll.position());
                if (click.x() < tabX || click.x() >= tabX + TAB_WIDTH) return;
                if (click.x() >= tabX + TAB_WIDTH - 20 && click.x() < tabX + TAB_WIDTH) {
                    playClickSound();
                    ToolWorkspaceManager.getInstance().close(tabs.get(index).id());
                    MinecraftClient.getInstance().setScreen(open(parent));
                    return;
                }
                playClickSound();
                keyboardIndex = index;
                WorkspaceNavigation.open(parent, tabs.get(index).id());
            }
        }

        @Override
        protected void onDrag(Click click, double deltaX, double deltaY) {
            if (maxScroll > 0 && click.y() >= getY() + 16) moveScrollbar(click.x());
        }

        private void moveScrollbar(double mouseX) {
            float ratio = (float) ((mouseX - getX()) / getWidth());
            scroll.jump(Math.clamp(ratio * maxScroll, 0, maxScroll));
        }

        @Override
        public boolean keyPressed(KeyInput input) {
            if (!isFocused()) return super.keyPressed(input);
            if (input.key() == GLFW.GLFW_KEY_LEFT || input.key() == GLFW.GLFW_KEY_RIGHT) {
                int direction = input.key() == GLFW.GLFW_KEY_LEFT ? -1 : 1;
                keyboardIndex = Math.clamp(keyboardIndex + direction, 0, tabs.size() - 1);
                int tabLeft = keyboardIndex * (TAB_WIDTH + TAB_GAP);
                if (tabLeft < scroll.position()) scroll.move(tabLeft - scroll.position(), 0, maxScroll);
                else if (tabLeft + TAB_WIDTH > scroll.position() + getWidth()) {
                    scroll.move(tabLeft + TAB_WIDTH - getWidth() - scroll.position(), 0, maxScroll);
                }
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER
                    || input.key() == GLFW.GLFW_KEY_SPACE) {
                playClickSound();
                WorkspaceNavigation.open(parent, tabs.get(keyboardIndex).id());
                return true;
            }
            return super.keyPressed(input);
        }
    }

    static String toolName(ToolWorkspaceManager.Type type) {
        return switch (type) { case BUILDER -> "WynnBuilder"; case CRAFTER -> "WynnCrafter"; case ATLAS -> "WynnAtlas"; };
    }

    private static String shortToolName(ToolWorkspaceManager.Type type) {
        return switch (type) { case BUILDER -> "Builder"; case CRAFTER -> "Crafter"; case ATLAS -> "Atlas"; };
    }
}
