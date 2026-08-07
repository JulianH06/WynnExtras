package julianh06.wynnextras.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import julianh06.wynnextras.event.SetEntityDataEvent;
import julianh06.wynnextras.wynncraft.state.RaidState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPacketListenerMixin {
    @Inject(method = "onTitle", at = @At("TAIL"))
    private void wynnExtras$observeRaidTitle(TitleS2CPacket packet, CallbackInfo ci) {
        RaidState.observeTitle(packet.text());
    }

    @Inject(method = {
            "onScoreboardObjectiveUpdate",
            "onScoreboardScoreUpdate",
            "onScoreboardScoreReset",
            "onScoreboardDisplay"
    }, at = @At("TAIL"))
    private void wynnExtras$observeRaidScoreboard(CallbackInfo ci) {
        RaidState.observeScoreboard();
    }

    @ModifyArg(
            method = "onEntityTrackerUpdate(Lnet/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/data/DataTracker;writeUpdatedEntries(Ljava/util/List;)V"),
            index = 0)
    private List<DataTracker.SerializedEntry<?>> handleSetEntityDataPre(
            List<DataTracker.SerializedEntry<?>> packedItems,
            @Local(argsOnly = true) EntityTrackerUpdateS2CPacket packet) {
        if (!isRenderThread()) return packedItems;

        SetEntityDataEvent event = new SetEntityDataEvent(packet);
        event.post();
        return event.getPackedItems();
    }

    @Unique
    private static boolean isRenderThread() {
        return MinecraftClient.getInstance().isOnThread();
    }
}
