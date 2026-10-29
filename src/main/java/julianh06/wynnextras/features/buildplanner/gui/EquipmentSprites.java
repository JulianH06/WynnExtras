package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.utils.UI.UIUtils;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

final class EquipmentSprites {
    private static final Identifier TEXTURE = Identifier.of("wynnextras", "textures/gui/buildplanner/equipment.png");
    private EquipmentSprites() {}

    static void draw(DrawContext context, String type, int x, int y, int size) {
        UIUtils ui = new UIUtils(context, 1.0, 0, 0);
        int index = List.of("bow", "spear", "wand", "dagger", "relik", "helmet", "chestplate", "leggings",
                "boots", "ring", "bracelet", "necklace").indexOf(type);
        if (index >= 0) {
            ui.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, index * 120, 0,
                    size, size, 120, 120, 1440, 120);
            return;
        }
        ItemStack stack = new ItemStack(switch (type) {
            case "potion" -> Items.POTION;
            case "scroll" -> Items.PAPER;
            case "food" -> Items.BREAD;
            default -> Items.BOOK;
        });
        ui.pushMatrix();
        ui.translate(x, y);
        ui.scale(size / 16.0F, size / 16.0F);
        ui.drawItem(stack, 0, 0);
        ui.popMatrix();
    }
}
