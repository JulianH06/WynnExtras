package julianh06.wynnextras.mixin;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryInfo;
import com.wynntils.models.territories.type.GuildResource;
import com.wynntils.models.territories.type.GuildResourceValues;
import com.wynntils.utils.type.CappedValue;
import julianh06.wynnextras.duckInterfaces.Estimation;
import julianh06.wynnextras.duckInterfaces.TerritoryInfoMixinDuck;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(value = TerritoryInfo.class, remap = false)
public class TerritoryInfoMixin implements TerritoryInfoMixinDuck {
    @Unique private static int ThisIsStupid = 60;

    @Shadow private String guildName;
    @Shadow private String guildPrefix;
    @Shadow private GuildResourceValues defences;
    @Final @Shadow private boolean headquarters;
    @Shadow private final List<String> tradingRoutes = new ArrayList();
    @Shadow private final HashMap<GuildResource, Integer> generators = new HashMap();
    @Shadow private final HashMap<GuildResource, CappedValue> storage = new HashMap();

    @Unique
    private int getUniqueConnections(Integer depth) {
        Set<TerritoryInfo> connections = new HashSet<>();
        Set<TerritoryInfo> friendlyConnections = new HashSet<>();
        connections.add((TerritoryInfo) (Object) this);
        friendlyConnections.add((TerritoryInfo) (Object) this);
        for (int i = 0; i < depth; i++) {
            // Yes I turn it into an array to lazy-dodge concurrent modifications
            for (TerritoryInfo territoryInfo : connections.toArray(new TerritoryInfo[0])) {
                for (String route : territoryInfo.getTradingRoutes()) {
                    TerritoryInfo routeInfo = Models.Territory.getTerritoryPoisFromAdvancement().stream()
                            .filter(territoryPoi -> territoryPoi.getName().equals(route))
                            .findFirst()
                            .orElseThrow()
                            .getTerritoryInfo();
                    connections.add(routeInfo);
                    if (routeInfo.getGuildPrefix().equals(guildPrefix)) {
                        friendlyConnections.add(routeInfo);
                    }
                }
            }
        }
        return friendlyConnections.size() - 1; // Do not include self
    }

    @Unique
    private Map<GuildResource, Integer> estimateUsedResources(Integer producedEmeralds) {
        final Map<Integer, Integer> storageToCost = new HashMap<>();
        storageToCost.put(300, 0);
        storageToCost.put(600, 200);
        storageToCost.put(1200, 400);
        storageToCost.put(2400, 1000);
        storageToCost.put(4500, 2500);
        storageToCost.put(10200, 8000);
        storageToCost.put(24000, 24000);
        int ore = 0, crops = 0, wood = 0, fish = 0, emeralds = 0;
        if (defences.getLevel() >= GuildResourceValues.MEDIUM.getLevel()) {
            // assume vol and aur
            crops += 800;
            ore += 200;
            if (defences.getLevel() >= GuildResourceValues.HIGH.getLevel()) {
                // +1 vol, +2 mob
                ore += 200;
                wood += 400;
                if (defences.getLevel() >= GuildResourceValues.VERY_HIGH.getLevel()) {
                    // +multihit
                    fish += 4800;
                }
            }
        }
        if (producedEmeralds > 140000) {
            // either 3 3 or a very cursed 3 2 / 2 3 on a double territory, assume 3 3
            crops += 32000;
            ore += 32000;
        } else if (producedEmeralds > 70000) {
            // thanks ANO on maro peaks
            int storedCrops =
                    storage.getOrDefault(GuildResource.CROPS, CappedValue.EMPTY).current();
            int storedOres =
                    storage.getOrDefault(GuildResource.ORE, CappedValue.EMPTY).current();
            if (storedOres >= storedCrops) {
                crops += 8000;
                ore += 32000;
            } else {
                // probably lol
                crops += 32000;
                ore += 8000;
            }
        } else if (producedEmeralds > 30000) {
            // I give up, good enough
            crops += 8000;
            ore += 8000;
        }
        // if someone is doing 1 1 they're trolling themselves
        int resourceCap = 0;
        int emeraldCap =
                storage.getOrDefault(GuildResource.EMERALDS, CappedValue.EMPTY).max();
        for (GuildResource resource : GuildResource.values()) {
            if (resource != GuildResource.EMERALDS) {
                resourceCap = Math.max(
                        storage.getOrDefault(resource, CappedValue.EMPTY).max(), resourceCap);
            }
        }
        if (headquarters) resourceCap /= 5;
        if (storageToCost.get(resourceCap) == null) {
            return null;
        }
        emeralds += storageToCost.get(resourceCap) * 2;
        // emeralds can be at 0 if we're unlucky, just deal with it
        wood += storageToCost.getOrDefault(emeraldCap / 10, 0);
        Map<GuildResource, Integer> result = new HashMap<>();
        result.put(GuildResource.ORE, ore);
        result.put(GuildResource.CROPS, crops);
        result.put(GuildResource.WOOD, wood);
        result.put(GuildResource.FISH, fish);
        result.put(GuildResource.EMERALDS, emeralds); // neither used nor complete (resource prod boost)
        return result;
    }

