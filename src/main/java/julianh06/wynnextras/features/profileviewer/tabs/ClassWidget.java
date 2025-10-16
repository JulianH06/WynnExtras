package julianh06.wynnextras.features.profileviewer.tabs;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;

import static julianh06.wynnextras.features.profileviewer.PVScreen.*;

public class ClassWidget extends Widget {
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

    CharacterData characterData;
    private final Runnable action;

    public ClassWidget(CharacterData characterData) {
        super(0, 0, 0, 0);
        this.characterData = characterData;
        this.action = () -> {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
        if(characterData.getLevel() == 106) {
            classTexture = getGoldClassTexture(characterData.getType());
        } else {
            classTexture = getClassTexture(characterData.getType());
        }

        if(selectedCharacter == characterData) {
            if(hovered) {
                if (SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    ui.drawImage(classBackgroundTextureActiveHoveredDark, x, y, 390, 132);
                } else {
                    ui.drawImage(classBackgroundTextureActiveHovered, x, y, 390, 132);
                }
            } else {
                if (SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                    ui.drawImage(classBackgroundTextureActiveDark, x, y, 390, 132);
                } else {
                    ui.drawImage(classBackgroundTextureActive, x, y, 390, 132);
                }
            }
        } else if(hovered) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage(classBackgroundTextureHoveredDark, x, y, 390, 132);
            } else {
                ui.drawImage(classBackgroundTextureHovered, x, y, 390, 132);
            }
        } else if(characterData.getTotalLevel() != 1690) {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage(classBackgroundTextureDark, x, y, 390, 132);
            } else {
                ui.drawImage(classBackgroundTexture, x, y, 390, 132);
            }
        } else {
            if(SimpleConfig.getInstance(WynnExtrasConfig.class).darkmodeToggle) {
                ui.drawImage(classBackgroundTextureGoldDark, x, y, 390, 132);
            } else {
                ui.drawImage(classBackgroundTextureGold, x, y, 390, 132);
            }
        }

        if (classTexture != null) {
            int level = characterData.getLevel();
            int totalLevel = characterData.getTotalLevel();
            CustomColor levelColor;
            if (characterData.getContentCompletion() == 1133) {
                levelColor = CommonColors.RAINBOW;
            } else {
                levelColor = CustomColor.fromHexString("FFFFFF");
            }

            ui.drawImage(classTexture, x + 12, y + 12, 90, 102);
            ui.drawText(getClassName(characterData), x + 111, y + 18, levelColor, 2.1f);
            ui.drawText("Level " + level, x + 111, y + 42, levelColor, 2.1f);
            ui.drawText("Total Level " + totalLevel, x + 111, y + 66, levelColor, 2.1f);
            ui.drawText("Completion " + (characterData.getContentCompletion() * 100/1133) + "%", x + 111, y + 90, levelColor, 2.1f);
        }

        List<String> gamemodes = characterData.getGamemode();
        int k = 0;
        if(gamemodes != null) {
            if(gamemodes.contains("ultimate_ironman")) {
                ui.drawImage(ultimateIronmanTexture, x + 350, y + 85, 30, 30);
                k++;
            } else if (gamemodes.contains("ironman")) {
                ui.drawImage(ironmanTexture, x + 350, y + 85, 30, 30);
                k++;
            }
            if(gamemodes.contains("hunted")) {
                ui.drawImage(huntedTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                k++;
            }
            if(gamemodes.contains("hardcore")) {
                if(characterData.getDeaths() == 0) {
                    ui.drawImage(hardcoreTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                } else {
                    ui.drawImage(hardcoreFailedTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
                }
                k++;
            }
            if(gamemodes.contains("craftsman")) {
                ui.drawImage(craftsmanTexture, x - ((k % 2) * 35) + 350, y + 85 - (Math.floorDiv(k, 2) * 35), 30, 30);
            }
        }
    }
}
