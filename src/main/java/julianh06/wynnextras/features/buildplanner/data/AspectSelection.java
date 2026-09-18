package julianh06.wynnextras.features.buildplanner.data;

public record AspectSelection(WynnAspect aspect, int tier) {
    public AspectSelection {
        if (aspect == null) {
            throw new IllegalArgumentException("Aspect cannot be null");
        }
        tier = Math.max(1, Math.min(aspect.tiers().size(), tier));
    }

    public WynnAspect.Tier selectedTier() {
        return aspect.tier(tier);
    }
}
