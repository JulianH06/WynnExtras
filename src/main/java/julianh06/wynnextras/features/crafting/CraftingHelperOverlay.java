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
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.IMaterial;
import julianh06.wynnextras.features.crafting.data.IRecipeData;
import julianh06.wynnextras.features.crafting.data.VcitCompat;
import julianh06.wynnextras.features.crafting.data.recipes.AlchemismRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.CookingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.RecipeLoader;
import julianh06.wynnextras.features.crafting.data.recipes.ScribingRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.ChestplateRecipes;
import julianh06.wynnextras.features.crafting.data.recipes.armouring.HelmetRecipes;
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
import julianh06.wynnextras.features.crafting.wynnbuilder.DecodedCraft;
import julianh06.wynnextras.features.crafting.wynnbuilder.WynnBuilderDecoder;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
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

public class CraftingHelperOverlay extends WEMenuExtension {
    private static final boolean registeredScroll = false;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 50; // in ms
    public static float targetOffset = 0;
    public static float actualOffset = 0;

    static HelperWidget helperWidget;

    SelectionWidget selectionWidget1;
    SelectionWidget selectionWidget2;
    SelectionWidget selectionWidget3;


    private static final Queue<Integer> WB_CLICK_QUEUE = new ArrayDeque<>();
    private static long wbLastClick = 0;
    private static boolean wbClicking = false;
    private static String wbStatusMessage = "";
    private static int wbTotalClicks = 0;
    private static int wbClicksDone = 0;
    private static boolean wbIsReuse = false; // true = filling from Reuse Last, false = from Clipboard
    private static long wbFinishedTime = 0; // timestamp when queue emptied, used to delay completion check
    private static boolean lastResultSlotsEmpty = true; // tracks output slots to detect craft completion
    private static final int[] RESULT_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26};
    private static final int[] INGREDIENT_SLOTS = {2, 3, 11, 12, 20, 21};

    // "Reuse last" - stores item names from the last craft (saved when items are queued for placement)
    private static final List<String> lastMaterialNames = new ArrayList<>();  // material names (from inventory)
    private static final List<Integer> lastMaterialCounts = new ArrayList<>(); // click count per material
    private static final List<String> lastIngredientNames = new ArrayList<>(); // ingredient names (from inventory)

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
        wbStatusMessage = "";
        WB_CLICK_QUEUE.clear();
        wbClicking = false;

        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        ProfessionType type = container.getProfessionType();

        if (type == null) return;

        if (lastState.isEmpty()) return;

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
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
        if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return;

        if (state == null) state = RecipeState.NONE;

        int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
        int yStart = ((HandledScreenAccessor) screen).getY() + 23;
        int widgetWidth = 200;
        int widgetHeight = ((HandledScreenAccessor) screen).getBackgroundHeight() - 24;

        if (profSpeedBombWidget == null) profSpeedBombWidget = new ProfBombWidget(BombType.PROFESSION_SPEED);
        if (profXpBombWidget == null) profXpBombWidget = new ProfBombWidget(BombType.PROFESSION_XP);

        int speedWidth = MinecraftClient.getInstance().textRenderer.getWidth(profSpeedBombWidget.text);
        int xpWidth = MinecraftClient.getInstance().textRenderer.getWidth(profXpBombWidget.text);
        profSpeedBombWidget.setBounds(screen.width / 2 - speedWidth / 2, ((HandledScreenAccessor) screen).getY() - 43, speedWidth, 10);
        profXpBombWidget.setBounds(screen.width / 2 - xpWidth / 2, ((HandledScreenAccessor) screen).getY() - 57, xpWidth, 10);

        profSpeedBombWidget.draw(ctx, mouseX, mouseY, delta, ui);
        profXpBombWidget.draw(ctx, mouseX, mouseY, delta, ui);

        boolean dontShowWorldText = profSpeedBombWidget.bomb != null && profSpeedBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName());

        if (profXpBombWidget.bomb != null && profXpBombWidget.bomb.server().equals(Models.WorldState.getCurrentWorldName()))
            dontShowWorldText = true;

        if ((profXpBombWidget.isActive || profSpeedBombWidget.isActive) && !dontShowWorldText) {
            int currentWorldTextYOffset = profXpBombWidget.isActive ? 67 : 53;
            ui.drawCenteredText("There are no active profession bombs on your world. Click below to switch worlds.", screen.width / 2f, ((HandledScreenAccessor) screen).getY() - currentWorldTextYOffset, CustomColor.fromHexString("FF0000"), 1f);
        }

        if (!profXpBombWidget.isActive && !profSpeedBombWidget.isActive) {
            ui.drawCenteredText("There are no active profession bombs.", screen.width / 2f, ((HandledScreenAccessor) screen).getY() - 40, CustomColor.fromHexString("FF0000"), 1f);
        }

        ProfessionType type = container.getProfessionType();
        lastState.put(type, state);

        if (selectionWidget1 == null) {
            selectionWidget1 = new SelectionWidget(0);
            rootWidgets.add(selectionWidget1);
        }

        if (selectionWidget2 == null) {
            selectionWidget2 = new SelectionWidget(1);
            rootWidgets.add(selectionWidget2);
        }

        if (selectionWidget3 == null) {
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

        if (WynnExtrasConfig.INSTANCE.craftingHelperDarkMode) {
            ui.drawNineSlice(xStart + 1.7f, yStart - (big ? 22 : 0), widgetWidth,
                    widgetHeight + (big ? 22 : 0), 11, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
        } else {
            ui.drawNineSlice(xStart + 1.7f, yStart - (big ? 22 : 0), widgetWidth,
                    widgetHeight + (big ? 22 : 0), 11, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
        }

        int step = 47;
        int recipeWidgetAmount = 14;

        int contentHeight = recipeWidgetAmount * step;

        int visibleHeight = helperWidget == null ? 0 : helperWidget.getHeight();

        int maxOffset = Math.max(0, contentHeight - visibleHeight);

        if (helperWidget == null) {
            helperWidget = new HelperWidget(maxOffset);
            rootWidgets.add(helperWidget);
        }

        if (helperWidget.recipeData == null) {
            IRecipeData data = getRecipeDataInstance(type);
            if (
                    type == ProfessionType.SCRIBING ||
                            type == ProfessionType.ALCHEMISM ||
                            type == ProfessionType.COOKING ||
                            state != RecipeState.NONE
            ) helperWidget.setRecipeData(data);
        }

        if (scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget(maxOffset);
        }

        helperWidget.maxOffset = maxOffset;
        scrollBarWidget.maxOffset = maxOffset;

        scrollBarWidget.setBounds(xStart + 5 + widgetWidth, ((HandledScreenAccessor) screen).getY() + (big ? 3 : 23), 10, ((HandledScreenAccessor) screen).getBackgroundHeight() - (big ? 4 : 25));
        scrollBarWidget.draw(ctx, mouseX, mouseY, delta, ui);

        int scissorX1 = xStart;
        int scissorY1 = yStart + (big ? -16 : 7);
        int scissorX2 = xStart + widgetWidth;
        int scissorY2 = yStart + widgetHeight - 7;

        // Buttons (left side of crafting station, right-aligned near GUI)
        int leftX = ((HandledScreenAccessor) screen).getX();
        int screenY = ((HandledScreenAccessor) screen).getY();

        int wbBtnW = (leftX - 10) / 2;
        int wbBtnH = 7;
        int wbBtnX = leftX - wbBtnW - 2;
        int wbBtnY = screenY + 2;

        int rBtnX = wbBtnX;
        int rBtnY = wbBtnY;
        int rBtnW = wbBtnW;
        int rBtnH = wbBtnH;

        boolean clipboardFilling = wbClicking && !wbIsReuse;
        boolean btnHovered = !wbClicking && mouseX >= wbBtnX && mouseX <= wbBtnX + wbBtnW && mouseY >= wbBtnY && mouseY <= wbBtnY + wbBtnH;

        if (clipboardFilling) {
            ui.drawButton(rBtnX, rBtnY, rBtnW, rBtnH, 3, false, true);
            int progress = wbTotalClicks > 0 ? (int) ((float) wbClicksDone / wbTotalClicks * rBtnW) : 0;
            ui.drawRect(rBtnX, rBtnY, progress, rBtnH, CustomColor.fromHexString("2a7a2a").withAlpha(0.5f));
            ui.drawCenteredText("Filling... " + wbClicksDone + "/" + wbTotalClicks, rBtnX + rBtnW / 2f, rBtnY + rBtnH / 2f, 1f);
        } else {
            ui.drawButton(rBtnX, rBtnY, rBtnW, rBtnH, 3, btnHovered, true);
            ui.drawCenteredText("Load from Clipboard", rBtnX + rBtnW / 2f, rBtnY + rBtnH / 2f, 1f);
        }

        // Reuse Last button (below Load from Clipboard)
        int reuseBtnY = wbBtnY + wbBtnH + 2;
        int rReuseBtnY = reuseBtnY;
        boolean hasLastCraft = !lastMaterialNames.isEmpty() || !lastIngredientNames.isEmpty();
        boolean reuseFilling = wbClicking && wbIsReuse;
        boolean reuseBtnHovered = !wbClicking && hasLastCraft && mouseX >= wbBtnX && mouseX <= wbBtnX + wbBtnW && mouseY >= reuseBtnY && mouseY <= reuseBtnY + wbBtnH;

        if (reuseFilling) {
            ui.drawButton(rBtnX, rReuseBtnY, rBtnW, rBtnH, 3, false, true);
            int progress = wbTotalClicks > 0 ? (int) ((float) wbClicksDone / wbTotalClicks * rBtnW) : 0;
            ui.drawRect(rBtnX, rReuseBtnY, progress, rBtnH, CustomColor.fromHexString("2a7a2a").withAlpha(0.5f));
            ui.drawCenteredText("Filling... " + wbClicksDone + "/" + wbTotalClicks, rBtnX + rBtnW / 2f, rReuseBtnY + rBtnH / 2f, 1f);
        } else {
            ui.drawButton(rBtnX, rReuseBtnY, rBtnW, rBtnH, 3, reuseBtnHovered, true);
            ui.drawCenteredText("Reuse Last", rBtnX + rBtnW / 2f, rReuseBtnY + rBtnH / 2f, hasLastCraft ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("666666"), 1f);
        }

        // Auto Start toggle (below Reuse Last)
        int autoStartBtnY = reuseBtnY + wbBtnH + 2;
        int rAutoStartBtnY = autoStartBtnY;
        boolean autoStartHovered = mouseX >= wbBtnX && mouseX <= wbBtnX + wbBtnW && mouseY >= autoStartBtnY && mouseY <= autoStartBtnY + wbBtnH;
        boolean autoStartOn = WynnExtrasConfig.INSTANCE.craftingAutoStart;
        ui.drawButton(rBtnX, rAutoStartBtnY, rBtnW, rBtnH, 3, autoStartHovered, true);
        String autoStartLabel = "Auto Start: " + (autoStartOn ? "§aON" : "§cOFF");
        ui.drawCenteredText(autoStartLabel, rBtnX + rBtnW / 2f, rAutoStartBtnY + rBtnH / 2f, 1f);

        // Status message below buttons
        if (!wbStatusMessage.isEmpty()) {
            CustomColor statusColor = wbStatusMessage.startsWith("Missing") || wbStatusMessage.startsWith("Wrong") || wbStatusMessage.startsWith("Invalid") || wbStatusMessage.startsWith("Unknown") || wbStatusMessage.startsWith("Paste") || wbStatusMessage.startsWith("Not")
                    ? CustomColor.fromHexString("FF4444") : CustomColor.fromHexString("44FF44");
            int statusRY = rAutoStartBtnY + rBtnH + 3;
            ui.drawText(wbStatusMessage, rBtnX, statusRY, statusColor, HorizontalAlignment.LEFT, VerticalAlignment.TOP, 2f);
        }

        // Process WynnBuilder click queue
        processWynnBuilderClicks();

        // Auto-capture craft: when any result slot gets an item, save the current materials + ingredients
        try {
            boolean hasOutput = false;
            for (int slot : RESULT_SLOTS) {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.isEmpty() && !name.contains("Crafted Item Slot")) {
                        hasOutput = true;
                        break;
                    }
                }
            }
            if (lastResultSlotsEmpty && hasOutput) {
                System.out.println("[REUSE-DEBUG] Result slots got items! Capturing...");
                captureCurrentMaterials();
                captureCurrentIngredients();
            }
            lastResultSlotsEmpty = !hasOutput;
        } catch (Exception ignored) {}

        ui.drawCenteredText(statusMessage, xStart, ((HandledScreenAccessor) screen).getY() + ((HandledScreenAccessor) screen).getBackgroundHeight() + 10, CustomColor.fromHexString("FF0000"), 1f);

        ctx.enableScissor(
                scissorX1,
                scissorY1,
                scissorX2,
                scissorY2);

        selectionWidget1.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget2.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);
        selectionWidget3.setScissorBounds(scissorX1, scissorY1, scissorX2, scissorY2);

        helperWidget.setBounds(xStart + 2, yStart + (big ? -5 : 7), widgetWidth, widgetHeight + (big ? 4 : -14));
    }

    private void setupSelectionWidget(SelectionWidget selectionWidget, ProfessionType type, int i, int maxWidgets, int xStart, int yStart, int widgetWidth) {
        int spacing = 20;

        int totalSpacing = spacing * (maxWidgets - 1);
        int sectionWidth = (widgetWidth - totalSpacing) / maxWidgets;

        int x = xStart + 2 + i * (sectionWidth + spacing);
        int y = yStart - 20;

        selectionWidget.setBounds(x, y, sectionWidth, 18);

        selectionWidget.setText(getSelectorText(type, i));
    }

    private String getSelectorText(ProfessionType type, int i) {
        return switch (type) {
            case ARMOURING -> switch (i) {
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
        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer)) return;
        if (!(McUtils.screen() instanceof HandledScreen<?>)) return;

        try {
            ctx.disableScissor();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if(scrollBarWidget != null) scrollBarWidget.mouseClicked(x, y, button);
        if(profSpeedBombWidget != null) profSpeedBombWidget.mouseClicked(x, y, button);
        if(profXpBombWidget != null) profXpBombWidget.mouseClicked(x, y, button);

        // Check button clicks (screen coordinates)
        if (McUtils.screen() instanceof HandledScreen<?> screen) {
            int leftX = ((HandledScreenAccessor) screen).getX();
            int screenY = ((HandledScreenAccessor) screen).getY();
            int wbBtnW = (leftX - 10) / 2;
            int wbBtnH = 7;
            int wbBtnX = leftX - wbBtnW - 2;
            int wbBtnY = screenY + 2;

            if (x >= wbBtnX && x <= wbBtnX + wbBtnW && y >= wbBtnY && y <= wbBtnY + wbBtnH) {
                if (!wbClicking) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                    loadFromWynnBuilder(clipboard);
                }
                return true;
            }

            // Reuse Last button click
            int reuseBtnY = wbBtnY + wbBtnH + 2;
            boolean hasLastCraft = !lastMaterialNames.isEmpty() || !lastIngredientNames.isEmpty();
            if (hasLastCraft && x >= wbBtnX && x <= wbBtnX + wbBtnW && y >= reuseBtnY && y <= reuseBtnY + wbBtnH) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                reuseLast();
                return true;
            }

            // Auto Start toggle click
            int autoStartBtnY = reuseBtnY + wbBtnH + 2;
            if (x >= wbBtnX && x <= wbBtnX + wbBtnW && y >= autoStartBtnY && y <= autoStartBtnY + wbBtnH) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                WynnExtrasConfig.INSTANCE.craftingAutoStart = !WynnExtrasConfig.INSTANCE.craftingAutoStart;
                WynnExtrasConfig.save();
                return true;
            }
        }

        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (scrollBarWidget != null) scrollBarWidget.mouseReleased(x, y, button);
        return super.mouseReleased(x, y, button);
    }


    private void loadFromWynnBuilder(String link) {

        // Block re-clicking while already processing
        if (wbClicking) {
            return;
        }

        if (link == null || link.isBlank()) {
            wbStatusMessage = "Clipboard is empty.";
            return;
        }

        if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) {
            wbStatusMessage = "Not at a crafting station.";
            return;
        }

        DecodedCraft craft = WynnBuilderDecoder.decode(link);
        if (craft == null) {
            wbStatusMessage = "Invalid WynnBuilder link.";
            return;
        }

        RecipeLoader.RecipeData recipeData = RecipeLoader.getRecipeById(craft.recipeId());
        if (recipeData == null) {
            wbStatusMessage = "Unknown recipe ID: " + craft.recipeId();
            return;
        }

        // Verify correct crafting station
        ProfessionType stationProf = container.getProfessionType();
        if (stationProf != recipeData.skill()) {
            wbStatusMessage = "Wrong station! Need " + recipeData.skill().getDisplayName() + ", at " + stationProf.getDisplayName();
            return;
        }

        // Get material data for this recipe
        IRecipeData materialData = getRecipeDataForType(recipeData.type());
        if (materialData == null) {
            wbStatusMessage = "Could not find material data for " + recipeData.type();
            return;
        }

        List<Pair<IMaterial, Integer>> materials = materialData.getMaterials(recipeData.lvl().x);
        if (materials == null || materials.size() < 2) {
            wbStatusMessage = "Could not determine materials for this recipe.";
            return;
        }

        // Clear queue
        WB_CLICK_QUEUE.clear();

        // Reset all crafting slots first (not counted in progress)
        try {
            for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                if (stack != null && !stack.isEmpty()) {
                    String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                    if (!name.contains("Material Slot") && !name.contains("Ingredient Slot") && !name.isEmpty()) {
                        ContainerUtils.clickOnSlot(slot, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                    }
                }
            }
        } catch (Exception ignored) {}

        // Queue material clicks - match by base material name (ignore stars/tiers)
        for (int m = 0; m < 2; m++) {
            Pair<IMaterial, Integer> mat = materials.get(m);
            int amount = mat.getSecond();
            String matName = mat.getFirst().getName();
            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    String slotName = slot.getStack().getCustomName().getString();
                    if (slotName.contains(matName)) {
                        for (int i = 0; i < amount; i++) {
                            WB_CLICK_QUEUE.add(slot.id);
                        }
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Note: ingredient auto-fill from clipboard is not available (requires ingredient loader)
        // Ingredients can still be placed manually or via "Reuse Last"

        wbTotalClicks = WB_CLICK_QUEUE.size();
        wbClicksDone = 0;

        // Save for "Reuse Last"
        List<String> matNamesForSave = new ArrayList<>();
        List<Integer> matCountsForSave = new ArrayList<>();
        for (int m = 0; m < 2; m++) {
            matNamesForSave.add(materials.get(m).getFirst().getName());
            matCountsForSave.add(materials.get(m).getSecond());
        }
        saveLastCraft(matNamesForSave, matCountsForSave, new ArrayList<>());

        wbStatusMessage = recipeData.type().getDisplayName() + " " + recipeData.lvl().x + "-" + recipeData.lvl().y;

        wbIsReuse = false;
        wbFinishedTime = 0;
        wbClicking = true;
    }

    private static void processWynnBuilderClicks() {
        if (!wbClicking) return;

        if (WB_CLICK_QUEUE.isEmpty()) {
            // Wait 500ms after last click before checking completion
            if (wbFinishedTime == 0) {
                wbFinishedTime = System.currentTimeMillis();
                return;
            }
            if (System.currentTimeMillis() - wbFinishedTime < 500) return;

            wbClicking = false;
            wbFinishedTime = 0;
            if (wbStatusMessage.startsWith("Done!") || wbStatusMessage.startsWith("Crafting!")) return;

            // Check if slot 13 still shows "Incomplete Recipe"
            try {
                ItemStack craftSlot = McUtils.containerMenu().getSlot(13).getStack();
                String craftName = craftSlot.getCustomName() != null ? craftSlot.getCustomName().getString() : "";
                if (craftName.contains("Incomplete")) {
                    wbStatusMessage = "Missing materials or ingredients!";
                    return;
                }
            } catch (Exception ignored) {}

            wbStatusMessage = "Done!";

            // Auto Start: shift-click the craft button (slot 13) after filling
            if (WynnExtrasConfig.INSTANCE.craftingAutoStart) {
                ContainerUtils.shiftClickOnSlot(13, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
                wbStatusMessage = "Crafting!";
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now - wbLastClick < 10) return; // 10ms between clicks

        Integer next = WB_CLICK_QUEUE.poll();
        if (next == null) return;

        ContainerUtils.clickOnSlot(next, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
        wbLastClick = now;
        wbClicksDone++;
    }

    private static boolean isProfSpeedBombActive() {
        try {
            String currentWorld = Models.WorldState.getCurrentWorldName();
            for (BombInfo bomb : Models.Bomb.getBombBells()) {
                if (bomb.bomb() == BombType.PROFESSION_SPEED && bomb.isActive() && bomb.server().equals(currentWorld)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static String formatMissing(List<String> missingItems) {
        if (missingItems.isEmpty()) return "";
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String item : missingItems) {
            counts.merge(item, 1, Integer::sum);
        }
        StringBuilder sb = new StringBuilder("Missing: ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!first) sb.append(", ");
            if (entry.getValue() > 1) sb.append(entry.getValue()).append("x ");
            sb.append(entry.getKey());
            first = false;
        }
        return sb.toString();
    }

    private static void saveLastCraft(List<String> matNames, List<Integer> matCounts, List<String> ingNames) {
        lastMaterialNames.clear();
        lastMaterialNames.addAll(matNames);
        lastMaterialCounts.clear();
        lastMaterialCounts.addAll(matCounts);
        // Only overwrite ingredients if new list has actual entries
        if (ingNames != null && ingNames.stream().anyMatch(Objects::nonNull)) {
            lastIngredientNames.clear();
            lastIngredientNames.addAll(ingNames);
        }
        System.out.println("[REUSE-DEBUG] SAVED mats=" + lastMaterialNames + " counts=" + lastMaterialCounts + " ings=" + lastIngredientNames);
    }

    /**
     * Read ingredient names currently in the crafting slots and save them.
     */
    private static void captureCurrentIngredients() {
        if (McUtils.containerMenu() == null) return;
        List<String> ingNames = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : INGREDIENT_SLOTS) {
            try {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                if (!name.isEmpty() && !name.contains("Ingredient Slot")) {
                    ingNames.add(name);
                    hasAny = true;
                } else {
                    ingNames.add(null);
                }
            } catch (Exception e) {
                ingNames.add(null);
            }
        }
        if (hasAny) {
            lastIngredientNames.clear();
            lastIngredientNames.addAll(ingNames);
        }
    }

    /**
     * Read material names currently in the crafting slots and save them.
     */
    private static void captureCurrentMaterials() {
        if (McUtils.containerMenu() == null) return;
        List<String> matNames = new ArrayList<>();
        List<Integer> matCounts = new ArrayList<>();
        boolean hasAny = false;
        for (int slot : new int[]{0, 9}) {
            try {
                ItemStack stack = McUtils.containerMenu().getSlot(slot).getStack();
                String name = stack.getCustomName() != null ? stack.getCustomName().getString() : "";
                if (!name.isEmpty() && !name.contains("Material Slot")) {
                    matNames.add(name);
                    matCounts.add(stack.getCount());
                    hasAny = true;
                } else {
                    matNames.add(null);
                    matCounts.add(0);
                }
            } catch (Exception e) {
                matNames.add(null);
                matCounts.add(0);
            }
        }
        if (hasAny) {
            lastMaterialNames.clear();
            lastMaterialNames.addAll(matNames);
            lastMaterialCounts.clear();
            lastMaterialCounts.addAll(matCounts);
        }
    }

    private void reuseLast() {
        if (McUtils.containerMenu() == null) return;

        if (lastMaterialNames.isEmpty() && lastIngredientNames.isEmpty()) {
            wbStatusMessage = "No previous craft to reuse.";
            return;
        }

        // Clear existing slots and queue
        WB_CLICK_QUEUE.clear();
        wbClicking = false;

        // Reset all crafting slots immediately
        for (int slot : new int[]{0, 9, 2, 3, 11, 12, 20, 21}) {
            ContainerUtils.clickOnSlot(slot, McUtils.containerMenu().syncId, 0, McUtils.containerMenu().getStacks());
        }

        // Queue material clicks
        for (int m = 0; m < Math.min(2, lastMaterialNames.size()); m++) {
            String matName = lastMaterialNames.get(m);
            int amount = lastMaterialCounts.get(m);
            if (matName == null || amount <= 0) continue;

            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    if (slot.getStack().getCustomName().getString().contains(matName)) {
                        for (int i = 0; i < amount; i++) {
                            WB_CLICK_QUEUE.add(slot.id);
                        }
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Queue ingredient clicks
        System.out.println("[REUSE-DEBUG] Queuing ingredients: " + lastIngredientNames);
        for (int i = 0; i < Math.min(6, lastIngredientNames.size()); i++) {
            String ingName = lastIngredientNames.get(i);
            if (ingName == null) continue;

            boolean found = false;
            for (Slot slot : McUtils.containerMenu().slots) {
                try {
                    if (!(slot.inventory instanceof PlayerInventory)) continue;
                    String slotName = slot.getStack().getCustomName().getString();
                    if (slotName.contains(ingName)) {
                        WB_CLICK_QUEUE.add(slot.id);
                        System.out.println("[REUSE-DEBUG] Found ingredient '" + ingName + "' in slot " + slot.id);
                        found = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (!found) {
                System.out.println("[REUSE-DEBUG] NOT FOUND ingredient '" + ingName + "' in inventory");
            }
        }

        wbTotalClicks = WB_CLICK_QUEUE.size();
        wbClicksDone = 0;
        wbStatusMessage = "Reusing last craft...";

        wbIsReuse = true;
        wbFinishedTime = 0;
        wbClicking = true;
    }

    private static IRecipeData getRecipeDataForType(CraftableType type) {
        return switch (type) {
            case HELMET -> HelmetRecipes.INSTANCE;
            case CHESTPLATE -> ChestplateRecipes.INSTANCE;
            case LEGGINGS -> LeggingsRecipes.INSTANCE;
            case BOOTS -> BootsRecipes.INSTANCE;
            case SPEAR -> SpearRecipes.INSTANCE;
            case DAGGER -> DaggerRecipes.INSTANCE;
            case BOW -> BowRecipes.INSTANCE;
            case WAND -> WandRecipes.INSTANCE;
            case RELIK -> RelikRecipes.INSTANCE;
            case RING -> RingRecipes.INSTANCE;
            case BRACELET -> BraceletRecipes.INSTANCE;
            case NECKLACE -> NecklaceRecipes.INSTANCE;
            case POTION -> AlchemismRecipes.INSTANCE;
            case SCROLL -> ScribingRecipes.INSTANCE;
            case FOOD -> CookingRecipes.INSTANCE;
        };
    }

    private static IRecipeData getRecipeDataInstance(ProfessionType type) {
        if (state == null) return null;

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
            case null, default -> null;
        };
    }

    private static void drawRecipe(DrawContext ctx, int x, int y, int width, int height, int level,
                                   IRecipeData recipe, boolean hovered, UIUtils ui) {
        if (recipe == null) return;

        List<Pair<IMaterial, Integer>> materials = recipe.getMaterials(level);

        if (materials.isEmpty() || materials.size() < 2) return;

        //ui.drawRect(x, y, width, height, CustomColor.fromHexString("080808"));

        drawMaterialIcon(ctx, ui, materials.getFirst().getFirst(), x + 3, y + 2, 20);
        ui.drawText(materials.getFirst().getFirst().getName() + " " + materials.getFirst().getSecond(), x + 27, y + height / 4f + 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 1f);

        drawMaterialIcon(ctx, ui, materials.get(1).getFirst(), x + 3, y + 20, 20);
        ui.drawText(materials.get(1).getFirst().getName() + " " + materials.get(1).getSecond(), x + 27, y + 3 * height / 4f - 1, CustomColor.fromHexString("FFFFFF"), HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 1f);
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

            if (MinecraftClient.getInstance().currentScreen == null) return;
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
                        targetOffset -= 47f;
                    } else /*if(canScrollFurther)*/ {
                        targetOffset += 47f;
                    }
                }
                return true;
            });
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;
            ProfessionType type = container.getProfessionType();

            if (state == RecipeState.NONE && type != ProfessionType.ALCHEMISM && type != ProfessionType.COOKING && type != ProfessionType.SCRIBING) {
                ui.drawCenteredText("Select the type", x + width / 2f, y + height / 2f - 10, CustomColor.fromHexString("FF0000"), 2);
                ui.drawCenteredText("you want to craft.", x + width / 2f, y + height / 2f + 10, CustomColor.fromHexString("FF0000"), 2);
            }

            if (recipeData == null) return;

            float snapValue = 0.5f;

            int widgetHeight = 43;
            int widgetAmount = 14;

            boolean big = type == ProfessionType.ALCHEMISM || type == ProfessionType.COOKING || type == ProfessionType.SCRIBING;
            targetOffset = ui == null ? 0 : Math.clamp(targetOffset, 0, maxOffset);

            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if (Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle)
                actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            Map<RecipeState, Float> map = lastOffset.get(type) == null ? new HashMap<>() : lastOffset.get(type);
            map.put(state, actualOffset);
            lastOffset.put(type, map);

            if (recipeWidgets.isEmpty()) {
                for (int i = 0; i < widgetAmount; i++) {
                    // Reverse order: highest level first (103, 100, 90, 80, ... 10, 0)
                    int level;
                    if (i == 0) level = 103;
                    else level = (widgetAmount - 1 - i) * 10;

                    RecipeWidget recipeWidget = new RecipeWidget(recipeData, i, level);

                    recipeWidgets.add(recipeWidget);
                    addChild(recipeWidget);
                }
            }

            for (int i = 0; i < widgetAmount; i++) {
                int baseY = y + 3 + 47 * i;
                int drawY = baseY - (int) actualOffset;

                recipeWidgets.get(i).setBounds(
                        x + 10,
                        drawY,
                        width - 20,
                        widgetHeight
                );
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (contains((int) mx, (int) my) && recipeData != null && !recipeWidgets.isEmpty()) {
                resetMaterialSlots();
            }

            return super.mouseClicked(mx, my, button);
        }

        public void setRecipeData(IRecipeData recipeData) {
            boolean hadRecipe = this.recipeData != null;
            this.recipeData = recipeData;
            recipeWidgets.clear();
            children.clear();
            if (hadRecipe) resetMaterialSlots();
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
                super(0, 0, 0, 0);
                this.recipeData = recipeData;
                this.index = index;
                this.level = level;
                isClicking = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHexString("FF0000") : CustomColor.fromHexString("FFFFFF"));
                ui.drawButton(x, y, width, height, 6, hovered && helperWidget.hovered, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
                drawRecipe(ctx, x, y, width, height, level, recipeData, hovered, ui);
                ui.drawLine(x + width * 0.8f, y + 2, x + width * 0.8f, y + height - 3, 1f, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode ? hovered ? CustomColor.fromHexString("6a6a71") : CustomColor.fromHexString("444448") : hovered ? CustomColor.fromHexString("c5b490") : CustomColor.fromHexString("a68a73"));
                if (level < 100) {
                    ui.drawCenteredText(String.valueOf(Math.max(1, level)), x + width * 0.9f, y + height / 4f + 1, 1f);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f, 1f);
                    ui.drawCenteredText(String.valueOf(level + 9), x + width * 0.9f, y + 3 * height / 4f - 1, 1f);
                } else {
                    ui.drawCenteredText(String.valueOf(level), x + width * 0.9f, y + height / 4f + 1, 1f);
                    ui.drawCenteredText("-", x + width * 0.9f, y + 2 * height / 4f, 1f);
                    ui.drawCenteredText(String.valueOf(level + 4), x + width * 0.9f, y + 3 * height / 4f - 1, 1f);
                }

                if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return;

                ProfessionType profession = container.getProfessionType();

                checkClick();
            }

            @Override
            protected boolean onClick(int button) {
                if (!helperWidget.hovered) return false;

                if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container))
                    return false;

                statusMessage = "";

                ProfessionType profession = container.getProfessionType();

                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());

                List<Pair<IMaterial, Integer>> materials = recipeData.getMaterials(this.level);

                if (materials.isEmpty() || materials.size() < 2) return true;

                clickMaterial(materials.getFirst(), true);
                clickMaterial(materials.get(1), false);

                // Save for "Reuse Last" (recipe clicks only place materials, no ingredients)
                List<String> matNames = new ArrayList<>();
                List<Integer> matCounts = new ArrayList<>();
                for (Pair<IMaterial, Integer> mat : materials) {
                    matNames.add(mat.getFirst().getName());
                    matCounts.add(mat.getSecond());
                }
                saveLastCraft(matNames, matCounts, new ArrayList<>());

                return true;
            }

            private void clickMaterial(Pair<IMaterial, Integer> material, boolean isFirstMaterial) {
                int materialAmount = material.getSecond();

                List<Slot> slots = McUtils.containerMenu().slots;
                int available = 0;

                boolean canClick = false;
                for (Slot slot : slots) {
                    try {
                        if (!(slot.inventory instanceof PlayerInventory)) continue;

                        if (slot.getStack().getCustomName().getString().contains(material.getFirst().getName())) {
                            canClick = true;
                            for (int i = 0; i < materialAmount; i++) {
                                CLICK_QUEUE.add(slot.id);
                            }
                            break;
                        }

                        if (available >= materialAmount) break;
                    } catch (Exception ignored) {
                    }
                }

                if (!canClick) {
                    statusMessage = "You don't have the required materials to craft this.";
                }
            }

            private void checkClick() {
                if (McUtils.containerMenu().getSlot(0).getStack().getCustomName() == null ||
                        McUtils.containerMenu().getSlot(9).getStack().getCustomName() == null) return;

                if (McUtils.containerMenu().getSlot(0).getStack().getCustomName().getString() == null ||
                        McUtils.containerMenu().getSlot(9).getStack().getCustomName().getString() == null) return;

                if ((!McUtils.containerMenu().getSlot(0).getStack().getCustomName().getString().contains("Material Slot")
                        || !McUtils.containerMenu().getSlot(9).getStack().getCustomName().getString().contains("Material Slot")) && !isClicking)
                    return;

                isClicking = true;
                if (!CLICK_QUEUE.isEmpty() && lastClick < Time.now().timestamp() - 1) {
                    Integer next = CLICK_QUEUE.poll();
                    if (next == null) return;

                    ContainerUtils.clickOnSlot(
                            next,
                            McUtils.containerMenu().syncId,
                            0,
                            McUtils.containerMenu().getStacks()
                    );

                    lastClick = Time.now().timestamp();
                } else if (CLICK_QUEUE.isEmpty()) isClicking = false;
            }
        }
    }

    private static class SelectionWidget extends Widget {
        final int index;

        String text;

        int scissorX1, scissorY1, scissorX2, scissorY2;

        public SelectionWidget(int index) {
            super(0, 0, 0, 0);
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
            if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return;
            if (state == null) return;

            ctx.disableScissor();
            ui.drawButton(x, y - 2, width + 2, height + 3, 4, hovered, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
            if (index == state.ordinal() - 1)
                ui.drawRectBorders(x + 2, y - 1, x + width, y + height - 1, CustomColor.fromHexString("FFFF00"));
            ui.drawCenteredText(text, x + width / 2f, y + height / 2f, 1f);
            int xStart = ((HandledScreenAccessor) screen).getX() + ((HandledScreenAccessor) screen).getBackgroundWidth();
            int yStart = ((HandledScreenAccessor) screen).getY() + 7;
            int widgetWidth = 200;
            int widgetHeight = ((HandledScreenAccessor) screen).getBackgroundHeight() - 8;
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
                    if (state != RecipeState.FIRST) state = RecipeState.FIRST;
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

            if (!(Models.Container.getCurrentContainer() instanceof CraftingStationContainer container)) return true;
            ProfessionType type = container.getProfessionType();

            targetOffset = 0;

            if (type == null) return true;

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
            float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.max(-1.15f, Math.min(relativeY, scrollAreaHeight));

            float scrollPercent = relativeY / scrollAreaHeight;

            targetOffset = scrollPercent * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderBackground(x, y, width, height, 2, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);

            int buttonHeight = 17;
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
            int buttonHeight = 17;
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
                ui.drawButton(x, y, width, height, 2, hovered || isHeld, WynnExtrasConfig.INSTANCE.craftingHelperDarkMode);
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
                if (bomb != null) {
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
                    if (type == BombType.PROFESSION_SPEED) bombType = "Speed";
                    if (type == BombType.PROFESSION_XP) bombType = "XP";

                    text = "§6" + (hovered ? "§n" : "") + "Profession " + bombType + " §7" + (hovered ? "§n" : "") + "on " + worldColor + bomb.server() + " §6" + (hovered ? "§n" : "") + "(" + bomb.getRemainingString() + ")";

                    if (bomb.getRemainingLong() < 30000) {
                        long seconds = Time.now().timestamp() / 1000;

                        String color = (seconds % 2 == 0) ? "§c" : "§4";
                        color += (hovered ? "§n" : "");

                        text = color + "Profession " + bombType + " on "
                                + bomb.server()
                                + " (" + bomb.getRemainingString() + ") (EXPIRING SOON)";
                    }

                    ui.drawCenteredText(text, x + width / 2f, y + height / 2f, 1f);
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (bomb.server().equals(Models.WorldState.getCurrentWorldName())) return true;

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if (bomb == null) return true;
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