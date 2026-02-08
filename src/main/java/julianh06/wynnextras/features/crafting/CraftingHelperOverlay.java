package julianh06.wynnextras.features.crafting;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.worlds.type.BombInfo;
import com.wynntils.models.worlds.type.BombType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Time;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.crafting.data.*;
import julianh06.wynnextras.features.crafting.data.recipes.AlchemismRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.CookingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.ChestplateRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.HelmetRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.ScribingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.BraceletRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.NecklaceRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.jeweling.RingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.tailoring.BootsRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.tailoring.LeggingsRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing.DaggerRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.weaponsmithing.SpearRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.BowRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.RelikRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.woodworking.WandRecipes;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class CraftingHelperOverlay extends WEHandledScreen {
    private static boolean registeredScroll = false;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms
    public static float targetOffset = 0;
    public static float actualOffset = 0;

    static HelperWidget helperWidget;

    SelectionWidget selectionWidget1;
    SelectionWidget selectionWidget2;
    SelectionWidget selectionWidget3;

    private static RecipeState state = RecipeState.NONE;

    private final static Map<ProfessionType, Map<RecipeState, Float>> lastOffset = new HashMap<>();
    private final static Map<ProfessionType, RecipeState> lastState = new HashMap<>();

    Identifier l = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/l.png");
    Identifier r = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/r.png");
    Identifier t = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/t.png");
    Identifier b = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/b.png");
    Identifier tl = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/tl.png");
    Identifier tr = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/tr.png");
    Identifier bl = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/bl.png");
    Identifier br = Identifier.of("wynnextras", "textures/gui/craftinghelper/light/br.png");

    Identifier ld = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/l.png");
    Identifier rd = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/r.png");
    Identifier td = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/t.png");
    Identifier bd = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/b.png");
    Identifier tld = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/tl.png");
    Identifier trd = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/tr.png");
    Identifier bld = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/bl.png");
    Identifier brd = Identifier.of("wynnextras", "textures/gui/craftinghelper/dark/br.png");


    ProfBombWidget profSpeedBombWidget;
    ProfBombWidget profXpBombWidget;

    static ScrollBarWidget scrollBarWidget = null;

    static String statusMessage = "";

    public CraftingHelperOverlay() {
        state = RecipeState.NONE;
        helperWidget = null;
        selectionWidget1 = null;
        selectionWidget2 = null;
        selectionWidget3 = null;
        profSpeedBombWidget = null;
        profXpBombWidget = null;
        actualOffset = 0;
        targetOffset = ui == null ? -10 : -10 / ui.getScaleFactorF();
        statusMessage = "";

        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        ProfessionType type = container.getProfessionType();

        if(type == null) return;

        if(lastState.isEmpty()) return;

        state = lastState.get(type);

        Map<RecipeState, Float> offsets = lastOffset.get(type);
        if (offsets == null) return;

        Float offset = offsets.get(state);
        if (offset == null) return;

        actualOffset = offset;
        targetOffset = offset;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) { }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        if(state == null) state = RecipeState.NONE;

        int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
        int yStart = (int) (((HandledScreenAccessor) screen).getY() + (70 / ui.getScaleFactor()));
        int widgetWidth = 600;
        int widgetHeight = (int) (((HandledScreenAccessor) screen).getBackgroundHeight() - (24 * 3 / ui.getScaleFactor()));

        if(profSpeedBombWidget == null) profSpeedBombWidget = new ProfBombWidget(BombType.PROFESSION_SPEED);
        if(profXpBombWidget == null) profXpBombWidget = new ProfBombWidget(BombType.PROFESSION_XP);

        int speedWidth = (int) (MinecraftClient.getInstance().textRenderer.getWidth(profSpeedBombWidget.text) * ui.getScaleFactor());
        int xpWidth = (int) (MinecraftClient.getInstance().textRenderer.getWidth(profXpBombWidget.text) * ui.getScaleFactor());
        profSpeedBombWidget.setBounds((int) ((screen.width / 2f) * ui.getScaleFactorF() - speedWidth / 2f), (int) (((HandledScreenAccessor) screen).getY() * ui.getScaleFactorF() - 100), speedWidth, 30);
        profXpBombWidget.setBounds((int) ((screen.width / 2f) * ui.getScaleFactorF() - xpWidth / 2f), (int) (((HandledScreenAccessor) screen).getY() * ui.getScaleFactorF() - 140), xpWidth, 30);

        profSpeedBombWidget.draw(ctx, mouseX, mouseY, delta, ui);
        profXpBombWidget.draw(ctx, mouseX, mouseY, delta, ui);

        boolean dontShowWorldText = false;

        if(profSpeedBombWidget.bomb != null && profSpeedBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName())) dontShowWorldText = true;
        if(profXpBombWidget.bomb != null && profXpBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName())) dontShowWorldText = true;

        if ((profXpBombWidget.isActive || profSpeedBombWidget.isActive) && !dontShowWorldText) {
            int currentWorldTextYOffset = profXpBombWidget.isActive ? 170 : 130;
            ui.drawCenteredText("There are no active profession bombs on your world. Click below to switch worlds.", (screen.width / 2f) * ui.getScaleFactorF(), (int) (((HandledScreenAccessor) screen).getY() * ui.getScaleFactorF() - currentWorldTextYOffset), CustomColor.fromHexString("FF0000"));
        }

        if(!profXpBombWidget.isActive && !profSpeedBombWidget.isActive) {
            ui.drawCenteredText("There are no active profession bombs.", (screen.width / 2f) * ui.getScaleFactorF(), (int) (((HandledScreenAccessor) screen).getY() * ui.getScaleFactorF() - 90), CustomColor.fromHexString("FF0000"));
        }

        ProfessionType type = container.getProfessionType();
        lastState.put(type, state);
        
        if(selectionWidget1 == null) {
            selectionWidget1 = new SelectionWidget(0);
            rootWidgets.add(selectionWidget1);
        }

        if(selectionWidget2 == null) {
            selectionWidget2 = new SelectionWidget(1);
            rootWidgets.add(selectionWidget2);
        }

        if(selectionWidget3 == null) {
            selectionWidget3 = new SelectionWidget(2);
            rootWidgets.add(selectionWidget3);
        }

        boolean big = false;

        switch (type) {
            case JEWELING, WOODWORKING -> {
                setupSelectionWidget(selectionWidget1, type, 0, 3, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget2, type, 1, 3, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget3, type, 2, 3, xStart, yStart, widgetWidth);
            }
            case WEAPONSMITHING, ARMOURING, TAILORING -> {
                setupSelectionWidget(selectionWidget1, type, 0, 2, xStart, yStart, widgetWidth);
                setupSelectionWidget(selectionWidget2, type, 1, 2, xStart, yStart, widgetWidth);
                selectionWidget3.setBounds(0, 0, 0, 0);
            }
            case null, default -> {
                selectionWidget1.setBounds(0, 0, 0, 0);
                selectionWidget2.setBounds(0, 0, 0, 0);
                selectionWidget3.setBounds(0, 0, 0, 0);
                big = true;
            }
        }

        if(WynnExtrasConfig.INSTANCE.craftingHelperDarkMode) {
            ui.drawNineSlice((int) (xStart * ui.getScaleFactor() + 5),
                    (int) (yStart * ui.getScaleFactor()) - (big ? 66 : 0), widgetWidth,
                    (int) (widgetHeight * ui.getScaleFactor()) + (big ? 66 : 0), 33, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
        } else {
            ui.drawNineSlice((int) (xStart * ui.getScaleFactor() + 5),
                    (int) (yStart * ui.getScaleFactor()) - (big ? 66 : 0), widgetWidth,
                    (int) (widgetHeight * ui.getScaleFactor()) + (big ? 66 : 0), 33, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
        }

        int step = 142;
        int recipeWidgetAmount = 12;

        int contentHeight = recipeWidgetAmount * step;

        int visibleHeight = helperWidget == null ? 0 : helperWidget.getHeight();

        int maxOffset = Math.max(0, contentHeight - visibleHeight);

        if(helperWidget == null) {
            helperWidget = new HelperWidget(maxOffset);
            rootWidgets.add(helperWidget);
        }

        if(helperWidget.recipeData == null) {
            IRecipeData data = getRecipeDataInstance(type);
            helperWidget.setRecipeData(data);
        }

        if(scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget(maxOffset);
        }

        helperWidget.maxOffset = maxOffset;
        scrollBarWidget.maxOffset = maxOffset;

        scrollBarWidget.setBounds((int) ((xStart + 5) * ui.getScaleFactor()) + widgetWidth, (int) ((int) (((HandledScreenAccessor) screen).getY() + (big ? 10 : 70) / ui.getScaleFactor()) * ui.getScaleFactor()), 30, (int) ((((HandledScreenAccessor) screen).getBackgroundHeight() - (big ? 12 : 75) / ui.getScaleFactor()) * ui.getScaleFactor()));
        scrollBarWidget.draw(ctx, mouseX, mouseY, delta, ui);

        int scissorX1 = xStart;
        int scissorY1 = (int) (yStart + Math.round((big ? - 46.5f : 20) / ui.getScaleFactor()));
        int scissorX2 = xStart + widgetWidth;
        int scissorY2 = (int) (yStart + widgetHeight - Math.round(20 / ui.getScaleFactor()));

        ui.drawCenteredText(statusMessage, (xStart + (ui.getScaleFactorF() == 2 ? 40 : 0)) * ui.getScaleFactorF(), (((HandledScreenAccessor) screen).getY() + ((HandledScreenAccessor) screen).getBackgroundHeight() + 10) * ui.getScaleFactorF(), CustomColor.fromHexString("FF0000"));

        ctx.enableScissor(
                scissorX1,
                scissorY1,
                scissorX2,
                scissorY2);

        selectionWidget1.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget2.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget3.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);

        helperWidget.setBounds((int) (xStart * ui.getScaleFactor() + 5), (int) ((yStart + (big ? - 15 : 7)) * ui.getScaleFactor()), widgetWidth, (int) ((widgetHeight + (big ? 12 : - 14)) * ui.getScaleFactor()));
    }
    
    private void setupSelectionWidget(SelectionWidget selectionWidget, ProfessionType type, int i, int maxWidgets, int xStart, int yStart, int widgetWidth) {
        int spacing = 20;

        int totalSpacing = spacing * (maxWidgets - 1);
        int sectionWidth = (widgetWidth - totalSpacing) / maxWidgets;

        int x = (int) ((xStart + 2) * ui.getScaleFactor()) + i * (sectionWidth + spacing);
        int y = (int) (yStart * ui.getScaleFactor()) - 60;

        selectionWidget.setBounds(x, y, sectionWidth, 50);

        selectionWidget.setText(getSelectorText(type, i));
    }

    private String getSelectorText(ProfessionType type, int i) {
        return switch (type) {
            case ARMOURING -> switch(i) {
                case 0 -> "Helmet";
                case 1 -> "Chestplate";
                default -> null;
            };
            case WOODWORKING -> switch (i) {
                case 0 -> "Bow";
                case 1 -> "Wand";
                case 2 -> "Relik";
                default -> null;
            };
            case JEWELING -> switch (i) {
                case 0 -> "Ring";
                case 1 -> "Bracelet";
                case 2 -> "Necklace";
                default -> null;
            };
            case TAILORING -> switch (i) {
                case 0 -> "Pants";
                case 1 -> "Boots";
                default -> null;
            };
            case WEAPONSMITHING -> switch (i) {
                case 0 -> "Spear";
                case 1 -> "Dagger";
                default -> null;
            };
            case null, default -> null;
        };
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?>)) return;

        try {
            ctx.disableScissor();
        } catch (Exception ignored) {}
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if(scrollBarWidget != null) scrollBarWidget.mouseClicked(x, y, button);
        if(profSpeedBombWidget != null) profSpeedBombWidget.mouseClicked(x, y, button);
        if(profXpBombWidget != null) profXpBombWidget.mouseClicked(x, y, button);
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if(scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        return super.mouseReleased(x, y, button);
    }

    private static IRecipeData getRecipeDataInstance(ProfessionType type) {
        if(state == null) return null;

        return switch (type) {
            case WEAPONSMITHING -> switch (state) {
                case FIRST -> SpearRecipes.INSTANCE;
                case SECOND -> DaggerRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case ARMOURING -> switch (state) {
                case FIRST -> HelmetRecipes.INSTANCE;
                case SECOND -> ChestplateRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case WOODWORKING -> switch (state) {
                case FIRST -> BowRecipes.INSTANCE;
                case SECOND -> WandRecipes.INSTANCE;
                case THIRD -> RelikRecipes.INSTANCE;
                case NONE -> null;
            };
            case JEWELING -> switch (state) {
                case FIRST -> RingRecipes.INSTANCE;
                case SECOND -> BraceletRecipes.INSTANCE;
                case THIRD -> NecklaceRecipes.INSTANCE;
                case NONE -> null;
            };
            case ALCHEMISM -> AlchemismRecipes.INSTANCE;
            case SCRIBING -> ScribingRecipes.INSTANCE;
            case COOKING -> CookingRecipes.INSTANCE;
            case TAILORING -> switch (state) {
                case FIRST -> LeggingsRecipes.INSTANCE;
                case SECOND -> BootsRecipes.INSTANCE;
                case NONE, THIRD -> null;
            };
            case null, default ->  null;
        };
    }

    private static void drawRecipe(DrawContext ctx, int x, int y, int width, int height, int level,
                                   IRecipeData recipe, boolean hovered, UIUtils ui) {
        if(recipe == null) return;

        List<Pair<IMaterial, Integer>> materials = recipe.getMaterials(level);

        if(materials.isEmpty() || materials.size() < 2) return;

        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("080808"));

        drawMaterialIcon(ctx, ui, materials.getFirst().getFirst(), x + 10, y + 5, 60);
        ui.drawText(materials.getFirst().getFirst().getName() + " " + materials.getFirst().getSecond(), x + 80, y + height / 4f + 4, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);

        drawMaterialIcon(ctx, ui, materials.get(1).getFirst(), x + 10, y + 60, 60);
        ui.drawText(materials.get(1).getFirst().getName() + " " + materials.get(1).getSecond(), x + 80, y + 3 * height / 4f - 4, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);
    }

    private static void drawMaterialIcon(DrawContext ctx, UIUtils ui, IMaterial material, float x, float y, float size) {
        ItemStack stack = buildMaterialStack(material);
        if (shouldUseVcit(stack)) {
            drawItemScaled(ctx, ui, stack, x, y, size);
            return;
        }
        ui.drawImage(material.getTexture(), x, y, size, size);
    }

    private static ItemStack buildMaterialStack(IMaterial material) {
        ItemStack inventoryMatch = findInventoryMaterial(material);
        if (inventoryMatch != null && !inventoryMatch.isEmpty()) {
            return inventoryMatch;
        }
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Refined " + material.getName() + " "));
        return stack;
    }

    private static ItemStack findInventoryMaterial(IMaterial material) {
        if (McUtils.containerMenu() == null) {
            return null;
        }
        List<Slot> slots = McUtils.containerMenu().slots;
        for (Slot slot : slots) {
            try {
                if (!(slot.inventory instanceof PlayerInventory)) {
                    continue;
                }
                ItemStack stack = slot.getStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Text name = stack.getCustomName();
                if (name != null && name.getString().contains(material.getName())) {
                    return stack;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean shouldUseVcit(ItemStack stack) {
        if (!WynnExtrasConfig.INSTANCE.craftingDynamicTextures) {
            return false;
        }
        return VcitCompat.hasModel(stack);
    }

    private static void drawItemScaled(DrawContext ctx, UIUtils ui, ItemStack stack, float x, float y, float size) {
        float px = ui.sx(x);
        float py = ui.sy(y);
        float scale = (float) ui.sw(size) / 16.0f;
        ctx.getMatrices().pushMatrix();
        //ctx.getMatrices().translate(px, py, 100.0f);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().popMatrix();
    }


    private enum RecipeState {
        NONE,
        FIRST,
        SECOND,
        THIRD
    }

    private static class HelperWidget extends Widget {
        IRecipeData recipeData;
        List<RecipeWidget> recipeWidgets = new ArrayList<>();
        private static final Queue<Integer> CLICK_QUEUE = new ArrayDeque<>();
        public int maxOffset;
        private static long lastClick = 0;

        public HelperWidget(int maxOffset) {
            super(0, 0, 0, 0);
            this.maxOffset = maxOffset;
            recipeData = null;

            if(MinecraftClient.getInstance().currentScreen == null) return;
            ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                    screen,
                    mX,
                    mY,
                    horizontalAmount,
                    verticalAmount,
                    consumed
            ) -> {
                long now = System.currentTimeMillis();
                if (now - lastScrollTime < scrollCooldown) {
                    return true;
                }
                lastScrollTime = now;

                if (hovered) {
                    if (verticalAmount > 0) {
                        targetOffset -= 104f;
                    } else /*if(canScrollFurther)*/ {
                        targetOffset += 104f;
                    }
                }
                return true;
            });
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
            ProfessionType type = container.getProfessionType();

            if(state == RecipeState.NONE && type != ProfessionType.ALCHEMISM && type != ProfessionType.COOKING && type != ProfessionType.SCRIBING) {
                ui.drawCenteredText("Select the type", x + width / 2f, y + height / 2f - 30, CustomColor.fromHexString("FF0000"), 4);
                ui.drawCenteredText("you want to craft.", x + width / 2f, y + height / 2f + 30, CustomColor.fromHexString("FF0000"), 4);
            }

            if(recipeData == null) return;

            float snapValue = 0.5f;

            int widgetHeight = 130;
            int widgetAmount = 12;

            boolean big = type == ProfessionType.ALCHEMISM || type == ProfessionType.COOKING || type == ProfessionType.SCRIBING;
            targetOffset = ui == null ? 0 : Math.clamp(targetOffset, big ? (-8 * (ui.getScaleFactorF() - 3)) : 0, maxOffset);

            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            Map<RecipeState, Float> map = lastOffset.get(type) == null ? new HashMap<>() : lastOffset.get(type);
            map.put(state, actualOffset);
            lastOffset.put(type, map);

            if(recipeWidgets.isEmpty()) {
                for (int i = 0; i < widgetAmount; i++) {
                    int level = i * 10;
                    if(i == 11) level = 103;

                    RecipeWidget recipeWidget = new RecipeWidget(recipeData, i, level);

                    recipeWidgets.add(recipeWidget);
                    addChild(recipeWidget);
                }
            }

            for (int i = 0; i < widgetAmount; i++) {
                int baseY = y + 10 + 140 * i;
                int drawY = baseY - (int) actualOffset;

                recipeWidgets.get(i).setBounds(
                        x + 30,
                        drawY,
                        width - 60,
                        widgetHeight
                );
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (contains((int) mx, (int) my)) {
                resetMaterialSlots();
            }

            return super.mouseClicked(mx, my, button);
        }

        public void setRecipeData(IRecipeData recipeData) {
            this.recipeData = recipeData;
            recipeWidgets.clear();
            children.clear();
            resetMaterialSlots();
        }

        private static void resetMaterialSlots() {
            ContainerUtils.clickOnSlot(0, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
            ContainerUtils.clickOnSlot(9, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
            CLICK_QUEUE.clear();
        }

        private static class RecipeWidget extends Widget {
            final IRecipeData recipeData;
            final int index;
            final int level;
            boolean isClicking;

            public RecipeWidget(IRecipeData recipeData, int index, int level) {
                super(0, 0 , 0,0);
                this.recipeData = recipeData;
                this.index = index;
                this.level = level;
                isClicking = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHexString("FF0000") : CustomColor.fromHexString("FFFFFF"));
                ui.drawButton(x, y, width, height, 19, hovered && helperWidget.hovered, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
                drawRecipe(ctx, x, y, width, height, level, recipeData, hovered, ui);
                ui.drawLine(x + width * 0.8f, y + 5, x + width * 0.8f, y + height - 9, ui.getScaleFactorF(), WynnExtrasConfig.INSTANCE.craftingHelperDarkMode ? hovered ? CustomColor.fromHexString("6a6a71") : CustomColor.fromHexString("444448") : hovered ? CustomColor.fromHexString("c5b490") : CustomColor.fromHexString("a68a73"));
                if(level < 100) {
                    ui.drawCenteredText(String.valueOf(Math.max(1, level)), x + width * 0.9f, y + height / 4f + 4);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f);
                    ui.drawCenteredText(String.valueOf(level + 9), x + width * 0.9f, y + 3 * height / 4f - 4);
                } else if(level == 100) {
                    ui.drawCenteredText("100", x + width * 0.9f, y + height / 4f + 4);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f);
                    ui.drawCenteredText("103", x + width * 0.9f, y + 3 * height / 4f - 4);
                } else if(level == 103) {
                    ui.drawCenteredText("103", x + width * 0.9f, y + height / 4f + 4);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f);
                    ui.drawCenteredText("105", x + width * 0.9f, y + 3 * height / 4f - 4);
                }

                if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;

                ProfessionType profession = container.getProfessionType();

                try {
                    int level = Models.Profession.getLevel(profession);

                    if (level > 0 && level < this.level) {
                        ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHSV(0, 0, 0, 0.5f) : CustomColor.fromHSV(0, 0, 0, 0.75f));
                        ui.drawCenteredText("Requires " + profession.getDisplayName(), x + width / 2f, y + height / 2f - 20, hovered ? CustomColor.fromHexString("FF0000").withAlpha(0.2f) : CustomColor.fromHexString("FF0000"));
                        ui.drawCenteredText("level " + this.level + " to craft.", x + width / 2f, y + height / 2f + 20, hovered ? CustomColor.fromHexString("FF0000").withAlpha(0.2f) : CustomColor.fromHexString("FF0000"));
                    }
                } catch (Exception ignored) {}
                checkClick();
            }

            @Override
            protected boolean onClick(int button) {
                if(!helperWidget.hovered) return false;

                if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return false;

                statusMessage = "";

                ProfessionType profession = container.getProfessionType();

                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

                List<Pair<IMaterial, Integer>> materials = recipeData.getMaterials(this.level);

                if(materials.isEmpty() || materials.size() < 2) return true;

                clickMaterial(materials.getFirst(), true);
                clickMaterial(materials.get(1), false);

                return true;
            }

            private void clickMaterial(Pair<IMaterial, Integer> material, boolean isFirstMaterial) {
                int materialAmount = material.getSecond();

                List<Slot> slots = McUtils.containerMenu().slots;
                int available = 0;

                boolean canClick = false;
                for(Slot slot : slots) {
                    try {
                        if(!(slot.inventory instanceof PlayerInventory)) continue;

                        if(slot.getStack().getCustomName().getString().contains(material.getFirst().getName())) {
                            canClick = true;
                            for (int i = 0; i < materialAmount; i++) {
                                CLICK_QUEUE.add(slot.id);
                            }
                            break;
                        }

                        if(available >= materialAmount) break;
                    } catch (Exception ignored) {}
                }

                if(!canClick) {
                    statusMessage = "You don't have the required materials to craft this.";
                }
            }

            private void checkClick() {
                if(McUtils.containerMenu().getSlot(0).getStack().getCustomName() == null ||
                        McUtils.containerMenu().getSlot(9).getStack().getCustomName() == null) return;

                if(McUtils.containerMenu().getSlot(0).getStack().getCustomName().getString() == null ||
                        McUtils.containerMenu().getSlot(9).getStack().getCustomName().getString() == null) return;

                if((!McUtils.containerMenu().getSlot(0).getStack().getCustomName().getString().contains("Material Slot")
                || !McUtils.containerMenu().getSlot(9).getStack().getCustomName().getString().contains("Material Slot")) && !isClicking) return;

                isClicking = true;
                if(!CLICK_QUEUE.isEmpty() && lastClick < Time.now().timestamp() - 1) {
                    Integer next = CLICK_QUEUE.poll();
                    if (next == null) return;

                    ContainerUtils.clickOnSlot(
                            next,
                            McUtils.containerMenu().syncId,
                            0,
                            McUtils.containerMenu().getStacks()
                    );

                    lastClick = Time.now().timestamp();
                } else if(CLICK_QUEUE.isEmpty()) isClicking = false;
            }
        }
    }

    private static class SelectionWidget extends Widget {
        final int index;

        String text;

        int scissorX1, scissorY1, scissorX2, scissorY2;

        public SelectionWidget(int index) {
            super(0, 0 , 0,0);
            this.index = index;
        }

        public void setScissorBounds(int scissorX1, int scissorY1, int scissorX2, int scissorY2) {
            this.scissorX1 = scissorX1;
            this.scissorX2 = scissorX2;
            this.scissorY1 = scissorY1;
            this.scissorY2 = scissorY2;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;
            if(state == null) return;

            ctx.disableScissor();
            ui.drawButton(x, y - 6, width + 2, height + 10, 13, hovered, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
            if(index == state.ordinal() - 1) ui.drawRectBorders(x + 2, y - 2, x + width, y + height - 2, CustomColor.fromHexString("FFFF00"));
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f);
            int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
            int yStart = ((HandledScreenAccessor) screen).getY() + 22;
            int widgetWidth = 600;
            int widgetHeight = ((HandledScreenAccessor) screen).getBackgroundHeight() - 24;
            ctx.enableScissor(
                    scissorX1,
                    scissorY1,
                    scissorX2,
                    scissorY2);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

            switch (index) {
                case 0 -> {
                    if (state != RecipeState.FIRST) state =  RecipeState.FIRST;
                    else state = RecipeState.NONE;
                }
                case 1 -> {
                    if (state != RecipeState.SECOND) state = RecipeState.SECOND;
                    else state = RecipeState.NONE;
                }
                case 2 -> {
                    if (state != RecipeState.THIRD) state = RecipeState.THIRD;
                    else state = RecipeState.NONE;
                }
            }

            helperWidget.recipeData = null;

            if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return true;
            ProfessionType type = container.getProfessionType();

            targetOffset = 0;

            if(type == null) return true;

            Map<RecipeState, Float> offsets = lastOffset.get(type);
            if (offsets == null) return true;

            Float offset = offsets.get(state);
            if (offset == null) return true;

            targetOffset = offset;

            return true;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private static class ScrollBarWidget extends Widget {
        ScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseY = 0;
        public int maxOffset;

        public ScrollBarWidget(int maxOffset) {
            super(0, 0, 0, 0);
            this.scrollBarButtonWidget = new ScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
            this.maxOffset = maxOffset;
        }

        private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
            float relativeY = mouseY * ui.getScaleFactorF() - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.max(-1.15f * ui.getScaleFactorF(), Math.min(relativeY, scrollAreaHeight));

            float scrollPercent = relativeY / scrollAreaHeight;

            targetOffset = scrollPercent * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderBackground(x, y, width, height, 5, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);

            int buttonHeight = 50;
            int scrollAreaHeight = height - buttonHeight;

            if (scrollBarButtonWidget.isHeld) {
                setOffset(mouseY, maxOffset, scrollAreaHeight);
                actualOffset = targetOffset;
            }

            float percent = maxOffset == 0 ? 0 : actualOffset / maxOffset;
            percent = Math.clamp(percent, 0f, 1f);

            int yPos = y + (int) (scrollAreaHeight * percent);

            scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            int buttonHeight = 30;
            int scrollAreaHeight = height - buttonHeight;

            setOffset(currentMouseY, maxOffset, scrollAreaHeight);

            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarButtonWidget.mouseReleased(mx, my, button);
            return true;
        }

        private static class ScrollBarButtonWidget extends Widget {
            public boolean isHeld;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHeld = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 5, hovered || isHeld, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
            }

            @Override
            protected boolean onClick(int button) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                isHeld = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                isHeld = false;
                return true;
            }
        }
    }

    private static class ProfBombWidget extends Widget {
        final BombType type;
        public BombInfo bomb;
        public boolean isActive;
        public String text;

        public ProfBombWidget(BombType type) {
            super(0, 0, 0, 0);
            this.type = type;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            try {
                if(bomb != null) {
                    if (bomb.server().equals(Models.WorldState.getCurrentWorldName())) hovered = false;
                }

                String currentWorld = Models.WorldState.getCurrentWorldName();
                isActive = false;
                bomb = null;

                for (BombInfo bomb : Models.Bomb.getBombBells()) {
                    if (bomb.bomb() == type) {
                        isActive = true;
                        if (bomb.server().equals(currentWorld)) {
                            this.bomb = bomb;
                            break;
                        }
                        if (this.bomb == null || bomb.getRemainingLong() > this.bomb.getRemainingLong()) {
                            this.bomb = bomb;
                        }
                    }
                }

                if (isActive) {
                    String worldColor = bomb.server().equals(currentWorld) ? "§a" : "§f";
                    worldColor += (hovered ? "§n" : "");
                    String bombType = "?";
                    if(type == BombType.PROFESSION_SPEED) bombType = "Speed";
                    if(type == BombType.PROFESSION_XP) bombType = "XP";

                    text = "§6" + (hovered ? "§n" : "") + "Profession " + bombType  + " §7" + (hovered ? "§n" : "") + "on " + worldColor + bomb.server() + " §6" + (hovered ? "§n" : "") + "(" + bomb.getRemainingString() + ")";

                    if (bomb.getRemainingLong() < 30000) {
                        long seconds = Time.now().timestamp() / 1000;

                        String color = (seconds % 2 == 0) ? "§c" : "§4";
                        color += (hovered ? "§n" : "");

                        text = color + "Profession " + bombType + " on "
                                + bomb.server()
                                + " (" + bomb.getRemainingString() + ") (EXPIRING SOON)";
                    }

                    ui.drawCenteredText(text, x + width / 2f, y + height / 2f);
                }
            } catch (Exception ignored) {}
        }

        @Override
        protected boolean onClick(int button) {
            if(bomb.server().equals(Models.WorldState.getCurrentWorldName())) return true;

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if(bomb == null) return true;
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                McUtils.setScreen(null);
                client.player.networkHandler.sendChatCommand("switch " + bomb.server());
            }

            return true;
        }
    }
}
//TODO: cant click on item after switching to account bank
//TODO: bug in character bank when character is not known (when restarting game while in raid and joining again wynntils doesnt know which class you are on)
//TODO: wynnbuilder loader: link at the left where you can paste a wynnbuilder link to load a recipe