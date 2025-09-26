package julianh06.wynnextras.utils.render;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WEScreen extends Screen {
    protected DrawContext drawContext;
    protected double scaleFactor;

    protected int screenWidth;
    protected int screenHeight;
    protected int width;
    protected int height;
    protected int xStart;
    protected int yStart;

    protected WEScreen(Text title) {
        super(title);
    }

    protected void drawBackground() {
        if(MinecraftClient.getInstance().currentScreen == null) return;
        RenderUtils.drawRect(drawContext.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);
    }

    protected void drawText(String text, float x, float y, CustomColor color, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, TextShadow shadow, float textScale) {
        FontRenderer.getInstance().renderText(drawContext.getMatrices(), StyledText.fromComponent(Text.of(text)), xStart + (float) (x / scaleFactor), yStart + (float) (y / scaleFactor), color, horizontalAlignment, verticalAlignment, shadow, (float) (textScale / scaleFactor));
    }

    protected void drawText(String text, float x, float y, CustomColor color, HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, float textScale) {
        this.drawText(text, x, y, color, horizontalAlignment, verticalAlignment, TextShadow.NORMAL, textScale);
    }

    protected void drawText(String text, float x, float y, CustomColor color, float textScale) {
        this.drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, textScale);
    }

    protected void drawText(String text, float x, float y, CustomColor color) {
        this.drawText(text, x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
    }

    protected void drawText(String text, float x, float y) {
        this.drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.TOP, TextShadow.NORMAL, 1f);
    }

    protected void drawCenteredText(String text, float x, float y, CustomColor color, float textScale) {
        this.drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, textScale);
    }

    protected void drawCenteredText(String text, float x, float y, CustomColor color) {
        this.drawText(text, x, y, color, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1f);
    }

    protected void drawCenteredText(String text, float x, float y) {
        this.drawText(text, x, y, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, TextShadow.NORMAL, 1f);
    }

    protected void drawImage(Identifier texture, float x, float y, float width, float height) {
        RenderUtils.drawTexturedRect(
                drawContext.getMatrices(),
                texture,
                (float) (xStart + x / scaleFactor), (float) (yStart + y / scaleFactor),
                (float) (width / scaleFactor), (float) (height / scaleFactor),
                (int) (width / scaleFactor), (int) (height / scaleFactor)
        );
    }
}
