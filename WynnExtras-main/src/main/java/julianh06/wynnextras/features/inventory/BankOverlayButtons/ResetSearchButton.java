package julianh06.wynnextras.features.inventory.BankOverlayButtons;

import julianh06.wynnextras.utils.overlays.EasyButton;
import julianh06.wynnextras.utils.overlays.EasyTextInput;
import org.jetbrains.annotations.Nullable;

public class ResetSearchButton extends EasyButton {
    EasyTextInput input;

    public ResetSearchButton(int x, int y, int height, int width, EasyTextInput input, @Nullable String text) {
        super(x, y, height, width, text);
        this.input = input;
    }

    @Override
    public void click() {
        input.clearInput();
        input.setActive(false);
    }
}
