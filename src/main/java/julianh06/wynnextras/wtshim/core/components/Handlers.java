// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Handlers registry. Static fields WynnExtras reads.
 */
package julianh06.wynnextras.wtshim.core.components;

import julianh06.wynnextras.wtshim.handlers.bossbar.BossBarHandler;
import julianh06.wynnextras.wtshim.handlers.chat.ChatHandler;
import julianh06.wynnextras.wtshim.handlers.command.CommandHandler;
import julianh06.wynnextras.wtshim.handlers.container.ContainerQueryHandler;
import julianh06.wynnextras.wtshim.handlers.item.ItemHandler;
import julianh06.wynnextras.wtshim.handlers.scoreboard.ScoreboardHandler;

public final class Handlers {
    private Handlers() {}

    public static CommandHandler Command = new CommandHandler();
    public static ItemHandler Item = new ItemHandler();
    public static ChatHandler Chat = new ChatHandler();
    public static ScoreboardHandler Scoreboard = new ScoreboardHandler();
    public static BossBarHandler BossBar = new BossBarHandler();
    public static ContainerQueryHandler ContainerQuery = new ContainerQueryHandler();
}
