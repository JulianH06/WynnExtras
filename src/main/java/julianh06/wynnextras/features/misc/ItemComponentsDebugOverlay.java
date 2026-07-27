package julianh06.wynnextras.features.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemComponentsDebugOverlay {
    private static final int MIN_WIDTH = 220;
    private static final int MIN_HEIGHT = 120;
    private static final int TITLE_HEIGHT = 18;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 10;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int RESIZE_GRIP = 12;
    private static final int MAX_RAW_LINE_CHARS = 220;
    private static final int MAX_LINES = 1_500;
    private static final int MAX_DETAIL_LINES = 400;
    private static final int MAX_DETAIL_CHARS = 32_000;
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService DETAIL_SERIALIZER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WynnExtras item component serializer");
        thread.setDaemon(true);
        return thread;
    });
    private static final ConcurrentLinkedQueue<ComponentResult> COMPLETED_RESULTS = new ConcurrentLinkedQueue<>();

    private static boolean visible = false;
    private static String title = "Item Components";
    private static List<String> itemLines = new ArrayList<>();
    private static List<ComponentEntry> components = new ArrayList<>();
    private static List<RenderedLine> wrappedLines = new ArrayList<>();
    private static int wrapWidth = -1;
    private static int x;
    private static int y;
    private static int width;
    private static int height;
    private static int scroll;
    private static boolean configLoaded = false;
    private static boolean dragging = false;
    private static boolean resizing = false;
    private static boolean selecting = false;
    private static int dragOffsetX;
    private static int dragOffsetY;
    private static int lastMouseX;
    private static int lastMouseY;
    private static long sessionId;
    private static long nextRequestId;
    private static SelectionPosition selectionStart;
    private static SelectionPosition selectionEnd;

    public static void registerInventoryScreenHooks() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) return;
            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
                    render(context, mouseX, mouseY));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, input) -> {
                if (handleKeyPressed(input.key(), input.modifiers())) return false;
                int debugKey = WynnExtrasConfig.INSTANCE.debugItemComponentsKey;
                if (debugKey == GLFW.GLFW_KEY_UNKNOWN || input.key() != debugKey) return true;
                return !openHoveredStack(inventoryScreen);
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) ->
                    !mouseClicked(click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, click) ->
                    !mouseReleased(click.x(), click.y(), click.button()));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, click, horizontalAmount, verticalAmount) ->
                    !mouseDragged(click.x(), click.y()));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontalAmount, verticalAmount) ->
                    !mouseScrolled(mouseX, mouseY, verticalAmount));
            ScreenEvents.remove(screen).register(s -> reset());
        });
    }

    public static void open(ItemStack newStack) {
        if (newStack == null || newStack.isEmpty()) return;
        loadConfig();
        sessionId++;
        scroll = 0;
        clearSelection();
        title = "Item Components: " + newStack.getName().getString();
        rebuildComponents(newStack.copy());
        wrapWidth = -1;
        visible = true;
        clampToScreen();
    }

    public static void close() {
        if (!visible) return;
        visible = false;
        dragging = false;
        resizing = false;
        clearSelection();
        sessionId++;
        saveConfig();
    }

    public static void reset() {
        visible = false;
        dragging = false;
        resizing = false;
        clearSelection();
        sessionId++;
        title = "Item Components";
        itemLines = new ArrayList<>();
        components = new ArrayList<>();
        wrappedLines = new ArrayList<>();
        wrapWidth = -1;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static boolean isDragging() {
        return dragging || resizing;
    }

    public static boolean openHoveredStack(HandledScreen<?> screen) {
        Slot slot = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (slot == null) slot = findSlotAtLastMouse(screen);
        if (slot == null || !slot.hasStack()) return false;

        open(slot.getStack());
        return true;
    }

    public static void render(DrawContext context, int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (!visible) return;
        applyCompletedResults();
        loadConfig();
        clampToScreen();

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        rebuildWrappedLinesIfNeeded(tr);

        int right = x + width;
        int bottom = y + height;
        boolean hovered = isInBounds(mouseX, mouseY, x, y, width, height);

        context.fill(x, y, right, bottom, 0xE0101010);
        context.fill(x, y, right, y + TITLE_HEIGHT, hovered ? 0xFF303846 : 0xFF252B35);
        context.fill(x, y + TITLE_HEIGHT, right, y + TITLE_HEIGHT + 1, 0xFF5B6D83);
        context.fill(x, bottom - 1, right, bottom, 0xFF5B6D83);
        context.fill(x, y, x + 1, bottom, 0xFF5B6D83);
        context.fill(right - 1, y, right, bottom, 0xFF5B6D83);

        String clippedTitle = tr.trimToWidth(title, width - 42);
        context.drawTextWithShadow(tr, clippedTitle, x + PADDING, y + 5, 0xFFFFFFFF);

        int closeX = right - 16;
        int closeY = y + 3;
        boolean closeHovered = isInBounds(mouseX, mouseY, closeX, closeY, 11, 11);
        context.fill(closeX, closeY, closeX + 11, closeY + 11, closeHovered ? 0xFF883333 : 0xFF4A2A2A);
        context.drawTextWithShadow(tr, "x", closeX + 3, closeY + 1, 0xFFFFFFFF);

        int contentX = x + PADDING;
        int contentY = y + TITLE_HEIGHT + PADDING;
        int contentW = width - PADDING * 2 - SCROLLBAR_WIDTH;
        int contentH = height - TITLE_HEIGHT - PADDING * 2;
        int maxScroll = getMaxScroll(contentH);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        context.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        int lineY = contentY - scroll;
        for (int lineIndex = 0; lineIndex < wrappedLines.size(); lineIndex++) {
            RenderedLine line = wrappedLines.get(lineIndex);
            if (lineY > contentY + contentH) break;
            if (lineY + LINE_HEIGHT >= contentY) {
                drawSelection(context, tr, line, lineIndex, lineY, contentX);
                int color = line.componentHeader ? 0xFF8FC5FF : line.text.startsWith("  ") ? 0xFFB8C7D9 : 0xFFE7EDF5;
                context.drawTextWithShadow(tr, line.text, contentX, lineY, color);
            }
            lineY += LINE_HEIGHT;
        }
        context.disableScissor();

        drawScrollbar(context, contentY, contentH, maxScroll);
        drawResizeGrip(context, right, bottom);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) return false;
        if (!isInBounds(mouseX, mouseY, x, y, width, height)) return false;

        if (isInBounds(mouseX, mouseY, x + width - 16, y + 3, 11, 11)) {
            close();
            return true;
        }

        if (isInBounds(mouseX, mouseY, x + width - RESIZE_GRIP, y + height - RESIZE_GRIP, RESIZE_GRIP, RESIZE_GRIP)) {
            resizing = true;
            dragOffsetX = (int) mouseX - width;
            dragOffsetY = (int) mouseY - height;
            return true;
        }

        if (isInBounds(mouseX, mouseY, x, y, width, TITLE_HEIGHT)) {
            dragging = true;
            dragOffsetX = (int) mouseX - x;
            dragOffsetY = (int) mouseY - y;
            return true;
        }

        int contentY = y + TITLE_HEIGHT + PADDING;
        int contentH = height - TITLE_HEIGHT - PADDING * 2;
        int contentW = width - PADDING * 2 - SCROLLBAR_WIDTH;
        if (isInBounds(mouseX, mouseY, x + PADDING, contentY, contentW, contentH)) {
            int lineIndex = ((int) mouseY - contentY + scroll) / LINE_HEIGHT;
            if (lineIndex >= 0 && lineIndex < wrappedLines.size()) {
                ComponentEntry entry = wrappedLines.get(lineIndex).entry;
                if (entry != null) {
                    clearSelection();
                    toggleComponent(entry);
                } else {
                    selectionStart = selectionPosition(mouseX, mouseY);
                    selectionEnd = selectionStart;
                    selecting = selectionStart != null;
                }
            }
        }
        return true;
    }

    public static boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && selecting) {
            selecting = false;
            return true;
        }
        if (button != 0 || (!dragging && !resizing)) return false;
        dragging = false;
        resizing = false;
        saveConfig();
        return true;
    }

    public static boolean mouseDragged(double mouseX, double mouseY) {
        if (!visible) return false;

        if (dragging) {
            x = (int) mouseX - dragOffsetX;
            y = (int) mouseY - dragOffsetY;
            clampToScreen();
            return true;
        }

        if (resizing) {
            width = Math.max(MIN_WIDTH, (int) mouseX - dragOffsetX);
            height = Math.max(MIN_HEIGHT, (int) mouseY - dragOffsetY);
            clampToScreen();
            wrapWidth = -1;
            clearSelection();
            return true;
        }

        if (selecting) {
            SelectionPosition position = selectionPosition(mouseX, mouseY);
            if (position != null) selectionEnd = position;
            return true;
        }

        return false;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!visible) return false;
        int contentY = y + TITLE_HEIGHT + PADDING;
        int contentH = height - TITLE_HEIGHT - PADDING * 2;
        if (!isInBounds(mouseX, mouseY, x, contentY, width, contentH)) return false;
        scroll = MathHelper.clamp(scroll - (int) (verticalAmount * 24), 0, getMaxScroll(contentH));
        return true;
    }

    public static boolean handleKeyPressed(int keyCode, int modifiers) {
        if (!visible || keyCode != GLFW.GLFW_KEY_C || (modifiers & GLFW.GLFW_MOD_CONTROL) == 0 || !hasSelection()) return false;
        MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
        return true;
    }

    private static SelectionPosition selectionPosition(double mouseX, double mouseY) {
        if (wrappedLines.isEmpty()) return null;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        rebuildWrappedLinesIfNeeded(tr);

        int contentX = x + PADDING;
        int contentY = y + TITLE_HEIGHT + PADDING;
        int lineIndex = MathHelper.clamp(((int) mouseY - contentY + scroll) / LINE_HEIGHT, 0, wrappedLines.size() - 1);
        String text = wrappedLines.get(lineIndex).text;
        int availableWidth = Math.max(0, (int) mouseX - contentX);
        int charIndex = 0;
        while (charIndex < text.length() && tr.getWidth(text.substring(0, charIndex + 1)) <= availableWidth) {
            charIndex++;
        }
        return new SelectionPosition(lineIndex, charIndex);
    }

    private static void drawSelection(DrawContext context, TextRenderer tr, RenderedLine line, int lineIndex, int lineY, int contentX) {
        if (!hasSelection()) return;

        SelectionPosition start = earlierSelectionPosition(selectionStart, selectionEnd);
        SelectionPosition end = laterSelectionPosition(selectionStart, selectionEnd);
        if (lineIndex < start.lineIndex || lineIndex > end.lineIndex) return;

        int startIndex = lineIndex == start.lineIndex ? start.charIndex : 0;
        int endIndex = lineIndex == end.lineIndex ? end.charIndex : line.text.length();
        if (startIndex >= endIndex) return;

        int selectionStartX = contentX + tr.getWidth(line.text.substring(0, startIndex));
        int selectionEndX = contentX + tr.getWidth(line.text.substring(0, endIndex));
        context.fill(selectionStartX, lineY, selectionEndX, lineY + LINE_HEIGHT, 0xA06086C5);
    }

    private static boolean hasSelection() {
        return selectionStart != null && selectionEnd != null && compareSelectionPositions(selectionStart, selectionEnd) != 0;
    }

    private static String getSelectedText() {
        SelectionPosition start = earlierSelectionPosition(selectionStart, selectionEnd);
        SelectionPosition end = laterSelectionPosition(selectionStart, selectionEnd);
        StringBuilder copiedText = new StringBuilder();
        for (int lineIndex = start.lineIndex; lineIndex <= end.lineIndex; lineIndex++) {
            String line = wrappedLines.get(lineIndex).text;
            int startIndex = lineIndex == start.lineIndex ? start.charIndex : 0;
            int endIndex = lineIndex == end.lineIndex ? end.charIndex : line.length();
            copiedText.append(line, startIndex, endIndex);
            if (lineIndex < end.lineIndex) copiedText.append('\n');
        }
        return copiedText.toString();
    }

    private static SelectionPosition earlierSelectionPosition(SelectionPosition first, SelectionPosition second) {
        return compareSelectionPositions(first, second) <= 0 ? first : second;
    }

    private static SelectionPosition laterSelectionPosition(SelectionPosition first, SelectionPosition second) {
        return compareSelectionPositions(first, second) >= 0 ? first : second;
    }

    private static int compareSelectionPositions(SelectionPosition first, SelectionPosition second) {
        int lineComparison = Integer.compare(first.lineIndex, second.lineIndex);
        return lineComparison != 0 ? lineComparison : Integer.compare(first.charIndex, second.charIndex);
    }

    private static void clearSelection() {
        selecting = false;
        selectionStart = null;
        selectionEnd = null;
    }

    private static void rebuildComponents(ItemStack stack) {
        itemLines = new ArrayList<>();
        itemLines.add("Item: " + Registries.ITEM.getId(stack.getItem()));
        itemLines.add("Name: " + stack.getName().getString());
        itemLines.add("Count: " + stack.getCount());

        List<ComponentType<?>> types = new ArrayList<>(stack.getComponents().getTypes());
        types.sort(Comparator.comparing(ItemComponentsDebugOverlay::componentTypeName));
        itemLines.add("Components (" + types.size() + "): click a component to inspect it");
        components = new ArrayList<>();
        for (ComponentType<?> type : types) {
            Object value = getComponent(stack, type);
            components.add(new ComponentEntry(type, componentTypeName(type), value == null ? "<null>" : value.getClass().getSimpleName(), stack));
        }
    }

    private static void toggleComponent(ComponentEntry entry) {
        entry.expanded = !entry.expanded;
        if (!entry.expanded || entry.loading || entry.detailLines != null) {
            wrapWidth = -1;
            return;
        }

        entry.loading = true;
        long requestId = ++nextRequestId;
        entry.requestId = requestId;
        long requestSession = sessionId;
        CompletableFuture.supplyAsync(() -> serializeComponent(entry.stackSnapshot, entry.type), DETAIL_SERIALIZER)
                .handle((lines, throwable) -> {
                    if (throwable != null) {
                        lines = List.of("  <failed to serialize: " + throwable.getClass().getSimpleName() + ">");
                    }
                    COMPLETED_RESULTS.add(new ComponentResult(requestSession, entry, requestId, lines));
                    return null;
                });
        wrapWidth = -1;
        clearSelection();
    }

    private static void applyCompletedResults() {
        boolean changed = false;
        ComponentResult result;
        while ((result = COMPLETED_RESULTS.poll()) != null) {
            if (result.sessionId != sessionId || !components.contains(result.entry) || result.entry.requestId != result.requestId) continue;
            result.entry.loading = false;
            result.entry.detailLines = result.lines;
            changed = true;
        }
        if (changed) wrapWidth = -1;
        if (changed) clearSelection();
    }

    private static List<String> serializeComponent(ItemStack stack, ComponentType<?> type) {
        try {
            Object value = getComponent(stack, type);
            if (value == null) return List.of("  null");

            DataResult<JsonElement> result = encodeComponent(type, value);
            JsonElement json = result.result().orElse(null);
            if (json == null) return List.of("  <the component codec could not serialize this value>");
            return formatJson(json);
        } catch (Exception exception) {
            return List.of("  <failed to serialize: " + exception.getClass().getSimpleName() + ">");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static DataResult<JsonElement> encodeComponent(ComponentType<?> type, Object value) {
        return ((ComponentType) type).getCodec().encodeStart(JsonOps.INSTANCE, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getComponent(ItemStack stack, ComponentType<?> type) {
        return stack.get((ComponentType) type);
    }

    private static List<String> formatJson(JsonElement json) {
        String formatted = PRETTY_GSON.toJson(json);
        boolean truncated = formatted.length() > MAX_DETAIL_CHARS;
        if (truncated) formatted = formatted.substring(0, MAX_DETAIL_CHARS);

        String[] jsonLines = formatted.split("\\R", -1);
        List<String> result = new ArrayList<>();
        for (int index = 0; index < jsonLines.length && result.size() < MAX_DETAIL_LINES; index++) {
            addDisplayLine(result, "  " + jsonLines[index]);
        }
        if (truncated || jsonLines.length > MAX_DETAIL_LINES) {
            result.add("  ... truncated to keep the debug window responsive");
        }
        return result;
    }

    private static void addDisplayLine(List<String> lines, String line) {
        if (line.length() <= MAX_RAW_LINE_CHARS) {
            lines.add(line);
            return;
        }

        String indent = line.startsWith("  ") ? "  " : "";
        int start = 0;
        while (start < line.length() && lines.size() < MAX_DETAIL_LINES) {
            int end = Math.min(line.length(), start + MAX_RAW_LINE_CHARS);
            int split = end;
            if (end < line.length()) {
                int space = line.lastIndexOf(' ', end);
                if (space > start + 40) split = space + 1;
            }
            lines.add((start == 0 ? "" : indent) + line.substring(start, split).stripLeading());
            start = split;
        }
    }

    private static void rebuildWrappedLinesIfNeeded(TextRenderer tr) {
        int contentWidth = width - PADDING * 2 - SCROLLBAR_WIDTH;
        if (contentWidth == wrapWidth) return;
        wrapWidth = contentWidth;
        wrappedLines = new ArrayList<>();
        int maxTextWidth = Math.max(20, contentWidth - 2);
        for (String itemLine : itemLines) addWrappedLines(itemLine, null, false, tr, maxTextWidth);
        for (ComponentEntry entry : components) {
            String marker = entry.expanded ? "▼ " : "▶ ";
            String suffix = entry.loading ? " (loading...)" : "";
            addWrappedLines(marker + entry.typeName + ": " + entry.valueType + suffix, entry, true, tr, maxTextWidth);
            if (entry.expanded && entry.detailLines != null) {
                for (String detailLine : entry.detailLines) addWrappedLines(detailLine, null, false, tr, maxTextWidth);
            }
            if (wrappedLines.size() >= MAX_LINES) {
                wrappedLines.add(new RenderedLine("... additional expanded data is not shown", null, false));
                return;
            }
        }
    }

    private static void addWrappedLines(String line, ComponentEntry entry, boolean componentHeader, TextRenderer tr, int maxWidth) {
        for (String part : wrapLine(tr, line, maxWidth)) {
            if (wrappedLines.size() >= MAX_LINES) return;
            wrappedLines.add(new RenderedLine(part, entry, componentHeader));
        }
    }

    private static List<String> wrapLine(TextRenderer tr, String line, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (line.isEmpty()) {
            result.add("");
            return result;
        }

        String remaining = line;
        String indent = line.startsWith("  ") ? "  " : "";
        while (tr.getWidth(remaining) > maxWidth) {
            int split = findSplitIndex(tr, remaining, maxWidth);
            result.add(remaining.substring(0, split));
            remaining = indent + remaining.substring(split).stripLeading();
        }
        result.add(remaining);
        return result;
    }

    private static int findSplitIndex(TextRenderer tr, String text, int maxWidth) {
        int low = 1;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (tr.getWidth(text.substring(0, mid)) <= maxWidth) low = mid;
            else high = mid - 1;
        }

        int space = text.lastIndexOf(' ', low);
        if (space > 0) return space + 1;
        return Math.max(1, low);
    }

    private static int getMaxScroll(int contentH) {
        return Math.max(0, wrappedLines.size() * LINE_HEIGHT - contentH);
    }

    private static Slot findSlotAtLastMouse(HandledScreen<?> screen) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int left = accessor.getX();
        int top = accessor.getY();

        for (Slot slot : screen.getScreenHandler().slots) {
            int slotX = left + slot.x;
            int slotY = top + slot.y;
            if (lastMouseX >= slotX && lastMouseX < slotX + 16 && lastMouseY >= slotY && lastMouseY < slotY + 16) {
                return slot;
            }
        }

        return null;
    }

    private static void loadConfig() {
        if (configLoaded) return;
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        x = config.debugItemComponentsWindowX;
        y = config.debugItemComponentsWindowY;
        width = Math.max(MIN_WIDTH, config.debugItemComponentsWindowW);
        height = Math.max(MIN_HEIGHT, config.debugItemComponentsWindowH);
        configLoaded = true;
    }

    private static void saveConfig() {
        if (!configLoaded) return;
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.debugItemComponentsWindowX = x;
        config.debugItemComponentsWindowY = y;
        config.debugItemComponentsWindowW = width;
        config.debugItemComponentsWindowH = height;
        WynnExtrasConfig.save();
    }

    private static String componentTypeName(ComponentType<?> type) {
        return String.valueOf(Registries.DATA_COMPONENT_TYPE.getId(type));
    }

    private static void drawScrollbar(DrawContext context, int contentY, int contentH, int maxScroll) {
        int sbX = x + width - PADDING - SCROLLBAR_WIDTH;
        context.fill(sbX, contentY, sbX + SCROLLBAR_WIDTH, contentY + contentH, 0xFF1B2028);
        if (maxScroll <= 0) return;

        int thumbH = Math.max(18, contentH * contentH / (contentH + maxScroll));
        int travel = contentH - thumbH;
        int thumbY = contentY + (travel <= 0 ? 0 : (int) (travel * (scroll / (double) maxScroll)));
        context.fill(sbX + 1, thumbY, sbX + SCROLLBAR_WIDTH - 1, thumbY + thumbH, 0xFF6D829A);
    }

    private static void drawResizeGrip(DrawContext context, int right, int bottom) {
        context.fill(right - 10, bottom - 3, right - 2, bottom - 2, 0xFF9AAFC7);
        context.fill(right - 7, bottom - 6, right - 2, bottom - 5, 0xFF9AAFC7);
        context.fill(right - 4, bottom - 9, right - 2, bottom - 8, 0xFF9AAFC7);
    }

    private static void clampToScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        width = Math.min(Math.max(width, MIN_WIDTH), Math.max(MIN_WIDTH, screenW));
        height = Math.min(Math.max(height, MIN_HEIGHT), Math.max(MIN_HEIGHT, screenH));
        x = MathHelper.clamp(x, 0, Math.max(0, screenW - width));
        y = MathHelper.clamp(y, 0, Math.max(0, screenH - height));
    }

    private static boolean isInBounds(double mouseX, double mouseY, int bx, int by, int bw, int bh) {
        return mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
    }

    private static class ComponentEntry {
        private final ComponentType<?> type;
        private final String typeName;
        private final String valueType;
        private final ItemStack stackSnapshot;
        private boolean expanded;
        private boolean loading;
        private long requestId;
        private List<String> detailLines;

        private ComponentEntry(ComponentType<?> type, String typeName, String valueType, ItemStack stackSnapshot) {
            this.type = type;
            this.typeName = typeName;
            this.valueType = valueType;
            this.stackSnapshot = stackSnapshot;
        }
    }

    private static class SelectionPosition {
        private final int lineIndex;
        private final int charIndex;

        private SelectionPosition(int lineIndex, int charIndex) {
            this.lineIndex = lineIndex;
            this.charIndex = charIndex;
        }
    }

    private static class RenderedLine {
        private final String text;
        private final ComponentEntry entry;
        private final boolean componentHeader;

        private RenderedLine(String text, ComponentEntry entry, boolean componentHeader) {
            this.text = text;
            this.entry = entry;
            this.componentHeader = componentHeader;
        }
    }

    private static class ComponentResult {
        private final long sessionId;
        private final ComponentEntry entry;
        private final long requestId;
        private final List<String> lines;

        private ComponentResult(long sessionId, ComponentEntry entry, long requestId, List<String> lines) {
            this.sessionId = sessionId;
            this.entry = entry;
            this.requestId = requestId;
            this.lines = lines;
        }
    }
}
