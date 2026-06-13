package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.waypoints.old.WaypointCategory;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static julianh06.wynnextras.features.waypoints.WaypointEditMode.*;

public class WaypointEditModeUI extends WEScreen {
    private enum VisibilityTarget { NAME, BLOCK, DISTANCE }

    private final ClickAreaWidget panelBlocker = new ClickAreaWidget(() -> true, this::clearUiFocus);
    private final DropdownFieldWidget packageField = new DropdownFieldWidget(this, Dropdown.PACKAGE);
    private final DropdownFieldWidget categoryField = new DropdownFieldWidget(this, Dropdown.CATEGORY);
    private final CoordinateInputWidget xCoordinateField = new CoordinateInputWidget(this, 0, "X");
    private final CoordinateInputWidget yCoordinateField = new CoordinateInputWidget(this, 1, "Y");
    private final CoordinateInputWidget zCoordinateField = new CoordinateInputWidget(this, 2, "Z");
    private final NameInputWidget nameField = new NameInputWidget(this);
    private final ActionButtonWidget freeMoveButton = new ActionButtonWidget(this, () -> "Free Move Mode", WaypointEditMode::enterFreeMoveMode);
    private final ActionButtonWidget removeButton = new ActionButtonWidget(this, () -> "Remove Waypoint", this::handleRemove);
    private final ActionButtonWidget editCurrentButton = new ActionButtonWidget(this, () -> "Edit Current", this::handleEditCurrent);
    private final ActionButtonWidget primaryButton = new ActionButtonWidget(this, () -> selectedWaypoint == null ? "Add Waypoint" : "Save Changes", this::handlePrimaryAction);
    private final ActionButtonWidget secondaryButton = new ActionButtonWidget(this, () -> selectedWaypoint == null ? "Exit Waypoint Edit Mode" : "Discard Changes", this::handleSecondaryAction);
    private final ActionButtonWidget showNameButton = new ActionButtonWidget(this, () -> visibilityLabel("Text", selectedWaypoint == null ? null : selectedWaypoint.showNameOverride, selectedWaypoint == null || selectedWaypoint.shouldShowName()), () -> toggleVisibility(VisibilityTarget.NAME, false), () -> toggleVisibility(VisibilityTarget.NAME, true));
    private final ActionButtonWidget showBlockButton = new ActionButtonWidget(this, () -> visibilityLabel("Block", selectedWaypoint == null ? null : selectedWaypoint.showOverride, selectedWaypoint == null || selectedWaypoint.shouldShowBlock()), () -> toggleVisibility(VisibilityTarget.BLOCK, false), () -> toggleVisibility(VisibilityTarget.BLOCK, true));
    private final ActionButtonWidget showDistanceButton = new ActionButtonWidget(this, () -> visibilityLabel("Distance", selectedWaypoint == null ? null : selectedWaypoint.showDistanceOverride, selectedWaypoint == null || selectedWaypoint.shouldShowDistance()), () -> toggleVisibility(VisibilityTarget.DISTANCE, false), () -> toggleVisibility(VisibilityTarget.DISTANCE, true));
    private final CategoryNameInputWidget categoryNameField = new CategoryNameInputWidget(this);
    private final ActionButtonWidget categoryShowNameButton = new ActionButtonWidget(this, () -> categoryDefaultLabel("Text", activeCategory == null || activeCategory.showNameByDefault), () -> toggleCategoryDefault(VisibilityTarget.NAME));
    private final ActionButtonWidget categoryShowBlockButton = new ActionButtonWidget(this, () -> categoryDefaultLabel("Block", activeCategory == null || activeCategory.showBlockByDefault), () -> toggleCategoryDefault(VisibilityTarget.BLOCK));
    private final ActionButtonWidget categoryShowDistanceButton = new ActionButtonWidget(this, () -> categoryDefaultLabel("Distance", activeCategory == null || activeCategory.showDistanceByDefault), () -> toggleCategoryDefault(VisibilityTarget.DISTANCE));
    private final CategoryColorPickerWidget categoryColorPicker = new CategoryColorPickerWidget(this);
    private final InfoButtonWidget infoButton = new InfoButtonWidget(this);
    private final DropdownWidget dropdownWidget = new DropdownWidget(this);
    private boolean dropdownScrollbarDragging = false;
    private Dropdown dropdownScrollbarDragTarget = Dropdown.NONE;
    private float dropdownScrollbarDragOffset = 0;
    private float packageScrollTarget = packageScroll;
    private float categoryScrollTarget = categoryScroll;
    private float waypointScrollTarget = waypointScroll;
    private DropdownFieldWidget activeDropdownField = null;
    private int waypointDropdownX = 0;
    private int waypointDropdownY = 0;
    private int waypointDropdownW = 0;
    private WaypointEditMode.WaypointPositionStats lastPreviewStats = new WaypointEditMode.WaypointPositionStats(0, List.of(), List.of());
    private julianh06.wynnextras.features.waypoints.old.Waypoint nameInputWaypoint = null;
    private String nameInput = "";
    private boolean nameFocused = false;
    private WaypointCategory categoryNameInputCategory = null;
    private String categoryNameInput = "";
    private boolean categoryNameFocused = false;

    WaypointEditModeUI() {
        super(Text.literal("Waypoint Edit Mode"));
        addRootWidget(panelBlocker);
        addRootWidget(packageField);
        addRootWidget(categoryField);
        addRootWidget(xCoordinateField);
        addRootWidget(yCoordinateField);
        addRootWidget(zCoordinateField);
        addRootWidget(nameField);
        addRootWidget(freeMoveButton);
        addRootWidget(removeButton);
        addRootWidget(editCurrentButton);
        addRootWidget(primaryButton);
        addRootWidget(secondaryButton);
        addRootWidget(showNameButton);
        addRootWidget(showBlockButton);
        addRootWidget(showDistanceButton);
        addRootWidget(categoryNameField);
        addRootWidget(categoryShowNameButton);
        addRootWidget(categoryShowBlockButton);
        addRootWidget(categoryShowDistanceButton);
        addRootWidget(categoryColorPicker);
        addRootWidget(infoButton);
        addRootWidget(dropdownWidget);
    }

    @Override
    protected double getTargetScaleFactor() {
        return 2;
    }

    @Override
    protected int getMinLogicalWidth() {
        return 1600;
    }

    @Override
    protected int getMinLogicalHeight() {
        return 720;
    }

    @Override
    protected boolean shouldRenderBlur() {
        return false;
    }

    @Override
    protected boolean shouldRenderBackground() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ensureSelectionDefaults();
        syncNameInput();
        syncCategoryNameInput();
        syncCoordinateWidgets();
        lastPreviewStats = statsAt(previewPos);
        updateDropdownScroll(delta);
        updateEditorWidgets();
        updateHoveredWaypoint(mouseX, mouseY);
        drawEditHud();
        drawPanel(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int button = click.button();

        updateEditorWidgets();
        if (super.mouseClicked(click, doubleClick)) return true;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;

        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        int screenMx = (int) click.x();
        int screenMy = (int) click.y();
        if (handleWaypointClick(screenMx, screenMy, (int) (click.x() / getMatrixScale()), (int) (click.y() / getMatrixScale()))
                && client != null && client.player != null) {
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        updateEditorWidgets();
        if (dropdownWidget.mouseScrolled(mouseX / getMatrixScale(), mouseY / getMatrixScale(), verticalAmount)) return true;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        updateEditorWidgets();
        if (dropdownWidget.mouseDragged(click.x() / getMatrixScale(), click.y() / getMatrixScale(), click.button(), dx, dy)) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (selectedWaypoint != null && key == GLFW.GLFW_KEY_ESCAPE) {
            saveEditedWaypoint();
            return true;
        }
        if (categoryNameField.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                applyCategoryNameInput();
                setFocusedWidget(null);
                return true;
            }
            categoryNameField.keyPressed(key, input.scancode(), input.modifiers());
            return true;
        }
        if (nameField.isFocused()) {
            nameField.keyPressed(key, input.scancode(), input.modifiers());
            return true;
        }

        if (selectedWaypoint != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                saveEditedWaypoint();
                return true;
            }
        }

