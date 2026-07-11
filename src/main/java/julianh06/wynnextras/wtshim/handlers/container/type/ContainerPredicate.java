// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.type;

import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;

@FunctionalInterface
public interface ContainerPredicate {
    boolean execute(ContainerContent container) throws ContainerQueryException;
}
