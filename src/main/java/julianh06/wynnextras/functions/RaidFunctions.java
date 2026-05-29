package julianh06.wynnextras.functions;

import com.wynntils.core.consumers.functions.arguments.Argument;
import com.wynntils.core.consumers.functions.arguments.FunctionArguments;
import julianh06.wynnextras.features.raid.RaidLootConfig;
import julianh06.wynnextras.features.raid.RaidLootData;
import net.minecraft.client.resource.language.I18n;

import java.util.Arrays;
import java.util.List;
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

    public static class RaidDropFunction extends WEFunctionBase<Integer>{

        @Override
        public Integer getValue(FunctionArguments functionArguments) {
            RAID raid = RAID.fromString(functionArguments.getArgument("raid").getStringValue());
            MODE mode = MODE.fromString(functionArguments.getArgument("mode").getStringValue());
            TYPE type = TYPE.fromString(functionArguments.getArgument("type").getStringValue());
            RaidLootData data = RaidLootConfig.INSTANCE.data;
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

            return (int) switch (type){
                case AMPLIFIER1 -> selectedData.amplifierTier1;
                case AMPLIFIER2 -> selectedData.amplifierTier2;
                case AMPLIFIER3 -> selectedData.amplifierTier3;
                case AMPLIFIER4 -> selectedData.amplifierTier4;
                case TOTAL_AMPLIFIER -> selectedData.amplifierTier1 + selectedData.amplifierTier2 + selectedData.amplifierTier3 + selectedData.amplifierTier4;
                case CHARMS -> selectedData.totalCharms;
                case EMERALDS -> selectedData.liquidEmeralds * 64 * 64 + selectedData.emeraldBlocks * 64;
                case FABLED_ASPECTS -> selectedData.fabledAspects;
                case FABLED_TOMES -> selectedData.fabledTomes;
                case LEGENDARY_ASPECTS -> selectedData.legendaryAspects;
                case MYTHIC_ASPECTS -> selectedData.mythicAspects;
                case MYTHIC_TOMES -> selectedData.mythicTomes;
                case PACKED_BAGS -> selectedData.packedBags;
                case STUFFED_BAGS -> selectedData.stuffedBags;
                case TOTAL_ASPECTS -> selectedData.legendaryAspects + selectedData.fabledAspects + selectedData.mythicAspects;
                case TOTAL_BAGS -> selectedData.packedBags + selectedData.variedBags + selectedData.stuffedBags;
                case TOTAL_TOMES -> selectedData.fabledTomes + selectedData.mythicTomes;
                case VARIED_BAGS -> selectedData.variedBags;
                case WARDS -> selectedData.totalWards;
                case null, default -> -1;
            };
        }

        @Override
        public FunctionArguments.Builder getArgumentsBuilder() {
            return new FunctionArguments.RequiredArgumentBuilder(List.of(
                    new Argument("raid", String.class, null),
                    new Argument("mode", String.class, null),
                    new Argument("type", String.class, null)
            ));
        }

        @Override
        public String getTranslation(String keySuffix, Object... parameters) {
            return I18n.translate(this.getTypeName().toLowerCase(Locale.ROOT) + ".wynnextras." + this.getTranslationKeyName() + "." + keySuffix, new Object[]{
                    String.join(", ", Arrays.stream(RAID.values()).map(Enum::name).sorted().toList()),
                    String.join(", ", Arrays.stream(MODE.values()).map(Enum::name).sorted().toList()),
                    String.join(", ", Arrays.stream(TYPE.values()).map(Enum::name).sorted().toList())
            });
        }
    }
}
