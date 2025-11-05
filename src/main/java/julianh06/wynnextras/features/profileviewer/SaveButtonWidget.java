package julianh06.wynnextras.features.profileviewer;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.features.abilitytree.AbilityIdConverter;
import julianh06.wynnextras.features.abilitytree.TreeData;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.data.SkillPoints;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SaveButtonWidget extends Widget {
    private String characterUUID;
    private Runnable action;
    public AbilityTreeData classTree = null;

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
            String abilityFileName = playerName + "_" + characterUUID + ".json";
            AbilityIdConverter.convert(className.toLowerCase(), abilityFileName); // calls method from your new class with class argument
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
        ui.drawRect(x, y, width, height, CustomColor.fromHexString("FFFFFF"));
    }

    public void setCharacterUUID(String characterUUID) {
        this.characterUUID = characterUUID;
    }
}
