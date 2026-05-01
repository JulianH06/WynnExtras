package julianh06.wynnextras.config.configoptions;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class ImageOption extends ConfigOption {
    final Identifier identifier;
    final int imgW, imgH;
    final float widthFraction;

    public ImageOption(Identifier identifier, int imgW, int imgH, float widthFraction, List<DescLine> lines) {
        super("", lines.stream().map(dl -> dl.text).reduce("", (a, b) -> a + " " + b).trim());
        this.identifier = identifier;
        this.imgW = imgW;
        this.imgH = imgH;
        this.widthFraction = MathHelper.clamp(widthFraction, 0.0f, 1.0f);
        this.richDesc = lines;
    }

    private int calcDisplayW(int contentW) {
        int availW = contentW - 16;
        return (int) (availW * widthFraction);
    }

    @Override
    public int getHeight(int contentW) {
        int displayW = calcDisplayW(contentW);
        int displayH = (imgW > 0) ? (int) ((float) displayW / imgW * imgH) : imgH;
        int textW = Math.max(1, contentW - 24);
        int linesH = (richDesc != null && !richDesc.isEmpty())
            ? calcRichDescHeight(MinecraftClient.getInstance().textRenderer, richDesc, textW)
            : 0;
        return 8 + displayH + (linesH > 0 ? 4 + linesH : 0) + 8;
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        ctx.fill(x, y, x + w, y + h - 5, PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);

        int padding = 8;
        int availW = w - padding * 2;

        int displayW = (int) (availW * widthFraction);
        int displayH = (imgW > 0 && imgH > 0) ? (int) ((float) displayW / imgW * imgH) : 0;
        int texW = imgW > 0 ? imgW : displayW;
        int texH = imgH > 0 ? imgH : displayH;

        int imgX = x + padding + (availW - displayW) / 2;
        RenderUtils.drawTexturedRect(ctx, identifier, CustomColor.NONE, imgX, y + padding, displayW, displayH, texW, texH);

        if (richDesc != null && !richDesc.isEmpty()) {
            int textY = y + padding + displayH + 4;
            int textW = availW - 8;
            for (DescLine dl : richDesc) {
                textY += renderRichDescLine(ctx, dl, x + 8, textY, textW, TEXT_DIM);
            }
        }
    }
}
