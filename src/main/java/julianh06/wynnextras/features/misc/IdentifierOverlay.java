package julianh06.wynnextras.features.misc;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.ItemIdentifierContainer;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class IdentifierOverlay extends WEHandledScreen {
    SourceOfThruthOpenerWidget sourceOfThruthOpenerWidget = null;

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof ItemIdentifierContainer)) return;

        RenderUtils.drawRect(ctx.getMatrices(), CustomColor.fromInt(-804253680), 0, 0, 0, MinecraftClient.getInstance().currentScreen.width, MinecraftClient.getInstance().currentScreen.height);

        if(sourceOfThruthOpenerWidget == null) {
            sourceOfThruthOpenerWidget = new SourceOfThruthOpenerWidget(0, 0, 0, 0);
            rootWidgets.add(sourceOfThruthOpenerWidget);
        }

        Screen screen = McUtils.screen();
        if (!(screen instanceof HandledScreen<?> containerScreen)) return;
        int yPos = (int) (((HandledScreenAccessor) containerScreen).getY() - 105 / ui.getScaleFactor());

        sourceOfThruthOpenerWidget.setBounds((int) ((screenWidth * ui.getScaleFactor()) / 2) - 200, (int) (yPos * ui.getScaleFactor()), 400, 60);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    private static class SourceOfThruthOpenerWidget extends Widget {
        public SourceOfThruthOpenerWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawCenteredText("Should i keep gambling?", x + width / 2f, y - 25);
            ui.drawButton(x, y, width, height, 13, hovered);
            ui.drawCenteredText("Find out here!", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.mc().currentScreen = null;
            WEScreen.open(SourceOfThruth::new);
            return true;
        }
    }
}
