package julianh06.wynnextras.utils.UI;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class ImageWidget extends Widget{
    Identifier image;

    public ImageWidget(Identifier image, int x, int y, int width, int height, UIUtils ui) {
        super(x, y, width, height);
        // set default action
        this.ui = ui;
        this.image = image;
    }

    public ImageWidget(Identifier image, UIUtils ui) {
        this(image, 0, 0, 0, 0, ui);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.drawImage(image, x, y, width, height);
    }
}
