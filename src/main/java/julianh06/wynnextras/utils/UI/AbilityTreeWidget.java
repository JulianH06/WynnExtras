package julianh06.wynnextras.utils.UI;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.AbilityMapData;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeCache;
import julianh06.wynnextras.features.profileviewer.data.AbilityTreeData;
import julianh06.wynnextras.utils.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.BiConsumer;

import static julianh06.wynnextras.utils.WynncraftApiHandler.parseStyledHtml;

public class AbilityTreeWidget extends Widget {
    private static final int SEARCH_OVERLAY_PADDING = 4;
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

    public static final Identifier ultimateNodeBase = Identifier.of("wynnextras", "textures/gui/profileviewer/node/node.png");
    public static final Identifier ultimateNodeSelected = Identifier.of("wynnextras", "textures/gui/profileviewer/node/selected.png");

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
    public static final Identifier RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active.png");
    public static final Identifier right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active1.png");
    public static final Identifier RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active2.png");
    public static final Identifier RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/right_down_left_active3.png");

    public static final Identifier up_right_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down.png");
    public static final Identifier UP_RIGHT_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active.png");
    public static final Identifier UP_RIGHT_down = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active1.png");
    public static final Identifier up_RIGHT_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active2.png");
    public static final Identifier UP_right_DOWN = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_active3.png");

    public static final Identifier up_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left.png");
    public static final Identifier UP_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active.png");
    public static final Identifier UP_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active1.png");
    public static final Identifier up_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active2.png");
    public static final Identifier UP_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_down_left_active3.png");

    public static final Identifier up_right_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left.png");
    public static final Identifier UP_RIGHT_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active.png");
    public static final Identifier UP_right_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active1.png");
    public static final Identifier UP_RIGHT_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active2.png");
    public static final Identifier up_RIGHT_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_left_active3.png");

    public static final Identifier up_right_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left.png");
    public static final Identifier UP_RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active.png");
    public static final Identifier UP_RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active1.png");
    public static final Identifier UP_RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active2.png");
    public static final Identifier up_RIGHT_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active3.png");
    public static final Identifier UP_right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active4.png");
    public static final Identifier UP_right_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active5.png");
    public static final Identifier UP_RIGHT_down_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active6.png");
    public static final Identifier up_RIGHT_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active7.png");
    public static final Identifier up_right_DOWN_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active8.png");
    public static final Identifier UP_right_DOWN_left = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active9.png");
    public static final Identifier up_RIGHT_down_LEFT = Identifier.of("wynnextras", "textures/gui/profileviewer/connector/up_right_down_left_active10.png");

    private String className;

    private int scrollOffset;

    private AbilityMapData.Node currentHoveredNode;
    private boolean loaded;

    public AbilityMapData classTree;
    public AbilityMapData playerTree;

    private final AbilityTreeState state = new AbilityTreeState();

    private final List<NodeWidget> nodeWidgets = new ArrayList<>();

    private String searchInput = "";
    private Set<String> searchIds = Set.of();
    private boolean searchActive;
    private Set<String> selectedIds;
    private Set<String> warningIds = Set.of();
    private Set<String> errorIds = Set.of();
    private BiConsumer<String, Boolean> nodeClickListener;
    private boolean draggingNodes;
    private boolean dragNodeState;
    private final Set<String> draggedNodeIds = new HashSet<>();

    int botLimit;
    private boolean clipEnabled;
    private int clipX;
    private int clipY;
    private int clipWidth;
    private int clipHeight;

