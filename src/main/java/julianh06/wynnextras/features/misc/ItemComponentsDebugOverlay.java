package julianh06.wynnextras.features.misc;

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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private static final int MAX_LORE_LINES = 80;

    private static boolean visible = false;
    private static String title = "Item Components";
    private static List<String> rawLines = new ArrayList<>();
    private static List<String> wrappedLines = new ArrayList<>();
    private static int wrapWidth = -1;
    private static int x;
    private static int y;
    private static int width;
    private static int height;
    private static int scroll;
    private static boolean configLoaded = false;
    private static boolean dragging = false;
    private static boolean resizing = false;
    private static int dragOffsetX;
    private static int dragOffsetY;
    private static int lastMouseX;
    private static int lastMouseY;

    public static void registerInventoryScreenHooks() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) return;

            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, tickDelta) ->
                    render(context, mouseX, mouseY));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, input) -> {
                if (input.key() != WynnExtrasConfig.INSTANCE.debugItemComponentsKey) return true;
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
        scroll = 0;
        title = "Item Components: " + newStack.getName().getString();
        rebuildRawLines(newStack);
        wrapWidth = -1;
        visible = true;
        clampToScreen();
    }

    public static void close() {
        if (!visible) return;
        visible = false;
        dragging = false;
        resizing = false;
        saveConfig();
    }

    public static void reset() {
        visible = false;
        dragging = false;
        resizing = false;
        title = "Item Components";
        rawLines = new ArrayList<>();
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
        for (String line : wrappedLines) {
            if (lineY > contentY + contentH) break;
            if (lineY + LINE_HEIGHT >= contentY) {
                int color = line.startsWith("  ") ? 0xFFB8C7D9 : 0xFFE7EDF5;
                context.drawTextWithShadow(tr, line, contentX, lineY, color);
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

        return true;
    }

    public static boolean mouseReleased(double mouseX, double mouseY, int button) {
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

    private static void rebuildRawLines(ItemStack sourceStack) {
        rawLines = new ArrayList<>();
        rawLines.add("Item: " + Registries.ITEM.getId(sourceStack.getItem()));
        rawLines.add("Name: " + sourceStack.getName().getString());
        rawLines.add("Count: " + sourceStack.getCount());
        rawLines.add("Components (" + sourceStack.getComponents().size() + "):");

        List<ComponentType<?>> types = new ArrayList<>(sourceStack.getComponents().getTypes());
        types.sort(Comparator.comparing(ItemComponentsDebugOverlay::componentTypeName));
        for (ComponentType<?> type : types) {
            if (rawLines.size() >= MAX_LINES) break;
            addComponentLines(sourceStack, type);
        }
        if (rawLines.size() >= MAX_LINES) {
            rawLines.add("... truncated to keep the debug window responsive");
        }
    }

    private static <T> void addComponentLines(ItemStack sourceStack, ComponentType<T> type) {
        String typeName = componentTypeName(type);
        T value = sourceStack.get(type);
        if (value == null) {
            addRawLine(typeName + ": <null>");
            return;
        }

        addRawLine(typeName + ": " + value.getClass().getSimpleName());

        try {
            if (type == DataComponentTypes.CUSTOM_NAME && value instanceof Text text) {
                addRawLine("  value: " + text.getString());
            } else if (type == DataComponentTypes.ITEM_NAME && value instanceof Text text) {
                addRawLine("  value: " + text.getString());
            } else if (type == DataComponentTypes.LORE && value instanceof LoreComponent lore) {
                List<Text> lines = lore.lines();
                addRawLine("  lines: " + lines.size());
                for (int i = 0; i < Math.min(lines.size(), MAX_LORE_LINES); i++) {
                    addRawLine("  [" + i + "] " + lines.get(i).getString());
                }
                if (lines.size() > MAX_LORE_LINES) {
                    addRawLine("  ... " + (lines.size() - MAX_LORE_LINES) + " more lore lines");
                }
            } else if (type == DataComponentTypes.CUSTOM_MODEL_DATA && value instanceof CustomModelDataComponent customModelData) {
                addRawLine("  first float: " + customModelData.getFloat(0));
            } else if (value instanceof Boolean || value instanceof Number || value instanceof String) {
                addRawLine("  value: " + value);
            } else {
                addRawLine("  value omitted to avoid freezing on large component data");
            }
        } catch (Exception e) {
            addRawLine("  failed to read value: " + e.getClass().getSimpleName());
        }
    }

    private static String componentTypeName(ComponentType<?> type) {
        return String.valueOf(Registries.DATA_COMPONENT_TYPE.getId(type));
    }

    private static void addRawLine(String line) {
        if (line.length() <= MAX_RAW_LINE_CHARS) {
            rawLines.add(line);
            return;
        }

        String indent = line.startsWith("  ") ? "  " : "";
        int start = 0;
        while (start < line.length() && rawLines.size() < MAX_LINES) {
            int end = Math.min(line.length(), start + MAX_RAW_LINE_CHARS);
            int split = end;
            if (end < line.length()) {
                int space = line.lastIndexOf(' ', end);
                if (space > start + 40) split = space + 1;
            }
            rawLines.add((start == 0 ? "" : indent) + line.substring(start, split).stripLeading());
            start = split;
        }
    }

    private static void rebuildWrappedLinesIfNeeded(TextRenderer tr) {
        int contentWidth = width - PADDING * 2 - SCROLLBAR_WIDTH;
        if (contentWidth == wrapWidth) return;
        wrapWidth = contentWidth;
        wrappedLines = new ArrayList<>();
        int maxTextWidth = Math.max(20, contentWidth - 2);
        for (String rawLine : rawLines) {
            wrappedLines.addAll(wrapLine(tr, rawLine, maxTextWidth));
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
}
