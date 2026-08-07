package julianh06.wynnextras.features.raid.tna;

import julianh06.wynnextras.utils.text.StyledText;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.render.FontRenderer;
import julianh06.wynnextras.utils.render.RenderUtils;
import julianh06.wynnextras.utils.render.Texture;
import julianh06.wynnextras.utils.render.HorizontalAlignment;
import julianh06.wynnextras.utils.render.TextShadow;
import julianh06.wynnextras.utils.render.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.Pair;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Map;

@WEModule
public class TreeRoomMinimap {
    private static final int DEFAULT_SIZE = 130;
    private static final float OLD_DEFAULT_SCALE = 1.75f;
    private static final float DEFAULT_SCREEN_HEIGHT_RATIO = 0.20f;
    private static final float DEFAULT_SCALE_EPSILON = 0.0001f;
    private static final float MIN_SCALE = 0.3f;
    private static final float MAX_SCALE = 3.0f;
    private static final Texture mapTexture = Texture.WYNN_MAP_TEXTURES;
    private static final Identifier background = Identifier.of("wynnextras", "textures/treeroomminimap/treeroomminimap.png");
    private static final Identifier heart = Identifier.of("wynnextras", "textures/treeroomminimap/heart.png");
    private static final int grooves = 3;

    // Position - loaded from config
    private static int xPos = 5;
    private static int yPos = 5;
    private static final int WIDTH = 126;

    // Dragging state
    private static boolean isDragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private static final Map<Grotto, Pair<Integer, Integer>> heartPositionMap = Map.of(
            Grotto.Gray, new Pair<>(12, 83),
            Grotto.Black, new Pair<>(16, 53),
            Grotto.White, new Pair<>(50, 68),
            Grotto.Orange, new Pair<>(57, 25),
            Grotto.Blue, new Pair<>(95, 49)
    );

    private static final Map<Grotto, Pair<Integer, Integer>> playerPositionMap = Map.of(
            Grotto.Gray, new Pair<>(30, 82),
            Grotto.Black, new Pair<>(28, 60),
            Grotto.White, new Pair<>(50, 54),
            Grotto.Orange, new Pair<>(73, 30),
            Grotto.Blue, new Pair<>(103, 35),
            Grotto.Entrance, new Pair<>(55, 100)
    );

    private static final Map<Grotto, Map<Boolean, Identifier>> pathTextures = Map.of(
            Grotto.Gray, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_gray.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/gray_to_entrance.png")),
            Grotto.Black, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/gray_to_black.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/black_to_gray.png")),
            Grotto.White, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/black_to_white.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/white_to_black.png")),
            Grotto.Orange, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/white_to_orange.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/orange_to_white.png")),
            Grotto.Blue, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/orange_to_blue.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/blue_to_orange.png")),
            Grotto.Entrance, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/blue_to_entrance.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_blue.png")),
            Grotto.Outside, Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_outside.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/outside_to_blue.png"))
    );

    private static final Identifier specialPathTexture1 = Identifier.of("wynnextras", "textures/treeroomminimap/paths/special_path_1.png");
    private static final Identifier specialPathTexture2 = Identifier.of("wynnextras", "textures/treeroomminimap/paths/special_path_2.png");

    private static boolean configLoaded = false;

    private static void loadConfig() {
        if (configLoaded) return;
        syncFromConfig();
    }

