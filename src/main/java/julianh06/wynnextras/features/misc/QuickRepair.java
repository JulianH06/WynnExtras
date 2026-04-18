package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.lwjgl.glfw.GLFW;

public class QuickRepair {

    private static final String BLACKSMITH_TITLE = "\uDAFF\uDFF8\uE016";
    private static final String REPAIR_TITLE = "\uDAFF\uDFF8\uE017";
    private static final int SLOT_REPAIR_ITEMS = 18;
    private static final int SLOT_ITEM = 11;
    private static final int EMPTY_CLOSE_THRESHOLD = 6;

    private static boolean repairing = false;
    private static int spamCooldown = 0;
    private static int emptySlotTicks = 0;
    private static boolean keyWasDown = false;

    public static void startRepair() {
        repairing = true;
        spamCooldown = 0;
        emptySlotTicks = 0;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return;
            if (client.player == null) return;

            String title = getCurrentTitle(client);

            if (title == null || (!title.equals(BLACKSMITH_TITLE) && !title.equals(REPAIR_TITLE))) {
                repairing = false;
                spamCooldown = 0;
                emptySlotTicks = 0;
                keyWasDown = false;
                return;
            }

            // Check keybind (manual GLFW poll, not Fabric KeyBinding)
            long window = client.getWindow().getHandle();
            int key = WynnExtrasConfig.INSTANCE.quickRepairKey;
            boolean keyDown = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (keyDown && !keyWasDown && title.equals(BLACKSMITH_TITLE)) {
                startRepair();
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRepairing..."));
            }
            keyWasDown = keyDown;

            if (!repairing) return;

            if (spamCooldown > 0) { spamCooldown--; return; }

            ScreenHandler menu = McUtils.containerMenu();
            if (menu == null) return;

            if (title.equals(BLACKSMITH_TITLE)) {
                if (menu.slots.size() > SLOT_REPAIR_ITEMS) {
                    ItemStack stack = menu.getSlot(SLOT_REPAIR_ITEMS).getStack();
                    if (!stack.isEmpty()) {
                        ContainerUtils.clickOnSlot(SLOT_REPAIR_ITEMS, menu.syncId, 0, menu.getStacks());
                        spamCooldown = 4;
                    }
                }
            } else if (title.equals(REPAIR_TITLE)) {
                if (menu.slots.size() > SLOT_ITEM) {
                    ItemStack stack = menu.getSlot(SLOT_ITEM).getStack();
                    if (!stack.isEmpty()) {
                        String itemName = stack.getName().getString().replaceAll("§[0-9a-fk-or]", "").toLowerCase();
                        if (itemName.contains("empty")) {
                            emptySlotTicks++;
                            if (emptySlotTicks >= EMPTY_CLOSE_THRESHOLD) {
                                repairing = false;
                                emptySlotTicks = 0;
                                client.execute(() -> client.player.closeHandledScreen());
                                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aAll items repaired!"));
                            }
                            return;
                        }
                        emptySlotTicks = 0;
                        ContainerUtils.clickOnSlot(SLOT_ITEM, menu.syncId, 0, menu.getStacks());
                        spamCooldown = 2;
                    }
                }
            }
        });
    }

    private static String getCurrentTitle(MinecraftClient mc) {
        if (mc.currentScreen == null) return null;
        return mc.currentScreen.getTitle().getString();
    }

    private static final int BTN_W = 70, BTN_H = 16;

    public static void renderButton(DrawContext ctx, HandledScreen<?> screen, int mouseX, int mouseY) {
        if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return;
        String title = screen.getTitle().getString();
        if (!title.equals(BLACKSMITH_TITLE)) return;

        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int bx = acc.getX() + acc.getBackgroundWidth() + 4;
        int by = acc.getY() + 4;

        MinecraftClient mc = MinecraftClient.getInstance();
        String keyName = GLFW.glfwGetKeyName(WynnExtrasConfig.INSTANCE.quickRepairKey, 0);
        if (keyName == null) keyName = "?";
        String label = "Repair [" + keyName.toUpperCase() + "]";

        boolean hover = mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H;
        ctx.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF2a2a2a);
        ctx.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, hover ? 0xFF4a8c3a : 0xFF3a3a3a);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, label, bx + BTN_W / 2, by + 4, 0xFFFFFFFF);
    }

    public static boolean handleClick(double mouseX, double mouseY, HandledScreen<?> screen) {
        if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return false;
        String title = screen.getTitle().getString();
        if (!title.equals(BLACKSMITH_TITLE)) return false;

        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int bx = acc.getX() + acc.getBackgroundWidth() + 4;
        int by = acc.getY() + 4;

        if (mouseX >= bx && mouseX < bx + BTN_W && mouseY >= by && mouseY < by + BTN_H) {
            startRepair();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRepairing..."));
            return true;
        }
        return false;
    }
}
