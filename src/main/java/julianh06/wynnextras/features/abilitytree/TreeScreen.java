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
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.Searchbar;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.tabs.TreeTabWidget;
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

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;
import static julianh06.wynnextras.features.profileviewer.PVScreen.treeSearchBar;

public class TreeScreen extends WEScreen {
    private Map<String, TreeData> trees = new HashMap<>();

    public enum Classes {Warrior, Shaman, Mage, Assassin, Archer}

    public List<ClassListWidget> classListWidgets = new ArrayList<>();

    static TreeData currentViewedTreeData;

    public AbilityTreeWidget abilityWidget;
    static TextInputWidget treeSearchBar;

    static int leftScrollOffset, rightScrollOffset;

    Identifier abilityTreeBackground = Identifier.of("wynnextras", "textures/gui/treeloader/abilitytreebackground.png");
    static Identifier trashcan = Identifier.of("wynnextras", "textures/gui/treeloader/trashcan.png");

    public TreeScreen() {
        super(Text.of("Ability Tree Screen"));
        this.trees = TreeData.trees;
        treeSearchBar = null;
    }

    @Override
    protected void init() {
        classListWidgets.clear();
        rootWidgets.clear();
        currentViewedTreeData = null;
        abilityWidget = null;
        treeSearchBar = null;
        registerScrolling();
        leftScrollOffset = 0;
        rightScrollOffset = 0;

        ClassType type = Models.Character.getClassType();

        if(type != ClassType.NONE) {
            List<TreeListElement> classTrees = new ArrayList<>();
            int i = 0;
            for(TreeData treeData : trees.values()) {
                if(treeData == null) continue;
                if(treeData.className.equals(type.getName())) {
                    classTrees.add(new TreeListElement(i, ui, treeData, true, this));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, type.getName(), true);
            classListWidgets.add(element);
            addRootWidget(element);
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
                    classTrees.add(new TreeListElement(i, ui, treeData, false, this));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, classes.toString(), false);
            classListWidgets.add(element);
            addRootWidget(element);
        }
    }

