// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.scriptedquery;

import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContent;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerPredicate;

public class ConditionalQueryStep extends QueryStep {
    private final ContainerPredicate conditionPredicate;

    ConditionalQueryStep(ContainerPredicate conditionPredicate, QueryStep queryStep) {
        super(queryStep);
        this.conditionPredicate = conditionPredicate;
    }

    @Override
    boolean startStep(ScriptedContainerQuery query, ContainerContent container) throws ContainerQueryException {
        if (conditionPredicate.execute(container)) {
            // Run this as a normal step
            return super.startStep(query, container);
        } else {
            // Skip this, and retry with next step from query
            if (!query.popOneStep()) return false;
            return query.startStep(container);
        }
    }
}
