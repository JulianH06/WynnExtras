package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

final class PlannerPickerList<T> extends Widget {
    private final int left;
    private final int top;
    private final int listWidth;
    private final int listHeight;
    private final SmoothScroll scroll;
    private final PlannerScrollbar scrollbar;
    private final RowRenderer<T> renderer;
    private final HoverRenderer<T> hoverRenderer;
    private final Consumer<T> select;
    private List<Row<T>> rows;
    private int maxScroll;

    PlannerPickerList(int left, int top, int width, int height, List<Row<T>> rows, SmoothScroll scroll,
            RowRenderer<T> renderer, HoverRenderer<T> hoverRenderer, Consumer<T> select) {
        super(left * 2, top * 2, width * 2, height * 2);
        setUiScaleFactor(1.0);
        this.left = left;
        this.top = top;
        listWidth = width;
        listHeight = height;
        this.rows = List.copyOf(rows);
        this.scroll = scroll;
        scrollbar = new PlannerScrollbar(scroll, 12);
        this.renderer = renderer;
        this.hoverRenderer = hoverRenderer;
        this.select = select;
    }

    void setRows(List<Row<T>> rows) {
        this.rows = List.copyOf(rows);
        scroll.jump(0.0F);
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        int contentHeight = rows.stream().mapToInt(Row::height).sum();
        maxScroll = Math.max(0, contentHeight - listHeight);
        scroll.clamp(0.0F, maxScroll);
        int offset = Math.round(scroll.update(delta));
        T hoveredValue = null;
        ui.enableScissor(left, top, left + listWidth, top + listHeight);
        try {
            int rowY = top - offset;
            for (Row<T> row : rows) {
                if (rowY + row.height() > top && rowY < top + listHeight) {
                    if (row.value() == null) {
                        ui.drawTextWithShadow(textRenderer(), row.heading(), left + 4, rowY + 4, UiTheme.accent());
                    } else {
                        boolean hovered = mouseX >= left && mouseX < left + listWidth - 6
                                && mouseY >= rowY && mouseY < rowY + row.height();
                        renderer.render(context, ui, textRenderer(), row.value(), left, rowY,
                                listWidth - 6, row.height(), hovered);
                        if (hovered) hoveredValue = row.value();
                    }
                }
                rowY += row.height();
            }
        } finally {
            ui.disableScissor();
        }
        scrollbar.configure(left + listWidth - 5, top, listHeight, maxScroll);
        scrollbar.draw(context, mouseX, mouseY, delta, ui);
        if (hoveredValue != null && hoverRenderer != null) {
            hoverRenderer.render(context, hoveredValue, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !containsLogical(mouseX, mouseY)) return false;
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) return true;
        int contentY = (int) (mouseY - top + scroll.position());
        for (Row<T> row : rows) {
            if (contentY < row.height()) {
                if (row.value() != null) select.accept(row.value());
                return true;
            }
            contentY -= row.height();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return scrollbar.mouseReleased(mouseX, mouseY, button);
    }

    void scroll(double amount) {
        scroll.move((float) -amount * 48.0F, 0.0F, maxScroll);
    }

    void chooseFirst() {
        rows.stream().map(Row::value).filter(Objects::nonNull).findFirst().ifPresent(select);
    }

    boolean containsLogical(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < left + listWidth && mouseY >= top && mouseY < top + listHeight;
    }

    private static TextRenderer textRenderer() {
        return net.minecraft.client.MinecraftClient.getInstance().textRenderer;
    }

    record Row<T>(String heading, T value, int height) {
        static <T> Row<T> heading(String heading, int height) {
            return new Row<>(heading, null, height);
        }

        static <T> Row<T> value(T value, int height) {
            return new Row<>("", value, height);
        }
    }

    @FunctionalInterface
    interface RowRenderer<T> {
        void render(DrawContext context, UIUtils ui, TextRenderer textRenderer,
                T value, int x, int y, int width, int height, boolean hovered);
    }

    @FunctionalInterface
    interface HoverRenderer<T> {
        void render(DrawContext context, T value, int mouseX, int mouseY);
    }
}
