package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.data.ItemInspection;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class ItemInspectionCard {
    private static final int WHITE = UiTheme.TEXT;
    private static final int[] ELEMENT_COLORS = {0xFF00AA00, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFFFFFF};
    private static final String[] SYMBOLS = {"\u2724", "\u2726", "\u2749", "\u2739", "\u274B"};

    private ItemInspectionCard() {}

    static int render(DrawContext context, TextRenderer font, WynnItem item, String powders,
            int screenWidth, int screenHeight, int rarityColor, SmoothScroll scroll, IconDrawer icon) {
        int width = Math.min(270, screenWidth - 24);
        int contentWidth = Math.max(1, width - 26);
        List<Line> lines = layout(font, ItemInspection.rows(item, powders), contentWidth, rarityColor);
        List<OrderedText> title = font.wrapLines(Text.literal(item.displayName())
                .formatted(Formatting.UNDERLINE), Math.max(1, (int) ((width - 26) / 1.25F)));
        int titleHeight = title.size() * 13;
        int headerHeight = titleHeight + 82;
        int contentHeight = lines.stream().mapToInt(Line::height).sum();
        int height = Math.min(screenHeight - 24, headerHeight + contentHeight + 14);
        int left = (screenWidth - width) / 2;
        int top = (screenHeight - height) / 2;
        int viewport = Math.max(1, height - headerHeight - 10);
        int maxScroll = Math.max(0, contentHeight - viewport);
        scroll.clamp(0, maxScroll);
        int offset = Math.round(scroll.update());

        context.fill(0, 0, screenWidth, screenHeight, 0xA0000000);
        UiTheme.drawRoundedBox(context, left, top, width, height, UiTheme.SURFACE, UiTheme.BORDER);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(left + width / 2.0F, top + 13);
        context.getMatrices().scale(1.25F, 1.25F);
        for (int i = 0; i < title.size(); i++) {
            OrderedText line = title.get(i);
            context.drawText(font, line, -font.getWidth(line) / 2, i * 10, rarityColor, false);
        }
        context.getMatrices().popMatrix();
        int centerY = top + titleHeight + 43;
        for (int radius = 28; radius >= 4; radius -= 4) {
            int color = UiTheme.lerpColor(UiTheme.SURFACE, rarityColor, (28 - radius) / 48.0F);
            circle(context, left + width / 2, centerY, radius, color);
        }
        icon.draw(left + width / 2 - 18, centerY - 18, 36);

        context.enableScissor(left + 2, top + headerHeight, left + width - 2, top + height - 8);
        try {
            int y = top + headerHeight - offset;
            for (Line line : lines) {
                context.drawText(font, line.left(), left + 13, y, WHITE, false);
                if (line.right() != null) {
                    context.drawText(font, line.right(), left + width - 13 - line.rightWidth(), y, WHITE, false);
                }
                y += line.height();
            }
        } finally {
            context.disableScissor();
        }
        if (maxScroll > 0) {
            int trackTop = top + headerHeight;
            int thumbHeight = Math.max(10, viewport * viewport / contentHeight);
            int thumbY = trackTop + Math.round(offset / (float) maxScroll * (viewport - thumbHeight));
            context.fill(left + width - 5, trackTop, left + width - 3, trackTop + viewport, UiTheme.BORDER);
            context.fill(left + width - 5, thumbY, left + width - 3, thumbY + thumbHeight, rarityColor);
        }
        return maxScroll;
    }

    private static List<Line> layout(TextRenderer font, List<ItemInspection.Row> rows, int width, int rarityColor) {
        return layout(font, rows, width, rarityColor, false);
    }

    private static List<Line> layout(TextRenderer font, List<ItemInspection.Row> rows, int width, int rarityColor,
            boolean rangeColumns) {
        List<Line> lines = new ArrayList<>();
        for (ItemInspection.Row row : rows) {
            if (row.kind() == ItemInspection.Kind.GAP) {
                lines.add(new Line(Text.empty().asOrderedText(), null, 0, 9));
                continue;
            }
            int color = switch (row.kind()) {
                case RARITY -> rarityColor;
                case RESTRICTION -> 0xFFFF5555;
                case MAJOR -> 0xFF55FFFF;
                case WARNING -> 0xFFFFAA00;
                case NOTE -> 0xFFAAAAAA;
                default -> WHITE;
            };
            MutableText label = Text.empty();
            if (row.element() >= 0) {
                int space = row.label().indexOf(' ');
                label.append(Text.literal(SYMBOLS[row.element()] + " " + row.label().substring(0, space))
                        .withColor(ELEMENT_COLORS[row.element()]));
                label.append(Text.literal(row.label().substring(space)).withColor(WHITE));
            } else {
                label.append(Text.literal(row.label()).withColor(color));
            }
            if (row.kind() == ItemInspection.Kind.LORE) {
                label.formatted(Formatting.ITALIC);
            }
            if (row.kind() == ItemInspection.Kind.SKILL || row.kind() == ItemInspection.Kind.INFO) {
                label.formatted(Formatting.BOLD);
            }
            if (!row.value().isEmpty()) {
                label.append(Text.literal(": ").withColor(WHITE));
                int valueColor = row.kind() == ItemInspection.Kind.SKILL || row.kind() == ItemInspection.Kind.IDENTIFICATION
                        || row.kind() == ItemInspection.Kind.MODIFIER
                        ? row.beneficial() ? 0xFF00FF40 : 0xFFFF5555 : WHITE;
                Text value = Text.literal(row.value()).withColor(valueColor);
                if (row.kind() == ItemInspection.Kind.IDENTIFICATION) {
                    if (rangeColumns) {
                        String[] bounds = row.value().replace("+", "").split(" to ", 2);
                        Text minimum = Text.literal(bounds[0]).withColor(valueColor);
                        Text maximum = Text.literal(bounds[bounds.length - 1]).withColor(valueColor);
                        int minimumWidth = font.getWidth(minimum);
                        int maximumWidth = font.getWidth(maximum);
                        int middleWidth = width - 2 * Math.max(minimumWidth, maximumWidth) - 12;
                        if (middleWidth >= 45) {
                            List<OrderedText> middle = font.wrapLines(label, middleWidth);
                            for (int i = 0; i < middle.size(); i++) {
                                lines.add(new Line(i == 0 ? minimum.asOrderedText() : Text.empty().asOrderedText(),
                                        i == 0 ? maximum.asOrderedText() : null, maximumWidth, 13,
                                        middle.get(i), font.getWidth(middle.get(i))));
                            }
                            continue;
                        }
                    }
                    int valueWidth = font.getWidth(value);
                    int available = width - valueWidth - 8;
                    if (available >= 50) {
                        List<OrderedText> wrapped = font.wrapLines(label, available);
                        for (int i = 0; i < wrapped.size(); i++) {
                            lines.add(new Line(wrapped.get(i), i == 0 ? value.asOrderedText() : null, valueWidth, 13));
                        }
                    } else {
                        append(lines, font, label, width);
                        append(lines, font, value, width);
                    }
                    continue;
                }
                label.append(value);
            }
            append(lines, font, label, width);
        }
        return lines;
    }

    private static void append(List<Line> lines, TextRenderer font, Text text, int width) {
        for (OrderedText line : font.wrapLines(text, width)) {
            lines.add(new Line(line, null, 0, 13));
        }
    }

    private static void circle(DrawContext context, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            context.fill(centerX - dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
        }
    }

    static Embedded embedded(TextRenderer font, Text title, List<ItemInspection.Row> rows, int width,
            int color, boolean icon) {
        List<OrderedText> titles = font.wrapLines(title.copy().formatted(Formatting.UNDERLINE), width - 24);
        List<Line> lines = layout(font, rows, width - 24, color, true);
        int header = 12 + titles.size() * 13 + (icon ? 54 : 9);
        return new Embedded(width, header + lines.stream().mapToInt(Line::height).sum() + 12,
                titles, lines, color, header, icon);
    }

    static final class Embedded {
        private final int width;
        private final int height;
        private final List<OrderedText> titles;
        private final List<Line> lines;
        private final int color;
        private final int header;
        private final boolean icon;

        private Embedded(int width, int height, List<OrderedText> titles, List<Line> lines,
                int color, int header, boolean icon) {
            this.width = width;
            this.height = height;
            this.titles = titles;
            this.lines = lines;
            this.color = color;
            this.header = header;
            this.icon = icon;
        }

        int height() { return height; }

        void render(DrawContext context, TextRenderer font, int x, int y, IconDrawer drawer) {
            UiTheme.drawRoundedBox(context, x + 2, y + 2, width, height, UiTheme.SHADOW, UiTheme.SHADOW);
            UiTheme.drawRoundedBox(context, x, y, width, height, UiTheme.SURFACE, UiTheme.BORDER);
            for (int i = 0; i < titles.size(); i++) {
                context.drawText(font, titles.get(i), icon ? x + (width - font.getWidth(titles.get(i))) / 2 : x + 12,
                        y + 12 + i * 13, color, false);
            }
            if (icon) {
                int centerY = y + 12 + titles.size() * 13 + 25;
                for (int radius = 22; radius >= 4; radius -= 3) {
                    circle(context, x + width / 2, centerY, radius,
                            UiTheme.lerpColor(UiTheme.SURFACE, color, (22 - radius) / 44.0F));
                }
                drawer.draw(x + width / 2 - 15, centerY - 15, 30);
            }
            int lineY = y + header;
            for (Line line : lines) {
                context.drawText(font, line.left(), x + 12, lineY, WHITE, false);
                if (line.right() != null) {
                    context.drawText(font, line.right(), x + width - 12 - line.rightWidth(), lineY, WHITE, false);
                }
                if (line.center() != null) {
                    context.drawText(font, line.center(), x + (width - line.centerWidth()) / 2, lineY, WHITE, false);
                }
                lineY += line.height();
            }
        }
    }

    @FunctionalInterface
    interface IconDrawer {
        void draw(int x, int y, int size);
    }

    private record Line(OrderedText left, OrderedText right, int rightWidth, int height, OrderedText center, int centerWidth) {
        private Line(OrderedText left, OrderedText right, int rightWidth, int height) {
            this(left, right, rightWidth, height, null, 0);
        }
    }
}
