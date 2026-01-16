package julianh06.wynnextras.core;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.config.simpleconfig.SimpleConfig;
import julianh06.wynnextras.features.abilitytree.TreeScreen;
import julianh06.wynnextras.features.profileviewer.PV;
import julianh06.wynnextras.utils.LinkUtils;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.stream.IntStream;

public class MainScreen extends WEScreen {
    Identifier logoTexture = Identifier.of("wynnextras", "textures/general/wynnextrasbanner.png");
    public static int listLength = 6;

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
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {

    }

    @Override
    public void updateValues() {
        logo.setBounds(getLogicalWidth() / 2 - 400, 0, 800, 250);
        modrinthButton.setBounds(getLogicalWidth() / 2 - 200, getLogicalHeight() - 110, 100, 100);
        discordButton.setBounds(getLogicalWidth() / 2 - 50, getLogicalHeight() - 110, 100, 100);
        gitHubButton.setBounds(getLogicalWidth() / 2 + 100, getLogicalHeight() - 110, 100, 100);
        this.listX = (float) getLogicalWidth() / 2 - 350;
        this.listY = 250f;
        this.listWidth = 700f;
        this.listHeight = getLogicalHeight() - 490;
        if(getLogicalHeight() > 550) {
            this.listItemHeight = (getLogicalHeight() - 500) / (float) listLength;
        } else this.listItemHeight = 0;
        this.listSpacing = 20f;
        //System.out.println(listElements);
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
                WEScreen.open(TreeScreen::new);
            }
            case 2 -> {
                PV.open(McUtils.playerName());
            }
            case 3 -> {
                MinecraftUtils.mc().setScreen(null);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("we waypoints");
                    //the waypoint gui has not been migrated to the new WEScreen system yet
                }
            }
            case 4 -> {
                MinecraftUtils.mc().setScreen(null);
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("we raidlist");
                    //the raidlist has not been migrated to the new WEScreen system yet
                }
            }
            case 5 -> {
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
            case 1 -> "Ability Tree Loader";
            case 2 -> "Profile Viewer";
            case 3 -> "Waypoints";
            case 4 -> "Raid List";
            case 5 -> "Close";
            default -> "null";
        };
    }

    // Helper overrides: forward keyboard to focused element if present
    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if (focusedWidget != null && focusedWidget.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (focusedElement != null && focusedElement.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        int modifiers = input.modifiers();

        if (focusedWidget != null && focusedWidget.charTyped(chr, modifiers)) return true;
        if (focusedElement != null && focusedElement.charTyped(chr, modifiers)) return true;
        return super.charTyped(input);
    }

    public static void open() {
        WEScreen.open(MainScreen::new);
    }

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

            ui.drawButton(x, y, width, height, 12, hovered);

            ui.drawCenteredText(textForIndex(id), x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), 6f);
        }

        @Override
        protected boolean onClick(int mouseX, int mouseY, int button) {
            if (ui == null) return false;

            int sx = (int) ui.sx(x);
            int sy = (int) ui.sy(y);
            int sw = ui.sw(width);
            int sh = ui.sh(height);

            System.out.println("id: " + id);
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

