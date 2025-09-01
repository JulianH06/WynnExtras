package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.utils.overlays.EasyButton;

public class RaidFilterButton extends EasyButton {
    public boolean isActive = true;

    public RaidFilterButton(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    @Override
    public void click() {
        RaidListScreen.currentCollapsed = -1;
        //RaidListScreen.scrollOffset = 0;
        isActive = !isActive;
    }
}
