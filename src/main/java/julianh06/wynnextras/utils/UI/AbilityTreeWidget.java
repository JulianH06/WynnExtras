package julianh06.wynnextras.utils.UI;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.utils.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import java.util.*;

import static julianh06.wynnextras.features.profileviewer.WynncraftApiHandler.fetchClassAbilityMap;
import static julianh06.wynnextras.features.profileviewer.WynncraftApiHandler.parseStyledHtml;

public class AbilityTreeWidget extends Widget {
    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground.png");
    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackground_dark.png");

    static Identifier borderTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders.png");
    static Identifier borderTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/treetabbackgroundborders_dark.png");
    public static Identifier pageLineTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/pageline.png");

    public static Identifier strengthTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/strength.png");
    public static Identifier dexterityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/dexterity.png");
    public static Identifier intelligenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/intelligence.png");
    public static Identifier defenceTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/defence.png");
    public static Identifier agilityTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/skillpoints/agility.png");

    public static Identifier warrior = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior.png");
    public static Identifier warriorActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/warrior_active.png");

    public static Identifier shaman = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman.png");
    public static Identifier shamanActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/shaman_active.png");

    public static Identifier archer = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer.png");
    public static Identifier archerActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/archer_active.png");

    public static Identifier mage = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage.png");
    public static Identifier mageActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/mage_active.png");

    public static Identifier assassin = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin.png");
    public static Identifier assassinActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/assassin_active.png");

    public static Identifier white = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white.png");
    public static Identifier whiteActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/white_active.png");

    public static Identifier yellow = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow.png");
    public static Identifier yellowActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/yellow_active.png");

    public static Identifier blue = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue.png");
    public static Identifier blueActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/blue_active.png");

    public static Identifier purple = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple.png");
    public static Identifier purpleActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/purple_active.png");

    public static Identifier red = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red.png");
    public static Identifier redActive = Identifier.of("wynnextras", "textures/gui/profileviewer/node/red_active.png");

    public static Identifier vertical = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical.png");
    public static Identifier verticalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/vertical_active.png");

    public static Identifier horizontal = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal.png");
    public static Identifier horizontalActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/horizontal_active.png");

    public static Identifier down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left.png");
    public static Identifier down_leftActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/down_left_active.png");

    public static Identifier right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down.png");
    public static Identifier right_downActive = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_active.png");

    public static Identifier right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left.png");
    public static Identifier RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active.png");
    public static Identifier right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active1.png");
    public static Identifier RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active2.png");
    public static Identifier RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active3.png");

    public static Identifier up_right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down.png");
    public static Identifier UP_RIGHT_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active.png");
    public static Identifier UP_RIGHT_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active1.png");
    public static Identifier up_RIGHT_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active2.png");
    public static Identifier UP_right_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active3.png");

    public static Identifier up_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left.png");
    public static Identifier UP_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active.png");
    public static Identifier UP_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active1.png");
    public static Identifier up_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active2.png");
    public static Identifier UP_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active3.png");

    public static Identifier up_right_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left.png");
    public static Identifier UP_RIGHT_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active.png");
    public static Identifier UP_right_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active1.png");
    public static Identifier UP_RIGHT_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active2.png");
    public static Identifier up_RIGHT_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active3.png");

    public static Identifier up_right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left.png");
    public static Identifier UP_RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active.png");
    public static Identifier UP_RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active1.png");
    public static Identifier UP_RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active2.png");
    public static Identifier up_RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active3.png");
    public static Identifier UP_right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active4.png");
    public static Identifier UP_right_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active5.png");
    public static Identifier UP_RIGHT_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active6.png");
    public static Identifier up_RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active7.png");
    public static Identifier up_right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active8.png");
    public static Identifier UP_right_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active9.png");
    public static Identifier up_RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active10.png");

    public final String className;

    private int scrollOffset;

    public static AbilityMapData.Node currentHoveredNode = null;
    public static boolean loaded = false;

    public AbilityMapData classTree;
    public AbilityMapData playerTree;

    private final AbilityTreeState state = new AbilityTreeState();

    private final List<NodeWidget> nodeWidgets = new ArrayList<>();

    private String searchInput = "";

    int botLimit;

