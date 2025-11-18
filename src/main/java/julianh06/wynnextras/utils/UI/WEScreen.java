package julianh06.wynnextras.utils.UI;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
    protected int xStart;
    protected int yStart;
    protected int screenWidth;
    protected int screenHeight;
    protected UIUtils ui;

    public final List<Widget> rootWidgets = new ArrayList<>();
    protected final List<WEElement<?>> listElements = new ArrayList<>(); // generisch
    protected Widget focusedWidget = null;
    protected WEElement<?> focusedElement = null;
    protected float listX, listY, listWidth, listHeight;
    protected float listItemHeight;
    protected float listSpacing;
    protected float listScrollOffset = 0f;
    protected int firstVisibleIndex = 0;
    protected int lastVisibleIndex = -1;
    protected float listViewportPadding = 1f;

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
                verticalAmount
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return;
            }
            lastScrollTime = now;

            if (verticalAmount > 0) {
                scrollList(30); //Scroll up
            } else {
                scrollList(-30); //Scroll down
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        //this.renderPanoramaBackground(context, delta);
        if(super.client == null) super.client = MinecraftClient.getInstance();
        if(super.client != null) super.applyBlur();

        this.drawContext = context;
        computeScaleAndOffsets();
        if (ui == null) ui = new UIUtils(context, scaleFactor, xStart, yStart);
        else ui.updateContext(context, scaleFactor, xStart, yStart);

        ui.drawBackground();
        updateValues();
        updateVisibleListRange();
        layoutListElements();
        drawBackground(context, mouseX, mouseY, delta);
        drawContent(context, mouseX, mouseY, delta);

        for (Widget w : rootWidgets) {
            w.draw(context, mouseX, mouseY, delta, ui);
        }

        // draw only visible range with small buffer for smoothness
        int start = Math.max(0, firstVisibleIndex - 1);
        int end = Math.min(listElements.size() - 1, lastVisibleIndex + 1);
        for (int i = start; i <= end; i++) {
            WEElement<?> e = listElements.get(i);
            e.draw(context, mouseX, mouseY, delta, ui);
        }
        drawForeground(context, mouseX, mouseY, delta);
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

    protected void updateElementBounds() {
        if (ui == null) return;

        for (WEElement<?> e : listElements) {
            int sx = (int) (e.x * McUtils.guiScale());
            int sy = (int) (e.y * McUtils.guiScale());
            int sw = (int) (e.width * McUtils.guiScale());
            int sh = (int) (e.height * McUtils.guiScale());
            e.setBounds(sx, sy, sw, sh);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // root widgets (topmost-first)
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget w = rootWidgets.get(i);
            if (w.mouseClicked(mouseX, mouseY, button)) {
                setFocusedWidget(w);
                setFocusedElement(null);
                return true;
            }
        }

        // list viewport handling
        if (isInsideListViewport(mouseX, mouseY)) {
            updateVisibleListRange();
            // iterate visible elements topmost-first (last index is visually lower, so reverse visible order)
            for (int i = lastVisibleIndex; i >= firstVisibleIndex; i--) {
                WEElement<?> e = listElements.get(i);
                if (e.mouseClicked(mouseX, mouseY, button)) {
                    setFocusedElement(e);
                    setFocusedWidget(null);
                    return true;
                }
            }
        }

        // click outside -> clear focus
        setFocusedWidget(null);
        setFocusedElement(null);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            if (rootWidgets.get(i).mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(ui == null) return false;
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            Widget w = rootWidgets.get(i);
            if (w.contains((int) mouseX, (int) mouseY)) {
                w.mouseDragged(mouseX, mouseY, button, dx, dy);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        // fallback to focused-first then all widgets
        for (Widget w : rootWidgets) {
            if (w.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focusedWidget != null && focusedWidget.charTyped(chr, modifiers)) return true;
        for (Widget w : rootWidgets) {
            if (w.charTyped(chr, modifiers)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    protected void setFocusedElement(WEElement<?> e) {
        if (focusedElement == e) return;
        if (focusedElement != null) focusedElement.setFocused(false);
        focusedElement = e;
        if (focusedElement != null) focusedElement.setFocused(true);
    }

    protected void setFocusedWidget(Widget w) {
        if (focusedWidget == w) return;
        if (focusedWidget != null) focusedWidget.setFocused(false);
        focusedWidget = w;
        if (focusedWidget != null) focusedWidget.setFocused(true);
    }

    protected void updateVisibleListRange() {
        int total = listElements.size();
        if (total == 0 || listItemHeight <= 0 || listHeight <= 0) {
            firstVisibleIndex = 0;
            lastVisibleIndex = Math.max(-1, total - 1);
            return;
        }

        float slot = listItemHeight + listSpacing;
        int start = (int) Math.floor(listScrollOffset / slot);
        start = Math.max(0, start);

        int visibleCount = (int) Math.ceil(listHeight / slot) + 1; // +1 buffer
        firstVisibleIndex = Math.min(total - 1, start);
        lastVisibleIndex = Math.min(total - 1, start + visibleCount - 1);
    }

    protected boolean isInsideListViewport(double mouseX, double mouseY) {
        if (ui == null) return false;
        float sx = ui.sx(listX);
        float sy = ui.sy(listY);
        int sw = ui.sw(listWidth);
        int sh = ui.sh(listHeight + (listSpacing * (listElements.size() - 1)));
        return mouseX >= sx && mouseY >= sy && mouseX < sx + sw && mouseY < sy + sh;
    }

    private double lastScaleFactor = -1;
    private int lastScreenWidth = -1;
    private int lastScreenHeight = -1;

    public void computeScaleAndOffsets() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window w = client.getWindow();
        if (w == null) return;

        this.scaleFactor = Math.max(1.0, w.getScaleFactor());
        this.screenWidth = w.getScaledWidth();
        this.screenHeight = w.getScaledHeight();

        this.xStart = 0;
        this.yStart = 0;

        if (ui != null) ui.updateContext(drawContext, scaleFactor, xStart, yStart);
    }

    protected void scrollList(float delta) {
        float contentHeight = listElements.size() * (listItemHeight + listSpacing) - listSpacing;
        listScrollOffset -= delta; // negative/positive depending on wheel direction; adjust if needed
        float scrollPadding = 40f;
        float maxScroll = Math.max(0f, contentHeight - listHeight + scrollPadding);
        listScrollOffset = Math.max(0f, Math.min(listScrollOffset, maxScroll));

        updateVisibleListRange();
        // reposition visible elements after scroll
        layoutListElements();
    }

    protected void layoutListElements() {
        if (listElements.isEmpty()) return;
        float yy = listY - listScrollOffset;
        for (int i = 0; i < listElements.size(); i++) {
            WEElement<?> e = listElements.get(i);
            int logicalX = Math.round(listX);
            int logicalY = Math.round(yy);
            int logicalW = Math.round(listWidth);
            int logicalH = Math.round(listItemHeight);
            e.setBounds(logicalX, logicalY, logicalW, logicalH);
            yy += listItemHeight + listSpacing;
        }
    }

    // Layout helper: position list vertically with spacing and optional scroll offset (logical coords)
    protected void layoutVertical(List<? extends Widget> list, float startX, float startY, float itemHeight, float spacing, float scrollOffset) {
        float yy = startY - scrollOffset;
        for (Widget w : list) {
            // Widgets expected to expose setBounds(int x,int y,int w,int h) in your implementation
            // Here we assume Widget has that method; if your Widget uses different API adapt accordingly.
            w.setBounds((int) startX, (int) yy, (int) (getLogicalWidth()), (int) itemHeight);
            yy += itemHeight + spacing;
        }
    }

    // logical UI width used when laying out elements; override if you use different logical area
    protected int getLogicalWidth() {
        // default: scaled screen width in logical units (inverse of ui transform)
        return (int) Math.round(screenWidth * scaleFactor);
    }

    // logical UI width used when laying out elements; override if you use different logical area
    protected int getLogicalHeight() {
        // default: scaled screen width in logical units (inverse of ui transform)
        return (int) Math.round(screenHeight * scaleFactor);
    }

    // Root widget management
    public void addRootWidget(Widget w) {
        if (w == null) return;
        this.rootWidgets.add(w);
        if (ui != null) {
            // if your Widget has setUi method, call it here, otherwise widgets should use UIUtils via constructor
            try {
                w.getClass().getMethod("setUi", UIUtils.class).invoke(w, ui);
            } catch (Exception ignored) {
                // ignore if not present; prefer constructor injection
            }
        }
    }

    protected void addListElement(WEElement<?> e) {
        if (e == null) return;
        listElements.add(e);
        // falls das WEElement die UIUtils benötigt, setze sie (wenn Methode vorhanden)
        try {
            e.getClass().getMethod("setUi", UIUtils.class).invoke(e, ui);
        } catch (Exception ignored) {}
        layoutListElements();         // berechne / setze Bounds für alle Elemente
        updateVisibleListRange();     // aktualisiere visible range
    }

    protected void removeListElement(WEElement<?> e) {
        if (e == null) return;
        listElements.remove(e);
        if (focusedElement == e) setFocusedElement(null);
        layoutListElements();
        updateVisibleListRange();
    }

    public void removeRootWidget(Widget w) {
        if (w == null) return;
        rootWidgets.remove(w);
        if (focusedWidget == w) setFocusedWidget(null);
    }

    @Override
    public void removed() {
        super.removed();
        // cleanup if needed
        rootWidgets.clear();
        listElements.clear();
        focusedWidget = null;
    }

    // Utility helpers that delegate to UIUtils for compatibility with existing call sites

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
        // schedule on client thread (safe from other threads)
        client.send(() -> {
            WEScreen current = null;
            if (client.currentScreen instanceof WEScreen) current = (WEScreen) client.currentScreen;
            // optional: skip if same class already open
            if (current != null && current.getClass() == screenSupplier.get().getClass()) {
                return;
            }
            client.setScreen(screenSupplier.get());
        });
    }

    //needed for the class selection stuff

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

    public int getxStart() {
        return xStart;
    }

    public int getyStart() {
        return yStart;
    }
}