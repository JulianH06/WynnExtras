// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 *
 * Yarn adaptation of Wynntils' ClientPacketListenerMixin (the subset of packet
 * events the shim's models/handlers consume).
 */
package julianh06.wynnextras.wtshim.fabric.mixin;

import julianh06.wynnextras.wtshim.core.events.MixinHelper;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.mc.event.AdvancementUpdateEvent;
import julianh06.wynnextras.wtshim.mc.event.ConnectionEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetContentEvent;
import julianh06.wynnextras.wtshim.mc.event.ContainerSetSlotEvent;
import julianh06.wynnextras.wtshim.mc.event.MenuEvent;
import julianh06.wynnextras.wtshim.mc.event.PlayerInfoEvent;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardEvent;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardSetDisplayObjectiveEvent;
import julianh06.wynnextras.wtshim.mc.event.ScoreboardSetObjectiveEvent;
import julianh06.wynnextras.wtshim.mc.event.SubtitleSetTextEvent;
import julianh06.wynnextras.wtshim.mc.event.TitleSetTextEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Unique
    private static boolean isRenderThread() {
        return MinecraftClient.getInstance().isOnThread();
    }

    @Inject(method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V", at = @At("RETURN"))
    private void onGameJoinPost(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.postAlways(new ConnectionEvent.ConnectedEvent());
    }

    // Tab-list display-name updates carry Wynncraft's world-name entry (WORLD_NAME_UUID) —
    // WorldStateModel reads it to detect the current "WC<N>" world. Yarn equivalent of
    // Wynntils' ClientPacketListenerMixin#handlePlayerInfoUpdate (UPDATE_DISPLAY_NAME branch).
    @Inject(
            method = "onPlayerList(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;)V",
            at = @At("RETURN"))
    private void onPlayerListPost(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;
        if (!packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)) return;

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            if (entry.displayName() == null) continue;
            MixinHelper.post(new PlayerInfoEvent.PlayerDisplayNameChangeEvent(entry.profileId(), entry.displayName()));
        }
    }

    // Yarn equivalent of Wynntils' ClientPacketListenerMixin#handlePlayerInfoRemove.
    @Inject(
            method = "onPlayerRemove(Lnet/minecraft/network/packet/s2c/play/PlayerRemoveS2CPacket;)V",
            at = @At("RETURN"))
    private void onPlayerRemovePost(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        for (UUID uuid : packet.profileIds()) {
            MixinHelper.post(new PlayerInfoEvent.PlayerLogOutEvent(uuid));
        }
    }

    @Inject(
            method = "onOpenScreen(Lnet/minecraft/network/packet/s2c/play/OpenScreenS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onOpenScreenPre(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MenuEvent.MenuOpenedEvent.Pre event = new MenuEvent.MenuOpenedEvent.Pre(
                packet.getScreenHandlerType(), packet.getName(), packet.getSyncId());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onOpenScreen(Lnet/minecraft/network/packet/s2c/play/OpenScreenS2CPacket;)V",
            at = @At("RETURN"))
    private void onOpenScreenPost(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new MenuEvent.MenuOpenedEvent.Post(
                packet.getScreenHandlerType(), packet.getName(), packet.getSyncId()));
    }

    @Inject(
            method = "onCloseScreen(Lnet/minecraft/network/packet/s2c/play/CloseScreenS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onCloseScreenPre(CloseScreenS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MenuEvent.MenuClosedEvent event = new MenuEvent.MenuClosedEvent(packet.getSyncId());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onInventoryPre(InventoryS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ContainerSetContentEvent.Pre event = new ContainerSetContentEvent.Pre(
                packet.contents(), packet.cursorStack(), packet.syncId(), packet.revision());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onInventory(Lnet/minecraft/network/packet/s2c/play/InventoryS2CPacket;)V",
            at = @At("RETURN"))
    private void onInventoryPost(InventoryS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ContainerSetContentEvent.Post(
                packet.contents(), packet.cursorStack(), packet.syncId(), packet.revision()));
    }

    @Inject(
            method =
                    "onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onScreenHandlerSlotUpdatePre(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ContainerSetSlotEvent.Pre event = new ContainerSetSlotEvent.Pre(
                packet.getSyncId(), packet.getRevision(), packet.getSlot(), packet.getStack());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "onScreenHandlerSlotUpdate(Lnet/minecraft/network/packet/s2c/play/ScreenHandlerSlotUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void onScreenHandlerSlotUpdatePost(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ContainerSetSlotEvent.Post(
                packet.getSyncId(), packet.getRevision(), packet.getSlot(), packet.getStack()));
    }

    @Inject(
            method = "onTitle(Lnet/minecraft/network/packet/s2c/play/TitleS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onTitlePre(TitleS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        TitleSetTextEvent event = new TitleSetTextEvent(packet.text());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onSubtitle(Lnet/minecraft/network/packet/s2c/play/SubtitleS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onSubtitlePre(SubtitleS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        SubtitleSetTextEvent event = new SubtitleSetTextEvent(packet.text());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method =
                    "onScoreboardObjectiveUpdate(Lnet/minecraft/network/packet/s2c/play/ScoreboardObjectiveUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void onScoreboardObjectiveUpdate(ScoreboardObjectiveUpdateS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ScoreboardSetObjectiveEvent(
                packet.getName(), packet.getDisplayName(), packet.getType(), packet.getMode()));
    }

    @Inject(
            method =
                    "onScoreboardScoreUpdate(Lnet/minecraft/network/packet/s2c/play/ScoreboardScoreUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void onScoreboardScoreUpdate(ScoreboardScoreUpdateS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ScoreboardEvent.Set(
                StyledText.fromString(packet.scoreHolderName()), packet.objectiveName(), packet.score()));
    }

    @Inject(
            method =
                    "onScoreboardScoreReset(Lnet/minecraft/network/packet/s2c/play/ScoreboardScoreResetS2CPacket;)V",
            at = @At("RETURN"))
    private void onScoreboardScoreReset(ScoreboardScoreResetS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new ScoreboardEvent.Reset(
                StyledText.fromString(packet.scoreHolderName()), packet.objectiveName()));
    }

    @Inject(
            method = "onScoreboardDisplay(Lnet/minecraft/network/packet/s2c/play/ScoreboardDisplayS2CPacket;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onScoreboardDisplay(ScoreboardDisplayS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        ScoreboardSetDisplayObjectiveEvent event =
                new ScoreboardSetDisplayObjectiveEvent(packet.getSlot(), packet.getName());
        MixinHelper.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onAdvancements(Lnet/minecraft/network/packet/s2c/play/AdvancementUpdateS2CPacket;)V",
            at = @At("RETURN"))
    private void onAdvancements(AdvancementUpdateS2CPacket packet, CallbackInfo ci) {
        if (!isRenderThread()) return;

        MixinHelper.post(new AdvancementUpdateEvent(
                packet.shouldClearCurrent(),
                packet.getAdvancementsToEarn(),
                packet.getAdvancementIdsToRemove(),
                packet.getAdvancementsToProgress()));
    }
}
