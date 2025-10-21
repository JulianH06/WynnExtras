package julianh06.wynnextras.features.abilitytree;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.container.scriptedquery.QueryStep;
import com.wynntils.handlers.container.scriptedquery.ScriptedContainerQuery;
import com.wynntils.handlers.container.type.ContainerContent;
import com.wynntils.handlers.container.type.ContainerContentChangeType;
import com.wynntils.models.character.CharacterModel;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.character.type.SavableSkillPointSet;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.items.items.game.HorseItem;
import com.wynntils.models.items.items.game.TomeItem;
import com.wynntils.models.items.items.gui.SkillPointItem;
import com.wynntils.models.stats.type.SkillStatType;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.wynn.ContainerUtils;
import com.wynntils.utils.wynn.InventoryUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.tabs.TreeTabWidget;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.UI.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static julianh06.wynnextras.features.profileviewer.PVScreen.getClassName;
import static julianh06.wynnextras.features.profileviewer.PVScreen.selectedCharacter;
import static julianh06.wynnextras.features.profileviewer.WynncraftApiHandler.parseStyledHtml;
import static julianh06.wynnextras.features.profileviewer.tabs.TreeTabWidget.*;

public class TreeScreen extends WEScreen {
    private Map<String, TreeData> trees = new HashMap<>();

    public enum Classes {Warrior, Shaman, Mage, Assassin, Archer}

    public List<ClassListWidget> classListWidgets = new ArrayList<>();

    static TreeData currentViewedTreeData;
    public static AbilityMapData.Node currentHoveredNode = null;
    List<NodeWidget> nodeWidgets = new ArrayList<>();

    public TreeScreen() {
        super(Text.of("Ability Tree Screen"));
        this.trees = TreeData.trees;
    }

