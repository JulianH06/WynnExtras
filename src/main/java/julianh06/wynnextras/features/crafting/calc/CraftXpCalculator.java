package julianh06.wynnextras.features.crafting.calc;

/**
 * Static utility class for crafting XP calculations.
 * <p>
 * Data and formulas ported from the updated profession calculator spreadsheet
 * (built on top of orengr's original, reworked by reyzhia) which reflects the
 * post-rebalance level/material data.
 * <p>
 * XP per craft model:
 * <pre>
 *   base_xp   = matType.a * ingredientLevel + matType.b      (per material band)
 *   noDecay   = base_xp * ingTierMult * matTierMult * ratioMult * eventMult
 *   xp/craft  = noDecay * decay(profLevel, matType)
 * </pre>
 * Crafts to advance levels use the per-level decay so the total matches the
 * spreadsheet exactly (the floor-decay scaling the sheet displays cancels out).
 */
public final class CraftXpCalculator {

    private CraftXpCalculator() {}

    // Ingredient tier XP multipliers (T0=no ing, T1, T2, T3). Shared across all bands.
    public static final double[] ING_TIER_XP_MULT = {1.0, 1.25, 1.75, 2.5};

    // ── Decay model ──
    // decay = max(FLOOR, 1 - slope * (profLevel - decayStart)); slope/start are per material type.
    private static final double DECAY_FLOOR = 0.6;

    // ── Recipe ratio multipliers (fitted; normalised to high:low form) ──
    private static final double RATIO_MULT_1_1 = 0.6938718508;
    private static final double RATIO_MULT_2_1 = 1.0;
    private static final double RATIO_MULT_3_1 = 1.332391588;

    /**
     * Material types. {@code baseXp = baseA * ingredientLevel + baseB}.
     * <ul>
     *   <li>{@code SKY} is the mod's pre-rework material, kept on the old calculations
     *       (constant base independent of ingredient level, old decay, old XP table, no ratio multiplier).</li>
     *   <li>The three {@code MAT_*} entries are the new materials, named by the profession-level range
     *       they cover, using the updated spreadsheet's fitted band curves.</li>
     * </ul>
     */
    public enum MaterialType {
        //          legacy  baseA         baseB           matT2        matT3        decayStart  decaySlope
        SKY        (true,   0.0,          72079.58333333, 2.0,         4.0,         110,        0.04),
        MAT_107_110(false,  632.220688,   5207.82427,     1.990295355, 3.990295355, 121,        0.03935639787),
        MAT_110_115(false,  459.8416055,  5011.27599,     2.040171898, 4.040171898, 126,        0.03935639787),
        MAT_115_MAX(false,  549.9980003,  7055.690619,    2.665881591, 4.665881591, 131,        0.03935639787);

        /** When true, use the mod's old calculations (constant base, no fitted ratio multiplier, legacy XP table). */
        public final boolean legacy;
        public final double baseA;
        public final double baseB;
        /** Material tier XP multipliers: index [1]=T1(=1), [2]=T2, [3]=T3. Index 0 unused. */
        public final double[] matTierMult;
        public final int decayStart;
        public final double decaySlope;

        MaterialType(boolean legacy, double baseA, double baseB, double matT2, double matT3, int decayStart, double decaySlope) {
            this.legacy = legacy;
            this.baseA = baseA;
            this.baseB = baseB;
            this.matTierMult = new double[]{0.0, 1.0, matT2, matT3};
            this.decayStart = decayStart;
            this.decaySlope = decaySlope;
        }
    }

    // Legacy cumulative effective-XP table for SKY (levels 99-132), from the mod's pre-rework calculator.
    private static final double[] CUMUL_EFFXP_SKY = {
            /* 99  */ 6370718.4,   /* 100 */ 7591908.6,   /* 101 */ 8953536.6,   /* 102 */ 10471753.2,
            /* 103 */ 12164565.6,  /* 104 */ 14052052.8,  /* 105 */ 16156602,    /* 106 */ 18503175.6,
            /* 107 */ 21119606.4,  /* 108 */ 24036928.2,  /* 109 */ 27289743,    /* 110 */ 30916632.6,
            /* 111 */ 35129115.1,  /* 112 */ 40030248.36, /* 113 */ 45743411.54, /* 114 */ 52416931.54,
            /* 115 */ 60229956.79, /* 116 */ 69399982.58, /* 117 */ 80192595.08, /* 118 */ 92934227.73,
            /* 119 */ 108029083,   /* 120 */ 125981900,   /* 121 */ 145999292,   /* 122 */ 168318687,
            /* 123 */ 193204814,   /* 124 */ 220952848,   /* 125 */ 251891907,   /* 126 */ 286388960,
            /* 127 */ 324853176,   /* 128 */ 367740779,   /* 129 */ 415560459,   /* 130 */ 468879404,
            /* 131 */ 528330029,   /* 132 */ 594617478,
    };
    private static final int SKY_MIN_LEVEL = 99;

