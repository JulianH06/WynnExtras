// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Yarn adaptation of Wynntils' BossHealthOverlayMixin. Deviation: we do NOT
 * replace the bossBars map with a concurrent one (Wynntils does that for its
 * own render-thread access patterns; the shim only reads it in event handlers
 * on the render thread).
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import java.util.Map;
import java.util.UUID;
import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.mc.event.BossHealthUpdateEvent;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    @Shadow
    @Final
    Map<UUID, ClientBossBar> bossBars;

    @Inject(
            method = "handlePacket(Lnet/minecraft/network/packet/s2c/play/BossBarS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void handlePacketPre(BossBarS2CPacket packet, CallbackInfo ci) {
        BossHealthUpdateEvent event = new BossHealthUpdateEvent(packet, bossBars);
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
}