    public AbilityTreeWidget(String className, int x, int y, int width, int height, int botLimit) {
        super(x, y, width, height);
        this.className = normalizeClassName(className);
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

    public void setClassName(String className) {
        String normalized = normalizeClassName(className);
        if (this.className.equals(normalized)) return;

        this.className = normalized;
        this.classTree = null;
        currentHoveredNode = null;
        refreshState();
    }

    public String getClassName() {
        return className;
    }

    public void setPlayerTree(AbilityMapData playerTree) {
        this.playerTree = playerTree;
        this.selectedIds = null;
        refreshState();
    }

    public void setSelectedIds(Set<String> selectedIds) {
        Set<String> copy = selectedIds == null ? null : Set.copyOf(selectedIds);
        if (Objects.equals(this.selectedIds, copy)) return;
        this.selectedIds = copy;
        refreshState();
    }

    public void setNodeClickListener(BiConsumer<String, Boolean> listener) {
        this.nodeClickListener = listener;
    }

    private boolean beginNodeDrag(AbilityMapData.Node node) {
        if (node.meta == null || node.meta.id == null || nodeClickListener == null) return false;
        draggingNodes = true;
        dragNodeState = !node.unlocked;
        draggedNodeIds.clear();
        applyDraggedNode(node);
        return true;
    }

    private void applyDraggedNode(AbilityMapData.Node node) {
        if (node.meta == null || node.meta.id == null || !draggedNodeIds.add(node.meta.id)) return;
        nodeClickListener.accept(node.meta.id, dragNodeState);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!insideClip(mouseX, mouseY)) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || !draggingNodes) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        if (!insideClip(mouseX, mouseY)) return true;
        for (NodeWidget widget : nodeWidgets) {
            if (widget.contains((int) mouseX, (int) mouseY)) {
                applyDraggedNode(widget.node);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingNodes) {
            draggingNodes = false;
            draggedNodeIds.clear();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean insideClip(double mouseX, double mouseY) {
        if (!clipEnabled || ui == null) return true;
        float left = ui.sx(clipX);
        float top = ui.sy(clipY);
        return mouseX >= left && mouseX < left + ui.sw(clipWidth)
                && mouseY >= top && mouseY < top + ui.sh(clipHeight);
    }

    public void setViewport(int x, int y, int width, int height, int botLimit) {
        if (this.x == x && this.y == y && this.width == width && this.height == height
                && this.botLimit == botLimit) return;
        setBounds(x, y, width, height);
        this.botLimit = botLimit;
        refreshState();
    }

    public void clearTrees() {
        this.classTree = null;
        this.playerTree = null;
        refreshState();
    }

    public void setScrollOffset(int scrollOffset) {
        if (this.scrollOffset == scrollOffset) return;

        this.scrollOffset = scrollOffset;
        refreshState();
    }

    public void setClipBounds(int x, int y, int width, int height) {
        clipEnabled = true;
        clipX = x;
        clipY = y;
        clipWidth = Math.max(0, width);
        clipHeight = Math.max(0, height);
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

        if (this.classTree == null || (this.playerTree == null && this.selectedIds == null)) {
            loaded = false;
            return;
        }

        state.prepare(this.classTree, this.playerTree, this.selectedIds);
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

    public void setSearchIds(Set<String> searchIds) {
        this.searchIds = searchIds == null ? Set.of() : Set.copyOf(searchIds);
    }

    public void setSearchActive(boolean searchActive) {
        this.searchActive = searchActive;
    }

    public void setHighlightedIds(Set<String> warningIds, Set<String> errorIds) {
        this.warningIds = warningIds == null ? Set.of() : Set.copyOf(warningIds);
        this.errorIds = errorIds == null ? Set.of() : Set.copyOf(errorIds);
    }

    public AbilityMapData.Node getCurrentHoveredNode() {
        return currentHoveredNode;
    }

    @Override
    public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
        if (!clipEnabled) {
            super.draw(ctx, mouseX, mouseY, tickDelta, ui);
            return;
        }
        this.ui = ui;
        if (!visible || this.ui == null) return;
        hovered = contains(mouseX, mouseY);
        updateValues();
        ui.enableScissor(clipX, clipY, clipX + clipWidth, clipY + clipHeight);
        try {
            drawBackground(ctx, mouseX, mouseY, tickDelta);
            drawContent(ctx, mouseX, mouseY, tickDelta);
            for (Widget child : children) {
                child.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
            drawForeground(ctx, mouseX, mouseY, tickDelta);
        } finally {
            ui.disableScissor();
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        currentHoveredNode = null;
        if (!loaded || state.classTree == null) return;

        drawPageDividers();

        for (AbilityMapData.Node node : state.connectors) {
            int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
            Identifier tex = connectorTextureFor(node);
            if (tex != null && yStart + 111 > y && yStart - 34 < y + botLimit) {
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

    private void drawPageDividers() {
        int page = 0;
        for (List<AbilityMapData.Node> nodes : state.classTree.pages.values()) {
            int yStart = nodes.stream()
                    .mapToInt(node -> y + 75 + node.coordinates.y * 75 - scrollOffset)
                    .max().orElse(y);
            page++;
            if (page >= state.classTree.pages.size()
                    || yStart + 125 <= y
                    || yStart + 75 >= y + botLimit) {
                continue;
            }
            ui.drawImage(pageLineTexture, x + 1000, yStart + 75, 730, 32);
            ui.drawText(String.valueOf(page + 1), x + 1000, yStart + 110,
                    CustomColor.fromHexString("434654"));
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (!loaded) return;

        if (searchActive && clipEnabled) drawSearchOverlay(ctx, mouseX, mouseY, tickDelta);

        if (!searchIds.isEmpty()) {
            for (AbilityMapData.Node node : state.abilities) {
                if (node.meta == null || node.meta.id == null || !searchIds.contains(node.meta.id)) continue;
                int xStart = x + node.coordinates.x * 75 + 925;
                int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
                if (yStart + 88 > y && yStart - 12 < y + botLimit) {
                    ui.drawRectBorders(xStart + 13, yStart - 12, 100, 100,
                            CustomColor.fromHexString("FFFF00"));
                }
            }
        }

        AbilityTreeData treeData = AbilityTreeCache.getClassTree(this.className);
        if (treeData != null && treeData.pages != null && searchInput != null && !searchInput.isEmpty()) {
            String search = normalizedSearchText(searchInput);
            if (!search.isEmpty()) {
                for (AbilityMapData.Node node : state.abilities) {
                    if (node.meta == null || node.meta.id == null) continue;
                    Map<String, AbilityTreeData.Ability> page = treeData.pages.get(node.meta.page);
                    if (page == null) continue;
                    AbilityTreeData.Ability ability = findAbilityMatchForHovered(page, node);
                    if (ability == null || ability.name == null
                            || !normalizedSearchText(ability.name).contains(search)) continue;
                    int xStart = x + node.coordinates.x * 75 + 925;
                    int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
                    if (yStart + 88 > y && yStart - 12 < y + botLimit) {
                        ui.drawRectBorders(xStart + 13, yStart - 12, 100, 100,
                                CustomColor.fromHexString("FFFF00"));
                    }
                }
            }
        }

        if (!warningIds.isEmpty() || !errorIds.isEmpty()) {
            for (AbilityMapData.Node node : state.abilities) {
                if (node.meta == null || node.meta.id == null) continue;
                boolean warning = warningIds.contains(node.meta.id);
                boolean error = errorIds.contains(node.meta.id);
                if (!warning && !error) continue;
                int xStart = x + node.coordinates.x * 75 + 925;
                int yStart = y + 75 + node.coordinates.y * 75 - scrollOffset;
                if (yStart + 84 <= y || yStart - 8 >= y + botLimit) continue;
                if (warning) {
                    ui.drawRectBorders(xStart + 17, yStart - 8, 92, 92,
                            CustomColor.fromHexString("FFAA00"));
                }
                if (error) {
                    ui.drawRectBorders(xStart + 22, yStart - 3, 82, 82,
                            CustomColor.fromHexString("FF5555"));
                }
            }
        }

    }

    private void drawSearchOverlay(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ui.disableScissor();
        ui.drawRect(
                clipX - SEARCH_OVERLAY_PADDING,
                clipY - SEARCH_OVERLAY_PADDING,
                clipWidth + SEARCH_OVERLAY_PADDING * 2,
                clipHeight + SEARCH_OVERLAY_PADDING * 2,
                CustomColor.fromInt(0x88000000));
        ui.enableScissor(clipX, clipY, clipX + clipWidth, clipY + clipHeight);

        for (NodeWidget nodeWidget : nodeWidgets) {
            if (nodeWidget.node.meta != null && nodeWidget.node.meta.id != null
                    && searchIds.contains(nodeWidget.node.meta.id)) {
                nodeWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
        }
    }

    private String normalizedSearchText(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)§[0-9A-FK-ORX]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    public void drawNodeTooltip(DrawContext ctx, int mouseX, int mouseY) {
        if (!loaded) return;

        AbilityTreeData treeData = AbilityTreeCache.getClassTree(this.className);

        if (currentHoveredNode == null || currentHoveredNode.meta == null || treeData == null || treeData.pages == null) return;

        Map<String, AbilityTreeData.Ability> page = treeData.pages.get(currentHoveredNode.meta.page);
        if (page == null) return;

        AbilityTreeData.Ability ability = findAbilityMatchForHovered(page, currentHoveredNode);
        if (ability == null || ability.name == null) return;

        List<String> description = new ArrayList<>();
        description.add(0, ability.name);
        if (ability.description != null) {
            description.addAll(ability.description);
        }
        int tx = (int)(mouseX * PVScreen.currentMatrixScale);
        int ty = (int)(mouseY * PVScreen.currentMatrixScale);
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer,
                parseStyledHtml(description), tx, ty);
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
        if (node.meta != null && node.meta.id != null) {
            AbilityTreeData.Ability ability = page.get(node.meta.id);
            if (ability != null) return ability;
        }

        for (AbilityTreeData.Ability a : page.values()) {
            if (a.coordinates != null && a.coordinates.x == node.coordinates.x &&
                    (a.coordinates.y == (node.coordinates.y % 6) ||
                            ((node.coordinates.y % 6) == 0 && (a.coordinates.y % 6) == 0))) {
                return a;
            }
        }
        return null;
    }

    private String normalizeClassName(String className) {
        return className == null ? "" : className.toLowerCase();
    }

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

        void prepare(AbilityMapData classTree, AbilityMapData playerTree, Set<String> selectedIds) {
            this.classTree = classTree;
            this.playerTree = playerTree;
            if (selectedIds != null) {
                unlockedIds.addAll(selectedIds);
            } else {
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
            }
            for (List<AbilityMapData.Node> nodes : this.classTree.pages.values()) {
                for (AbilityMapData.Node node : nodes) {
                    if (node.meta != null) {
                        if ("ability".equals(node.type) && node.meta.id != null) {
                            node.unlocked = unlockedIds.contains(node.meta.id);
                        } else {
                            Pair<Integer, Integer> coords = new Pair<>(node.coordinates.x, node.coordinates.y);
                            node.unlocked = selectedIds == null
                                    ? connectorCoordinates.contains(coords)
                                    : node.meta.paths != null && node.meta.paths.stream().anyMatch(path ->
                                            path.size() >= 2 && unlockedIds.contains(path.get(0))
                                                    && unlockedIds.contains(path.get(1)));
                        }
                    }
                    if ("ability".equals(node.type)) abilities.add(node);
                    else connectors.add(node);
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
            Identifier overlay = null;
            String iconName = ((AbilityMapData.Icon.IconValue) ((AbilityMapData.Icon) node.meta.icon).value).name;
            switch (iconName) {
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
                default -> {
                    if (iconName.startsWith("abilityTree.ultimate")) {
                        String suffix = iconName.substring("abilityTree.ultimate".length());
                        String snakeName = suffix.replaceAll("([A-Z])", "_$1").toLowerCase().replaceFirst("^_", "");
                        String cn = parent instanceof AbilityTreeWidget atw ? atw.className.toLowerCase() : "";
                        texture = node.unlocked ? ultimateNodeSelected : ultimateNodeBase;
                        String overlayFile = node.unlocked ? snakeName + "_selected" : snakeName;
                        overlay = Identifier.of("wynnextras", "textures/gui/profileviewer/node/" + cn + "/" + overlayFile + ".png");
                    } else {
                        System.out.println("[WynnExtras] Unknown ability node icon: " + iconName);
                    }
                }
            }
            if(texture != null && yStart + 100 > y && yStart - 25 < y + botLimit) {
                ui.drawImage(texture, xStart, yStart - 25, 125, 125);
                if(overlay != null) {
                    ui.drawImage(overlay, xStart, yStart - 25, 125, 125);
                }
                if(contains(mouseX, mouseY)
                        && mouseY < ui.sy(y + botLimit)
                        && mouseY >= ui.sy(y)) {
                    if (parent instanceof AbilityTreeWidget tree) tree.currentHoveredNode = node;
                }
            }
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0 || node.meta == null || node.meta.id == null
                    || !(parent instanceof AbilityTreeWidget tree) || tree.nodeClickListener == null) {
                return false;
            }
            return tree.beginNodeDrag(node);
        }
    }
}
