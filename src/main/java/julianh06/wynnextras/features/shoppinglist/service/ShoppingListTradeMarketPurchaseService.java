package julianh06.wynnextras.features.shoppinglist.service;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.shoppinglist.ShoppingListFeature;
import julianh06.wynnextras.features.shoppinglist.cart.ShoppingEntry;
import julianh06.wynnextras.features.shoppinglist.model.RequirementType;
import julianh06.wynnextras.features.shoppinglist.util.ShoppingListMaterialNameNormalizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class ShoppingListTradeMarketPurchaseService {
    private static final String PURCHASE_MENU_TITLE_PREFIX = "\uDAFF\uDFE8\uE015";
    private static final int PURCHASE_ITEM_SLOT = 22;
    private static final int AMOUNT_SLOT = 31;
    private static final ShoppingListHaveCountService HAVE_COUNT_SERVICE = new ShoppingListHaveCountService();

    private static PurchaseContext pendingContext;

    private ShoppingListTradeMarketPurchaseService() {}

    public static void handleAmountSlotClick(HandledScreen<?> screen, Slot focusedSlot, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || screen == null || screen.getTitle() == null
                || !screen.getTitle().getString().startsWith(PURCHASE_MENU_TITLE_PREFIX)
                || focusedSlot == null || focusedSlot.id != AMOUNT_SLOT) {
            return;
        }

        pendingContext = findContext(screen);
    }

    public static PurchaseContext currentContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (pendingContext == null || client == null || !(client.currentScreen instanceof ChatScreen)) {
            return null;
        }
        return pendingContext;
    }

    public static void clearContext() {
        clear();
    }

    public static boolean submitAmount(int amount) {
        PurchaseContext context = currentContext();
        MinecraftClient client = MinecraftClient.getInstance();
        if (context == null || amount <= 0 || client == null || client.player == null
                || client.player.networkHandler == null) {
            return false;
        }

        String message = Integer.toString(amount);
        client.player.networkHandler.sendChatMessage(message);
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addToMessageHistory(message);
        }
        clear();
        client.setScreen(null);
        return true;
    }

    private static PurchaseContext findContext(HandledScreen<?> screen) {
        if (screen.getScreenHandler() == null || screen.getScreenHandler().slots.size() <= PURCHASE_ITEM_SLOT) {
            return null;
        }
        ItemStack purchaseStack = screen.getScreenHandler().slots.get(PURCHASE_ITEM_SLOT).getStack();
        if (purchaseStack == null || purchaseStack.isEmpty()) {
            return null;
        }

        ShoppingListHaveCountService.StackSnapshot purchaseSnapshot = HAVE_COUNT_SERVICE.snapshot(purchaseStack);
        if (purchaseSnapshot == null) {
            return null;
        }
        for (Map.Entry<ShoppingEntry, Integer> cartEntry : ShoppingListFeature.shoppingCartService().cart().entries().entrySet()) {
            ShoppingEntry entry = cartEntry.getKey();
            if (!ShoppingListHaveCountService.matches(entry, purchaseSnapshot)) {
                continue;
            }
            int needed = ShoppingListRequirementCalculator.adjustedRequired(
                    entry,
                    cartEntry.getValue(),
                    WynnExtrasConfig.INSTANCE.shoppingListCraftMultiplier,
                    WynnExtrasConfig.INSTANCE.shoppingListProfessionSpeed);
            int have = HAVE_COUNT_SERVICE.count(entry).total();
            return new PurchaseContext(displayName(entry), needed, have);
        }
        return null;
    }

    private static String displayName(ShoppingEntry entry) {
        String cleanedName = ShoppingListTextCleaner.clean(entry.displayName());
        if (entry.type() != RequirementType.MATERIAL) {
            return cleanedName;
        }
        return ShoppingListMaterialNameNormalizer.baseName(cleanedName) + " T" + entry.materialTier();
    }

    private static void clear() {
        pendingContext = null;
    }

    public record PurchaseContext(String itemName, int needed, int have) {
        public int remaining() {
            return Math.max(0, needed - have);
        }

        public boolean needsRemainingButton() {
            return have < needed;
        }
    }
}
