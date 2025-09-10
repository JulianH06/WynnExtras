package julianh06.wynnextras.features.profileviewer;

import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.overlays.EasyButton;

public class CharacterButton extends EasyButton {
    CharacterData character;

    public CharacterButton(int x, int y, int height, int width, CharacterData character) {
        super(x, y, height, width);
        this.character = character;
    }

    @Override
    public void click() {
        if(PVScreen.selectedCharacter == character) {
            PVScreen.selectedCharacter = null;
            return;
        }
        PVScreen.selectedCharacter = character;
    }

    public void setCharacter(CharacterData data) {
        character = data;
    }
}
