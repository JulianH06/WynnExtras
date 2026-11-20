package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.config.SavedCraft;
import julianh06.wynnextras.features.buildplanner.config.SavedCraftManager;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.SmoothScroll;
import julianh06.wynnextras.utils.UI.TextInputWidget;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.wynncraft.state.CharacterClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

final class CraftedItemOverlay extends Widget {
    private static final int PADDING = 20;
    private static final int INPUT_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 34;
    private static final int ROW_HEIGHT = 58;
    private static final float TEXT_SCALE = 1.5F;
    private static final float INPUT_TEXT_SCALE = 3.6F;

    private final BuilderScreen screen;
    private final String slotType;
    private final String slotId;
    private final Consumer<WynnItem> equip;
    private final Runnable cancel;
    private final SmoothScroll scroll = new SmoothScroll();
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    private final int panelWidth;
    private final int panelHeight;
    private final int left;
    private final int top;
    private final TextInputWidget codeField;
    private final TextInputWidget searchField;
    private final CraftedList craftList;
    private List<CraftEntry> crafts = List.of();
    private WynnItem decodedItem;

    CraftedItemOverlay(BuilderScreen screen, String slotType, String slotId, WynnItem current,
            Consumer<WynnItem> equip, Runnable cancel, int width, int height) {
        super(0, 0, width, height);
        setUiScaleFactor(1.0);
        this.screen = screen;
        this.slotType = slotType;
        this.slotId = slotId;
        this.equip = equip;
        this.cancel = cancel;
        panelWidth = Math.min(780, width - 80);
        panelHeight = Math.min(490, height - 70);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        codeField = input(left + PADDING, top + 43, panelWidth - PADDING * 2 - 108, INPUT_HEIGHT,
                "WynnBuilder crafted link or hash");
        codeField.setMaxLength(2048);
        codeField.setInputAndMoveCursorToEnd(current != null && current.isCrafted() ? current.craftedCode() : "");
        codeField.setOnChange(this::updatePreview);
        addChild(new ThemedButton(left + panelWidth - PADDING - 96, top + 43, 96, INPUT_HEIGHT,
                Text.literal("Load"), this::loadInput).setTextScale(TEXT_SCALE));

        searchField = input(left + PADDING, top + 118, panelWidth - PADDING * 2, 30,
                "Search saved crafts...");
        searchField.setMaxLength(64);
        searchField.setOnChange(value -> rebuildCrafts());

        craftList = new CraftedList(left + PADDING, top + 158,
                panelWidth - PADDING * 2, panelHeight - 218);
        addChild(craftList);

        int footer = top + panelHeight - BUTTON_HEIGHT - 12;
        addChild(new ThemedButton(left + PADDING, footer, 150, BUTTON_HEIGHT,
                Text.literal("Open Crafter"), this::openCrafter).setTextScale(TEXT_SCALE));
        addChild(new ThemedButton(left + panelWidth - PADDING - 110, footer, 110, BUTTON_HEIGHT,
                Text.literal("Cancel"), this::cancel).setTextScale(TEXT_SCALE));
        loadCrafts();
        updatePreview(codeField.getInput());
    }

    private TextInputWidget input(int x, int y, int width, int height, String placeholder) {
        TextInputWidget field = new TextInputWidget(
                x * 2, y * 2, width * 2, height * 2, 12, 16, INPUT_TEXT_SCALE);
        field.setPlaceholder(placeholder);
        field.setBackgroundColor(CustomColor.fromInt(UiTheme.SURFACE));
        field.setFocusedColor(CustomColor.fromInt(UiTheme.hoverBackground()));
        field.setTextColor(CustomColor.fromInt(UiTheme.TEXT));
        field.setPlaceholderColor(CustomColor.fromInt(UiTheme.MUTED));
        field.setCursorColor(CustomColor.fromInt(UiTheme.TEXT));
        return field;
    }

    List<TextInputWidget> inputFields() {
        return List.of(codeField, searchField);
    }

