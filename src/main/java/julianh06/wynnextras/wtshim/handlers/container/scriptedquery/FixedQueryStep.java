// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.scriptedquery;

import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContent;

class FixedQueryStep extends QueryStep {
    FixedQueryStep() {
        // Just use a dummy action, it will never be run
        super(c -> true);
    }

    @Override
    public boolean startStep(ScriptedContainerQuery query, ContainerContent container) throws ContainerQueryException {
        // A FixedQueryStep always gets it handleContent called
        getHandleContent().processContainer(container);

        // Try again with next
        if (!query.popOneStep()) return false;
        return query.startStep(container);
    }
}
