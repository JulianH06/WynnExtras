package julianh06.wynnextras.features.profileviewer;

import julianh06.wynnextras.utils.text.StyledText;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.TextShadow;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OpenInBrowserButton extends EasyButton {
    private final String url;

    public OpenInBrowserButton(int x, int y, int height, int width, String url) {
        super(x, y, height, width, "Open in browser");
        this.url = url;
    }

    @Override
    public void click() {
        LinkUtils.openLink(url);
    }

    @Override
    public void drawWithTexture(DrawContext context, Identifier texture) {
        //enderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, width, height, (int) width, (int) height);
        if(buttonText == null) {
            return;
        }
        FontRenderer.getInstance().renderText(context, StyledText.fromComponent(Text.of(buttonText)), x + width / 2f, y + height / 2f - 2, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, height / 20f);
    }
}
