package julianh06.wynnextras.features.badges;

import com.wynntils.core.components.Models;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.RenderWorldEvent;
import julianh06.wynnextras.utils.WEVec;
import julianh06.wynnextras.utils.render.WorldRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Renders WynnExtras badge (star) above players who use the mod.
 */
@WEModule
public class BadgeRenderer {
    private static final Text BADGE_TEXT = Text.literal("\u2620").formatted(Formatting.GOLD);
    private static final float BADGE_SCALE = 0.5f;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (!WynnExtrasConfig.INSTANCE.badgesEnabled) return;
        if (!Models.WorldState.onWorld()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        ClientPlayerEntity localPlayer = mc.player;

        // Iterate through all players in the world
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) continue;
            if (player == localPlayer) continue; // Don't render badge for self

            // Check if this player is a WynnExtras user
            String uuid = player.getUuid().toString();
            if (!BadgeService.isWynnExtrasUser(uuid)) continue;

            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);

            // Get player position for badge rendering
            WEVec playerPos = new WEVec(
                    player.getLerpedPos(tickDelta).x,
                    player.getLerpedPos(tickDelta).y + (player.isSneaking() ? 1.72 : 2.15) + 0.4,
                    player.getLerpedPos(tickDelta).z
            );

            //System.out.println("reder at " + player.getLerpedPos(tickDelta).x + " " + player.getLerpedPos(tickDelta).y + " " + player.getLerpedPos(tickDelta).z);

            // Render the star badge
            //u2618
            //u2620
            //u2665
            WorldRenderUtils.drawText(event, playerPos, BADGE_TEXT, BADGE_SCALE, true);
        }
    }
}
