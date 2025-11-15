package julianh06.wynnextras.features.inventory.BankOverlayButtons;

import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.features.inventory.BankOverlay;
import julianh06.wynnextras.features.inventory.BankOverlayType;
import julianh06.wynnextras.features.inventory.data.AccountBankData;
import julianh06.wynnextras.features.inventory.data.CharacterBankData;
import julianh06.wynnextras.mixin.BankOverlay.HandledScreenMixin;
import julianh06.wynnextras.utils.overlays.EasyButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.tick.Tick;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.wynntils.utils.wynn.ContainerUtils.clickOnSlot;
import static julianh06.wynnextras.features.inventory.BankOverlay.*;

public class CharacterBankButton extends EasyButton {
    public CharacterBankButton(int x, int y, int height, int width) {
        super(x, y, height, width);
    }

    public CharacterBankButton(int x, int y, int height, int width, @Nullable String text) {
        super(x, y, height, width, text);
    }

    @Override
    public void click() {
        ScreenHandler currScreenHandler = McUtils.containerMenu();

        activeInv = 0;
        scrollOffset = 0;
        currentData.save();

        if(currentOverlayType == BankOverlayType.CHARACTER) expectedOverlayType = BankOverlayType.ACCOUNT;
        else if (currentOverlayType == BankOverlayType.ACCOUNT) expectedOverlayType = BankOverlayType.CHARACTER;

        if(currScreenHandler == null) { return; }
        clickOnSlot(47, currScreenHandler.syncId, 0, currScreenHandler.getStacks());
        timeSinceSwitch = Time.now().timestamp();
    }
}
