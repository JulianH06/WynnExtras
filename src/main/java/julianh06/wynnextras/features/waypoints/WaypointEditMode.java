package julianh06.wynnextras.features.waypoints;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.KeyInputEvent;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.waypoints.old.Waypoint;
import julianh06.wynnextras.features.waypoints.old.WaypointCategory;
import julianh06.wynnextras.features.waypoints.old.WaypointData;
import julianh06.wynnextras.features.waypoints.old.WaypointPackage;
import julianh06.wynnextras.mixin.Invoker.GameRendererInvoker;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@WEModule
public class WaypointEditMode {
    enum Mode { EDIT, FREE_MOVE }
    enum Dropdown { NONE, PACKAGE, CATEGORY, WAYPOINT }

    static final int PANEL_W = 570;
    static final int PANEL_H = 360;
    static final int CATEGORY_PANEL_H = 260;
    static final int PANEL_GAP = 12;
    static final int ROW_H = 40;
    static final int DROPDOWN_MAX_H = 210;
    static final int FIELD_H = 50;
    static final int ACTION_H = 50;
    static final int TEXT = 0xFFE8DCC8;
    static final int TEXT_DIM = 0xFFB9A98B;
    static final int PANEL_BG = 0xDD1A1410;
    static final int FIELD_BG = 0xEE2E251C;
    static final int FIELD_HOVER = 0xFF4D3C2D;
    static final int GOLD = 0xFFECC600;
    static final int RED = 0xFFE05A5A;
    static final int GREEN = 0xFF5FB75F;
    static final int ORANGE = 0xFFE59B42;

    static boolean enabled = false;
    static Mode mode = Mode.EDIT;
    static boolean closingForFreeMove = false;

    static WaypointPackage activePackage = null;
    static WaypointCategory activeCategory = null;
    static BlockPos previewPos = BlockPos.ORIGIN;
    static Waypoint selectedWaypoint = null;
    static WaypointPackage selectedWaypointPackage = null;
    static WaypointSnapshot selectedSnapshot = null;
    static Waypoint hoveredWaypoint = null;

    static Dropdown activeDropdown = Dropdown.NONE;
    static String packageSearch = "";
    static String categorySearch = "";
    static boolean searchFocused = false;
    static int focusedCoordinate = -1;
    static String xInput = "0";
    static String yInput = "0";
    static String zInput = "0";
    static float packageScroll = 0;
    static float categoryScroll = 0;
    static float waypointScroll = 0;
    static boolean wasReturnKeyDown = false;
    static String editWarning = "";
    static long editWarningUntil = 0;
    static float renderTickDelta = 1f;
    static String previewName = "Waypoint";
    static String waypointSearch = "";
    static BlockPos waypointSelectionPos = BlockPos.ORIGIN;

    public WaypointEditMode() {
        HudRenderCallback.EVENT.register(WaypointEditMode::renderHud);
    }

    public static boolean isEditing(Waypoint waypoint) {
        return enabled && selectedWaypoint == waypoint;
    }

