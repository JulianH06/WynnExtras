package julianh06.wynnextras.utils.UI;

import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.utils.colors.CustomColor;
import julianh06.wynnextras.wtshim.utils.mc.McUtils;
import julianh06.wynnextras.wtshim.utils.render.FontRenderer;
import julianh06.wynnextras.wtshim.utils.render.RenderUtils;
import julianh06.wynnextras.wtshim.utils.render.type.HorizontalAlignment;
import julianh06.wynnextras.wtshim.utils.render.type.TextShadow;
import julianh06.wynnextras.wtshim.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class WEScreen extends Screen {
    protected DrawContext drawContext;
    protected double scaleFactor;
    protected double matrixScale = 1.0;
    protected int xStart;
    protected int yStart;
    protected int screenWidth;
    protected int screenHeight;
    protected UIUtils ui;

    protected double getTargetScaleFactor() { return -1; }
    protected int getMinLogicalWidth()  { return 0; }
    protected int getMinLogicalHeight() { return 0; }
    protected boolean shouldRenderBlur() { return true; }
    protected boolean shouldRenderBackground() { return true; }
    protected double actualScale = 1.0;

    public final List<Widget> rootWidgets = new ArrayList<>();
    protected Widget focusedWidget = null;

    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    protected WEScreen(Text title) {
        super(title);
        screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    protected void registerScrolling() {
        ScreenMouseEvents.afterMouseScroll(this).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount,
                consumed
        ) -> {
            long now = System.currentTimeMillis();
            if (isScrollOnCooldown(now)) {
                return true;
            }

            if (verticalAmount > 0) {
                scrollList(30); //Scroll up
            } else {
                scrollList(-30); //Scroll down
            }
            return true;
        });
    }

    private static boolean isScrollOnCooldown(long now) {
        if (now - lastScrollTime < scrollCooldown) {
            return true;
        }

        lastScrollTime = now;
        return false;
    }

    @Override
    public void blur() {
        if (shouldRenderBlur()) super.blur();
    }

    @Override
    public void applyBlur(DrawContext context) {
        if (shouldRenderBlur()) super.applyBlur(context);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (shouldRenderBackground()) super.renderBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void renderInGameBackground(DrawContext context) {
        if (shouldRenderBackground()) super.renderInGameBackground(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            if (super.client != null) applyBlur(context);
        } catch (Exception ignored) {}

        this.drawContext = context;
        computeScaleAndOffsets();
        if (ui == null) ui = new UIUtils(context, scaleFactor, xStart, yStart);
        else ui.updateContext(context, scaleFactor, xStart, yStart);

        int mx = (int)(mouseX / matrixScale);
        int my = (int)(mouseY / matrixScale);

        if (shouldRenderBackground()) ui.drawBackground();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale((float) matrixScale, (float) matrixScale);
        updateValues();
        drawBackground(context, mx, my, delta);
        drawContent(context, mx, my, delta);

        for (Widget w : rootWidgets) {
            w.draw(context, mx, my, delta, ui);
        }

        drawForeground(context, mx, my, delta);

        context.getMatrices().popMatrix();
    }

    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) { /* override */ }
    protected abstract void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta);
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) { /* override */ }

    protected void updateWidgetBounds() {
        if (ui == null) return;

        for (Widget w : rootWidgets) {
            int sx = (int) (w.x);
            int sy = (int) (w.y * McUtils.guiScale());
            int sw = (int) (w.width * McUtils.guiScale());
            int sh = (int) (w.height * McUtils.guiScale());
            w.setBounds(sx, sy, sw, sh);
        }
    }

    protected void updateValues() {}

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x() / matrixScale;
        double mouseY = click.y() / matrixScale;
        int button = click.button();

        clearUiFocus();

        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget w = rootWidgets.get(i);
            if (w.mouseClicked(mouseX, mouseY, button)) {
                setFocusedWidget(w);
                return true;
            }
        }

        setFocusedWidget(null);
        return super.mouseClicked(click, doubleClick);
    }

    @Override public boolean mouseReleased(Click click) {
        double mouseX = click.x() / matrixScale;
        double mouseY = click.y() / matrixScale;
        int button = click.button();

        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            if (rootWidgets.get(i).mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(click);
    }


    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double mouseX = click.x() / matrixScale;
        double mouseY = click.y() / matrixScale;
        int button = click.button();

        if(ui == null) return false;
        if (focusedWidget != null && focusedWidget.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget w = rootWidgets.get(i);
            if (w.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        }
        return super.mouseDragged(click, dx, dy);
    }


    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        for (Widget w : rootWidgets) {
            if (w.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        int modifiers = input.modifiers();

        if (focusedWidget != null && focusedWidget.charTyped(chr, modifiers)) return true;
        for (Widget w : rootWidgets) {
            if (w.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(input);
    }

    protected void setFocusedWidget(Widget w) {
        if (focusedWidget == w) return;
        if (focusedWidget != null) focusedWidget.setFocused(false);
        focusedWidget = w;
        if (focusedWidget != null) focusedWidget.setFocused(true);
    }

    protected void clearUiFocus() {
        for (Widget w : rootWidgets) {
            w.clearFocusTree();
        }
        focusedWidget = null;
    }

    private double lastScaleFactor = -1;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;

    public void computeScaleAndOffsets() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window w = client.getWindow();
        if (w == null) return;

        double actualScale = Math.max(1.0, w.getScaleFactor());
        this.actualScale = actualScale;
        double target = getTargetScaleFactor();
        if (target > 1.0 && actualScale > target) {
            this.matrixScale = target / actualScale;
            this.scaleFactor = target;
        } else {
            this.matrixScale = 1.0;
            this.scaleFactor = actualScale;
        }

        int minW = getMinLogicalWidth();
        int minH = getMinLogicalHeight();
        if (minW > 0) matrixScale = Math.min(matrixScale, w.getScaledWidth()  * scaleFactor / (double) minW);
        if (minH > 0) matrixScale = Math.min(matrixScale, w.getScaledHeight() * scaleFactor / (double) minH);
        matrixScale = Math.min(1.0, matrixScale);
        this.screenWidth = (int) Math.round(w.getScaledWidth() / matrixScale);
        this.screenHeight = (int) Math.round(w.getScaledHeight() / matrixScale);

        this.xStart = 0;
        this.yStart = 0;

        if (ui != null) ui.updateContext(drawContext, scaleFactor, xStart, yStart);
    }

    protected void scrollList(float delta) {
    }

    protected void layoutVertical(List<? extends Widget> list, float startX, float startY, float itemHeight, float spacing, float scrollOffset) {
        float yy = startY - scrollOffset;
        for (Widget w : list) {
            w.setBounds((int) startX, (int) yy, (int) (getLogicalWidth()), (int) itemHeight);
            yy += itemHeight + spacing;
        }
    }

    protected int getLogicalWidth() {
        Window w = MinecraftClient.getInstance().getWindow();
        return (int) Math.round(w.getScaledWidth() * scaleFactor / matrixScale);
    }

    protected int getLogicalHeight() {
        Window w = MinecraftClient.getInstance().getWindow();
        return (int) Math.round(w.getScaledHeight() * scaleFactor / matrixScale);
    }

    public void addRootWidget(Widget w) {
        if (w == null) return;
        this.rootWidgets.add(w);
        if (ui != null) {
            try {
                w.getClass().getMethod("setUi", UIUtils.class).invoke(w, ui);
            } catch (Exception ignored) { }
        }
    }

    public void removeRootWidget(Widget w) {
        if (w == null) return;
        rootWidgets.remove(w);
        if (focusedWidget == w) setFocusedWidget(null);
    }

    @Override
    public void removed() {
        super.removed();
        rootWidgets.clear();
        focusedWidget = null;
    }

    protected void drawText(String text, float x, float y, CustomColor color,
                            HorizontalAlignment horizontalAlignment,
                            VerticalAlignment verticalAlignment,
                            TextShadow shadow, float textScale) {
        if (ui == null) return;
        ui.drawText(text, x, y, color, horizontalAlignment, verticalAlignment, shadow, textScale);
    }

    protected void drawText(String text, float x, float y, CustomColor color,
                            HorizontalAlignment horizontalAlignment,
                            VerticalAlignment verticalAlignment, float textScale) {
        drawText(text, x, y, color, horizontalAlignment, verticalAlignment, TextShadow.NORMAL, textScale);
    }

    protected void drawText(String text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, textScale);
    }

    protected void drawText(String text, float x, float y, CustomColor color) {
        drawText(text, x, y, color, 3f);
    }

    protected void drawText(String text, float x, float y) {
        drawText(text, x, y, CustomColor.fromHexString("FFFFFF"));
    }

    protected void drawCenteredText(String text, float x, float y, CustomColor color, float textScale) {
        drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    protected void drawCenteredText(String text, float x, float y, CustomColor color) {
        drawCenteredText(text, x, y, color, 3f);
    }

    protected void drawCenteredText(String text, float x, float y) {
        drawCenteredText(text, x, y, CustomColor.fromHexString("FFFFFF"));
    }

    protected void drawImage(net.minecraft.util.Identifier texture, float x, float y, float width, float height) {
        if (ui == null) return;
        ui.drawImage(texture, x, y, width, height);
    }

    /**
     * Open a screen safely on the client thread.
     * Usage: WEScreen.open(() -> new MainScreen());
     */
    public static void open(Supplier<? extends WEScreen> screenSupplier) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        client.send(() -> {
            WEScreen current = null;
            if (client.currentScreen instanceof WEScreen) current = (WEScreen) client.currentScreen;
            if (current != null && current.getClass() == screenSupplier.get().getClass()) {
                return;
            }
            client.setScreen(screenSupplier.get());
        });
    }

    public void setDrawContext(DrawContext drawContext) {
        this.drawContext = drawContext;
    }

    public void setUi(UIUtils ui) {
        this.ui = ui;
    }

    public UIUtils getUi() {
        return this.ui;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public double getMatrixScale() {
        return matrixScale;
    }

    public int getxStart() {
        return xStart;
    }

    public int getyStart() {
        return yStart;
    }
}
