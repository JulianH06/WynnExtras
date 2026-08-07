package julianh06.wynnextras.features.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.utils.ContainerUtils;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@WEModule
public class BombRethrow {
    private static final Pattern MENU_COUNT_PATTERN = Pattern.compile("^\\d+x\\s+");
    private static final int OPEN_TIMEOUT_TICKS = 80;
    private static final int MENU_SCAN_TICKS = 20;

    private static RethrowRequest pendingRequest = null;

    private static final List<BombMapping> BOMB_MAPPINGS = List.of(
            new BombMapping("CombatXP", "Combat XP Bomb", List.of("Combat Experience Bomb", "Combat XP Bomb")),
            new BombMapping("ProfXP", "Profession XP Bomb", List.of("Profession Experience Bomb", "Profession XP Bomb")),
            new BombMapping("ProfSpeed", "Profession Speed Bomb", List.of("Profession Speed Bomb")),
            new BombMapping("Loot", "Loot Bomb", List.of("Loot Bomb")),
            new BombMapping("LootChest", "Loot Chest Bomb", List.of("Loot Chest Bomb")),
            new BombMapping("Dungeon", "Dungeon Bomb", List.of("Dungeon Bomb"))
    );

    private static final Command rethrowBombCmd = new Command(
            "rethrowbomb",
            "",
            context -> {
                requestRethrow(StringArgumentType.getString(context, "bombType"));
                return 1;
            },
            null,
            List.of(bombTypeArgument())
    );

    private static ArgumentBuilder<FabricClientCommandSource, ?> bombTypeArgument() {
        return ClientCommandManager.argument("bombType", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    for (BombMapping mapping : BOMB_MAPPINGS) {
                        builder.suggest(mapping.commandName());
                    }
                    return builder.buildFuture();
                });
    }

    @SubscribeEvent
    void onChat(ChatEvent event) {
        if (!WynnExtrasConfig.INSTANCE.bombRethrowSuggestion) return;

        BombMapping mapping = getExpiredBombMapping(event.message.getString());
        if (mapping == null) return;

        MinecraftClient.getInstance().send(() -> MinecraftUtils.sendMessageToClient(
                WynnExtras.addWynnExtrasPrefix(Text.literal(""))
                        .append(Text.literal("§e§nClick here to rethrow " + mapping.commandName()).setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.RunCommand("/we rethrowbomb " + mapping.commandName()))))
        ));
    }

    @SubscribeEvent
    void onTick(TickEvent event) {
        if (pendingRequest == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            pendingRequest = null;
            return;
        }

        pendingRequest.ticks++;
        if (pendingRequest.ticks > OPEN_TIMEOUT_TICKS) {
            fail("§cCould not find the bomb menu.");
            return;
        }

        if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
            return;
        }

        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        if (!title.equals("\uDAFF\uDFF8\uE033\uDAFF\uDF80\uF010")) return;

        pendingRequest.menuTicks++;
        ScreenHandler menu = screen.getScreenHandler();
        int slot = findBombSlot(menu, pendingRequest.mapping.menuName());
        if (slot >= 0) {
            ContainerUtils.clickOnSlot(slot, menu.syncId, 0, menu.getStacks());
            pendingRequest = null;
            return;
        }

        if (pendingRequest.menuTicks >= MENU_SCAN_TICKS) {
            client.player.closeHandledScreen();
            fail("§cNo " + pendingRequest.mapping.menuName() + " found in your bomb menu.");
        }
    }

    private static void requestRethrow(String bombType) {
        BombMapping mapping = getMapping(bombType);
        if (mapping == null && "TEST".equalsIgnoreCase(bombType)) {
            mapping = new BombMapping("TEST", "TEST", List.of("TEST"));
        }

        if (mapping == null) {
            MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cUnsupported bomb type: " + bombType));
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return;

        pendingRequest = new RethrowRequest(mapping);
        client.getNetworkHandler().sendChatCommand("bomb");
    }

    private static BombMapping getExpiredBombMapping(String message) {
        String clean = Formatting.strip(message);
        if (clean == null) return null;

        clean = clean.trim();
        for (BombMapping mapping : BOMB_MAPPINGS) {
            for (String expiredName : mapping.expiredNames()) {
                String suffix = " " + expiredName + " has expired!";
                for (String line : clean.split("\\R")) {
                    line = line.trim();
                    if (line.contains(suffix) && line.indexOf(suffix) > 0) return mapping;
                }
            }
        }
        return null;
    }

    private static BombMapping getMapping(String bombType) {
        String clean = normalizeBombType(bombType);
        for (BombMapping mapping : BOMB_MAPPINGS) {
            if (normalizeBombType(mapping.commandName()).equals(clean)) return mapping;
            if (normalizeBombType(mapping.menuName()).equals(clean)) return mapping;
            for (String expiredName : mapping.expiredNames()) {
                if (normalizeBombType(expiredName).equals(clean)) return mapping;
            }
        }
        return null;
    }

    private static int findBombSlot(ScreenHandler menu, String menuName) {
        String expected = normalizeMenuItemName(menuName);
        for (int i = 0; i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            String itemName = normalizeMenuItemName(stack.getName().getString());
            if (itemName.equals(expected)) return i;
        }
        return -1;
    }

    private static String normalizeBombType(String value) {
        return normalizeMenuItemName(value);
    }

    private static String normalizeMenuItemName(String value) {
        String clean = Formatting.strip(value);
        if (clean == null) clean = value;

        clean = MENU_COUNT_PATTERN.matcher(clean.trim()).replaceFirst("");
        clean = clean.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (clean.endsWith("s")) clean = clean.substring(0, clean.length() - 1);
        return clean;
    }

    private static void fail(String message) {
        MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(message));
        pendingRequest = null;
    }

    private record BombMapping(String commandName, String menuName, List<String> expiredNames) {}

    private static final class RethrowRequest {
        private final BombMapping mapping;
        private int ticks;
        private int menuTicks;

        private RethrowRequest(BombMapping mapping) {
            this.mapping = mapping;
        }
    }
}