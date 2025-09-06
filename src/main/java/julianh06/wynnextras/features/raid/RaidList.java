package julianh06.wynnextras.features.raid;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.*;
import julianh06.wynnextras.mixin.Accessor.RaidInfoAccessor;
import net.minecraft.client.MinecraftClient;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.List;

import static julianh06.wynnextras.features.raid.RaidListData.INSTANCE;

@WEModule
public class RaidList {
    public static boolean inRaidListMenu = false;

    private static Command raidListCmd = new Command(
            "raidlist",
            "",
            context -> {
                MinecraftClient client = context.getSource().getClient();
                client.send(() -> client.setScreen(null));
                inRaidListMenu = true;
                return 1;
            },
            null,
            null
    );

    @SubscribeEvent
    void onRaidEnded(RaidEndedEvent event) {
        List<String> members = new ArrayList<>(RaidListScreen.currentPlayers);
        if(event instanceof RaidEndedEvent.Completed) {
            Long raidEndTime = ((RaidInfoAccessor) event.getRaid()).getRaidStartTime() + event.getRaid().getTimeInRaid();
            INSTANCE.raids.add(new RaidData(event.getRaid(), members, raidEndTime, true));
            RaidListData.save();
        }
        if(event instanceof RaidEndedEvent.Failed) {
            Long raidEndTime = ((RaidInfoAccessor) event.getRaid()).getRaidStartTime() + event.getRaid().getTimeInRaid();
            INSTANCE.raids.add(new RaidData(event.getRaid(), members, raidEndTime, false));
            RaidListData.save();
        }
        RaidListScreen.currentPlayers.clear();
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if(inRaidListMenu) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new RaidListScreen()));
            inRaidListMenu = false;
        }
    }
}
