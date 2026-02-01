package julianh06.wynnextras.features.raid;

import java.util.HashMap;
import java.util.Map;

public class RaidLootData {

    // ===== Emeralds (roh) =====
    public long emeraldBlocks = 0;
    public long liquidEmeralds = 0;

    // ===== Amplifiers =====
    public int amplifierTier1 = 0;
    public int amplifierTier2 = 0;
    public int amplifierTier3 = 0;

    // ===== Crafter Bags =====
    public int totalBags = 0;
    public int stuffedBags = 0;
    public int packedBags = 0;
    public int variedBags = 0;

    // ===== Tomes =====
    public int totalTomes = 0;
    public int mythicTomes = 0;
    public int fabledTomes = 0;

    // ===== Charms =====
    public int totalCharms = 0;

    // ===== Aspects =====
    public int totalAspects = 0;
    public int mythicAspects = 0;
    public int fabledAspects = 0;
    public int legendaryAspects = 0;
    public int rareAspects = 0;

    /* =========================
       Emerald Normalisierung
       ========================= */
    public long getTotalLiquidEmeralds() {
        return liquidEmeralds + (emeraldBlocks / 64);
    }

    public long getRemainingEmeraldBlocks() {
        return emeraldBlocks % 64;
    }

    public long getStacks() {
        return getTotalLiquidEmeralds() / 64;
    }

    public long getRemainingLiquidEmeralds() {
        return getTotalLiquidEmeralds() % 64;
    }

    /* =========================
       Amplifier Totals
       ========================= */
    public int getTotalAmplifiers() {
        return amplifierTier1 + amplifierTier2 + amplifierTier3;
    }

    /* =========================
       Crafter Bag Totals
       ========================= */
    public int getTotalCrafterBags() {
        return totalBags;
    }

    /* =========================
       Tome Totals
       ========================= */
    public int getTotalTomesCount() {
        return totalTomes;
    }

    /* =========================
       Charms Totals
       ========================= */
    public int getTotalCharmsCount() {
        return totalCharms;
    }

    /* =========================
       Per-Raid Tracking
       ========================= */
    public Map<String, RaidSpecificLoot> perRaidData = new HashMap<>();

    public RaidSpecificLoot getOrCreateRaidData(String raidName) {
        return perRaidData.computeIfAbsent(raidName, k -> new RaidSpecificLoot());
    }

    /* =========================
       Session Tracking (transient - not saved)
       ========================= */
    public transient RaidSpecificLoot sessionData = new RaidSpecificLoot();
    public transient Map<String, RaidSpecificLoot> sessionPerRaidData = new HashMap<>();

    public RaidSpecificLoot getOrCreateSessionRaidData(String raidName) {
        if (sessionPerRaidData == null) sessionPerRaidData = new HashMap<>();
        return sessionPerRaidData.computeIfAbsent(raidName, k -> new RaidSpecificLoot());
    }

    public void initSession() {
        if (sessionData == null) sessionData = new RaidSpecificLoot();
        if (sessionPerRaidData == null) sessionPerRaidData = new HashMap<>();
    }

    public void resetSession() {
        sessionData = new RaidSpecificLoot();
        sessionPerRaidData = new HashMap<>();
    }

    public void resetAll() {
        emeraldBlocks = 0;
        liquidEmeralds = 0;
        amplifierTier1 = 0;
        amplifierTier2 = 0;
        amplifierTier3 = 0;
        totalBags = 0;
        stuffedBags = 0;
        packedBags = 0;
        variedBags = 0;
        totalTomes = 0;
        mythicTomes = 0;
        fabledTomes = 0;
        totalCharms = 0;
        totalAspects = 0;
        mythicAspects = 0;
        fabledAspects = 0;
        legendaryAspects = 0;
        rareAspects = 0;
        perRaidData = new HashMap<>();
        resetSession();
    }

    public void resetRaid(String raidName) {
        perRaidData.remove(raidName);
        if (sessionPerRaidData != null) sessionPerRaidData.remove(raidName);
    }

    public static class RaidSpecificLoot {
        public long emeraldBlocks = 0;
        public long liquidEmeralds = 0;
        public int amplifierTier1 = 0;
        public int amplifierTier2 = 0;
        public int amplifierTier3 = 0;
        public int totalBags = 0;
        public int stuffedBags = 0;
        public int packedBags = 0;
        public int variedBags = 0;
        public int totalTomes = 0;
        public int mythicTomes = 0;
        public int fabledTomes = 0;
        public int totalCharms = 0;
        public int totalAspects = 0;
        public int mythicAspects = 0;
        public int fabledAspects = 0;
        public int legendaryAspects = 0;
        public int rareAspects = 0;
        public int completionCount = 0;

        public long getTotalLiquidEmeralds() {
            return liquidEmeralds + (emeraldBlocks * 64);
        }

        public int getTotalAmplifiers() {
            return amplifierTier1 + amplifierTier2 + amplifierTier3;
        }
    }
}
