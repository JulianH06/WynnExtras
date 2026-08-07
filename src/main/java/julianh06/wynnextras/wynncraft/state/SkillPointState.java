package julianh06.wynnextras.wynncraft.state;

import julianh06.wynnextras.wynncraft.menu.MenuType;
import julianh06.wynnextras.wynncraft.menu.WynncraftMenuService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillPointState {
    private static final Pattern NUMBER = Pattern.compile("(?:^|\\D)(\\d{1,3})(?:\\D|$)");
    private static final Pattern AVAILABLE = Pattern.compile("(?i)(?:available|remaining)\\s+(?:skill\\s*)?points?\\D*(\\d{1,3})");
    private static final Map<SkillPoint, Integer> ASSIGNED = new EnumMap<>(SkillPoint.class);
    private static int available;
    private static long updatedAt;

    private SkillPointState() {}

    public static int assigned(SkillPoint skill) {
        return skill == null ? 0 : ASSIGNED.getOrDefault(skill, 0);
    }

    public static int available() {
        return available;
    }

    public static long updatedAt() {
        return updatedAt;
    }

    public static void updateFromCurrentMenu() {
        if (!WynncraftMenuService.isCurrent(MenuType.CHARACTER_INFO)) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
            if (screen.getScreenHandler().slots.size() < 16) return;

            EnumMap<SkillPoint, Integer> parsed = new EnumMap<>(SkillPoint.class);
            int parsedAvailable = 0;
            boolean found = false;
            for (int i = 0; i < 5; i++) {
                ItemStack stack = screen.getScreenHandler().slots.get(11 + i).getStack();
                String text = stackText(stack);
                Integer value = parseAssigned(text, SkillPoint.values()[i]);
                if (value != null) {
                    parsed.put(SkillPoint.values()[i], value);
                    found = true;
                }
                Matcher availableMatcher = AVAILABLE.matcher(text);
                if (availableMatcher.find()) parsedAvailable = safeInt(availableMatcher.group(1));
            }
            if (!found) return;
            ASSIGNED.clear();
            ASSIGNED.putAll(parsed);
            available = parsedAvailable;
            updatedAt = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    static Integer parseAssigned(String text, SkillPoint skill) {
        if (text == null || skill == null) return null;
        String label = skill == SkillPoint.DEFENCE ? "defen[cs]e" : skill.name().toLowerCase();
        Matcher matcher = Pattern.compile("(?i)" + label + "\\D{0,12}(\\d{1,3})").matcher(text);
        if (matcher.find()) return safeInt(matcher.group(1));
        matcher = NUMBER.matcher(text);
        return matcher.find() ? safeInt(matcher.group(1)) : null;
    }

    private static String stackText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        StringBuilder text = new StringBuilder(stack.getName().getString());
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) for (Text line : lore.lines()) text.append('\n').append(line.getString());
        return text.toString().replaceAll("§[0-9a-fk-or]", "");
    }

    private static int safeInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