    // ── XP curve ──
    // XP required to advance FROM the previous level TO this level, indexed by level.
    private static final int MIN_LEVEL = 100;
    private static final int MAX_LEVEL = 132;
    private static final long[] XP_REQUIRED = { // index 0 = level 100, last = level 132
            2_035_317L,  2_269_380L,  2_530_361L,  2_821_354L,  3_145_812L,
            3_507_582L,  3_910_956L,  4_360_718L,  4_862_203L,  5_421_358L,
            6_044_816L,  6_739_972L,  7_515_071L,  8_379_306L,  9_342_928L,
            10_417_367L, 11_615_366L, 12_951_135L, 14_440_517L, 16_101_179L,
            17_952_817L, 20_017_392L, 22_319_395L, 24_886_127L, 27_748_034L,
            30_939_059L, 34_497_053L, 38_464_216L, 42_887_603L, 47_819_680L,
            53_318_945L, 59_450_625L, 66_287_449L,
    };

    /** Lowest level the XP curve covers. Nothing below this can be estimated. */
    public static final int CURVE_MIN_LEVEL = MIN_LEVEL;
    /** Highest profession level. */
    public static final int CURVE_MAX_LEVEL = MAX_LEVEL;

    /**
     * Raw XP needed to advance from {@code level - 1} to {@code level}, or 0 when the level sits
     * outside the covered range.
     */
    public static long xpToReachLevel(int level) {
        int index = level - MIN_LEVEL;
        if (index < 0 || index >= XP_REQUIRED.length) return 0;
        return XP_REQUIRED[index];
    }

    /**
     * Total XP still needed to reach {@code targetLevel}, given the progress already made inside
     * the current level. Returns 0 when the target is not ahead of the player or falls outside
     * the covered range.
     */
    public static long xpBetween(int currentLevel, long xpIntoCurrentLevel, long xpForCurrentLevel, int targetLevel) {
        if (targetLevel <= currentLevel || targetLevel > MAX_LEVEL) return 0;

        long total = Math.max(0, xpForCurrentLevel - xpIntoCurrentLevel);
        for (int level = currentLevel + 2; level <= targetLevel; level++) {
            total += xpToReachLevel(level);
        }
        return total;
    }

    /** Base XP for a full craft at the given material band and ingredient level. */
    public static double computeBaseXp(MaterialType matType, double ingredientLevel) {
        return matType.baseA * ingredientLevel + matType.baseB;
    }

    /** XP decay multiplier for a profession level with the given material type. */
    public static double getDecay(double profLevel, MaterialType matType) {
        if (profLevel <= matType.decayStart) return 1.0;
        return Math.max(DECAY_FLOOR, 1.0 - matType.decaySlope * (profLevel - matType.decayStart));
    }

    /** Fitted recipe-ratio multiplier, normalised so order of the two amounts doesn't matter. */
    public static double getRatioMult(int amount1, int amount2) {
        int hi = Math.max(amount1, amount2);
        int lo = Math.max(1, Math.min(amount1, amount2));
        double ratio = (double) hi / lo;
        if (ratio >= 2.5) return RATIO_MULT_3_1;
        if (ratio >= 1.5) return RATIO_MULT_2_1;
        return RATIO_MULT_1_1;
    }

    /**
     * Weighted material-tier multiplier for the two material slots, weighted by their amounts.
     */
    public static double computeMatMult(MaterialType matType, int matTier1, int matTier2, int amount1, int amount2) {
        double[] m = matType.matTierMult;
        return (m[matTier1] * amount1 + m[matTier2] * amount2) / (double) (amount1 + amount2);
    }

    /**
     * XP per craft with decay removed (independent of profession level).
     * This is the spreadsheet's "no-decay XP per craft".
     */
    public static double computeNoDecayXp(MaterialType matType, double ingredientLevel, int ingTier,
                                          int matTier1, int matTier2, int amount1, int amount2, double eventMult) {
        double base = computeBaseXp(matType, ingredientLevel);
        double matMult = computeMatMult(matType, matTier1, matTier2, amount1, amount2);
        // Legacy (SKY) had no separate fitted ratio multiplier.
        double ratioMult = matType.legacy ? 1.0 : getRatioMult(amount1, amount2);
        return base * ING_TIER_XP_MULT[ingTier] * matMult * ratioMult * eventMult;
    }

