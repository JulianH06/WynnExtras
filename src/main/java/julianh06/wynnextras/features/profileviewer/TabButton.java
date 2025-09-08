package julianh06.wynnextras.features.profileviewer;

import julianh06.wynnextras.utils.overlays.EasyButton;

public class TabButton extends EasyButton {
    PVScreen.Tab tab;

    public TabButton(int x, int y, int height, int width, PVScreen.Tab tab) {
        super(x, y, height, width);
        this.tab = tab;
    }

    @Override
    public void click() {
        PVScreen.currentTab = tab;
    }
}
