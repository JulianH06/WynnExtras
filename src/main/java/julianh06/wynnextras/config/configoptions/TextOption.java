package julianh06.wynnextras.config.configoptions;

import net.minecraft.client.gui.DrawContext;

import static julianh06.wynnextras.config.ConfigTheme.*;

public class TextOption extends ConfigOption {
    public TextOption(String name, String desc) {
        super(name, desc);
    }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int h, int mx, int my, boolean hovered, int categoryColor) {
        ctx.fill(x, y, x + w, y + h - 5, hovered ? PARCHMENT_HOVER : PARCHMENT);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 6, x + w, y + h - 5, BORDER_DARK);
        drawWrappedTexts(ctx, x, y, w, controlWidth(), name, desc, richDesc, TEXT_LIGHT, TEXT_DIM);
    }
}
