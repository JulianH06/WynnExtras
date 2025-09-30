package julianh06.wynnextras.core;

import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.profileviewer.*;
import julianh06.wynnextras.utils.render.WEScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

public class MainScreen extends WEScreen {
    protected MainScreen() {
        super(Text.of("WynnExtras"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(MinecraftClient.getInstance().getWindow() == null) return;
        super.drawContext = context;
        super.scaleFactor = MinecraftClient.getInstance().getWindow().getScaleFactor();
        scaleFactor = (int) super.scaleFactor;
        if(scaleFactor == 0) return;

        screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        width = (int) (600 * 3 / scaleFactor);
        height = (int) (250 * 3 / scaleFactor);
        xStart = screenWidth / 2 - width / 2;
        yStart = screenHeight / 2 - height / 2;
        drawBackground();
    }
}
