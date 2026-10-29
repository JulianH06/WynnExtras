package julianh06.wynnextras.features.crafting.wynnbuilder;

import julianh06.wynnextras.wynncraft.codec.WynnBuilderCraftCodec;

public class WynnBuilderDecoder {
    public static DecodedCraft decode(String input) {
        var craft = WynnBuilderCraftCodec.decode(input);
        return craft == null ? null : new DecodedCraft(craft.ingredientIds(), craft.recipeId(),
                craft.material1Tier(), craft.material2Tier(), craft.attackSpeed());
    }

    public static boolean isNoIngredient(int id) {
        return WynnBuilderCraftCodec.isNoIngredient(id);
    }
}
