package julianh06.wynnextras.config.configoptions;

import julianh06.wynnextras.utils.MinecraftUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class LineListOption extends ConfigOption {
    private static final int ROW_HEIGHT = 18;

    private final Supplier<List<String>> activeGetter;
    private final Consumer<List<String>> activeSetter;
    private final Supplier<List<String>> availableGetter;
    private final Consumer<List<String>> availableSetter;
    private final Supplier<Map<String, String>> labelGetter;
    private final String activeTitle;
    private final String availableTitle;
    private String draggedLine = null;
    private boolean draggedFromActive = false;
    private int dragStartIndex = -1;
    private int lastX, lastY, lastW;

    public LineListOption(String name, String desc,
                          Supplier<List<String>> activeGetter, Consumer<List<String>> activeSetter,
                          Supplier<List<String>> availableGetter, Consumer<List<String>> availableSetter,
                          Supplier<Map<String, String>> labelGetter,
                          String activeTitle, String availableTitle) {
        super(name, desc);
        this.activeGetter = activeGetter;
        this.activeSetter = activeSetter;
        this.availableGetter = availableGetter;
        this.availableSetter = availableSetter;
        this.labelGetter = labelGetter;
        this.activeTitle = activeTitle;
        this.availableTitle = availableTitle;
    }

    @Override
    public int getHeight(int contentW) {
        int activeCount = activeGetter.get().size();
        int availableCount = availableGetter.get().size();
        return 62 + activeCount * ROW_HEIGHT + 23 + Math.max(1, availableCount) * ROW_HEIGHT + 12;
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        lastX = x;
        lastY = y;
        lastW = w;
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, 0, name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);

        int listX = x + 8;
        int listW = w - 16;
        int rowY = y + 48;
        ctx.drawTextWithShadow(tr, activeTitle, listX, rowY - 11, categoryColor);
        for (String id : activeGetter.get()) {
            drawLineRow(ctx, listX, rowY, listW, id, mx, my);
            rowY += ROW_HEIGHT;
        }

        int availableY = rowY + 23;
        ctx.drawTextWithShadow(tr, availableTitle, listX, availableY - 11, categoryColor);
        List<String> available = availableGetter.get();
        if (available.isEmpty()) {
            ctx.drawTextWithShadow(tr, "None", listX + 6, availableY + 4, TEXT_DIM);
        } else {
            for (String id : available) {
                drawLineRow(ctx, listX, availableY, listW, id, mx, my);
                availableY += ROW_HEIGHT;
            }
        }

        if (draggedLine != null) {
            int activeY = y + 48;
            int currentAvailableY = activeY + activeGetter.get().size() * ROW_HEIGHT + 23;
            int dropY = getDropIndicatorY(mx, my, listX, listW, activeY, currentAvailableY);
            if (dropY >= 0) ctx.fill(listX, dropY, listX + listW, dropY + 2, GOLD);
            drawGhostRow(ctx, mx - listW / 2, my - ROW_HEIGHT / 2, listW, draggedLine);
        }
    }

    private void drawLineRow(DrawContext ctx, int x, int y, int w, String id, int mx, int my) {
        var tr = MinecraftClient.getInstance().textRenderer;
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_HEIGHT - 2;
        int bg = id.equals(draggedLine) ? BG_DARK : (hover ? BG_LIGHT : BG_MEDIUM);
        ctx.fill(x, y, x + w, y + ROW_HEIGHT - 2, bg);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.drawTextWithShadow(tr, "=", x + 6, y + 5, TEXT_DIM);
        ctx.drawTextWithShadow(tr, displayName(id), x + 18, y + 5, TEXT_LIGHT);
    }

    private void drawGhostRow(DrawContext ctx, int x, int y, int w, String id) {
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.fill(x, y, x + w, y + ROW_HEIGHT - 2, 0xDD4d3c2d);
        ctx.fill(x, y, x + w, y + 1, GOLD);
        ctx.drawTextWithShadow(tr, displayName(id), x + 8, y + 5, TEXT_LIGHT);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) {
        if (btn != 0) return false;
        lastX = x;
        lastY = y;
        lastW = w;
        int listX = x + 8;
        int listW = w - 16;
        int rowY = y + 48;
        int activeIndex = rowIndexAt(mx, my, listX, rowY, listW, activeGetter.get().size());
        if (activeIndex >= 0) {
            draggedLine = activeGetter.get().get(activeIndex);
            draggedFromActive = true;
            dragStartIndex = activeIndex;
            return true;
        }

        int availableY = rowY + activeGetter.get().size() * ROW_HEIGHT + 23;
        int availableIndex = rowIndexAt(mx, my, listX, availableY, listW, availableGetter.get().size());
        if (availableIndex >= 0) {
            draggedLine = availableGetter.get().get(availableIndex);
            draggedFromActive = false;
            dragStartIndex = availableIndex;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (draggedLine == null) return false;
        List<String> active = new ArrayList<>(activeGetter.get());
        List<String> available = new ArrayList<>(availableGetter.get());

        int listX = lastX + 8;
        int listW = lastW - 16;
        int rowY = lastY + 48;
        int originalActiveCount = active.size();
        int originalAvailableCount = available.size();
        int activeDropIndex = dropIndexAt(mx, my, listX, rowY, listW, originalActiveCount);
        int availableY = rowY + originalActiveCount * ROW_HEIGHT + 23;
        int availableDropIndex = dropIndexAt(mx, my, listX, availableY, listW, originalAvailableCount);

        active.remove(draggedLine);
        available.remove(draggedLine);

        if (activeDropIndex >= 0) {
            if (draggedFromActive && activeDropIndex > dragStartIndex) activeDropIndex--;
            active.add(Math.min(activeDropIndex, active.size()), draggedLine);
            save(active, available);
        } else if (availableDropIndex >= 0) {
            if (!draggedFromActive && availableDropIndex > dragStartIndex) availableDropIndex--;
            available.add(Math.min(availableDropIndex, available.size()), draggedLine);
            save(active, available);
        } else if (draggedFromActive) {
            active.add(Math.min(dragStartIndex, active.size()), draggedLine);
        } else {
            available.add(Math.min(dragStartIndex, available.size()), draggedLine);
        }

        draggedLine = null;
        dragStartIndex = -1;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int x, int y, int w, int h) {
        return draggedLine != null;
    }

    private int rowIndexAt(double mx, double my, int x, int y, int w, int count) {
        if (mx < x || mx >= x + w || my < y || my >= y + count * ROW_HEIGHT) return -1;
        int index = (int) ((my - y) / ROW_HEIGHT);
        return index >= 0 && index < count ? index : -1;
    }

    private int dropIndexAt(double mx, double my, int x, int y, int w, int count) {
        if (mx < x || mx >= x + w) return -1;
        if (count == 0) return my >= y && my < y + ROW_HEIGHT ? 0 : -1;
        if (my < y || my >= y + count * ROW_HEIGHT) return -1;
        return Math.min(count, Math.max(0, (int) ((my - y + ROW_HEIGHT / 2.0) / ROW_HEIGHT)));
    }

    private int getDropIndicatorY(int mx, int my, int x, int w, int activeY, int availableY) {
        int activeIndex = dropIndexAt(mx, my, x, activeY, w, activeGetter.get().size());
        if (activeIndex >= 0) return activeY + activeIndex * ROW_HEIGHT;
        int availableIndex = dropIndexAt(mx, my, x, availableY, w, availableGetter.get().size());
        if (availableIndex >= 0) return availableY + availableIndex * ROW_HEIGHT;
        return -1;
    }

    private void save(List<String> active, List<String> available) {
        activeSetter.accept(active);
        availableSetter.accept(available);
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    private String displayName(String id) {
        return labelGetter.get().getOrDefault(id, id);
    }
}
