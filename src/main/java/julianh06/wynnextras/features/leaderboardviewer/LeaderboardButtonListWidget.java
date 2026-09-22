package julianh06.wynnextras.features.leaderboardviewer;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.profileviewer.PVScreen;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardButtonListWidget extends Widget {
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_GAP = 8;
    private static final int SCROLLBAR_WIDTH = 20;
    private static final int SCROLLBAR_GAP = 10;
    private static final float SCROLL_STEP = 96f;

    private final List<LeaderboardButtonWidget> buttons = new ArrayList<>();
    private final ScrollBarWidget scrollBar = new ScrollBarWidget();
    private float targetOffset;
    private float actualOffset;
    private float maxOffset;

    public LeaderboardButtonListWidget() {
        addChild(scrollBar);
    }

    public void addButton(LeaderboardButtonWidget button) {
        buttons.add(button);
        addChild(button);
    }

    public void removeButton(LeaderboardButtonWidget button) {
        buttons.remove(button);
        removeChild(button);
    }

    public void resetScroll() {
        targetOffset = 0;
        actualOffset = 0;
    }

    public void restoreScroll(float targetOffset, float actualOffset) {
        this.targetOffset = Math.max(0, targetOffset);
        this.actualOffset = Math.max(0, actualOffset);
    }

    public float getTargetOffset() {
        return targetOffset;
    }

    public float getActualOffset() {
        return actualOffset;
    }

    @Override
    protected void updateValues() {
        int contentHeight = buttons.isEmpty() ? 0
                : buttons.size() * BUTTON_HEIGHT + (buttons.size() - 1) * BUTTON_GAP;
        maxOffset = Math.max(0, contentHeight - height);
        targetOffset = Math.clamp(targetOffset, 0, maxOffset);
        actualOffset = Math.clamp(actualOffset, 0, maxOffset);
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ctx.enableScissor((int) ui.sx(x), (int) ui.sy(y), (int) ui.sx(x + width), (int) ui.sy(y + height));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        float difference = targetOffset - actualOffset;
        if (Math.abs(difference) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) {
            actualOffset = targetOffset;
        } else {
            actualOffset += difference * Math.min(1f, 0.3f * tickDelta);
        }

        int buttonWidth = maxOffset > 0 ? width - SCROLLBAR_WIDTH - SCROLLBAR_GAP : width;
        int buttonY = y - Math.round(actualOffset);
        for (LeaderboardButtonWidget button : buttons) {
            button.setBounds(x, buttonY, buttonWidth, BUTTON_HEIGHT);
            buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        }

        scrollBar.setVisible(maxOffset > 0);
        if (maxOffset > 0) scrollBar.setBounds(x + width - SCROLLBAR_WIDTH, y, SCROLLBAR_WIDTH, height);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        ctx.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!contains((int) mx, (int) my)) return false;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxOffset <= 0 || !contains((int) mx, (int) my)) return false;
        targetOffset = Math.clamp(targetOffset - (float) delta * SCROLL_STEP, 0, maxOffset);
        return true;
    }

    private class ScrollBarWidget extends Widget {
        private final ScrollBarThumbWidget thumb = new ScrollBarThumbWidget();
        private int currentMouseY;

        private ScrollBarWidget() {
            addChild(thumb);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseY = mouseY;
            ui.drawSliderFade(x, y, width, height, 12, PVScreen.DarkModeToggleWidget.fade);
            int thumbHeight = Math.max(45, Math.round(height * height / (height + maxOffset)));
            int travel = height - thumbHeight;
            int thumbY = maxOffset <= 0 ? y : y + Math.round(travel * actualOffset / maxOffset);
            thumb.setBounds(x, thumbY, width, thumbHeight);
        }

        @Override
        protected boolean onClick(int button) {
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            setOffset(currentMouseY);
            actualOffset = targetOffset;
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (!thumb.held) return false;
            setOffset((int) mouseY);
            actualOffset = targetOffset;
            return true;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (!thumb.held) return false;
            thumb.held = false;
            return true;
        }

        private void setOffset(int mouseY) {
            int travel = height - thumb.getHeight();
            if (travel <= 0) return;
            float logicalMouseY = (float) ((mouseY - ui.getYStart()) * ui.getScaleFactor());
            float thumbY = Math.clamp(logicalMouseY - y - thumb.getHeight() / 2f, 0, travel);
            targetOffset = thumbY / travel * maxOffset;
        }
    }

    private static class ScrollBarThumbWidget extends Widget {
        private boolean held;

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButtonFade(x, y, width, height, 12, hovered || held);
        }

        @Override
        protected boolean onClick(int button) {
            held = true;
            return true;
        }
    }
}
