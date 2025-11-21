package julianh06.wynnextras.features.inventory.BankOverlayButtons;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.utils.overlays.EasyButton;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.annotation.Nullable;
import net.minecraft.screen.ScreenHandler;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;

public class DumpAllButton extends EasyButton {
    public DumpAllButton(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public DumpAllButton(int x, int y, int height, int width, @Nullable String text) {
        super(x, y, height, width, text);
    }

    @Override
    public void click() {
        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if(currScreenHandler == null) { return; }
        clickOnSlot(46, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
    }
}
