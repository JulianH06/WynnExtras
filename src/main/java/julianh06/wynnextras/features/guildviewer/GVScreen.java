package julianh06.wynnextras.features.guildviewer;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.OpenInBrowserButton;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.Searchbar;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static julianh06.wynnextras.utils.UI.UIUtils.*;

public class GVScreen extends WEScreen {
    static Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    static Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");

    static Identifier xpbarborder = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarborder.png");
    static Identifier xpbarborder_dark = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarborder_dark.png");
    static Identifier xpbarbackground = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarbackground.png");
    static Identifier xpbarbackground_dark = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarbackground_dark.png");
    static Identifier xpbarprogress = Identifier.of("wynnextras", "textures/gui/guildviewer/xpbarprogress.png");

    static Identifier openInBrowserButtonTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture.png");
    static Identifier openInBrowserButtonTextureW = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide.png");
    static Identifier openInBrowserButtonTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexture_dark.png");
    static Identifier openInBrowserButtonTextureWDark = Identifier.of("wynnextras", "textures/gui/profileviewer/openinbrowserbuttontexturewide_dark.png");

    static OpenInBrowserButton openInBrowserButton;
    public static Searchbar searchBar;

    PVScreen.BackgroundImageWidget backgroundImageWidget = new PVScreen.BackgroundImageWidget();

    public static PVScreen.DarkModeToggleWidget darkModeToggleWidget = new PVScreen.DarkModeToggleWidget();

    private static float targetOffset = 0;
    private static float actualOffset = 0;
    float maxOffset = 0;

    static ScrollBarWidget scrollBarWidget = null;

    static boolean mouseInMenu = false;

    List<GuildMemeberWidget> memeberWidgets = new ArrayList<>();

    protected GVScreen(String guild) {
        super(Text.of("guild viewer"));
        openInBrowserButton = null;
        searchBar = null;
        PVScreen.currentTab = PVScreen.Tab.General;
    }

    @Override
    public void init() {
        super.init();

        registerScrolling();
        targetOffset = 0;
        actualOffset = 0;
        rootWidgets.clear();
        addRootWidget(backgroundImageWidget);
        addRootWidget(darkModeToggleWidget);
        memeberWidgets = new ArrayList<>();
        openInBrowserButton = null;
        searchBar = null;
    }