    protected void initWithoutInitingScrolling() {
        classListWidgets.clear();
        rootWidgets.clear();
        currentViewedTreeData = null;
        abilityWidget = null;
        treeSearchBar = null;
        leftScrollOffset = 0;
        rightScrollOffset = 0;

        ClassType type = Models.Character.getClassType();

        if(type != ClassType.NONE) {
            List<TreeListElement> classTrees = new ArrayList<>();
            int i = 0;
            for(TreeData treeData : trees.values()) {
                if(treeData == null) continue;
                if(treeData.className.equals(type.getName())) {
                    classTrees.add(new TreeListElement(i, ui, treeData, true, this));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, type.getName(), true);
            classListWidgets.add(element);
            addRootWidget(element);
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
                    classTrees.add(new TreeListElement(i, ui, treeData, false, this));
                    i++;
                }
            }
            ClassListWidget element = new ClassListWidget(classTrees, classes.toString(), false);
            classListWidgets.add(element);
            addRootWidget(element);
        }
    }

    @Override
    protected void scrollList(float delta) {
        int sectionWidth = 900;
        int xStart = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth);
        int yStart = 0;
        if(mouseX > ui.sx(xStart - 75)
                && mouseY > ui.sy(yStart)
                && mouseX < ui.sx(xStart - 25 + sectionWidth)
                && mouseY < yStart + (float) (screenHeight * ui.getScaleFactor())) {
            leftScrollOffset -= (int) (delta);
            if(leftScrollOffset < 0) leftScrollOffset = 0;
        }
        if(mouseX > ui.sx(xStart + sectionWidth + 15)
                && mouseY > ui.sy(yStart)
                && mouseX < ui.sx(xStart + 15 + sectionWidth * 2)
                && mouseY < yStart + (float) (screenHeight * ui.getScaleFactor())) {
            rightScrollOffset -= (int) (delta);
            if(rightScrollOffset < 0) rightScrollOffset = 0;
        }
    }

    @Override
    public void updateValues() {
        int sectionWidth = 900;
        int xStart = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth);
        int yStart = -leftScrollOffset;
        for(ClassListWidget element : classListWidgets) {
            element.setBounds(xStart - 25 - 20, yStart, sectionWidth, 50);
            yStart += element.getHeight() + 20;
        }
        if(abilityWidget == null && currentViewedTreeData != null) {
            this.abilityWidget = new AbilityTreeWidget(currentViewedTreeData.className, xStart, 0, 1800, 750, 100000);
            rootWidgets.add(abilityWidget);
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;
        int sectionWidth = 900;
        int xStart = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth) - 20;
        int yStart = 0;

        ui.drawRect( xStart - 25 - 20, yStart, sectionWidth + 40, (float) (screenHeight * ui.getScaleFactor()), CustomColor.fromHexString("000000").withAlpha(0.20f));
        //ui.drawRect((float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart, sectionWidth, (float) (screenHeight * ui.getScaleFactor()), CustomColor.fromHexString("404040"));

        ui.drawImage(abilityTreeBackground, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart - (rightScrollOffset % (sectionWidth * 0.70f)), sectionWidth, sectionWidth * 0.70f);
        ui.drawImage(abilityTreeBackground, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart + sectionWidth * 0.70f - (rightScrollOffset % (sectionWidth * 0.70f)), sectionWidth, sectionWidth * 0.70f);
        ui.drawImage(abilityTreeBackground, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart + sectionWidth * 0.70f * 2 - (rightScrollOffset % (sectionWidth * 0.70f)), sectionWidth, sectionWidth * 0.70f);
        ui.drawImage(abilityTreeBackground, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart + sectionWidth * 0.70f * 3 - (rightScrollOffset % (sectionWidth * 0.70f)), sectionWidth, sectionWidth * 0.70f);

        //ui.drawImage(treeBorderTop, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart, sectionWidth, 30);
        //ui.drawImage(treeBorderMid, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, yStart + 30, sectionWidth, (float) (screenHeight * ui.getScaleFactor() - 60));
        //ui.drawImage(treeBorderBot, (float) (screenWidth * ui.getScaleFactor()) / 2 + 25, (float) (screenHeight * ui.getScaleFactor() - 30), sectionWidth, 30);

        if(currentViewedTreeData == null) return;
        //ui.drawCenteredText(characterUUID, x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);

        AbilityMapData tree = AbilityTreeCache.getClassMap(currentViewedTreeData.className.toLowerCase());
        if (tree == null) {
            if (!AbilityTreeCache.isLoading(currentViewedTreeData.className.toLowerCase()) && !AbilityTreeCache.isLoading(currentViewedTreeData.className.toLowerCase() + "tree")) {
                AbilityTreeCache.loadClassTree(currentViewedTreeData.className.toLowerCase());
            }
            return;
        }
        AbilityMapData playerTree = currentViewedTreeData.playerMap;

        if(tree == null || playerTree == null) return;

        abilityWidget.setPlayerTree(playerTree);
        abilityWidget.setClassTree(tree);
        abilityWidget.setScrollOffset(rightScrollOffset);
        if(searchBar != null) {
            abilityWidget.setSearchInput(searchBar.getInput());
        }
        abilityWidget.setBounds(xStart, -100, 1800, 750);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int sectionWidth = 900;
        int x = (int) (screenWidth * ui.getScaleFactor() / 2 - sectionWidth) - 20;
        int y = 0;

        if(abilityWidget != null && treeSearchBar != null) {
            AbilityTreeData treeData = AbilityTreeCache.getClassTree(abilityWidget.className.toLowerCase());
            if (treeData != null) {
                if (treeData.pages != null) {
                    for (Map<String, AbilityTreeData.Ability> pagee : treeData.pages.values()) {
                        for (AbilityTreeData.Ability ability : pagee.values()) {
                            if (treeSearchBar.getInput().isEmpty() || ability.name == null) {
                                continue;
                            }
                            if (!ability.name.toLowerCase().contains(treeSearchBar.getInput().toLowerCase())) {
                                continue;
                            }
                            int yStart = y - 25 + ability.coordinates.y * 75 - rightScrollOffset + (450 * (ability.page - 1));
                            if (yStart + 75 > y) {
                                ui.drawRectBorders(x + ability.coordinates.x * 75 + 943, yStart - 7, x + ability.coordinates.x * 75 + 943 + 90, yStart - 7 + 90, CustomColor.fromHexString("FFFF00"));
                            }
                        }
                    }
                }
            }
        }

        if(treeSearchBar == null) {
            treeSearchBar = new TextInputWidget(x + sectionWidth, getLogicalHeight() - 50, sectionWidth, 40, 10, 13);
            treeSearchBar.setBackgroundColor(null);
            treeSearchBar.setTextColor(CustomColor.fromHexString("FFFFFF"));
            treeSearchBar.setPlaceholder("Search for ability...");
            rootWidgets.add(treeSearchBar);
            //treeSearchBar.draw(ctx, mouseX, mouseY, tickDelta, ui);
        } else {
            ui.drawButton(x + sectionWidth + 40, getLogicalHeight() - 50, sectionWidth, 50, 17, treeSearchBar.isHovered());
            treeSearchBar.setBounds(x + sectionWidth + 43, getLogicalHeight() - 50, sectionWidth, 40);
            treeSearchBar.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        if(abilityWidget != null) abilityWidget.drawNodeTooltip(ctx, mouseX, mouseY);
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
        scrollOffset = 0;
        TreeData.saveAll();
        TreeData.loadAll();
        super.close();
    }

