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
    public int amplifierTier4 = 0;

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

    // ===== Wards =====
    public int totalWards = 0;

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

    public int getTotalAmplifiers() {
        return amplifierTier1 + amplifierTier2 + amplifierTier3 + amplifierTier4;
    }

    public int getTotalCrafterBags() {
        return totalBags;
    }

    public int getTotalTomesCount() {
        return totalTomes;
    }

    public int getTotalCharmsCount() {
        return totalCharms;
    }

    public Map<String, RaidSpecificLoot> perRaidData = new HashMap<>();

    public transient RaidSpecificLoot latestData = new RaidSpecificLoot();

    public RaidSpecificLoot getOrCreateRaidData(String raidName) {
        return perRaidData.computeIfAbsent(raidName, k -> new RaidSpecificLoot());
    }

    public transient RaidSpecificLoot sessionData = new RaidSpecificLoot();
    public transient Map<String, RaidSpecificLoot> sessionPerRaidData = new HashMap<>();

    public RaidSpecificLoot getOrCreateSessionRaidData(String raidName) {
        if (sessionPerRaidData == null) sessionPerRaidData = new HashMap<>();
        return sessionPerRaidData.computeIfAbsent(raidName, k -> new RaidSpecificLoot());
    }

    public void initSession() {
        if (sessionData == null) sessionData = new RaidSpecificLoot();
        if (sessionPerRaidData == null) sessionPerRaidData = new HashMap<>();
        if (latestData == null) latestData = new RaidSpecificLoot();
    }

    public void resetSession() {
        sessionData = new RaidSpecificLoot();
        sessionPerRaidData = new HashMap<>();
        latestData = new RaidSpecificLoot();
    }

    public void resetAll() {
        emeraldBlocks = 0;
        liquidEmeralds = 0;
        amplifierTier1 = 0;
        amplifierTier2 = 0;
        amplifierTier3 = 0;
        amplifierTier4 = 0;
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
        totalWards = 0;
        perRaidData = new HashMap<>();
        resetSession();
    }

    public void resetRaid(String raidName) {
        perRaidData.remove(raidName);
        if (sessionPerRaidData != null) sessionPerRaidData.remove(raidName);
    }

    public static RaidLootData.RaidSpecificLoot createAggregateData(RaidLootData data) {
        RaidLootData.RaidSpecificLoot agg = new RaidLootData.RaidSpecificLoot();
        agg.emeraldBlocks = data.emeraldBlocks;
        agg.liquidEmeralds = data.liquidEmeralds;
        agg.amplifierTier1 = data.amplifierTier1;
        agg.amplifierTier2 = data.amplifierTier2;
        agg.amplifierTier3 = data.amplifierTier3;
        agg.amplifierTier4 = data.amplifierTier4;
        agg.totalBags = data.totalBags;
        agg.stuffedBags = data.stuffedBags;
        agg.packedBags = data.packedBags;
        agg.variedBags = data.variedBags;
        agg.totalTomes = data.totalTomes;
        agg.mythicTomes = data.mythicTomes;
        agg.fabledTomes = data.fabledTomes;
        agg.totalCharms = data.totalCharms;
        agg.totalWards = data.totalWards;
        agg.mythicAspects = data.mythicAspects;
        agg.fabledAspects = data.fabledAspects;
        agg.legendaryAspects = data.legendaryAspects;
        return agg;
    }

    public static class RaidSpecificLoot {
        public long emeraldBlocks = 0;
        public long liquidEmeralds = 0;
        public int amplifierTier1 = 0;
        public int amplifierTier2 = 0;
        public int amplifierTier3 = 0;
        public int amplifierTier4 = 0;
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
        public int totalWards = 0;
        public int completionCount = 0;

        public long getTotalLiquidEmeralds() {
            return liquidEmeralds + (emeraldBlocks * 64);
        }

        public int getTotalAmplifiers() {
            return amplifierTier1 + amplifierTier2 + amplifierTier3 + amplifierTier4;
        }

        /** Adds every counter from {@code other} into this instance (in-place). */
        public void mergeFrom(RaidSpecificLoot other) {
            if (other == null) return;
            this.emeraldBlocks      += other.emeraldBlocks;
            this.liquidEmeralds     += other.liquidEmeralds;
            this.amplifierTier1     += other.amplifierTier1;
            this.amplifierTier2     += other.amplifierTier2;
            this.amplifierTier3     += other.amplifierTier3;
            this.amplifierTier4     += other.amplifierTier4;
            this.totalBags          += other.totalBags;
            this.stuffedBags        += other.stuffedBags;
            this.packedBags         += other.packedBags;
            this.variedBags         += other.variedBags;
            this.totalTomes         += other.totalTomes;
            this.mythicTomes        += other.mythicTomes;
            this.fabledTomes        += other.fabledTomes;
            this.totalCharms        += other.totalCharms;
            this.totalAspects       += other.totalAspects;
            this.mythicAspects      += other.mythicAspects;
            this.fabledAspects      += other.fabledAspects;
            this.legendaryAspects   += other.legendaryAspects;
            this.totalWards         += other.totalWards;
            this.completionCount    += other.completionCount;
        }
    }
}