    public static void toggleFromCommand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled) {
            enterEditMode();
            return;
        }

        if (mode == Mode.FREE_MOVE) {
            enterEditMode();
            return;
        }

        exit();
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
        }
    }

    public static void enterEditMode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ensureSelectionDefaults();
        if (!enabled) {
            previewPos = initialPreviewPos(mc.player);
            selectedWaypoint = null;
            selectedWaypointPackage = null;
            selectedSnapshot = null;
        }

        enabled = true;
        mode = Mode.EDIT;
        activeDropdown = Dropdown.NONE;
        focusedCoordinate = -1;
        syncCoordinateInputs();
        closingForFreeMove = false;
        mc.send(() -> mc.setScreen(new WaypointEditModeUI()));
    }

    public static void enterFreeMoveMode() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled) return;
        mode = Mode.FREE_MOVE;
        activeDropdown = Dropdown.NONE;
        searchFocused = false;
        focusedCoordinate = -1;
        hoveredWaypoint = null;
        wasReturnKeyDown = isReturnKeyDown();
        closingForFreeMove = true;
        mc.send(() -> mc.setScreen(null));
    }

    public static void exit() {
        enabled = false;
        mode = Mode.EDIT;
        activeDropdown = Dropdown.NONE;
        searchFocused = false;
        focusedCoordinate = -1;
        selectedWaypoint = null;
        selectedWaypointPackage = null;
        selectedSnapshot = null;
        hoveredWaypoint = null;
        closingForFreeMove = false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof WaypointEditModeUI) {
            mc.send(() -> mc.setScreen(null));
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!enabled || mode != Mode.FREE_MOVE) {
            wasReturnKeyDown = false;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        int key = WynnExtrasConfig.INSTANCE.waypointEditReturnKey;
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;

        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        if (down && !wasReturnKeyDown) enterEditMode();
        wasReturnKeyDown = down;
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (!enabled || mode != Mode.FREE_MOVE) return;
        int key = WynnExtrasConfig.INSTANCE.waypointEditReturnKey;
        if (key == GLFW.GLFW_KEY_UNKNOWN || event.getKey() != key) return;
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            wasReturnKeyDown = false;
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS && !wasReturnKeyDown) {
            wasReturnKeyDown = true;
            enterEditMode();
        }
    }

    static boolean isReturnKeyDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        int key = WynnExtrasConfig.INSTANCE.waypointEditReturnKey;
        return key != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!enabled) return;
        renderTickDelta = event.partialTicks;
        if (!isPreviewOnBarrier()) {
            Color color = categoryColor(activeCategory);
            float blink = 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 600.0);
            Box previewBox = new Box(previewPos);
            if (previewShouldShowBlock()) {
                WorldRenderUtils.INSTANCE_WAYPOINTS.buffer = new BufferBuilder(
                        WorldRenderUtils.allocator,
                        WorldRenderUtils.FILLED_BOX.getVertexFormatMode(),
                        WorldRenderUtils.FILLED_BOX.getVertexFormat()
                );
                WorldRenderUtils.INSTANCE_WAYPOINTS.drawFilledBoundingBox(event, previewBox, color, previewAlpha() * blink);
                WorldRenderUtils.INSTANCE_WAYPOINTS.drawFilledBoxes(MinecraftClient.getInstance(), WorldRenderUtils.FILLED_BOX);
            }
            WorldRenderUtils.drawEdges(event, previewBox, solidColor(color), 3, true);
            renderPreviewText(event);
        }

        if (hoveredWaypoint != null && hoveredWaypoint != selectedWaypoint) {
            ClientWorld world = MinecraftClient.getInstance().world;
            BlockPos pos = new BlockPos(hoveredWaypoint.x, hoveredWaypoint.y, hoveredWaypoint.z);
            if (world == null || !world.getBlockState(pos).isOf(Blocks.BARRIER)) {
                WorldRenderUtils.drawEdges(event, new Box(pos), solidColor(categoryColor(hoveredWaypoint.getCategory())), 3, true);
            }
        }
    }

    static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!enabled || mode != Mode.FREE_MOVE) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        String modeText = mode == Mode.FREE_MOVE
                ? "you are in waypoint free move mode"
                : "you are in waypoint edit mode";
        ctx.fill(6, 6, 10 + tr.getWidth(modeText), 22, 0xAA000000);
        ctx.drawText(tr, modeText, 10, 10, TEXT, true);

        if (isPreviewOnBarrier()) {
            String warning = "Wynncraft rules do not allow waypoints on barriers, this waypoint preview will not be rendered.";
            int x = (mc.getWindow().getScaledWidth() - tr.getWidth(warning)) / 2;
            int y = mc.getWindow().getScaledHeight() - 36;
            ctx.fill(x - 6, y - 4, x + tr.getWidth(warning) + 6, y + 12, 0xCC220000);
            ctx.drawText(tr, warning, x, y, 0xFFFF7777, true);
        }
    }

    static void ensureSelectionDefaults() {
        if (WaypointData.INSTANCE.packages.isEmpty()) {
            WaypointPackage pkg = new WaypointPackage("Default");
            WaypointData.INSTANCE.packages.add(pkg);
            activePackage = pkg;
            WaypointData.save();
        }

        if (activePackage == null || !WaypointData.INSTANCE.packages.contains(activePackage)) {
            activePackage = WaypointData.INSTANCE.activePackage != null
                    ? WaypointData.INSTANCE.activePackage
                    : WaypointData.INSTANCE.packages.getFirst();
        }

        if (activeCategory != null && !activePackage.categories.contains(activeCategory)) {
            activeCategory = null;
        }
        if (activeCategory == null && !activePackage.categories.isEmpty()) {
            activeCategory = activePackage.categories.getFirst();
        }
    }

    static BlockPos initialPreviewPos(ClientPlayerEntity player) {
        Direction facing = player.getHorizontalFacing();
        BlockPos base = player.getBlockPos().down();
        return base.offset(facing, 3);
    }

    static boolean isPreviewOnBarrier() {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world != null && world.getBlockState(previewPos).isOf(Blocks.BARRIER);
    }

    static Color categoryColor(WaypointCategory category) {
        if (category == null || category.color == null) return Color.CYAN;
        float[] hsb = category.color.asHSB();
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    static Color solidColor(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
    }

    static int categoryColorInt(WaypointCategory category) {
        return category == null || category.color == null ? 0xFFFFFFFF : category.color.asInt();
    }

    static void movePreview(Direction direction) {
        previewPos = previewPos.offset(direction);
        syncCoordinateInputs();
    }

    static void movePreviewVertical(int delta) {
        previewPos = previewPos.add(0, delta, 0);
        syncCoordinateInputs();
    }

    static void setPreviewPos(BlockPos pos) {
        previewPos = pos;
        syncCoordinateInputs();
    }

    static void syncCoordinateInputs() {
        if (focusedCoordinate != 0) xInput = String.valueOf(previewPos.getX());
        if (focusedCoordinate != 1) yInput = String.valueOf(previewPos.getY());
        if (focusedCoordinate != 2) zInput = String.valueOf(previewPos.getZ());
    }

    static void applyCoordinateInputs() {
        try {
            int x = Integer.parseInt(xInput.trim());
            int y = Integer.parseInt(yInput.trim());
            int z = Integer.parseInt(zInput.trim());
            previewPos = new BlockPos(x, y, z);
        } catch (NumberFormatException ignored) {
        }
    }

    static void movePreviewHorizontal(int key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        Direction forward = mc.player.getHorizontalFacing();
        Direction dir = switch (key) {
            case GLFW.GLFW_KEY_W -> forward;
            case GLFW.GLFW_KEY_S -> forward.getOpposite();
            case GLFW.GLFW_KEY_A -> forward.rotateYCounterclockwise();
            case GLFW.GLFW_KEY_D -> forward.rotateYClockwise();
            default -> null;
        };
        if (dir != null) movePreview(dir);
    }

    static void addWaypoint() {
        ensureSelectionDefaults();
        Waypoint waypoint = new Waypoint(previewPos.getX(), previewPos.getY(), previewPos.getZ());
        waypoint.id = UUID.randomUUID().toString();
        waypoint.name = previewName == null || previewName.isBlank() ? "Waypoint" : previewName.trim();
        waypoint.setCategory(activeCategory);
        activePackage.waypoints.add(waypoint);
        selectedWaypoint = null;
        selectedWaypointPackage = null;
        selectedSnapshot = null;
        WaypointData.save();
    }

    static void removeWaypointsAtPreview() {
        ensureSelectionDefaults();
        if (activePackage == null) {
            showEditWarning("No active waypoint package selected.");
            return;
        }

        List<Waypoint> activePackageMatches = activePackage.waypoints.stream()
                .filter(WaypointEditMode::isAtPreviewPos)
                .toList();
        if (!activePackageMatches.isEmpty()) {
            activePackage.waypoints.removeAll(activePackageMatches);
            if (activePackageMatches.contains(selectedWaypoint)) {
                selectedWaypoint = null;
                selectedWaypointPackage = null;
                selectedSnapshot = null;
            }
            WaypointData.save();
            showEditWarning("Removed " + activePackageMatches.size() + " waypoint" + (activePackageMatches.size() == 1 ? "" : "s") + " from " + activePackage.name + ".");
            return;
        }

        List<String> otherPackages = WaypointData.INSTANCE.packages.stream()
                .filter(pkg -> pkg != activePackage)
                .filter(pkg -> pkg.waypoints.stream().anyMatch(WaypointEditMode::isAtPreviewPos))
                .map(pkg -> pkg.name)
                .distinct()
                .toList();
        if (!otherPackages.isEmpty()) {
            showEditWarning("Waypoint exists in other package" + (otherPackages.size() == 1 ? "" : "s") + ": " + String.join(", ", otherPackages) + ".");
            return;
        }

        showEditWarning("No waypoint at this position.");
    }

    static void removeSelectedWaypoint() {
        if (selectedWaypoint == null) {
            removeWaypointsAtPreview();
            return;
        }

        WaypointPackage pkg = selectedWaypointPackage != null ? selectedWaypointPackage : packageOf(selectedWaypoint);
        if (pkg == null) {
            showEditWarning("Selected waypoint package not found.");
            return;
        }

        String name = selectedWaypoint.name == null || selectedWaypoint.name.isBlank() ? "Waypoint" : selectedWaypoint.name;
        pkg.waypoints.remove(selectedWaypoint);
        selectedWaypoint = null;
        selectedWaypointPackage = null;
        selectedSnapshot = null;
        WaypointData.save();
        showEditWarning("Removed " + name + " from " + pkg.name + ".");
    }

    static boolean isAtPreviewPos(Waypoint waypoint) {
        return waypoint.x == previewPos.getX()
                && waypoint.y == previewPos.getY()
                && waypoint.z == previewPos.getZ();
    }

    static void showEditWarning(String warning) {
        editWarning = warning;
        editWarningUntil = System.currentTimeMillis() + 3000;
    }

    static void saveChanges() {
        if (selectedWaypoint == null) {
            addWaypoint();
            return;
        }
        Waypoint waypoint = selectedWaypoint;
        selectedWaypoint.x = previewPos.getX();
        selectedWaypoint.y = previewPos.getY();
        selectedWaypoint.z = previewPos.getZ();
        selectedWaypoint.setCategory(activeCategory);
        if (selectedWaypointPackage != activePackage) {
            if (selectedWaypointPackage != null) selectedWaypointPackage.waypoints.remove(selectedWaypoint);
            activePackage.waypoints.add(selectedWaypoint);
            selectedWaypointPackage = activePackage;
        }
        selectedWaypoint = null;
        selectedWaypointPackage = null;
        selectedSnapshot = null;
        activeCategory = waypoint.getCategory();
        WaypointData.save();
    }

    static void discardChanges() {
        if (selectedWaypoint != null && selectedSnapshot != null) {
            selectedSnapshot.applyTo(selectedWaypoint);
        }
        selectedWaypoint = null;
        selectedWaypointPackage = null;
        selectedSnapshot = null;
        ensureSelectionDefaults();
    }

    static void selectWaypoint(WaypointPackage pkg, Waypoint waypoint) {
        if (selectedWaypoint != null && selectedWaypoint != waypoint && selectedSnapshot != null) {
            selectedSnapshot.applyTo(selectedWaypoint);
        }
        selectedWaypoint = waypoint;
        selectedWaypointPackage = pkg;
        selectedSnapshot = new WaypointSnapshot(waypoint);
        activePackage = pkg;
        activeCategory = waypoint.getCategory();
        previewName = waypoint.name == null || waypoint.name.isBlank() ? "Waypoint" : waypoint.name;
        setPreviewPos(new BlockPos(waypoint.x, waypoint.y, waypoint.z));
    }

    static boolean selectWaypointAtPreview() {
        ensureSelectionDefaults();
        List<WaypointChoice> choices = waypointChoicesAt(previewPos);
        if (choices.size() == 1) {
            WaypointChoice choice = choices.getFirst();
            selectWaypoint(choice.pkg, choice.waypoint);
            return true;
        }
        if (!choices.isEmpty()) return false;
        showEditWarning("No waypoint at this position.");
        return false;
    }

    static boolean selectWaypointAt(int mouseX, int mouseY) {
        WaypointHit hit = findWaypointAt(mouseX, mouseY);
        if (hit == null) return false;
        selectWaypoint(hit.pkg, hit.waypoint);
        return true;
    }

    static List<WaypointChoice> waypointChoicesAt(BlockPos pos) {
        ensureSelectionDefaults();
        List<WaypointChoice> choices = new ArrayList<>();
        if (activePackage != null && WaypointData.INSTANCE.packages.contains(activePackage)) {
            addWaypointChoicesAt(choices, activePackage, pos);
        }
        for (WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if (pkg == activePackage) continue;
            addWaypointChoicesAt(choices, pkg, pos);
        }
        return choices;
    }

    private static void addWaypointChoicesAt(List<WaypointChoice> choices, WaypointPackage pkg, BlockPos pos) {
        for (Waypoint waypoint : pkg.waypoints) {
            if (waypoint.x == pos.getX() && waypoint.y == pos.getY() && waypoint.z == pos.getZ()) {
                choices.add(new WaypointChoice(pkg, waypoint));
            }
        }
    }

    static WaypointHit findWaypointAt(int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        Vec3d origin = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d direction = rayDirectionFromMouse(mouseX, mouseY);
        WaypointHit closest = null;
        for (WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if (!pkg.enabled) continue;
            for (Waypoint waypoint : pkg.waypoints) {
                if (waypoint == selectedWaypoint) continue;
                if (mc.world.getBlockState(new BlockPos(waypoint.x, waypoint.y, waypoint.z)).isOf(Blocks.BARRIER)) continue;

                Box box = new Box(waypoint.x, waypoint.y, waypoint.z, waypoint.x + 1, waypoint.y + 1, waypoint.z + 1);
                double distance = rayIntersectionDistance(origin, direction, box);
                if (Double.isNaN(distance)) continue;
                if (closest == null || distance < closest.distance) closest = new WaypointHit(pkg, waypoint, distance);
            }
        }

        return closest;
    }

    static WaypointPackage packageOf(Waypoint waypoint) {
        for (WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            if (pkg.waypoints.contains(waypoint)) return pkg;
        }
        return null;
    }

    static WaypointPositionStats statsAt(BlockPos pos) {
        int waypoints = 0;
        Set<String> categories = new LinkedHashSet<>();
        Set<String> packages = new LinkedHashSet<>();

        for (WaypointPackage pkg : WaypointData.INSTANCE.packages) {
            boolean packageHasWaypoint = false;
            for (Waypoint waypoint : pkg.waypoints) {
                if (waypoint.x != pos.getX() || waypoint.y != pos.getY() || waypoint.z != pos.getZ()) continue;
                waypoints++;
                packageHasWaypoint = true;
                WaypointCategory category = waypoint.getCategory();
                categories.add(category == null ? "No Category" : category.name);
            }
            if (packageHasWaypoint) packages.add(pkg.name);
        }

        return new WaypointPositionStats(waypoints, new ArrayList<>(categories), new ArrayList<>(packages));
    }

    static void setPreviewName(String name) {
        previewName = name == null || name.isBlank() ? "Waypoint" : name;
    }

    static boolean previewShouldShowBlock() {
        return previewVisibility(selectedWaypoint == null ? null : selectedWaypoint.showOverride, selectedWaypoint == null || selectedWaypoint.show, activeCategory == null || activeCategory.showBlockByDefault);
    }

    static boolean previewShouldShowName() {
        return previewVisibility(selectedWaypoint == null ? null : selectedWaypoint.showNameOverride, selectedWaypoint == null || selectedWaypoint.showName, activeCategory == null || activeCategory.showNameByDefault);
    }

    static boolean previewShouldShowDistance() {
        return previewVisibility(selectedWaypoint == null ? null : selectedWaypoint.showDistanceOverride, selectedWaypoint == null || selectedWaypoint.showDistance, activeCategory == null || activeCategory.showDistanceByDefault);
    }

    static boolean previewVisibility(Boolean override, boolean waypointValue, boolean categoryValue) {
        if (override != null) return override;
        return activeCategory != null ? categoryValue : waypointValue;
    }

    static float previewAlpha() {
        return activeCategory == null ? 0.5f : activeCategory.alpha;
    }

    static boolean previewSeeThrough() {
        return selectedWaypoint != null && selectedWaypoint.seeThrough;
    }

    static void renderPreviewText(RenderWorldEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && previewShouldShowDistance()) {
            WEVec pos = new WEVec(previewPos.getX() + 0.5f, previewPos.getY() + 1.5f, previewPos.getZ() + 0.5f);
            WEVec playerPos = new WEVec(mc.player.getBlockPos().toBottomCenterPos());
            WorldRenderUtils.drawText(event, pos, Text.of((int) pos.distanceTo(playerPos) + "m"), 0.75f, !previewSeeThrough());
        }
        if (previewShouldShowName()) {
            WEVec namePos = new WEVec(previewPos.getX() + 0.5f, previewPos.getY() + 2f, previewPos.getZ() + 0.5f);
            WorldRenderUtils.drawText(event, namePos, Text.of(previewName), 0.75f, !previewSeeThrough());
        }
    }

    static Vec3d rayDirectionFromMouse(int mouseX, int mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int scaledWidth = Math.max(1, mc.getWindow().getScaledWidth());
        int scaledHeight = Math.max(1, mc.getWindow().getScaledHeight());
        int framebufferWidth = Math.max(1, mc.getWindow().getFramebufferWidth());
        int framebufferHeight = Math.max(1, mc.getWindow().getFramebufferHeight());
        float framebufferX = mouseX * framebufferWidth / (float) scaledWidth;
        float framebufferY = framebufferHeight - mouseY * framebufferHeight / (float) scaledHeight;

        Camera camera = mc.gameRenderer.getCamera();
        float fov = ((GameRendererInvoker) mc.gameRenderer).invokeGetFov(camera, renderTickDelta, true);
        Matrix4f projection = mc.gameRenderer.getBasicProjectionMatrix(fov);
        Vector3f origin = new Vector3f();
        Vector3f direction = new Vector3f();
        projection.unprojectRay(framebufferX, framebufferY, new int[] {0, 0, framebufferWidth, framebufferHeight}, origin, direction);
        direction.normalize();
        camera.getRotation().transform(direction);
        return new Vec3d(direction.x, direction.y, direction.z).normalize();
    }

    static double rayIntersectionDistance(Vec3d origin, Vec3d direction, Box box) {
        double tMin = 0;
        double tMax = Double.MAX_VALUE;

        RayRange x = clipRayAxis(origin.x, direction.x, box.minX, box.maxX, tMin, tMax);
        if (x == null) return Double.NaN;
        tMin = x.min;
        tMax = x.max;

        RayRange y = clipRayAxis(origin.y, direction.y, box.minY, box.maxY, tMin, tMax);
        if (y == null) return Double.NaN;
        tMin = y.min;
        tMax = y.max;

        RayRange z = clipRayAxis(origin.z, direction.z, box.minZ, box.maxZ, tMin, tMax);
        if (z == null) return Double.NaN;
        tMin = z.min;
        tMax = z.max;

        return tMax >= tMin ? tMin : Double.NaN;
    }

    static RayRange clipRayAxis(double origin, double direction, double min, double max, double tMin, double tMax) {
        if (Math.abs(direction) < 1.0E-7) return origin >= min && origin <= max ? new RayRange(tMin, tMax) : null;

        double a = (min - origin) / direction;
        double b = (max - origin) / direction;
        double near = Math.min(a, b);
        double far = Math.max(a, b);
        double clippedMin = Math.max(tMin, near);
        double clippedMax = Math.min(tMax, far);
        return clippedMax >= clippedMin && clippedMax >= 0 ? new RayRange(clippedMin, clippedMax) : null;
    }

    record RayRange(double min, double max) {}
    record WaypointHit(WaypointPackage pkg, Waypoint waypoint, double distance) {}
    record WaypointChoice(WaypointPackage pkg, Waypoint waypoint) {}
    record WaypointPositionStats(int waypointCount, List<String> categories, List<String> packages) {}

    static class WaypointSnapshot {
        private final String name;
        private final int x;
        private final int y;
        private final int z;
        private final boolean show;
        private final boolean showName;
        private final boolean showDistance;
        private final boolean seeThrough;
        private final Boolean showOverride;
        private final Boolean showNameOverride;
        private final Boolean showDistanceOverride;
        private final WaypointCategory category;

        private WaypointSnapshot(Waypoint waypoint) {
            this.name = waypoint.name;
            this.x = waypoint.x;
            this.y = waypoint.y;
            this.z = waypoint.z;
            this.show = waypoint.show;
            this.showName = waypoint.showName;
            this.showDistance = waypoint.showDistance;
            this.seeThrough = waypoint.seeThrough;
            this.showOverride = waypoint.showOverride;
            this.showNameOverride = waypoint.showNameOverride;
            this.showDistanceOverride = waypoint.showDistanceOverride;
            this.category = waypoint.getCategory();
        }

        private void applyTo(Waypoint waypoint) {
            waypoint.name = name;
            waypoint.x = x;
            waypoint.y = y;
            waypoint.z = z;
            waypoint.show = show;
            waypoint.showName = showName;
            waypoint.showDistance = showDistance;
            waypoint.seeThrough = seeThrough;
            waypoint.showOverride = showOverride;
            waypoint.showNameOverride = showNameOverride;
            waypoint.showDistanceOverride = showDistanceOverride;
            waypoint.setCategory(category);
        }
    }
}