    @Override
    protected void init() {
        classListWidgets.clear();
        rootWidgets.clear();
        //System.out.println(trees);
        AtomicInteger index = new AtomicInteger(0);
        int y = 0;
        ClassType type = Models.Character.getClassType();
        //McUtils.sendMessageToClient(Text.of(type.toString()));

        if(type != ClassType.NONE) {
            List<TreeListElement> classTrees = new ArrayList<>();
            int i = 0;
            for(TreeData treeData : trees.values()) {
                if(treeData == null) continue;
                if(treeData.className.equals(type.getName())) {
                    classTrees.add(new TreeListElement(i, ui, treeData, true));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, type.getName(), true);
            classListWidgets.add(element);
            addRootWidget(element);
            y += 100;
        }

        for(Classes classes : Classes.values()) {
            if(classes.toString().equals(type.getName())) {
                continue;
            }
            List<TreeListElement> classTrees = new ArrayList<>();
            int i = 0;
            for(TreeData treeData : trees.values()) {
                if(treeData == null) continue;
                if(treeData.className.equals(classes.toString())) {
                    classTrees.add(new TreeListElement(i, ui, treeData, false));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, classes.toString(), false);
            classListWidgets.add(element);
            addRootWidget(element);
            y += 100;
        }
//        trees.values().forEach(data -> {
//            int i = index.getAndIncrement();
//            TreeListElement e = new TreeListElement(i, ui, data);
//            addListElement(e);
//        });

    }

    @Override
    protected void scrollList(float delta) {
        //scrollOffset -= (int) (delta);
        //if(scrollOffset < 0) scrollOffset = 0;
    }

    @Override
    public void updateValues() {
        int sectionWidth = 900;
        int xStart = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth);
        int yStart = 0;
        for(ClassListWidget element : classListWidgets) {
            element.setBounds(xStart - 25, yStart, sectionWidth, 50);
            yStart += element.getHeight() + 20;
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int sectionWidth = 900;
        int xStart = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth);
        int yStart = 0;
        ui.drawRect( xStart - 25, yStart, sectionWidth, (float) (screenHeight * ui.getScaleFactor()), CustomColor.fromHexString("707070"));
        ui.drawRect((float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart, sectionWidth, (float) (screenHeight * ui.getScaleFactor()), CustomColor.fromHexString("404040"));
    }

    @Override
    public void close() {
        for(ClassListWidget widget : classListWidgets) {
            for(TreeListElement element : widget.elements) {
                if(element.nameInput == null) continue;
                if(element.nameInput.getInput() == null) continue;
                TreeData.getTree(element.data.name).visibleName = element.nameInput.getInput();
            }
        }
        TreeData.saveAll();
        TreeData.loadAll();
        super.close();
    }

    public static class ClassListWidget extends Widget {
        Identifier arrowClosedWhite = Identifier.of("wynnextras", "textures/gui/treeloader/arrow_closed_white.png");
        Identifier arrowClosedGray = Identifier.of("wynnextras", "textures/gui/treeloader/arrow_closed_gray.png");
        Identifier arrowOpenedWhite = Identifier.of("wynnextras", "textures/gui/treeloader/arrow_opened_white.png");
        Identifier arrowOpenedGray = Identifier.of("wynnextras", "textures/gui/treeloader/arrow_opened_gray.png");


        String playerClass;
        boolean active;
        boolean expanded;
        private Runnable action;
        List<TreeListElement> elements;

        public ClassListWidget(List<TreeListElement> elements, String playerClass, boolean active) {
            super(0, 0, 0, 0);
            this.playerClass = playerClass;
            this.active = active;
            this.expanded = active;
            this.elements = elements;
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                expanded = !expanded;
            };
            for(TreeListElement element : elements) {
                addChild(element);
            }
        }

        public int getHeight() {
            if(!expanded) return 50;
            return 50 + children.size() * 210;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            //System.out.println(x);
            //ui.drawRect(x, y, width, height, CustomColor.fromHexString("909090"));
            ui.drawCenteredText(playerClass, x + width / 2f, y + 25, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("B0B0B0"));
            ui.drawRect(x, y + 22.5f, width / 2f - McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")), 5, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("B0B0B0"));
            ui.drawRect(x + width / 2f + McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")), y + 22.5f, width / 2f - McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")) - 50, 5, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("B0B0B0"));
            ui.drawImage(expanded ? (active ? arrowOpenedWhite : arrowOpenedGray)
                    : (active ? arrowClosedWhite : arrowClosedGray),x + width - 45, y + 5, 40, 40);
            if(!expanded) {
                clearChildren();
                return;
            }

            int i = 0;

            boolean addChildren = children.isEmpty();
            for(TreeListElement element : elements) {
                if(addChildren) addChild(element);
                element.setBounds(x, y + 50 + 210 * i, width, 185);
                i++;
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            for(TreeListElement element : elements) {;
                if(element == null) continue;
                if(element.nameInput == null) continue;

                element.nameInput.setFocused(false);
                element.nameInput.blinkToggle = false;
            }
            return super.mouseClicked(mx, my, button);
        }
    }

    public static class TreeListElement extends Widget {
        private final int id;
        TreeData data;
        TextInputWidget nameInput;
        LoadButton loadButton;
        ViewButton viewButton;
        LoadButton loadButtonWithSkillpoints;
        boolean active;

        public TreeListElement(int id, UIUtils ui, TreeData data, boolean active) {
            super(0, 0, 100, 20);
            this.id = id;
            this.ui = ui;
            this.data = data;
            this.active = active;
            viewButton = new ViewButton(data);
            addChild(viewButton);
            if(!active) return;
            loadButton = new LoadButton(false);
            loadButtonWithSkillpoints = new LoadButton(true);
            addChild(loadButton);
            addChild(loadButtonWithSkillpoints);
        }

        @Override
        protected void drawBackground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            if(nameInput == null) {
                nameInput = new TextInputWidget(x, y, width, 50, 10, 13);
                nameInput.setInput(data.visibleName);
                nameInput.setBackgroundColor(null);
                nameInput.setTextColor(CustomColor.fromHexString("FFFFFF"));
                nameInput.setPlaceholder("<Click here to add a name>");
                children.add(nameInput);
            } else {
                nameInput.setBounds(x, y, width, 40);
            }

            if(!active) {
                viewButton.setBounds(x, y + 55, (int) ((width / 2f) - 25), 125);
            } else {
                viewButton.setBounds(x, y + 55, (int) ((width / 2f) - 25) / 2 - 2, 60);
                loadButton.setBounds(x + (int) ((width / 2f) - 25) / 2 + 2, y + 55, (int) ((width / 2f) - 25) / 2 - 2, 60);
                loadButtonWithSkillpoints.setBounds(x, y + 120, (int) (width / 2f) - 25, 60);
            }

            ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHexString("FF0000") : CustomColor.fromHexString("808080"));
            //ui.drawRect(x + width / 2f - 10, y + 30, width / 2f, height - 40, CustomColor.fromHexString("808080"));
            int iconSize = 75;
            int spacing = 18;

            ui.drawButton(x + width / 2f - 20, y + 55, width / 2f + 20, iconSize + 50, 17, false);

            ui.drawImage(TreeTabWidget.strengthTexture, x + width / 2f - 10, y + 60, iconSize, iconSize);
            ui.drawCenteredText(String.valueOf(data.strength), x + width / 2f + iconSize / 2f - 10, y + 80 + iconSize, CustomColor.fromHexString("00a800"));

            ui.drawImage(TreeTabWidget.dexterityTexture, x + width / 2f + (iconSize + spacing) - 10, y + 60, iconSize, iconSize);
            ui.drawCenteredText(String.valueOf(data.dexterity), x + width / 2f + iconSize / 2f + (iconSize + spacing) - 10, y + 80 + iconSize, CustomColor.fromHexString("fcfc54"));

            ui.drawImage(TreeTabWidget.intelligenceTexture, x + width / 2f + (iconSize + spacing) * 2 - 10, y + 60, iconSize, iconSize);
            ui.drawCenteredText(String.valueOf(data.intelligence), x + width / 2f + iconSize / 2f + (iconSize + spacing) * 2 - 10, y + 80 + iconSize, CustomColor.fromHexString("54fcfc"));

            ui.drawImage(TreeTabWidget.defenceTexture, x + width / 2f + (iconSize + spacing) * 3 - 10, y + 60, iconSize, iconSize);
            ui.drawCenteredText(String.valueOf(data.defence), x + width / 2f + iconSize / 2f + (iconSize + spacing) * 3 - 10, y + 80 + iconSize, CustomColor.fromHexString("fc5454"));

            ui.drawImage(TreeTabWidget.agilityTexture, x + width / 2f + (iconSize + spacing) * 4 - 10, y + 60, iconSize, iconSize);
            ui.drawCenteredText(String.valueOf(data.agility), x + width / 2f + iconSize / 2f + (iconSize + spacing) * 4 - 10, y + 80 + iconSize, CustomColor.fromHexString("fcfcfc"));

            ui.drawButton(x, y, width, 50, 17, hovered);

            //ui.drawText(data.name, x, y);
            //ui.drawRect(x, y, width, height);
            //            if(this.height <= 0) return;
//            if (ui == null) return;
//
//            // transformiere logical bounds → screen coords
//            int sx = (int) ui.sx(x);
//            int sy = (int) ui.sy(y);
//            int sw = ui.sw(width);
//            int sh = ui.sh(height);
//
//            // Hover-Erkennung in screen coords
//            hoveredLocal = mouseX >= sx && mouseY >= sy && mouseX < sx + sw && mouseY < sy + sh;
//            int fill = hoveredLocal ? 0xFFEFEFEF : 0xFFFFFFFF;
//
//            // zeichne Hintergrund über UIUtils
////            ui.drawRect(x, y, width, height, CustomColor.fromInt(fill));
//            ui.drawButton(x, y, width, height, 12, hovered);
//
//            // zeichne Text über UIUtils (zentriert oder linksbündig)
//            ui.drawCenteredText(textForIndex(id), x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 6f);
        }