    @Unique
    private Map<GuildResource, Estimation> estimateTowerStats(
            Map<GuildResource, Integer> usedResources, Integer predictedResourceTime) {
        final double[] damageBonuses = new double[] {1.4, 1.8, 2.2, 2.6, 3, 3.4, 3.8, 4.2, 4.6, 5, 5.4};
        final double[] healthBonuses = new double[] {1.5, 2, 2.5, 3.2, 4, 5, 6.2, 7.4, 8.6, 9.8, 11};
        final double[] speedBonuses = new double[] {1.5, 2, 2.5, 3.2, 4, 5, 6, 7.2, 7.6, 8.4, 9.4};
        final double[] defenceBonuses = new double[] {4, 5.5, 6.25, 7, 7.5, 7.9, 8.2, 8.4, 8.6, 8.8, 9};
        final int[] costs = new int[] {100, 300, 600, 1200, 2400, 4800, 8400, 12000, 15600, 19200, 22800};
        final Map<GuildResource, double[]> resourceToDefence = new HashMap<>();
        resourceToDefence.put(GuildResource.ORE, damageBonuses);
        resourceToDefence.put(GuildResource.WOOD, healthBonuses);
        resourceToDefence.put(GuildResource.CROPS, speedBonuses);
        resourceToDefence.put(GuildResource.FISH, defenceBonuses);
        Map<GuildResource, Estimation> result = new HashMap<>();
        result.put(GuildResource.ORE, new Estimation(1000d, 0));
        result.put(GuildResource.WOOD, new Estimation(300000d, 0));
        result.put(GuildResource.CROPS, new Estimation(0.5d, 0));
        result.put(GuildResource.FISH, new Estimation(10d, 0));
        int secondsPassed = 60 - predictedResourceTime;
        double multiplication = 60d / secondsPassed;
        for (GuildResource resource : result.keySet()) {
            double stored = storage.getOrDefault(resource, CappedValue.EMPTY).current();
            // Account for generated resources (note: quadractic nightmare, += and -= valid)
            int produced = generators.getOrDefault(resource, 0);
            if (produced > 0 && defences.getLevel() <= GuildResourceValues.MEDIUM.getLevel()
                    || produced > 24000 && !headquarters) {
                // Product is likely self-sufficient, do not estimate
                result.put(resource, new Estimation(-1d, -1));
                continue;
            }
            stored -= (produced / 3600d * secondsPassed);
            if (stored <= 0) {
                // Unlucky timing, better luck next time
                result.put(resource, new Estimation(-1d, -1));
                continue;
            }
            double storageAtStart = stored * multiplication;
            // Account for what we assume has been spent
            storageAtStart -= (usedResources.get(resource) / 60d);
            // Find the closest defence price
            int bestPrediction = 0;
            for (int i = 0; i < costs.length; i++) {
                double costPerMinute = costs[i] / 60d;
                if (costPerMinute > storageAtStart) {
                    if (i > 0) {
                        if (Math.abs(storageAtStart - costPerMinute) < Math.abs(storageAtStart - costs[i - 1] / 60d)) {
                            bestPrediction = i; // prefer the closer one
                        }
                    }
                    break;
                } else {
                    bestPrediction = i;
                }
            }
            result.put(
                    resource,
                    new Estimation(
                            result.get(resource).value() * resourceToDefence.get(resource)[bestPrediction],
                            bestPrediction + 1));
        }
        return result;
    }

