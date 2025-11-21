package julianh06.wynnextras.features.inventory.BankOverlayButtons;

import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

import static com.wynntils.utils.wynn.ContainerUtils.shiftClickOnSlot;

public class DumpExceptHotbarButton extends EasyButton {
    public DumpExceptHotbarButton(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public DumpExceptHotbarButton(int x, int y, int height, int width, @Nullable String text) {
        super(x, y, height, width, text);
    }

    @Override
    public void click() {
        ScreenHandler currScreenHandler = McUtils.containerMenu();
        if(currScreenHandler == null) { return; }
        shiftClickOnSlot(46, currScreenHandler.syncId, 1, currScreenHandler.getStacks());
    }
}
