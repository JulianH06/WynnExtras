package julianh06.wynnextras.features.guildviewer;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.guildviewer.data.GuildData;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.tabs.GeneralTabWidget;
import julianh06.wynnextras.features.profileviewer.tabs.PlayerWidget;
import julianh06.wynnextras.mixin.Accessor.BannerBlockEntityAccessor;
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
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GVScreen extends WEScreen {
    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground.png");
    static Identifier alsobackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground.png");

    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/profileviewerbackground_dark.png");
    static Identifier alsobackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/alsoprofileviewerbackground_dark.png");

    PVScreen.BackgroundImageWidget backgroundImageWidget = new PVScreen.BackgroundImageWidget();

    private static int scrollOffset = 0;

    protected GVScreen(String guild) {
        super(Text.of("guild viewer"));
    }

    @Override
    public void init() {
        super.init();

        rootWidgets.clear();
        addRootWidget(backgroundImageWidget);
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
        registerScrolling();
        //addRootWidget(hier jetzt alle verschiedenen tabs);
    }

    @Override
    public void updateValues() {
//        if(dummy != null) {
//            Identifier dummyTexture = dummy.getSkinTextures().texture();
//            lastViewedPlayersSkins.put(PV.currentPlayerData.getUsername(), dummyTexture);
//        }

        int xStart = getLogicalWidth() / 2 - 900 - (getLogicalWidth() - 1800 < 200 ? 50 : 0);
        int yStart = getLogicalHeight() / 2 - 375;
        backgroundImageWidget.setBounds(xStart, yStart, 1800, 750);

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

    @Override //im drawing the tab stuff in updateValues so the background has to be rendered first that's why this override exists
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

        int xStart = getLogicalWidth() / 2 - 900 - (getLogicalWidth() - 1800 < 200 ? 50 : 0);
        int yStart = getLogicalHeight() / 2 - 374;

        if(GV.currentGuildData == null) return;
        if(GV.currentGuildData.members == null) return;

        int textX = xStart + 1180;
        int spacing = 75;
        int yOffset = 20 + spacing - scrollOffset;
        ui.drawText(GV.currentGuildData.name + " [" + GV.currentGuildData.prefix + "]", xStart, yStart);
        //ui.drawRect(xStart + 565, yStart, 1800 - 565, 100);
        ui.drawCenteredText("Members: " + GV.currentGuildData.members.total + "/" + getMaxMembers(GV.currentGuildData.level) + " Online: " + GV.currentGuildData.online, textX, yStart + 30);

        renderBanner(GV.currentGuildData.banner, context.getMatrices(), 250, 300, 85);

        context.enableScissor(0, (int) ui.sy(yStart + 50), getLogicalWidth(), (int) ui.sy(yStart + 750));
        ui.drawCenteredText("★★★★★ OWNER ★★★★★", textX, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        for(GuildData.Member member : GV.currentGuildData.members.owner.values()) {
            ui.drawCenteredText(member.username, textX, yStart + yOffset);
            yOffset += spacing;
        }

        ui.drawCenteredText("★★★★ CHIEF ★★★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        {
            int i = 0;
            Map<String, GuildData.Member> players = GV.currentGuildData.members.chief;
            for (GuildData.Member member : players.values()) {
                int lastRowAmount = players.keySet().size() % 3;
                if(players.keySet().size() - i <= lastRowAmount) {
                    switch (lastRowAmount) {
                        case 1 -> {
                            ui.drawCenteredText(member.username, textX, yStart + yOffset);
                            continue;
                        }
                        case 2 -> {
                            if(i % 2 == 0) ui.drawCenteredText(member.username, textX - 200, yStart + yOffset);
                            else ui.drawCenteredText(member.username, textX + 200, yStart + yOffset);
                            i++;
                            continue;
                        }
                    }
                }
                switch (i % 3) {
                    case 0 -> ui.drawCenteredText(member.username, textX - 400, yStart + yOffset);
                    case 1 -> ui.drawCenteredText(member.username, textX, yStart + yOffset);
                    case 2 -> ui.drawCenteredText(member.username, textX + 400, yStart + yOffset);
                }
                if((i + 1) % 3 == 0) {
                    yOffset += spacing;
                }
                i++;
            }
            yOffset += spacing;
        }

        ui.drawCenteredText("★★★ STRATEGIST ★★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        {
            int i = 0;
            Map<String, GuildData.Member> players = GV.currentGuildData.members.strategist;
            for (GuildData.Member member : players.values()) {
                int lastRowAmount = players.keySet().size() % 3;
                if(players.keySet().size() - i <= lastRowAmount) {
                    switch (lastRowAmount) {
                        case 1 -> {
                            ui.drawCenteredText(member.username, textX, yStart + yOffset);
                            continue;
                        }
                        case 2 -> {
                            if(i % 2 == 0) ui.drawCenteredText(member.username, textX - 200, yStart + yOffset);
                            else ui.drawCenteredText(member.username, textX + 200, yStart + yOffset);
                            i++;
                            continue;
                        }
                    }
                }
                switch (i % 3) {
                    case 0 -> ui.drawCenteredText(member.username, textX - 400, yStart + yOffset);
                    case 1 -> ui.drawCenteredText(member.username, textX, yStart + yOffset);
                    case 2 -> ui.drawCenteredText(member.username, textX + 400, yStart + yOffset);
                }
                if((i + 1) % 3 == 0) {
                    yOffset += spacing;
                }
                i++;
            }
            yOffset += spacing;
        }

        ui.drawCenteredText("★★ CAPTAIN ★★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        {
            int i = 0;
            Map<String, GuildData.Member> players = GV.currentGuildData.members.captain;
            for (GuildData.Member member : players.values()) {
                int lastRowAmount = players.keySet().size() % 3;
                if(players.keySet().size() - i <= lastRowAmount) {
                    switch (lastRowAmount) {
                        case 1 -> {
                            ui.drawCenteredText(member.username, textX, yStart + yOffset);
                            continue;
                        }
                        case 2 -> {
                            if(i % 2 == 0) ui.drawCenteredText(member.username, textX - 200, yStart + yOffset);
                            else ui.drawCenteredText(member.username, textX + 200, yStart + yOffset);
                            i++;
                            continue;
                        }
                    }
                }
                switch (i % 3) {
                    case 0 -> ui.drawCenteredText(member.username, textX - 400, yStart + yOffset);
                    case 1 -> ui.drawCenteredText(member.username, textX, yStart + yOffset);
                    case 2 -> ui.drawCenteredText(member.username, textX + 400, yStart + yOffset);
                }
                if((i + 1) % 3 == 0) {
                    yOffset += spacing;
                }
                i++;
            }
            yOffset += spacing;
        }

        ui.drawCenteredText("★ RECRUITER ★", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        {
            int i = 0;
            Map<String, GuildData.Member> players = GV.currentGuildData.members.recruiter;
            for (GuildData.Member member : players.values()) {
                int lastRowAmount = players.keySet().size() % 3;
                if(players.keySet().size() - i <= lastRowAmount) {
                    switch (lastRowAmount) {
                        case 1 -> {
                            ui.drawCenteredText(member.username, textX, yStart + yOffset);
                            continue;
                        }
                        case 2 -> {
                            if(i % 2 == 0) ui.drawCenteredText(member.username, textX - 200, yStart + yOffset);
                            else ui.drawCenteredText(member.username, textX + 200, yStart + yOffset);
                            i++;
                            continue;
                        }
                    }
                }
                switch (i % 3) {
                    case 0 -> ui.drawCenteredText(member.username, textX - 400, yStart + yOffset);
                    case 1 -> ui.drawCenteredText(member.username, textX, yStart + yOffset);
                    case 2 -> ui.drawCenteredText(member.username, textX + 400, yStart + yOffset);
                }
                if((i + 1) % 3 == 0) {
                    yOffset += spacing;
                }
                i++;
            }
            yOffset += spacing;
        }

        ui.drawCenteredText("RECRUIT", xStart + 1180, yStart + yOffset, CustomColor.fromHexString("00FFFF"));
        yOffset += spacing;
        {
            int i = 0;
            Map<String, GuildData.Member> players = GV.currentGuildData.members.recruit;
            for (GuildData.Member member : players.values()) {
                int lastRowAmount = players.keySet().size() % 3;
                if(players.keySet().size() - i <= lastRowAmount) {
                    switch (lastRowAmount) {
                        case 1 -> {
                            ui.drawCenteredText(member.username, textX, yStart + yOffset);
                            continue;
                        }
                        case 2 -> {
                            if(i % 2 == 0) ui.drawCenteredText(member.username, textX - 200, yStart + yOffset);
                            else ui.drawCenteredText(member.username, textX + 200, yStart + yOffset);
                            i++;
                            continue;
                        }
                    }
                }
                switch (i % 3) {
                    case 0 -> ui.drawCenteredText(member.username, textX - 400, yStart + yOffset);
                    case 1 -> ui.drawCenteredText(member.username, textX, yStart + yOffset);
                    case 2 -> ui.drawCenteredText(member.username, textX + 400, yStart + yOffset);
                }
                if((i + 1) % 3 == 0) {
                    yOffset += spacing;
                }
                i++;
            }
        }
        context.disableScissor();
    }

    public static void renderBanner(GuildData.Banner banner, MatrixStack matrices, int x, int y, float scale) {
        if(banner == null) return;

        BlockState state = Blocks.WHITE_BANNER.getDefaultState();
        BannerBlockEntity be = new BannerBlockEntity(MinecraftClient.getInstance().player.getBlockPos(), state, dyeColorFromName(banner.base));
        be.setWorld(MinecraftClient.getInstance().world);

        BannerPatternsComponent.Builder builder = new BannerPatternsComponent.Builder();

        for(GuildData.BannerLayer layer : banner.layers) {
            String pattern = layer.pattern.toUpperCase();
            try {
                RegistryEntry<BannerPattern> patternRegistryEntry = resolvePatternEntry(pattern);
                DyeColor color = dyeColorFromName(layer.colour);

                if(patternRegistryEntry != null) builder.add(patternRegistryEntry, color);
            } catch (Exception e) {
                return;
            }
        }

        BannerPatternsComponent patternsComponent = builder.build();
        //ComponentMap map = ComponentMap.builder()
//                .add(DataComponentTypes.BANNER_PATTERNS, patternsComponent)
//                .add(DataComponentTypes.BASE_COLOR, base)
//                .build();

        ((BannerBlockEntityAccessor) be).setPatterns(patternsComponent);
        //be.setComponents(map);
        //System.out.println(be.getPatterns());

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
        if(level < 2) return 4;
        if(level < 6) return 8;
        if(level < 15) return 16;
        if(level < 24) return 26;
        if(level < 33) return 38;
        if(level < 42) return 48;
        if(level < 54) return 60;
        if(level < 66) return 72;
        if(level < 75) return 80;
        if(level < 81) return 86;
        if(level < 87) return 92;
        if(level < 93) return 98;
        if(level < 96) return 102;
        if(level < 99) return 106;
        if(level < 102) return 110;
        if(level < 105) return 115;
        if(level < 108) return 118;
        if(level < 111) return 122;
        if(level < 114) return 126;
        if(level < 117) return 130;
        if(level < 120) return 140;
        return 150;
    }

    public static RegistryEntry<BannerPattern> resolvePatternEntry(String patternName) throws Exception {
        if (patternName == null) return null;

        RegistryEntryLookup<BannerPattern> lookup;
        lookup = MinecraftClient.getInstance().world.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN);

        patternName = patternName.toUpperCase();

        switch (patternName) {

            // ---- Wynncraft → Vanilla Mapping ----
            case "RHOMBUS_MIDDLE":
            case "RHOMBUS":
            case "LOZENGE":
                return lookup.getOrThrow(BannerPatterns.RHOMBUS);

            case "CIRCLE_MIDDLE":
            case "CIRCLE":
            case "ROUNDEL":
                return lookup.getOrThrow(BannerPatterns.CIRCLE);

            case "GRADIENT_UP":
                return lookup.getOrThrow(BannerPatterns.GRADIENT_UP);

            case "GRADIENT":
                return lookup.getOrThrow(BannerPatterns.GRADIENT);

            case "CROSS":
                return lookup.getOrThrow(BannerPatterns.CROSS);

            case "FLOWER":
            case "FLOWER_CHARGE":
                return lookup.getOrThrow(BannerPatterns.FLOWER);

            case "STRAIGHT_CROSS":
                return lookup.getOrThrow(BannerPatterns.STRAIGHT_CROSS);

            case "BASE":
                return lookup.getOrThrow(BannerPatterns.BASE);

            case "BORDER":
                return lookup.getOrThrow(BannerPatterns.BORDER);

            case "BRICKS":
                return lookup.getOrThrow(BannerPatterns.BRICKS);

            case "CURLY_BORDER":
                return lookup.getOrThrow(BannerPatterns.CURLY_BORDER);

            case "DIAGONAL_LEFT":
            case "DIAGONAL":
                return lookup.getOrThrow(BannerPatterns.DIAGONAL_LEFT);

            case "DIAGONAL_RIGHT":
                return lookup.getOrThrow(BannerPatterns.DIAGONAL_RIGHT);

            case "DIAGONAL_UP_RIGHT":
                return lookup.getOrThrow(BannerPatterns.DIAGONAL_UP_RIGHT);

            case "DIAGONAL_UP_LEFT":
                return lookup.getOrThrow(BannerPatterns.DIAGONAL_UP_LEFT);

            case "HALF_HORIZONTAL":
                return lookup.getOrThrow(BannerPatterns.HALF_HORIZONTAL);

            case "HALF_HORIZONTAL_BOTTOM":
                return lookup.getOrThrow(BannerPatterns.HALF_HORIZONTAL_BOTTOM);

            case "HALF_VERTICAL":
                return lookup.getOrThrow(BannerPatterns.HALF_VERTICAL);

            case "HALF_VERTICAL_RIGHT":
                return lookup.getOrThrow(BannerPatterns.HALF_VERTICAL_RIGHT);

            case "STRIPE_BOTTOM":
                return lookup.getOrThrow(BannerPatterns.STRIPE_BOTTOM);

            case "STRIPE_TOP":
                return lookup.getOrThrow(BannerPatterns.STRIPE_TOP);

            case "STRIPE_LEFT":
                return lookup.getOrThrow(BannerPatterns.STRIPE_LEFT);

            case "STRIPE_RIGHT":
                return lookup.getOrThrow(BannerPatterns.STRIPE_RIGHT);

            case "STRIPE_MIDDLE":
                return lookup.getOrThrow(BannerPatterns.STRIPE_MIDDLE);

            case "STRIPE_CENTER":
                return lookup.getOrThrow(BannerPatterns.STRIPE_CENTER);

            case "STRIPE_DOWNLEFT":
                return lookup.getOrThrow(BannerPatterns.STRIPE_DOWNLEFT);

            case "STRIPE_DOWNRIGHT":
                return lookup.getOrThrow(BannerPatterns.STRIPE_DOWNRIGHT);

            case "SMALL_STRIPES":
                return lookup.getOrThrow(BannerPatterns.SMALL_STRIPES);

            case "SQUARE_BOTTOM_LEFT":
                return lookup.getOrThrow(BannerPatterns.SQUARE_BOTTOM_LEFT);

            case "SQUARE_BOTTOM_RIGHT":
                return lookup.getOrThrow(BannerPatterns.SQUARE_BOTTOM_RIGHT);

            case "SQUARE_TOP_LEFT":
                return lookup.getOrThrow(BannerPatterns.SQUARE_TOP_LEFT);

            case "SQUARE_TOP_RIGHT":
                return lookup.getOrThrow(BannerPatterns.SQUARE_TOP_RIGHT);

            case "TRIANGLE_BOTTOM":
                return lookup.getOrThrow(BannerPatterns.TRIANGLE_BOTTOM);

            case "TRIANGLE_TOP":
                return lookup.getOrThrow(BannerPatterns.TRIANGLE_TOP);

            case "TRIANGLES_BOTTOM":
                return lookup.getOrThrow(BannerPatterns.TRIANGLES_BOTTOM);

            case "TRIANGLES_TOP":
                return lookup.getOrThrow(BannerPatterns.TRIANGLES_TOP);

            case "GLOBE":
                return lookup.getOrThrow(BannerPatterns.GLOBE);

            case "CREEPER":
                return lookup.getOrThrow(BannerPatterns.CREEPER);

            case "SKULL":
                return lookup.getOrThrow(BannerPatterns.SKULL);

            case "PIGLIN":
                return lookup.getOrThrow(BannerPatterns.PIGLIN);

            case "FLOW":
                return lookup.getOrThrow(BannerPatterns.FLOW);

            case "GUSTER":
                return lookup.getOrThrow(BannerPatterns.GUSTER);

            case "MOJANG":
                return lookup.getOrThrow(BannerPatterns.MOJANG);

            default:
                System.err.println("[WynnExtras] Unknown banner pattern: " + patternName);
                return null;
        }
    }

    public static class BackgroundImageWidget extends Widget {
        public BackgroundImageWidget() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            //if(currentTab == PVScreen.Tab.General) {
                if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(backgroundTextureDark, x, y, width, height);
                else ui.drawImage(backgroundTexture, x, y, width, height);
            //} else {
            //    if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) ui.drawImage(alsobackgroundTextureDark, x, y, width, height);
            //    else ui.drawImage(alsobackgroundTexture, x, y, width,height);
            //}
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {}

    }

    @Override
    protected void scrollList(float delta) {
        scrollOffset -= (int) (delta);
        if(scrollOffset < 0) scrollOffset = 0;
    }
}
