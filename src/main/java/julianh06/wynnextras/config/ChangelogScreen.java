package julianh06.wynnextras.config;

import com.wynntils.utils.mc.McUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * /we changelog screen.
 *
 * To add an entry for a new update, edit buildEntries() below. Use:
 *   addInfo("text")                               - read-only info line
 *   addToggle("text", getter, setter, default)    - ON/OFF toggle
 *   addKeybind("text", getter, setter, default)   - key rebind button
 *   addToggleKeybind(...)                         - both toggle AND keybind
 *
 * Reset / All ON / All OFF buttons apply to all toggles & keybinds on the screen.
 */
public class ChangelogScreen extends Screen {

    private void buildEntries() {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

        addToggle("Raid Session Tracker (runs/hr, avg time, fails)", () -> c.raidSessionEnabled, v -> c.raidSessionEnabled = v, false);
        addToggle("Raid Session: only show in raid", () -> c.raidSessionOnlyInRaid, v -> c.raidSessionOnlyInRaid = v, false);
        addToggle("Raid Session: only show in inventory", () -> c.raidSessionOnlyInInventory, v -> c.raidSessionOnlyInInventory = v, false);
        addToggleKeybind("Quick Repair at Blacksmith",
                () -> c.quickRepairEnabled, v -> c.quickRepairEnabled = v, true,
                () -> c.quickRepairKey, v -> c.quickRepairKey = v, GLFW.GLFW_KEY_R);
        addToggle("Bomb share suggestions in chat", () -> c.bombShareSuggestion, v -> c.bombShareSuggestion = v, true);
        addToggle("Block Guild Raid clicks (Shift to allow)", () -> c.shiftDisableGuildRaid, v -> c.shiftDisableGuildRaid = v, true);
        addToggle("Auto /stream on world swap", () -> c.autoStreamEnabled, v -> c.autoStreamEnabled = v, false);
        addToggle("Auto skip dialogue", () -> c.autoSkipDialogueEnabled, v -> c.autoSkipDialogueEnabled = v, false);
        addToggle("Stack duplicate chat messages", () -> c.stackDuplicateMessages, v -> c.stackDuplicateMessages = v, false);
        addToggle("Aura ping HUD overlay", () -> c.auraPingEnabled, v -> c.auraPingEnabled = v, false);
        addToggle("Weekly war count HUD", () -> c.weeklyWarCountEnabled, v -> c.weeklyWarCountEnabled = v, false);
        addToggle("War DPS / Tower HP info", () -> c.warDpsEnabled, v -> c.warDpsEnabled = v, false);
        addToggle("Attack Timer Menu", () -> c.attackTimerMenuEnabled, v -> c.attackTimerMenuEnabled = v, false);
        addToggle("War beacon at soonest territory", () -> c.warBeaconEnabled, v -> c.warBeaconEnabled = v, false);
        addInfo("Config Profiles (save/switch named toggle presets)");
        addInfo("Disable/Enable WynnExtras toggle (preserves settings)");
        addInfo("Bank search: slot: filter (e.g. slot:necklace, slot:chestplate)");
        addInfo("Bank search: id: filter (e.g. id:walkspeed>10, id:strength)");
        addInfo("Bank search: identified: filter (true/false)");
        addInfo("/we hide, /we hide war, /we hide all commands");
        addInfo("Bank bag overlay: fixed flickering on page change");
        addInfo("Totem timer: fixed estimate clearing on hotbar swap");
        addInfo("Totem timer: works on all classes now");
        addInfo("Blood sorrow: triggers on sound + blood pool change");
        addInfo("Bombshare: filter-specific 'no active X bombs' messages");
        addInfo("Bombshare: 'Share all Bombs' rename, shorter prefix");
        addInfo("TWP completions in profile viewer (unknown API key fallback)");
        addInfo("Performance: throttled entity scans, cached lookups");
        addInfo("Bank: offhand swap (F key) in custom overlay");
        addInfo("Bank: slower page reload for reliability");
    }

    // ==================== theme ====================
    private static final int BG_DARK = 0xFF1a1410;
    private static final int BG_MEDIUM = 0xFF2e251c;
    private static final int BG_LIGHT = 0xFF4d3c2d;
    private static final int PARCHMENT = 0xFF6c4f36;
    private static final int PARCHMENT_HOVER = 0xFF705030;
    private static final int GOLD = 0xFFcca76f;
    private static final int GOLD_DARK = 0xFFecc600;
    private static final int TEXT_LIGHT = 0xFFe8dcc8;
    private static final int TEXT_DIM = 0xFF9a8b70;
    private static final int BORDER_DARK = 0xFF3a2d24;
    private static final int TOGGLE_ON = 0xFF4a8c3a;
    private static final int TOGGLE_OFF = 0xFF5c4535;
    private static final int ACCENT_RED = 0xFFa83232;
    private static final int LISTENING_COLOR = 0xFFFFDD44;

