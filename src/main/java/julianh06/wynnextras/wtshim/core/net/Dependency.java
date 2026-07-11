// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — download dependency descriptors (faithful port). */
package julianh06.wynnextras.wtshim.core.net;

import julianh06.wynnextras.wtshim.core.components.CoreComponent;
import julianh06.wynnextras.wtshim.utils.type.Pair;
import java.util.List;
import java.util.Set;

public abstract class Dependency {
    private static final Dependency EMPTY = new Dependency() {
        @Override
        public List<Pair<CoreComponent, UrlId>> dependencies() {
            return List.of();
        }

        @Override
        public String toString() {
            return "EmptyDependency{}";
        }
    };

    public abstract List<Pair<CoreComponent, UrlId>> dependencies();

    public final boolean dependsOn(CoreComponent component, UrlId urlId) {
        return dependencies().stream().anyMatch(pair -> pair.a() == component && pair.b() == urlId);
    }

    public static Dependency empty() {
        return EMPTY;
    }

    public static Dependency simple(CoreComponent component, UrlId urlId) {
        return new SimpleComponentDataDependency(component, urlId);
    }

    public static Dependency multi(CoreComponent component, Set<UrlId> urlIds) {
        return new SingleComponentMultiDataDependency(component, urlIds);
    }

    public static Dependency complex(Set<Dependency> dependencies) {
        return new ComplexDependency(dependencies);
    }

    private static final class SimpleComponentDataDependency extends Dependency {
        private final CoreComponent component;
        private final UrlId urlId;

        private SimpleComponentDataDependency(CoreComponent component, UrlId urlId) {
            this.component = component;
            this.urlId = urlId;
        }

        @Override
        public List<Pair<CoreComponent, UrlId>> dependencies() {
            return List.of(Pair.of(component, urlId));
        }

        @Override
        public String toString() {
            return "SimpleComponentDataDependency{" + "component=" + component + ", urlId=" + urlId + '}';
        }
    }

    private static final class SingleComponentMultiDataDependency extends Dependency {
        private final CoreComponent component;
        private final Set<UrlId> urlIds;

        private SingleComponentMultiDataDependency(CoreComponent component, Set<UrlId> urlIds) {
            this.component = component;
            this.urlIds = urlIds;
        }

        @Override
        public List<Pair<CoreComponent, UrlId>> dependencies() {
            return urlIds.stream().map(urlId -> Pair.of(component, urlId)).toList();
        }

        @Override
        public String toString() {
            return "SingleComponentMultiDataDependency{" + "component=" + component + ", urlIds=" + urlIds + '}';
        }
    }

    private static final class ComplexDependency extends Dependency {
        private final Set<Dependency> dependencies;

        private ComplexDependency(Set<Dependency> dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public List<Pair<CoreComponent, UrlId>> dependencies() {
            return dependencies.stream()
                    .flatMap(dependency -> dependency.dependencies().stream())
                    .toList();
        }

        @Override
        public String toString() {
            return "ComplexDependency{" + "dependencies=" + dependencies + '}';
        }
    }
}
