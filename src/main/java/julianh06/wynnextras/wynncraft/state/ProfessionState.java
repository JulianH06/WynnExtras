package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.compat.wynntils.WynntilsProfessionAdapter;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.event.ChatEvent;
import julianh06.wynnextras.event.TickEvent;
import julianh06.wynnextras.features.misc.ProfessionOverlay;
import julianh06.wynnextras.features.profileviewer.data.CharacterData;
import julianh06.wynnextras.features.profileviewer.data.Profession;
import julianh06.wynnextras.utils.enums.WEProfessionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WEModule
public final class ProfessionState {
    public record Xp(double current, double max) {}

    private static final Pattern MENU_LEVEL = Pattern.compile("(?i)(?:lv\\.?|level)\\D*(\\d{1,3})\\s+(?:\\S+\\s+)?(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing).*?(\\d{1,3}(?:\\.\\d+)?)%");
    private static final Pattern XP_GAIN = Pattern.compile("(?i)\\+([\\d,.]+)\\s+(?:\\S+\\s+)?(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing)\\s+(?:xp|experience).*?([\\d.]+)%");
    private static final Pattern LEVEL_UP = Pattern.compile("(?i)(?:level|lv\\.?)\\D*(\\d{1,3})\\s+(?:in\\s+)?(?:\\S+\\s+)?(armouring|tailoring|weaponsmithing|woodworking|jeweling|alchemism|scribing|cooking|mining|woodcutting|farming|fishing)");
    private static final Pattern FORMATTING_CODE = Pattern.compile("§[0-9a-fk-or]");
    private static WEProfessionType lastGainProfession;
    private static float lastGainAmount;
    private static long lastGainTime;
    private static final Map<Integer, String> observedLabels = new HashMap<>();

    public static int level(WEProfessionType profession) {
        return WynnExtrasConfig.INSTANCE.professionLevels.getOrDefault(key(profession), 0);
    }

