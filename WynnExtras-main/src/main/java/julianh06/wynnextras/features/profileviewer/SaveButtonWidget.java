package julianh06.wynnextras.features.profileviewer;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.abilitytree.TreeData;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.data.SkillPoints;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SaveButtonWidget extends Widget {
    private String characterUUID;
    private Runnable action;
    public AbilityTreeData classTree = null;

    static Identifier saveButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/savebutton.png");
    static Identifier saveButtonTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/savebutton_dark.png");
    static Identifier saveButtonTextureHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/savebuttonhovered.png");
    static Identifier saveButtonTextureHoveredDark = Identifier.of("wynnextras", "textures/gui/profileviewer/savebuttonhovered_dark.png");

    public void setClassTree(AbilityTreeData classTree) {
        this.classTree = classTree;
    }

    public SaveButtonWidget(String playerName, String className, SkillPoints skillPoints, AbilityMapData classMap, AbilityMapData playerTree) {
        super(0, 0, 0, 0);
        this.action = () -> {
            if(classTree == null) {
                McUtils.sendMessageToClient(Text.of("Error while setting classtree, try again"));
                return;
            }
            TreeLoader.savePlayerAbilityTree(playerName, characterUUID, className.toLowerCase(), skillPoints, classMap, classTree, playerTree);
            //String abilityFileName = playerName + "_" + characterUUID + ".json";
            //AbilityIdConverter.convert(className.toLowerCase(), abilityFileName); // calls method from your new class with class argument
            TreeData.loadAll();
        };
    }


//    public void setValues(String playerName, String characterUUID, String className) {
//        this.playerName = playerName;
//        this.characterUUID = characterUUID;
//        this.className = className;
//    }

    @Override
    protected boolean onClick(int button) {
        if (!isEnabled()) return false;
        if (action != null) action.run();
        return true;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("FFFFFF"));
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(hovered ? saveButtonTextureHoveredDark : saveButtonTextureDark, x, y, width, height);
        else ui.drawImage(hovered ? saveButtonTextureHovered : saveButtonTexture, x, y, width, height);
        ui.drawCenteredText("Save", x + 287.5f, y + 80, CustomColor.fromHexString("FFFFFF"), 6f);
        ui.drawCenteredText("Tree", x + 287.5f, y + 155, CustomColor.fromHexString("FFFFFF"), 6f);
    }

    public void setCharacterUUID(String characterUUID) {
        this.characterUUID = characterUUID;
    }
}
