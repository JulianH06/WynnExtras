package julianh06.wynnextras.features.crafting.calc;

/**
 * Static utility class for crafting XP calculations.
 * Formulas ported from wynnextrasbuilder crafting calculator.
 */
public final class CraftXpCalculator {

    private CraftXpCalculator() {}

    // Ingredient tier XP multipliers (T0=no ing, T1, T2, T3)
    public static final double[] ING_TIER_XP_MULT = {1.0, 1.25, 1.75, 2.5};

    // Material tier XP multipliers (T0=no mat, T1, T2, T3)
    public static final double[] MAT_TIER_XP_MULT = {0, 1.0, 2.0, 4.0};

    public enum MaterialType {
        SKY(110, 103),
        DERNIC(113, 105);

        public final int decayStartLevel;
        public final int recipeLevel;

        MaterialType(int decayStartLevel, int recipeLevel) {
            this.decayStartLevel = decayStartLevel;
            this.recipeLevel = recipeLevel;
        }
    }

    // Cumulative effective XP tables from prof calculator spreadsheet.
    // Indexed by level (99-132). Formula: crafts = ceil((cumul[to] - cumul[from]) / (ingBase * 0.6 * matMult * bonus))
    // These are NOT raw XP - they factor in decay schedules for accurate craft estimates.
    private static final double[] CUMUL_EFFXP_DERNIC = {
            /* 99 */ 10617864,    /* 100 */ 11839054.2,  /* 101 */ 13200682.2,  /* 102 */ 14718898.8,
            /* 103 */ 16411711.2, /* 104 */ 18299198.4,  /* 105 */ 20403747.6,  /* 106 */ 22750321.2,
            /* 107 */ 25366752,   /* 108 */ 28284073.8,  /* 109 */ 31536888.6,  /* 110 */ 35163778.2,
            /* 111 */ 39207761.4, /* 112 */ 43716804,    /* 113 */ 48744387.6,  /* 114 */ 54583717.6,
            /* 115 */ 61377652.6, /* 116 */ 69297220.33, /* 117 */ 78548031.04, /* 118 */ 89378418.79,
            /* 119 */ 102089875.9,/* 120 */ 117050556.7, /* 121 */ 134712961.4, /* 122 */ 155637394.2,
            /* 123 */ 180523521.2,/* 124 */ 208271555.2, /* 125 */ 239210614.2, /* 126 */ 273707667.2,
            /* 127 */ 312171883.2,/* 128 */ 355059486.2, /* 129 */ 402879166.2, /* 130 */ 456198111.2,
            /* 131 */ 515648736.2,/* 132 */ 581936185.2,
    };

    private static final double[] CUMUL_EFFXP_SKY = {
            /* 99 */ 6370718.4,   /* 100 */ 7591908.6,   /* 101 */ 8953536.6,   /* 102 */ 10471753.2,
            /* 103 */ 12164565.6, /* 104 */ 14052052.8,  /* 105 */ 16156602,    /* 106 */ 18503175.6,
            /* 107 */ 21119606.4, /* 108 */ 24036928.2,  /* 109 */ 27289743,    /* 110 */ 30916632.6,
            /* 111 */ 35129115.1, /* 112 */ 40030248.36, /* 113 */ 45743411.54, /* 114 */ 52416931.54,
            /* 115 */ 60229956.79,/* 116 */ 69399982.58, /* 117 */ 80192595.08, /* 118 */ 92934227.73,
            /* 119 */ 108029083,  /* 120 */ 125981900,   /* 121 */ 145999292,   /* 122 */ 168318687,
            /* 123 */ 193204814,  /* 124 */ 220952848,   /* 125 */ 251891907,   /* 126 */ 286388960,
            /* 127 */ 324853176,  /* 128 */ 367740779,   /* 129 */ 415560459,   /* 130 */ 468879404,
            /* 131 */ 528330029,  /* 132 */ 594617478,
    };

    /**
     * Compute the base XP per craft for 6 ingredient slots at a given level and tier.
     * Formula: 6 * (level * 648.5 + 19700) / 7.2 * ING_MULT[tier]
     */
    public static double computeIngBaseFullTier(int recipeLevel, int ingTier) {
        double base = 6.0 * (recipeLevel * 648.5 + 19700.0) / 7.2;
        return base * ING_TIER_XP_MULT[ingTier];
    }

    /**
     * Compute the weighted material multiplier from two material tiers and their amounts.
     * matMult = (MAT_XP[tier1] * ratio1 + MAT_XP[tier2] * ratio2) / (ratio1 + ratio2)
     */
    public static double computeMatMult(int matTier1, int matTier2, int ratio1, int ratio2) {
        return (MAT_TIER_XP_MULT[matTier1] * ratio1 + MAT_TIER_XP_MULT[matTier2] * ratio2) / (double)(ratio1 + ratio2);
    }

    /**
     * Get the XP decay factor for a given crafting level.
     * -0.04 per level past decay start, minimum 0.6.
     */
    private static double getDecayFactor(int craftingLevel, MaterialType matType) {
        if (craftingLevel <= matType.decayStartLevel) return 1.0;
        int levelsOverDecay = craftingLevel - matType.decayStartLevel;
        return Math.max(0.6, 1.0 - levelsOverDecay * 0.04);
    }

    /**
     * Compute XP per craft: ingBase * decay * matMult * bonusMult
     */
    public static double computeXpPerCraft(double ingBase, double matMult, double bonusMult, int craftingLevel, MaterialType matType) {
        double decay = getDecayFactor(craftingLevel, matType);
        return ingBase * decay * matMult * bonusMult;
    }

    /**
     * Estimate number of crafts needed to go from one level to another.
     * Uses cumulative effective XP tables: crafts = ceil((cumul[to] - cumul[from]) / (ingBase * 0.6 * matMult * bonus))
     */
    public static int estimateCraftsToLevel(int fromLevel, int toLevel, double ingBase, double matMult, double bonusMult, MaterialType matType) {
        if (fromLevel >= toLevel || fromLevel < 99 || toLevel > 132) return 0;

        double[] cumul = matType == MaterialType.SKY ? CUMUL_EFFXP_SKY : CUMUL_EFFXP_DERNIC;
        double cumFrom = cumul[fromLevel - 99];
        double cumTo = cumul[toLevel - 99];
        double effXpNeeded = cumTo - cumFrom;
        if (effXpNeeded <= 0) return 0;

        double maxDecayXpPerCraft = ingBase * 0.6 * matMult * bonusMult;
        if (maxDecayXpPerCraft <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(effXpNeeded / maxDecayXpPerCraft);
    }

    /**
     * Estimate number of crafts for overflow XP at level 132.
     */
    public static int estimateCraftsForOverflow(double overflowNeeded, double ingBase, double matMult, double bonusMult, MaterialType matType) {
        if (overflowNeeded <= 0) return 0;
        double xpPerCraft = computeXpPerCraft(ingBase, matMult, bonusMult, 132, matType);
        if (xpPerCraft <= 0) return Integer.MAX_VALUE;
        return (int) Math.ceil(overflowNeeded / xpPerCraft);
    }

    public static String formatXp(double xp) {
        if (xp >= 1_000_000_000) return String.format("%.1fB", xp / 1_000_000_000);
        if (xp >= 1_000_000) return String.format("%.1fM", xp / 1_000_000);
        if (xp >= 10_000) return String.format("%.1fK", xp / 1_000);
        return String.format("%.0f", xp);
    }

    public static String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