        public static class LoadButton extends Widget {
            boolean withSkillpoints;
            private Runnable action;

            public LoadButton(boolean withSkillpoints) {
                super(0, 0, 0, 0);
                this.withSkillpoints = withSkillpoints;
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                };
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 17, hovered);
                //ui.drawRect(x, y, width, height);
                ui.drawCenteredText("Load Tree" + (withSkillpoints ? " and Skillpoints" : ""), x + width / 2f, y + height / 2f);
            }

            @Override
            protected boolean onClick(int button) {
                if (!isEnabled()) return false;
                if (action != null) action.run();
                return true;
            }
        }

        public static class ViewButton extends Widget {
            private Runnable action;

            public ViewButton(TreeData data) {
                super(0, 0, 0, 0);
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    System.out.println("clicked viewbutton");
                    currentViewedTreeData = data;
                };
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

                ui.drawButton(x, y, width, height, 17, hovered);
                //ui.drawRect(x, y, width, height);
                ui.drawCenteredText("View Tree", x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), (height == 60 ? 3f : 4f));
            }

            @Override
            protected boolean onClick(int button) {
                if (!isEnabled()) return false;
                if (action != null) action.run();
                return true;
            }
        }
    }


    //Wynntils Skillpoint loader, slightly changed to work without having to save the points in their ui

    private static final int TOME_SLOT = 8;
    private static final int[] SKILL_POINT_TOTAL_SLOTS = {11, 12, 13, 14, 15};
    private static final int SKILL_POINT_TOME_SLOT = 4;
    private static final int CONTENT_BOOK_SLOT = 62;
    private static final int TOME_MENU_CONTENT_BOOK_SLOT = 89;

    private Map<Skill, Integer> totalSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> gearSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> craftedSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> tomeSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> statusEffectSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> setBonusSkillPoints = new EnumMap<>(Skill.class);
    private Map<Skill, Integer> assignedSkillPoints = new EnumMap<>(Skill.class);

    public void loadSkillpoints(SavableSkillPointSet points) {
        ContainerUtils.closeBackgroundContainer();

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Loading Skill Point Loadout Query")
                .onError(msg -> WynntilsMod.warn("Failed to load skill point loadout: " + msg))
                .then(QueryStep.useItemInHotbar(InventoryUtils.COMPASS_SLOT_NUM)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer((container) -> loadSkillPointsOnServer(container, points)))
                .build();
        query.executeQuery();
    }

    private void loadSkillPointsOnServer(ContainerContent containerContent, SavableSkillPointSet points) {
        // we need to figure out which points we can subtract from first to actually allow assigning for positive points
        Map<Skill, Integer> negatives = new EnumMap<>(Skill.class);
        Map<Skill, Integer> positives = new EnumMap<>(Skill.class);
        for (int i = 0; i < Skill.values().length; i++) {
            int buildTarget = points.getSkillPointsAsArray()[i];
            int difference = buildTarget - getAssignedSkillPoints(Skill.values()[i]);

            // no difference automatically dropped here
            if (difference > 0) {
                positives.put(Skill.values()[i], difference);
            } else if (difference < 0) {
                negatives.put(Skill.values()[i], difference);
            }
        }

        boolean confirmationCompleted = false;
        for (Map.Entry<Skill, Integer> entry : negatives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // confirmation required, force loop to repeat this iteration
                    i--;
                    confirmationCompleted = true;
                }
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                        containerContent.items());
                if (!confirmationCompleted) {
                    // needs to exist in both loops in case of 1s only
                    i--;
                    confirmationCompleted = true;
                }
            }
        }

        for (Map.Entry<Skill, Integer> entry : positives.entrySet()) {
            int difference5s = Math.abs(entry.getValue()) / 5;
            int difference1s = Math.abs(entry.getValue()) % 5;

            for (int i = 0; i < difference5s; i++) {
                ContainerUtils.shiftClickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
            for (int i = 0; i < difference1s; i++) {
                ContainerUtils.clickOnSlot(
                        SKILL_POINT_TOTAL_SLOTS[entry.getKey().ordinal()],
                        containerContent.containerId(),
                        GLFW.GLFW_MOUSE_BUTTON_LEFT,
                        containerContent.items());
            }
        }

        // Server needs 2 ticks, give a couple extra to be safe
        Managers.TickScheduler.scheduleLater(this::populateSkillPoints, 4);
    }

    public int getAssignedSkillPoints(Skill skill) {
        return assignedSkillPoints.getOrDefault(skill, 0);
    }

    public void populateSkillPoints() {
        ContainerUtils.closeBackgroundContainer();

        Managers.TickScheduler.scheduleNextTick(() -> {
            assignedSkillPoints = new EnumMap<>(Skill.class);
            calculateGearSkillPoints();
            calculateStatusEffectSkillPoints();
            queryTotalAndTomeSkillPoints();
        });
    }

    private boolean verifyChange(ContainerContent content, Int2ObjectFunction<ItemStack> changes, ContainerContentChangeType changeType, int contentBookSlot) {
        return changeType == ContainerContentChangeType.SET_CONTENT && changes.containsKey(contentBookSlot) && ((ItemStack)content.items().get(contentBookSlot)).getItem() == Items.POTION;
    }

    private void calculateGearSkillPoints() {
        gearSkillPoints = new EnumMap<>(Skill.class);
        craftedSkillPoints = new EnumMap<>(Skill.class);
        setBonusSkillPoints = new EnumMap<>(Skill.class);

        for (ItemStack itemStack : Models.Inventory.getEquippedItems()) {
            calculateSingleGearSkillPoints(itemStack);
        }

        Models.Set.getUniqueSetNames().forEach(name -> {
            int trueCount = Models.Set.getTrueCount(name);
            Models.Set.getSetInfo(name).getBonusForItems(trueCount).forEach((bonus, value) -> {
                if (bonus instanceof SkillStatType skillStat) {
                    setBonusSkillPoints.merge(skillStat.getSkill(), value, Integer::sum);
                }
            });
        });
    }

    private void calculateSingleGearSkillPoints(ItemStack itemStack) {
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isEmpty()) return; // Empty slot

        if (wynnItemOptional.get() instanceof GearItem gear) {
            gear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    gearSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });

        } else if (wynnItemOptional.get() instanceof CraftedGearItem craftedGear) {
            craftedGear.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    craftedSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse gear: " + LoreUtils.getStringLore(itemStack));
        }
    }

    private void calculateStatusEffectSkillPoints() {
        statusEffectSkillPoints = new EnumMap<>(Skill.class);
        Models.StatusEffect.getStatusEffects().forEach(statusEffect -> {
            for (Skill skill : Skill.values()) {
                if (statusEffect.getName().contains(skill.getDisplayName())) {
                    statusEffectSkillPoints.merge(
                            skill,
                            Integer.parseInt(statusEffect.getModifier().getStringWithoutFormatting()),
                            Integer::sum);
                }
            }
        });
    }

    private void queryTotalAndTomeSkillPoints() {
        totalSkillPoints = new EnumMap<>(Skill.class);
        tomeSkillPoints = new EnumMap<>(Skill.class);

        ScriptedContainerQuery query = ScriptedContainerQuery.builder("Total and Tome Skill Point Query")
                .onError(msg -> WynntilsMod.warn("Failed to query skill points: " + msg))
                .then(QueryStep.useItemInHotbar(CharacterModel.CHARACTER_INFO_SLOT)
                        .expectContainerTitle(ContainerModel.CHARACTER_INFO_NAME)
                        .verifyContentChange((container, changes, changeType) ->
                                verifyChange(container, changes, changeType, CONTENT_BOOK_SLOT))
                        .processIncomingContainer(this::processTotalSkillPoints))
                .conditionalThen(
                        this::checkTomesUnlocked,
                        QueryStep.clickOnSlot(TOME_SLOT)
                                .expectContainerTitle(ContainerModel.MASTERY_TOMES_NAME)
                                .verifyContentChange((container, changes, changeType) ->
                                        verifyChange(container, changes, changeType, TOME_MENU_CONTENT_BOOK_SLOT))
                                .processIncomingContainer(this::processTomeSkillPoints))
                .execute(this::calculateAssignedSkillPoints)
                .build();

        query.executeQuery();
    }

    private boolean checkTomesUnlocked(ContainerContent content) {
        return LoreUtils.getStringLore(content.items().get(TOME_SLOT)).contains("✔");
    }

    private void processTotalSkillPoints(ContainerContent content) {
        for (Integer slot : SKILL_POINT_TOTAL_SLOTS) {
            Optional<WynnItem> wynnItemOptional =
                    Models.Item.getWynnItem(content.items().get(slot));
            if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof SkillPointItem skillPoint) {
                totalSkillPoints.merge(skillPoint.getSkill(), skillPoint.getSkillPoints(), Integer::sum);
            } else {
                WynntilsMod.warn("Skill Point Model failed to parse skill point item: "
                        + LoreUtils.getStringLore(content.items().get(slot)));
            }
        }
    }

    private void processTomeSkillPoints(ContainerContent content) {
        ItemStack itemStack = content.items().get(SKILL_POINT_TOME_SLOT);
        Optional<WynnItem> wynnItemOptional = Models.Item.getWynnItem(itemStack);
        if (wynnItemOptional.isPresent() && wynnItemOptional.get() instanceof TomeItem tome) {
            tome.getIdentifications().forEach(x -> {
                if (x.statType() instanceof SkillStatType skillStat) {
                    tomeSkillPoints.merge(skillStat.getSkill(), x.value(), Integer::sum);
                }
            });
        } else if (LoreUtils.getStringLore(itemStack).contains("§6Requirements:")) {
            // no-op, this is a tome that has not been unlocked or is not used by the player
        } else {
            WynntilsMod.warn("Skill Point Model failed to parse tome: "
                    + LoreUtils.getStringLore(content.items().get(SKILL_POINT_TOME_SLOT)));
        }
    }

    private void calculateAssignedSkillPoints() {
        for (Skill skill : Skill.values()) {
            assignedSkillPoints.put(
                    skill,
                    getTotalSkillPoints(skill)
                            - getGearSkillPoints(skill)
                            - getSetBonusSkillPoints(skill)
                            - getTomeSkillPoints(skill)
                            - getCraftedSkillPoints(skill)
                            - getStatusEffectSkillPoints(skill));
        }
    }

    public int getTotalSkillPoints(Skill skill) {
        return totalSkillPoints.getOrDefault(skill, 0);
    }

    public int getGearSkillPoints(Skill skill) {
        return gearSkillPoints.getOrDefault(skill, 0);
    }

    public int getCraftedSkillPoints(Skill skill) {
        return craftedSkillPoints.getOrDefault(skill, 0);
    }

    public int getTomeSkillPoints(Skill skill) {
        return tomeSkillPoints.getOrDefault(skill, 0);
    }

    public int getStatusEffectSkillPoints(Skill skill) {
        return statusEffectSkillPoints.getOrDefault(skill, 0);
    }

    public int getSetBonusSkillPoints(Skill skill) {
        return setBonusSkillPoints.getOrDefault(skill, 0);
    }
}
