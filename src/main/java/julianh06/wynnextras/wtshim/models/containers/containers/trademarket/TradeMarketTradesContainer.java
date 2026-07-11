// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras - TradeMarketTradesContainer. Title pattern verbatim from Wynntils.
 * DEVIATION: Wynntils' Scrollable/Bounded property interfaces dropped (unported ContainerBounds). */
package julianh06.wynnextras.wtshim.models.containers.containers.trademarket;

import julianh06.wynnextras.wtshim.models.containers.Container;
import java.util.regex.Pattern;

public class TradeMarketTradesContainer extends Container {
    private static final Pattern TITLE_PATTERN = Pattern.compile("\uDAFF\uDFE8\uE013");

    public TradeMarketTradesContainer() {
        super(TITLE_PATTERN);
    }
}
