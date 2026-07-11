// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.type;

import julianh06.wynnextras.wtshim.models.containers.Container;

@FunctionalInterface
public interface ContainerVerification {
    boolean verify(Class<? extends Container> containerType);
}
