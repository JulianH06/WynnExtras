package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.config.ToolWorkspaceManager;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class WorkspaceTabs {
    private WorkspaceTabs() {}

    public static Screen open(Screen parent) {
        var tab = ToolWorkspaceManager.getInstance().activeTab();
        return switch (tab.type()) {
            case BUILDER -> new WynnBuilderScreen(parent, tab.id());
            case CRAFTER -> new WynnCrafterScreen(parent, tab.id());
            case ATLAS -> new WynnAtlasScreen(parent, tab.id());
        };
    }

    static String builderId() {
        var workspace = ToolWorkspaceManager.getInstance();
        return workspace.activeTab().type() == ToolWorkspaceManager.Type.BUILDER
                ? workspace.activeId() : workspace.add(ToolWorkspaceManager.Type.BUILDER);
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

    static void add(Screen screen, Screen parent, String id, int x, int y, int width, Consumer<ThemedButton> add) {
        var client = MinecraftClient.getInstance();
        var workspace = ToolWorkspaceManager.getInstance();
        List<ToolWorkspaceManager.Tab> tabs = workspace.tabs();
        int active = 0;
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).id().equals(id)) active = i;
        int count = Math.max(1, (width - 48) / 110);
        int start = active / count * count;
        int tabWidth = (width - 48) / count;
        for (int i = start; i < Math.min(tabs.size(), start + count); i++) {
            var tab = tabs.get(i);
            String label = switch (tab.type()) { case BUILDER -> "B: "; case ATLAS -> "A: "; case CRAFTER -> "C: "; };
            label += tab.name();
            String fitted = client.textRenderer.getWidth(label) <= tabWidth - 10 ? label
                    : client.textRenderer.trimToWidth(label, Math.max(1, tabWidth - 22)) + "...";
            ThemedButton button = new ThemedButton(x + (i - start) * tabWidth, y, tabWidth - 3, 20,
                    Text.literal(fitted).withColor(tab.id().equals(id) ? UiTheme.accentRgb() : UiTheme.TEXT & 0xFFFFFF), () -> {
                        if (!tab.id().equals(id)) {
                            workspace.select(tab.id());
                            client.setScreen(open(parent));
                        }
                    });
            button.setTooltip(Tooltip.of(Text.literal(toolName(tab.type()) + " - " + tab.name()
                    + " (scroll here to switch tabs)")));
            add.accept(button);
        }
        int controls = x + width - 46;
        ThemedButton plus = new ThemedButton(controls, y, 21, 20, Text.literal("+"),
                () -> client.setScreen(new Chooser(screen, parent)));
        plus.setTooltip(Tooltip.of(Text.literal("New WynnBuilder, WynnAtlas or WynnCrafter tab")));
        add.accept(plus);
        ThemedButton close = new ThemedButton(controls + 24, y, 21, 20, Text.literal("X"), () ->
                client.setScreen(new ThemedConfirmScreen(confirmed -> {
                    if (confirmed) {
                        workspace.close(id);
                        client.setScreen(open(parent));
                    } else client.setScreen(screen);
                }, Text.literal("Close " + workspace.find(id).name() + "?"),
                        Text.literal("This removes this tool draft. Named builds and other tabs are not deleted."))));
        close.setTooltip(Tooltip.of(Text.literal("Close this tab")));
        add.accept(close);
    }

    static boolean scroll(Screen parent, String id, int x, int y, int width,
            double mouseX, double mouseY, double horizontal, double vertical) {
        double amount = vertical != 0 ? vertical : horizontal;
        if (amount == 0 || mouseX < x || mouseX >= x + width - 48 || mouseY < y || mouseY >= y + 20) {
            return false;
        }
        var workspace = ToolWorkspaceManager.getInstance();
        var tabs = workspace.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (!tabs.get(i).id().equals(id)) continue;
            int next = Math.max(0, Math.min(tabs.size() - 1, i - (int) Math.signum(amount)));
            if (next != i) {
                workspace.select(tabs.get(next).id());
                MinecraftClient.getInstance().setScreen(open(parent));
            }
            break;
        }
        return true;
    }

    static String toolName(ToolWorkspaceManager.Type type) {
        return switch (type) { case BUILDER -> "WynnBuilder"; case CRAFTER -> "WynnCrafter"; case ATLAS -> "WynnAtlas"; };
    }

    private static final class Chooser extends Screen {
        private final Screen previous;
        private final Screen parent;

        private Chooser(Screen previous, Screen parent) {
            super(Text.literal("Open a new tab"));
            this.previous = previous;
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = (width - 240) / 2;
            int y = (height - 160) / 2;
            int row = 0;
            for (var type : ToolWorkspaceManager.Type.values()) {
                addDrawableChild(new ThemedButton(x + 16, y + 32 + row++ * 28, 208, 22,
                        Text.literal(toolName(type)), () -> {
                            ToolWorkspaceManager.getInstance().add(type);
                            client.setScreen(open(parent));
                        }));
            }
            addDrawableChild(new ThemedButton(x + 76, y + 125, 88, 20, Text.literal("Cancel"), this::close));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            UiTheme.drawPanel(context, (width - 240) / 2, (height - 160) / 2, 240, 160);
            context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, (height - 160) / 2 + 12, UiTheme.accentRgb());
            super.render(context, mouseX, mouseY, delta);
        }

        @Override public void close() { client.setScreen(previous); }
    }
}
