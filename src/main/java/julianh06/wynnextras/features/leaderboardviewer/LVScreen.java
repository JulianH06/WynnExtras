package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.Guild;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class LVScreen extends WEScreen {
    @Override protected double getTargetScaleFactor() { return 2.0; }
    @Override protected int getMinLogicalWidth()  { return 2100; }
    @Override protected int getMinLogicalHeight() { return 870; }

    private final PVScreen.DarkModeToggleWidget darkModeToggleWidget = new PVScreen.DarkModeToggleWidget();
    private final ActionButtonWidget browserButton = new ActionButtonWidget(
            "Open in browser", this::openCurrentLeaderboardInBrowser);
    private final ActionButtonWidget reloadButton = new ActionButtonWidget("Reload", this::reloadCurrentLeaderboard);
    private final AutoRefreshWidget autoRefreshWidget = new AutoRefreshWidget();
    private static final SessionState SESSION_STATE = new SessionState();
    private static final long AUTO_REFRESH_INTERVAL_MS = 60_000;

    public enum Type { Player, Guild, Gamemode }
    private Type currentType;
    private TypeWidget currentTypeWidget;
    private final List<TypeButtonWidget> typeButtonWidgets = new ArrayList<>();
    private final Map<Type, TypeWidget> typeWidgets = new EnumMap<>(Type.class);
    private long nextAutoRefreshAt = System.currentTimeMillis() + AUTO_REFRESH_INTERVAL_MS;
    private String refreshLeaderboardId;

    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/background.png");
    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/background_dark.png");
    static Identifier signLeftTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_left.png");
    static Identifier signMiddleTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_middle.png");
    static Identifier signRightTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_right.png");
    static Identifier signLeftTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_left_dark.png");
    static Identifier signMiddleTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_middle_dark.png");
    static Identifier signRightTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/sign_right_dark.png");

    protected LVScreen() {
        this(null);
    }

    protected LVScreen(String initialLeaderboardId) {
        super(Text.of("leaderboard viewer"));
        currentType = initialLeaderboardId == null ? SESSION_STATE.currentType : Type.Player;

        int j = 0;
        for(Type type : Type.values()) {
            typeButtonWidgets.add(new TypeButtonWidget(j, type, this));
            TypeWidget typeWidget = new TypeWidget(type, LeaderboardCatalog.categoriesFor(type));
            typeWidgets.put(type, typeWidget);
            if (initialLeaderboardId == null) typeWidget.restoreState(SESSION_STATE.states.get(type));
            j++;
        }
        String playerName = MinecraftUtils.playerName();
        setOwnIdentity(playerName, null, null);
        if (playerName != null && !playerName.isBlank()) {
            WynncraftApiHandler.fetchPlayerData(playerName).thenAccept(playerData -> {
                if (playerData == null) return;
                Guild guild = playerData.getGuild();
                MinecraftClient.getInstance().execute(() -> setOwnIdentity(
                        playerName,
                        guild == null ? null : guild.getName(),
                        guild == null ? null : guild.getPrefix()));
            });
        }
        LeaderboardService.fetchLeaderboardTypes().thenAccept(types ->
                MinecraftClient.getInstance().execute(() -> {
                    TypeWidget guildWidget = typeWidgets.get(Type.Guild);
                    guildWidget.setCategories(LeaderboardCatalog.guildCategories(types));
                    TypeWidget.State savedGuildState = SESSION_STATE.states.get(Type.Guild);
                    if (initialLeaderboardId == null && guildWidget.getActiveLeaderboard() == null
                            && savedGuildState.leaderboardId() != null) {
                        guildWidget.restoreState(savedGuildState);
                    }
                    if (initialLeaderboardId != null && guildWidget.selectLeaderboardById(initialLeaderboardId)) {
                        if (currentTypeWidget == null) currentType = Type.Guild;
                        else selectType(Type.Guild);
                    }
                }));
        GuildSeasonService.fetchSeasons().thenAccept(seasons ->
                MinecraftClient.getInstance().execute(() ->
                        typeWidgets.get(Type.Guild).setGuildSeasons(seasons)));
        if (initialLeaderboardId != null) {
            for (Type type : Type.values()) {
                if (!typeWidgets.get(type).selectLeaderboardById(initialLeaderboardId)) continue;
                currentType = type;
                break;
            }
        }
    }

    @Override
    public void init() {
        super.init();

        rootWidgets.clear();
        addRootWidget(darkModeToggleWidget);
        addRootWidget(browserButton);
        addRootWidget(reloadButton);
        addRootWidget(autoRefreshWidget);
        for(TypeButtonWidget typeButtonWidget : typeButtonWidgets) {
            addRootWidget(typeButtonWidget);
        }
        currentTypeWidget = getTypeWidget(currentType);
    }

    @Override
    public void updateValues() {
        int xStart = getLogicalWidth() / 2 - 900;
        int yStart = getLogicalHeight() / 2 - 375;
        darkModeToggleWidget.setBounds(xStart + 1800 - 120, yStart + 750, 120, 60);
        browserButton.setBounds(xStart, yStart + 758, 260, 48);
        reloadButton.setBounds(xStart + 272, yStart + 758, 150, 48);
        autoRefreshWidget.setBounds(xStart + 440, yStart + 758, 300, 48);

        int totalHeight = 30;
        for(TypeButtonWidget typeButtonWidget : typeButtonWidgets) {
            typeButtonWidget.setBounds(xStart + 1796, yStart + totalHeight, 100, 100);
            totalHeight += 120;
        }

        if(currentTypeWidget == null) return;
        if(!rootWidgets.contains(currentTypeWidget)){
            addRootWidget(currentTypeWidget);
        }

        currentTypeWidget.setBounds(xStart, yStart, 1800, 750);

        String activeId = currentTypeWidget.getActiveLeaderboard() == null
                ? null : currentTypeWidget.getActiveLeaderboard().id();
        if (!Objects.equals(activeId, refreshLeaderboardId)) {
            refreshLeaderboardId = activeId;
            nextAutoRefreshAt = System.currentTimeMillis() + AUTO_REFRESH_INTERVAL_MS;
        } else if (System.currentTimeMillis() >= nextAutoRefreshAt) {
            if (activeId == null) nextAutoRefreshAt = System.currentTimeMillis() + AUTO_REFRESH_INTERVAL_MS;
            else reloadCurrentLeaderboard();
        }
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int xStart = getLogicalWidth() / 2 - 900;
        int yStart = getLogicalHeight() / 2 - 375;
        PVScreen.DarkModeToggleWidget.drawImageWithFade(backgroundTextureDark, backgroundTexture, xStart, yStart, 1800, 750, ui);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        for (TypeButtonWidget button : typeButtonWidgets) {
            if (!button.isHovered()) continue;
            TextInputWidget.drawFittingTooltip(ctx, List.of(Text.of(button.type.name())), mouseX, mouseY);
            return;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double mx = mouseX / matrixScale;
        double my = mouseY / matrixScale;
        for (int i = rootWidgets.size() - 1; i >= 0; i--) {
            if (rootWidgets.get(i).mouseScrolled(mx, my, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void removed() {
        SESSION_STATE.currentType = currentType;
        for (Type type : Type.values()) {
            SESSION_STATE.states.put(type, typeWidgets.get(type).snapshotState());
        }
        super.removed();
    }

    float getSpecialElementScale() {
        return (float) (matrixScale / scaleFactor);
    }

    private TypeWidget getTypeWidget(Type type) {
        return typeWidgets.get(type);
    }

    private void selectType(Type type) {
        if (type == currentType) return;
        removeRootWidget(currentTypeWidget);
        currentType = type;
        currentTypeWidget = getTypeWidget(type);
        addRootWidget(currentTypeWidget);
    }

    private void setOwnIdentity(String playerName, String guildName, String guildPrefix) {
        for (TypeWidget widget : typeWidgets.values()) {
            widget.setOwnIdentity(playerName, guildName, guildPrefix);
        }
    }

    private void openCurrentLeaderboardInBrowser() {
        TypeWidget widget = currentTypeWidget == null ? getTypeWidget(currentType) : currentTypeWidget;
        LeaderboardDefinition leaderboard = widget.getActiveLeaderboard();
        if (leaderboard == null || leaderboard.id() == null) {
            LinkUtils.openLink("https://wynncraft.com/stats/");
            return;
        }
        LinkUtils.openLink("https://wynncraft.com/stats/"
                + currentType.name().toLowerCase(Locale.ROOT) + "?lb_type=" + leaderboard.id());
    }

    private void reloadCurrentLeaderboard() {
        TypeWidget widget = currentTypeWidget == null ? getTypeWidget(currentType) : currentTypeWidget;
        widget.reloadLeaderboard();
        nextAutoRefreshAt = System.currentTimeMillis() + AUTO_REFRESH_INTERVAL_MS;
    }

    public static void drawDynamicSign(UIUtils ui, int x, int y, int width, int height) {
        if (ui == null || width <= 0 || height <= 0) return;
        int capWidth = Math.min(Math.round(height * 12f / 20f), width / 2);
        int middleWidth = Math.max(0, width - capWidth * 2);
        PVScreen.DarkModeToggleWidget.drawImageWithFade(
                signLeftTextureDark, signLeftTexture, x, y, capWidth, height, ui);
        if (middleWidth > 0) {
            PVScreen.DarkModeToggleWidget.drawImageWithFade(
                    signMiddleTextureDark, signMiddleTexture,
                    x + capWidth, y, middleWidth, height, ui);
        }
        PVScreen.DarkModeToggleWidget.drawImageWithFade(
                signRightTextureDark, signRightTexture,
                x + width - capWidth, y, capWidth, height, ui);
    }

    private class AutoRefreshWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            long remaining = Math.max(0, nextAutoRefreshAt - System.currentTimeMillis());
            long seconds = (remaining + 999) / 1000;
            drawDynamicSign(ui, x, y - 8, width, height);
            ui.drawCenteredText("Auto refresh in " + seconds + "s", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFFFFF"), 2.2f);
        }
    }

    private static class ActionButtonWidget extends Widget {
        private final String label;
        private final Runnable action;

        private ActionButtonWidget(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            drawDynamicSign(ui, x, y - 8, width, height);
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString(hovered ? "FFD966" : "FFFFFF"), 2.3f);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            action.run();
            return true;
        }
    }

    public static class TypeButtonWidget extends Widget {
        static Identifier guildTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/guild_icon.png");
        static Identifier playerTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/player_icon.png");
        static Identifier gamemodeTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/gamemode_icon.png");

        static Identifier playerTabTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/tab.png");
        static Identifier longPlayerTabTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/tab_long.png");

        static Identifier playerTabTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/tab_dark.png");
        static Identifier longPlayerTabTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/tab_long_dark.png");

        int index;
        Type type;
        LVScreen parent;

        public TypeButtonWidget(int index, Type type, LVScreen parent) {
            super(0, 0, 0, 0);
            this.index = index;
            this.type = type;
            this.parent = parent;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if(this.ui == null) return;

            boolean active = type == parent.currentType;
            if(hovered || active) {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(longPlayerTabTextureDark, longPlayerTabTexture, x, y, 130, 100, ui);
            } else {
                PVScreen.DarkModeToggleWidget.drawImageWithFade(playerTabTextureDark, playerTabTexture, x, y, 120, 100, ui);
            }

            Identifier texture = switch (type) {
                case Guild -> guildTexture;
                case Player -> playerTexture;
                case Gamemode -> gamemodeTexture;
                case null -> null;
            };

            if(texture == null) return;

            ui.drawImage(texture, x + 25 + ((hovered || active) ? 10 : 0), y + 10, 80, 80);

        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if(type == parent.currentType) return false;
            parent.selectType(type);
            return true;
        }
    }

    private static class SessionState {
        private Type currentType = Type.Player;
        private final Map<Type, TypeWidget.State> states = new EnumMap<>(Type.class);

        private SessionState() {
            for (Type type : Type.values()) {
                states.put(type, new TypeWidget.State(null, null, 1, 0, 0));
            }
        }
    }
}
