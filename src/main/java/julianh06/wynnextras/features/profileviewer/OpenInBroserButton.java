package julianh06.wynnextras.features.profileviewer;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.net.URI;

public class OpenInBroserButton extends EasyButton {
    private final String url;

    public OpenInBroserButton(int x, int y, int height, int width, String url) {
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
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, width, height, (int) width, (int) height);
        if(buttonText == null) {
            return;
        }
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, buttonText, x + 2, y + 8, CustomColor.fromHexString("FFFFFF").asInt());
    }
}
