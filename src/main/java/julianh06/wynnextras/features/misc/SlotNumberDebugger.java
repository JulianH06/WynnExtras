package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.core.WynnExtras;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

public final class SlotNumberDebugger {
    private static boolean enabled;

    private SlotNumberDebugger() {}

    public static void toggle() {
        enabled = !enabled;
        WynnExtras.sendMessageToClient(
                enabled ? "§aSlot numbers enabled." : "§cSlot numbers disabled.");
    }

    public static void render(DrawContext context, Slot slot) {
        if (!enabled) return;

        String slotNumber = Integer.toString(slot.id);
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int x = slot.x + 16 - textRenderer.getWidth(slotNumber);
        context.drawText(textRenderer, slotNumber, x, slot.y, 0xFFFFFF55, true);
    }
}
