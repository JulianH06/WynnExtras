package julianh06.wynnextras.features.buildplanner.gui;

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
        int index = List.of("bow", "spear", "wand", "dagger", "relik", "helmet", "chestplate", "leggings",
                "boots", "ring", "bracelet", "necklace").indexOf(type);
        if (index >= 0) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, index * 120, 0,
                    size, size, 120, 120, 1440, 120);
            return;
        }
        ItemStack stack = new ItemStack(switch (type) {
            case "potion" -> Items.POTION;
            case "scroll" -> Items.PAPER;
            case "food" -> Items.BREAD;
            default -> Items.BOOK;
        });
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(size / 16.0F, size / 16.0F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
    }
}
