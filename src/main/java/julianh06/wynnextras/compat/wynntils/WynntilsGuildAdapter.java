package julianh06.wynnextras.compat.wynntils;

import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WynntilsGuildAdapter {
    public record Resource(String symbol, String name, int generation, int current, int max, boolean stored) {}
    public record Territory(String name, String guildName, String guildPrefix, List<Resource> resources,
                            String treasury, String defences, boolean headquarters,
                            List<String> estimates, String timeHeld) {}

    private WynntilsGuildAdapter() {}

    public static void fixTradeRoutes() {
        try {
            List<?> pois = territoryPois();
            Map<String, Object> byName = new HashMap<>();
            for (Object poi : pois) byName.put(string(invoke(poi, "getName")), poi);
            int fixes = 0;
            for (Object poi : pois) {
                Object info = invoke(poi, "getTerritoryInfo");
                Object routes = invoke(info, "getTradingRoutes");
                if (!(routes instanceof List<?> routeNames)) continue;
                for (Object routeName : routeNames) {
                    Object routePoi = byName.get(String.valueOf(routeName));
                    Object routeInfo = invoke(routePoi, "getTerritoryInfo");
                    Object reverse = invoke(routeInfo, "getTradingRoutes");
                    if (reverse instanceof List raw && !raw.contains(string(invoke(poi, "getName")))) {
                        raw.add(string(invoke(poi, "getName")));
                        fixes++;
                    }
                }
            }
            if (fixes > 0) julianh06.wynnextras.core.WynnExtras.LOGGER.info("Applied " + fixes + " trade route fixes");
        } catch (Throwable ignored) {}
    }

    public static Territory territory(Object poi) {
        try {
            Object info = invoke(poi, "getTerritoryInfo");
            Object profile = invoke(poi, "getTerritoryProfile");
            if (info == null || profile == null) return null;
            List<Resource> resources = new ArrayList<>();
            Class<? extends Enum> resourceClass = WynntilsCompat.requireClass(
                    "com.wynntils.models.territories.type.GuildResource").asSubclass(Enum.class);
            for (Object resource : resourceClass.getEnumConstants()) {
                int generation = number(invoke(info, "getGeneration", resource));
                Object storage = invoke(info, "getStorage", resource);
                resources.add(new Resource(string(invoke(resource, "getPrettySymbol")), string(invoke(resource, "getName")),
                        generation, number(invoke(storage, "current")), number(invoke(storage, "max")), storage != null));
            }
            Object treasury = invoke(info, "getTreasury");
            Object defences = invoke(info, "getDefences");
            String guildName = string(invoke(info, "getGuildName"));
            String profileGuild = string(invoke(profile, "getGuild"));
            String held = profileGuild.equals(guildName)
                    ? string(invoke(profile, "getTimeAcquiredColor")) + string(invoke(profile, "getReadableRelativeTimeAcquired")) : "-";
            List<String> estimates = estimateDefences(info);
            return new Territory(string(invoke(poi, "getName")), guildName, string(invoke(info, "getGuildPrefix")),
                    resources, formattedValue(treasury, "getTreasuryColor"), formattedValue(defences, "getDefenceColor"),
                    Boolean.TRUE.equals(invoke(info, "isHeadquarters")), estimates, held);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<?> territoryPois() throws ReflectiveOperationException {
        Class<?> models = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
        Object model = models.getField("Territory").get(null);
        Object result = model.getClass().getMethod("getTerritoryPoisFromAdvancement").invoke(model);
        return result instanceof List<?> list ? list : List.of();
    }

    private static String formattedValue(Object value, String colorMethod) {
        return string(invoke(value, colorMethod)) + string(invoke(value, "getAsString"));
    }

    private static List<String> estimateDefences(Object info) {
        try {
            int defenceLevel = number(invoke(invoke(info, "getDefences"), "getLevel"));
            int veryLow = resourceValueLevel("VERY_LOW");
            if (defenceLevel <= veryLow) return null;
            Map<?, ?> generators = asMap(invoke(info, "getGenerators"));
            Map<?, ?> storage = asMap(invoke(info, "getStorage"));
            if (generators.size() > 3) return List.of(Formatting.RED + "Unknown " + Formatting.GRAY + "(Rainbow territory)");
            Integer producedEmeralds = mapNumber(generators, "EMERALDS");
            if (producedEmeralds == null) return List.of(Formatting.RED + "Unknown " + Formatting.GRAY + "(Missing generation info)");

            int[] used = estimateUsedResources(defenceLevel, producedEmeralds, storage,
                    Boolean.TRUE.equals(invoke(info, "isHeadquarters")));
            if (used == null) return List.of(Formatting.RED + "Unknown " + Formatting.GRAY + "(Invalid storage info)");

            int currentEmeralds = capped(storage, "EMERALDS", "current");
            int predicted = Math.clamp((int) Math.round(currentEmeralds / (producedEmeralds / 60d) * 60), 0, 60);
            boolean fallback = predicted == 60;
            if (fallback) predicted = 30;
            int seconds = Math.max(1, 60 - predicted);
            double multiplier = 60d / seconds;
            String[] resources = {"ORE", "WOOD", "CROPS", "FISH"};
            double[] bases = {1000d, 300000d, 0.5d, 10d};
            double[][] bonuses = {
                    {1.4,1.8,2.2,2.6,3,3.4,3.8,4.2,4.6,5,5.4},
                    {1.5,2,2.5,3.2,4,5,6.2,7.4,8.6,9.8,11},
                    {1.5,2,2.5,3.2,4,5,6,7.2,7.6,8.4,9.4},
                    {4,5.5,6.25,7,7.5,7.9,8.2,8.4,8.6,8.8,9}
            };
            int[] costs = {100,300,600,1200,2400,4800,8400,12000,15600,19200,22800};
            double[] values = new double[4];
            int[] tiers = new int[4];
            for (int r = 0; r < 4; r++) {
                double stored = capped(storage, resources[r], "current");
                int produced = valueOrZero(mapNumber(generators, resources[r]));
                stored -= produced / 3600d * seconds;
                if (stored <= 0) { values[r] = -1; tiers[r] = -1; continue; }
                double start = stored * multiplier - used[r] / 60d;
                int best = 0;
                for (int i = 0; i < costs.length; i++) {
                    if (costs[i] / 60d <= start) best = i;
                    else break;
                }
                values[r] = bases[r] * bonuses[r][best];
                tiers[r] = best + 1;
            }
            double connectionBoost = 1 + 0.3 * uniqueConnections(info, 1);
            if (Boolean.TRUE.equals(invoke(info, "isHeadquarters"))) connectionBoost *= 1.5 + 0.25 * uniqueConnections(info, 3);
            List<String> result = new ArrayList<>();
            if (fallback) result.add(Formatting.YELLOW + "Warning: using fallback time");
            result.add(Formatting.WHITE + "Ⓑ " + format("%.0f", values[0] * connectionBoost) + "-"
                    + format("%.0f", values[0] * connectionBoost * 1.5) + " Damage" + tier(tiers[0])
                    + Formatting.DARK_GRAY + " (x" + String.format("%.2f", connectionBoost) + ")");
            result.add(Formatting.YELLOW + "Ⓙ " + format("%.2f", values[2]) + " Attacks/s" + tier(tiers[2]));
            result.add(Formatting.GOLD + "Ⓒ " + format("%.0f", values[1] * connectionBoost / 1000) + "k HP" + tier(tiers[1]));
            result.add(Formatting.AQUA + "Ⓚ " + format("%.0f", values[3]) + "% Defence" + tier(tiers[3]));
            result.add(Formatting.DARK_GRAY + "Next resource move prediction: " + (60 - predicted) + "s");
            return result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int[] estimateUsedResources(int defenceLevel, int emeraldsProduced, Map<?, ?> storage, boolean headquarters) {
        int[] used = new int[5];
        if (defenceLevel >= resourceValueLevel("MEDIUM")) { used[2] += 800; used[0] += 200; }
        if (defenceLevel >= resourceValueLevel("HIGH")) { used[0] += 200; used[1] += 400; }
        if (defenceLevel >= resourceValueLevel("VERY_HIGH")) used[3] += 4800;
        if (emeraldsProduced > 140000) { used[2] += 32000; used[0] += 32000; }
        else if (emeraldsProduced > 70000) {
            if (capped(storage, "ORE", "current") >= capped(storage, "CROPS", "current")) { used[2] += 8000; used[0] += 32000; }
            else { used[2] += 32000; used[0] += 8000; }
        } else if (emeraldsProduced > 30000) { used[2] += 8000; used[0] += 8000; }
        int cap = 0;
        for (String resource : List.of("ORE", "WOOD", "CROPS", "FISH")) cap = Math.max(cap, capped(storage, resource, "max"));
        if (headquarters) cap /= 5;
        Map<Integer,Integer> costs = Map.of(300,0,600,200,1200,400,2400,1000,4500,2500,10200,8000,24000,24000);
        if (!costs.containsKey(cap)) return null;
        used[4] = costs.get(cap) * 2;
        return used;
    }

    private static int uniqueConnections(Object info, int depth) {
        try {
            String ownPrefix = string(invoke(info, "getGuildPrefix"));
            Map<String,Object> infos = new HashMap<>();
            for (Object poi : territoryPois()) infos.put(string(invoke(poi, "getName")), invoke(poi, "getTerritoryInfo"));
            java.util.Set<Object> seen = new java.util.HashSet<>();
            java.util.Set<Object> friendly = new java.util.HashSet<>();
            seen.add(info); friendly.add(info);
            for (int i = 0; i < depth; i++) {
                for (Object current : List.copyOf(seen)) {
                    Object routes = invoke(current, "getTradingRoutes");
                    if (!(routes instanceof Iterable<?> values)) continue;
                    for (Object route : values) {
                        Object next = infos.get(String.valueOf(route));
                        if (next != null && seen.add(next) && ownPrefix.equals(string(invoke(next, "getGuildPrefix")))) friendly.add(next);
                    }
                }
            }
            return Math.max(0, friendly.size() - 1);
        } catch (Throwable ignored) { return 0; }
    }

    private static int resourceValueLevel(String name) {
        try {
            Class<? extends Enum> type = WynntilsCompat.requireClass(
                    "com.wynntils.models.territories.type.GuildResourceValues").asSubclass(Enum.class);
            Object value = Enum.valueOf(type, name);
            return number(type.getMethod("getLevel").invoke(value));
        } catch (Throwable ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static Map<?, ?> asMap(Object value) { return value instanceof Map<?,?> map ? map : Map.of(); }
    private static Integer mapNumber(Map<?,?> map, String key) {
        for (var entry : map.entrySet()) if (entry.getKey() instanceof Enum<?> value && value.name().equals(key)) {
            return entry.getValue() instanceof Number number ? number.intValue() : null;
        }
        return null;
    }
    private static int capped(Map<?,?> map, String key, String method) {
        for (var entry : map.entrySet()) if (entry.getKey() instanceof Enum<?> value && value.name().equals(key)) {
            return number(invoke(entry.getValue(), method));
        }
        return 0;
    }
    private static int valueOrZero(Integer value) { return value == null ? 0 : value; }
    private static String format(String pattern, double value) { return value < 0 ? "(???)" : String.format(pattern, value); }
    private static String tier(int tier) { return tier < 0 ? "" : Formatting.GRAY + " (" + tier + ")"; }

    private static Object invoke(Object target, String name, Object... args) {
        if (target == null) return null;
        try {
            for (var method : target.getClass().getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) return method.invoke(target, args);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private static int number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
}
