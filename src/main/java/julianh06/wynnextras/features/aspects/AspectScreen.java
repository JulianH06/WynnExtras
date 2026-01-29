package julianh06.wynnextras.features.aspects;

import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.aspects.pages.LootPoolPage;
import julianh06.wynnextras.features.aspects.pages.PageWidget;
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.features.profileviewer.tabs.*;
import julianh06.wynnextras.utils.UI.WEScreen;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AspectScreen extends WEScreen {
    public enum Page {LootPools, Aspects, Gambits, RaidLoot, Explore, Leaderboard}

    static LootPoolPage lootPoolPage;

    private static Page currentPage = Page.LootPools;
    private static PageWidget currentWidget;
    private static long lastScrollTime = 0;
    private static final long scrollCooldown = 0; // in ms

    private List<PageSwitchButton> pageSwitchButtons = new ArrayList<>();
    private boolean registeredScroll = false;

    protected AspectScreen() {
        super(Text.of("WynnExtras Aspects"));

        for(Page page : Page.values()) {
            pageSwitchButtons.add(new PageSwitchButton(page));
        }
    }

    @Override
    protected void init() {
        super.init();

        registeredScroll = false;

//        // Initialize page instances
//        if (gambitsPage == null) {
//            gambitsPage = new GambitsPage(this);
//            raidLootPage = new RaidLootPage(this);
//            explorePage = new ExplorePage(this);
//            leaderboardPage = new LeaderboardPage(this);
//        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        for(PageSwitchButton button : pageSwitchButtons) {
            if(button.mouseClicked(click.x(), click.y(), click.button())) return true;
        }

        if(currentWidget == null) return true;

        if(currentWidget.mouseClicked(click.x(), click.y(), click.button())) return true;

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if(currentWidget != null) currentWidget.mouseReleased(click.x(), click.y(), click.button());
        return super.mouseReleased(click);
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        int centerX = getLogicalWidth() / 2;
        FontRenderer.getInstance().renderText(
                ctx,
                StyledText.fromComponent(WynnExtras.addWynnExtrasPrefix("")),
                ui.sx(centerX + 7),
                ui.sy(10),
                CustomColor.fromHexString("FFFFFF"),
                HorizontalAlignment.CENTER,
                VerticalAlignment.TOP,
                TextShadow.NORMAL,
                (float)(3f / scaleFactor)
        );


        int buttonAmount = pageSwitchButtons.size();
        int buttonWidth = 230;
        int buttonHeight = 50;
        int spacing = 20;
        int totalButtonWidth = (buttonWidth + spacing) * buttonAmount;

        int x = (int) (((width * ui.getScaleFactorF()) - totalButtonWidth + spacing) / 2f);
        int y = (int) (height * ui.getScaleFactorF() - buttonHeight - spacing);

        for(PageSwitchButton button : pageSwitchButtons) {
            button.setBounds(x, y, buttonWidth, buttonHeight);
            button.draw(ctx, mouseX, mouseY, tickDelta, ui);
            x += buttonWidth + spacing;
        }

        currentWidget = getTabWidget(currentPage);

        if(currentWidget == null) return;

        currentWidget.setBounds(0, 0, screenWidth, screenHeight);
        currentWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

        if(MinecraftClient.getInstance().currentScreen == null || registeredScroll) return;
        ScreenMouseEvents.afterMouseScroll(MinecraftClient.getInstance().currentScreen).register((
                screen,
                mX,
                mY,
                horizontalAmount,
                verticalAmount,
                consumed
        ) -> {
            long now = System.currentTimeMillis();
            if (now - lastScrollTime < scrollCooldown) {
                return true;
            }
            lastScrollTime = now;

            if(currentWidget == null) return false;

            return currentWidget.mouseScrolled(mX, mY, verticalAmount);
        });
        registeredScroll = true;
    }

    private PageWidget getTabWidget(Page page) {
        return switch (page) {
            case LootPools -> lootPoolPage == null ? lootPoolPage = new LootPoolPage(this) : lootPoolPage;
            case Aspects -> null;
            case Gambits -> null;
            case RaidLoot -> null;
            case Explore -> null;
            case Leaderboard -> null;
            case null, default -> null;
        };
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    private static class PageSwitchButton extends Widget {
        final Page page;

        public PageSwitchButton(Page page) {
            super(0, 0, 0, 0);
            this.page = page;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 12, hovered, WynnExtrasConfig.INSTANCE.darkmodeToggle);
            String name = page.name();
            if(page == Page.LootPools) name = "Loot Pools";
            if(page == Page.RaidLoot) name = "Raid Loot";
            ui.drawCenteredText(name, x + width / 2f, y + height / 2f, currentPage == page ? CustomColor.fromHexString("FFFF00") : CustomColor.fromHexString("FFFFFF"));
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            currentPage = page;
            return true;
        }
    }
}
