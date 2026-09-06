package julianh06.wynnextras.compat.wynntils;

import julianh06.wynnextras.wynncraft.state.ProfessionState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class WynntilsProfessionAdapter {
    public record Progress(int level, double percent) {}

    private record ModelBinding(Object model, Class<? extends Enum> professionType,
                                Method getLevel, Method getProgress) {}

    private record EventBinding(Method getProfession, Method getGainedXpRaw,
                                Method getCurrentXpPercentage, Method getDisplayName) {}

    private static final WynntilsCapability<ModelBinding> MODEL = new WynntilsCapability<>(
            "profession-model",
            () -> {
                Class<?> modelsClass = WynntilsCompat.requireClass("com.wynntils.core.components.Models");
                Class<?> professionModelClass = WynntilsCompat.requireClass("com.wynntils.models.profession.ProfessionModel");
                Class<? extends Enum> professionType = WynntilsCompat.requireClass(
                        "com.wynntils.models.profession.type.ProfessionType").asSubclass(Enum.class);
                Field professionField = modelsClass.getField("Profession");
                Object model = professionField.get(null);
                return new ModelBinding(
                        model,
                        professionType,
                        professionModelClass.getMethod("getLevel", professionType),
                        professionModelClass.getMethod("getProgress", professionType)
                );
            }
    );

    private static final WynntilsCapability<EventBinding> XP_GAIN_EVENT = new WynntilsCapability<>(
            "profession-xp-gain-event",
            () -> {
                Class<?> eventClass = WynntilsCompat.requireClass(
                        "com.wynntils.models.profession.event.ProfessionXpGainEvent");
                Class<?> professionType = WynntilsCompat.requireClass(
                        "com.wynntils.models.profession.type.ProfessionType");
                return new EventBinding(
                        eventClass.getMethod("getProfession"),
                        eventClass.getMethod("getGainedXpRaw"),
                        eventClass.getMethod("getCurrentXpPercentage"),
                        professionType.getMethod("getDisplayName")
                );
            }
    );

    private WynntilsProfessionAdapter() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<Progress> progress(String professionName) {
        return MODEL.invoke(binding -> {
            Enum<?> profession = Enum.valueOf(binding.professionType, professionName.toUpperCase());
            int level = ((Number) binding.getLevel.invoke(binding.model, profession)).intValue();
            double percent = ((Number) binding.getProgress.invoke(binding.model, profession)).doubleValue();
            return new Progress(level, percent);
        });
    }

    public static void observeXpGain(Object event) {
        XP_GAIN_EVENT.run(binding -> {
            Object profession = binding.getProfession.invoke(event);
            String name = (String) binding.getDisplayName.invoke(profession);
            float gainedXp = ((Number) binding.getGainedXpRaw.invoke(event)).floatValue();
            double currentPercent = ((Number) binding.getCurrentXpPercentage.invoke(event)).doubleValue();
            ProfessionState.observeXpGain(name, gainedXp, currentPercent);
            return null;
        });
    }
}
