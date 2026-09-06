package julianh06.wynnextras.features.profileviewer.tabs;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class ClassWidget extends Widget {
    private static final int GAMEMODE_ICON_SIZE = 30;
    private static final int GAMEMODE_TEXTURE_SIZE = 20;

    static Identifier classBackgroundTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive.png");
    static Identifier classBackgroundTextureGold = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold.png");
    static Identifier classBackgroundTextureActive = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive.png");

    static Identifier classBackgroundTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactive_dark.png");
    static Identifier classBackgroundTextureGoldDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundinactivegold_dark.png");
    static Identifier classBackgroundTextureActiveDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactive_dark.png");

    static Identifier classBackgroundTextureHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered.png");
    static Identifier classBackgroundTextureHoveredDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundhovered_dark.png");
    static Identifier classBackgroundTextureActiveHovered = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactivehovered.png");
    static Identifier classBackgroundTextureActiveHoveredDark = Identifier.of("wynnextras", "textures/gui/profileviewer/classbackgroundactivehovered_dark.png");

    static Identifier ironmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/ironman.png");
    static Identifier ultimateIronmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/ultimateironman.png");
    static Identifier huntedTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hunted.png");
    static Identifier hardcoreTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hardcore.png");
    static Identifier hardcoreFailedTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/hardcorefailed.png");
    static Identifier craftsmanTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/gamemodes/craftsman.png");

    static Identifier onlineCircleTextureDark = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle_dark.png");
    static Identifier onlineCircleTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/onlinecircle.png");

    CharacterData characterData;
    private final Runnable action;
    private final boolean isAtiveCharacter;

    public ClassWidget(CharacterData characterData, boolean isAtiveCharacter) {
        super(0, 0, 0, 0);
        this.characterData = characterData;
        this.isAtiveCharacter = isAtiveCharacter;
        this.action = () -> {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            if(PVScreen.selectedCharacter == characterData) {
                PVScreen.selectedCharacter = null;
                return;
            }
            PVScreen.selectedCharacter = characterData;
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
        if(x == 0) return;

        Identifier classTexture;
        if(characterData.getLevel() == 121) {
            classTexture = getGoldClassTexture(characterData.getType());
        } else {
            classTexture = getClassTexture(characterData.getType());
        }

        if(selectedCharacter == characterData) {
            if(hovered) {
                DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureActiveHoveredDark, classBackgroundTextureActiveHovered,  x, y, 390, 132, ui);
            } else {
                DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureActiveDark, classBackgroundTextureActive,  x, y, 390, 132, ui);
            }
        } else if(hovered) {
            float fade = DarkModeToggleWidget.fade;
            ui.drawImage(classBackgroundTextureHovered, x, y, 390, 132, 0, 10, 130, 44, 130, 54, 1f - fade);
            ui.drawImage(classBackgroundTextureHoveredDark, x, y, 390, 132, 0, 10, 130, 44, 130, 54, fade);
        } else if(characterData.getTotalLevel() != 1690) {
            float fade = DarkModeToggleWidget.fade;
            ui.drawImage(classBackgroundTexture, x, y, 390, 132, 0, 10, 130, 44, 130, 54, 1f - fade);
            ui.drawImage(classBackgroundTextureDark, x, y, 390, 132, 0, 10, 130, 44, 130, 54, fade);
        } else {
            DarkModeToggleWidget.drawImageWithFade(classBackgroundTextureGoldDark, classBackgroundTextureGold,  x, y, 390, 132, ui);
        }

        if (classTexture != null) {
            int level = characterData.getLevel();
            int totalLevel = characterData.getTotalLevel();
            Integer contentCompletionMax = WynncraftApiHandler.getContentCompletionMax();
            CustomColor levelColor;
            if (contentCompletionMax != null && characterData.getContentCompletion() == contentCompletionMax && !WynnExtrasConfig.INSTANCE.removeChroma) {
                levelColor = CustomColor.RAINBOW;
            } else {
                levelColor = CustomColor.fromHexString("FFFFFF");
            }

            ui.drawImage(classTexture, x + 12, y + 12, 90, 102);
            ui.drawText(getClassName(characterData), x + 111, y + 18, levelColor, 2.1f);
            ui.drawText("Level " + level, x + 111, y + 42, levelColor, 2.1f);
            ui.drawText("Total Level " + totalLevel, x + 111, y + 66, levelColor, 2.1f);
            String completionText = contentCompletionMax == null
                    ? "Completion loading..."
                    : "Completion " + Math.min(100, characterData.getContentCompletion() * 100L / contentCompletionMax) + "%";
            ui.drawText(completionText, x + 111, y + 90, levelColor, 2.1f);
        }

        List<String> gamemodes = characterData.getGamemode();
        int k = 0;
        if(gamemodes != null) {
            if(gamemodes.contains("ultimate_ironman")) {
                drawGamemodeIcon(ultimateIronmanTexture, x + 350, y + 85);
                k++;
            } else if (gamemodes.contains("ironman")) {
                drawGamemodeIcon(ironmanTexture, x + 350, y + 85);
                k++;
            }
            if(gamemodes.contains("hunted")) {
                drawGamemodeIcon(huntedTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35));
                k++;
            }
            if(gamemodes.contains("hardcore")) {
                if(characterData.getDeaths() == 0) {
                    drawGamemodeIcon(hardcoreTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35));
                } else {
                    drawGamemodeIcon(hardcoreFailedTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35));
                }
                k++;
            }
            if(gamemodes.contains("craftsman")) {
                drawGamemodeIcon(craftsmanTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35));
            }
        }

        if(isAtiveCharacter) {
            DarkModeToggleWidget.drawImageWithFade(onlineCircleTextureDark, onlineCircleTexture, x + 6, y + 6, 18, 18, ui);
        }
    }

    private void drawGamemodeIcon(Identifier texture, float iconX, float iconY) {
        ui.drawPixelAlignedImage(texture, iconX, iconY, GAMEMODE_ICON_SIZE, GAMEMODE_ICON_SIZE,
                GAMEMODE_TEXTURE_SIZE, GAMEMODE_TEXTURE_SIZE, PVScreen.currentMatrixScale);
    }
}
