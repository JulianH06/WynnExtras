package julianh06.wynnextras.config.configoptions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.BooleanSupplier;

import static julianh06.wynnextras.config.ConfigTheme.*;

public abstract class ConfigOption {
    public final String name, desc;
    protected List<DescLine> richDesc = null;
    private BooleanSupplier visibilityCondition = () -> true;

    protected ConfigOption(String name, String desc) { this.name = name; this.desc = desc; }

    public abstract void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor);
    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int btn) { return false; }
    public boolean mouseReleased(double mx, double my, int btn) { return false; }
    public boolean mouseDragged(double mx, double my, int x, int y, int w, int h) { return false; }

    public ConfigOption visibleWhen(BooleanSupplier condition) {
        this.visibilityCondition = condition;
        return this;
    }

    public ConfigOption withRichDesc(List<DescLine> lines) {
        this.richDesc = lines;
        return this;
    }

    public boolean isVisible() {
        return visibilityCondition.getAsBoolean();
    }

    public int controlWidth() { return 0; }

    public int getHeight(int contentW) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int textW = Math.max(20, contentW - 16 - controlWidth());
        int nameLines = tr.wrapLines(Text.literal(name), textW).size();
        if (richDesc != null && !richDesc.isEmpty()) {
            int richH = calcRichDescHeight(tr, richDesc, textW);
            return 8 + nameLines * 10 + 4 + richH + 13;
        }
        int descLineCount = (desc == null || desc.isEmpty()) ? 0 : tr.wrapLines(Text.literal(desc), textW).size();
        int extraLines = (nameLines - 1) + Math.max(0, descLineCount - 1);
        return OPTION_HEIGHT + extraLines * 10;
    }

    public static int calcRichDescHeight(net.minecraft.client.font.TextRenderer tr, List<DescLine> lines, int availW) {
        int total = 0;
        for (DescLine dl : lines) {
            var txt = Text.literal(dl.text).setStyle(Style.EMPTY
                .withBold(dl.bold).withItalic(dl.italic).withUnderline(dl.underline));
            int wrapW = Math.max(1, (int)(availW / dl.scale));
            int numWrapped = Math.max(1, tr.wrapLines(txt, wrapW).size());
            total += (int)(10 * dl.scale) * numWrapped;
        }
        return total;
    }

    public static int drawWrappedTexts(DrawContext ctx, int x, int y, int w, int controlW, String name, String desc, List<DescLine> richDesc, int nameColor, int descColor) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int textW = Math.max(20, w - 16 - controlW);
        var nameLines = tr.wrapLines(Text.literal(name), textW);
        int ny = y + 8;
        for (var line : nameLines) {
            ctx.drawTextWithShadow(tr, line, x + 8, ny, nameColor);
            ny += 10;
        }
        int dy = ny + 4;
        if (richDesc != null && !richDesc.isEmpty()) {
            for (DescLine dl : richDesc) {
                dy += renderRichDescLine(ctx, dl, x + 8, dy, textW, descColor);
            }
        } else if (desc != null && !desc.isEmpty()) {
            var descLines = tr.wrapLines(Text.literal(desc), textW);
            for (var line : descLines) {
                ctx.drawTextWithShadow(tr, line, x + 8, dy, descColor);
                dy += 10;
            }
        }
        return dy;
    }

    public static int renderRichDescLine(DrawContext ctx, DescLine dl, int x, int y, int availW, int color) {
        var tr = MinecraftClient.getInstance().textRenderer;
        var style = Style.EMPTY.withBold(dl.bold).withItalic(dl.italic).withUnderline(dl.underline);
        var txt = Text.literal(dl.text).setStyle(style);
        float s = dl.scale;
        int lineH = Math.max(1, (int)(10 * s));
        int wrapW = Math.max(1, (int)(availW / s));
        int drawColor = dl.color != null ? dl.color.asInt() : color;
        var wrapped = tr.wrapLines(txt, wrapW);
        int curY = y;
        for (var wl : wrapped) {
            int textPxW = (int)(tr.getWidth(wl) * s);
            int drawX = switch (dl.align) {
                case LEFT   -> x;
                case CENTER -> x + (availW - textPxW) / 2;
                case RIGHT  -> x + availW - textPxW;
            };
            if (s != 1.0f) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(drawX, curY);
                ctx.getMatrices().scale(s, s);
                ctx.drawTextWithShadow(tr, wl, 0, 0, drawColor);
                ctx.getMatrices().popMatrix();
            } else {
                ctx.drawTextWithShadow(tr, wl, drawX, curY, drawColor);
            }
            curY += lineH;
        }
        return curY - y;
    }
}
