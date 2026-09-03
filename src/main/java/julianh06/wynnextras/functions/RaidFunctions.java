package julianh06.wynnextras.functions;

import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import net.minecraft.client.resource.language.I18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class RaidFunctions {
    private enum TYPE {
        AMPLIFIER1,
        AMPLIFIER2,
        AMPLIFIER3,
        AMPLIFIER4,
        TOTAL_AMPLIFIER,
        CHARMS,
        EMERALDS,
        FABLED_ASPECTS,
        FABLED_TOMES,
        LEGENDARY_ASPECTS,
        MYTHIC_ASPECTS,
        MYTHIC_TOMES,
        PACKED_BAGS,
        STUFFED_BAGS,
        TOTAL_ASPECTS,
        TOTAL_BAGS,
        TOTAL_TOMES,
        VARIED_BAGS,
        WARDS;

        static TYPE fromString(String name){
            try {
                return valueOf(name.toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
    private enum RAID {
        ALL,
        NOTG,
        NOL,
        TCC,
        TNA,
        TWP;

        static RAID fromString(String name){
            try {
                return valueOf(name.toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException e) {
                return ALL;
            }
        }
    }
    private enum MODE{
        ALL,
        SESSION,
        LATEST;

        static MODE fromString(String name){
            try {
                return valueOf(name.toUpperCase(Locale.ROOT).replace(' ', '_'));
            } catch (IllegalArgumentException e) {
                return ALL;
            }
        }
    }

    public static long getRaidDrop(String raidName, String modeName, String typeName) {
            RAID raid = RAID.fromString(raidName);
            MODE mode = MODE.fromString(modeName);
            TYPE type = TYPE.fromString(typeName);
            RaidLootData data = RaidLootConfig.INSTANCE.data;
            data.initSession();
            RaidLootData.RaidSpecificLoot selectedData;

            if (mode == MODE.LATEST){
                selectedData = data.latestData;
            } else {
                if (Objects.requireNonNull(raid) == RAID.ALL) {
                    if (mode == MODE.SESSION) {
                        selectedData = data.sessionData;
                    } else {
                        selectedData = RaidLootData.createAggregateData(data);
                    }
                } else {
                    if (mode == MODE.SESSION) {
                        selectedData = data.sessionPerRaidData != null ?
                                data.sessionPerRaidData.getOrDefault(raid.name(), new RaidLootData.RaidSpecificLoot()) :
                                new RaidLootData.RaidSpecificLoot();
                    } else {
                        selectedData = data.perRaidData.getOrDefault(raid.name(), new RaidLootData.RaidSpecificLoot());
                    }
                }
            }

            return switch (type) {
                case AMPLIFIER1 -> (long) selectedData.amplifierTier1;
                case AMPLIFIER2 -> (long) selectedData.amplifierTier2;
                case AMPLIFIER3 -> (long) selectedData.amplifierTier3;
                case AMPLIFIER4 -> (long) selectedData.amplifierTier4;
                case TOTAL_AMPLIFIER -> (long) selectedData.amplifierTier1 + selectedData.amplifierTier2 + selectedData.amplifierTier3 + selectedData.amplifierTier4;
                case CHARMS -> (long) selectedData.totalCharms;
                case EMERALDS -> selectedData.liquidEmeralds * 64L * 64L + selectedData.emeraldBlocks * 64L;
                case FABLED_ASPECTS -> (long) selectedData.fabledAspects;
                case FABLED_TOMES -> (long) selectedData.fabledTomes;
                case LEGENDARY_ASPECTS -> (long) selectedData.legendaryAspects;
                case MYTHIC_ASPECTS -> (long) selectedData.mythicAspects;
                case MYTHIC_TOMES -> (long) selectedData.mythicTomes;
                case PACKED_BAGS -> (long) selectedData.packedBags;
                case STUFFED_BAGS -> (long) selectedData.stuffedBags;
                case TOTAL_ASPECTS -> (long) selectedData.legendaryAspects + selectedData.fabledAspects + selectedData.mythicAspects;
                case TOTAL_BAGS -> (long) selectedData.packedBags + selectedData.variedBags + selectedData.stuffedBags;
                case TOTAL_TOMES -> (long) selectedData.fabledTomes + selectedData.mythicTomes;
                case VARIED_BAGS -> (long) selectedData.variedBags;
                case WARDS -> (long) selectedData.totalWards;
                case null, default -> -1L;
            };
    }

    public static String getTranslation(String keySuffix) {
            return I18n.translate("wynnextrasfunction.wynnextras.raid_drop." + keySuffix, new Object[]{
                    String.join(", ", Arrays.stream(RAID.values()).map(Enum::name).sorted().toList()),
                    String.join(", ", Arrays.stream(MODE.values()).map(Enum::name).sorted().toList()),
                    String.join(", ", Arrays.stream(TYPE.values()).map(Enum::name).sorted().toList())
            });
    }
}
