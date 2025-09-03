package julianh06.wynnextras.features.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.components.Models;
import com.wynntils.models.players.event.HadesRelationsUpdateEvent;
import com.wynntils.models.raid.RaidModel;
import com.wynntils.models.raid.raids.*;
import com.wynntils.models.raid.type.RaidInfo;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.*;
import julianh06.wynnextras.features.inventory.BankOverlayData;
import julianh06.wynnextras.features.misc.ItemStackDeserializer;
import julianh06.wynnextras.features.misc.ItemStackSerializer;
import julianh06.wynnextras.mixin.Accessor.RaidInfoAccessor;
import julianh06.wynnextras.utils.OptionalTypeAdapter;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.tick.Tick;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static julianh06.wynnextras.features.raid.RaidListData.INSTANCE;

@WEModule
public class RaidList {
    public static boolean inRaidListMenu = false;
    private static boolean opened = false;

    private static Command raidListCmd = new Command(
            "Raidlist",
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
        List<String> members = Models.Party.getPartyMembers();
        McUtils.sendMessageToClient(Text.of("[Wynnextras] Raid ended."));
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
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if(inRaidListMenu) {
            MinecraftClient.getInstance().send(() -> MinecraftClient.getInstance().setScreen(new RaidListScreen()));
            inRaidListMenu = false;
        }
    }
}
