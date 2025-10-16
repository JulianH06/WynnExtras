package julianh06.wynnextras.core;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.WynnExtrasModMenuApiImpl;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.stream.IntStream;

public class MainScreen extends WEScreen {
    private TestButton testButton;

    Identifier logoTexture = Identifier.of("wynnextras", "textures/general/wynnextrasbanner.png");
    public static int listLength = 5;

    public MainScreen() {
        super(Text.of("WynnExtras"));
        // Standard-Layoutwerte für die Liste (logical coords)
    }

    //config
    //pv
    //waypoints
    //raidlist
    //

    ImageWidget logo = new ImageWidget(logoTexture, ui);
    ModrinthButton modrinthButton = new ModrinthButton();
    DiscordButton discordButton = new DiscordButton();
    GitHubButton gitHubButton = new GitHubButton();

    @Override
    protected void init() {
        super.init();

        rootWidgets.clear();
        listElements.clear();

        addRootWidget(logo);
        addRootWidget(modrinthButton);
        addRootWidget(discordButton);
        addRootWidget(gitHubButton);

        IntStream.range(0, listLength).forEach(i -> {
            SimpleListElement e = new SimpleListElement(i, ui, this);
            addListElement(e);
        });
    }

    @Override
    public void updateValues() {
        logo.setBounds(getLogicalWidth() / 2 - 600, 0, 1200, 375);
        modrinthButton.setBounds(getLogicalWidth() / 2 - 200, getLogicalHeight() - 110, 100, 100);
        discordButton.setBounds(getLogicalWidth() / 2 - 50, getLogicalHeight() - 110, 100, 100);
        gitHubButton.setBounds(getLogicalWidth() / 2 + 100, getLogicalHeight() - 110, 100, 100);
        this.listX = (float) getLogicalWidth() / 2 - 350;
        this.listY = 375f;
        this.listWidth = 700f;
        this.listHeight = getLogicalHeight() - 490;
        if(getLogicalHeight() > 550) {
            this.listItemHeight = (getLogicalHeight() - 600) / (float) listLength;
        } else this.listItemHeight = 0;
        this.listSpacing = 20f;
    }

