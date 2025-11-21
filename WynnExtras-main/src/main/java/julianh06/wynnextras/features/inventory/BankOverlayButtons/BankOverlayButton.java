package julianh06.wynnextras.features.inventory.BankOverlayButtons;

import com.wynntils.screens.playerviewer.widgets.SimplePlayerInteractionButton;
import com.wynntils.utils.render.Texture;
import net.minecraft.text.Text;

public class BankOverlayButton extends SimplePlayerInteractionButton {
    public BankOverlayButton(int x, int y, Text message, Texture icon, Runnable runnable) {
        super(x, y, message, icon, runnable);
    }

    @Override
    public void onPress() {
        System.out.println("PersonalStorageUtils");
    }
}
