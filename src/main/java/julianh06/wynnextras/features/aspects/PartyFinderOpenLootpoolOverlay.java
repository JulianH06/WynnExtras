package julianh06.wynnextras.features.aspects;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

public class PartyFinderOpenLootpoolOverlay extends WEHandledScreen {
    LootPoolOpenButton lootPoolOpenButton = null;

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(MinecraftClient.getInstance().currentScreen == null) return;
        if(MinecraftClient.getInstance().currentScreen.getTitle() == null) return;
        if(MinecraftClient.getInstance().currentScreen.getTitle().getString() == null) return;
        if(!(MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE4\uE03E") ||
            MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE4\uE03F") ||
            MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("\uDAFF\uDFE1\uE00C"))) return;

        //" \uDAFF\uDFE4\uE03F"
        //" \uDAFF\uDFE1\uE00C" NOTG
        //WynnExtras.LOGGER.info(MinecraftClient.getInstance().currentScreen.getTitle().getString());

        if(lootPoolOpenButton == null) {
            lootPoolOpenButton = new LootPoolOpenButton(0, 0, 0, 0);
            rootWidgets.add(lootPoolOpenButton);
        }

        lootPoolOpenButton.setBounds(0, 0, 400, 60);

    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    private static class LootPoolOpenButton extends Widget {
        public LootPoolOpenButton(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText("View Weekly Lootpools", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            if(MinecraftClient.getInstance().currentScreen != null) MinecraftClient.getInstance().currentScreen.close();
            WEScreen.open(AspectScreen::new);
            AspectScreen.currentPage = AspectScreen.Page.LootPools;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }
}