    public static void syncFromConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        xPos = config.treeMapX;
        yPos = config.treeMapY;
        configLoaded = true;
    }

    private static void saveConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.treeMapX = xPos;
        config.treeMapY = yPos;
        WynnExtrasConfig.save();
    }

    public static boolean usesDefaultScale(float scale) {
        return Math.abs(scale - OLD_DEFAULT_SCALE) < DEFAULT_SCALE_EPSILON;
    }

    public static float clampScale(float scale) {
        return Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public static float getEffectiveScale() {
        return getEffectiveScale(WynnExtrasConfig.INSTANCE.tnaTreeMapScale);
    }

    public static float getEffectiveScale(float configScale) {
        float scale = configScale;
        if (usesDefaultScale(configScale)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getWindow() != null) {
                scale = mc.getWindow().getScaledHeight() * DEFAULT_SCREEN_HEIGHT_RATIO / DEFAULT_SIZE;
            }
        }
        return clampScale(scale);
    }

    public static void register() {
        HudRenderCallback.EVENT.register((context, renderTickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options.hudHidden) return;
            boolean isInventory = mc.currentScreen instanceof InventoryScreen;
            boolean isChat = mc.currentScreen instanceof ChatScreen;
            if (isInventory || isChat) return;

            TreeRoomMinimap.render(context, renderTickCounter);
        });
    }

    public static void render(DrawContext context, RenderTickCounter renderTickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            TnaApi.reset();
            return;
        }

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;

        if(!config.tnaTreeMap || config.showTreeMapOnlyWhileInsideOfTree && !TnaApi.inTree()) {
            return;
        }

        if(!config.showTreeMapEverywhere && !TnaApi.inTreeRoom()) {
            return;
        }

        float scale = getEffectiveScale();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(xPos, yPos);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-xPos, -yPos);

        RenderUtils.drawTexturedRect(
                context,
                background,
                4 + xPos,
                4 + yPos,
                DEFAULT_SIZE - 8,
                DEFAULT_SIZE - 8,
                0,
                0,
                112,
                112,
                113,
                113);

        renderHeart(TnaApi.getHeartGrotto(), context);
        if(config.showPathsOnTreeMap && MinecraftClient.getInstance().player != null && TnaApi.inTree()) renderFullPath(context);
        renderPlayer(TnaApi.getPlayerGrotto(), context, getSkinTexture(TnaApi.getPlayerInTree()));

        renderOverlay(context);
        context.getMatrices().popMatrix();
    }

    public static void renderOverlay(DrawContext context) {
        float renderX = xPos;
        float renderY = yPos;

        renderMapBorder(context, renderX, renderY, DEFAULT_SIZE, DEFAULT_SIZE);
        FontRenderer.getInstance().renderText(
                context,
                StyledText.fromComponent(WynnExtras.addWynnExtrasPrefix("")),
                renderX + 6,
                renderY + 6,
                CustomColor.fromHexString("FFFFFF"),
                HorizontalAlignment.LEFT,
                VerticalAlignment.TOP,
                TextShadow.OUTLINE,
                1f
        );

        FontRenderer.getInstance().renderText(
                context,
                StyledText.fromComponent(Text.of("Tree Map")),
                renderX + 7,
                renderY + 16,
                CustomColor.fromHexString("FF9900"),
                HorizontalAlignment.LEFT,
                VerticalAlignment.TOP,
                TextShadow.OUTLINE,
                1f
        );
    }

    private static void renderMapBorder(DrawContext guiGraphics, float renderX, float renderY, float width, float height) {
        // Scale to stay the same.
        float groovesWidth = grooves * width / DEFAULT_SIZE;
        float groovesHeight = grooves * height / DEFAULT_SIZE;

        RenderUtils.drawTexturedRect(
            guiGraphics,
            mapTexture,
            renderX - groovesWidth,
            renderY - groovesHeight,
            width + 2 * groovesWidth,
            height + 2 * groovesHeight,
            0,
            0,
            112,
            112,
            mapTexture.width(),
            mapTexture.height());
    }

    private static void renderHeart(Grotto room, DrawContext context) {
        Pair<Integer, Integer> position = heartPositionMap.getOrDefault(room, null);

        if(position == null) return;

        RenderUtils.drawTexturedRect(
                context,
                heart,
                xPos + position.first(),
                yPos + position.second(),
                16,
                16,
                0,
                0,
                112,
                112,
                128,
                128);
    }

    private static void renderPlayer(Grotto room, DrawContext context, Identifier texture) {
        Pair<Integer, Integer> position = playerPositionMap.getOrDefault(room, null);

        if(position == null || texture == null) return;

        RenderUtils.drawTexturedRect(
                context,
                texture,
                xPos + position.getFirst(), yPos + position.getSecond(),
                16, 16,
                8, 8, 8, 8,
                64, 64
        );

        RenderUtils.drawTexturedRect(
                context,
                texture,
                xPos + position.getFirst(), yPos + position.getSecond(),
                16, 16,
                40, 8, 8, 8,
                64, 64
        );
    }

    private static void renderFullPath(DrawContext context) {
        if(TnaApi.hasHeart()) {
            if(TnaApi.getPlayerGrotto().equals(Grotto.Entrance)) {
                return;
            }
            if(TnaApi.getPlayerGrotto().equals(Grotto.Blue) || TnaApi.getPlayerGrotto().equals(Grotto.Orange)) {
                if(TnaApi.getPlayerGrotto().equals(Grotto.Orange)) renderPath(pathTextures.get(Grotto.Entrance).get(true), context);
                renderPath(pathTextures.get(Grotto.Outside).get(true), context);
            } else {
                renderPath(pathTextures.get(Grotto.Gray).get(false), context);
                if(TnaApi.getPlayerGrotto().equals(Grotto.Gray)) return;
                renderPath(pathTextures.get(Grotto.Black).get(false), context);
                if(TnaApi.getPlayerGrotto().equals(Grotto.Black)) return;
                renderPath(pathTextures.get(Grotto.White).get(false), context);
            }
        } else {
            switch (TnaApi.getHeartGrotto()) {
                case Grotto.Blue -> {
                    switch (TnaApi.getPlayerGrotto()) {
                        case Grotto.Entrance -> {
                            renderPath(pathTextures.get(Grotto.Outside).get(false), context);
                            if (TnaApi.getPlayerGrotto().equals(Grotto.Entrance)) return;
                            renderPath(pathTextures.get(Grotto.Gray).get(false), context);
                            return;
                        }
                        case Grotto.Gray -> {
                            renderPath(specialPathTexture1, context);
                            return;
                        }
                        case Grotto.Orange -> {
                            return;
                        }
                    }
                    renderPath(pathTextures.get(Grotto.Blue).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.White)) return;
                    renderPath(pathTextures.get(Grotto.Orange).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Black)) return;
                    renderPath(pathTextures.get(Grotto.White).get(true), context);
                }
                case Grotto.Orange -> {
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Entrance) || TnaApi.getPlayerGrotto().equals(Grotto.Gray) || TnaApi.getPlayerGrotto().equals(Grotto.Blue)) {
                        renderPath(specialPathTexture2, context);
                        if (TnaApi.getPlayerGrotto().equals(Grotto.Blue)) return;
                        renderPath(specialPathTexture1, context);
                        return;
                    }
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Orange)) return;
                    renderPath(pathTextures.get(Grotto.Orange).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.White)) return;
                    renderPath(pathTextures.get(Grotto.White).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Black)) return;
                    renderPath(pathTextures.get(Grotto.Black).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Gray)) return;
                    renderPath(pathTextures.get(Grotto.Gray).get(true), context);
                }
                case Grotto.White -> {
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Blue)) {
                        renderPath(pathTextures.get(Grotto.Blue).get(false), context);
                        return;
                    }
                    if (TnaApi.getPlayerGrotto().equals(Grotto.White)) return;
                    renderPath(pathTextures.get(Grotto.White).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Black)) return;
                    renderPath(pathTextures.get(Grotto.Black).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Gray)) return;
                    renderPath(pathTextures.get(Grotto.Gray).get(true), context);
                }
                case Grotto.Black -> {
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Blue) || TnaApi.getPlayerGrotto().equals(Grotto.Orange)) {
                        return;
                    }
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Black)) return;
                    renderPath(pathTextures.get(Grotto.Black).get(true), context);
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Gray)) return;
                    renderPath(pathTextures.get(Grotto.Gray).get(true), context);
                }
                case Grotto.Gray -> {
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Blue) || TnaApi.getPlayerGrotto().equals(Grotto.Orange) || TnaApi.getPlayerGrotto().equals(Grotto.White)) {
                        return;
                    }
                    if (TnaApi.getPlayerGrotto().equals(Grotto.Gray)) return;
                    renderPath(pathTextures.get(Grotto.Gray).get(true), context);
                }
            }
        }
    }

    private static void renderPath(Identifier path, DrawContext context) {
        RenderUtils.drawTexturedRect(
            context,
            path,
            xPos + 4,
            yPos + 4,
            DEFAULT_SIZE - 8,
            DEFAULT_SIZE - 8,
            0,
            0,
            112,
            112,
            113,
            113);
    }

    private static boolean isInBounds(double mouseX, double mouseY, int[] bounds) {
        return mouseX >= bounds[0] && mouseX <= bounds[2] && mouseY >= bounds[1] && mouseY <= bounds[3];
    }

    public static boolean handleClick(double mouseX, double mouseY, int button, int action, boolean ctrlHeld, boolean shiftHeld) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.tnaTreeMap) return false;

        loadConfig();
        MinecraftClient mc = MinecraftClient.getInstance();

        float scale = getEffectiveScale();
        int scaledW = (int) (WIDTH * scale);
        boolean inBounds = mouseX >= xPos - 2 && mouseX <= xPos + scaledW + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + scaledW + 4;

        if (action == 0) {
            if (button == 0 && isDragging) {
                isDragging = false;
                saveConfig();
                return true;
            }
            return false;
        }

        if (!inBounds) return false;

        boolean inInventoryScreen = mc.currentScreen instanceof InventoryScreen;
        boolean inChatScreen = mc.currentScreen instanceof ChatScreen;
        boolean canInteract = inInventoryScreen || inChatScreen;

        if (action == 1) {
            // Right click while in inventory/chat = start drag (only if not on filter/mode)
            if (button == 0 && canInteract) {
                isDragging = true;
                dragOffsetX = (int) mouseX - xPos;
                dragOffsetY = (int) mouseY - yPos;
                return true;
            }
        }

        return inBounds;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (!isDragging) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            isDragging = false;
            return;
        }

        xPos = (int) mouseX - dragOffsetX;
        yPos = (int) mouseY - dragOffsetY;

        if (mc.getWindow() != null) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            int scaledW = (int) (WIDTH * getEffectiveScale());
            int maxX = Math.max(0, screenWidth - scaledW);
            int maxY = Math.max(0, screenHeight - scaledW);
            xPos = Math.clamp(xPos, 0, maxX);
            yPos = Math.clamp(yPos, 0, maxY);
        }
    }

    public static boolean isDragging() {
        return isDragging;
    }

    public static boolean handleScroll(double mouseX, double mouseY, double verticalAmount) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        if (!config.tnaTreeMap) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean inEditScreen = mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof ChatScreen;
        if (!inEditScreen) return false;

        float scale = getEffectiveScale();
        int scaledW = (int) (WIDTH * scale);
        boolean inBounds = mouseX >= xPos - 2 && mouseX <= xPos + scaledW + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + scaledW + 4;
        if (!inBounds) return false;

        float newScale = clampScale(scale + (float) verticalAmount * 0.1f);
        config.tnaTreeMapScale = newScale;
        WynnExtrasConfig.save();
        return true;
    }

    private static Identifier getSkinTexture(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            return DefaultSkinHelper.getTexture();
        }

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(name);

        if (entry != null) {
            return entry.getSkinTextures().body().texturePath();
        }

        return DefaultSkinHelper.getTexture();
    }

}