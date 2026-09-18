package julianh06.wynnextras.features.buildplanner.gui;

import julianh06.wynnextras.features.buildplanner.PlannerLog;
import julianh06.wynnextras.features.buildplanner.data.CraftedItemCodec;
import julianh06.wynnextras.features.buildplanner.data.WynnItem;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

final class CraftedItemScreen extends Screen {
    private final WynnBuilderScreen parent;
    private final String slotType;
    private final String slotId;
    private final Consumer<WynnItem> equip;
    private String input;
    private String status = "";
    private WynnItem preview;
    private TextFieldWidget codeField;
    private ThemedButton equipButton;
    private ThemedButton copyButton;
    private int panelWidth;
    private int panelHeight;

    CraftedItemScreen(WynnBuilderScreen parent, String slotType, String slotId, WynnItem current, Consumer<WynnItem> equip) {
        super(Text.literal("Crafted Item"));
        this.parent = parent;
        this.slotType = slotType;
        this.slotId = slotId;
        this.equip = equip;
        input = current != null && current.isCrafted() ? current.craftedCode() : "";
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(440, width - 24);
        panelHeight = Math.min(222, height - 20);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        codeField = new TextFieldWidget(textRenderer, left + 14, top + 43,
                panelWidth - 100, 20, Text.literal("WynnCrafter code or link"));
        codeField.setMaxLength(2048);
        codeField.setPlaceholder(Text.literal("CR-... or WynnCrafter link"));
        codeField.setText(input);
        addDrawableChild(codeField);
        addDrawableChild(new ThemedButton(left + panelWidth - 78, top + 43, 64, 20,
                Text.literal("Paste"), () -> codeField.setText(client.keyboard.getClipboard())));
        int footer = top + panelHeight - 30;
        int buttonWidth = (panelWidth - 46) / 4;
        addDrawableChild(new ThemedButton(left + 14, footer, buttonWidth, 20, Text.literal("Cancel"), this::close));
        addDrawableChild(new ThemedButton(left + 20 + buttonWidth, footer, buttonWidth, 20,
                Text.literal("WynnCrafter"), () -> WorkspaceTabs.openCrafter(parent, slotId, preview)));
        copyButton = addDrawableChild(new ThemedButton(left + 26 + buttonWidth * 2, footer, buttonWidth, 20,
                Text.literal("Copy Code"), () -> client.keyboard.setClipboard(preview.craftedCode())));
        equipButton = addDrawableChild(new ThemedButton(left + 32 + buttonWidth * 3, footer, buttonWidth, 20,
                Text.literal("Equip"), () -> {
                    equip.accept(preview);
                    close();
                }));
        codeField.setChangedListener(value -> {
            input = value;
            updatePreview();
        });
        updatePreview();
        setInitialFocus(codeField);
    }

    private void updatePreview() {
        preview = null;
        status = "";
        if (!input.isBlank()) {
            try {
                preview = CraftedItemCodec.decodeForSlot(input, slotType);
            } catch (IllegalArgumentException exception) {
                status = exception.getMessage();
            } catch (IllegalStateException exception) {
                PlannerLog.LOGGER.error("Could not load crafted item data.", exception);
                status = "Crafting data could not be loaded. See the log.";
            }
        }
        equipButton.active = preview != null;
        copyButton.active = preview != null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        UiTheme.drawPanel(context, left, top, panelWidth, panelHeight);
        context.drawCenteredTextWithShadow(textRenderer, "Crafted " + slotType, width / 2, top + 12, UiTheme.accentRgb());
        String instruction = java.util.Set.of("ring", "bracelet", "necklace").contains(slotType)
                ? "Any ring, bracelet or necklace code works here"
                : "Paste a WynnCrafter code or item link";
        context.drawTextWithShadow(textRenderer, instruction, left + 14, top + 29, 0xFFAAAAAA);
        if (preview != null) {
            context.drawTextWithShadow(textRenderer, preview.displayName(), left + 14, top + 78, 0xFF00AAAA);
            context.drawTextWithShadow(textRenderer, "Material tiers: " + preview.stat("materialTier1")
                    + " / " + preview.stat("materialTier2") + "    Powder slots: " + preview.stat("powderSlots"),
                    left + 14, top + 95, 0xFFE8E8E8);
            context.drawTextWithShadow(textRenderer, "Durability: " + preview.stat("durabilityMin")
                    + "-" + preview.stat("durabilityMax"), left + 14, top + 110, 0xFFE8E8E8);
            String base = "weapon".equals(preview.type())
                    ? "Base damage: " + preview.stat("nDamMin") + "-" + preview.stat("nDamMax")
                            + "    Speed: " + preview.attackSpeed()
                    : "Base health: " + preview.stat("hp");
            context.drawTextWithShadow(textRenderer, base, left + 14, top + 125, 0xFFE8E8E8);
            context.drawTextWithShadow(textRenderer, "Uses maximum rolls at full durability.", left + 14, top + 143, 0xFFAAAAAA);
        } else if (status.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "Legacy and current CR- codes are supported.",
                    width / 2, top + 98, 0xFFAAAAAA);
        }
        if (!status.isEmpty()) {
            int y = top + 82;
            for (var line : textRenderer.wrapLines(Text.literal(status), panelWidth - 28)) {
                context.drawTextWithShadow(textRenderer, line, left + 14, y, 0xFFFF5555);
                y += 12;
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