    private static final int ITEMS_PER_PAGE = 10;
    private static final int ROW_H = 28;
    private static final int TOGGLE_W = 36;
    private static final int KEY_W = 60;

    private static class Entry {
        final String description;
        final Supplier<Boolean> boolGet;
        final Consumer<Boolean> boolSet;
        final Boolean boolDefault;
        final Supplier<Integer> keyGet;
        final Consumer<Integer> keySet;
        final Integer keyDefault;
        boolean listening = false;

        Entry(String d, Supplier<Boolean> bg, Consumer<Boolean> bs, Boolean bd,
              Supplier<Integer> kg, Consumer<Integer> ks, Integer kd) {
            description = d; boolGet = bg; boolSet = bs; boolDefault = bd;
            keyGet = kg; keySet = ks; keyDefault = kd;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private int page = 0;

    public ChangelogScreen() {
        super(Text.literal("Changelog"));
        buildEntries();
    }

    // ==================== entry helpers ====================
    private void addInfo(String desc) { entries.add(new Entry(desc, null, null, null, null, null, null)); }
    private void addToggle(String desc, Supplier<Boolean> get, Consumer<Boolean> set, boolean def) {
        entries.add(new Entry(desc, get, set, def, null, null, null));
    }
    private void addKeybind(String desc, Supplier<Integer> get, Consumer<Integer> set, int def) {
        entries.add(new Entry(desc, null, null, null, get, set, def));
    }
    private void addToggleKeybind(String desc, Supplier<Boolean> bget, Consumer<Boolean> bset, boolean bdef,
                                   Supplier<Integer> kget, Consumer<Integer> kset, int kdef) {
        entries.add(new Entry(desc, bget, bset, bdef, kget, kset, kdef));
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            default -> "K_" + key;
        };
    }