    public AbilityTreeWidget(String className, int x, int y, int width, int height, int botLimit) {
        super(x, y, width, height);
        this.className = className == null ? "" : className;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.botLimit = botLimit;
    }

    public void setClassTree(AbilityMapData classTree) {
        this.classTree = classTree;
        refreshState();
    }

    public void setPlayerTree(AbilityMapData playerTree) {
        this.playerTree = playerTree;
        refreshState();
    }

    public void clearTrees() {
        this.classTree = null;
        this.playerTree = null;
        refreshState();
    }

    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    public void refreshState() {
        state.reset();

        if (!nodeWidgets.isEmpty()) {
            children.removeAll(nodeWidgets);
            nodeWidgets.clear();
        }

        currentHoveredNode = null;

        if (this.classTree == null) {
            AbilityMapData loadedClass = AbilityTreeCache.getClassMap(this.className);
            if (loadedClass != null) this.classTree = loadedClass;
        }

        if (this.classTree == null || this.playerTree == null) {
            loaded = false;
            return;
        }

        state.prepare(this.classTree, this.playerTree);
        for (AbilityMapData.Node n : state.abilities) {
            NodeWidget w = new NodeWidget(x, y, n, botLimit, scrollOffset);
            w.parent = this;
            nodeWidgets.add(w);
            children.add(w);
        }

        loaded = true;
    }


    public AbilityMapData.Node getNodeAt(int pageId, int coordX, int coordY) {
        if (state.classTree == null || state.classTree.pages == null) return null;
        List<AbilityMapData.Node> nodes = state.classTree.pages.get(pageId);
        if (nodes == null) return null;
        for (AbilityMapData.Node n : nodes) {
            if (n.coordinates.x == coordX && n.coordinates.y == coordY) return n;
        }
        return null;
    }

    public void setSearchInput(String input) {
        this.searchInput = input == null ? "" : input;
    }