    @Override
    public void updateValues() {
        int xStart = getLogicalWidth() / 2 - 900/* - (getLogicalWidth() - 1800 < 200 ? 50 : 0)*/;
        int yStart = getLogicalHeight() / 2 - 375;
        backgroundImageWidget.setBounds(xStart, yStart, 1800, 750);
        darkModeToggleWidget.setBounds(xStart + 1800 - 120, yStart + 750, 120, 60);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    //im drawing the tab stuff in updateValues so the background has to be rendered first that's why this override exists
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mouseInMenu = false;
        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;

        this.drawContext = context;
        computeScaleAndOffsets();
        if (ui == null) ui = new UIUtils(context, scaleFactor, xStart, yStart);
        else ui.updateContext(context, scaleFactor, xStart, yStart);

        ui.drawBackground();
        backgroundImageWidget.draw(context, mouseX, mouseY, delta, ui);
        updateValues();
        updateVisibleListRange();
        layoutListElements();

        targetOffset = ui == null ? 0 : Math.clamp(targetOffset, 0, maxOffset);

        float snapValue = 0.5f;
        float speed = 0.3f;
        float diff = (targetOffset - actualOffset);
        if(Math.abs(diff) < snapValue || !SimpleConfig.getInstance(WynnExtrasConfig.class).smoothScrollToggle || scrollBarWidget.scrollBarButtonWidget.isHeld) actualOffset = targetOffset;
        else actualOffset += diff * speed * delta;
        if(actualOffset < 0) actualOffset = 0;

        int xStart = getLogicalWidth() / 2 - 900/* - (getLogicalWidth() - 1800 < 200 ? 50 : 0)*/;
        int yStart = getLogicalHeight() / 2 - 374;

        if(backgroundImageWidget.contains(mouseX, mouseY)) mouseInMenu = true;

        if(openInBrowserButton == null && GV.currentGuildData != null) {
            openInBrowserButton = new OpenInBrowserButton(-1, -1, (int) (20 * 3 / scaleFactor), (int) (87 * 3 / scaleFactor), "https://wynncraft.com/stats/guild/" + GV.currentGuildData.prefix + "?prefix=true");
        }

        if (openInBrowserButton != null) {
            openInBrowserButton.setX((int) (xStart / scaleFactor));
            openInBrowserButton.setY((int) ((yStart + backgroundImageWidget.getHeight()) / scaleFactor) + 1);
            openInBrowserButton.buttonText = "Open in browser";

            PVScreen.DarkModeToggleWidget.drawImageWithFade(openInBrowserButtonTextureDark, openInBrowserButtonTexture, xStart, yStart + backgroundImageWidget.getHeight(), 260, 60, ui);
            openInBrowserButton.drawWithTexture(context, null);
        }

        //Player searchbar
        PVScreen.DarkModeToggleWidget.drawImageWithFade(openInBrowserButtonTextureWDark, openInBrowserButtonTextureW, xStart + 267, yStart + 750, 300, 60, ui);

        if(searchBar == null || searchBar.getInput().equals("Unknown guild")) {
            searchBar = new Searchbar(-1, -1, (int) (14 * 3 / scaleFactor), (int) (100 * 3 / scaleFactor));
            if(GV.currentGuildData == null) {
                searchBar.setInput("Unknown guild");
            } else if(GV.currentGuildData.prefix == null) {
                searchBar.setInput("Unknown guild");
            } else {
                searchBar.setInput(GV.currentGuildData.prefix);
            }
        }

        if (searchBar != null) {
            searchBar.setX((int) ((xStart + 89 * 3) / ui.getScaleFactor()));
            searchBar.setY((int) ((yStart + backgroundImageWidget.getHeight() + 20) / scaleFactor) + 1);
            searchBar.drawWithoutBackground(context, CustomColor.fromHexString("FFFFFF"));
        }

        if (GV.currentGuildData == null) return;
        if (GV.currentGuildData.members == null) return;

        int textX = xStart + 1180;
        int spacing = 150;

        int contentHeight = 100;

        int yOffset = -(int) actualOffset;

        ui.drawText("[" + GV.currentGuildData.prefix + "] " + GV.currentGuildData.name, xStart + 19, yStart + 19);

        PVScreen.DarkModeToggleWidget.drawImageWithFade(onlineCircleTextureDark, onlineCircleTexture, xStart + 15, yStart + 60, 33, 33, ui);

        ui.drawText("Online: " + GV.currentGuildData.online + "/" + GV.currentGuildData.members.total, xStart + 57, yStart + 66, CustomColor.fromHexString("FFFFFF"), 3f);

        ui.drawCenteredText("Members: " + GV.currentGuildData.members.total + "/" + getMaxMembers(GV.currentGuildData.level), textX, yStart + 30);

        ui.drawCenteredText("Level " + GV.currentGuildData.level, xStart + 285, yStart + 590);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(xpbarbackground_dark, xpbarbackground, xStart + 66, yStart + 540, 435, 30, ui);

        context.enableScissor((int) ui.sx(xStart + 66), (int) ui.sy(yStart + 540), (int) ui.sx(xStart + 66 + 435 * (GV.currentGuildData.xpPercent / 100f)), (int) ui.sy(yStart + 540 + 35));
        ui.drawImage(xpbarprogress, xStart + 66, yStart + 540, 435, 30);
        context.disableScissor();

        PVScreen.DarkModeToggleWidget.drawImageWithFade(xpbarborder_dark, xpbarborder, xStart + 66, yStart + 540, 435, 30, ui);
        ui.drawCenteredText(GV.currentGuildData.xpPercent + "%", xStart + 285, yStart + 540 + 17, CustomColor.fromHexString("FFFFFF"), 2.5f);

        Instant instant = Instant.parse(GV.currentGuildData.created);
        ZoneId zone = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .withZone(zone);

        String formatted = formatter.format(instant);
        ui.drawCenteredText("Created: " + formatted, xStart + 285, yStart + 630);

        DecimalFormat formatter2 = new DecimalFormat("#,###");
        ui.drawCenteredText("Total wars: " + formatter2.format(GV.currentGuildData.wars), xStart + 285, yStart + 670);

        ui.drawCenteredText("Current territories: " + GV.currentGuildData.territories, xStart + 285, yStart + 710);

        //renderBanner(GV.currentGuildData.banner, context.getMatrices(), (int) ui.sx(xStart + 350), (int) ui.sy(yStart + 515), 210 / ui.getScaleFactorF());

        if (memeberWidgets.isEmpty()) {
            for (GuildData.Member member : GV.currentGuildData.members.owner.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
            for (GuildData.Member member : GV.currentGuildData.members.chief.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
            for (GuildData.Member member : GV.currentGuildData.members.strategist.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
            for (GuildData.Member member : GV.currentGuildData.members.captain.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
            for (GuildData.Member member : GV.currentGuildData.members.recruiter.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
            for (GuildData.Member member : GV.currentGuildData.members.recruit.values()) {
                memeberWidgets.add(new GuildMemeberWidget(member, this));
            }
        }

        int count = 0;

        context.enableScissor(0, (int) ui.sy(yStart + 50), getLogicalWidth(), (int) ui.sy(yStart + 738));

        ui.drawCenteredText("★★★★★ OWNER ★★★★★", textX, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.owner,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            count = result.getLeft();
            contentHeight += result.getRight();
        }

        contentHeight += 25;
        ui.drawCenteredText("★★★★ CHIEF ★★★★", xStart + 1180, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.chief,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            count = result.getLeft();
            contentHeight += result.getRight();
        }

        contentHeight += 25;
        ui.drawCenteredText("★★★ STRATEGIST ★★★", xStart + 1180, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.strategist,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            count = result.getLeft();
            contentHeight += result.getRight();
        }

        contentHeight += 25;
        ui.drawCenteredText("★★ CAPTAIN ★★", xStart + 1180, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.captain,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            count = result.getLeft();
            contentHeight += result.getRight();
        }

        contentHeight += 25;
        ui.drawCenteredText("★ RECRUITER ★", xStart + 1180, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.recruiter,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            count = result.getLeft();
            contentHeight += result.getRight();
        }

        contentHeight += 25;
        ui.drawCenteredText("RECRUIT", xStart + 1180, yStart + yOffset + contentHeight, CustomColor.fromHexString("00FFFF"));
        contentHeight += 50;
        {
            Pair<Integer, Integer> result = setWidgetBounds(
                    memeberWidgets,
                    count,
                    GV.currentGuildData.members.recruit,
                    textX - 175,
                    yStart,
                    yOffset + contentHeight,
                    spacing
            );

            contentHeight += result.getRight();
        }

        for(GuildMemeberWidget widget : memeberWidgets) {
            widget.draw(context, mouseX, mouseY, delta, ui);
        }
        context.disableScissor();

        darkModeToggleWidget.draw(context, mouseX, mouseY, delta, ui);

        int visibleHeight = 738 - 50;
        maxOffset = Math.max(
                0,
                contentHeight - visibleHeight
        );


        if(scrollBarWidget == null) {
            scrollBarWidget = new ScrollBarWidget(maxOffset);
        }

        scrollBarWidget.maxOffset = maxOffset;

        scrollBarWidget.setBounds(xStart + 1820, yStart, 30, 750);
        scrollBarWidget.draw(context, mouseX, mouseY, delta, ui);
    }

    @Override
    public void close() {
        GV.currentGuild = "";
        GV.currentGuildData = null;
        openInBrowserButton = null;
        searchBar = null;
        super.close();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if(scrollBarWidget != null) scrollBarWidget.mouseClicked(mouseX, mouseY, button);
        if(openInBrowserButton == null || searchBar == null || darkModeToggleWidget == null) return false;

        if(darkModeToggleWidget.contains(PVScreen.mouseX, PVScreen.mouseY)) {
            darkModeToggleWidget.action.run();
            return false;
        }

        if(openInBrowserButton.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            openInBrowserButton.click();
            return false;
        }
        if(searchBar != null) {
            if (searchBar.isClickInBounds(PVScreen.mouseX, PVScreen.mouseY)) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                searchBar.click();
                return false;
            } else {
                searchBar.setActive(false);
            }
        }
        for(GuildMemeberWidget widget : memeberWidgets) {
            widget.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if(scrollBarWidget != null) scrollBarWidget.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(click);
    }

//    public static void renderBanner(GuildData.Banner banner, MatrixStack matrices, int x, int y, float scale) {
//        BlockState state = Blocks.WHITE_BANNER.getDefaultState();
//        BannerBlockEntity be = new BannerBlockEntity(MinecraftClient.getInstance().player.getBlockPos(), state, banner != null ? dyeColorFromName(banner.base) : DyeColor.WHITE);
//        be.setWorld(MinecraftClient.getInstance().world);
//
//        BannerPatternsComponent.Builder builder = new BannerPatternsComponent.Builder();
//
//        if (banner != null) {
//            for (GuildData.BannerLayer layer : banner.layers) {
//                String pattern = layer.pattern.toUpperCase();
//                try {
//                    RegistryEntry<BannerPattern> patternRegistryEntry = resolvePatternEntry(pattern);
//                    DyeColor color = dyeColorFromName(layer.colour);
//
//                    if (patternRegistryEntry != null) builder.add(patternRegistryEntry, color);
//                } catch (Exception e) {
//                    return;
//                }
//            }
//        }
//
//        BannerPatternsComponent patternsComponent = builder.build();
//
//        ((BannerBlockEntityAccessor) be).setPatterns(patternsComponent);
//
//        BlockEntityRenderManager dispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();
//
//        matrices.push();
//        matrices.translate(x, y, 0);
//        matrices.scale(scale, scale, scale);
//
//        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(200));
//        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
//
//        VertexConsumerProvider.Immediate buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
//        dispatcher.render(be, 0, matrices, buffer);
//        buffer.draw();
//        matrices.pop();
//    }

//    private static DyeColor dyeColorFromName(String base) {
//        try {
//            return DyeColor.byName(base.toLowerCase(), DyeColor.WHITE);
//        } catch (Exception e) {
//            return DyeColor.WHITE;
//        }
//    }

    private static int getMaxMembers(int level) {
        if (level < 2) return 4;
        if (level < 6) return 8;
        if (level < 15) return 16;
        if (level < 24) return 26;
        if (level < 33) return 38;
        if (level < 42) return 48;
        if (level < 54) return 60;
        if (level < 66) return 72;
        if (level < 75) return 80;
        if (level < 81) return 86;
        if (level < 87) return 92;
        if (level < 93) return 98;
        if (level < 96) return 102;
        if (level < 99) return 106;
        if (level < 102) return 110;
        if (level < 105) return 115;
        if (level < 108) return 118;
        if (level < 111) return 122;
        if (level < 114) return 126;
        if (level < 117) return 130;
        if (level < 120) return 140;
        return 150;
    }

    public static RegistryEntry<BannerPattern> resolvePatternEntry(String patternName) throws Exception {

        if (patternName == null) return null;

        RegistryEntryLookup<BannerPattern> lookup;
        lookup = MinecraftClient.getInstance().world.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN);

        patternName = patternName.toUpperCase();

        return switch (patternName) {
            case "BASE" -> lookup.getOrThrow(BannerPatterns.BASE);
            case "BORDER" -> lookup.getOrThrow(BannerPatterns.BORDER);
            case "BRICKS" -> lookup.getOrThrow(BannerPatterns.BRICKS);
            case "CIRCLE", "CIRCLE_MIDDLE" -> lookup.getOrThrow(BannerPatterns.CIRCLE);
            case "CREEPER" -> lookup.getOrThrow(BannerPatterns.CREEPER);
            case "CROSS" -> lookup.getOrThrow(BannerPatterns.CROSS);
            case "CURLY_BORDER" -> lookup.getOrThrow(BannerPatterns.CURLY_BORDER);
            case "DIAGONAL_LEFT", "DIAGONAL_UP_LEFT", "DIAGONAL_LEFT_MIRROR" ->
                    lookup.getOrThrow(BannerPatterns.DIAGONAL_UP_LEFT);
            case "DIAGONAL_RIGHT", "DIAGONAL_UP_RIGHT", "DIAGONAL_RIGHT_MIRROR" ->
                    lookup.getOrThrow(BannerPatterns.DIAGONAL_UP_RIGHT);
            case "FLOWER" -> lookup.getOrThrow(BannerPatterns.FLOWER);
            case "GLOBE" -> lookup.getOrThrow(BannerPatterns.GLOBE);
            case "GRADIENT" -> lookup.getOrThrow(BannerPatterns.GRADIENT);
            case "GRADIENT_UP" -> lookup.getOrThrow(BannerPatterns.GRADIENT_UP);
            case "HALF_HORIZONTAL", "HALF_HORIZONTAL_BOTTOM", "HALF_HORIZONTAL_MIRROR" ->
                    lookup.getOrThrow(BannerPatterns.HALF_HORIZONTAL_BOTTOM);
            case "HALF_VERTICAL", "HALF_VERTICAL_RIGHT", "HALF_VERTICAL_MIRROR" ->
                    lookup.getOrThrow(BannerPatterns.HALF_VERTICAL_RIGHT);
            case "MOJANG" -> lookup.getOrThrow(BannerPatterns.MOJANG);
            case "PIGLIN" -> lookup.getOrThrow(BannerPatterns.PIGLIN);
            case "RHOMBUS", "RHOMBUS_MIDDLE" -> lookup.getOrThrow(BannerPatterns.RHOMBUS);
            case "SKULL" -> lookup.getOrThrow(BannerPatterns.SKULL);
            case "SMALL_STRIPES", "STRIPE_SMALL" -> lookup.getOrThrow(BannerPatterns.SMALL_STRIPES);
            case "SQUARE_BOTTOM_LEFT" -> lookup.getOrThrow(BannerPatterns.SQUARE_BOTTOM_LEFT);
            case "SQUARE_BOTTOM_RIGHT" -> lookup.getOrThrow(BannerPatterns.SQUARE_BOTTOM_RIGHT);
            case "SQUARE_TOP_LEFT" -> lookup.getOrThrow(BannerPatterns.SQUARE_TOP_LEFT);
            case "SQUARE_TOP_RIGHT" -> lookup.getOrThrow(BannerPatterns.SQUARE_TOP_RIGHT);
            case "STRAIGHT_CROSS" -> lookup.getOrThrow(BannerPatterns.STRAIGHT_CROSS);
            case "STRIPE_BOTTOM" -> lookup.getOrThrow(BannerPatterns.STRIPE_BOTTOM);
            case "STRIPE_CENTER" -> lookup.getOrThrow(BannerPatterns.STRIPE_CENTER);
            case "STRIPE_DOWNLEFT" -> lookup.getOrThrow(BannerPatterns.STRIPE_DOWNLEFT);
            case "STRIPE_DOWNRIGHT" -> lookup.getOrThrow(BannerPatterns.STRIPE_DOWNRIGHT);
            case "STRIPE_LEFT" -> lookup.getOrThrow(BannerPatterns.STRIPE_LEFT);
            case "STRIPE_MIDDLE" -> lookup.getOrThrow(BannerPatterns.STRIPE_MIDDLE);
            case "STRIPE_RIGHT" -> lookup.getOrThrow(BannerPatterns.STRIPE_RIGHT);
            case "STRIPE_TOP" -> lookup.getOrThrow(BannerPatterns.STRIPE_TOP);
            case "TRIANGLE_BOTTOM" -> lookup.getOrThrow(BannerPatterns.TRIANGLE_BOTTOM);
            case "TRIANGLE_TOP" -> lookup.getOrThrow(BannerPatterns.TRIANGLE_TOP);
            case "TRIANGLES_BOTTOM" -> lookup.getOrThrow(BannerPatterns.TRIANGLES_BOTTOM);
            case "TRIANGLES_TOP" -> lookup.getOrThrow(BannerPatterns.TRIANGLES_TOP);
            default -> {
                System.err.println("[WynnExtras] Unknown banner pattern: " + patternName);
                yield null;
            }
        };
    }

    private static Pair<Integer, Integer> setWidgetBounds(
            List<GuildMemeberWidget> memberWidgets,
            int startIndex,
            Map<String, GuildData.Member> players,
            int centerX,
            int yStart,
            int yOffset,
            int spacing
    ) {
        int widgetHeight = 120;
        int widgetWidth = 350;

        int index = 0;
        int row = 0;

        int total = players.size();
        int fullRows = total / 3;
        int lastRowCount = total % 3;

        for (GuildData.Member ignored : players.values()) {
            int col = index % 3;
            int x;

            boolean isLastRow = row == fullRows && lastRowCount != 0;

            if (isLastRow) {
                if (lastRowCount == 1) {
                    x = centerX;
                } else {
                    x = (col == 0)
                            ? centerX - 200
                            : centerX + 200;
                }
            } else {
                x = switch (col) {
                    case 0 -> centerX - 400;
                    case 1 -> centerX;
                    default -> centerX + 400;
                };
            }

            memberWidgets
                    .get(startIndex + index)
                    .setBounds(x, yStart + yOffset + row * spacing, widgetWidth, widgetHeight);

            index++;

            if (index % 3 == 0) {
                row++;
            }
        }

        int rowsUsed = fullRows + (lastRowCount > 0 ? 1 : 0);
        int heightUsed = rowsUsed * spacing;

        return new Pair<>(startIndex + total, heightUsed);
    }

    @Override
    protected void scrollList(float delta) {
        if (delta > 0) {
            targetOffset -= 40f;
        } else {
            targetOffset += 40f;
        }

        if(targetOffset < 0) targetOffset = 0;
    }

    private static class GuildMemeberWidget extends Widget {
        static Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
        static Identifier classBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive_dark.png");

        static Identifier classBackgroundTextureHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered.png");
        static Identifier classBackgroundTextureHoveredDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered_dark.png");

        static Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
        static Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");

        private final Runnable action;

        GuildData.Member member;

        public GuildMemeberWidget(GuildData.Member member, GVScreen parent) {
            super(0, 0, 0, 0);
            this.member = member;
            this.action = () -> {
                if(!mouseInMenu) return;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                parent.close();
                PV.open(member.username);
            };
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(hovered && mouseInMenu) {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureHoveredDark, classBackgroundTextureHovered, x, y, width, height, ui);
            } else {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureDark, classBackgroundTexture,  x, y, width, height, ui);
            }
            //ui.drawRect(x, y, width, height);
            ui.drawCenteredText(member.username, x + 175, y + 25);

            Instant instant = Instant.parse(member.joined);
            ZoneId zone = ZoneId.systemDefault();
            DateTimeFormatter formatter = DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                    .withZone(zone);

            String formatted = formatter.format(instant);
            ui.drawCenteredText("Joined: " + formatted, x + 175, y + 55);

            ui.drawCenteredText("Contributed: " + formatLong(member.contributed), x + 175, y + 85);

            if(member.online) {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(onlineCircleTextureDark, onlineCircleTexture, x + 5, y + 5, 20, 20, ui);
            }
        }

        public static String formatLong(long value) {
            if (value < 1_000) {
                return String.valueOf(value);
            } else if (value < 1_000_000) {
                return String.format("%.2fk", value / 1_000.0);
            } else if (value < 1_000_000_000) {
                return String.format("%.2fM", value / 1_000_000.0);
            } else if (value < 1_000_000_000_000L) {
                return String.format("%.2fB", value / 1_000_000_000.0);
            } else {
                return String.format("%.2fT", value / 1_000_000_000_000.0);
            }
        }

    }

    private static class ScrollBarWidget extends Widget {
        ScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseY = 0;
        public float maxOffset;

        public ScrollBarWidget(float maxOffset) {
            super(0, 0, 0, 0);
            this.scrollBarButtonWidget = new ScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
            this.maxOffset = maxOffset;
        }

        private void setOffset(int mouseY, float maxOffset, int scrollAreaHeight) {
            float relativeY = mouseY * ui.getScaleFactorF() - y - scrollBarButtonWidget.getHeight() / 2f;
            relativeY = Math.max(-1.15f * ui.getScaleFactorF(), Math.min(relativeY, scrollAreaHeight));

            float scrollPercent = relativeY / scrollAreaHeight;

            targetOffset = scrollPercent * maxOffset;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;

            int scale = 5;

            ui.drawSliderBackground(x, y, width, height, scale, false);

            if (PVScreen.DarkModeToggleWidget.fade > 0.001f) {
                RenderUtils.drawRect(
                        ctx,
                        CustomColor.fromHexString("1b1b1c").withAlpha(PVScreen.DarkModeToggleWidget.fade),
                        ui.sx(x + scale) - 1,
                        ui.sy(y + scale) - 1,
                        ui.sw(width - scale * 2) + 2,
                        ui.sh(height - scale * 2) + 2
                );
            }

            ui.drawButtonTextures(
                    x, y, width, height, scale,
                    SimpleConfig.getInstance(WynnExtrasConfig.class).pvDarkmodeToggle,
                    sliderButtontlDark, sliderButtontrDark, sliderButtonblDark, sliderButtonbrDark,
                    sliderButtontopDark, sliderButtonbotDark, sliderButtonleftDark, sliderButtonrightDark,
                    sliderButtontl, sliderButtontr, sliderButtonbl, sliderButtonbr,
                    sliderButtontop, sliderButtonbot, sliderButtonleft, sliderButtonright, 1
            );

            updateScrollButton(mouseY);
        }

        private void updateScrollButton(int mouseY) {
            int buttonHeight = 50;
            int scrollAreaHeight = height - buttonHeight;

            if (scrollBarButtonWidget.isHeld) {
                setOffset(mouseY, maxOffset, scrollAreaHeight);
                actualOffset = targetOffset;
            }

            float percent = maxOffset == 0 ? 0 : actualOffset / maxOffset;
            percent = Math.clamp(percent, 0f, 1f);

            int yPos = y + (int) (scrollAreaHeight * percent);
            scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            int buttonHeight = 30;
            int scrollAreaHeight = height - buttonHeight;

            setOffset(currentMouseY, maxOffset, scrollAreaHeight);

            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarButtonWidget.mouseReleased(mx, my, button);
            return true;
        }

        private static class ScrollBarButtonWidget extends Widget {
            public boolean isHeld;

            public ScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHeld = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButtonFade(x, y, width, height, 5, hovered || isHeld);
            }

            @Override
            protected boolean onClick(int button) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                isHeld = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                isHeld = false;
                return true;
            }
        }
    }
}