//    public static void onClick() {
//        if(treeSearchBar == null) return;
//
//        if (treeSearchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
//            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//            treeSearchBar.click();
//        } else {
//            treeSearchBar.setActive(false);
//        }
//    }

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
            //ui.drawRect(x, y, width, height, CustomColor.fromHexString("909090"));
            ui.drawCenteredText(playerClass, x + width / 2f, y + 25, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("909090"));
            ui.drawRect(x, y + 22.5f, width / 2f - McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")), 5, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("909090"));
            ui.drawRect(x + width / 2f + McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")), y + 22.5f, width / 2f - McUtils.mc().textRenderer.getWidth(Text.of("     " + playerClass + "     ")) - 50, 5, active ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("909090"));
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
        DeleteButton deleteButton;
        ConfirmationButton yesButton;
        ConfirmationButton noButton;
        boolean active;
        public boolean pendingDeletion = false;
        public TreeScreen parentScreen;

        public TreeListElement(int id, UIUtils ui, TreeData data, boolean active, TreeScreen screen) {
            super(0, 0, 100, 20);
            this.id = id;
            this.ui = ui;
            this.data = data;
            this.active = active;
            viewButton = new ViewButton(data, screen);
            addChild(viewButton);
            deleteButton = new DeleteButton(this);
            addChild(deleteButton);
            parentScreen = screen;
            if(!active) return;
            loadButton = new LoadButton(false, data.name);
            loadButtonWithSkillpoints = new LoadButton(true, data.name);
            addChild(loadButton);
            addChild(loadButtonWithSkillpoints);
        }

        @Override
        protected void drawBackground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            //ui.drawRect(x - 7, y - 7, width + 14, height + 14, CustomColor.fromHexString("9b785a"));
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            if(nameInput == null) {
                nameInput = new TextInputWidget(x, y, width - 140, 50, 10, 13);
                nameInput.setInput(data.visibleName);
                nameInput.setBackgroundColor(null);
                nameInput.setTextColor(CustomColor.fromHexString("FFFFFF"));
                nameInput.setPlaceholder("<Click here to add a name>");
                children.add(nameInput);
            } else {
                nameInput.setBounds(x, y, width - 140, 40);
            }

            if(!active) {
                viewButton.setBounds(x, y + 55, (int) ((width / 2f) - 25), 125);
            } else {
                viewButton.setBounds(x, y + 55, (int) ((width / 2f) - 25) / 2 - 2, 60);
                loadButton.setBounds(x + (int) ((width / 2f) - 25) / 2 + 2, y + 55, (int) ((width / 2f) - 25) / 2 - 2, 60);
                loadButtonWithSkillpoints.setBounds(x, y + 120, (int) (width / 2f) - 25, 60);
            }

            //if(deleteButton != null) deleteButton.setBounds(x + width - 130, y, 130, 50);
            if(deleteButton != null) deleteButton.setBounds(x + width - 50, y, 50, 50);

            //ui.drawRect(x, y, width, height, hovered ? CustomColor.fromHexString("FF0000") : CustomColor.fromHexString("808080"));
            //ui.drawRect(x + width / 2f - 10, y + 30, width / 2f, height - 40, CustomColor.fromHexString("808080"));
            int iconSize = 75;
            int spacing = 18;

            ui.drawButton(x + width / 2f - 20, y + 55, width / 2f + 20, iconSize + 50, 17, false);

            if(!pendingDeletion) {
                if(yesButton != null) {
                    children.remove(yesButton);
                    yesButton = null;
                }

                if(noButton != null) {
                    children.remove(noButton);
                    noButton = null;
                }

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
            } else {
                ui.drawCenteredText("Do you want to delete this tree?", x + width * 3/4f - 7.5f, y + 90, CustomColor.fromHexString("FF0000"), 2.70f);
                if(yesButton == null) {
                    yesButton = new ConfirmationButton(this, true);
                    children.add(yesButton);
                }
                if(noButton == null) {
                    noButton = new ConfirmationButton(this, false);
                    children.add(noButton);
                }

                yesButton.setBounds((int) (x + width * 3/4f - 87.5f) - 50, y + 110, 100, 50);
                noButton.setBounds((int) (x + width * 3/4f + 72.5f) - 50, y + 110, 100, 50);
            }

            ui.drawButton(x, y, width - 60, 50, 17, nameInput.isHovered());

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

            public LoadButton(boolean withSkillpoints, String treeName) {
                super(0, 0, 0, 0);
                this.withSkillpoints = withSkillpoints;
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    TreeData tree = TreeData.getTree(treeName);
                    if (tree == null) {
                        McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(Text.of("This tree doesn't exist.")));
                        return;
                    }
                    McUtils.mc().setScreen(null);
                    TreeLoader.resetAll();
                    TreeLoader.wasStarted = true;
                    TreeLoader.resetTree = true;
                    List<AbilityTreeData.Ability> abilities = TreeLoader.calculateNodeOrder(tree.playerTree.archetypes, TreeLoader.convertNodeMapToList(tree.playerMap), new ArrayList<>(), tree.playerTree);

                    List<AbilityMapData.Node> nodes = new ArrayList<>();
                    for(AbilityTreeData.Ability ability : abilities) {
                        nodes.add(TreeLoader.getNodeFromAbility(ability, tree.playerMap));
                    }
                    TreeLoader.abilitiesToClick2 = nodes;
                    TreeLoader.loadSkillpoints = withSkillpoints;
                    int[] points = new int[5];
                    points[0] = tree.strength;
                    points[1] = tree.dexterity;
                    points[2] = tree.intelligence;
                    points[3] = tree.defence;
                    points[4] = tree.agility;
                    TreeLoader.skillPointSet = withSkillpoints ? new SavableSkillPointSet(points) : null;
                    TreeLoader.classTree = tree.playerTree;
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

            public ViewButton(TreeData data, TreeScreen screen) {
                super(0, 0, 0, 0);
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    currentViewedTreeData = data;
                    screen.removeRootWidget(screen.abilityWidget);
                    screen.abilityWidget = null;
                    //if(screen.abilityWidget != null) screen.abilityWidget.clearTrees();
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

        public static class DeleteButton extends Widget {
            private Runnable action;

            public DeleteButton(TreeListElement parent) {
                super(0, 0, 0, 0);
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    parent.pendingDeletion = true;
                };
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 17, hovered);
                //ui.drawText("DELETE", x + width - 65, y + 12, CustomColor.fromHexString("FF0000"), HorizontalAlignment.CENTER, VerticalAlignment.TOP, 3f);
                ui.drawImage(trashcan, x + 10, y + 10, 30, 30);
            }

            @Override
            protected boolean onClick(int button) {
                if (!isEnabled()) return false;
                if (action != null) action.run();
                return true;
            }
        }

        public static class ConfirmationButton extends Widget {
            private Runnable action;
            boolean yesno;

            public ConfirmationButton(TreeListElement parent, boolean yesno) {
                super(0, 0, 0, 0);
                this.action = () -> {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    if(yesno) {
                        TreeLoader.deletePlayerAbilityTree(parent.data.name + ".json");
                        List<Boolean> expanded = new ArrayList<>();
                        for(ClassListWidget classListWidget : parent.parentScreen.classListWidgets) {
                            expanded.add(classListWidget.expanded);
                        }
                        parent.parentScreen.initWithoutInitingScrolling();
                        int i = 0;
                        for(ClassListWidget classListWidget : parent.parentScreen.classListWidgets) {
                            classListWidget.expanded = expanded.get(i);
                            i++;
                        }
                        return;
                    }
                    parent.pendingDeletion = false;
                };
                this.yesno = yesno;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, 17, hovered);
                ui.drawCenteredText(yesno ? "YES" : "NO", x + 50, y + 25, CustomColor.fromHexString("FFFFFF"), 2.75f);
                //ui.drawImage(trashcan, x + 10, y + 10, 30, 30);
            }

            @Override
            protected boolean onClick(int button) {
                if (!isEnabled()) return false;
                if (action != null) action.run();
                return true;
            }
        }
    }
}