//package julianh06.wynnextras.config;
//
//import com.wynntils.utils.mc.McUtils;
//import net.minecraft.client.gui.Click;
//import net.minecraft.client.gui.DrawContext;
//import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.client.input.CharInput;
//import net.minecraft.client.input.KeyInput;
//import net.minecraft.sound.SoundEvents;
//import net.minecraft.text.Text;
//import net.minecraft.util.math.MathHelper;
//import org.lwjgl.glfw.GLFW;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static julianh06.wynnextras.config.ConfigTheme.*;
//
//public class ProfilesScreen extends Screen {
//    final Screen parent;
//    String nameInput = "";
//    double scroll = 0;
//
//    public ProfilesScreen(Screen parent) {
//        super(Text.literal("Config Profiles"));
//        this.parent = parent;
//    }
//
//    @Override
//    public void render(DrawContext ctx, int mx, int my, float delta) {
//        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
//        ctx.fill(0, 0, width, height, BG_DARK);
//
//        int px = width / 2 - 200, pw = 400;
//        ctx.fill(px, 20, px + pw, height - 20, BG_MEDIUM);
//        ctx.fill(px + 2, 22, px + pw - 2, height - 22, BG_LIGHT);
//
//        ctx.drawCenteredTextWithShadow(textRenderer, "Config Profiles", width / 2, 35, GOLD);
//        String activeStr = config.activeProfile != null ? "Active: " + config.activeProfile : "No active profile";
//        ctx.drawCenteredTextWithShadow(textRenderer, activeStr, width / 2, 50, TEXT_DIM);
//        ctx.fill(px + 20, 65, px + pw - 20, 66, GOLD_DARK);
//
//        int inputY = 80;
//        int inputW = pw - 90;
//        ctx.fill(px + 15, inputY, px + 15 + inputW, inputY + 24, BORDER_DARK);
//        ctx.fill(px + 16, inputY + 1, px + 14 + inputW, inputY + 23, PARCHMENT_LIGHT);
//        String shown = nameInput.length() > 40 ? nameInput.substring(0, 38) + ".." : nameInput;
//        ctx.drawTextWithShadow(textRenderer, shown + "_", px + 20, inputY + 8, TEXT_LIGHT);
//
//        boolean saveH = mx >= px + pw - 65 && mx < px + pw - 15 && my >= inputY && my < inputY + 24;
//        ctx.fill(px + pw - 65, inputY, px + pw - 15, inputY + 24, BORDER_DARK);
//        ctx.fill(px + pw - 64, inputY + 1, px + pw - 16, inputY + 23, saveH ? TOGGLE_ON : PARCHMENT);
//        ctx.drawCenteredTextWithShadow(textRenderer, "Save", px + pw - 40, inputY + 8, TEXT_LIGHT);
//
//        int listTop = inputY + 35;
//        ctx.enableScissor(px + 10, listTop, px + pw - 10, height - 70);
//        int y = listTop - (int) scroll;
//        List<String> names = new ArrayList<>(config.configProfiles.keySet());
//        for (String name : names) {
//            if (y + 24 > listTop && y < height - 70) {
//                boolean isActive = name.equals(config.activeProfile);
//                ctx.fill(px + 15, y, px + pw - 130, y + 24, isActive ? PARCHMENT_LIGHT : PARCHMENT);
//                String t = name.length() > 30 ? name.substring(0, 28) + ".." : name;
//                ctx.drawTextWithShadow(textRenderer, t, px + 20, y + 8, isActive ? GOLD : TEXT_LIGHT);
//
//                boolean applyH = mx >= px + pw - 125 && mx < px + pw - 75 && my >= y && my < y + 24;
//                ctx.fill(px + pw - 125, y, px + pw - 75, y + 24, BORDER_DARK);
//                ctx.fill(px + pw - 124, y + 1, px + pw - 76, y + 23, applyH ? TOGGLE_ON : PARCHMENT);
//                ctx.drawCenteredTextWithShadow(textRenderer, "Apply", px + pw - 100, y + 8, TEXT_LIGHT);
//
//                boolean overH = mx >= px + pw - 70 && mx < px + pw - 45 && my >= y && my < y + 24;
//                ctx.fill(px + pw - 70, y, px + pw - 45, y + 24, BORDER_DARK);
//                ctx.fill(px + pw - 69, y + 1, px + pw - 46, y + 23, overH ? PARCHMENT_HOVER : PARCHMENT);
//                ctx.drawCenteredTextWithShadow(textRenderer, "Save", px + pw - 57, y + 8, TEXT_LIGHT);
//
//                boolean delH = mx >= px + pw - 40 && mx < px + pw - 15 && my >= y && my < y + 24;
//                ctx.fill(px + pw - 40, y, px + pw - 15, y + 24, BORDER_DARK);
//                ctx.fill(px + pw - 39, y + 1, px + pw - 16, y + 23, delH ? ACCENT_RED : PARCHMENT);
//                ctx.drawCenteredTextWithShadow(textRenderer, "X", px + pw - 27, y + 8, TEXT_LIGHT);
//            }
//            y += 28;
//        }
//        ctx.disableScissor();
//
//        if (names.isEmpty()) ctx.drawCenteredTextWithShadow(textRenderer, "No profiles yet — type a name above and click Save", width / 2, height / 2, TEXT_DIM);
//
//        int by = height - 55;
//        boolean doneH = mx >= width / 2 - 50 && mx < width / 2 + 50 && my >= by && my < by + 24;
//        ctx.fill(width / 2 - 50, by, width / 2 + 50, by + 24, BORDER_DARK);
//        ctx.fill(width / 2 - 49, by + 1, width / 2 + 49, by + 23, doneH ? TOGGLE_ON : PARCHMENT);
//        ctx.drawCenteredTextWithShadow(textRenderer, "Done", width / 2, by + 8, TEXT_LIGHT);
//    }
//
//    @Override
//    public boolean mouseClicked(Click click, boolean doubleClick) {
//        double mx = click.x();
//        double my = click.y();
//        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
//
//        int px = width / 2 - 200, pw = 400;
//        int inputY = 80;
//
//        if (mx >= px + pw - 65 && mx < px + pw - 15 && my >= inputY && my < inputY + 24) {
//            if (!nameInput.isBlank()) {
//                config.saveCurrentAsProfile(nameInput.trim());
//                nameInput = "";
//                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//            }
//            return true;
//        }
//
//        int by = height - 55;
//        if (mx >= width / 2 - 50 && mx < width / 2 + 50 && my >= by && my < by + 24) {
//            client.setScreen(parent);
//            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//            return true;
//        }
//
//        int listTop = inputY + 35;
//        int y = listTop - (int) scroll;
//        List<String> names = new ArrayList<>(config.configProfiles.keySet());
//        for (String name : names) {
//            if (my >= y && my < y + 24) {
//                if (mx >= px + pw - 125 && mx < px + pw - 75) {
//                    config.applyProfile(name);
//                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//                    return true;
//                }
//                if (mx >= px + pw - 70 && mx < px + pw - 45) {
//                    config.saveCurrentAsProfile(name);
//                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//                    return true;
//                }
//                if (mx >= px + pw - 40 && mx < px + pw - 15) {
//                    config.deleteProfile(name);
//                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
//                    return true;
//                }
//            }
//            y += 28;
//        }
//        return super.mouseClicked(click, doubleClick);
//    }
//
//    @Override
//    public boolean keyPressed(KeyInput input) {
//        int key = input.key();
//        boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
//
//        if (ctrl && key == GLFW.GLFW_KEY_V) {
//            String clip = client.keyboard.getClipboard();
//            if (clip != null && !clip.isEmpty()) nameInput += clip.replaceAll("[\\r\\n\\t]", "");
//            return true;
//        } else if (ctrl && key == GLFW.GLFW_KEY_C) {
//            client.keyboard.setClipboard(nameInput);
//            return true;
//        } else if (ctrl && key == GLFW.GLFW_KEY_X) {
//            client.keyboard.setClipboard(nameInput);
//            nameInput = "";
//            return true;
//        }
//
//        if (key == 259 && !nameInput.isEmpty()) {
//            nameInput = nameInput.substring(0, nameInput.length() - 1);
//            return true;
//        }
//        if (key == 257) {
//            if (!nameInput.isBlank()) {
//                WynnExtrasConfig.INSTANCE.saveCurrentAsProfile(nameInput.trim());
//                nameInput = "";
//            }
//            return true;
//        }
//        if (key == 256) {
//            client.setScreen(parent);
//            return true;
//        }
//        return super.keyPressed(input);
//    }
//
//    @Override
//    public boolean charTyped(CharInput charInput) {
//        long window = client.getWindow().getHandle();
//        boolean ctrlHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
//                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
//        if (ctrlHeld) return true;
//        int c = charInput.codepoint();
//        if (c >= 32) {
//            nameInput += (char) c;
//            return true;
//        }
//        return super.charTyped(charInput);
//    }
//
//    @Override
//    public boolean mouseScrolled(double mx, double my, double h, double v) {
//        int max = Math.max(0, WynnExtrasConfig.INSTANCE.configProfiles.size() * 28 - (height - 180));
//        scroll = MathHelper.clamp(scroll - v * 25, 0, max);
//        return true;
//    }
//
//    @Override
//    public void close() { client.setScreen(parent); }
//}
