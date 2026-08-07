package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class ProfessionState {
    public record Xp(int current, int max) {}

    private static final Pattern MENU_LEVEL = Pattern.compile("(?i)(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing).*?(?:lv\\.?|level)\\D*(\\d{1,3})(?:.*?(\\d{1,3}(?:\\.\\d+)?)%)?");
    private static final Pattern XP_GAIN = Pattern.compile("(?i)\\+([\\d,.]+)\\s+(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing)\\s+(?:xp|experience)");
    private static final Pattern LEVEL_UP = Pattern.compile("(?i)(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing).*?(?:level|lv\\.?)\\D*(\\d{1,3})");

    public static int level(WEProfessionType profession) {
        return WynnExtrasConfig.INSTANCE.professionLevels.getOrDefault(key(profession), 0);
    }

    public static Xp xp(WEProfessionType profession) {
        String key = key(profession);
        return new Xp(WynnExtrasConfig.INSTANCE.professionXpCurrent.getOrDefault(key, 0),
                WynnExtrasConfig.INSTANCE.professionXpMax.getOrDefault(key, 0));
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 10 != 0) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
            for (var slot : screen.getScreenHandler().slots) parseMenuStack(slot.getStack());
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        String line = clean(event.message.getString());
        Matcher gain = XP_GAIN.matcher(line);
        if (gain.find()) {
            WEProfessionType profession = WEProfessionType.fromString(gain.group(2));
            float amount = floatValue(gain.group(1));
            if (profession != null && amount > 0) {
                ProfessionOverlay.onXpGain(profession, amount);
                String key = key(profession);
                WynnExtrasConfig.INSTANCE.professionXpCurrent.merge(key, Math.round(amount), Integer::sum);
                WynnExtrasConfig.save();
            }
        }
        Matcher levelUp = LEVEL_UP.matcher(line);
        if (levelUp.find()) update(WEProfessionType.fromString(levelUp.group(1)), intValue(levelUp.group(2)), null);
    }

    private static void parseMenuStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        StringBuilder text = new StringBuilder(clean(stack.getName().getString()));
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) for (Text line : lore.lines()) text.append(' ').append(clean(line.getString()));
        Matcher matcher = MENU_LEVEL.matcher(text);
        if (!matcher.find()) return;
        Integer percent = matcher.group(3) == null ? null : Math.round(floatValue(matcher.group(3)));
        update(WEProfessionType.fromString(matcher.group(1)), intValue(matcher.group(2)), percent);
    }

    private static void update(WEProfessionType profession, int level, Integer percent) {
        if (profession == null || level <= 0) return;
        String key = key(profession);
        Integer previousLevel = WynnExtrasConfig.INSTANCE.professionLevels.put(key, level);
        boolean changed = previousLevel == null || previousLevel != level;
        if (percent != null) {
            Integer old = WynnExtrasConfig.INSTANCE.professionXpCurrent.put(key, percent);
            changed |= old == null || old != percent;
            WynnExtrasConfig.INSTANCE.professionXpMax.put(key, 100);
        }
        if (changed) WynnExtrasConfig.save();
    }

    private static String key(WEProfessionType profession) {
        return CharacterState.id().orElse("unknown") + ":" + profession.name();
    }

    private static String clean(String value) { return value.replaceAll("§[0-9a-fk-or]", "").trim(); }
    private static int intValue(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; } }
    private static float floatValue(String value) { try { return Float.parseFloat(value.replace(",", "")); } catch (Exception ignored) { return 0; } }
}
