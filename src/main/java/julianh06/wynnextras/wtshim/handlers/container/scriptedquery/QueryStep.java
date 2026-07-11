// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
package julianh06.wynnextras.wtshim.handlers.container.scriptedquery;

import julianh06.wynnextras.wtshim.core.components.Handlers;
import julianh06.wynnextras.wtshim.core.text.StyledText;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryException;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryStep;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerAction;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContent;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContentChangeType;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerContentVerification;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerPredicate;
import julianh06.wynnextras.wtshim.handlers.container.type.ContainerVerification;
import julianh06.wynnextras.wtshim.models.containers.Container;
import julianh06.wynnextras.wtshim.utils.wynn.ContainerUtils;
import java.util.function.Supplier;
import net.minecraft.item.Item;
import org.lwjgl.glfw.GLFW;

public class QueryStep {
    // We should never get to MenuOpenedEvent
    private static final ContainerVerification EXPECT_SAME_MENU = (type) -> false;
    private static final ContainerContentVerification WAIT_FOR_SET_CONTENT =
            (container, changes, changeType) -> changeType == ContainerContentChangeType.SET_CONTENT;
    private static final ContainerAction IGNORE_INCOMING_CONTAINER = c -> {};

    private final ContainerPredicate startAction;
    private ContainerVerification verification = EXPECT_SAME_MENU;
    private ContainerContentVerification contentVerification = WAIT_FOR_SET_CONTENT;
    private ContainerAction handleContent = IGNORE_INCOMING_CONTAINER;

    protected QueryStep(ContainerPredicate startAction) {
        this.startAction = startAction;
    }

    protected QueryStep(QueryStep queryStep) {
        this.startAction = queryStep.startAction;
        this.verification = queryStep.verification;
        this.contentVerification = queryStep.contentVerification;
        this.handleContent = queryStep.handleContent;
    }

    // region Builder API actions

    public static QueryStep useItemInHotbar(int slotNum) {
        return new QueryStep((container) -> ContainerUtils.openInventory(slotNum));
    }

    public static QueryStep clickOnSlot(int slotNum) {
        return new QueryStep(container -> {
            ContainerUtils.clickOnSlot(
                    slotNum, container.containerId(), GLFW.GLFW_MOUSE_BUTTON_LEFT, container.items());
            return true;
        });
    }

    public static QueryStep clickOnSlot(int slotNum, Supplier<Integer> mouseButtonSupplier) {
        return new QueryStep(container -> {
            ContainerUtils.clickOnSlot(slotNum, container.containerId(), mouseButtonSupplier.get(), container.items());
            return true;
        });
    }

    public static QueryStep clickOnMatchingSlot(int slotNum, Item expectedItemType, StyledText expectedItemName) {
        return new QueryStep(container -> {
            if (!ScriptedContainerQuery.containerHasSlot(container, slotNum, expectedItemType, expectedItemName))
                throw new ContainerQueryException("Cannot find matching slot");

            ContainerUtils.clickOnSlot(
                    slotNum, container.containerId(), GLFW.GLFW_MOUSE_BUTTON_LEFT, container.items());
            return true;
        });
    }

    public static QueryStep sendCommand(String command) {
        return new QueryStep(container -> {
            Handlers.Command.queueCommand(command);
            return true;
        });
    }

    public QueryStep expectContainer(Class<? extends Container> expectedContainerType) {
        this.verification = (type) -> type == expectedContainerType;
        return this;
    }

    public QueryStep verifyContentChange(ContainerContentVerification verification) {
        this.contentVerification = verification;
        return this;
    }

    public QueryStep processIncomingContainer(ContainerAction action) {
        this.handleContent = action;
        return this;
    }

    // endregion

    // region ScriptedContainerQuery support

    ContainerVerification getVerification() {
        return verification;
    }

    ContainerContentVerification getContentVerification() {
        return contentVerification;
    }

    ContainerAction getHandleContent() {
        return handleContent;
    }

    boolean startStep(ScriptedContainerQuery query, ContainerContent container) throws ContainerQueryException {
        return startAction.execute(container);
    }

    ContainerQueryStep getNextStep(ScriptedContainerQuery query) {
        // Go to next step, if any
        if (!query.popOneStep()) return null;

        return query;
    }

    // endregion
}
