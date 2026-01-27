package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class PlayerWidget extends Widget {
    static Identifier playerTabTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/playertab.png");
    static Identifier longPlayerTabTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/playertab_long.png");

    static Identifier playerTabTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/playertab_dark.png");
    static Identifier longPlayerTabTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/playertab_long_dark.png");

    public int index;
    private final Runnable action;

    public PlayerWidget(int i) {
        super(0, 0, 100, 100);
        index = i;
        this.action = () -> {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            PV.open(lastViewedPlayers.get(index));
        };
    }

    public void draw(DrawContext ctx, int x, int y) {
        //System.out.println(hovered);
        if(this.ui == null) return;
        setBounds(x, y, 100, 80);
        //ui.drawText(lastViewedPlayers.get(index), x, y);
        if(hovered) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(longPlayerTabTextureDark, longPlayerTabTexture, x - 3, y, 110, 80, ui);
        } else {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(playerTabTextureDark, playerTabTexture, x - 3, y, 100, 80, ui);
        }

        //first layer of the head
        Identifier texture = lastViewedPlayersSkins.get(lastViewedPlayers.get(index));
        if(texture == null) return;
        RenderUtils.drawTexturedRect(
                ctx,
                texture,
                ui.sx(x + 22 + (hovered ? 10 : 0)), ui.sy(y + 10),
                ui.sw(60), ui.sh(60),
                8, 8, 8, 8,
                64, 64
        );

        //second layer
        RenderUtils.drawTexturedRect(
                ctx,
                texture,
                ui.sx(x + 22 + (hovered ? 10 : 0)), ui.sy(y + 10),   // z = 1 = über dem Kopf, removed in 1.21.11?
                ui.sw(60), ui.sh(60),
                40, 8, 8, 8,
                64, 64
        );
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hovered) {
            //ui.drawText(lastViewedPlayers.get(index), (float) (mouseX * ui.getScaleFactor() + 10), (float) (mouseY * ui.getScaleFactor() + 10));
            ctx.drawTooltip(McUtils.mc().textRenderer, Text.of(lastViewedPlayers.get(index)), mouseX, mouseY);
        }
        //uses custom draw method which gets called in updatevalues to render over the background
    }

    @Override
    protected boolean onClick(int button) {
        if (!isEnabled()) return false;
        if (action != null) action.run();
        return true;
    }
}
