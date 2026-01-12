package julianh06.wynnextras.features.guildviewer;

import com.wynntils.screens.partymanagement.widgets.PartyMemberWidget;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.OpenInBrowserButton;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.Searchbar;
import julianh06.wynnextras.features.profileviewer.tabs.GeneralTabWidget;
import julianh06.wynnextras.features.profileviewer.tabs.PlayerWidget;
import julianh06.wynnextras.mixin.Accessor.BannerBlockEntityAccessor;
import julianh06.wynnextras.mixin.Accessor.BannerBlockEntityRendererAccessor;
import julianh06.wynnextras.mixin.Invoker.BannerBlockEntityRendererInvoker;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

import javax.xml.crypto.Data;
import java.sql.Time;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

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
//        lastViewedPlayersWidget.clear();
//        if(currentTabWidget instanceof GeneralTabWidget) {
//            currentTabWidget = null;
//        }
//        if(currentTabWidget == null) currentTabWidget = new GeneralTabWidget();

//        for(PVScreen.TabButtonWidget tabButtonWidget : tabButtonWidgets) {
//            addRootWidget(tabButtonWidget);
//        }
//        for(int i = 0; i < lastViewedPlayers.size(); i++) {
//            PlayerWidget widget = new PlayerWidget(i);
//            lastViewedPlayersWidget.add(widget);
//            addRootWidget(widget);
//        }
//        addedNewest = false;
        //addRootWidget(hier jetzt alle verschiedenen tabs);
    }

    @Override
    public void updateValues() {
//        if(dummy != null) {
//            Identifier dummyTexture = dummy.getSkinTextures().texture();
//            lastViewedPlayersSkins.put(PV.currentPlayerData.getUsername(), dummyTexture);
//        }

        int xStart = getLogicalWidth() / 2 - 900/* - (getLogicalWidth() - 1800 < 200 ? 50 : 0)*/;
        int yStart = getLogicalHeight() / 2 - 375;
        backgroundImageWidget.setBounds(xStart, yStart, 1800, 750);
        darkModeToggleWidget.setBounds(xStart + 1800 - 120, yStart + 750, 120, 60);
//        int totalWidth = 24;
//        for(PVScreen.TabButtonWidget tabButtonWidget : tabButtonWidgets) {
//            int signWidth = drawDynamicNameSign(drawContext, tabButtonWidget.tab.toString(), xStart + totalWidth, yStart - 57);
//            //24; //+ totalXOffset + (float) signWidth / 2
//            tabButtonWidget.setBounds(xStart + totalWidth, yStart - 55, signWidth, 55);
//            tabButtonWidget.setTextOffset(signWidth / 2, 17);
//            totalWidth += signWidth + 12;
//        }
//        if(currentTabWidget == null) return;
//        if(!rootWidgets.contains(currentTabWidget)){
//            addRootWidget(currentTabWidget);
//        }
//        currentTabWidget.setBounds(xStart, yStart, 1800, 750);
//        if(!rootWidgets.contains(currentTabWidget)) {
//            for (int i = 0; i < lastViewedPlayers.size(); i++) {
//                PlayerWidget widget = new PlayerWidget(i);
//                lastViewedPlayersWidget.add(widget);
//                //addRootWidget(widget);
//            }
//        }
//        for(PlayerWidget playerWidget : lastViewedPlayersWidget) {
//            playerWidget.draw(super.drawContext, xStart + currentTabWidget.getWidth(), yStart + 100 * playerWidget.index + 30);
//        }
        //System.out.println(rootWidgets);
//        for(int i = 0; i < lastViewedPlayers.size(); i++) {
//            ui.drawText(lastViewedPlayers.get(i),  + 110, yStart + 100 * i + 55);
//
//            ui.drawImage(playerTabTexture, xStart + currentTabWidget.getWidth(), yStart + 100 * i + 25, 100, 80);
//
//            //to only draw the head
//            RenderUtils.drawTexturedRect(
//                    super.drawContext.getMatrices(),
//                    lastViewedPlayersSkins.get(lastViewedPlayers.get(i)),
//                    ui.sx(xStart + currentTabWidget.getWidth() + 25), ui.sy(yStart + 100 * i + 35), 0,
//                    ui.sw(60), ui.sh(60),
//                    8, 8, 8, 8,
//                    64, 64
//            );
//        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
    }

    @Override
    //im drawing the tab stuff in updateValues so the background has to be rendered first that's why this override exists
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        PVScreen.mouseX = mouseX;
        PVScreen.mouseY = mouseY;
        super.applyBlur();

        this.drawContext = context;
        computeScaleAndOffsets();
        if (ui == null) ui = new UIUtils(context, scaleFactor, xStart, yStart);
        else ui.updateContext(context, scaleFactor, xStart, yStart);

        ui.drawBackground();
        backgroundImageWidget.draw(context, mouseX, mouseY, delta, ui);
        updateValues();
        updateVisibleListRange();
        layoutListElements();

        float snapValue = 0.5f;
        float speed = 0.3f;
        float diff = (targetOffset - actualOffset);
        if(Math.abs(diff) < snapValue || !SimpleConfig.getInstance(WynnExtrasConfig.class).smoothScrollToggle) actualOffset = targetOffset;
        else actualOffset += diff * speed * delta;

        int xStart = getLogicalWidth() / 2 - 900/* - (getLogicalWidth() - 1800 < 200 ? 50 : 0)*/;
        int yStart = getLogicalHeight() / 2 - 374;

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
        int yOffset = spacing - (int) actualOffset - 50;
        ui.drawText("[" + GV.currentGuildData.prefix + "] " + GV.currentGuildData.name, xStart + 19, yStart + 19);

        PVScreen.DarkModeToggleWidget.drawImageWithFade(onlineCircleTextureDark, onlineCircleTexture, xStart + 15, yStart + 60, 33, 33, ui);

        ui.drawText("Online: " + GV.currentGuildData.online + "/" + GV.currentGuildData.members.total, xStart + 57, yStart + 66, CustomColor.fromHexString("FFFFFF"), 3f);

        //ui.drawRect(xStart + 565, yStart, 1800 - 565, 100);
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

        renderBanner(GV.currentGuildData.banner, context.getMatrices(), (int) ui.sx(xStart + 350), (int) ui.sy(yStart + 515), 210 / ui.getScaleFactorF());

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

        ui.drawCenteredText("★★★★★ OWNER ★★★★★", textX, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.owner;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
            count = 1;
            yOffset = result.getRight();
        }

        ui.drawCenteredText("★★★★ CHIEF ★★★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.chief;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
            count = result.getLeft();
            yOffset = result.getRight();
        }

        ui.drawCenteredText("★★★ STRATEGIST ★★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.strategist;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
            count = result.getLeft();
            yOffset = result.getRight();
        }

        ui.drawCenteredText("★★ CAPTAIN ★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.captain;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
            count = result.getLeft();
            yOffset = result.getRight();
        }

        ui.drawCenteredText("★ RECRUITER ★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.recruiter;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
            count = result.getLeft();
            yOffset = result.getRight();
        }

        ui.drawCenteredText("RECRUIT", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += 50;
        {
            Map<String, GuildData.Member> players = GV.currentGuildData.members.recruit;
            Pair<Integer, Integer> result = setWidgetBounds(memeberWidgets, count, players, textX - 175, yStart, yOffset, spacing);
        }
        for(GuildMemeberWidget widget : memeberWidgets) {
            widget.draw(context, mouseX, mouseY, delta, ui);
        }
        context.disableScissor();

        darkModeToggleWidget.draw(context, mouseX, mouseY, delta, ui);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static void renderBanner(GuildData.Banner banner, MatrixStack matrices, int x, int y, float scale) {
        BlockState state = Blocks.WHITE_BANNER.getDefaultState();
        BannerBlockEntity be = new BannerBlockEntity(MinecraftClient.getInstance().player.getBlockPos(), state, banner != null ? dyeColorFromName(banner.base) : DyeColor.WHITE);
        be.setWorld(MinecraftClient.getInstance().world);

        BannerPatternsComponent.Builder builder = new BannerPatternsComponent.Builder();

        if (banner != null) {
            for (GuildData.BannerLayer layer : banner.layers) {
                String pattern = layer.pattern.toUpperCase();
                try {
                    RegistryEntry<BannerPattern> patternRegistryEntry = resolvePatternEntry(pattern);
                    DyeColor color = dyeColorFromName(layer.colour);

                    if (patternRegistryEntry != null) builder.add(patternRegistryEntry, color);
                } catch (Exception e) {
                    return;
                }
            }
        }

        BannerPatternsComponent patternsComponent = builder.build();

        ((BannerBlockEntityAccessor) be).setPatterns(patternsComponent);

        BlockEntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, scale);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(200));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

        VertexConsumerProvider.Immediate buffer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        dispatcher.render(be, 0, matrices, buffer);
        buffer.draw();
        matrices.pop();
    }

    private static DyeColor dyeColorFromName(String base) {
        try {
            return DyeColor.byName(base.toLowerCase(), DyeColor.WHITE);
        } catch (Exception e) {
            return DyeColor.WHITE;
        }
    }

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

    private static Pair<Integer, Integer> setWidgetBounds(List<GuildMemeberWidget> memeberWidgets, int count, Map<String, GuildData.Member> players, int textX, int yStart, int yOffset, int spacing) {
        int widgetHeight = 120;
        int widgetWidth = 350;
        int i = 0;
        boolean setCoordsOfFirstWidgetOfLastRow = false;
        for (GuildData.Member member : players.values()) {
            int lastRowAmount = players.keySet().size() % 3;
            if (players.keySet().size() - i <= lastRowAmount) {
                switch (lastRowAmount) {
                    case 1 -> {
                        memeberWidgets.get(count + i).setBounds(textX, yStart + yOffset, widgetWidth, widgetHeight);
                        yOffset += spacing;
                    }
                    case 2 -> {
                        if (i % 2 == 0) {
                            memeberWidgets.get(count + i).setBounds(textX - 200, yStart + yOffset, widgetWidth, widgetHeight);
                        } else {
                            memeberWidgets.get(count + i).setBounds(textX + 200, yStart + yOffset, widgetWidth, widgetHeight);
                        }
                        if(!setCoordsOfFirstWidgetOfLastRow) {
                            setCoordsOfFirstWidgetOfLastRow = true;
                        } else {
                            yOffset += spacing;
                            setCoordsOfFirstWidgetOfLastRow = false;
                        }
                    }
                }
            } else {
                switch (i % 3) {
                    case 0 ->
                            memeberWidgets.get(count + i).setBounds(textX - 400, yStart + yOffset, widgetWidth, widgetHeight);
                    case 1 ->
                            memeberWidgets.get(count + i).setBounds(textX, yStart + yOffset, widgetWidth, widgetHeight);
                    case 2 ->
                            memeberWidgets.get(count + i).setBounds(textX + 400, yStart + yOffset, widgetWidth, widgetHeight);
                }
            }
            if ((i + 1) % 3 == 0) {
                yOffset += spacing;
            }
            i++;
        }
        yOffset += 50;
        count += i;
        return new Pair<>(count, yOffset);
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
            if(hovered) {
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
}

