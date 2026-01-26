package julianh06.wynnextras.features.render;


import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerRenderFilter {
    private static final Set<UUID> hiddenPlayers = new HashSet<>();

    public static void hide(PlayerEntity player) {
        hiddenPlayers.add(player.getUuid());
    }

    public static void show(PlayerEntity player) {
        hiddenPlayers.remove(player.getUuid());
    }

    public static boolean isHidden(PlayerEntity player) {
        return hiddenPlayers.contains(player.getUuid());
    }
}
