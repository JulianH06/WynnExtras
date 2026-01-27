package julianh06.wynnextras.utils.overlays;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class EasyButton extends EasyElement{
    @Nullable public String buttonText;
    CustomColor color = CustomColor.fromHexString("FFFFFF");

    public EasyButton(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public EasyButton(int x, int y, int height, int width, @Nullable String text) {
        this(x, y, height, width);
        buttonText = text;
    }

    @Override
    public void click() {}

    @Override
    public void draw(DrawContext context) {RenderUtils.drawRect(context, CustomColor.fromHexString("FFFFFF"), x, y, width, height);}

    public void drawWithTexture(DrawContext context, Identifier texture) {
        if(texture != null) {
            RenderUtils.drawTexturedRect(context, texture, CustomColor.NONE, x, y, width, height, (int) width, (int) height);
        }
        if(buttonText == null) {
            return;
        }
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, buttonText, x + 2, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
    }
}