    private void loadCrafts() {
        List<CraftEntry> loaded = new ArrayList<>();
        for (SavedCraft saved : SavedCraftManager.getInstance().getAll()) {
            try {
                loaded.add(new CraftEntry(saved, CraftedItemCodec.decodeForSlot(saved.code(), slotType)));
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
        }
        crafts = loaded;
        rebuildCrafts();
    }

    private void rebuildCrafts() {
        String query = searchField.getInput().trim().toLowerCase(Locale.ROOT);
        craftList.setEntries(crafts.stream().filter(entry -> query.isEmpty()
                || entry.saved().name().toLowerCase(Locale.ROOT).contains(query)
                || entry.item().displayName().toLowerCase(Locale.ROOT).contains(query)
                || entry.item().subType().toLowerCase(Locale.ROOT).contains(query)).toList());
    }

    private void updatePreview(String input) {
        decodedItem = null;
        if (input == null || input.isBlank()) return;
        try {
            decodedItem = CraftedItemCodec.decodeForSlot(input, slotType);
        } catch (IllegalArgumentException ignored) {
        } catch (IllegalStateException exception) {
            PlannerLog.LOGGER.error("Could not load crafted item data", exception);
        }
    }

    private void loadInput() {
        updatePreview(codeField.getInput());
        if (decodedItem != null) equip.accept(decodedItem);
    }

    private void cancel() {
        cancel.run();
    }

    private void openCrafter() {
        updatePreview(codeField.getInput());
        if (!codeField.getInput().isBlank() && decodedItem == null) return;
        WorkspaceTabs.openCrafter(screen, slotId, decodedItem);
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
        ui.fill(0, 0, width, height, 0x99000000);
        UiTheme.drawPanel(context, ui, left, top, panelWidth, panelHeight);
        drawScaled("Paste a WynnBuilder crafted link or hash", left + PADDING, top + 27, TEXT_SCALE, UiTheme.TEXT);
        drawScaled("Saved crafts", left + PADDING, top + 101, TEXT_SCALE, UiTheme.TEXT);
    }

    private void drawScaled(String text, int x, int centerY, float scale, int color) {
        ui.pushMatrix();
        ui.translate(x, centerY);
        ui.scale(scale, scale);
        ui.drawTextWithShadow(textRenderer, text, 0, -4, color);
        ui.popMatrix();
    }

    @Override
    protected boolean onClick(int button) {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    private final class CraftedList extends Widget {
        private final PlannerScrollbar scrollbar = new PlannerScrollbar(scroll, 12);
        private List<CraftEntry> entries = List.of();
        private int maxScroll;

        private CraftedList(int left, int top, int width, int height) {
            super(left, top, width, height);
        }

        private void setEntries(List<CraftEntry> entries) {
            this.entries = List.copyOf(entries);
            scroll.jump(0.0F);
        }

        @Override
        protected void drawContent(DrawContext context, int mouseX, int mouseY, float delta) {
            maxScroll = Math.max(0, entries.size() * ROW_HEIGHT - height);
            scroll.clamp(0.0F, maxScroll);
            int offset = Math.round(scroll.update(delta));
            CraftEntry hoveredEntry = null;
            ui.enableScissor(x, y, x + width, y + height);
            try {
                if (entries.isEmpty()) {
                    ui.drawCenteredTextWithShadow(textRenderer, "No matching saved crafts",
                            x + width / 2, y + 18, UiTheme.MUTED);
                }
                for (int index = 0; index < entries.size(); index++) {
                    int rowY = y + index * ROW_HEIGHT - offset;
                    if (rowY + ROW_HEIGHT <= y || rowY >= y + height) continue;
                    CraftEntry entry = entries.get(index);
                    if (drawEntry(context, entry, rowY, mouseX, mouseY)) hoveredEntry = entry;
                }
            } finally {
                ui.disableScissor();
            }
            scrollbar.configure(x + width - 5, y, height, maxScroll);
            scrollbar.draw(context, mouseX, mouseY, delta, ui);
            if (hoveredEntry != null) {
                WynnItemTooltip.render(screen, context, textRenderer, hoveredEntry.item(), mouseX, mouseY,
                        CharacterClass.ARCHER, 121, new int[5], false);
            }
        }

        private boolean drawEntry(DrawContext context, CraftEntry entry, int rowY, int mouseX, int mouseY) {
            int contentWidth = width - 9;
            boolean hovered = mouseX >= x && mouseX < x + contentWidth
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 4;
            UiTheme.drawRoundedBox(context, ui, x, rowY, contentWidth, ROW_HEIGHT - 4,
                    hovered ? UiTheme.hoverBackground() : UiTheme.SURFACE,
                    hovered ? UiTheme.TEXT : UiTheme.BORDER);
            EquipmentSprites.draw(context, entry.item().subType(), x + 9, rowY + 9, 34);
            drawText(entry.saved().name(), x + 52, rowY + 18, 1.35F, 0xFF55DDCC);
            drawText("Level " + entry.item().stat("lvlLow") + "-" + entry.item().stat("lvl"),
                    x + 52, rowY + 39, 1.05F, UiTheme.MUTED);
            return hovered;
        }

        private void drawText(String text, int x, int centerY, float scale, int color) {
            String fitted = fit(text, Math.round((width - 75) / scale));
            ui.pushMatrix();
            ui.translate(x, centerY);
            ui.scale(scale, scale);
            ui.drawTextWithShadow(textRenderer, fitted, 0, -4, color);
            ui.popMatrix();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !contains((int) mouseX, (int) mouseY)) return false;
            if (scrollbar.mouseClicked(mouseX, mouseY, button)) return true;
            int index = (int) (mouseY - y + scroll.position()) / ROW_HEIGHT;
            if (index < 0 || index >= entries.size()) return true;
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            equip.accept(entries.get(index).item());
            return true;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return scrollbar.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return scrollbar.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        protected boolean onScroll(double amount) {
            scroll.move((float) -amount * 48.0F, 0.0F, maxScroll);
            return true;
        }
    }

    private String fit(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0 && textRenderer.getWidth(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return value.substring(0, end) + ellipsis;
    }

    private record CraftEntry(SavedCraft saved, WynnItem item) {}
}