        if (focusedCoordinate >= 0) {
            CoordinateInputWidget coordinateField = currentCoordinateField();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                applyCoordinateInputs();
                focusedCoordinate = -1;
                setFocusedWidget(null);
                syncCoordinateInputs();
                return true;
            }
            if (coordinateField != null && coordinateField.keyPressed(key, input.scancode(), input.modifiers())) return true;
            return true;
        }

        if (searchFocused && activeDropdown != Dropdown.NONE) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                activeDropdown = Dropdown.NONE;
                activeDropdownField = null;
                searchFocused = false;
                return true;
            }
            if (dropdownWidget.keyPressed(key, input.scancode(), input.modifiers())) return true;
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            exit();
            return true;
        }
        if (matchesKey(key, WynnExtrasConfig.INSTANCE.waypointEditReturnKey)) {
            enterFreeMoveMode();
            return true;
        }
        if (selectedWaypoint == null && matchesKey(key, WynnExtrasConfig.INSTANCE.waypointEditAddKey)) {
            addWaypoint();
            return true;
        }
        if (matchesKey(key, WynnExtrasConfig.INSTANCE.waypointEditRemoveKey)) {
            removeSelectedWaypoint();
            return true;
        }
        if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_D) {
            movePreviewHorizontal(key);
            return true;
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            movePreviewVertical(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            movePreviewVertical(-1);
            return true;
        }
        return true;
    }

    private boolean isWaypointMovementKey(int key) {
        return key == GLFW.GLFW_KEY_W
                || key == GLFW.GLFW_KEY_A
                || key == GLFW.GLFW_KEY_S
                || key == GLFW.GLFW_KEY_D
                || key == GLFW.GLFW_KEY_SPACE
                || key == GLFW.GLFW_KEY_LEFT_SHIFT
                || key == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (nameField.isFocused()) {
            nameField.charTyped((char) input.codepoint(), input.modifiers());
            return true;
        }
        if (categoryNameField.isFocused()) {
            categoryNameField.charTyped((char) input.codepoint(), input.modifiers());
            return true;
        }
        if (focusedCoordinate >= 0) {
            CoordinateInputWidget coordinateField = currentCoordinateField();
            if (coordinateField != null) coordinateField.charTyped((char) input.codepoint(), input.modifiers());
            return true;
        }
        if (!searchFocused || activeDropdown == Dropdown.NONE) return true;
        dropdownWidget.charTyped((char) input.codepoint(), input.modifiers());
        return true;
    }

    @Override
    public void close() {
        applyCategoryNameInput();
        if (closingForFreeMove) {
            closingForFreeMove = false;
            return;
        }
        exit();
    }

    @Override
    public void removed() {
        super.removed();
        applyCategoryNameInput();
        if (closingForFreeMove) {
            closingForFreeMove = false;
            return;
        }
        if (enabled && mode == Mode.EDIT) {
            enabled = false;
            activeDropdown = Dropdown.NONE;
            activeDropdownField = null;
            searchFocused = false;
            focusedCoordinate = -1;
            selectedWaypoint = null;
            selectedWaypointPackage = null;
            selectedSnapshot = null;
        }
    }

    private int p(int screenPixels) {
        return screenPixels;
    }

    private float ts(float scale) {
        return scale;
    }

    private boolean matchesKey(int key, int configuredKey) {
        if (configuredKey == GLFW.GLFW_KEY_UNKNOWN) return false;
        if (key == configuredKey) return true;
        return configuredKey == GLFW.GLFW_KEY_ENTER && key == GLFW.GLFW_KEY_KP_ENTER;
    }

    private void updateEditorWidgets() {
        int panelW = p(PANEL_W);
        int panelX = getLogicalWidth() - panelW - p(12);
        int panelY = p(12);
        setWidgetBounds(panelBlocker, 0, 0, getLogicalWidth(), getLogicalHeight());
        infoButton.setLogicalBounds(panelX + panelW - p(55), panelY + p(25), textLogicalWidth("[?]", 2.5f), p(18));

        boolean editing = selectedWaypoint != null;
        boolean hasCategory = activeCategory != null;
        nameField.setVisible(editing);
        categoryField.setVisible(true);
        removeButton.setVisible(true);
        primaryButton.setVisible(true);
        secondaryButton.setVisible(true);
        editCurrentButton.setVisible(!editing);
        showNameButton.setVisible(editing);
        showBlockButton.setVisible(editing);
        showDistanceButton.setVisible(editing);
        categoryNameField.setVisible(hasCategory);
        categoryShowNameButton.setVisible(hasCategory);
        categoryShowBlockButton.setVisible(hasCategory);
        categoryShowDistanceButton.setVisible(hasCategory);
        categoryColorPicker.setVisible(hasCategory);

        int coordX = panelX + p(14);
        int coordW = panelW - p(28);
        int coordGap = p(8);
        int fieldW = (coordW - coordGap * 2) / 3;

        if (editing) {
            packageField.setLogicalBounds(panelX + p(14), panelY + p(80), panelW - p(28), p(FIELD_H));
            categoryField.setLogicalBounds(panelX + p(14), panelY + p(160), panelW - p(28), p(FIELD_H));
            nameField.setLogicalBounds(panelX + p(14), panelY + p(250), panelW - p(28), p(38));
            int coordY = panelY + p(332);
            xCoordinateField.setLogicalBounds(coordX, coordY, fieldW, p(32));
            yCoordinateField.setLogicalBounds(coordX + fieldW + coordGap, coordY, fieldW, p(32));
            zCoordinateField.setLogicalBounds(coordX + (fieldW + coordGap) * 2, coordY, fieldW, p(32));
            freeMoveButton.setLogicalBounds(panelX + p(14), panelY + p(380), panelW - p(28), p(50));
            int toggleY = panelY + p(480);
            int toggleW = (panelW - p(28) - p(16)) / 3;
            showNameButton.setLogicalBounds(panelX + p(14), toggleY, toggleW, p(58));
            showBlockButton.setLogicalBounds(panelX + p(14) + toggleW + p(8), toggleY, toggleW, p(58));
            showDistanceButton.setLogicalBounds(panelX + p(14) + (toggleW + p(8)) * 2, toggleY, toggleW, p(58));
            int buttonY = panelY + p(550);
            int buttonW = (panelW - p(28) - p(20)) / 3;
            removeButton.setLogicalBounds(panelX + p(14), buttonY, buttonW, p(ACTION_H));
            primaryButton.setLogicalBounds(panelX + p(14) + buttonW + p(10), buttonY, buttonW, p(ACTION_H));
            secondaryButton.setLogicalBounds(panelX + p(14) + (buttonW + p(10)) * 2, buttonY, buttonW, p(ACTION_H));
        } else {
            nameFocused = false;
            nameField.setFocused(false);
            packageField.setLogicalBounds(panelX + p(14), panelY + p(80), panelW - p(28), p(FIELD_H));
            categoryField.setLogicalBounds(panelX + p(14), panelY + p(160), panelW - p(28), p(FIELD_H));
            int coordY = panelY + p(252);
            xCoordinateField.setLogicalBounds(coordX, coordY, fieldW, p(32));
            yCoordinateField.setLogicalBounds(coordX + fieldW + coordGap, coordY, fieldW, p(32));
            zCoordinateField.setLogicalBounds(coordX + (fieldW + coordGap) * 2, coordY, fieldW, p(32));
            freeMoveButton.setLogicalBounds(panelX + p(14), panelY + p(290), panelW - p(28), p(50));
            int actionsX = getLogicalWidth() - p(1128);
            int actionsY = getLogicalHeight() - p(58);
            int actionW = p(265);
            int actionGap = p(14);
            removeButton.setLogicalBounds(actionsX, actionsY, actionW, p(ACTION_H));
            editCurrentButton.setLogicalBounds(actionsX + actionW + actionGap, actionsY, actionW, p(ACTION_H));
            primaryButton.setLogicalBounds(actionsX + (actionW + actionGap) * 2, actionsY, actionW, p(ACTION_H));
            secondaryButton.setLogicalBounds(actionsX + (actionW + actionGap) * 3, actionsY, actionW, p(ACTION_H));
        }

        if (activeDropdown == Dropdown.NONE) {
            dropdownScrollbarDragging = false;
            dropdownScrollbarDragTarget = Dropdown.NONE;
            dropdownWidget.setVisible(false);
            dropdownWidget.setBounds(0, 0, 0, 0);
            dropdownWidget.clearChildren();
            activeDropdownField = null;
        } else if (activeDropdown == Dropdown.WAYPOINT) {
            int dropdownW = waypointDropdownW <= 0 ? p(430) : waypointDropdownW;
            int dropdownX = MathHelper.clamp(waypointDropdownX, p(8), Math.max(p(8), getLogicalWidth() - dropdownW - p(8)));
            int dropdownY = MathHelper.clamp(waypointDropdownY, p(8), Math.max(p(8), getLogicalHeight() - p(260)));
            dropdownWidget.configure(activeDropdown, dropdownX, dropdownY, dropdownW);
        } else {
            Widget field = activeDropdownField != null ? activeDropdownField : activeDropdown == Dropdown.PACKAGE ? packageField : categoryField;
            int dropdownX = field.getX();
            int dropdownY = field.getY() + field.getHeight();
            int dropdownW = field.getWidth();
            dropdownWidget.configure(activeDropdown, dropdownX, dropdownY, dropdownW);
        }

        int categoryPanelX = panelX;
        int categoryPanelY = panelY + editorPanelHeight() + p(PANEL_GAP);
        if (hasCategory) {
            categoryNameField.setLogicalBounds(categoryPanelX + p(14), categoryPanelY + p(68), panelW - p(28), p(38));
            int toggleY = categoryPanelY + p(142);
            int toggleW = (panelW - p(28) - p(16)) / 3;
            categoryShowNameButton.setLogicalBounds(categoryPanelX + p(14), toggleY, toggleW, p(48));
            categoryShowBlockButton.setLogicalBounds(categoryPanelX + p(14) + toggleW + p(8), toggleY, toggleW, p(48));
            categoryShowDistanceButton.setLogicalBounds(categoryPanelX + p(14) + (toggleW + p(8)) * 2, toggleY, toggleW, p(48));
            categoryColorPicker.setLogicalBounds(categoryPanelX + p(14), categoryPanelY + p(210), panelW - p(28), p(32));
        } else {
            categoryNameFocused = false;
            categoryNameField.setFocused(false);
            categoryNameField.setLogicalBounds(0, 0, 0, 0);
            categoryShowNameButton.setLogicalBounds(0, 0, 0, 0);
            categoryShowBlockButton.setLogicalBounds(0, 0, 0, 0);
            categoryShowDistanceButton.setLogicalBounds(0, 0, 0, 0);
            categoryColorPicker.setLogicalBounds(0, 0, 0, 0);
        }
    }

    private void setWidgetBounds(Widget widget, int x, int y, int w, int h) {
        widget.setBounds(x, y, w, h);
    }

    private void togglePackageDropdown() {
        applyCategoryNameInput();
        applyCoordinateInputs();
        focusedCoordinate = -1;
        syncCoordinateInputs();
        nameFocused = false;
        categoryNameFocused = false;
        activeDropdownField = packageField;
        activeDropdown = activeDropdown == Dropdown.PACKAGE ? Dropdown.NONE : Dropdown.PACKAGE;
        if (activeDropdown == Dropdown.NONE) activeDropdownField = null;
        searchFocused = false;
    }

    private void toggleCategoryDropdown(DropdownFieldWidget field) {
        applyCategoryNameInput();
        applyCoordinateInputs();
        focusedCoordinate = -1;
        syncCoordinateInputs();
        nameFocused = false;
        categoryNameFocused = false;
        boolean closeCurrent = activeDropdown == Dropdown.CATEGORY && activeDropdownField == field;
        activeDropdown = closeCurrent ? Dropdown.NONE : Dropdown.CATEGORY;
        activeDropdownField = closeCurrent ? null : field;
        searchFocused = false;
    }

    private void focusCoordinate(int coordinate) {
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        nameFocused = false;
        categoryNameFocused = false;
        focusedCoordinate = coordinate;
        setFocusedWidget(currentCoordinateField());
    }

    private void handlePrimaryAction() {
        applyCategoryNameInput();
        if (selectedWaypoint == null) addWaypoint();
        else saveEditedWaypoint();
    }

    private void saveEditedWaypoint() {
        applyNameInput();
        applyCategoryNameInput();
        applyCoordinateInputs();
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        focusedCoordinate = -1;
        nameFocused = false;
        categoryNameFocused = false;
        setFocusedWidget(null);
        syncCoordinateInputs();
        saveChanges();
    }

    private void handleSecondaryAction() {
        if (selectedWaypoint == null) exit();
        else {
            nameFocused = false;
            categoryNameFocused = false;
            discardChanges();
        }
    }

    private void handleRemove() {
        nameFocused = false;
        categoryNameFocused = false;
        focusedCoordinate = -1;
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        removeSelectedWaypoint();
    }

    private void handleEditCurrent() {
        applyCoordinateInputs();
        focusedCoordinate = -1;
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        nameFocused = false;
        categoryNameFocused = false;
        if (openWaypointSelectionOrSelect(previewPos, editCurrentButton.getX(), editCurrentButton.getY() + editCurrentButton.getHeight(), p(430))
                && client != null && client.player != null) {
            client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
        }
    }

    private boolean handleWaypointClick(int screenMouseX, int screenMouseY, int logicalMouseX, int logicalMouseY) {
        WaypointHit hit = findWaypointAt(screenMouseX, screenMouseY);
        if (hit == null) return false;
        BlockPos pos = new BlockPos(hit.waypoint().x, hit.waypoint().y, hit.waypoint().z);
        return openWaypointSelectionOrSelect(pos, logicalMouseX + p(8), logicalMouseY + p(8), p(430));
    }

    private boolean openWaypointSelectionOrSelect(BlockPos pos, int dropdownX, int dropdownY, int dropdownW) {
        List<WaypointChoice> choices = waypointChoicesAt(pos);
        if (choices.isEmpty()) {
            showEditWarning("No waypoint at this position.");
            return false;
        }
        if (choices.size() == 1) {
            WaypointChoice choice = choices.getFirst();
            selectWaypoint(choice.pkg(), choice.waypoint());
            return true;
        }

        waypointSelectionPos = pos;
        waypointSearch = "";
        waypointScroll = 0;
        waypointScrollTarget = 0;
        waypointDropdownX = dropdownX;
        waypointDropdownY = dropdownY;
        waypointDropdownW = dropdownW;
        activeDropdown = Dropdown.WAYPOINT;
        activeDropdownField = null;
        searchFocused = false;
        showEditWarning("Select which waypoint to edit.");
        return true;
    }

    private void clearUiFocus() {
        applyCategoryNameInput();
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        focusedCoordinate = -1;
        nameFocused = false;
        categoryNameFocused = false;
        syncCoordinateInputs();
    }

    private void focusName() {
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        focusedCoordinate = -1;
        syncCoordinateInputs();
        categoryNameFocused = false;
        nameFocused = true;
        setFocusedWidget(nameField);
    }

    private void focusCategoryName() {
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
        focusedCoordinate = -1;
        syncCoordinateInputs();
        applyNameInput();
        nameFocused = false;
        categoryNameFocused = activeCategory != null;
        setFocusedWidget(activeCategory == null ? null : categoryNameField);
    }

    private void syncNameInput() {
        if (selectedWaypoint == nameInputWaypoint) return;
        nameInputWaypoint = selectedWaypoint;
        nameInput = selectedWaypoint == null || selectedWaypoint.name == null ? "" : selectedWaypoint.name;
        nameField.setInputAndMoveCursorToEnd(nameInput);
        setPreviewName(nameInput);
        nameFocused = false;
    }

    private void applyNameInput() {
        if (selectedWaypoint == null) return;
        selectedWaypoint.name = nameInput == null || nameInput.isBlank() ? "Waypoint" : nameInput.trim();
        nameInput = selectedWaypoint.name;
        nameField.setInputAndMoveCursorToEnd(nameInput);
        setPreviewName(nameInput);
    }

    private void syncCategoryNameInput() {
        if (activeCategory == categoryNameInputCategory) return;
        categoryNameInputCategory = activeCategory;
        categoryNameInput = activeCategory == null || activeCategory.name == null ? "" : activeCategory.name;
        categoryNameField.setInputAndMoveCursorToEnd(categoryNameInput);
        categoryNameFocused = false;
    }

    private void applyCategoryNameInput() {
        if (activeCategory == null) return;
        String value = categoryNameInput == null || categoryNameInput.isBlank() ? "New Category" : categoryNameInput.trim();
        if (!value.equals(activeCategory.name)) {
            activeCategory.name = value;
            WaypointData.save();
        }
        categoryNameInput = value;
        categoryNameField.setInputAndMoveCursorToEnd(value);
    }

    private CoordinateInputWidget currentCoordinateField() {
        return switch (focusedCoordinate) {
            case 0 -> xCoordinateField;
            case 1 -> yCoordinateField;
            case 2 -> zCoordinateField;
            default -> null;
        };
    }

    private void toggleVisibility(VisibilityTarget target, boolean reverse) {
        if (selectedWaypoint == null) return;
        switch (target) {
            case NAME -> selectedWaypoint.setShowNameOverride(nextOverride(selectedWaypoint.showNameOverride, reverse));
            case BLOCK -> selectedWaypoint.setShowOverride(nextOverride(selectedWaypoint.showOverride, reverse));
            case DISTANCE -> selectedWaypoint.setShowDistanceOverride(nextOverride(selectedWaypoint.showDistanceOverride, reverse));
        }
    }

    private void toggleCategoryDefault(VisibilityTarget target) {
        if (activeCategory == null) return;
        switch (target) {
            case NAME -> activeCategory.showNameByDefault = !activeCategory.showNameByDefault;
            case BLOCK -> activeCategory.showBlockByDefault = !activeCategory.showBlockByDefault;
            case DISTANCE -> activeCategory.showDistanceByDefault = !activeCategory.showDistanceByDefault;
        }
        WaypointData.save();
    }

    private Boolean nextOverride(Boolean override, boolean reverse) {
        if (reverse) {
            if (override == null) return false;
            return override ? null : true;
        }
        if (override == null) return true;
        return override ? false : null;
    }

    private String visibilityLabel(String label, Boolean override, boolean effective) {
        String state = override == null ? "Category (" + (effective ? "On" : "Off") + ")" : (override ? "On" : "Off");
        return label + ": " + state;
    }

    private String categoryDefaultLabel(String label, boolean enabled) {
        return label + " Default: " + (enabled ? "On" : "Off");
    }

    private void setActiveCategoryColor(int rgb) {
        if (activeCategory == null) return;
        activeCategory.color = CustomColor.fromInt(rgb & 0xFFFFFF);
        WaypointData.save();
    }

    private CustomColor color(int argb) {
        return CustomColor.fromInt(argb);
    }

    private int toScreenMouseX(int mouseX) {
        return (int) Math.round(mouseX * getMatrixScale());
    }

    private int toScreenMouseY(int mouseY) {
        return (int) Math.round(mouseY * getMatrixScale());
    }

    private void updateHoveredWaypoint(int mouseX, int mouseY) {
        if (isMouseOverEditorUi(mouseX, mouseY)) {
            hoveredWaypoint = null;
            return;
        }
        WaypointHit hit = findWaypointAt(toScreenMouseX(mouseX), toScreenMouseY(mouseY));
        hoveredWaypoint = hit == null ? null : hit.waypoint();
    }

    private void drawPanel(DrawContext ctx, int mouseX, int mouseY) {
        int panelW = p(PANEL_W);
        int x = getLogicalWidth() - panelW - p(12);
        int y = p(12);
        ui.drawVanillaPanel(x, y, panelW, editorPanelHeight(), 7, 8, 8, 12, 12);

        ui.drawText("Waypoint Editor", x + p(14), y + p(22), color(TEXT), ts(3.0f));
        if (selectedWaypoint != null) {
            ui.drawText("Text", x + p(14), y + p(222), color(TEXT_DIM), ts(2.7f));
            ui.drawText("Coordinates", x + p(14), y + p(302), color(TEXT_DIM), ts(2.7f));
            ui.drawText("Visibility", x + p(14), y + p(450), color(TEXT_DIM), ts(2.7f));
        } else {
            ui.drawText("Coordinates", x + p(14), y + p(222), color(TEXT_DIM), ts(2.7f));
        }

        drawCategoryPanel(x, y + editorPanelHeight() + p(PANEL_GAP));
    }

    private void drawCategoryPanel(int x, int y) {
        ui.drawVanillaPanel(x, y, p(PANEL_W), p(CATEGORY_PANEL_H), 7, 8, 8, 12, 12);
        ui.drawText("Category Editor", x + p(14), y + p(22), color(TEXT), ts(3.0f));
        if (activeCategory == null) {
            ui.drawText("No category selected", x + p(14), y + p(78), color(TEXT_DIM), ts(2.7f));
            return;
        }
        ui.drawText("Name", x + p(14), y + p(46), color(TEXT_DIM), ts(3f));
        ui.drawText("Defaults", x + p(14), y + p(114), color(TEXT_DIM), ts(3f));
    }

    private int editorPanelHeight() {
        return selectedWaypoint == null ? p(PANEL_H) : p(620);
    }

    private void drawEditHud() {
        String modeText = "you are in waypoint edit mode";
        int labelW = p(10) + textLogicalWidth(modeText, 4.0f);
        ui.drawRect(p(6), p(6), labelW, p(45), color(0xAA000000));
        ui.drawText(modeText, p(10), p(10), color(TEXT), ts(4.0f));

        String previewInfo = waypointStatsText("Preview", lastPreviewStats);
        int previewW = p(10) + textLogicalWidth(previewInfo, 2.2f);
        ui.drawRect(p(6), p(55), previewW, p(24), color(0xAA000000));
        ui.drawText(previewInfo, p(10), p(59), color(TEXT_DIM), ts(2.2f));

        if (isPreviewOnBarrier()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            String warning = trimToWidthEnd(tr, "Wynncraft rules do not allow waypoints on barriers, this waypoint preview will not be rendered.", textMaxWidth(getLogicalWidth() - p(24), 2.0f));
            int warningW = textLogicalWidth(warning, 3.0f);
            int x = (getLogicalWidth() - warningW) / 2;
            int y = getLogicalHeight() - p(156);
            ui.drawRect(x - p(6), y - p(10), warningW + p(12), p(45), color(0xCC220000));
            ui.drawText(warning, x, y, color(0xFFFF7777), ts(3.0f));
        }

        if (!editWarning.isEmpty() && System.currentTimeMillis() < editWarningUntil) {
            int y = getLogicalHeight() - (isPreviewOnBarrier() ? p(158) : p(136));
            drawStatusWarning(editWarning, y);
        }
    }

    private void drawStatusWarning(String warning, int y) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String visibleWarning = trimToWidthEnd(tr, warning, textMaxWidth(getLogicalWidth() - p(24), 2.0f));
        int warningW = textLogicalWidth(visibleWarning, 2.0f);
        int x = (getLogicalWidth() - warningW) / 2;
        ui.drawRect(x - p(6), y - p(4), warningW + p(12), p(30), color(0xCC442600));
        ui.drawText(visibleWarning, x, y, color(0xFFFFC680), ts(2.0f));
    }

    private boolean isMouseOverEditorUi(int mx, int my) {
        return isMouseOverEditorPanel(mx, my)
                || isMouseOverCategoryPanel(mx, my)
                || containsVisible(removeButton, mx, my)
                || containsVisible(editCurrentButton, mx, my)
                || containsVisible(primaryButton, mx, my)
                || containsVisible(secondaryButton, mx, my)
                || containsVisible(categoryColorPicker, mx, my)
                || (dropdownWidget.isVisible() && dropdownWidget.contains(mx, my));
    }

    private boolean containsVisible(Widget widget, int mx, int my) {
        return widget.isVisible() && widget.contains(mx, my);
    }

    private boolean isMouseOverEditorPanel(int mx, int my) {
        int panelW = p(PANEL_W);
        int panelX = getLogicalWidth() - panelW - p(12);
        int panelY = p(12);
        return mx >= panelX && my >= panelY && mx < panelX + panelW && my < panelY + editorPanelHeight();
    }

    private boolean isMouseOverCategoryPanel(int mx, int my) {
        int panelW = p(PANEL_W);
        int panelX = getLogicalWidth() - panelW - p(12);
        int panelY = p(12) + editorPanelHeight() + p(PANEL_GAP);
        return mx >= panelX && my >= panelY && mx < panelX + panelW && my < panelY + p(CATEGORY_PANEL_H);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        drawHoveredWaypointTooltip(ctx, mouseX, mouseY);
    }

    private void drawHoveredWaypointTooltip(DrawContext ctx, int mouseX, int mouseY) {
        if (hoveredWaypoint == null || isMouseOverEditorUi(mouseX, mouseY)) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        WaypointPackage pkg = packageOf(hoveredWaypoint);
        WaypointCategory category = hoveredWaypoint.getCategory();
        WaypointEditMode.WaypointPositionStats stats = statsAt(new net.minecraft.util.math.BlockPos(hoveredWaypoint.x, hoveredWaypoint.y, hoveredWaypoint.z));

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("§e" + (hoveredWaypoint.name == null || hoveredWaypoint.name.isBlank() ? "Waypoint" : hoveredWaypoint.name)));
        lines.add(Text.literal("§7x: §f" + hoveredWaypoint.x + " §7y: §f" + hoveredWaypoint.y + " §7z: §f" + hoveredWaypoint.z));
        lines.add(Text.literal("§7Package: §f" + (pkg == null ? "Unknown" : pkg.name)));
        lines.add(Text.literal("§7Category: §f" + (category == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : category.name)));
        lines.add(Text.literal("§eClick to edit this waypoint"));
        if(stats.packages().size() > 1 || stats.categories().size() > 1) {
            lines.add(Text.literal(""));
            lines.add(Text.literal("§7" + waypointStatsText("On these coordinates", stats)));
            if (!stats.packages().isEmpty())
                lines.add(Text.literal("§7Packages: §f" + trimJoined(stats.packages(), 70)));
            if (!stats.categories().isEmpty())
                lines.add(Text.literal("§7Categories: §f" + trimJoined(stats.categories(), 70)));
        }
        ctx.drawTooltip(tr, lines, toScreenMouseX(mouseX), toScreenMouseY(mouseY));
    }

    private String waypointStatsText(String prefix, WaypointEditMode.WaypointPositionStats stats) {
        return prefix + ": " + stats.waypointCount() + " waypoint" + (stats.waypointCount() == 1 ? "" : "s")
                + ", " + stats.categories().size() + " categor" + (stats.categories().size() == 1 ? "y" : "ies")
                + ", " + stats.packages().size() + " package" + (stats.packages().size() == 1 ? "" : "s");
    }

    private String trimJoined(List<String> values, int maxChars) {
        String joined = String.join(", ", values);
        if (joined.length() <= maxChars) return joined;
        return joined.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private List<WaypointPackage> filteredPackages() {
        String q = packageSearch.toLowerCase(Locale.ROOT);
        return WaypointData.INSTANCE.packages.stream()
                .filter(pkg -> {
                    String name = pkg.name == null ? "" : pkg.name;
                    return q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q);
                })
                .toList();
    }

    private List<WaypointCategory> filteredCategories() {
        if (activePackage == null) return List.of();
        String q = categorySearch.toLowerCase(Locale.ROOT);
        return activePackage.categories.stream()
                .filter(category -> {
                    String name = category.name == null ? "" : category.name;
                    return q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q);
                })
                .toList();
    }

    private List<WaypointChoice> filteredWaypointChoices() {
        String q = waypointSearch.toLowerCase(Locale.ROOT);
        return waypointChoicesAt(waypointSelectionPos).stream()
                .filter(choice -> {
                    String waypointName = choice.waypoint().name == null || choice.waypoint().name.isBlank() ? "Waypoint" : choice.waypoint().name;
                    String packageName = choice.pkg().name == null ? "Unknown Package" : choice.pkg().name;
                    WaypointCategory category = choice.waypoint().getCategory();
                    String categoryName = category == null || category.name == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : category.name;
                    return q.isEmpty()
                            || waypointName.toLowerCase(Locale.ROOT).contains(q)
                            || packageName.toLowerCase(Locale.ROOT).contains(q)
                            || categoryName.toLowerCase(Locale.ROOT).contains(q);
                })
                .toList();
    }

    private String currentSearchText() {
        if (activeDropdown == Dropdown.PACKAGE) return packageSearch;
        if (activeDropdown == Dropdown.CATEGORY) return categorySearch;
        if (activeDropdown == Dropdown.WAYPOINT) return waypointSearch;
        return "";
    }

    private void setSearchText(String value) {
        if (activeDropdown == Dropdown.PACKAGE) {
            packageSearch = value;
            packageScroll = 0;
            packageScrollTarget = 0;
        } else if (activeDropdown == Dropdown.CATEGORY) {
            categorySearch = value;
            categoryScroll = 0;
            categoryScrollTarget = 0;
        } else if (activeDropdown == Dropdown.WAYPOINT) {
            waypointSearch = value;
            waypointScroll = 0;
            waypointScrollTarget = 0;
        }
    }

    private void setCoordinateInput(int coordinate, String value) {
        switch (coordinate) {
            case 0 -> xInput = value;
            case 1 -> yInput = value;
            case 2 -> zInput = value;
        }
    }

    private void syncCoordinateWidgets() {
        if (focusedCoordinate != 0) xCoordinateField.setInput(xInput);
        if (focusedCoordinate != 1) yCoordinateField.setInput(yInput);
        if (focusedCoordinate != 2) zCoordinateField.setInput(zInput);
    }

    private String trimToWidth(TextRenderer tr, String text, int maxWidth) {
        if (tr.getWidth(text) <= maxWidth) return text;
        String result = text;
        while (!result.isEmpty() && tr.getWidth(result) > maxWidth) {
            result = result.substring(1);
        }
        return result;
    }

    private String trimToWidthEnd(TextRenderer tr, String text, int maxWidth) {
        if (tr.getWidth(text) <= maxWidth) return text;
        String result = text;
        while (!result.isEmpty() && tr.getWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private int textMaxWidth(int logicalWidth, float textScale) {
        return Math.max(0, (int) (logicalWidth / Math.max(0.1f, textScale)));
    }

    private int textLogicalWidth(String text, float textScale) {
        return (int) Math.ceil(MinecraftClient.getInstance().textRenderer.getWidth(text) * textScale);
    }

    private float fitTextScale(String text, int logicalWidth, float preferredScale, float minScale) {
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        if (textWidth <= 0) return preferredScale;
        return Math.max(minScale, Math.min(preferredScale, logicalWidth / (float) textWidth));
    }

    private float fitTextScale(String[] lines, int logicalWidth, int logicalHeight, float preferredScale, float minScale) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, textRenderer.getWidth(line));
        }
        if (textWidth <= 0) return preferredScale;

        float widthScale = logicalWidth / (float) textWidth;
        float heightScale = (logicalHeight - p(10)) / (float) (textRenderer.fontHeight * lines.length);
        return Math.max(minScale, Math.min(preferredScale, Math.min(widthScale, heightScale)));
    }

    private String[] buttonLabelLines(String label) {
        int stateSeparator = label.indexOf(": ");
        if (stateSeparator > 0) {
            return new String[] { label.substring(0, stateSeparator), label.substring(stateSeparator + 2) };
        }

        return switch (label) {
            case "Remove Waypoint" -> selectedWaypoint == null ? new String[] { label } : new String[] { "Remove", "Waypoint" };
            case "Save Changes" -> new String[] { "Save", "Changes" };
            case "Discard Changes" -> new String[] { "Discard", "Changes" };
            default -> new String[] { label };
        };
    }

    private static class ClickAreaWidget extends Widget {
        private final java.util.function.BooleanSupplier visibleSupplier;
        private final Runnable onClick;

        private ClickAreaWidget(java.util.function.BooleanSupplier visibleSupplier, Runnable onClick) {
            this.visibleSupplier = visibleSupplier;
            this.onClick = onClick;
        }

        @Override
        public void draw(DrawContext ctx, int mouseX, int mouseY, float tickDelta, UIUtils ui) {
            setVisible(visibleSupplier.getAsBoolean());
            super.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) { }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            onClick.run();
            return false;
        }
    }

    private static class ActionButtonWidget extends Widget {
        private final WaypointEditModeUI screen;
        private final Supplier<String> labelSupplier;
        private final Runnable onLeftClick;
        private final Runnable onRightClick;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private ActionButtonWidget(WaypointEditModeUI screen, Supplier<String> labelSupplier, Runnable onClick) {
            this(screen, labelSupplier, onClick, null);
        }

        private ActionButtonWidget(WaypointEditModeUI screen, Supplier<String> labelSupplier, Runnable onLeftClick, Runnable onRightClick) {
            this.screen = screen;
            this.labelSupplier = labelSupplier;
            this.onLeftClick = onLeftClick;
            this.onRightClick = onRightClick;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String label = labelSupplier.get();
            screen.ui.drawButton(logicalX, logicalY, logicalW, logicalH, hovered);
            String[] lines = screen.buttonLabelLines(label);
            if (lines.length == 1) {
                screen.ui.drawCenteredText(label, logicalX + logicalW / 2f, logicalY + logicalH / 2f, screen.color(TEXT), screen.ts(screen.fitTextScale(label, logicalW - screen.p(12), 3.0f, 1.25f)));
                return;
            }

            float scale = screen.fitTextScale(lines, logicalW - screen.p(12), logicalH, 2.0f, 1.25f);
            float lineHeight = MinecraftClient.getInstance().textRenderer.fontHeight * scale;
            float firstLineY = logicalY + logicalH / 2f - lineHeight / 2f;
            for (int i = 0; i < lines.length; i++) {
                screen.ui.drawCenteredText(lines[i], logicalX + logicalW / 2f, firstLineY + lineHeight * i, screen.color(TEXT), screen.ts(scale));
            }
        }

        @Override
        protected boolean onClick(int button) {
            Runnable action = switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> onLeftClick;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> onRightClick;
                default -> null;
            };
            if (action == null) return false;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            action.run();
            return true;
        }
    }

    private static class NameInputWidget extends TextInputWidget {
        private final WaypointEditModeUI screen;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private NameInputWidget(WaypointEditModeUI screen) {
            super(0, 0, 0, 0, 9, 9, 2.5f);
            this.screen = screen;
            setPlaceholder("Waypoint");
            setTextColor(screen.color(TEXT));
            setPlaceholderColor(screen.color(TEXT_DIM));
            setCursorColor(screen.color(TEXT));
            setSelectionColor(screen.color(0xAA3366CC));
            setOnChange(value -> {
                screen.nameInput = value;
                setPreviewName(value);
            });
            setOnFocus(widget -> screen.nameFocused = true);
            setOnBlur(widget -> screen.nameFocused = false);
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(isFocused() || hovered ? FIELD_HOVER : FIELD_BG));
            screen.ui.drawRect(logicalX, logicalY + logicalH - screen.p(2), logicalW, screen.p(2), screen.color(isFocused() ? GOLD : 0x882E251C));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            screen.focusName();
            return super.mouseClicked(mx, my, button);
        }
    }

    private static class CategoryNameInputWidget extends TextInputWidget {
        private final WaypointEditModeUI screen;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private CategoryNameInputWidget(WaypointEditModeUI screen) {
            super(0, 0, 0, 0, 9, 9, 2.5f);
            this.screen = screen;
            setPlaceholder("New Category");
            setTextColor(screen.color(TEXT));
            setPlaceholderColor(screen.color(TEXT_DIM));
            setCursorColor(screen.color(TEXT));
            setSelectionColor(screen.color(0xAA3366CC));
            setOnChange(value -> screen.categoryNameInput = value);
            setOnFocus(widget -> screen.categoryNameFocused = true);
            setOnBlur(widget -> screen.categoryNameFocused = false);
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(isFocused() || hovered ? FIELD_HOVER : FIELD_BG));
            screen.ui.drawRect(logicalX, logicalY + logicalH - screen.p(2), logicalW, screen.p(2), screen.color(isFocused() ? GOLD : 0x882E251C));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            screen.focusCategoryName();
            return super.mouseClicked(mx, my, button);
        }
    }

    private static class CategoryColorPickerWidget extends Widget {
        private static final int PICKER_W = 240;
        private static final int PICKER_H = 220;
        private static final int SV_CELL = 4;
        private static final int[] PRESET_COLORS = {
                0xFFFFFF, 0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
                0x55FFFF, 0x5555FF, 0xFF55FF, 0xAA55FF, 0x888888, 0x000000
        };

        private final WaypointEditModeUI screen;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;
        private boolean open = false;
        private float colorH = 0f;
        private float colorS = 0f;
        private float colorV = 1f;
        private int dragMode = 0;
        private float svCacheHue = -1f;
        private int[] svCacheColors = null;
        private int svCacheCols = 0;
        private int svCacheRows = 0;
        private int[] hueCacheColors = null;
        private int hueCacheRows = 0;

        private CategoryColorPickerWidget(WaypointEditModeUI screen) {
            this.screen = screen;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            if (open) screen.setWidgetBounds(this, 0, 0, screen.getLogicalWidth(), screen.getLogicalHeight());
            else screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int rgb = categoryColorInt(activeCategory) & 0xFFFFFF;
            int swatchX = swatchLogicalX();
            int swatchY = swatchLogicalY();
            int swatchW = screen.p(42);
            int swatchH = screen.p(28);
            screen.ui.drawText("Color", logicalX, logicalY + screen.p(3), screen.color(TEXT_DIM), screen.ts(2.7f));
            screen.ui.drawRect(swatchX, swatchY, swatchW, swatchH, CustomColor.fromInt(rgb));
            screen.ui.drawText(String.format("#%06X", rgb), swatchX + swatchW + screen.p(10), logicalY + screen.p(6), screen.color(TEXT), screen.ts(2.5f));
            if (open) renderPicker(ctx, pickerX(), pickerY());
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            if (isInScreen(mx, my, swatchScreenBounds())) {
                open = !open;
                if (open) setPickerColor(categoryColorInt(activeCategory));
                dragMode = 0;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            }
            if (!open) return false;

            int px = pickerX();
            int py = pickerY();
            if (!isIn(mx, my, px, py, PICKER_W, PICKER_H)) {
                open = false;
                return true;
            }

            int[] cb = closeButtonBounds(px, py);
            if (isIn(mx, my, cb[0], cb[1], cb[2], cb[3])) {
                open = false;
                return true;
            }

            int[] sv = svBoxBounds(px, py);
            if (isIn(mx, my, sv[0], sv[1], sv[2], sv[3])) {
                dragMode = 2;
                updateSv(mx, my, sv);
                return true;
            }

            int[] hb = hueBarBounds(px, py);
            if (isIn(mx, my, hb[0], hb[1], hb[2], hb[3])) {
                dragMode = 1;
                updateHue(my, hb);
                return true;
            }

            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int[] pb = presetBounds(px, py, i);
                if (isIn(mx, my, pb[0], pb[1], pb[2], pb[3])) {
                    setPickerColor(PRESET_COLORS[i]);
                    return true;
                }
            }

            for (int i = 0; i < 3; i++) {
                int[] bb = buttonBounds(px, py, i);
                if (isIn(mx, my, bb[0], bb[1], bb[2], bb[3])) {
                    if (i == 0) {
                        setPickerColor(0xFFFFFF);
                        screen.setActiveCategoryColor(0xFFFFFF);
                    }
                    if (i == 1) screen.setActiveCategoryColor(hsvToRgb(colorH, colorS, colorV));
                    open = false;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }

            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!visible || !enabled || !open || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || dragMode == 0) return false;
            int px = pickerX();
            int py = pickerY();
            if (dragMode == 1) updateHue(mouseY, hueBarBounds(px, py));
            if (dragMode == 2) updateSv(mouseX, mouseY, svBoxBounds(px, py));
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            dragMode = 0;
            return false;
        }

        private int pickerX() {
            int[] swatch = swatchScreenBounds();
            int preferred = swatch[0] + swatch[2] - PICKER_W;
            return MathHelper.clamp(preferred, screen.p(8), Math.max(screen.p(8), screen.getLogicalWidth() - PICKER_W - screen.p(8)));
        }

        private int pickerY() {
            int[] swatch = swatchScreenBounds();
            int below = swatch[1] + swatch[3] + screen.p(8);
            return MathHelper.clamp(below, screen.p(8), Math.max(screen.p(8), screen.getLogicalHeight() - PICKER_H - screen.p(8)));
        }

        private int swatchLogicalX() {
            return logicalX + screen.p(90);
        }

        private int swatchLogicalY() {
            return logicalY + screen.p(2);
        }

        private int[] swatchScreenBounds() {
            int x = Math.round(screen.ui.sx(swatchLogicalX()));
            int y = Math.round(screen.ui.sy(swatchLogicalY()));
            return new int[]{x, y, screen.ui.sw(screen.p(42)), screen.ui.sh(screen.p(28))};
        }

        private void renderPicker(DrawContext ctx, int px, int py) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            ctx.fill(px, py, px + PICKER_W, py + PICKER_H, 0xFF222222);
            ctx.fill(px, py, px + PICKER_W, py + 1, GOLD);
            ctx.fill(px, py + PICKER_H - 1, px + PICKER_W, py + PICKER_H, GOLD);
            ctx.fill(px, py, px + 1, py + PICKER_H, GOLD);
            ctx.fill(px + PICKER_W - 1, py, px + PICKER_W, py + PICKER_H, GOLD);
            ctx.drawCenteredTextWithShadow(tr, "Color: Category", px + PICKER_W / 2, py + 6, GOLD);

            int[] sv = svBoxBounds(px, py);
            ensureSvCache(sv[2], sv[3]);
            for (int row = 0; row < svCacheRows; row++) {
                int y0 = sv[1] + row * SV_CELL;
                int y1 = Math.min(sv[1] + sv[3], y0 + SV_CELL);
                for (int col = 0; col < svCacheCols; col++) {
                    int x0 = sv[0] + col * SV_CELL;
                    int x1 = Math.min(sv[0] + sv[2], x0 + SV_CELL);
                    ctx.fill(x0, y0, x1, y1, svCacheColors[row * svCacheCols + col]);
                }
            }
            drawColorCursor(ctx, sv[0] + (int) (colorS * (sv[2] - 1)), sv[1] + (int) ((1f - colorV) * (sv[3] - 1)));

            int[] hb = hueBarBounds(px, py);
            ensureHueCache(hb[3]);
            for (int row = 0; row < hueCacheRows; row++) {
                ctx.fill(hb[0], hb[1] + row, hb[0] + hb[2], hb[1] + row + 1, hueCacheColors[row]);
            }
            int hueCy = hb[1] + (int) ((colorH / 360f) * (hb[3] - 1));
            ctx.fill(hb[0] - 2, hueCy - 2, hb[0] + hb[2] + 2, hueCy - 1, 0xFFFFFFFF);
            ctx.fill(hb[0] - 2, hueCy + 1, hb[0] + hb[2] + 2, hueCy + 2, 0xFFFFFFFF);
            ctx.fill(hb[0] - 2, hueCy - 1, hb[0] - 1, hueCy + 1, 0xFFFFFFFF);
            ctx.fill(hb[0] + hb[2] + 1, hueCy - 1, hb[0] + hb[2] + 2, hueCy + 1, 0xFFFFFFFF);

            int currentRgb = hsvToRgb(colorH, colorS, colorV);
            int previewX = hb[0] + hb[2] + 10;
            int previewW = Math.min(40, px + PICKER_W - previewX - 8);
            ctx.fill(previewX, sv[1], previewX + previewW, sv[1] + 30, 0xFF000000 | currentRgb);
            ctx.fill(previewX - 1, sv[1] - 1, previewX + previewW + 1, sv[1], 0xFFAAAAAA);
            ctx.fill(previewX - 1, sv[1] + 30, previewX + previewW + 1, sv[1] + 31, 0xFFAAAAAA);
            ctx.fill(previewX - 1, sv[1], previewX, sv[1] + 30, 0xFFAAAAAA);
            ctx.fill(previewX + previewW, sv[1], previewX + previewW + 1, sv[1] + 30, 0xFFAAAAAA);
            ctx.drawTextWithShadow(tr, String.format("#%06X", currentRgb & 0xFFFFFF), previewX, sv[1] + 36, 0xFFFFFFFF);

            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int[] pb = presetBounds(px, py, i);
                ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + pb[3], 0xFF000000 | PRESET_COLORS[i]);
                ctx.fill(pb[0], pb[1], pb[0] + pb[2], pb[1] + 1, 0xFF555555);
                ctx.fill(pb[0], pb[1] + pb[3] - 1, pb[0] + pb[2], pb[1] + pb[3], 0xFF555555);
            }

            String[] labels = {"Reset", "Apply", "Close"};
            int[] btnColors = {0xFFAA3333, 0xFF338833, 0xFF3388AA};
            for (int i = 0; i < 3; i++) {
                int[] bb = buttonBounds(px, py, i);
                ctx.fill(bb[0], bb[1], bb[0] + bb[2], bb[1] + bb[3], btnColors[i]);
                ctx.drawCenteredTextWithShadow(tr, labels[i], bb[0] + bb[2] / 2, bb[1] + 5, 0xFFFFFFFF);
            }

            int[] cb = closeButtonBounds(px, py);
            ctx.fill(cb[0], cb[1], cb[0] + cb[2], cb[1] + cb[3], 0xFF552222);
            ctx.drawCenteredTextWithShadow(tr, "X", cb[0] + cb[2] / 2, cb[1] + 2, 0xFFFFFFFF);
        }

        private int[] closeButtonBounds(int px, int py) {
            return new int[]{px + PICKER_W - 16, py + 4, 12, 12};
        }

        private int[] svBoxBounds(int px, int py) {
            return new int[]{px + 12, py + 22, 120, 120};
        }

        private int[] hueBarBounds(int px, int py) {
            int[] sv = svBoxBounds(px, py);
            return new int[]{sv[0] + sv[2] + 10, sv[1], 14, sv[3]};
        }

        private int[] presetBounds(int px, int py, int presetIdx) {
            int cellW = (PICKER_W - 20) / PRESET_COLORS.length;
            return new int[]{px + 10 + presetIdx * cellW, py + 155, cellW - 2, 12};
        }

        private int[] buttonBounds(int px, int py, int btnIdx) {
            int gap = 6;
            int btnW = (PICKER_W - 16 - gap * 2) / 3;
            int btnH = 18;
            int startX = px + 8;
            int y = py + PICKER_H - btnH - 8;
            return new int[]{startX + btnIdx * (btnW + gap), y, btnW, btnH};
        }

        private void ensureSvCache(int width, int height) {
            int cols = (int) Math.ceil((double) width / SV_CELL);
            int rows = (int) Math.ceil((double) height / SV_CELL);
            if (svCacheColors != null && cols == svCacheCols && rows == svCacheRows && colorH == svCacheHue) return;
            svCacheCols = cols;
            svCacheRows = rows;
            svCacheColors = new int[cols * rows];
            svCacheHue = colorH;
            for (int row = 0; row < rows; row++) {
                float v = 1f - (row * SV_CELL) / (float) (height - 1);
                if (v < 0) v = 0;
                for (int col = 0; col < cols; col++) {
                    float s = (col * SV_CELL) / (float) (width - 1);
                    if (s > 1) s = 1;
                    svCacheColors[row * cols + col] = 0xFF000000 | hsvToRgb(colorH, s, v);
                }
            }
        }

        private void ensureHueCache(int height) {
            if (hueCacheColors != null && hueCacheRows == height) return;
            hueCacheRows = height;
            hueCacheColors = new int[height];
            for (int row = 0; row < height; row++) {
                float h = (row / (float) (height - 1)) * 360f;
                hueCacheColors[row] = 0xFF000000 | hsvToRgb(h, 1f, 1f);
            }
        }

        private void updateSv(double mx, double my, int[] sv) {
            colorS = MathHelper.clamp((float) (mx - sv[0]) / (sv[2] - 1), 0f, 1f);
            colorV = MathHelper.clamp(1f - (float) (my - sv[1]) / (sv[3] - 1), 0f, 1f);
        }

        private void updateHue(double my, int[] hb) {
            colorH = MathHelper.clamp((float) (my - hb[1]) / (hb[3] - 1) * 360f, 0f, 360f);
        }

        private void setPickerColor(int color) {
            int c = color & 0xFFFFFF;
            float[] hsv = rgbToHsv((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            colorH = hsv[0];
            colorS = hsv[1];
            colorV = hsv[2];
        }

        private static void drawColorCursor(DrawContext ctx, int cx, int cy) {
            ctx.fill(cx - 4, cy - 4, cx + 5, cy - 3, 0xFF000000);
            ctx.fill(cx - 4, cy + 3, cx + 5, cy + 4, 0xFF000000);
            ctx.fill(cx - 4, cy - 3, cx - 3, cy + 3, 0xFF000000);
            ctx.fill(cx + 3, cy - 3, cx + 4, cy + 3, 0xFF000000);
            ctx.fill(cx - 3, cy - 3, cx + 4, cy - 2, 0xFFFFFFFF);
            ctx.fill(cx - 3, cy + 2, cx + 4, cy + 3, 0xFFFFFFFF);
            ctx.fill(cx - 3, cy - 2, cx - 2, cy + 2, 0xFFFFFFFF);
            ctx.fill(cx + 2, cy - 2, cx + 3, cy + 2, 0xFFFFFFFF);
        }

        private static int hsvToRgb(float h, float s, float v) {
            float c = v * s;
            float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
            float m = v - c;
            float rf = 0, gf = 0, bf = 0;
            if (h < 60) { rf = c; gf = x; }
            else if (h < 120) { rf = x; gf = c; }
            else if (h < 180) { gf = c; bf = x; }
            else if (h < 240) { gf = x; bf = c; }
            else if (h < 300) { rf = x; bf = c; }
            else { rf = c; bf = x; }
            int r = Math.round((rf + m) * 255);
            int g = Math.round((gf + m) * 255);
            int b = Math.round((bf + m) * 255);
            return (r << 16) | (g << 8) | b;
        }

        private static float[] rgbToHsv(int r, int g, int b) {
            float rf = r / 255f, gf = g / 255f, bf = b / 255f;
            float max = Math.max(rf, Math.max(gf, bf));
            float min = Math.min(rf, Math.min(gf, bf));
            float d = max - min;
            float h = 0;
            if (d != 0) {
                if (max == rf) h = 60 * (((gf - bf) / d) % 6);
                else if (max == gf) h = 60 * ((bf - rf) / d + 2);
                else h = 60 * ((rf - gf) / d + 4);
            }
            if (h < 0) h += 360;
            float s = max == 0 ? 0 : d / max;
            return new float[]{h, s, max};
        }

        private static boolean isIn(double mx, double my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        private static boolean isInScreen(double mx, double my, int[] bounds) {
            return isIn(mx, my, bounds[0], bounds[1], bounds[2], bounds[3]);
        }
    }

    private static class CoordinateInputWidget extends TextInputWidget {
        private final WaypointEditModeUI screen;
        private final int coordinate;
        private final String label;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private CoordinateInputWidget(WaypointEditModeUI screen, int coordinate, String label) {
            super(0, 0, 0, 0, 27, 9, 2.3f);
            this.screen = screen;
            this.coordinate = coordinate;
            this.label = label;
            setTextColor(screen.color(TEXT));
            setPlaceholderColor(screen.color(TEXT_DIM));
            setCursorColor(screen.color(TEXT));
            setSelectionColor(screen.color(0xAA3366CC));
            setCharacterFilter(character -> (character >= '0' && character <= '9') || character == '-');
            setOnChange(value -> {
                screen.setCoordinateInput(coordinate, value);
                applyCoordinateInputs();
            });
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            boolean focused = focusedCoordinate == coordinate;
            screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(focused || hovered ? FIELD_HOVER : FIELD_BG));
            screen.ui.drawRect(logicalX, logicalY + logicalH - screen.p(2), logicalW, screen.p(2), screen.color(focused ? GOLD : 0x882E251C));
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawText(label, logicalX + screen.p(7), logicalY + screen.p(9), screen.color(TEXT_DIM), screen.ts(2.3f));
            super.drawContent(ctx, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            screen.focusCoordinate(coordinate);
            return super.mouseClicked(mx, my, button);
        }
    }

    private static class InfoButtonWidget extends Widget {
        private final WaypointEditModeUI screen;
        private int logicalX;
        private int logicalY;

        private InfoButtonWidget(WaypointEditModeUI screen) {
            this.screen = screen;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawText("[?]", logicalX, logicalY, screen.color(hovered ? 0xFFFFFFFF : 0xFFAAAAAA), screen.ts(2.5f));
            if (!hovered) return;

            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("§eWaypoint edit mode"));
            lines.add(Text.literal("§fWASD§7 moves the preview horizontally."));
            lines.add(Text.literal("§fSpace/Shift§7 moves the preview up/down."));
            lines.add(Text.literal("§7Use package/category dropdowns to choose where it saves."));
            lines.add(Text.literal("§7Click an existing waypoint to edit it."));
            lines.add(Text.literal("§7Add/remove hotkeys work when no text field is focused."));
            lines.add(Text.literal("§7Free Move Mode keeps the preview fixed while you move."));
            lines.add(Text.literal("§7Press your return keybind to resume editing."));
            lines.add(Text.literal("§cBarrier waypoints are saved but not rendered."));
            ctx.drawTooltip(tr, lines, screen.toScreenMouseX(mouseX), screen.toScreenMouseY(mouseY));
        }

        @Override
        protected boolean onClick(int button) {
            return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        }
    }

    private static class DropdownFieldWidget extends Widget {
        private final WaypointEditModeUI screen;
        private final Dropdown dropdown;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private DropdownFieldWidget(WaypointEditModeUI screen, Dropdown dropdown) {
            this.screen = screen;
            this.dropdown = dropdown;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String label = dropdown == Dropdown.PACKAGE ? "Package" : "Category";
            String value = dropdown == Dropdown.PACKAGE
                    ? activePackage == null ? "None" : activePackage.name
                    : activeCategory == null ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : activeCategory.name;
            boolean open = activeDropdown == dropdown && screen.activeDropdownField == this;
            screen.ui.drawText(label, logicalX, logicalY - screen.p(25), screen.color(TEXT_DIM), screen.ts(2.7f));
            screen.ui.drawButton(logicalX, logicalY, logicalW, logicalH, hovered || open);
            screen.ui.drawText(screen.trimToWidth(MinecraftClient.getInstance().textRenderer, value, screen.textMaxWidth(logicalW - screen.p(34), 3f)), logicalX + screen.p(10), logicalY + screen.p(12), screen.color(TEXT), screen.ts(3f));
            screen.ui.drawText(open ? "^" : "v", logicalX + logicalW - screen.p(30), logicalY + screen.p(12), screen.color(TEXT), screen.ts(3f));
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            if (dropdown == Dropdown.PACKAGE) screen.togglePackageDropdown();
            else screen.toggleCategoryDropdown(this);
            return true;
        }
    }

    private static class DropdownWidget extends Widget {
        private final WaypointEditModeUI screen;
        private Dropdown dropdown = Dropdown.NONE;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;
        private int rows;
        private int visibleRows;
        private int firstRow = -1;
        private float scrollOffset;

        private DropdownWidget(WaypointEditModeUI screen) {
            this.screen = screen;
        }

        private void configure(Dropdown dropdown, int x, int y, int w) {
            int rowH = screen.p(ROW_H);
            int newRows = screen.dropdownRowCount(dropdown);
            int newVisibleRows = Math.min(newRows, Math.max(1, (screen.p(DROPDOWN_MAX_H) - screen.p(30)) / rowH));
            screen.clampDropdownScroll(dropdown, newVisibleRows);
            float newScrollOffset = screen.dropdownScroll(dropdown);
            int newFirstRow = screen.firstVisibleDropdownRow(dropdown, newVisibleRows);
            int newLogicalH = screen.p(30) + newVisibleRows * rowH;
            float rowOffset = newScrollOffset - newFirstRow * rowH;
            boolean rebuildChildren = this.dropdown != dropdown
                    || logicalX != x
                    || logicalY != y
                    || logicalW != w
                    || logicalH != newLogicalH
                    || rows != newRows
                    || visibleRows != newVisibleRows
                    || firstRow != newFirstRow
                    || scrollOffset != newScrollOffset
                    || children.isEmpty();

            this.dropdown = dropdown;
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = newLogicalH;
            this.rows = newRows;
            this.visibleRows = newVisibleRows;
            this.firstRow = newFirstRow;
            this.scrollOffset = newScrollOffset;
            setVisible(true);
            screen.setWidgetBounds(this, x, y, w, logicalH);

            if (!rebuildChildren) return;
            clearChildren();
            DropdownSearchWidget searchWidget = new DropdownSearchWidget(screen);
            searchWidget.setLogicalBounds(x + screen.p(5), y + screen.p(5), w - screen.p(10), screen.p(22));
            searchWidget.setInput(screen.currentSearchText());
            searchWidget.setFocused(searchFocused);
            addDropdownChild(searchWidget);

            int listY = y + screen.p(30);
            int rowCount = Math.min(rows - firstRow, visibleRows + (rowOffset > 0 ? 1 : 0));
            for (int slot = 0; slot < rowCount; slot++) {
                int rowIndex = firstRow + slot;
                if (rowIndex >= rows) break;
                DropdownRowWidget rowWidget = new DropdownRowWidget(screen, dropdown, rowIndex);
                rowWidget.setClipBounds(x, listY, w, visibleRows * rowH);
                rowWidget.setLogicalBounds(x, listY + slot * rowH - (int) rowOffset, w, rowH);
                addDropdownChild(rowWidget);
            }

            if (rows > visibleRows) {
                DropdownScrollbarWidget scrollbarWidget = new DropdownScrollbarWidget(screen, dropdown);
                scrollbarWidget.setLogicalBounds(x + w - screen.p(16), listY, screen.p(16), visibleRows * rowH);
                addDropdownChild(scrollbarWidget);
            }
        }

        private void addDropdownChild(Widget child) {
            if (screen.ui != null) child.setUi(screen.ui);
            addChild(child);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(0xF21A1410));
            screen.ui.drawRect(logicalX, logicalY, logicalW, screen.p(1), screen.color(GOLD));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled || !contains((int) mx, (int) my)) return false;
            for (int i = children.size() - 1; i >= 0; i--) {
                if (children.get(i).mouseClicked(mx, my, button)) return true;
            }
            return onClick(button);
        }

        @Override
        protected boolean onClick(int button) {
            return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (!visible || !enabled || !contains((int) mx, (int) my)) return false;
            screen.scrollDropdownWidget(dropdown, delta);
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!visible || !enabled) return false;
            for (int i = children.size() - 1; i >= 0; i--) {
                if (children.get(i).mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            }
            return false;
        }
    }

    private static class DropdownScrollbarWidget extends Widget {
        private final WaypointEditModeUI screen;
        private final Dropdown dropdown;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private DropdownScrollbarWidget(WaypointEditModeUI screen, Dropdown dropdown) {
            this.screen = screen;
            this.dropdown = dropdown;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int trackX = logicalX + screen.p(10);
            int trackW = screen.p(3);
            int thumbH = screen.scrollbarThumbHeight(dropdown, logicalH);
            int thumbY = scrollbarThumbY(thumbH);
            screen.ui.drawRect(trackX, logicalY, trackW, logicalH, screen.color(0xAA000000));
            screen.ui.drawRect(trackX - screen.p(1), thumbY, trackW + screen.p(2), thumbH, screen.color(hovered || screen.isDropdownScrollbarDragging(dropdown) ? 0xFFFFE36A : GOLD));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (!visible || !enabled || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !contains((int) mx, (int) my)) return false;
            int thumbH = screen.scrollbarThumbHeight(dropdown, logicalH);
            int thumbY = scrollbarThumbY(thumbH);
            float logicalMouseY = screen.mouseToLogicalY(my);
            screen.startDropdownScrollbarDrag(dropdown, logicalMouseY >= thumbY && logicalMouseY < thumbY + thumbH ? logicalMouseY - thumbY : thumbH / 2f);
            updateScroll(logicalMouseY);
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            return button == GLFW.GLFW_MOUSE_BUTTON_LEFT && screen.stopDropdownScrollbarDrag(dropdown);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!screen.isDropdownScrollbarDragging(dropdown) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            updateScroll(screen.mouseToLogicalY(mouseY));
            return true;
        }

        private int scrollbarThumbY(int thumbH) {
            float maxScroll = screen.maxDropdownScroll(dropdown, logicalH / screen.p(ROW_H));
            if (maxScroll <= 0 || logicalH <= thumbH) return logicalY;
            return logicalY + (int) ((logicalH - thumbH) * (screen.dropdownScroll(dropdown) / maxScroll));
        }

        private void updateScroll(float logicalMouseY) {
            int thumbH = screen.scrollbarThumbHeight(dropdown, logicalH);
            float maxScroll = screen.maxDropdownScroll(dropdown, logicalH / screen.p(ROW_H));
            float maxThumbTravel = logicalH - thumbH;
            if (maxScroll <= 0 || maxThumbTravel <= 0) return;
            float thumbY = MathHelper.clamp(logicalMouseY - logicalY - screen.dropdownScrollbarDragOffset, 0, maxThumbTravel);
            screen.setDropdownScrollImmediate(dropdown, thumbY / maxThumbTravel * maxScroll);
        }
    }

    private static class DropdownSearchWidget extends TextInputWidget {
        private final WaypointEditModeUI screen;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;

        private DropdownSearchWidget(WaypointEditModeUI screen) {
            super(0, 0, 0, 0, 5, 5, 2.2f);
            this.screen = screen;
            setPlaceholder("Search...");
            setTextColor(screen.color(TEXT));
            setPlaceholderColor(screen.color(TEXT_DIM));
            setCursorColor(screen.color(TEXT));
            setSelectionColor(screen.color(0xAA3366CC));
            setOnChange(screen::setSearchText);
            setOnFocus(widget -> searchFocused = true);
            setOnBlur(widget -> searchFocused = false);
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(searchFocused || hovered ? FIELD_HOVER : FIELD_BG));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            searchFocused = true;
            return super.mouseClicked(mx, my, button);
        }
    }

    private static class DropdownRowWidget extends Widget {
        private final WaypointEditModeUI screen;
        private final Dropdown dropdown;
        private final int index;
        private int logicalX;
        private int logicalY;
        private int logicalW;
        private int logicalH;
        private int clipX;
        private int clipY;
        private int clipW;
        private int clipH;

        private DropdownRowWidget(WaypointEditModeUI screen, Dropdown dropdown, int index) {
            this.screen = screen;
            this.dropdown = dropdown;
            this.index = index;
        }

        private void setLogicalBounds(int x, int y, int w, int h) {
            this.logicalX = x;
            this.logicalY = y;
            this.logicalW = w;
            this.logicalH = h;
            screen.setWidgetBounds(this, x, y, w, h);
        }

        private void setClipBounds(int x, int y, int w, int h) {
            this.clipX = x;
            this.clipY = y;
            this.clipW = w;
            this.clipH = h;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            boolean selected = screen.isDropdownRowSelected(dropdown, index);
            ctx.enableScissor((int) screen.ui.sx(clipX), (int) screen.ui.sy(clipY), (int) screen.ui.sx(clipX + clipW), (int) screen.ui.sy(clipY + clipH));
            if (selected || hovered) {
                screen.ui.drawRect(logicalX, logicalY, logicalW, logicalH, screen.color(selected ? 0xAA6C4F36 : 0xAA4D3C2D));
            }
            screen.ui.drawRect(logicalX + screen.p(7), logicalY + screen.p(10), screen.p(20), screen.p(20), screen.color(screen.dropdownRowColor(dropdown, index)));
            screen.ui.drawText(screen.trimToWidthEnd(tr, screen.dropdownRowText(dropdown, index), screen.textMaxWidth(logicalW - screen.p(42), 3f)), logicalX + screen.p(33), logicalY + screen.p(8), screen.color(TEXT), screen.ts(3f));
            ctx.disableScissor();
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            screen.selectDropdownRow(dropdown, index);
            return true;
        }
    }

    private int dropdownRowCount(Dropdown dropdown) {
        if (dropdown == Dropdown.PACKAGE) return filteredPackages().size();
        if (dropdown == Dropdown.CATEGORY) return filteredCategories().size();
        if (dropdown == Dropdown.WAYPOINT) return filteredWaypointChoices().size();
        return 0;
    }

    private int firstVisibleDropdownRow(Dropdown dropdown, int visibleRows) {
        int rowH = p(ROW_H);
        int rows = dropdownRowCount(dropdown);
        int scroll = (int) dropdownScroll(dropdown);
        return MathHelper.clamp(scroll / rowH, 0, Math.max(0, rows - visibleRows));
    }

    private String dropdownRowText(Dropdown dropdown, int index) {
        if (dropdown == Dropdown.PACKAGE) {
            String name = filteredPackages().get(index).name;
            return name == null || name.isBlank() ? "Unnamed Package" : name;
        }
        if (dropdown == Dropdown.CATEGORY) {
            String name = filteredCategories().get(index).name;
            return name == null || name.isBlank() ? "Unnamed Category" : name;
        }
        WaypointChoice choice = filteredWaypointChoices().get(index);
        String waypointName = choice.waypoint().name == null || choice.waypoint().name.isBlank() ? "Waypoint" : choice.waypoint().name;
        String packageName = choice.pkg().name == null ? "Unknown Package" : choice.pkg().name;
        WaypointCategory category = choice.waypoint().getCategory();
        String categoryName = category == null || category.name == null || category.name.isBlank() ? WaypointData.UNCATEGORIZED_CATEGORY_NAME : category.name;
        return waypointName + "  |  " + packageName + "  |  " + categoryName;
    }

    private boolean isDropdownRowSelected(Dropdown dropdown, int index) {
        if (dropdown == Dropdown.PACKAGE) return activePackage == filteredPackages().get(index);
        if (dropdown == Dropdown.CATEGORY) {
            return activeCategory == filteredCategories().get(index);
        }
        return selectedWaypoint == filteredWaypointChoices().get(index).waypoint();
    }

    private int dropdownRowColor(Dropdown dropdown, int index) {
        if (dropdown == Dropdown.PACKAGE) return 0xFFFFFFFF;
        if (dropdown == Dropdown.CATEGORY) return categoryColorInt(filteredCategories().get(index));
        return categoryColorInt(filteredWaypointChoices().get(index).waypoint().getCategory());
    }

    private void selectDropdownRow(Dropdown dropdown, int index) {
        applyCategoryNameInput();
        if (dropdown == Dropdown.PACKAGE) {
            List<WaypointPackage> packages = filteredPackages();
            if (index < 0 || index >= packages.size()) return;
            activePackage = packages.get(index);
            WaypointData.resolveWaypointCategories(activePackage);
            if (activeCategory != null && !activePackage.categories.contains(activeCategory)) activeCategory = WaypointData.ensureUncategorizedCategory(activePackage);
            if (activeCategory == null) activeCategory = WaypointData.ensureUncategorizedCategory(activePackage);
        } else if (dropdown == Dropdown.CATEGORY) {
            List<WaypointCategory> categories = filteredCategories();
            if (index < 0 || index >= categories.size()) return;
            activeCategory = categories.get(index);
        } else if (dropdown == Dropdown.WAYPOINT) {
            List<WaypointChoice> choices = filteredWaypointChoices();
            if (index < 0 || index >= choices.size()) return;
            WaypointChoice choice = choices.get(index);
            selectWaypoint(choice.pkg(), choice.waypoint());
        }
        activeDropdown = Dropdown.NONE;
        activeDropdownField = null;
        searchFocused = false;
    }

    private void scrollDropdownWidget(Dropdown dropdown, double amount) {
        int visibleRows = visibleDropdownRows(dropdown);
        setDropdownScrollTarget(dropdown, dropdownTargetScroll(dropdown) + (float) (amount * -p(38)));
        clampDropdownScroll(dropdown, visibleRows);
    }

    private int visibleDropdownRows(Dropdown dropdown) {
        int rows = dropdownRowCount(dropdown);
        return Math.min(rows, Math.max(1, (p(DROPDOWN_MAX_H) - p(30)) / p(ROW_H)));
    }

    private float dropdownScroll(Dropdown dropdown) {
        if (dropdown == Dropdown.PACKAGE) return packageScroll;
        if (dropdown == Dropdown.CATEGORY) return categoryScroll;
        if (dropdown == Dropdown.WAYPOINT) return waypointScroll;
        return 0;
    }

    private float dropdownTargetScroll(Dropdown dropdown) {
        if (dropdown == Dropdown.PACKAGE) return packageScrollTarget;
        if (dropdown == Dropdown.CATEGORY) return categoryScrollTarget;
        if (dropdown == Dropdown.WAYPOINT) return waypointScrollTarget;
        return 0;
    }

    private void setDropdownScrollTarget(Dropdown dropdown, float value) {
        int visibleRows = visibleDropdownRows(dropdown);
        float clamped = MathHelper.clamp(value, 0, maxDropdownScroll(dropdown, visibleRows));
        if (dropdown == Dropdown.PACKAGE) packageScrollTarget = clamped;
        else if (dropdown == Dropdown.CATEGORY) categoryScrollTarget = clamped;
        else if (dropdown == Dropdown.WAYPOINT) waypointScrollTarget = clamped;
    }

    private void setDropdownScrollImmediate(Dropdown dropdown, float value) {
        int visibleRows = visibleDropdownRows(dropdown);
        float clamped = MathHelper.clamp(value, 0, maxDropdownScroll(dropdown, visibleRows));
        if (dropdown == Dropdown.PACKAGE) {
            packageScroll = clamped;
            packageScrollTarget = clamped;
        } else if (dropdown == Dropdown.CATEGORY) {
            categoryScroll = clamped;
            categoryScrollTarget = clamped;
        } else if (dropdown == Dropdown.WAYPOINT) {
            waypointScroll = clamped;
            waypointScrollTarget = clamped;
        }
    }

    private void clampDropdownScroll(Dropdown dropdown, int visibleRows) {
        float maxScroll = maxDropdownScroll(dropdown, visibleRows);
        if (dropdown == Dropdown.PACKAGE) {
            packageScroll = MathHelper.clamp(packageScroll, 0, maxScroll);
            packageScrollTarget = MathHelper.clamp(packageScrollTarget, 0, maxScroll);
        } else if (dropdown == Dropdown.CATEGORY) {
            categoryScroll = MathHelper.clamp(categoryScroll, 0, maxScroll);
            categoryScrollTarget = MathHelper.clamp(categoryScrollTarget, 0, maxScroll);
        } else if (dropdown == Dropdown.WAYPOINT) {
            waypointScroll = MathHelper.clamp(waypointScroll, 0, maxScroll);
            waypointScrollTarget = MathHelper.clamp(waypointScrollTarget, 0, maxScroll);
        }
    }

    private float maxDropdownScroll(Dropdown dropdown, int visibleRows) {
        int rowH = p(ROW_H);
        int rows = dropdownRowCount(dropdown);
        return Math.max(0, rows * rowH - visibleRows * rowH);
    }

    private int scrollbarThumbHeight(Dropdown dropdown, int trackH) {
        int rowH = p(ROW_H);
        int rows = dropdownRowCount(dropdown);
        if (rows <= 0) return trackH;
        return Math.max(p(18), trackH * trackH / (rows * rowH));
    }

    private void updateDropdownScroll(float tickDelta) {
        updateDropdownScroll(Dropdown.PACKAGE, tickDelta);
        updateDropdownScroll(Dropdown.CATEGORY, tickDelta);
        updateDropdownScroll(Dropdown.WAYPOINT, tickDelta);
    }

    private void updateDropdownScroll(Dropdown dropdown, float tickDelta) {
        int visibleRows = visibleDropdownRows(dropdown);
        clampDropdownScroll(dropdown, visibleRows);

        float target = dropdownTargetScroll(dropdown);
        float actual = dropdownScroll(dropdown);
        float diff = target - actual;
        if (Math.abs(diff) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle || isDropdownScrollbarDragging(dropdown)) {
            setDropdownActualScroll(dropdown, target);
            return;
        }
        setDropdownActualScroll(dropdown, actual + diff * 0.3f * tickDelta);
    }

    private void setDropdownActualScroll(Dropdown dropdown, float value) {
        int visibleRows = visibleDropdownRows(dropdown);
        float clamped = MathHelper.clamp(value, 0, maxDropdownScroll(dropdown, visibleRows));
        if (dropdown == Dropdown.PACKAGE) packageScroll = clamped;
        else if (dropdown == Dropdown.CATEGORY) categoryScroll = clamped;
        else if (dropdown == Dropdown.WAYPOINT) waypointScroll = clamped;
    }

    private void startDropdownScrollbarDrag(Dropdown dropdown, float dragOffset) {
        dropdownScrollbarDragging = true;
        dropdownScrollbarDragTarget = dropdown;
        dropdownScrollbarDragOffset = dragOffset;
    }

    private boolean stopDropdownScrollbarDrag(Dropdown dropdown) {
        if (!isDropdownScrollbarDragging(dropdown)) return false;
        dropdownScrollbarDragging = false;
        dropdownScrollbarDragTarget = Dropdown.NONE;
        return true;
    }

    private boolean isDropdownScrollbarDragging(Dropdown dropdown) {
        return dropdownScrollbarDragging && dropdownScrollbarDragTarget == dropdown;
    }

    private float mouseToLogicalY(double mouseY) {
        return (float) (mouseY * getScaleFactor());
    }
}
