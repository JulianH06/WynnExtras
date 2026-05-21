package julianh06.wynnextras.features.profileviewer.tabs;

import julianh06.wynnextras.core.WynnExtras;
import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.SaveButtonWidget;
import julianh06.wynnextras.features.profileviewer.Searchbar;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.features.profileviewer.data.SkillPoints;
import julianh06.wynnextras.utils.Pair;
import julianh06.wynnextras.utils.UI.AbilityTreeWidget;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.*;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class TreeTabWidget extends PVScreen.TabWidget {
    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground.png");
    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground_dark.png");

    static Identifier borderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders.png");
    static Identifier borderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders_dark.png");
    public static final Identifier pageLineTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/pageline.png");

    public static final Identifier strengthTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/strength.png");
    public static final Identifier dexterityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/dexterity.png");
    public static final Identifier intelligenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/intelligence.png");
    public static final Identifier defenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/defence.png");
    public static final Identifier agilityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/agility.png");

    public static final Identifier warrior = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior.png");
    public static final Identifier warriorActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior_active.png");

    public static final Identifier shaman = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman.png");
    public static final Identifier shamanActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman_active.png");

    public static final Identifier archer = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer.png");
    public static final Identifier archerActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer_active.png");

    public static final Identifier mage = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage.png");
    public static final Identifier mageActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage_active.png");

    public static final Identifier assassin = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin.png");
    public static final Identifier assassinActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin_active.png");

    public static final Identifier white = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white.png");
    public static final Identifier whiteActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white_active.png");

    public static final Identifier yellow = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow.png");
    public static final Identifier yellowActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow_active.png");

    public static final Identifier blue = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue.png");
    public static final Identifier blueActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue_active.png");

    public static final Identifier purple = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple.png");
    public static final Identifier purpleActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple_active.png");

    public static final Identifier red = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red.png");
    public static final Identifier redActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red_active.png");

    public static final Identifier vertical = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical.png");
    public static final Identifier verticalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical_active.png");

    public static final Identifier horizontal = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal.png");
    public static final Identifier horizontalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal_active.png");

    public static final Identifier down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left.png");
    public static final Identifier down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left_active.png");

    public static final Identifier right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down.png");
    public static final Identifier right_downActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_active.png");

    public static final Identifier right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left.png");
    public static final Identifier right_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active.png");

    public static final Identifier up_right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down.png");
    public static final Identifier up_right_downActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active.png");

    public static final Identifier up_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left.png");
    public static final Identifier up_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active.png");

    public static final Identifier up_right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left.png");
    public static final Identifier up_right_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active.png");

    public static final Identifier up_right_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left.png");
    public static final Identifier up_right_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active.png");

    static Identifier questSearchbarTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar.png");
    static Identifier questSearchbarTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/quests/questsearchbar_dark.png");


    int scrollOffset;

    SaveButtonWidget saveButtonWidget;

    public static AbilityMapData.Node currentHoveredNode = null;

    public static boolean loaded = false;

    private final AbilityTreeWidget abilityWidget;
    private final PVScreen.PVScrollBarWidget pvScrollBar;

    public TreeTabWidget() {
        super(0, 0, 0, 0);
        scrollOffset = 0;
        treeSearchBar = null;
        if(selectedCharacter != null) {
            this.abilityWidget = new AbilityTreeWidget(selectedCharacter.getType() , this.x, this.y, 1800, 750, 630);
            addChild(abilityWidget);
        } else {
            this.abilityWidget = null;
        }
        pvScrollBar = new PVScreen.PVScrollBarWidget();
        pvScrollBar.setVisible(false);
        addChild(pvScrollBar);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        scrollOffset = PVScreen.scrollOffset;
        if(PV.currentPlayerData == null) return;
        loaded = false;
        if(treeSearchBar == null) {
            treeSearchBar = new Searchbar( -1, -1, -1, -1);
            treeSearchBar.setSearchText("Search for ability...");
        }
        if(selectedCharacter == null) {
            ui.drawCenteredText("Select a character to view ability trees.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }
        //if(selectedCharacter.getSkillPoints() == null) return;

        String characterUUID = PV.currentPlayerData.getCharacters().entrySet().stream()
                .filter(e -> e.getValue().equals(selectedCharacter))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if(saveButtonWidget != null) {
            saveButtonWidget.setCharacterUUID(characterUUID);
            saveButtonWidget.setBounds(x + 280,  y + 424, 400, 225);
        }

        currentHoveredNode = null;

        if (characterUUID == null) return;

        String className = selectedCharacter.getType().toLowerCase();
        //WynnExtras.LOGGER.info(characterUUID);
        //ui.drawCenteredText(characterUUID, x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);

        AbilityMapData tree = AbilityTreeCache.getClassMap(className);
        if (tree == null) {
            if (!AbilityTreeCache.isLoading(className) && !AbilityTreeCache.isLoading(className + "tree")) {
                WynnExtras.LOGGER.info(className);
                AbilityTreeCache.loadClassTree(className);
            }
            ui.drawCenteredText("Loading class ability tree...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }


        //ctx.drawTooltip(McUtils.mc().textRenderer, Text.of(AbilityTreeCache.getClassTree(className).pages.get("1").get("bash").name), mouseX, mouseX);


        AbilityMapData playerTree = AbilityTreeCache.getPlayerTree(characterUUID);
        if (playerTree == null) {
            if (!AbilityTreeCache.isLoading(className)) {
                AbilityTreeCache.loadCharacterTree(characterUUID);
            }
            ui.drawCenteredText("Loading character ability tree...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        boolean hasNoAssignedSkillpoints = selectedCharacter.getSkillPoints() == null;
        if(!hasNoAssignedSkillpoints) hasNoAssignedSkillpoints = (selectedCharacter.getSkillPoints().getStrength() == 0) && (selectedCharacter.getSkillPoints().getDexterity() == 0) && (selectedCharacter.getSkillPoints().getIntelligence() == 0) && (Math.max(selectedCharacter.getSkillPoints().getDefence(), selectedCharacter.getSkillPoints().getDefense())== 0) && (selectedCharacter.getSkillPoints().getAgility() == 0);

        if(playerTree.pages.isEmpty() && hasNoAssignedSkillpoints) {
            List<String> apiKeyInfo = new ArrayList<>();
            if(MinecraftClient.getInstance().player != null && WynncraftApiHandler.INSTANCE.API_KEY == null || WynncraftApiHandler.INSTANCE.API_KEY.isEmpty()) {
                if(PV.currentPlayer.equalsIgnoreCase(MinecraftClient.getInstance().player.getName().getString())) {
                    apiKeyInfo.add("To get access to your private stats you need to set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                } else {
                    apiKeyInfo.add("You might be able to see them if you set an api-key.");
                    apiKeyInfo.add("You can find more info by using \"/we apikey\"");
                }
            }

            int apiKeyInfoY = y + 385;
            for(String line : apiKeyInfo) {
                ui.drawCenteredText(line, x + 900, apiKeyInfoY, CustomColor.fromHexString("FF0000"));
                apiKeyInfoY += 30;
            }

            ui.drawCenteredText("This Player has their build stats private.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 4f);
            return;
        }

        if(selectedCharacter != null && saveButtonWidget == null) {
            SkillPoints points;
            if(hasNoAssignedSkillpoints) {
                points = new SkillPoints();
                points.setStrength(0);
                points.setDexterity(0);
                points.setIntelligence(0);
                points.setDefence(0);
                points.setAgility(0);
            } else {
                points = selectedCharacter.getSkillPoints();
            }
            saveButtonWidget = new SaveButtonWidget(PV.currentPlayerData.getUsername(), selectedCharacter.getType(), points, tree, playerTree);
            children.add(saveButtonWidget);
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(questSearchbarTextureDark, questSearchbarTexture, x + 600F, y + height, 1050, 60, ui);

        //copied it from the quest search bar, its ugly but it works
        treeSearchBar.setX((int) ((x + 200 * 3) / ui.getScaleFactor()));
        treeSearchBar.setY((int) ((y + height + 7 * 3) / ui.getScaleFactor()));
        treeSearchBar.setWidth((int) (350 * 3 / ui.getScaleFactor()));
        treeSearchBar.setHeight((int) (14 * 3 / ui.getScaleFactor()));
        treeSearchBar.drawWithoutBackgroundButWithSearchtext(ctx, CustomColor.fromHexString("FFFFFF"), (float) ui.getScaleFactor());

        loaded = true;

        int maxCoordY = 0;
        for (List<AbilityMapData.Node> nodes : tree.pages.values()) {
            for (AbilityMapData.Node node : nodes) {
                maxCoordY = Math.max(maxCoordY, node.coordinates.y);
            }
        }
        PVScreen.maxScrollOffset = Math.max(0, maxCoordY * 75 - 480);
        scrollOffset = Math.min((int) PVScreen.maxScrollOffset, scrollOffset);

        pvScrollBar.setVisible(true);
        pvScrollBar.setBounds(x + 1910, y, 30, 750);

        Set<String> unlockedIds = new HashSet<>();
        Set<Pair<Integer, Integer>> connectorCoordinates = new HashSet<>();
        for (List<AbilityMapData.Node> nodes : playerTree.pages.values()) {
            for (AbilityMapData.Node node : nodes) {
                if (node.meta != null) {
                    if(node.type.equals("ability") && node.meta.id != null) {
                        unlockedIds.add(node.meta.id);
                    } else if(node.type.equals("connector")) {
                        connectorCoordinates.add(new Pair<>(node.coordinates.x, node.coordinates.y));
                    }
                }
            }
        }


        for (List<AbilityMapData.Node> nodes : tree.pages.values()) {
            for (AbilityMapData.Node node : nodes) {
                if (node.meta != null) {
                    if(node.type.equals("ability") && node.meta.id != null) {
                        node.unlocked = unlockedIds.contains(node.meta.id);
                    } else {
                        Pair<Integer, Integer> coords = new Pair<>(node.coordinates.x, node.coordinates.y);
                        node.unlocked = connectorCoordinates.contains(coords);
                    }
                }
            }
        }


//        List<AbilityMapData.Node> nodes = tree.pages.get(3);
//        //WynnExtras.LOGGER.info(nodes);
//        if (nodes == null) return;

        //ui.drawText(String.valueOf( "Strength: " + selectedCharacter.getSkillPoints().getStrength() + " Dexterity: " + selectedCharacter.getSkillPoints().getDexterity() + " Intelligence: " + selectedCharacter.getSkillPoints().getIntelligence()+ " Defence: " + selectedCharacter.getSkillPoints().getDefence()+ " Agility: " + selectedCharacter.getSkillPoints().getAgility()), x, y, CustomColor.fromHexString("FFFFFF"));
        PVScreen.DarkModeToggleWidget.drawImageWithFade(backgroundTextureDark, backgroundTexture, x + 30, y + 30, 1740, 690, ui);

        ui.drawCenteredText("Strength", x + 80 + 37.5f, y + 150, CustomColor.fromHexString("00a800"));
        ui.drawCenteredText(hasNoAssignedSkillpoints ? "unknown" : String.valueOf(selectedCharacter.getSkillPoints().getStrength()), x + 80 + 37.5f, y + 270, CustomColor.fromHexString("00a800"));
        ui.drawImage(strengthTexture, x + 80, y + 170, 75, 75);

        ui.drawCenteredText("Dexterity", x + 260 + 37.5f, y + 150, CustomColor.fromHexString("fcfc54"));
        ui.drawCenteredText(hasNoAssignedSkillpoints ? "unknown" : String.valueOf(selectedCharacter.getSkillPoints().getDexterity()), x + 260 + 37.5f, y + 270, CustomColor.fromHexString("fcfc54"));
        ui.drawImage(dexterityTexture, x + 260, y + 170, 75, 75);

        ui.drawCenteredText("Intelligence", x + 440 + 37.5f, y + 150, CustomColor.fromHexString("54fcfc"));
        ui.drawCenteredText(hasNoAssignedSkillpoints ? "unknown" : String.valueOf(selectedCharacter.getSkillPoints().getIntelligence()), x + 440 + 37.5f, y + 270, CustomColor.fromHexString("54fcfc"));
        ui.drawImage(intelligenceTexture, x + 440, y + 170, 75, 75);

        ui.drawCenteredText("Defence", x + 620 + 37.5f, y + 150, CustomColor.fromHexString("fc5454"));
        ui.drawCenteredText(hasNoAssignedSkillpoints ? "unknown" : String.valueOf(Math.max(selectedCharacter.getSkillPoints().getDefence(), selectedCharacter.getSkillPoints().getDefense())), x + 620 + 37.5f, y + 270, CustomColor.fromHexString("fc5454"));
        ui.drawImage(defenceTexture, x + 620, y + 170, 75, 75);

        ui.drawCenteredText("Agility", x + 800 + 37.5f, y + 150, CustomColor.fromHexString("fcfcfc"));
        ui.drawCenteredText(hasNoAssignedSkillpoints ? "unknown" : String.valueOf(selectedCharacter.getSkillPoints().getAgility()), x + 800 + 37.5f, y + 270, CustomColor.fromHexString("fcfcfc"));
        ui.drawImage(agilityTexture, x + 800, y + 170, 75, 75);

        //ui.drawCenteredText("Save &", x + 775 + 37.5f, y + 510, CustomColor.fromHexString("FFFFFF"), 6f);
        //ui.drawCenteredText("Load", x + 775 + 37.5f, y + 585, CustomColor.fromHexString("FFFFFF"), 6f);


        //ui.drawRect(x + 900, y, 900, height, CustomColor.fromHexString("000000"));
//        WynnExtras.LOGGER.info(height);
        List<AbilityMapData.Node> abilities = new ArrayList<>(); //this is so to make the nodes always draw over the connectors
        List<AbilityMapData.Node> connectors = new ArrayList<>();
        int i = 0;
        for(List<AbilityMapData.Node> nodes : tree.pages.values()) {
            int yStart = 0;
            for (AbilityMapData.Node node : nodes) {
                yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
                if (node.type.equals("ability")) {
                    abilities.add(node);
                } else {
                    connectors.add(node);
                }
            }
            i++;
//            if(i != 7 && yStart + 75 > y && yStart + 75 < y + height - 100) {
//                ui.drawImage(pageLineTexture, x + 1000, yStart + 75, 730, 32);
//                if(i == 1 && yStart - 400 > y && yStart - 400 < y + height - 100) {
//                    ui.drawText(String.valueOf(i), x + 1000, yStart - 400, CustomColor.fromHexString("434654"));
//                }
//                ui.drawText(String.valueOf(i + 1), x + 1000, yStart + 110, CustomColor.fromHexString("434654"));
//            }
        }

        abilityWidget.setPlayerTree(playerTree);
        abilityWidget.setClassTree(tree);
        abilityWidget.setScrollOffset(scrollOffset);
        if(searchBar != null) {
            abilityWidget.setSearchInput(searchBar.getInput());
        }
        abilityWidget.setBounds(x, y, 1800, 750);

//        for(AbilityMapData.Node node : connectors) {
//            int yStart = y + 75 + node.coordinates.y * 75 - PVScreen.scrollOffset;
//            Identifier texture = null;
//            switch ((String) node.meta.icon) {
//                case "connector_up_down" -> texture = node.unlocked ? verticalActive : vertical;
//                case "connector_right_left" -> texture = node.unlocked ? horizontalActive : horizontal;
//                case "connector_down_left" -> texture = node.unlocked ? down_leftActive : down_left;
//                case "connector_right_down" -> texture = node.unlocked ? right_downActive : right_down;
//                case "connector_right_down_left" -> texture = node.unlocked ? right_down_leftActive : right_down_left;
//                case "connector_up_right_down" -> texture = node.unlocked ? up_right_downActive : up_right_down;
//                case "connector_up_down_left" -> texture = node.unlocked ? up_down_leftActive : up_down_left;
//                case "connector_up_right_down_left" -> texture = node.unlocked ? up_right_down_leftActive : up_right_down_left;
//                case "connector_up_right_left" -> texture = node.unlocked ? up_right_leftActive : up_right_left;
//            }
//            if(texture != null && yStart - 25 > y && yStart - 25 < y + height - 100) {
//                ui.drawImage(texture, x + node.coordinates.x * 75 + 917, yStart - 34, 145, 145);
//            }
//        }
//        if(nodeWidgets.isEmpty()) {
//            for(AbilityMapData.Node node : abilities) {
//                NodeWidget w = new NodeWidget(x, y, node);
//                nodeWidgets.add(w);
//                children.add(w);
//            }
//        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(PV.currentPlayerData == null) return;
        if(selectedCharacter == null) {
            return;
        }

        if(!loaded) return;

        AbilityTreeData treeData = AbilityTreeCache.getClassTree(selectedCharacter.getType().toLowerCase());
        if(treeData != null) {
            if(selectedCharacter != null) {
                saveButtonWidget.setClassTree(treeData);
            }
            if(treeData.pages != null) {
                for(Map<String, AbilityTreeData.Ability> pagee : treeData.pages.values()) {
                    for(AbilityTreeData.Ability ability : pagee.values()) {
                        if(treeSearchBar.getInput().isEmpty() || ability.name == null) {
                            continue;
                        }
                        if(!ability.name.toLowerCase().contains(treeSearchBar.getInput().toLowerCase())) {
                            continue;
                        }
                        int yStart = y + 75 + ability.coordinates.y * 75 - PVScreen.scrollOffset + (450 * (ability.page - 1));
                        if(yStart - 25 > y && yStart - 25 < y + 630) {
                            ui.drawRectBorders(x + ability.coordinates.x * 75 + 943, yStart - 7, x + ability.coordinates.x * 75 + 943 + 90, yStart - 7 + 90, CustomColor.fromHexString("FFFF00"));
                        }
                    }
                }
            }
        }

        PVScreen.DarkModeToggleWidget.drawImageWithFade(borderTextureDark, borderTexture,  x, y, 1800, 750, ui);

        ui.drawCenteredText( PV.currentPlayerData.getUsername() + "'s build for " + getClassName(selectedCharacter), x + 900, y + 50, CustomColor.fromHexString("FFFFFF"), 3.9f);
        //ui.drawCenteredText( "coming soon", x + 730, y + 530, CustomColor.fromHexString("FF0000"), 6f);
        //ui.drawCenteredText( "coming soon", x + 240, y + 530, CustomColor.fromHexString("FF0000"), 6f);




        abilityWidget.drawNodeTooltip(ctx, mouseX, mouseY);

        //WynnExtras.LOGGER.info(y);
        //ui.drawRect(x + 950, y + 100, 100, 550);

    }
}
