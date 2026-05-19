package julianh06.wynnextras.features.raid;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.utils.Pair;
import net.neoforged.bus.api.SubscribeEvent;
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
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public class TreeRoomMinimap {
    private static final int DEFAULT_SIZE = 130;
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

    private static final Map<String, Pair<Integer, Integer>> heartPositionMap = Map.of(
            "Gray", new Pair<>(12, 83),
            "Black", new Pair<>(16, 53),
            "White", new Pair<>(50, 68),
            "Orange", new Pair<>(57, 25),
            "Blue", new Pair<>(95, 49)
    );

    private static final Map<String, Pair<Integer, Integer>> playerPositionMap = Map.of(
            "Gray", new Pair<>(30, 82),
            "Black", new Pair<>(28, 60),
            "White", new Pair<>(50, 54),
            "Orange", new Pair<>(73, 30),
            "Blue", new Pair<>(103, 35),
            "Entrance", new Pair<>(55, 100)
    );

    private static final Map<String, Map<Boolean, Identifier>> pathTextures = Map.of(
        "Gray", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_gray.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/gray_to_entrance.png")),
        "Black", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/gray_to_black.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/black_to_gray.png")),
        "White", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/black_to_white.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/white_to_black.png")),
        "Orange", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/white_to_orange.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/orange_to_white.png")),
        "Blue", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/orange_to_blue.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/blue_to_orange.png")),
        "Entrance", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/blue_to_entrance.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_blue.png")),
        "Outside", Map.of(true, Identifier.of("wynnextras", "textures/treeroomminimap/paths/entrance_to_outside.png"), false, Identifier.of("wynnextras", "textures/treeroomminimap/paths/outside_to_blue.png"))
    );

    private static final Identifier specialPathTexture1 = Identifier.of("wynnextras", "textures/treeroomminimap/paths/special_path_1.png");
    private static final Identifier specialPathTexture2 = Identifier.of("wynnextras", "textures/treeroomminimap/paths/special_path_2.png");

    private static boolean collectedHeart = false;
    private static String player = "";
    private static String playerGrotto = "";
    private static String heartGrotto = "";

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

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String raw = event.message.getString().replaceAll("\u00a7[0-9a-fk-orx]", "");
        handleMessage(raw);
    }

    private static void reset() {
        collectedHeart = false;
        player = "";
        playerGrotto = "";
        heartGrotto = "";
    }

    public static void render(DrawContext context, RenderTickCounter renderTickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;

        if(!config.tnaTreeMap || config.showTreeMapOnlyWhileInsideOfTree && !player.equals(MinecraftClient.getInstance().player.getName().getString())) {
            reset();
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        if(!config.showTreeMapEverywhere && (playerPos == null || playerPos.getX() < 24100 || playerPos.getX() > 24300 || playerPos.getZ() > -22100 || playerPos.getZ() < -22400)) {
            reset();
            return;
        }

        float scale = Math.max(0.3f, Math.min(3.0f, config.tnaTreeMapScale));
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

        renderHeart(heartGrotto, context);
        if(config.showPathsOnTreeMap && MinecraftClient.getInstance().player != null && player.equals(MinecraftClient.getInstance().player.getName().getString())) renderFullPath(context);
        renderPlayer(playerGrotto, context, getSkinTexture(player));

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

    private static void renderHeart(String room, DrawContext context) {
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

    private static void renderPlayer(String room, DrawContext context, Identifier texture) {
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
        if(collectedHeart) {
            if(playerGrotto.equals("Entrance")) {
                return;
            }
            if(playerGrotto.equals("Blue") || playerGrotto.equals("Orange")) {
                if(playerGrotto.equals("Orange")) renderPath(pathTextures.get("Entrance").get(true), context);
                renderPath(pathTextures.get("Outside").get(true), context);
            } else {
                renderPath(pathTextures.get("Gray").get(false), context);
                if(playerGrotto.equals("Gray")) return;
                renderPath(pathTextures.get("Black").get(false), context);
                if(playerGrotto.equals("Black")) return;
                renderPath(pathTextures.get("White").get(false), context);
            }
        } else {
            switch (heartGrotto) {
                case "Blue" -> {
                    switch (playerGrotto) {
                        case "Entrance" -> {
                            renderPath(pathTextures.get("Outside").get(false), context);
                            if (playerGrotto.equals("Entrance")) return;
                            renderPath(pathTextures.get("Gray").get(false), context);
                            return;
                        }
                        case "Gray" -> {
                            renderPath(specialPathTexture1, context);
                            return;
                        }
                        case "Orange" -> {
                            return;
                        }
                    }
                    renderPath(pathTextures.get("Blue").get(true), context);
                    if (playerGrotto.equals("White")) return;
                    renderPath(pathTextures.get("Orange").get(true), context);
                    if (playerGrotto.equals("Black")) return;
                    renderPath(pathTextures.get("White").get(true), context);
                }
                case "Orange" -> {
                    if (playerGrotto.equals("Entrance") || playerGrotto.equals("Gray") || playerGrotto.equals("Blue")) {
                        renderPath(specialPathTexture2, context);
                        if (playerGrotto.equals("Blue")) return;
                        renderPath(specialPathTexture1, context);
                        return;
                    }
                    if (playerGrotto.equals("Orange")) return;
                    renderPath(pathTextures.get("Orange").get(true), context);
                    if (playerGrotto.equals("White")) return;
                    renderPath(pathTextures.get("White").get(true), context);
                    if (playerGrotto.equals("Black")) return;
                    renderPath(pathTextures.get("Black").get(true), context);
                    if (playerGrotto.equals("Gray")) return;
                    renderPath(pathTextures.get("Gray").get(true), context);
                }
                case "White" -> {
                    if (playerGrotto.equals("Blue")) {
                        renderPath(pathTextures.get("Blue").get(false), context);
                        return;
                    }
                    if (playerGrotto.equals("White")) return;
                    renderPath(pathTextures.get("White").get(true), context);
                    if (playerGrotto.equals("Black")) return;
                    renderPath(pathTextures.get("Black").get(true), context);
                    if (playerGrotto.equals("Gray")) return;
                    renderPath(pathTextures.get("Gray").get(true), context);
                }
                case "Black" -> {
                    if (playerGrotto.equals("Blue") || playerGrotto.equals("Orange")) {
                        return;
                    }
                    if (playerGrotto.equals("Black")) return;
                    renderPath(pathTextures.get("Black").get(true), context);
                    if (playerGrotto.equals("Gray")) return;
                    renderPath(pathTextures.get("Gray").get(true), context);
                }
                case "Gray" -> {
                    if (playerGrotto.equals("Blue") || playerGrotto.equals("Orange") || playerGrotto.equals("White")) {
                        return;
                    }
                    if (playerGrotto.equals("Gray")) return;
                    renderPath(pathTextures.get("Gray").get(true), context);
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

        float scale = Math.max(0.3f, Math.min(3.0f, config.tnaTreeMapScale));
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
            xPos = Math.max(0, Math.min(xPos, screenWidth - WIDTH));
            yPos = Math.max(0, Math.min(yPos, screenHeight - 100));
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

        float scale = Math.max(0.3f, Math.min(3.0f, config.tnaTreeMapScale));
        int scaledW = (int) (WIDTH * scale);
        boolean inBounds = mouseX >= xPos - 2 && mouseX <= xPos + scaledW + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + scaledW + 4;
        if (!inBounds) return false;

        float newScale = (float) Math.max(0.3, Math.min(3.0, scale + verticalAmount * 0.1));
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

    private static final Pattern ENTER_TREE_PATTERN =
        Pattern.compile(".*?\\b([A-Za-z0-9_]{3,16}) has entered the tree!$");

    private static final Pattern ENTER_GROTTO_PATTERN =
        Pattern.compile(".*?\\b([A-Za-z0-9_]{3,16}) has entered the (Gray|Black|White|Orange|Blue) Grotto$");

    private static final Pattern HEART_PATTERN =
        Pattern.compile(".*?\\[\\+1 Isoptera Heart]$");

    private static final Pattern DEPOSITED_HEART_PATTERN =
            Pattern.compile(".*?\\[-1 Isoptera Heart]$");

    private static final Pattern ISOPTERA_PATTERN =
        Pattern.compile(".*?The Interdimensional Isoptera is in the (Gray|Black|White|Orange|Blue) Grotto$");

    public static void handleMessage(String message) {
        message = message.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("\uDAFF\uDFFC\uE001\uDB00\uDC06 ", "")
                .trim();

        Matcher treeMatcher = ENTER_TREE_PATTERN.matcher(message);
        if (treeMatcher.matches()) {
            player = treeMatcher.group(1);
            collectedHeart = false;
            playerGrotto = "Entrance";
            return;
        }

        Matcher grottoMatcher = ENTER_GROTTO_PATTERN.matcher(message);
        if (grottoMatcher.matches()) {
            player = grottoMatcher.group(1);
            playerGrotto = grottoMatcher.group(2);
            return;
        }

        Matcher heartMatcher = HEART_PATTERN.matcher(message);
        if (heartMatcher.matches()) {
            collectedHeart = true;
            heartGrotto = "";
            return;
        }

        Matcher depositedHeartMatcher = DEPOSITED_HEART_PATTERN.matcher(message);
        if (depositedHeartMatcher.matches()) {
            reset();
            return;
        }

        Matcher isoMatcher = ISOPTERA_PATTERN.matcher(message);
        if (isoMatcher.matches()) {
            heartGrotto = isoMatcher.group(1);
            return;
        }
    }
}