    public static Xp xp(WEProfessionType profession) {
        String key = key(profession);
        return new Xp(WynnExtrasConfig.INSTANCE.professionXpCurrent.getOrDefault(key, 0.0),
                WynnExtrasConfig.INSTANCE.professionXpMax.getOrDefault(key, 0.0));
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (event.ticks % 2 == 0) observeGatheringLabels();
        if (event.ticks % 10 != 0) return;
        try {
            boolean changed = syncFromWynntils();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen instanceof HandledScreen<?> screen) {
                for (var slot : screen.getScreenHandler().slots) changed |= parseMenuStack(slot.getStack());
            }
            if (changed) WynnExtrasConfig.save();
        } catch (Throwable ignored) {}
    }

    @SubscribeEvent
    public void onChat(ChatEvent event) {
        observeXpText(event.message.getString());
        String line = clean(event.message.getString());
        Matcher levelUp = LEVEL_UP.matcher(line);
        if (levelUp.find() && update(WEProfessionType.fromString(levelUp.group(2)), intValue(levelUp.group(1)), 0.0)) {
            WynnExtrasConfig.save();
        }
    }

    public static void syncFromApi(CharacterData character) {
        if (character == null || character.getProfessions() == null) return;
        boolean changed = false;
        for (Map.Entry<String, Profession> entry : character.getProfessions().entrySet()) {
            Profession profession = entry.getValue();
            if (profession == null) continue;
            changed |= update(WEProfessionType.fromString(entry.getKey()), profession.getLevel(),
                    (double) profession.getXpPercent());
        }
        if (changed) WynnExtrasConfig.save();
    }

    private static void observeGatheringLabels() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!WynnExtrasConfig.INSTANCE.professionOverlayEnabled || client.player == null || client.world == null) {
            observedLabels.clear();
            return;
        }

        Box searchBox = client.player.getBoundingBox().expand(48, 24, 48);
        Set<Integer> visibleLabels = new HashSet<>();
        for (Entity entity : client.world.getNonSpectatingEntities(Entity.class, searchBox)) {
            if (!(entity instanceof DisplayEntity.TextDisplayEntity label)) continue;
            String text = label.getText().getString();
            if (!text.toLowerCase().contains(" xp ")) continue;
            visibleLabels.add(label.getId());
            String previous = observedLabels.put(label.getId(), text);
            if (!text.equals(previous)) observeXpText(text);
        }
        observedLabels.keySet().retainAll(visibleLabels);
    }

    private static void observeXpText(String text) {
        String line = clean(text);
        Matcher gain = XP_GAIN.matcher(line);
        if (gain.find()) {
            WEProfessionType profession = WEProfessionType.fromString(gain.group(2));
            float amount = floatValue(gain.group(1));
            if (profession != null && amount > 0) {
                observeXpGain(profession, amount, doubleValue(gain.group(3)));
            }
        }
    }

    public static void observeXpGain(String professionName, float amount, double currentPercent) {
        observeXpGain(WEProfessionType.fromString(professionName), amount, currentPercent);
    }

    private static void observeXpGain(WEProfessionType profession, float amount, double currentPercent) {
        if (profession == null || amount <= 0) return;
        long now = System.currentTimeMillis();
        if (profession == lastGainProfession && Float.compare(amount, lastGainAmount) == 0
                && now - lastGainTime < 500) return;

        lastGainProfession = profession;
        lastGainAmount = amount;
        lastGainTime = now;

        syncFromWynntils(profession);
        String key = key(profession);
        WynnExtrasConfig.INSTANCE.professionXpCurrent.put(key, currentPercent);
        WynnExtrasConfig.INSTANCE.professionXpMax.put(key, 100.0);
        ProfessionOverlay.onXpGain(profession, amount);
        WynnExtrasConfig.save();
    }

    private static boolean syncFromWynntils() {
        boolean changed = false;
        for (WEProfessionType profession : WEProfessionType.values()) {
            changed |= syncFromWynntils(profession);
        }
        return changed;
    }

    private static boolean syncFromWynntils(WEProfessionType profession) {
        WynntilsProfessionAdapter.Progress progress = WynntilsProfessionAdapter.progress(profession.name()).orElse(null);
        return progress != null && update(profession, progress.level(), progress.percent());
    }

    private static boolean parseMenuStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        StringBuilder text = new StringBuilder(clean(stack.getName().getString()));
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) for (Text line : lore.lines()) text.append(' ').append(clean(line.getString()));
        Matcher matcher = MENU_LEVEL.matcher(text);
        if (!matcher.find()) return false;
        double percent = doubleValue(matcher.group(3));
        return update(WEProfessionType.fromString(matcher.group(2)), intValue(matcher.group(1)), percent);
    }

    private static boolean update(WEProfessionType profession, int level, Double percent) {
        if (profession == null || level <= 0) return false;
        String key = key(profession);
        Integer previousLevel = WynnExtrasConfig.INSTANCE.professionLevels.put(key, level);
        boolean changed = previousLevel == null || previousLevel != level;
        if (percent != null) {
            Double old = WynnExtrasConfig.INSTANCE.professionXpCurrent.put(key, percent);
            changed |= old == null || Double.compare(old, percent) != 0;
            WynnExtrasConfig.INSTANCE.professionXpMax.put(key, 100.0);
        }
        return changed;
    }

    private static String key(WEProfessionType profession) {
        return CharacterState.id().orElse("unknown") + ":" + profession.name();
    }

    private static String clean(String value) { return FORMATTING_CODE.matcher(value).replaceAll("").trim(); }
    private static int intValue(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; } }
    private static float floatValue(String value) { try { return Float.parseFloat(value.replace(",", "")); } catch (Exception ignored) { return 0; } }
    private static double doubleValue(String value) { try { return Double.parseDouble(value.replace(",", "")); } catch (Exception ignored) { return 0; } }
}