    /** XP per craft at a given profession level (no-decay value times decay). */
    public static double computeXpPerCraft(double noDecayXp, double profLevel, MaterialType matType) {
        return noDecayXp * getDecay(profLevel, matType);
    }

    /**
     * Estimate crafts to go from one level to another, applying the per-level decay.
     * Matches the spreadsheet's "estimated crafts needed".
     */
    public static int estimateCraftsToLevel(int fromLevel, int toLevel, double noDecayXp, MaterialType matType) {
        return estimateCraftsToLevel(fromLevel, toLevel, noDecayXp, matType, 0);
    }

    /**
     * Same estimate, but crediting progress already made inside the starting level.
     *
     * @param progressIntoLevel fraction of the starting level already earned, 0 to 1
     */
    public static int estimateCraftsToLevel(int fromLevel, int toLevel, double noDecayXp, MaterialType matType,
                                            double progressIntoLevel) {
        if (fromLevel >= toLevel) return 0;
        if (noDecayXp <= 0) return Integer.MAX_VALUE;

        double progress = Math.max(0, Math.min(1, progressIntoLevel));

        if (matType.legacy) {
            // Old approach: effective XP from a hardcoded table divided by floor-decay XP per craft.
            int from = Math.max(SKY_MIN_LEVEL, Math.min(MAX_LEVEL, fromLevel));
            int to = Math.max(SKY_MIN_LEVEL, Math.min(MAX_LEVEL, toLevel));
            double effXpNeeded = CUMUL_EFFXP_SKY[to - SKY_MIN_LEVEL] - CUMUL_EFFXP_SKY[from - SKY_MIN_LEVEL];
            if (from + 1 <= MAX_LEVEL) {
                double firstLevelEffXp = CUMUL_EFFXP_SKY[from + 1 - SKY_MIN_LEVEL] - CUMUL_EFFXP_SKY[from - SKY_MIN_LEVEL];
                effXpNeeded -= firstLevelEffXp * progress;
            }
            if (effXpNeeded <= 0) return 0;
            return (int) Math.ceil(effXpNeeded / (noDecayXp * DECAY_FLOOR));
        }

        double crafts = 0;
        int start = Math.max(MIN_LEVEL, fromLevel + 1);
        int end = Math.min(MAX_LEVEL, toLevel);
        for (int level = start; level <= end; level++) {
            double xpPerCraft = noDecayXp * getDecay(level, matType);
            if (xpPerCraft <= 0) return Integer.MAX_VALUE;
            double xpNeeded = XP_REQUIRED[level - MIN_LEVEL];
            if (level == start) xpNeeded *= (1 - progress);   // already part-way through this one
            crafts += xpNeeded / xpPerCraft;
        }
        return (int) Math.ceil(crafts);
    }

    /** Estimate crafts to earn a given amount of overflow XP at max level. */
    public static int estimateCraftsForOverflow(double overflowNeeded, double noDecayXp, MaterialType matType) {
        if (overflowNeeded <= 0) return 0;
        double xpPerCraft = computeXpPerCraft(noDecayXp, MAX_LEVEL, matType);
        if (xpPerCraft <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(overflowNeeded / xpPerCraft);
    }

    public static String formatXp(double xp) {
        if (xp >= 1_000_000_000) return String.format("%.1fB", xp / 1_000_000_000);
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000);
        if (xp >= 10_000) return String.format("%.1fK", xp / 1_000);
        return String.format("%.0f", xp);
    }

    public static long estimateProfessionXp(int level, int xpPercent) {
        if (level < MIN_LEVEL) return 0;

        long xp = 0;
        int cappedLevel = Math.min(level, MAX_LEVEL);
        for (int currentLevel = MIN_LEVEL + 1; currentLevel <= cappedLevel; currentLevel++) {
            xp += XP_REQUIRED[currentLevel - MIN_LEVEL];
        }

        if (cappedLevel >= MAX_LEVEL) {
            xp += estimateProfessionOverflowXp(xpPercent);
        } else {
            long currentLevelXp = XP_REQUIRED[cappedLevel + 1 - MIN_LEVEL];
            xp += currentLevelXp * Math.max(0, xpPercent) / 100L;
        }
        return xp;
    }

    public static long estimateProfessionOverflowXp(int xpPercent) {
        return XP_REQUIRED[MAX_LEVEL - MIN_LEVEL] * Math.max(0, xpPercent) / 100L;
    }

    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
