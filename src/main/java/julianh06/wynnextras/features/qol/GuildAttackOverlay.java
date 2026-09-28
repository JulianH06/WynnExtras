package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.HandledScreenAccess;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.WEHandledScreen;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class GuildAttackOverlay extends WEHandledScreen {
    private static final String TITLE_PREFIX = "Attacking: ";
    private static final int ATTACK_SLOT = 13;
    private static final float PANEL_WIDTH = 360;
    private static final float LINE_HEIGHT = 12;
    private static final int TOGGLE_WIDTH = 120;
    private static final int TOGGLE_HEIGHT = 20;
    private static final int TOGGLE_MARGIN = 8;
    private static final float TOGGLE_TEXT_SCALE = 0.8f;
    private static final ToggleButton VANILLA_TOGGLE_BUTTON = new ToggleButton();
    private static boolean forwardingSlotClick;

    private final HandledScreen<?> screen;
    private final AttackButton attackButton = new AttackButton();
    private final ToggleButton overlayToggleButton = new ToggleButton();
    private Click overlayClick;

    public GuildAttackOverlay(HandledScreen<?> screen) {
        this.screen = screen;
        rootWidgets.add(attackButton);
        rootWidgets.add(overlayToggleButton);
    }

    public static boolean isGuildAttackScreen(HandledScreen<?> screen) {
        return screen != null && clean(screen.getTitle().getString()).contains(TITLE_PREFIX);
    }

    @Override
    protected int getMinScreenWidth() {
        return 388;
    }

    @Override
    protected int getMinScreenHeight() {
        return 375;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {

    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ui.setScaleFactor(1);
        ItemStack attackItem = getAttackItem();
        List<Text> lore = getLore(attackItem);
        AttackDetails details = AttackDetails.from(lore);
        int renderedLines = Math.max(1, lore.size());

        float panelHeight = 177 + renderedLines * LINE_HEIGHT;
        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - panelHeight) / 2f;

        ui.drawVanillaPanel(panelX, panelY, PANEL_WIDTH, panelHeight, 4, 10, 10, 40, 50);
        ui.drawCenteredText(territoryName(), panelX + PANEL_WIDTH / 2f, panelY + 24,
                CustomColor.fromHexString("FFFFFF"), 3f);

        float summaryY = panelY + 48;
        float summaryWidth = 102;
        float summaryGap = 9;
        drawSummaryCard(panelX + 18, summaryY, summaryWidth, "Price (Emeralds)", details.priceText(),
                details.affordabilityColor());
        drawSummaryCard(panelX + 18 + summaryWidth + summaryGap, summaryY, summaryWidth, "Territory Defences", details.defenses,
                details.defenseColor(), 0.9f);
        drawSummaryCard(panelX + 18 + (summaryWidth + summaryGap) * 2, summaryY, summaryWidth, "Queue Time", details.queueTime,
                CustomColor.fromHexString("FFFFFF"));

        float loreY = panelY + 103;
        if (lore.isEmpty()) {
            ui.drawCenteredText("Loading attack information...", panelX + PANEL_WIDTH / 2f,
                    loreY + LINE_HEIGHT / 2f, CustomColor.fromHexString("AAAAAA"), 1f);
        } else {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i).getString();
                if (line.isBlank()) continue;
                ui.drawText(legacyText(line), panelX + 24, loreY + i * LINE_HEIGHT,
                        CustomColor.fromHexString("FFFFFF"), 1.2f);
            }
        }

        float buttonX = panelX + 24;
        float buttonY = panelY + panelHeight - 47;
        attackButton.setBounds(Math.round(buttonX), Math.round(buttonY), Math.round(PANEL_WIDTH - 48), 30);
        attackButton.setEnabled(!attackItem.isEmpty());
        layoutOverlayToggleButton();
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale((float) (1.0 / matrixScale), (float) (1.0 / matrixScale));
        AttackTimer.renderInScreen(ctx);
        ctx.getMatrices().popMatrix();
    }

    private void drawSummaryCard(float x, float y, float width, String label, String value, CustomColor valueColor) {
        drawSummaryCard(x, y, width, label, value, valueColor, 1.1f);
    }

    private void drawSummaryCard(float x, float y, float width, String label, String value, CustomColor valueColor, float labelSize) {
        ui.drawVanillaPanel(x, y, width, 43, 4, 5, 5, 17, 5);
        ui.drawCenteredText(label, x + width / 2f, y + 11,
                CustomColor.fromHexString("FFFFFF"), labelSize);
        ui.drawCenteredText(value, x + width / 2f, y + 28, valueColor, 1.5f);
    }

    private ItemStack getAttackItem() {
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || handler.slots.size() <= ATTACK_SLOT) return ItemStack.EMPTY;
        return handler.getSlot(ATTACK_SLOT).getStack();
    }

    private static List<Text> getLore(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        return lore == null ? List.of() : lore.lines();
    }

    private String territoryName() {
        String title = clean(screen.getTitle().getString());
        int prefix = title.indexOf(TITLE_PREFIX);
        return prefix >= 0 ? title.substring(prefix + TITLE_PREFIX.length()).trim() : title;
    }

    private void clickAttackItem() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != screen || !isGuildAttackScreen(screen)
                || client.player == null || getAttackItem().isEmpty() || overlayClick == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        Slot slot = handler.getSlot(ATTACK_SLOT);
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        Slot previousFocusedSlot = accessor.getFocusedSlot();
        Click slotClick = new Click(
                HandledScreenAccess.x(screen) + slot.x + 8,
                HandledScreenAccess.y(screen) + slot.y + 8,
                overlayClick.buttonInfo());

        forwardingSlotClick = true;
        accessor.setFocusedSlot(slot);
        try {
            screen.mouseClicked(slotClick, false);
        } finally {
            accessor.setFocusedSlot(previousFocusedSlot);
            forwardingSlotClick = false;
        }
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
    }

    public boolean mouseClicked(Click click) {
        overlayClick = click;
        try {
            return mouseClicked(click.x(), click.y(), click.button());
        } finally {
            overlayClick = null;
        }
    }

    public static boolean isForwardingSlotClick() {
        return forwardingSlotClick;
    }

    public static void renderVanillaToggleButton(DrawContext ctx, HandledScreen<?> screen) {
        if (WynnExtrasConfig.INSTANCE.guildAttackOverlayEnabled || !isGuildAttackScreen(screen)) return;

        AttackTimer.renderInScreen(ctx);
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        double mouseX = client.mouse.getX() * screenWidth / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        UIUtils vanillaUi = new UIUtils(ctx, 1, 0, 0);
        layoutToggleButton(VANILLA_TOGGLE_BUTTON, screenWidth, 1f);
        VANILLA_TOGGLE_BUTTON.draw(ctx, (int) mouseX, (int) mouseY, 0, vanillaUi);
    }

    public static boolean handleVanillaToggleClick(double mouseX, double mouseY, HandledScreen<?> screen) {
        if (WynnExtrasConfig.INSTANCE.guildAttackOverlayEnabled || !isGuildAttackScreen(screen)) return false;

        layoutToggleButton(VANILLA_TOGGLE_BUTTON, MinecraftClient.getInstance().getWindow().getScaledWidth(), 1f);
        return VANILLA_TOGGLE_BUTTON.mouseClicked(mouseX, mouseY, 0);
    }

    private void layoutOverlayToggleButton() {
        layoutToggleButton(overlayToggleButton, screenWidth, 1f);
    }

    private static void layoutToggleButton(ToggleButton button, float screenWidth, float logicalScale) {
        int x = Math.round((screenWidth - TOGGLE_WIDTH - TOGGLE_MARGIN) * logicalScale);
        int y = Math.round(TOGGLE_MARGIN * logicalScale);
        button.setBounds(x, y, Math.round(TOGGLE_WIDTH * logicalScale), Math.round(TOGGLE_HEIGHT * logicalScale));
        button.setTextScale(TOGGLE_TEXT_SCALE * logicalScale);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
    }

    private static Text legacyText(String value) {
        MutableText result = Text.empty();
        Style style = Style.EMPTY;
        StringBuilder part = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character != Formatting.FORMATTING_CODE_PREFIX || i + 1 >= value.length()) {
                part.append(character);
                continue;
            }

            Formatting formatting = Formatting.byCode(Character.toLowerCase(value.charAt(i + 1)));
            if (formatting == null) {
                part.append(character);
                continue;
            }

            append(result, part, style);
            if (formatting == Formatting.RESET) {
                style = Style.EMPTY;
            } else if (formatting.isColor()) {
                style = Style.EMPTY.withColor(formatting);
            } else {
                style = style.withFormatting(formatting);
            }
            i++;
        }

        append(result, part, style);
        return result;
    }

    private static void append(MutableText result, StringBuilder part, Style style) {
        if (part.isEmpty()) return;
        result.append(Text.literal(part.toString()).setStyle(style));
        part.setLength(0);
    }

    private record AttackDetails(String defenses, String price, String queueTime, Boolean affordable) {
        private static AttackDetails from(List<Text> lore) {
            String defenses = "Unknown";
            String price = "Unknown";
            String queueTime = "Unknown";
            Boolean affordable = null;

            for (Text text : lore) {
                String line = clean(text.getString());
                if (line.startsWith("Territory Defences:")) {
                    defenses = line.substring("Territory Defences:".length()).trim();
                } else if (line.startsWith("Price:")) {
                    String value = line.substring("Price:".length()).trim();
                    if (value.contains("✔")) affordable = true;
                    else if (value.contains("✖")) affordable = false;
                    price = value.replace("✔", "").replace("✖", "").trim();
                } else if (line.startsWith("Time to Start:")) {
                    queueTime = line.substring("Time to Start:".length()).trim();
                }
            }

            return new AttackDetails(defenses, price, queueTime, affordable);
        }

        private String priceText() {
            if (affordable == null) return "? " + price;
            return (affordable ? "✔ " : "✖ ") + price;
        }

        private CustomColor affordabilityColor() {
            if (affordable == null) return CustomColor.fromHexString("AAAAAA");
            return CustomColor.fromHexString(affordable ? "55FF55" : "FF5555");
        }

        private CustomColor defenseColor() {
            return switch (defenses) {
                case "Very Low", "Low" -> CustomColor.fromHexString("55FF55");
                case "Medium" -> CustomColor.fromHexString("FFFF55");
                case "High" -> CustomColor.fromHexString("FF5555");
                case "Very High" -> CustomColor.fromHexString("AA0000");
                default -> CustomColor.fromHexString("AAAAAA");
            };
        }
    }

    private class AttackButton extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawVanillaPanelButton(x, y, width, height, 5, 2, hovered && enabled);
            ui.drawCenteredText(enabled ? "Attack " + territoryName() : "Waiting for attack information...",
                    x + width / 2f, y + height / 2f,
                    enabled ? CustomColor.fromHexString("FFFFFF") : CustomColor.fromHexString("777777"), 1.5f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button == 0) clickAttackItem();
            return true;
        }
    }

    private static class ToggleButton extends Widget {
        private float textScale = TOGGLE_TEXT_SCALE;

        private void setTextScale(float textScale) {
            this.textScale = textScale;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(WynnExtrasConfig.INSTANCE.guildAttackOverlayEnabled
                            ? "Disable attack overlay" : "Enable attack overlay",
                    x + width / 2f, y + height / 2f, CustomColor.fromHexString("FFFFFF"), textScale);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != 0) return true;
            WynnExtrasConfig.INSTANCE.guildAttackOverlayEnabled = !WynnExtrasConfig.INSTANCE.guildAttackOverlayEnabled;
            WynnExtrasConfig.save();
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }
}
