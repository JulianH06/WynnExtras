package julianh06.wynnextras.mixin;

import java.util.UUID;
import julianh06.wynnextras.features.debug.GameDataRecorder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the /we record capture file with the packet-shaped data the chat events
 * can't see: titles, scoreboard updates, boss bars and container titles.
 * Read-only taps at RETURN — vanilla handling is never altered.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class GameDataRecorderMixin {
    @Inject(method = "onTitle(Lnet/minecraft/network/packet/s2c/play/TitleS2CPacket;)V", at = @At("RETURN"))
    private void wynnextras$recordTitle(TitleS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.record("title", packet.text());
    }

    @Inject(method = "onSubtitle(Lnet/minecraft/network/packet/s2c/play/SubtitleS2CPacket;)V", at = @At("RETURN"))
    private void wynnextras$recordSubtitle(SubtitleS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.record("subtitle", packet.text());
    }

    @Inject(
            method = "onScoreboardScoreUpdate(Lnet/minecraft/network/packet/s2c/play/ScoreboardScoreUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void wynnextras$recordScoreSet(ScoreboardScoreUpdateS2CPacket packet, CallbackInfo ci) {
        if (!GameDataRecorder.isEnabled()) return;
        GameDataRecorder.recordRaw("scoreboard_set", packet.scoreHolderName(),
                "objective", packet.objectiveName(), "score", String.valueOf(packet.score()));
        packet.display().ifPresent(display -> GameDataRecorder.record("scoreboard_set_display", display,
                "objective", packet.objectiveName()));
    }

    @Inject(
            method = "onScoreboardScoreReset(Lnet/minecraft/network/packet/s2c/play/ScoreboardScoreResetS2CPacket;)V",
            at = @At("RETURN"))
    private void wynnextras$recordScoreReset(ScoreboardScoreResetS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.recordRaw("scoreboard_reset", packet.scoreHolderName(),
                "objective", String.valueOf(packet.objectiveName()));
    }

    @Inject(
            method =
                    "onScoreboardObjectiveUpdate(Lnet/minecraft/network/packet/s2c/play/ScoreboardObjectiveUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void wynnextras$recordObjective(ScoreboardObjectiveUpdateS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.record("scoreboard_objective", packet.getDisplayName(),
                "name", packet.getName(), "mode", String.valueOf(packet.getMode()));
    }

    @Inject(
            method = "onScoreboardDisplay(Lnet/minecraft/network/packet/s2c/play/ScoreboardDisplayS2CPacket;)V",
            at = @At("RETURN"))
    private void wynnextras$recordScoreboardDisplay(ScoreboardDisplayS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.recordRaw("scoreboard_display", String.valueOf(packet.getName()),
                "slot", String.valueOf(packet.getSlot()));
    }

    @Inject(method = "onOpenScreen(Lnet/minecraft/network/packet/s2c/play/OpenScreenS2CPacket;)V", at = @At("RETURN"))
    private void wynnextras$recordContainerTitle(OpenScreenS2CPacket packet, CallbackInfo ci) {
        GameDataRecorder.record("container_title", packet.getName(),
                "syncId", String.valueOf(packet.getSyncId()));
    }

    @Inject(method = "onBossBar(Lnet/minecraft/network/packet/s2c/play/BossBarS2CPacket;)V", at = @At("RETURN"))
    private void wynnextras$recordBossBar(BossBarS2CPacket packet, CallbackInfo ci) {
        if (!GameDataRecorder.isEnabled()) return;
        packet.accept(new BossBarS2CPacket.Consumer() {
            @Override
            public void add(UUID uuid, Text name, float percent, BossBar.Color color, BossBar.Style style,
                            boolean darkenSky, boolean dragonMusic, boolean thickenFog) {
                GameDataRecorder.record("bossbar_add", name, "percent", String.valueOf(percent));
            }

            @Override
            public void updateName(UUID uuid, Text name) {
                GameDataRecorder.record("bossbar_name", name);
            }

            @Override
            public void updateProgress(UUID uuid, float percent) {
                GameDataRecorder.recordRaw("bossbar_progress", String.valueOf(percent));
            }
        });
    }
}