    @Unique
    public List<String> wynnextras$getEstimatedDefences() {
        if (defences.getLevel() <= GuildResourceValues.VERY_LOW.getLevel()) {
            return null;
        }
        if (generators.size() > 3) { // > 3 because Maltic Coast
            return List.of(new String[] {
                    Formatting.RED + "Unknown " + Formatting.GRAY + "(Rainbow territory)",
            });
        }
        // == Predict the current resource timing == //
        Integer producedEmeralds = generators.get(GuildResource.EMERALDS);
        if (producedEmeralds == null) {
            return List.of(new String[] {
                    Formatting.RED + "Unknown " + Formatting.GRAY + "(Missing generation info)",
            });
        }
        int currentEmeralds =
                storage.getOrDefault(GuildResource.EMERALDS, CappedValue.EMPTY).current();
        // 0 means we have all resources, 60 means we used them all
        int predictedResourceTime = Math.clamp(Math.round(currentEmeralds / (producedEmeralds / 60d) * 60), 0, 60);
        boolean usingFallbackTime = false;
        if (predictedResourceTime == 60) {
            predictedResourceTime = ThisIsStupid;
            usingFallbackTime = true;
        }
        ThisIsStupid = predictedResourceTime;
        // == Resolve connection boost == //
        double connectionBoost = 1 + 0.3 * getUniqueConnections(1);
        if (headquarters) connectionBoost *= (1.5 + 0.25 * getUniqueConnections(3));
        // == Get used resources == //
        // Assume 1 tier of Aura and Volley is in play, since it's Medium+
        Map<GuildResource, Integer> usedResources = estimateUsedResources(producedEmeralds);
        if (usedResources == null) {
            return List.of(new String[] {
                    Formatting.RED + "Unknown " + Formatting.GRAY + "(Invalid storage info)",
            });
        }
        // == Guess defences == //
        Map<GuildResource, Estimation> estimatedStats = estimateTowerStats(usedResources, predictedResourceTime);
        // == Return result == //
        List<String> result = new ArrayList<>();
        if (usingFallbackTime) result.add(Formatting.YELLOW + "Warning: using fallback time");
        Estimation hp = estimatedStats.get(GuildResource.WOOD);
        Estimation dmg = estimatedStats.get(GuildResource.ORE);
        Estimation speed = estimatedStats.get(GuildResource.CROPS);
        Estimation defence = estimatedStats.get(GuildResource.FISH);
        double dmgv = dmg.value() * connectionBoost;
        BiFunction<String, Double, String> format = (fmt, n) -> {
            return (n < 0) ? "(???)" : String.format(fmt, n);
        };
        Function<Estimation, String> tier = (est) -> {
            return est.tier() < 0 ? "" : Formatting.GRAY + " (" + est.tier() + ")";
        };
        String connectionText = Formatting.DARK_GRAY + " (x" + String.format("%.2f", connectionBoost) + ")";
        result.add(Formatting.WHITE + "Ⓑ " + format.apply("%.0f", dmgv) + "-" + format.apply("%.0f", dmgv * 1.5)
                + " Damage" + tier.apply(dmg) + connectionText);
        result.add(
                Formatting.YELLOW + "Ⓙ " + format.apply("%.2f", speed.value()) + " Attacks/s" + tier.apply(speed));
        result.add(Formatting.GOLD + "Ⓒ " + format.apply("%.0f", hp.value() * connectionBoost / 1000) + "k HP"
                + tier.apply(hp) + connectionText);
        result.add(
                Formatting.AQUA + "Ⓚ " + format.apply("%.0f", defence.value()) + "% Defence" + tier.apply(defence));
        result.add(Formatting.DARK_GRAY + "Next resource move prediction: " + (60 - predictedResourceTime) + "s");
        return result;
    }
}


;
