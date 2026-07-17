package julianh06.wynnextras.features.wci.ui;

import julianh06.wynnextras.features.wci.model.RequirementType;
import julianh06.wynnextras.features.wci.service.WciHaveCount;

import java.util.ArrayList;
import java.util.List;

public record WciRowDetail(
        String displayNameWithTier,
        String typeLabel,
        int have,
        int need,
        int inventory,
        int accountBank,
        int characterBank,
        int miscBucket,
        boolean bankCacheAvailable,
        boolean bankCachePossiblyIncomplete,
        String tradeMarketQuery) {
    public WciRowDetail {
        displayNameWithTier = displayNameWithTier == null ? "" : displayNameWithTier;
        typeLabel = typeLabel == null ? "" : typeLabel;
        have = Math.max(0, have);
        need = Math.max(0, need);
        inventory = Math.max(0, inventory);
        accountBank = Math.max(0, accountBank);
        characterBank = Math.max(0, characterBank);
        miscBucket = Math.max(0, miscBucket);
        tradeMarketQuery = tradeMarketQuery == null ? "" : tradeMarketQuery;
    }

    public static WciRowDetail from(WciShoppingListFormatter.Row row, WciHaveCount count) {
        WciHaveCount safeCount = count == null ? new WciHaveCount(0, 0, 0, false, false) : count;
        return new WciRowDetail(
                row.displayNameWithTier(),
                row.type() == RequirementType.MATERIAL ? "Material" : "Ingredient",
                safeCount.total(),
                row.needCount(),
                safeCount.inventory(),
                safeCount.accountBank(),
                safeCount.characterBank(),
                safeCount.miscBucket(),
                safeCount.bankCacheAvailable(),
                safeCount.bankCachePossiblyIncomplete(),
                row.tradeMarketQuery());
    }

    public List<String> tooltipLines() {
        return tooltipLines(WciMenuRenderPolicy.WciRowPrimaryAction.TRADE_MARKET_SEARCH);
    }

    public List<String> tooltipLines(WciMenuRenderPolicy.WciRowPrimaryAction primaryAction) {
        List<String> lines = new ArrayList<>();
        lines.add(displayNameWithTier);
        lines.add("Type: " + typeLabel);
        lines.add("Have: " + have + " / " + need);
        lines.add("Have stacks: " + WciQuantityFormatter.formatStacks(have));
        lines.add("Need stacks: " + WciQuantityFormatter.formatStacks(need));
        lines.add("Inventory: " + inventory);
        lines.add("Account Bank: " + accountBank);
        lines.add("Character Bank: " + characterBank);
        lines.add("Misc Bucket: " + miscBucket);
        String cacheStatus = bankCacheStatusLine();
        if (cacheStatus != null) {
            lines.add(cacheStatus);
        }
        lines.add(switch (primaryAction) {
            case TRADE_MARKET_SEARCH -> "Left-click: Trade Market";
            case COPY_ONLY -> "Left-click: Copy";
            case NONE -> "Left-click: Unavailable";
        });
        lines.add("Shift/Middle-click: Copy");
        return List.copyOf(lines);
    }

    public String bankCacheStatusLine() {
        if (!bankCacheAvailable) {
            return "Bank cache unavailable.";
        }
        if (bankCachePossiblyIncomplete) {
            return "Bank cache may be incomplete.";
        }
        return null;
    }
}