    public AbilityMapData.Node getCurrentHoveredNode() {
        return currentHoveredNode;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (!loaded || state.classTree == null) return;

        //System.out.println(nodeWidgets);

        for (AbilityMapData.Node node : state.connectors) {
            int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
            Identifier tex = connectorTextureFor(node);
            if (tex != null && yStart - 25 > y && yStart - 25 < y + botLimit) {
                ui.drawImage(tex, x + node.coordinates.x * 75 + 917, yStart - 34, 145, 145);
            }
        }

        if (nodeWidgets.isEmpty() && !state.abilities.isEmpty()) {
            for (AbilityMapData.Node node : state.abilities) {
                NodeWidget w = new NodeWidget(x, y, node, botLimit, scrollOffset);
                w.parent = this;
                nodeWidgets.add(w);
                children.add(w);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (!loaded) return;

        AbilityTreeData treeData = AbilityTreeCache.getClassTree(this.className);
        if (treeData != null && treeData.pages != null && searchInput != null && !searchInput.isEmpty()) {
            String search = searchInput.toLowerCase();
            for (Map<String, AbilityTreeData.Ability> page : treeData.pages.values()) {
                for (AbilityTreeData.Ability ability : page.values()) {
                    if (ability.name == null) continue;
                    if (ability.name.toLowerCase().contains(search)) {
                        int yStart = y + 75 + ability.coordinates.y * 75 - scrollOffset + (450 * (ability.page - 1));
                        if (yStart - 25 > y && yStart - 25 < y + botLimit) {
                            ui.drawRectBorders(x + ability.coordinates.x * 75 + 943, yStart - 7,
                                    x + ability.coordinates.x * 75 + 943 + 90, yStart - 7 + 90,
                                    CustomColor.fromHexString("FFFF00"));
                        }
                    }
                }
            }
        }

    }

    public void drawNodeTooltip(DrawContext ctx, int mouseX, int mouseY) {
        if (!loaded) return;

        AbilityTreeData treeData = AbilityTreeCache.getClassTree(this.className.toLowerCase());

        if (currentHoveredNode != null && treeData != null) {
            Map<String, AbilityTreeData.Ability> page = treeData.pages.get(currentHoveredNode.meta.page);
            if (page != null) {
                AbilityTreeData.Ability ability = findAbilityMatchForHovered(page, currentHoveredNode);
                if (ability != null && ability.description != null && ability.name != null) {
                    List<String> description = new ArrayList<>(ability.description);
                    description.add(0, ability.name);
                    ctx.drawTooltip(MinecraftClient.getInstance().textRenderer,
                            parseStyledHtml(description), mouseX, mouseY);
                }
            }
        }
    }

    private Identifier connectorTextureFor(AbilityMapData.Node node) {
        if (node.meta == null) return null;
        return switch ((String) node.meta.icon) {
            case "connector_up_down" -> node.unlocked ? verticalActive : vertical;
            case "connector_right_left" -> node.unlocked ? horizontalActive : horizontal;
            case "connector_down_left" -> node.unlocked ? down_leftActive : down_left;
            case "connector_right_down" -> node.unlocked ? right_downActive : right_down;
            case "connector_right_down_left" -> {
                if(!node.unlocked) yield right_down_left;
                else {
                    AbilityMapData.Node leftNeighbour = getNodeAt(node.meta.page, node.coordinates.x - 1, node.coordinates.y);
                    AbilityMapData.Node rightNeighbour = getNodeAt(node.meta.page, node.coordinates.x + 1, node.coordinates.y);
                    AbilityMapData.Node downNeighbour;
                    if(node.coordinates.y % 6 == 0) {
                        downNeighbour = getNodeAt(node.meta.page + 1, node.coordinates.x, node.coordinates.y + 1);
                    } else {
                        downNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y + 1);
                    }

                    if(leftNeighbour == null || rightNeighbour == null || downNeighbour == null) {
                        //System.out.println(node.meta.page);
                        yield RIGHT_DOWN_LEFT;
                    }
                    if(leftNeighbour.unlocked && rightNeighbour.unlocked && downNeighbour.unlocked) yield RIGHT_DOWN_LEFT;
                    else if (leftNeighbour.unlocked && rightNeighbour.unlocked) yield RIGHT_down_LEFT;
                    else if (leftNeighbour.unlocked && downNeighbour.unlocked) yield right_DOWN_LEFT;
                    else if (rightNeighbour.unlocked && downNeighbour.unlocked) yield RIGHT_DOWN_left;
                    else {
                        yield RIGHT_DOWN_LEFT;
                    }
                }
            }
            case "connector_up_right_down" -> {
                if(!node.unlocked) yield up_right_down;
                else {
                    AbilityMapData.Node upNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y - 1);
                    AbilityMapData.Node rightNeighbour = getNodeAt(node.meta.page, node.coordinates.x + 1, node.coordinates.y);
                    AbilityMapData.Node downNeighbour;
                    if(node.coordinates.y % 6 == 0) {
                        downNeighbour = getNodeAt(node.meta.page + 1, node.coordinates.x, node.coordinates.y + 1);
                    } else {
                        downNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y + 1);
                    }

                    if(upNeighbour == null || rightNeighbour == null || downNeighbour == null) {
                        yield UP_RIGHT_DOWN;
                    }
                    if(upNeighbour.unlocked && rightNeighbour.unlocked && downNeighbour.unlocked) yield UP_RIGHT_DOWN;
                    else if (upNeighbour.unlocked && rightNeighbour.unlocked) yield UP_RIGHT_down;
                    else if (upNeighbour.unlocked && downNeighbour.unlocked) yield UP_right_DOWN;
                    else if (rightNeighbour.unlocked && downNeighbour.unlocked) yield up_RIGHT_DOWN;
                    else {
                        yield UP_RIGHT_DOWN;
                    }
                }
            }
            case "connector_up_down_left" -> {
                if(!node.unlocked) yield up_down_left;
                else {
                    AbilityMapData.Node upNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y - 1);
                    AbilityMapData.Node leftNeighbour = getNodeAt(node.meta.page, node.coordinates.x - 1, node.coordinates.y);
                    AbilityMapData.Node downNeighbour;
                    if(node.coordinates.y % 6 == 0) {
                        downNeighbour = getNodeAt(node.meta.page + 1, node.coordinates.x, node.coordinates.y + 1);
                    } else {
                        downNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y + 1);
                    }

                    if(upNeighbour == null || leftNeighbour == null || downNeighbour == null) {
                        yield UP_DOWN_LEFT;
                    }
                    if(upNeighbour.unlocked && leftNeighbour.unlocked && downNeighbour.unlocked) yield UP_DOWN_LEFT;
                    else if (upNeighbour.unlocked && leftNeighbour.unlocked) yield UP_down_LEFT;
                    else if (upNeighbour.unlocked && downNeighbour.unlocked) yield UP_DOWN_left;
                    else if (leftNeighbour.unlocked && downNeighbour.unlocked) yield up_DOWN_LEFT;
                    else {
                        yield UP_DOWN_LEFT;
                    }
                }
            }
            case "connector_up_right_left" -> {
                if(!node.unlocked) yield up_right_left;
                else {
                    AbilityMapData.Node upNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y - 1);
                    AbilityMapData.Node leftNeighbour = getNodeAt(node.meta.page, node.coordinates.x - 1, node.coordinates.y);
                    AbilityMapData.Node rightNeighbour = getNodeAt(node.meta.page, node.coordinates.x + 1, node.coordinates.y);
                    if(upNeighbour == null || leftNeighbour == null || rightNeighbour == null) {
                        yield UP_RIGHT_LEFT;
                    }
                    if(upNeighbour.unlocked && leftNeighbour.unlocked && rightNeighbour.unlocked) yield UP_RIGHT_LEFT;
                    else if (upNeighbour.unlocked && leftNeighbour.unlocked) yield UP_right_LEFT;
                    else if (upNeighbour.unlocked && rightNeighbour.unlocked) yield UP_RIGHT_left;
                    else if (leftNeighbour.unlocked && rightNeighbour.unlocked) yield up_RIGHT_LEFT;
                    else {
                        yield UP_RIGHT_LEFT;
                    }
                }
            }
            case "connector_up_right_down_left" -> {
                if(!node.unlocked) yield up_right_down_left;
                else {
                    AbilityMapData.Node upNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y - 1);
                    AbilityMapData.Node leftNeighbour = getNodeAt(node.meta.page, node.coordinates.x - 1, node.coordinates.y);
                    AbilityMapData.Node rightNeighbour = getNodeAt(node.meta.page, node.coordinates.x + 1, node.coordinates.y);
                    AbilityMapData.Node downNeighbour;
                    if(node.coordinates.y % 6 == 0) {
                        downNeighbour = getNodeAt(node.meta.page + 1, node.coordinates.x, node.coordinates.y + 1);
                    } else {
                        downNeighbour = getNodeAt(node.meta.page, node.coordinates.x, node.coordinates.y + 1);
                    }

                    if(upNeighbour == null || leftNeighbour == null || rightNeighbour == null || downNeighbour == null) {
                        yield UP_RIGHT_DOWN_LEFT;
                    }
                    if(upNeighbour.unlocked && leftNeighbour.unlocked && rightNeighbour.unlocked && downNeighbour.unlocked) yield UP_RIGHT_DOWN_LEFT;
                    else if (upNeighbour.unlocked && leftNeighbour.unlocked && rightNeighbour.unlocked) yield UP_RIGHT_down_LEFT;
                    else if (upNeighbour.unlocked && downNeighbour.unlocked && rightNeighbour.unlocked) yield UP_RIGHT_DOWN_left;
                    else if (leftNeighbour.unlocked && downNeighbour.unlocked && rightNeighbour.unlocked) yield up_RIGHT_DOWN_LEFT;
                    else if (leftNeighbour.unlocked && downNeighbour.unlocked && upNeighbour.unlocked) yield UP_right_DOWN_LEFT;
                    else if (leftNeighbour.unlocked && upNeighbour.unlocked) yield UP_right_down_LEFT;
                    else if (rightNeighbour.unlocked && upNeighbour.unlocked) yield UP_RIGHT_down_left;
                    else if (rightNeighbour.unlocked && downNeighbour.unlocked) yield up_RIGHT_DOWN_left;
                    else if (leftNeighbour.unlocked && downNeighbour.unlocked) yield up_right_DOWN_LEFT;
                    else if (upNeighbour.unlocked && downNeighbour.unlocked) yield UP_right_DOWN_left;
                    else if (leftNeighbour.unlocked && rightNeighbour.unlocked) yield up_RIGHT_down_LEFT;
                    else {
                        yield UP_RIGHT_DOWN_LEFT;
                    }
                }
            }
            default -> null;
        };
    }

    private AbilityTreeData.Ability findAbilityMatchForHovered(Map<String, AbilityTreeData.Ability> page, AbilityMapData.Node node) {
        for (AbilityTreeData.Ability a : page.values()) {
            if (a.coordinates.x == node.coordinates.x &&
                    (a.coordinates.y == (node.coordinates.y % 6) ||
                            ((node.coordinates.y % 6) == 0 && (a.coordinates.y % 6) == 0))) {
                return a;
            }
        }
        return null;
    }

    // Konsolidierter interner Zustand
    private class AbilityTreeState {
        AbilityMapData classTree;
        AbilityMapData playerTree;
        final Set<String> unlockedIds = new HashSet<>();
        final Set<Pair<Integer, Integer>> connectorCoordinates = new HashSet<>();
        final List<AbilityMapData.Node> abilities = new ArrayList<>();
        final List<AbilityMapData.Node> connectors = new ArrayList<>();

        void reset() {
            classTree = null;
            playerTree = null;
            unlockedIds.clear();
            connectorCoordinates.clear();
            abilities.clear();
            connectors.clear();
        }

        void prepare(AbilityMapData classTree, AbilityMapData playerTree) {
            this.classTree = classTree;
            this.playerTree = playerTree;
            // playerMap -> unlocked sets
            for (List<AbilityMapData.Node> nodes : this.playerTree.pages.values()) {
                for (AbilityMapData.Node node : nodes) {
                    if (node.meta != null) {
                        if ("ability".equals(node.type) && node.meta.id != null) {
                            unlockedIds.add(node.meta.id);
                        } else if ("connector".equals(node.type)) {
                            connectorCoordinates.add(new Pair<>(node.coordinates.x, node.coordinates.y));
                        }
                    }
                }
            }
            // classTree -> set unlocked flags and split lists
            int i = 0;
            for (List<AbilityMapData.Node> nodes : this.classTree.pages.values()) {
                int yStart = 0;
                for (AbilityMapData.Node node : nodes) {
                    yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
                    if (node.meta != null) {
                        if ("ability".equals(node.type) && node.meta.id != null) {
                            node.unlocked = unlockedIds.contains(node.meta.id);
                        } else {
                            Pair<Integer, Integer> coords = new Pair<>(node.coordinates.x, node.coordinates.y);
                            node.unlocked = connectorCoordinates.contains(coords);
                        }
                    }
                    if ("ability".equals(node.type)) abilities.add(node);
                    else connectors.add(node);
                }
                i++;
                if(ui == null) return;
                if(i != 7 && yStart + 75 > y && yStart + 75 < y + botLimit) {
                    ui.drawImage(pageLineTexture, x + 1000, yStart + 75, 730, 32);
                    if(i == 1 && yStart - 400 > y && yStart - 400 < y + botLimit) {
                        ui.drawText(String.valueOf(i), x + 1000, yStart - 400, CustomColor.fromHexString("434654"));
                    }
                    ui.drawText(String.valueOf(i + 1), x + 1000, yStart + 110, CustomColor.fromHexString("434654"));
                }
            }
        }
    }

    public static class NodeWidget extends Widget {
        AbilityMapData.Node node;
        int x;
        int y;
        int botLimit;
        int scrollOffset;

        public NodeWidget(int x, int y, AbilityMapData.Node node, int botLimit, int scrollOffset) {
            super(0, 0, 75, 75);
            this.node = node;
            this.x = x;
            this.y = y;
            this.botLimit = botLimit;
            this.scrollOffset = scrollOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int xStart = x + node.coordinates.x * 75 + 925;
            int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;

            setBounds(xStart + 25, yStart, 75, 75);

            Identifier texture = null;
            switch (((AbilityMapData.Icon.IconValue) ((AbilityMapData.Icon) node.meta.icon).value).name) {
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
            if(texture != null && yStart - 25 > y && yStart - 25 < y + botLimit) {
                ui.drawImage(texture, xStart, yStart - 25, 125, 125);
                if(contains(mouseX, mouseY) && mouseY < ui.sy(y + botLimit + 25) && mouseY > ui.sy(y + 100)) {
                    currentHoveredNode = node;
                }
            }
        }
    }
}

