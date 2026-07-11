package julianh06.wynnextras.mixin;

import julianh06.wynnextras.mixin.Accessor.BossBarS2CPacketAccessor;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Shadow @Final private Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private void ignoreUnknownBossBarUpdates(BossBarS2CPacket packet, CallbackInfo ci) {
        UUID uuid = ((BossBarS2CPacketAccessor) packet).getUuid();
        if (bossBars.containsKey(uuid)) return;

        boolean[] updateAction = {false};
        packet.accept(new BossBarS2CPacket.Consumer() {
            @Override
            public void updateProgress(UUID uuid, float percent) {
                updateAction[0] = true;
            }

            @Override
            public void updateName(UUID uuid, Text name) {
                updateAction[0] = true;
            }

            @Override
            public void updateStyle(UUID id, BossBar.Color color, BossBar.Style style) {
                updateAction[0] = true;
            }

            @Override
            public void updateProperties(UUID uuid, boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                updateAction[0] = true;
            }
        });

        if (updateAction[0]) ci.cancel();
    }
}