    private int totalPages() { return Math.max(1, (entries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE); }

    private List<Entry> pageEntries() {
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, entries.size());
        return entries.subList(start, end);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int pw = 520, ph = ITEMS_PER_PAGE * ROW_H + 110;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;

        ctx.fill(0, 0, width, height, BG_DARK);
        ctx.fill(px, py, px + pw, py + ph, BG_MEDIUM);
        ctx.fill(px + 2, py + 2, px + pw - 2, py + ph - 2, BG_LIGHT);

        ctx.drawCenteredTextWithShadow(textRenderer, "Changelog", width / 2, py + 12, GOLD);
        ctx.drawCenteredTextWithShadow(textRenderer, "Page " + (page + 1) + "/" + totalPages(), width / 2, py + 26, TEXT_DIM);
        ctx.fill(px + 20, py + 40, px + pw - 20, py + 41, GOLD_DARK);

        List<Entry> items = pageEntries();
        int listY = py + 48;
        for (int i = 0; i < items.size(); i++) {
            Entry e = items.get(i);
            int ry = listY + i * ROW_H;
            boolean hover = mx >= px + 10 && mx < px + pw - 10 && my >= ry && my < ry + ROW_H - 2;

            ctx.fill(px + 10, ry, px + pw - 10, ry + ROW_H - 2, hover ? PARCHMENT_HOVER : PARCHMENT);

            int controlRight = px + pw - 16;

            if (e.boolGet != null) {
                int tx = controlRight - TOGGLE_W;
                int ty = ry + 5;
                boolean on = e.boolGet.get();
                ctx.fill(tx, ty, tx + TOGGLE_W, ty + 16, BORDER_DARK);
                ctx.fill(tx + 1, ty + 1, tx + TOGGLE_W - 1, ty + 15, on ? TOGGLE_ON : TOGGLE_OFF);
                ctx.drawCenteredTextWithShadow(textRenderer, on ? "ON" : "OFF", tx + TOGGLE_W / 2, ty + 4, TEXT_LIGHT);
                controlRight = tx - 6;
            }

            if (e.keyGet != null) {
                int kx = controlRight - KEY_W;
                int ky = ry + 5;
                ctx.fill(kx, ky, kx + KEY_W, ky + 16, BORDER_DARK);
                ctx.fill(kx + 1, ky + 1, kx + KEY_W - 1, ky + 15, e.listening ? PARCHMENT_HOVER : PARCHMENT);
                String label = e.listening ? "..." : "[" + keyName(e.keyGet.get()) + "]";
                ctx.drawCenteredTextWithShadow(textRenderer, label, kx + KEY_W / 2, ky + 4,
                        e.listening ? LISTENING_COLOR : TEXT_LIGHT);
            }

            int descColor = (e.boolGet == null && e.keyGet == null) ? TEXT_DIM : TEXT_LIGHT;
            ctx.drawTextWithShadow(textRenderer, e.description, px + 16, ry + 9, descColor);
        }

        // Bottom buttons
        int btnY = py + ph - 38;
        int btnW = 58, btnH = 20, gap = 6;
        String[] labels = {"All ON", "All OFF", "Reset", "Prev", "Next", "Close"};
        int totalBtnW = btnW * labels.length + gap * (labels.length - 1);
        int bx = width / 2 - totalBtnW / 2;

        for (int i = 0; i < labels.length; i++) {
            int x = bx + i * (btnW + gap);
            boolean bh = mx >= x && mx < x + btnW && my >= btnY && my < btnY + btnH;
            int fillColor = PARCHMENT;
            if (bh) fillColor = i == 2 ? ACCENT_RED : PARCHMENT_HOVER;
            ctx.fill(x, btnY, x + btnW, btnY + btnH, BORDER_DARK);
            ctx.fill(x + 1, btnY + 1, x + btnW - 1, btnY + btnH - 1, fillColor);
            ctx.drawCenteredTextWithShadow(textRenderer, labels[i], x + btnW / 2, btnY + 6, TEXT_LIGHT);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x(), my = click.y();
        int pw = 520, ph = ITEMS_PER_PAGE * ROW_H + 110;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;

        // Cancel any active listen on outside click
        boolean wasListening = false;
        for (Entry e : entries) if (e.listening) { wasListening = true; break; }

        List<Entry> items = pageEntries();
        int listY = py + 48;
        for (int i = 0; i < items.size(); i++) {
            Entry e = items.get(i);
            int ry = listY + i * ROW_H;
            int controlRight = px + pw - 16;

            if (e.boolGet != null) {
                int tx = controlRight - TOGGLE_W;
                int ty = ry + 5;
                if (mx >= tx && mx < tx + TOGGLE_W && my >= ty && my < ty + 16) {
                    e.boolSet.accept(!e.boolGet.get());
                    WynnExtrasConfig.save();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
                controlRight = tx - 6;
            }

            if (e.keyGet != null) {
                int kx = controlRight - KEY_W;
                int ky = ry + 5;
                if (mx >= kx && mx < kx + KEY_W && my >= ky && my < ky + 16) {
                    for (Entry other : entries) if (other != e) other.listening = false;
                    e.listening = !e.listening;
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    return true;
                }
            }
        }

        if (wasListening) {
            for (Entry e : entries) e.listening = false;
        }

        int btnY = py + ph - 38;
        int btnW = 58, btnH = 20, gap = 6;
        int btnCount = 6;
        int totalBtnW = btnW * btnCount + gap * (btnCount - 1);
        int bx = width / 2 - totalBtnW / 2;

        if (my >= btnY && my < btnY + btnH) {
            for (int i = 0; i < btnCount; i++) {
                int x = bx + i * (btnW + gap);
                if (mx >= x && mx < x + btnW) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    switch (i) {
                        case 0 -> { for (Entry e : entries) if (e.boolSet != null) e.boolSet.accept(true); WynnExtrasConfig.save(); }
                        case 1 -> { for (Entry e : entries) if (e.boolSet != null) e.boolSet.accept(false); WynnExtrasConfig.save(); }
                        case 2 -> {
                            for (Entry e : entries) {
                                if (e.boolSet != null && e.boolDefault != null) e.boolSet.accept(e.boolDefault);
                                if (e.keySet != null && e.keyDefault != null) e.keySet.accept(e.keyDefault);
                            }
                            WynnExtrasConfig.save();
                        }
                        case 3 -> page = Math.max(0, page - 1);
                        case 4 -> page = Math.min(totalPages() - 1, page + 1);
                        case 5 -> close();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (v > 0) page = Math.max(0, page - 1);
        else if (v < 0) page = Math.min(totalPages() - 1, page + 1);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        // If any entry is listening for a key, capture it
        for (Entry e : entries) {
            if (e.listening) {
                if (key != GLFW.GLFW_KEY_ESCAPE) {
                    e.keySet.accept(key);
                    WynnExtrasConfig.save();
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                }
                e.listening = false;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public void close() { client.setScreen(null); }
}
