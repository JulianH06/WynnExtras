package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.utils.Pair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static julianh06.wynnextras.features.profileviewer.PVScreen.getClassName;
import static julianh06.wynnextras.features.profileviewer.PVScreen.selectedCharacter;
import static julianh06.wynnextras.features.profileviewer.data.AbilityTreeData.iconToItemStack;

public class TreeTabWidget extends PVScreen.TabWidget {
    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground.png");
    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground_dark.png");

    static Identifier borderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders.png");
    static Identifier borderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders_dark.png");
    static Identifier pageLineTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/pageline.png");

    static Identifier strengthTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/strength.png");
    static Identifier dexterityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/dexterity.png");
    static Identifier intelligenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/intelligence.png");
    static Identifier defenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/defence.png");
    static Identifier agilityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/agility.png");


    static Identifier warrior = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior.png");
    static Identifier warriorActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior_active.png");

    static Identifier shaman = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman.png");
    static Identifier shamanActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman_active.png");

    static Identifier archer = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer.png");
    static Identifier archerActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer_active.png");

    static Identifier mage = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage.png");
    static Identifier mageActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage_active.png");

    static Identifier assassin = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin.png");
    static Identifier assassinActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin_active.png");

    static Identifier white = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white.png");
    static Identifier whiteActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white_active.png");

    static Identifier yellow = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow.png");
    static Identifier yellowActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow_active.png");

    static Identifier blue = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue.png");
    static Identifier blueActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue_active.png");

    static Identifier purple = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple.png");
    static Identifier purpleActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple_active.png");

    static Identifier red = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red.png");
    static Identifier redActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red_active.png");

    static Identifier vertical = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical.png");
    static Identifier verticalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical_active.png");

    static Identifier horizontal = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal.png");
    static Identifier horizontalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal_active.png");

    static Identifier down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left.png");
    static Identifier down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left_active.png");

    static Identifier right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down.png");
    static Identifier right_downActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_active.png");

    static Identifier right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left.png");
    static Identifier right_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active.png");

    static Identifier up_right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down.png");
    static Identifier up_right_downActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active.png");

    static Identifier up_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left.png");
    static Identifier up_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active.png");

    static Identifier up_right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left.png");
    static Identifier up_right_down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active.png");

    static Identifier up_right_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left.png");
    static Identifier up_right_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active.png");


    int scrollOffset;