    public static void actionForIndex(int i, Screen parent) {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        switch (i) {
            case 0 -> {
                MinecraftUtils.mc().send(() -> {
                    MinecraftUtils.mc().setScreen(SimpleConfig.getConfigScreen(WynnExtrasConfig.class, parent).get());
                });
            }
            case 1 -> {
                MinecraftUtils.mc().setScreen(null);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("we waypoints");
                    //the waypoint gui has not been migrated to the new WEScreen system yet
                }
            }
            case 2 -> {
                PV.open(McUtils.playerName());
            }
            case 3 -> {
                MinecraftUtils.mc().setScreen(null);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("we raidlist");
                    //the raidlist has not been migrated to the new WEScreen system yet
                }
            }
            case 4 -> {
                MinecraftUtils.mc().setScreen(null);
            }
            default -> {
                System.out.println("Not implemented yet :steamhappy:");
            }
        }
    }

    public static String textForIndex(int i) {
        return switch (i) {
            case 0 -> "Config";
            case 1 -> "Waypoints";
            case 2 -> "Profile Viewer";
            case 3 -> "Raid List";
            case 4 -> "Close";
            default -> "null";
        };
    }

    // Helper overrides: forward keyboard to focused element if present
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (focusedElement != null && focusedElement.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (focusedWidget != null && focusedWidget.charTyped(chr, modifiers)) return true;
        if (focusedElement != null && focusedElement.charTyped(chr, modifiers)) return true;
        return super.charTyped(chr, modifiers);
    }

    public static void open() {
        WEScreen.open(MainScreen::new);
    }

    // TestButton: einfacher Button der in die Konsole schreibt
    public static class TestButton extends Widget {
        private final Text label;
        private Runnable action;

        public TestButton(int x, int y, int width, int height, Text label, UIUtils ui, Screen parent) {
            super(x, y, width, height);
            this.label = label;
            // set default action
            this.action = () -> {
                MinecraftUtils.mc().send(() -> {
                    MinecraftUtils.mc().setScreen(SimpleConfig.getConfigScreen(WynnExtrasConfig.class, parent).get());
                });
            };
            this.ui = ui;
        }

        @Override
        protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            if (isHovered()) {
                ui.drawRect(x, y, width, height, CustomColor.fromHexString("777777"));
            } else {
                ui.drawRect(x, y, width, height, CustomColor.fromHexString("555555"));
            }
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawText(label.getString(), x, y, CustomColor.fromHexString("FFFFFF"), 5);
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }
    }

    // SimpleListElement: minimalistische WEElement-Implementierung, zeichnet weiße Rechtecke mit Label
    public static class SimpleListElement extends WEElement<String> {
        private final int id;
        private boolean hoveredLocal = false;
        private Screen parent;

        public SimpleListElement(int id, UIUtils ui, Screen parent) {
            super(0, 0, 100, 20, String.valueOf(id));
            this.id = id;
            this.ui = ui;
            this.parent = parent;
        }

        @Override
        protected void drawBackground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            if(id == listLength - 1) return;
            ui.drawRect(x + 30, y + height, width - 60, 20, CustomColor.fromHexString("4e392d"));
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
            if(this.height <= 0) return;
            if (ui == null) return;

            // transformiere logical bounds → screen coords
            int sx = (int) ui.sx(x);
            int sy = (int) ui.sy(y);
            int sw = ui.sw(width);
            int sh = ui.sh(height);

            // Hover-Erkennung in screen coords
            hoveredLocal = mouseX >= sx && mouseY >= sy && mouseX < sx + sw && mouseY < sy + sh;
            int fill = hoveredLocal ? 0xFFEFEFEF : 0xFFFFFFFF;

            // zeichne Hintergrund über UIUtils
//            ui.drawRect(x, y, width, height, CustomColor.fromInt(fill));
            ui.drawButton(x, y, width, height, 12, hovered);

            // zeichne Text über UIUtils (zentriert oder linksbündig)
            ui.drawCenteredText(textForIndex(id), x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 6f);
        }

        @Override
        protected boolean onClick(int mouseX, int mouseY, int button) {
            if (ui == null) return false;

            int sx = (int) ui.sx(x);
            int sy = (int) ui.sy(y);
            int sw = ui.sw(width);
            int sh = ui.sh(height);

            if (mouseX >= sx && mouseY >= sy && mouseX < sx + sw && mouseY < sy + sh) {
                System.out.println("[WynnExtras] clicked element " + id);
                actionForIndex(id, parent);
                return true;
            }
            return false;
        }
    }

    public static class DiscordButton extends Widget {
        Identifier discordTexture = Identifier.of("wynnextras", "textures/general/logos/discord.png");
        private Runnable action;

        public DiscordButton() {
            super(0, 0, 0, 0);
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                LinkUtils.openLink("https://discord.gg/UbC6vZDaD5");
            };
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 12, hovered);
            ui.drawImage(discordTexture, x, y, width, height);
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }
    }

    public static class ModrinthButton extends Widget {
        Identifier modrinthTexture = Identifier.of("wynnextras", "textures/general/logos/modrinth.png");
        private Runnable action;

        public ModrinthButton() {
            super(0, 0, 0, 0);
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                LinkUtils.openLink("https://modrinth.com/mod/wynnextras");
            };
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 12, hovered);
            ui.drawImage(modrinthTexture, x, y, width, height);
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }
    }

    public static class GitHubButton extends Widget {
        Identifier githubTexture = Identifier.of("wynnextras", "textures/general/logos/github.png");
        private Runnable action;

        public GitHubButton() {
            super(0, 0, 0, 0);
            this.action = () -> {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                LinkUtils.openLink("https://github.com/JulianH06/WynnExtras");
            };
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 12, hovered);
            ui.drawImage(githubTexture, x, y, width, height);
        }

        @Override
        protected boolean onClick(int button) {
            if (!isEnabled()) return false;
            if (action != null) action.run();
            return true;
        }
    }
}

