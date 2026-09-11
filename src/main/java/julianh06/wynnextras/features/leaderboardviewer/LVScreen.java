package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LVScreen extends WEScreen {
    @Override protected double getTargetScaleFactor() { return 2.0; }
    @Override protected int getMinLogicalWidth()  { return 2100; }
    @Override protected int getMinLogicalHeight() { return 870; }

    private final PVScreen.DarkModeToggleWidget darkModeToggleWidget = new PVScreen.DarkModeToggleWidget();

    public enum Type { Guild, Player, Gamemode }
    private Type currentType = Type.Player;
    private TypeWidget currentTypeWidget;
    private final List<TypeButtonWidget> typeButtonWidgets = new ArrayList<>();
    private final Map<Type, TypeWidget> typeWidgets = new EnumMap<>(Type.class);

    static Identifier backgroundTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/profileviewerbackground.png");
    static Identifier backgroundTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/profileviewerbackground_dark.png");

    protected LVScreen() {
        super(Text.of("leaderboard viewer"));

        int j = 0;
        for(Type type : Type.values()) {
            typeButtonWidgets.add(new TypeButtonWidget(j, type, this));
            typeWidgets.put(type, new TypeWidget(type, LeaderboardCatalog.categoriesFor(type)));
            j++;
        }
    }

    @Override
    public void init() {
        super.init();

        registerScrolling();
        rootWidgets.clear();
        addRootWidget(darkModeToggleWidget);
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
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int xStart = getLogicalWidth() / 2 - 900;
        int yStart = getLogicalHeight() / 2 - 375;
        PVScreen.DarkModeToggleWidget.drawImageWithFade(backgroundTextureDark, backgroundTexture, xStart, yStart, 1800, 750, ui);
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

    public static class TypeButtonWidget extends Widget {
        static Identifier guildTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/guild_icon.png");
        static Identifier playerTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/player_icon.png");
        static Identifier gamemodeTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/gamemode_icon.png");

        static Identifier playerTabTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/playertab.png");
        static Identifier longPlayerTabTexture = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/playertab_long.png");

        static Identifier playerTabTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/playertab_dark.png");
        static Identifier longPlayerTabTextureDark = Identifier.of("wynnextras", "textures/gui/leaderboardviewer/playertab_long_dark.png");

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

            if(hovered) {
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

            ui.drawImage(texture, x + 25 + (hovered ? 10 : 0), y + 10, 80, 80);

//            if(hovered) {
//                ctx.drawTooltip(MinecraftUtils.mc().textRenderer, Text.of(type.name()), mouseX, mouseY);
//            }
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
}