    public TreeTabWidget() {
        super(0, 0, 0, 0);
        scrollOffset = 0;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
//        System.out.println(PVScreen.scrollOffset);
        if(PV.currentPlayerData == null) return;
        if(selectedCharacter == null) {
            ui.drawCenteredText("Select a character to view ability trees.", x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);
            return;
        }
        if(selectedCharacter.getSkillPoints() == null) return;

        String characterUUID = PV.currentPlayerData.getCharacters().entrySet().stream()
                .filter(e -> e.getValue().equals(selectedCharacter))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (characterUUID == null) return;

        String className = selectedCharacter.getType().toLowerCase();
        //System.out.println(characterUUID);
        //ui.drawCenteredText(characterUUID, x + 900, y + 345, CustomColor.fromHexString("FF0000"), 5f);

        AbilityTreeData tree = AbilityTreeCache.getClassTree(className);
        if (tree == null) {
            if (!AbilityTreeCache.isLoading(className)) {
                AbilityTreeCache.loadClassTree(className);
            }
            ui.drawCenteredText("Loading class ability tree...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        AbilityTreeData playerTree = AbilityTreeCache.getPlayerTree(characterUUID);
        if (playerTree == null) {
            if (!AbilityTreeCache.isLoading(className)) {
                AbilityTreeCache.loadCharacterTree(characterUUID);
            }
            ui.drawCenteredText("Loading character ability tree...", x + 900, y + 365, CustomColor.fromHexString("FFFF00"), 4f);
            return;
        }

        boolean hasNoAssignedSkillpoints = (selectedCharacter.getSkillPoints().getStrength() == 0) && (selectedCharacter.getSkillPoints().getDexterity() == 0) && (selectedCharacter.getSkillPoints().getIntelligence() == 0) && (selectedCharacter.getSkillPoints().getDefence() == 0) && (selectedCharacter.getSkillPoints().getAgility() == 0);

        if(playerTree.pages.isEmpty() && hasNoAssignedSkillpoints) {
            ui.drawCenteredText("This Player has their build stats private.", x + 900, y + 365, CustomColor.fromHexString("FF0000"), 4f);
            return;
        }

        PVScreen.scrollOffset = Math.min(2700, PVScreen.scrollOffset);

        Set<String> unlockedIds = new HashSet<>();
        Set<Pair<Integer, Integer>> connectorCoordinates = new HashSet<>();
        for (List<AbilityTreeData.Node> nodes : playerTree.pages.values()) {
            for (AbilityTreeData.Node node : nodes) {
                if (node.meta != null) {
                    if(node.type.equals("ability") && node.meta.id != null) {
                        unlockedIds.add(node.meta.id);
                    } else if(node.type.equals("connector")) {
                        connectorCoordinates.add(new Pair<>(node.coordinates.x, node.coordinates.y));
                    }
                }
            }
        }


        for (List<AbilityTreeData.Node> nodes : tree.pages.values()) {
            for (AbilityTreeData.Node node : nodes) {
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


//        List<AbilityTreeData.Node> nodes = tree.pages.get(3);
//        //System.out.println(nodes);
//        if (nodes == null) return;

        //ui.drawText(String.valueOf( "Strength: " + selectedCharacter.getSkillPoints().getStrength() + " Dexterity: " + selectedCharacter.getSkillPoints().getDexterity() + " Intelligence: " + selectedCharacter.getSkillPoints().getIntelligence()+ " Defence: " + selectedCharacter.getSkillPoints().getDefence()+ " Agility: " + selectedCharacter.getSkillPoints().getAgility()), x, y, CustomColor.fromHexString("FFFFFF"));
        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(backgroundTextureDark, x + 30, y + 30, 1740, 690);
        } else {
            ui.drawImage(backgroundTexture, x + 30, y + 30, 1740, 690);
        }

        ui.drawCenteredText("Strength", x + 80 + 37.5f, y + 150, CustomColor.fromHexString("00a800"));
        ui.drawCenteredText(String.valueOf(selectedCharacter.getSkillPoints().getStrength()), x + 80 + 37.5f, y + 270, CustomColor.fromHexString("00a800"));
        ui.drawImage(strengthTexture, x + 80, y + 170, 75, 75);

        ui.drawCenteredText("Dexterity", x + 260 + 37.5f, y + 150, CustomColor.fromHexString("fcfc54"));
        ui.drawCenteredText(String.valueOf(selectedCharacter.getSkillPoints().getDexterity()), x + 260 + 37.5f, y + 270, CustomColor.fromHexString("fcfc54"));
        ui.drawImage(dexterityTexture, x + 260, y + 170, 75, 75);

        ui.drawCenteredText("Intelligence", x + 440 + 37.5f, y + 150, CustomColor.fromHexString("54fcfc"));
        ui.drawCenteredText(String.valueOf(selectedCharacter.getSkillPoints().getIntelligence()), x + 440 + 37.5f, y + 270, CustomColor.fromHexString("54fcfc"));
        ui.drawImage(intelligenceTexture, x + 440, y + 170, 75, 75);

        ui.drawCenteredText("Defence", x + 620 + 37.5f, y + 150, CustomColor.fromHexString("fc5454"));
        ui.drawCenteredText(String.valueOf(selectedCharacter.getSkillPoints().getDefence()), x + 620 + 37.5f, y + 270, CustomColor.fromHexString("fc5454"));
        ui.drawImage(defenceTexture, x + 620, y + 170, 75, 75);

        ui.drawCenteredText("Agility", x + 800 + 37.5f, y + 150, CustomColor.fromHexString("fcfcfc"));
        ui.drawCenteredText(String.valueOf(selectedCharacter.getSkillPoints().getAgility()), x + 800 + 37.5f, y + 270, CustomColor.fromHexString("fcfcfc"));
        ui.drawImage(agilityTexture, x + 800, y + 170, 75, 75);

        ui.drawCenteredText("Save", x + 285 + 37.5f, y + 510, CustomColor.fromHexString("FFFFFF"), 6f);
        ui.drawCenteredText("Tree", x + 285 + 37.5f, y + 585, CustomColor.fromHexString("FFFFFF"), 6f);

        ui.drawCenteredText("Load", x + 775 + 37.5f, y + 510, CustomColor.fromHexString("FFFFFF"), 6f);
        ui.drawCenteredText("Tree", x + 775 + 37.5f, y + 585, CustomColor.fromHexString("FFFFFF"), 6f);


        //ui.drawRect(x + 900, y, 900, height, CustomColor.fromHexString("000000"));
//        System.out.println(height);
        List<AbilityTreeData.Node> abilities = new ArrayList<>(); //this is so to make the nodes always draw over the connectors
        List<AbilityTreeData.Node> connectors = new ArrayList<>();
        int i = 0;
        for(List<AbilityTreeData.Node> nodes : tree.pages.values()) {
            int yStart = 0;
            for (AbilityTreeData.Node node : nodes) {
                yStart = y + 75 + node.coordinates.y * 75 - PVScreen.scrollOffset;
                if (node.type.equals("ability")) {
                    abilities.add(node);
                } else {
                    connectors.add(node);
                }
            }
            i++;
            if(i != 7 && yStart + 75 > y && yStart + 75 < y + height - 100) {
                ui.drawImage(pageLineTexture, x + 1000, yStart + 75, 730, 32);
                if(i == 1 && yStart - 400 > y && yStart - 400 < y + height - 100) {
                    ui.drawText(String.valueOf(i), x + 1000, yStart - 400, CustomColor.fromHexString("434654"));
                }
                ui.drawText(String.valueOf(i + 1), x + 1000, yStart + 110, CustomColor.fromHexString("434654"));
            }
        }

        for(AbilityTreeData.Node node : connectors) {
            int yStart = y + 75 + node.coordinates.y * 75 - PVScreen.scrollOffset;
            Identifier texture = null;
            switch ((String) node.meta.icon) {
                case "connector_up_down" -> texture = node.unlocked ? verticalActive : vertical;
                case "connector_right_left" -> texture = node.unlocked ? horizontalActive : horizontal;
                case "connector_down_left" -> texture = node.unlocked ? down_leftActive : down_left;
                case "connector_right_down" -> texture = node.unlocked ? right_downActive : right_down;
                case "connector_right_down_left" -> texture = node.unlocked ? right_down_leftActive : right_down_left;
                case "connector_up_right_down" -> texture = node.unlocked ? up_right_downActive : up_right_down;
                case "connector_up_down_left" -> texture = node.unlocked ? up_down_leftActive : up_down_left;
                case "connector_up_right_down_left" -> texture = node.unlocked ? up_right_down_leftActive : up_right_down_left;
                case "connector_up_right_left" -> texture = node.unlocked ? up_right_leftActive : up_right_left;
            }
            if(texture != null && yStart - 25 > y && yStart - 25 < y + height - 100) {
                ui.drawImage(texture, x + node.coordinates.x * 75 + 917, yStart - 34, 145, 145);
            }
        }

        for(AbilityTreeData.Node node : abilities) {
            int yStart = y + 75 + node.coordinates.y * 75 - PVScreen.scrollOffset;
            Identifier texture = null;
            switch (((AbilityTreeData.Icon.IconValue) ((AbilityTreeData.Icon) node.meta.icon).value).name) {
                case "abilityTree.nodeWarrior" -> texture = node.unlocked ? warriorActive : warrior;
                case "abilityTree.nodeShaman" -> texture = node.unlocked ? shamanActive : shaman;
                case "abilityTree.nodeArcher" -> texture = node.unlocked ? archerActive : archer;
                case "abilityTree.nodeMage" -> texture = node.unlocked ? mageActive : mage;
                case "abilityTree.nodeAssassin" -> texture = node.unlocked ? assassinActive : assassin;
                case "abilityTree.nodeWhite" -> texture = node.unlocked ? whiteActive : white;
                case "abilityTree.nodeYellow" -> texture = node.unlocked ? yellowActive : yellow;
                case "abilityTree.nodeBlue" -> texture = node.unlocked ? blueActive : blue;
                case "abilityTree.nodePurple" -> texture = node.unlocked ? purpleActive : purple;
                case "abilityTree.nodeRed" -> texture = node.unlocked ? redActive : red;
            }
            if(texture != null && yStart - 25 > y && yStart - 25 < y + height - 120) {
                ui.drawImage(texture, x + node.coordinates.x * 75 + 925, yStart - 25, 125, 125);
            }
        }

        if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
            ui.drawImage(borderTextureDark, x, y, 1800, 750);
        } else {
            ui.drawImage(borderTexture, x, y, 1800, 750);
        }
        ui.drawCenteredText( McUtils.playerName() + "'s build for " + getClassName(selectedCharacter), x + 900, y + 50, CustomColor.fromHexString("FFFFFF"), 3.9f);
        ui.drawCenteredText( "coming soon", x + 730, y + 530, CustomColor.fromHexString("FF0000"), 6f);
        ui.drawCenteredText( "coming soon", x + 240, y + 530, CustomColor.fromHexString("FF0000"), 6f);
    }
}
