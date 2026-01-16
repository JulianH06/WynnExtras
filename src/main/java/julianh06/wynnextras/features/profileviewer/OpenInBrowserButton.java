package julianh06.wynnextras.features.profileviewer;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.net.URI;

public class OpenInBrowserButton extends EasyButton {
    private final String url;
    int scaleFactor;

    public OpenInBrowserButton(int x, int y, int height, int width, String url) {
        super(x, y, height, width, "Open in browser");
        this.url = url;
    }

    @Override
    public void click() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.err.println("Error while opening the link");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawWithTexture(DrawContext context, Identifier texture) {
        //enderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, width, height, (int) width, (int) height);
        if(buttonText == null) {
            return;
        }
        scaleFactor = (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        FontRenderer.getInstance().renderText(context, StyledText.fromComponent(Text.of(buttonText)), (float) (x + 87.5f * 3 / scaleFactor / 2), y + (float) (8 * 3) / scaleFactor, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, TextShadow.NORMAL, 1f * 3 / scaleFactor);
    }
}
