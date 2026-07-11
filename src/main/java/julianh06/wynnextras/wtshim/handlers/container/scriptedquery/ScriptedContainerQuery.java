// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.scriptedquery;

import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryStep;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContent;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContentChangeType;
import julianh06.wynnextras.wtshim.models.containers.Container;
import julianh06.wynnextras.wtshim.utils.wynn.ItemUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ScriptedContainerQuery implements ContainerQueryStep {
    private final LinkedList<QueryStep> steps;
    private final Consumer<String> errorHandler;
    private final String name;
    private QueryStep currentStep = null;

    ScriptedContainerQuery(String name, LinkedList<QueryStep> steps, Consumer<String> errorHandler) {
        this.name = name;
        this.steps = steps;
        this.errorHandler = errorHandler;
    }

    public static QueryBuilder builder(String name) {
        return new QueryBuilder(name);
    }

    public static boolean containerHasSlot(
            ContainerContent container, int slotNum, Item expectedItemType, StyledText expectedItemName) {
        ItemStack itemStack = container.items().get(slotNum);
        return itemStack.isOf(expectedItemType)
                && ItemUtils.getItemName(itemStack).equals(expectedItemName);
    }

    public void executeQuery() {
        if (!popOneStep()) return;

        Handlers.ContainerQuery.runQuery(this);
    }

    @Override
    public boolean startStep(ContainerContent container) throws ContainerQueryException {
        return currentStep.startStep(this, container);
    }

    @Override
    public boolean verifyContainer(Class<? extends Container> containerType) {
        return currentStep.getVerification().verify(containerType);
    }

    @Override
    public boolean verifyContentChange(
            ContainerContent container, Int2ObjectMap<ItemStack> changes, ContainerContentChangeType changeType) {
        return currentStep.getContentVerification().verify(container, changes, changeType);
    }

    @Override
    public void handleContent(ContainerContent container) throws ContainerQueryException {
        currentStep.getHandleContent().processContainer(container);
    }

    @Override
    public ContainerQueryStep getNextStep(ContainerContent container) {
        return currentStep.getNextStep(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onError(String errorMsg) {
        errorHandler.accept(errorMsg);
        // Remove all remaining steps
        currentStep = null;
        steps.clear();
    }

    boolean popOneStep() {
        if (steps.isEmpty()) {
            currentStep = null;
            return false;
        }

        this.currentStep = steps.pop();
        return true;
    }
}
