package julianh06.wynnextras.features.crafting;

import com.wynntils.core.components.Models;
import com.wynntils.models.containers.containers.CraftingStationContainer;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
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
import net.minecraft.screen.slot.Slot;
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

    Identifier background = Identifier.of("wynnextras", "textures/gui/craftinghelper/bg.png");
    Identifier backgroundBig = Identifier.of("wynnextras", "textures/gui/craftinghelper/bgbig.png");

    public CraftingHelperOverlay() {
        state = RecipeState.NONE;
        helperWidget = null;
        selectionWidget1 = null;
        selectionWidget2 = null;
        selectionWidget3 = null;
        actualOffset = 0;
        targetOffset = 0;

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
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;

        ui.drawBackground();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        if(state == null) state = RecipeState.NONE;

        if(helperWidget == null) {
            helperWidget = new HelperWidget();
            rootWidgets.add(helperWidget);
        }

        int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
        int yStart = ((HandledScreenAccessor) screen).getY() + 22;
        int widgetWidth = 500;
        int widgetHeight = ((HandledScreenAccessor) screen).getBackgroundHeight() - 24;

        ProfessionType type = container.getProfessionType();
        lastState.put(type, state);
        if(helperWidget.recipeData == null) {
            IRecipeData data = getRecipeDataInstance(type);
            helperWidget.setRecipeData(data);
        }
        
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

        ui.drawImage(big ? backgroundBig : background,
                (int) (xStart * ui.getScaleFactor() + 5),
                (int) ((big ? (yStart - 22) : yStart) * ui.getScaleFactor()),
                widgetWidth,
                (int) ((big ? (widgetHeight + 22) : widgetHeight) * ui.getScaleFactor()));

        ctx.enableScissor(
                xStart,
                yStart + (big ? - 15 : 7),
                xStart + widgetWidth,
                yStart + widgetHeight - 7);

        helperWidget.setBounds((int) (xStart * ui.getScaleFactor() + 5), (int) ((yStart + (big ? - 15 : 7)) * ui.getScaleFactor()), widgetWidth, (int) ((widgetHeight + (big ? 12 : - 14)) * ui.getScaleFactor()));
    }
    
    private void setupSelectionWidget(SelectionWidget selectionWidget, ProfessionType type, int i, int maxWidgets, int xStart, int yStart, int widgetWidth) {
        int spacing = 20;

        int totalSpacing = spacing * (maxWidgets - 1);
        int sectionWidth = (widgetWidth - totalSpacing) / maxWidgets;

        int x = (int) ((xStart + 2) * ui.getScaleFactor()) + i * (sectionWidth + spacing);
        int y = (int) ((yStart - 20) * ui.getScaleFactor());


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

    private static void drawRecipe(int x, int y, int width, int height, int level, IRecipeData recipe, boolean hovered, UIUtils ui) {
        if(recipe == null) return;

        List<Pair<IMaterial, Integer>> materials = recipe.getMaterials(level);

        if(materials.isEmpty() || materials.size() < 2) return;

        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("080808"));

        ui.drawImage(materials.getFirst().getFirst().getTexture(), x, y + 10, 60, 60);
        ui.drawText(materials.getFirst().getFirst().getName() + " " + materials.getFirst().getSecond(), x + 70, y + 20);

        ui.drawImage(materials.get(1).getFirst().getTexture(), x, y + 60, 60, 60);
        ui.drawText(materials.get(1).getFirst().getName() + " " + materials.get(1).getSecond(), x + 70, y + 80);
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

        public HelperWidget() {
            super(0, 0, 0, 0);
            recipeData = null;

            if(MinecraftClient.getInstance().currentScreen == null) return;
            ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                    screen,
                    mX,
                    mY,
                    horizontalAmount,
                    verticalAmount
            ) -> {
                long now = System.currentTimeMillis();
                if (now - lastScrollTime < scrollCooldown) {
                    return;
                }
                lastScrollTime = now;

                if (hovered) {
                    if (verticalAmount > 0) {
                        targetOffset -= 104f;
                    } else /*if(canScrollFurther)*/ {
                        targetOffset += 104f;
                    }
                }
            });
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(state == RecipeState.NONE) {
                ui.drawCenteredText("Select the type", x + width / 2f, y + height / 2f - 30, CustomColor.fromHexString("FF0000"), 4);
                ui.drawCenteredText("you want to craft.", x + width / 2f, y + height / 2f + 30, CustomColor.fromHexString("FF0000"), 4);
            } else {
                System.out.println(state);
            }

            if(recipeData == null) return;

            float snapValue = 0.5f;

            int widgetHeight = 120;
            int widgetAmount = 12;

            int maxOffset = (widgetAmount - 2) * widgetHeight - 20;

            if (targetOffset > maxOffset) {
                targetOffset = maxOffset;
                snapValue = 0.75f;
            }
            if (targetOffset <= 0) {
                targetOffset = 0;
                snapValue = 0.75f;
            }

            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if(Math.abs(diff) < snapValue || !SimpleConfig.getInstance(WynnExtrasConfig.class).smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
            ProfessionType type = container.getProfessionType();

            Map<RecipeState, Float> map = lastOffset.get(type) == null ? new HashMap<>() : lastOffset.get(type);
            map.put(state, actualOffset);
            lastOffset.put(type, map);

            if(recipeWidgets.isEmpty()) {
                for (int i = 0; i < widgetAmount; i++) {
                    int level = i * 10;
                    if(i == 10) level = 103;
                    if(i == 11) level = 105;

                    RecipeWidget recipeWidget = new RecipeWidget(recipeData, i, level);

                    recipeWidgets.add(recipeWidget);
                    addChild(recipeWidget);
                }
            }

            for (int i = 0; i < widgetAmount; i++) {
                recipeWidgets.get(i).setBounds(x + 30, (int) (y - actualOffset + 130 * i) + 10, width - 60, widgetHeight);
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
                ui.drawButton(x, y, width, height, 19, hovered && helperWidget.hovered);
                drawRecipe(x, y, width, height, level, recipeData, hovered, ui);

                checkClick();
            }

            @Override
            protected boolean onClick(int button) {
                if(!helperWidget.hovered) return true;

                List<Pair<IMaterial, Integer>> materials = recipeData.getMaterials(level);

                if(materials.isEmpty() || materials.size() < 2) return true;

                clickMaterial(materials.getFirst(), true);
                clickMaterial(materials.get(1), false);

                return true;
            }

            private void clickMaterial(Pair<IMaterial, Integer> material, boolean isFirstMaterial) {
                int materialAmount = material.getSecond();

                List<Slot> slots = McUtils.containerMenu().slots;
                List<Pair<Slot, Integer>> clickableSlots = new ArrayList<>();
                int available = 0;

                for(Slot slot : slots) {
                    try {
                        if(slot.getStack().getCustomName().getString().contains(material.getFirst().getName())) {
                            int remaining = materialAmount - available;
                            available += slot.getStack().getCount();
                            int amount = Math.min(slot.getStack().getCount(), remaining);
                            clickableSlots.add(new Pair<>(slot, amount));
                        }

                        if(available >= materialAmount) break;
                    } catch (Exception ignored) {}
                }

                if(available < materialAmount) {
                    System.out.println("Not enough in inventory");
                    return;
                }

                for (Pair<Slot, Integer> slot : clickableSlots) {
                    for (int i = 0; i < slot.getSecond(); i++) {
                        CLICK_QUEUE.add(slot.getFirst().id);
                    }
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
                if(!CLICK_QUEUE.isEmpty()) {
                    Integer next = CLICK_QUEUE.poll();
                    if (next == null) return;

                    ContainerUtils.clickOnSlot(
                            next,
                            McUtils.containerMenu().syncId,
                            0,
                            McUtils.containerMenu().getStacks()
                    );
                } else isClicking = false;
            }
        }
    }

    private static class SelectionWidget extends Widget {
        final int index;

        String text;

        public SelectionWidget(int index) {
            super(0, 0 , 0,0);
            this.index = index;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(!(McUtils.screen() instanceof HandledScreen<?> screen)) return;
            if(state == null) return;

            ctx.disableScissor();
            ui.drawButton(x, y - 6, width + 2, height + 10, 13, hovered || index == state.ordinal() - 1);
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f);
            int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
            int yStart = ((HandledScreenAccessor) screen).getY() + 22;
            int widgetWidth = 500;
            int widgetHeight = ((HandledScreenAccessor) screen).getBackgroundHeight() - 24;
            ctx.enableScissor(
                    xStart,
                    yStart + 7,
                    xStart + widgetWidth,
                    yStart + widgetHeight - 7);
        }

        @Override
        protected boolean onClick(int button) {
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

            targetOffset = 0;

            if(!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return true;
            ProfessionType type = container.getProfessionType();

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
}
//TODO: fix paper textures
//TODO: remember which recipe was clicked on last and scroll to that and highlight that when reopening the station
//TODO: sky paper + starfish oil recipe is shown twice
//TODO: add toggle for guild map estimate thing
//TODO: save last scroll and last selected for each prof type
//TODO: remove tree timestamps and cooldown for timestamps (maybe make it switch for the notg minibosses then)