package julianh06.wynnextras.features.render;


import julianh06.wynnextras.config.WynnExtrasConfig;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerRenderFilter {
    private static WynnExtrasConfig config;


    private static final Set<UUID> hiddenPlayers = new HashSet<>();

    public static void hide(PlayerEntity player) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
        }
        if(config.printDebugToConsole) {
            System.out.println(player.getName() + " is now hidden");
        }
        hiddenPlayers.add(player.getUuid());
    }

    public static void show(PlayerEntity player) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
        }
        if(config.printDebugToConsole) {
            System.out.println(player.getName() + " is now shown");
        }
        hiddenPlayers.remove(player.getUuid());
    }

    public static boolean isHidden(PlayerEntity player) {
        if(config == null) {
            config = WynnExtrasConfig.INSTANCE;
        }
        return hiddenPlayers.contains(player.getUuid());
    }
}
