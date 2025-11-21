package julianh06.wynnextras.config.simpleconfig.gui.registry;

import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiProvider;
import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiRegistryAccess;
import julianh06.wynnextras.config.simpleconfig.gui.registry.api.GuiTransformer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
@SuppressWarnings("unchecked")
public class GuiRegistry implements GuiRegistryAccess {
    private final Map<Priority, List<ProviderEntry>> providers = new HashMap<>();
    private final List<TransformerEntry> transformers = new ArrayList<>();

    public GuiRegistry() {
        for(Priority priority : GuiRegistry.Priority.values()) {
            this.providers.put(priority, new ArrayList<>());
        }
    }

    private static <T> Optional<T> firstPresent(Stream<Supplier<Optional<T>>> optionals) {
        return optionals.map(Supplier::get).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    @Override
    public List<AbstractConfigListEntry<?>> get(String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        return firstPresent(
            Arrays.stream(Priority.values())
                .map(
                        (priority) -> () -> ((List) this.providers.get(priority)).stream()
                        .filter((entry) -> ((ProviderEntry) entry).predicate.test(field)).findFirst()
                )
        ).map((entry) -> ((ProviderEntry) entry).provider.get(name, field, config, defaults, registry)).orElse(null);
    }

    @Override
    public List<AbstractConfigListEntry<?>> transform(List<AbstractConfigListEntry<?>> guis, String name, Field field, Object config, Object defaults, GuiRegistryAccess registry) {
        List<GuiTransformer> transformers = this.transformers.stream()
                .filter((entry) -> entry.predicate.test(field))
                .map((entry) -> entry.transformer)
                .toList();

        for(GuiTransformer transformer: transformers) {
            guis = transformer.transform(guis, name, field, config, defaults, registry);
        }

        return guis;
    }

    private void registerProvider(Priority priority, GuiProvider provider, Predicate<Field> predicate) {
        this.providers.computeIfAbsent(priority, (p) -> new ArrayList<>()).add(new ProviderEntry(predicate, provider));
    }

    public final void registerTypeProvider(GuiProvider provider, Class<?>... types) {
        for(Class<?> type : types) {
            this.registerProvider(GuiRegistry.Priority.LAST, provider, (field) -> type == field.getType());
        }

    }

    public final void registerPredicateProvider(GuiProvider provider, Predicate<Field> predicate) {
        this.registerProvider(GuiRegistry.Priority.NORMAL, provider, predicate);
    }

    @SafeVarargs
    public final void registerAnnotationProvider(GuiProvider provider, Class<? extends Annotation>... types) {
        for(Class<? extends Annotation> type : types) {
            this.registerProvider(GuiRegistry.Priority.FIRST, provider, (field) -> field.isAnnotationPresent(type));
        }

    }

    @SafeVarargs
    public final void registerAnnotationProvider(GuiProvider provider, Predicate<Field> predicate, Class<? extends Annotation>... types) {
        for(Class<? extends Annotation> type : types) {
            this.registerProvider(GuiRegistry.Priority.FIRST, provider, (field) -> predicate.test(field) && field.isAnnotationPresent(type));
        }

    }

    public void registerPredicateTransformer(GuiTransformer transformer, Predicate<Field> predicate) {
        this.transformers.add(new TransformerEntry(predicate, transformer));
    }

    @SafeVarargs
    public final void registerAnnotationTransformer(GuiTransformer transformer, Class<? extends Annotation>... types) {
        this.registerAnnotationTransformer(transformer, (field) -> true, types);
    }

    @SafeVarargs
    public final void registerAnnotationTransformer(GuiTransformer transformer, Predicate<Field> predicate, Class<? extends Annotation>... types) {
        for(Class<? extends Annotation> type : types) {
            this.registerPredicateTransformer(transformer, (field) -> predicate.test(field) && field.isAnnotationPresent(type));
        }

    }


    // Classes
    private enum Priority {
        FIRST,
        NORMAL,
        LAST
    }

    private record ProviderEntry(Predicate<Field> predicate, GuiProvider provider) {}

    private record TransformerEntry(Predicate<Field> predicate, GuiTransformer transformer) {}
}
