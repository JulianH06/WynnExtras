package julianh06.wynnextras.utils.overlays;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
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
    public void click() {
        System.out.println("deleteButton pressed");
    }

    @Override
    public void draw(DrawContext context) {
        RenderUtils.drawRect(context.getMatrices(), color, x, y, 0.0f, width, height);
        if(buttonText == null) {
            return;
        }
        context.drawText(MinecraftClient.getInstance().textRenderer, buttonText, x + 1, y + 1, CustomColor.fromHexString("000000").asInt(), false);
    }

    public void drawWithTexture(DrawContext context, Identifier texture) {
        RenderUtils.drawTexturedRect(context.getMatrices(), texture, x, y, width, height, (int) width, (int) height);
        if(buttonText == null) {
            return;
        }
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, buttonText, x + 2, y + 3, CustomColor.fromHexString("FFFFFF").asInt());
    }
}
