package julianh06.wynnextras.features.misc;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import julianh06.wynnextras.utils.UI.WEMenuExtension;
import julianh06.wynnextras.utils.UI.Widget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.lwjgl.glfw.GLFW;

public class QuickRepair extends WEMenuExtension {

    private static final String BLACKSMITH_TITLE = "󏿸";
    private static final String REPAIR_TITLE = "󏿸";
    private static final int SLOT_REPAIR_ITEMS = 18;
    private static final int SLOT_ITEM = 11;
    private static final int EMPTY_CLOSE_THRESHOLD = 6;
    private static final int BTN_W = 90, BTN_H = 16;

    private static boolean repairing = false;
    private static int spamCooldown = 0;
    private static int emptySlotTicks = 0;
    private static boolean keyWasDown = false;

    private RepairButtonWidget repairButton = null;

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

    @Override
    protected void drawBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WynnExtrasConfig.INSTANCE.quickRepairEnabled) return;
        if (!(McUtils.screen() instanceof HandledScreen<?> screen)) return;
        if (!screen.getTitle().getString().equals(BLACKSMITH_TITLE)) return;

        if (repairButton == null) {
            repairButton = new RepairButtonWidget();
            rootWidgets.add(repairButton);
        }

        HandledScreenAccessor acc = (HandledScreenAccessor) screen;
        int bx = acc.getX() + acc.getBackgroundWidth() / 2 - BTN_W / 2;
        int by = acc.getY() + acc.getBackgroundWidth() - BTN_H / 4;
        repairButton.setBounds(bx, by, BTN_W, BTN_H);
        repairButton.setVisible(true);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (repairButton != null && !WynnExtrasConfig.INSTANCE.quickRepairEnabled)
            repairButton.setVisible(false);
    }

    private static class RepairButtonWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            String keyName = GLFW.glfwGetKeyName(WynnExtrasConfig.INSTANCE.quickRepairKey, 0);
            if (keyName == null) keyName = "?";
            String label = "Quick Repair [" + keyName.toUpperCase() + "]";

            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText(label, x + width / 2f, y + height / 2f, 1f);
        }

        @Override
        protected boolean onClick(int button) {
            startRepair();
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§aRepairing..."));
            return true;
        }
    }
}
