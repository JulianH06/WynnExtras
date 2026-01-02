package julianh06.wynnextras.features.crafting;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.Material;
import julianh06.wynnextras.features.crafting.data.ScribingRecipes;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.List;

public class CraftingHelperOverlay extends WEHandledScreen {
    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;

        ui.drawBackground();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        switch (container.getProfessionType()) {
            case WEAPONSMITHING -> {
                ui.drawText("WEAPONSMITHING", 0, 0);

            }
            case ARMOURING -> {
                ui.drawText("ARMOURING", 0, 0);

            }
            case WOODWORKING -> {

                ui.drawText("WOODWORKING", 0, 0);
            }
            case JEWELING -> {
                ui.drawText("JEWELING", 0, 0);

            }
            case ALCHEMISM -> {
                ui.drawText("ALCHEMISM", 0, 0);

            }
            case SCRIBING -> {

                ui.drawText("SCRIBING", 0, 0);
                ui.drawRect(((HandledScreenAccessor) screen).getX() * ui.getScaleFactorF(), ((HandledScreenAccessor) screen).getY() * ui.getScaleFactorF(), 1000, 1000, CustomColor.fromHexString("000000"));
                drawRecipe(0, 50, 0, ScribingRecipes.INSTANCE);
                drawRecipe(0, 100, 10, ScribingRecipes.INSTANCE);
                drawRecipe(0, 150, 20, ScribingRecipes.INSTANCE);
                drawRecipe(0, 200, 30, ScribingRecipes.INSTANCE);
                drawRecipe(0, 250, 40, ScribingRecipes.INSTANCE);
                drawRecipe(0, 300, 50, ScribingRecipes.INSTANCE);
                drawRecipe(0, 350, 60, ScribingRecipes.INSTANCE);
                drawRecipe(0, 400, 70, ScribingRecipes.INSTANCE);
                drawRecipe(0, 450, 80, ScribingRecipes.INSTANCE);
                drawRecipe(0, 500, 90, ScribingRecipes.INSTANCE);
                drawRecipe(0, 550, 100, ScribingRecipes.INSTANCE);
                drawRecipe(0, 600, 103, ScribingRecipes.INSTANCE);
                drawRecipe(0, 650, 105, ScribingRecipes.INSTANCE);
            }
            case COOKING -> {
                ui.drawText("COOKING", 0, 0);

            }
            case TAILORING -> {
                ui.drawText("TAILORING", 0, 0);

            }
            case null, default -> {

            }
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    private void drawRecipe(int x, int y, int level, IRecipeData recipe) {
        List<Pair<Material, Integer>> materials = recipe.getMaterials(level);
        if(materials.isEmpty()) return;
        if(materials.size() < 2) return;

        ui.drawRect(x, y, 500, 60, CustomColor.fromHexString("080808"));

        ui.drawImage(materials.getFirst().getFirst().getTexture(), x, y + 5, 20, 20);
        ui.drawText(materials.getFirst().getFirst().getName() + " " + materials.getFirst().getSecond(), x + 30, y + 5);

        ui.drawImage(materials.get(1).getFirst().getTexture(), x, y + 35, 20, 20);
        ui.drawText(materials.get(1).getFirst().getName() + " " + materials.get(1).getSecond(), x + 30, y + 35);
    }
}
//TODO: fix paper textures