package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.features.inventory.WeightDisplay;
import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import julianh06.wynnextras.wynncraft.item.IdentifierRollResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Random;

public final class IdentifierCaseOpeningOverlay extends WEMenuExtension {
    private static final int[] IDENTIFIER_INPUT_SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};
    private static final int[] AUGMENT_INPUT_SLOTS = {11};
    private static final long FADE_IN_MS = 320;
    private static final long RESULT_LOOKUP_GRACE_MS = 700;
    private static final long FADE_OUT_MS = 280;
    private static final long MINIMUM_SETTLE_MS = 3_000;
    private static final long DONE_FADE_IN_MS = 350;
    private static final long SOUND_COOLDOWN_MS = 55;

    private enum State {
        IDLE,
        FADING_IN,
        SPINNING,
        SETTLING,
        RESULTS,
        FADING_OUT
    }

    private record InputSnapshot(int slot, ItemStack stack, List<String> possibleNames) {}
    private record IdentifiedResult(ItemStack stack, float overallRoll) {}

    private final BackdropWidget backdropWidget = new BackdropWidget();
    private final MachinePanelWidget machinePanelWidget = new MachinePanelWidget();
    private final DoneButtonWidget doneButtonWidget = new DoneButtonWidget();
    private final List<RollStripWidget> rollWidgets = new ArrayList<>();

    private State state = State.IDLE;
    private long stateStartedAt;
    private long spinStartedAt;
    private long settleDurationMs = 3_000;
    private long resultCandidateSince;
    private long lastSoundAt;
    private int lastSoundCard = -1;
    private boolean wasWaiting;
    private List<InputSnapshot> cachedInputs = List.of();
    private List<IdentifiedResult> results = List.of();

    public IdentifierCaseOpeningOverlay() {
        rebuildWidgetOrder();
        setCaseWidgetsVisible(false);
    }

    public boolean isReplacingMenu() {
        return state != State.IDLE;
    }

    public boolean shouldHideVanilla() {
        float progress = fadeProgress();
        return switch (state) {
            case IDLE -> false;
            case FADING_IN -> progress >= 0.5f;
            case FADING_OUT -> progress < 0.5f;
            default -> true;
        };
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isReplacingMenu()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && state == State.RESULTS) startFadeOut();
        return true;
    }

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!isIdentifierMenu()) {
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        updateState(now);
        if (state == State.IDLE) return;

        setCaseWidgetsVisible(true);
        layoutWidgets();
        playScrollSound(now);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (state != State.RESULTS) return;
        for (RollStripWidget widget : rollWidgets) {
            if (!widget.resultHovered || widget.resultStack.isEmpty()) continue;
            MinecraftClient client = MinecraftClient.getInstance();
            List<Text> tooltip = widget.resultStack.getTooltip(Item.TooltipContext.DEFAULT, client.player, TooltipType.BASIC);
            ctx.drawTooltip(client.textRenderer, tooltip, mouseX, mouseY);
            return;
        }
    }

    private void updateState(long now) {
        ScreenHandler handler = MinecraftUtils.containerMenu();
        if (handler == null || handler.slots.size() <= 11) {
            reset();
            return;
        }

        boolean waiting = isWaiting(handler);
        if (state == State.IDLE) {
            if (!waiting) cachedInputs = collectInputs(handler);
            if (waiting && !wasWaiting && !cachedInputs.isEmpty()) startOpening(now);
            wasWaiting = waiting;
            return;
        }

        if ((state == State.FADING_IN || state == State.SPINNING) && !waiting) {
            captureResults(handler, now);
        }

        if (state == State.FADING_IN && now - stateStartedAt >= FADE_IN_MS) {
            state = State.SPINNING;
            stateStartedAt = now;
            spinStartedAt = now;
        }

        if (state == State.SPINNING && !results.isEmpty()) {
            beginSettling(now);
        }

        if (state == State.SETTLING && now - stateStartedAt >= settleDurationMs) {
            state = State.RESULTS;
            stateStartedAt = now;
            MinecraftUtils.playSoundUI(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.35f, 1.1f);
        }

        if (state == State.FADING_OUT && now - stateStartedAt >= FADE_OUT_MS) {
            finishFadeOut(handler, waiting);
        }
    }

    private void startOpening(long now) {
        state = State.FADING_IN;
        stateStartedAt = now;
        spinStartedAt = 0;
        resultCandidateSince = 0;
        lastSoundAt = 0;
        lastSoundCard = -1;
        results = List.of();
        rollWidgets.clear();

        Random random = new Random(now ^ cachedInputs.size() * 31L);
        for (int i = 0; i < cachedInputs.size(); i++) {
            rollWidgets.add(new RollStripWidget(i, random.nextLong()));
        }
        rebuildWidgetOrder();
        setCaseWidgetsVisible(true);
    }

    private void captureResults(ScreenHandler handler, long now) {
        List<ItemStack> stacks = new ArrayList<>(cachedInputs.size());
        for (InputSnapshot input : cachedInputs) {
            if (input.slot() >= handler.slots.size()) return;
            ItemStack stack = handler.getSlot(input.slot()).getStack();
            if (isEmptyInput(stack)) return;
            stacks.add(stack.copy());
        }

        if (resultCandidateSince == 0) resultCandidateSince = now;
        List<IdentifiedResult> found = new ArrayList<>(stacks.size());
        boolean allRollsAvailable = true;
        for (ItemStack stack : stacks) {
            OptionalDouble overall = findOverallPercentage(stack);
            if (overall.isEmpty()) allRollsAvailable = false;
            found.add(new IdentifiedResult(stack, overall.isPresent() ? (float) overall.getAsDouble() : Float.NaN));
        }

        if (allRollsAvailable || now - resultCandidateSince >= RESULT_LOOKUP_GRACE_MS) {
            results = List.copyOf(found);
        }
    }

    private void beginSettling(long now) {
        state = State.SETTLING;
        stateStartedAt = now;
        for (int i = 0; i < rollWidgets.size(); i++) {
            IdentifiedResult result = results.get(i);
            rollWidgets.get(i).prepareSettling(result.stack(), result.overallRoll(), now);
        }

        float settleSeconds = MINIMUM_SETTLE_MS / 1_000f;
        for (int iteration = 0; iteration < 12; iteration++) {
            float requiredSeconds = settleSeconds;
            for (RollStripWidget widget : rollWidgets) {
                requiredSeconds = Math.max(requiredSeconds, widget.selectSettleTarget(settleSeconds));
            }
            if (requiredSeconds <= settleSeconds + 0.001f) break;
            settleSeconds = requiredSeconds;
        }
        settleDurationMs = Math.max(MINIMUM_SETTLE_MS, (long) Math.ceil(settleSeconds * 1_000f));
        for (RollStripWidget widget : rollWidgets) widget.selectSettleTarget(settleDurationMs / 1_000f);
    }

    private void startFadeOut() {
        if (state != State.RESULTS) return;
        state = State.FADING_OUT;
        stateStartedAt = System.currentTimeMillis();
        MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.7f);
    }

    private void finishFadeOut(ScreenHandler handler, boolean waiting) {
        state = State.IDLE;
        stateStartedAt = 0;
        spinStartedAt = 0;
        resultCandidateSince = 0;
        results = List.of();
        rollWidgets.clear();
        rebuildWidgetOrder();
        setCaseWidgetsVisible(false);
        wasWaiting = waiting;
        if (!waiting) cachedInputs = collectInputs(handler);
    }

    private void reset() {
        state = State.IDLE;
        cachedInputs = List.of();
        results = List.of();
        rollWidgets.clear();
        wasWaiting = false;
        rebuildWidgetOrder();
        setCaseWidgetsVisible(false);
    }

    private void rebuildWidgetOrder() {
        rootWidgets.clear();
        rootWidgets.add(backdropWidget);
        rootWidgets.add(machinePanelWidget);
        rootWidgets.addAll(rollWidgets);
        rootWidgets.add(doneButtonWidget);
    }

    private void setCaseWidgetsVisible(boolean visible) {
        backdropWidget.setVisible(visible);
        machinePanelWidget.setVisible(visible);
        for (RollStripWidget widget : rollWidgets) widget.setVisible(visible);
        doneButtonWidget.setVisible(visible && state == State.RESULTS);
        doneButtonWidget.setEnabled(visible && state == State.RESULTS);
    }

    private void layoutWidgets() {
        int rows = Math.max(1, (rollWidgets.size() + 1) / 2);
        int panelWidth = Math.clamp(screenWidth - 24, 280, 720);
        int panelHeight = Math.clamp(56 + rows * 66, 160, screenHeight - 12);
        int panelX = (screenWidth - panelWidth) / 2;
        int panelY = (screenHeight - panelHeight) / 2;

        backdropWidget.setBounds(0, 0, screenWidth, screenHeight);
        machinePanelWidget.setBounds(panelX, panelY, panelWidth, panelHeight);

        int contentX = panelX + 12;
        int contentY = panelY + 10;
        int contentWidth = panelWidth - 24;
        int contentHeight = panelHeight - 49;
        int gap = 8;
        int rowHeight = Math.max(25, (contentHeight - gap * (rows - 1)) / rows);
        int columnWidth = (contentWidth - gap) / 2;
        int widgetIndex = 0;
        int row = 0;

        if (rollWidgets.size() % 2 == 1) {
            rollWidgets.get(widgetIndex++).setBounds(contentX, contentY, contentWidth, rowHeight);
            row++;
        }

        while (widgetIndex < rollWidgets.size()) {
            int y = contentY + row * (rowHeight + gap);
            rollWidgets.get(widgetIndex++).setBounds(contentX, y, columnWidth, rowHeight);
            if (widgetIndex < rollWidgets.size()) {
                rollWidgets.get(widgetIndex++).setBounds(contentX + columnWidth + gap, y, columnWidth, rowHeight);
            }
            row++;
        }

        doneButtonWidget.setBounds(panelX + panelWidth / 2 - 48, panelY + panelHeight - 31, 96, 21);
        doneButtonWidget.setVisible(state == State.RESULTS);
        doneButtonWidget.setEnabled(state == State.RESULTS);
    }

    private void playScrollSound(long now) {
        if (rollWidgets.isEmpty() || spinStartedAt == 0 || state == State.RESULTS || state == State.FADING_OUT) return;
        int card = rollWidgets.getFirst().centerCard(now);
        if (card == lastSoundCard || now - lastSoundAt < SOUND_COOLDOWN_MS) return;
        lastSoundCard = card;
        lastSoundAt = now;
        MinecraftUtils.playSoundAmbient(SoundEvents.UI_BUTTON_CLICK.value(), 0.12f, 1.75f);
    }

    private static List<InputSnapshot> collectInputs(ScreenHandler handler) {
        List<InputSnapshot> inputs = new ArrayList<>();
        int[] inputSlots = WynncraftMenuService.isCurrent(MenuType.AUGMENT_IDENTIFIER)
                ? AUGMENT_INPUT_SLOTS
                : IDENTIFIER_INPUT_SLOTS;
        for (int slot : inputSlots) {
            if (slot >= handler.slots.size()) continue;
            ItemStack stack = handler.getSlot(slot).getStack();
            if (!isEmptyInput(stack)) {
                ItemStack copy = stack.copy();
                inputs.add(new InputSnapshot(slot, copy, IdentifierRollResolver.possibleItemNames(copy)));
            }
        }
        return List.copyOf(inputs);
    }

    private static boolean isWaiting(ScreenHandler handler) {
        for (int slot = 0; slot < handler.slots.size(); slot++) {
            String name = cleanName(handler.getSlot(slot).getStack());
            if (name.toLowerCase(Locale.ROOT).startsWith("please wait")) return true;
        }
        return false;
    }

    private static boolean isEmptyInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        String name = cleanName(stack);
        return name.equalsIgnoreCase("Empty Item Slot") || name.equalsIgnoreCase("Item Slot");
    }

    private static boolean isIdentifierMenu() {
        return WynncraftMenuService.isCurrentAny(MenuType.ITEM_IDENTIFIER, MenuType.AUGMENT_IDENTIFIER);
    }

    private static String cleanName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        return IdentifierRollResolver.cleanDisplayName(stack.getName().getString());
    }

    private static OptionalDouble findOverallPercentage(ItemStack stack) {
        return IdentifierRollResolver.overallPercentage(stack);
    }

    private float alpha() {
        long elapsed = System.currentTimeMillis() - stateStartedAt;
        return switch (state) {
            case FADING_IN -> Math.clamp(elapsed / (float) FADE_IN_MS, 0f, 1f);
            case FADING_OUT -> 1f - Math.clamp(elapsed / (float) FADE_OUT_MS, 0f, 1f);
            case IDLE -> 0f;
            default -> 1f;
        };
    }

    private float fadeProgress() {
        long duration = state == State.FADING_OUT ? FADE_OUT_MS : FADE_IN_MS;
        return Math.clamp((System.currentTimeMillis() - stateStartedAt) / (float) duration, 0f, 1f);
    }

    private float backdropOpacity() {
        float progress = fadeProgress();
        return switch (state) {
            case FADING_IN -> progress < 0.5f
                    ? 0.92f * progress * 2f
                    : 0.92f + (0.5f - 0.92f) * (progress - 0.5f) * 2f;
            case FADING_OUT -> progress < 0.5f
                    ? 0.5f + (0.92f - 0.5f) * progress * 2f
                    : 0.92f * (1f - (progress - 0.5f) * 2f);
            case IDLE -> 0f;
            default -> 0.5f;
        };
    }

    private static CustomColor color(String hex, float alpha) {
        return CustomColor.fromHexString(hex).withAlpha(alpha);
    }

    private static String formatRollValue(float roll) {
        float rounded = Math.round(roll * 10f) / 10f;
        if (rounded == Math.rint(rounded)) return String.format(Locale.ROOT, "%.0f", rounded);
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private float doneAlpha() {
        if (state != State.RESULTS) return 0f;
        return Math.clamp((System.currentTimeMillis() - stateStartedAt) / (float) DONE_FADE_IN_MS, 0f, 1f);
    }

    private final class BackdropWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawRect(x, y, width, height, color("000000", backdropOpacity()));
        }
    }

    private final class MachinePanelWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawVanillaPanel(x, y, width, height, 5, alpha());
        }
    }

    private final class DoneButtonWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            float alpha = doneAlpha();
            ui.drawVanillaPanelButton(x, y, width, height, 5, 2, hovered, alpha);
            ui.drawCenteredText("Done", x + width / 2f, y + height / 2f,
                    CustomColor.fromHexString("FFFFFF").withAlpha(alpha), 1f);
        }

        @Override
        protected boolean onClick(int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || state != State.RESULTS) return false;
            startFadeOut();
            return true;
        }
    }

    private final class RollStripWidget extends Widget {
        private static final int CARD_WIDTH = 144;
        private static final int CARD_GAP = 6;
        private static final int CARD_STRIDE = CARD_WIDTH + CARD_GAP;
        private static final float START_SPEED = 1_800f;
        private static final float SPEED_DECAY_SECONDS = 1.6f;

        private final int itemIndex;
        private final long seed;
        private ItemStack resultStack = ItemStack.EMPTY;
        private float resultRoll = Float.NaN;
        private float settleStartOffset;
        private float settleStartVelocity;
        private float settleStopPosition;
        private int firstOffscreenCard;
        private float settleTargetOffset;
        private int resultCardIndex = -1;
        private boolean resultHovered;

        private RollStripWidget(int itemIndex, long seed) {
            this.itemIndex = itemIndex;
            this.seed = seed;
        }

        private void prepareSettling(ItemStack stack, float overallRoll, long now) {
            resultStack = stack.copy();
            resultRoll = overallRoll;
            settleStartOffset = spinningOffset(now);
            settleStartVelocity = spinningVelocity(now);
            double randomPosition = (mix(0x39A7F4C1L) >>> 11) * 0x1.0p-53;
            settleStopPosition = (float) ((randomPosition * 2d - 1d) * CARD_WIDTH * 0.44f);
            firstOffscreenCard = (int) Math.ceil(
                    (settleStartOffset + width + CARD_GAP) / CARD_STRIDE);
        }

        private float selectSettleTarget(float settleSeconds) {
            float idealTarget = settleStartOffset + settleStartVelocity * settleSeconds * 0.5f;
            resultCardIndex = Math.round(
                    (idealTarget - settleStopPosition + width / 2f - CARD_WIDTH / 2f) / CARD_STRIDE);
            resultCardIndex = Math.max(resultCardIndex, firstOffscreenCard);
            settleTargetOffset = resultCardIndex * CARD_STRIDE + CARD_WIDTH / 2f
                    - width / 2f + settleStopPosition;
            float minimumDistance = settleStartVelocity * settleSeconds / 3f;
            while (settleTargetOffset - settleStartOffset < minimumDistance) {
                resultCardIndex++;
                settleTargetOffset += CARD_STRIDE;
            }
            float distance = settleTargetOffset - settleStartOffset;
            return Math.max(MINIMUM_SETTLE_MS / 1_000f, distance * 1.51f / settleStartVelocity);
        }

        private int centerCard(long now) {
            return Math.round((offset(now) + width / 2f) / CARD_STRIDE);
        }

        private float offset(long now) {
            if (spinStartedAt == 0) return Math.floorMod(seed, CARD_STRIDE * 5L);
            if (state == State.SETTLING || state == State.RESULTS || state == State.FADING_OUT) {
                float progress = state == State.SETTLING
                        ? Math.clamp((now - stateStartedAt) / (float) settleDurationMs, 0f, 1f)
                        : 1f;
                float t2 = progress * progress;
                float t3 = t2 * progress;
                float distance = settleTargetOffset - settleStartOffset;
                float seconds = settleDurationMs / 1_000f;
                float distanceCurve = -2f * t3 + 3f * t2;
                float velocityCurve = t3 - 2f * t2 + progress;
                return settleStartOffset
                        + distance * distanceCurve
                        + settleStartVelocity * seconds * velocityCurve;
            }
            return spinningOffset(now);
        }

        private float spinningOffset(long now) {
            float startOffset = Math.floorMod(seed, CARD_STRIDE * 5L);
            if (spinStartedAt == 0) return startOffset;
            float seconds = Math.max(0, now - spinStartedAt) / 1_000f;
            float decayedDistance = START_SPEED * SPEED_DECAY_SECONDS
                    * (1f - (float) Math.exp(-seconds / SPEED_DECAY_SECONDS));
            return startOffset + decayedDistance;
        }

        private float spinningVelocity(long now) {
            if (spinStartedAt == 0) return START_SPEED;
            float seconds = Math.max(0, now - spinStartedAt) / 1_000f;
            return START_SPEED * (float) Math.exp(-seconds / SPEED_DECAY_SECONDS);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            float alpha = alpha();
            ui.drawRect(x, y, width, height, color("090C11", alpha));
            ui.drawRect(x + 1, y + 1, width - 2, height - 2, color("10151D", alpha));

            int viewportY = y + 1;
            int viewportHeight = Math.max(10, height - 2);
            float currentOffset = offset(System.currentTimeMillis());
            int firstCard = Math.max(0, (int) Math.floor(currentOffset / CARD_STRIDE) - 1);
            int visibleCards = width / CARD_STRIDE + 4;
            resultHovered = false;

            ctx.enableScissor((int) ui.sx(x + 1), (int) ui.sy(viewportY),
                    (int) ui.sx(x + width - 1), (int) ui.sy(viewportY + viewportHeight));
            for (int cardIndex = firstCard; cardIndex < firstCard + visibleCards; cardIndex++) {
                float cardX = x + cardIndex * CARD_STRIDE - currentOffset;
                boolean resultCard = cardIndex == resultCardIndex && !resultStack.isEmpty();
                float roll = resultCard ? resultRoll : randomRoll(cardIndex);
                drawCard(mouseX, mouseY, cardX, viewportY + 2, viewportHeight - 4,
                        cardName(cardIndex, resultCard), roll, resultCard, alpha);
            }
            ctx.disableScissor();

            float markerX = x + width / 2f;
            ui.drawRect(markerX - 1, viewportY, 2, viewportHeight, color("E4A83F", alpha));
            ui.drawRect(markerX - 5, viewportY, 10, 2, color("FFD36A", alpha));
            ui.drawRect(markerX - 5, viewportY + viewportHeight - 2, 10, 2, color("FFD36A", alpha));
        }

        private void drawCard(int mouseX, int mouseY, float cardX, int cardY, int cardHeight,
                              String name, float roll, boolean resultCard, float alpha) {
            float distance = Math.abs(cardX + CARD_WIDTH / 2f - (x + width / 2f));
            float emphasis = 1f - Math.clamp(distance / CARD_STRIDE, 0f, 1f);
            float visualWidth = CARD_WIDTH;
            CustomColor rollColor = rollColor(roll);
            CustomColor accent = rollColor.withAlpha(alpha);
            ui.drawRect(cardX, (float) cardY, visualWidth, (float) cardHeight, rollColor.withAlpha(alpha * 0.3f));

            float nameScale = 1.12f;
            List<String> nameLines = wrapName(name, visualWidth, nameScale);
            if (nameLines.size() == 1) {
                ui.drawCenteredText(nameLines.getFirst(), cardX + visualWidth / 2f, (float) cardY + (float) cardHeight * 0.34f,
                        color("F5F7FA", alpha), nameScale);
            } else {
                ui.drawCenteredText(nameLines.getFirst(), cardX + visualWidth / 2f, (float) cardY + (float) cardHeight * 0.24f,
                        color("F5F7FA", alpha), nameScale);
                ui.drawCenteredText(nameLines.get(1), cardX + visualWidth / 2f, (float) cardY + (float) cardHeight * 0.43f,
                        color("F5F7FA", alpha), nameScale);
            }
            ui.drawCenteredText(formatRoll(roll), cardX + visualWidth / 2f, (float) cardY + (float) cardHeight * 0.68f,
                    accent, 1f + emphasis * 0.06f);

            if (!resultCard) return;
            float sx = ui.sx(cardX);
            float sy = ui.sy((float) cardY);
            resultHovered = mouseX >= sx && mouseX < sx + ui.sw(visualWidth)
                    && mouseY >= sy && mouseY < sy + ui.sh((float) cardHeight);
            if (resultHovered) {
                ui.drawRect(cardX, (float) cardY, visualWidth, (float) cardHeight, color("FFFFFF", alpha * 0.1f));
            }
        }

        private List<String> wrapName(String name, float availableWidth, float textScale) {
            var renderer = MinecraftClient.getInstance().textRenderer;
            int maxWidth = Math.max(1, (int) ((availableWidth - 12) / textScale));
            if (renderer.getWidth(name) <= maxWidth) return List.of(name);

            String fitting = renderer.trimToWidth(name, maxWidth);
            int split = fitting.lastIndexOf(' ');
            if (split <= 0) split = fitting.length();
            String first = name.substring(0, split).trim();
            String second = name.substring(split).trim();
            if (renderer.getWidth(second) > maxWidth) {
                second = renderer.trimToWidth(second, Math.max(1, maxWidth - renderer.getWidth("..."))) + "...";
            }
            return second.isEmpty() ? List.of(first) : List.of(first, second);
        }

        private String cardName(int cardIndex, boolean resultCard) {
            if (resultCard) return cleanName(resultStack);
            List<String> names = cachedInputs.get(itemIndex).possibleNames();
            if (names.isEmpty()) return "Unknown Item";
            int nameIndex = (int) Math.floorMod(mix(cardIndex ^ 0x63D83595), names.size());
            return names.get(nameIndex);
        }

        private float randomRoll(int index) {
            long value = mix(index);
            double uniform = (value >>> 11) * 0x1.0p-53;
            return (float) (Math.pow(uniform, 2d) * 100d);
        }

        private long mix(long index) {
            long value = seed + index * 0x9E3779B97F4A7C15L;
            value ^= value >>> 30;
            value *= 0xBF58476D1CE4E5B9L;
            value ^= value >>> 27;
            value *= 0x94D049BB133111EBL;
            value ^= value >>> 31;
            return value;
        }

        private String formatRoll(float roll) {
            return Float.isNaN(roll) ? "N/A" : formatRollValue(roll) + "%";
        }

        private CustomColor rollColor(float roll) {
            if (Float.isNaN(roll)) return CustomColor.fromHexString("AAB2BF");
            return CustomColor.fromInt(WeightDisplay.getScaleColor(roll));
        }
    }
}
