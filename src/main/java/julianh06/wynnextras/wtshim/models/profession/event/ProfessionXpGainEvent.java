// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — ProfessionXpGainEvent.
 * Constructor signature matches WynnExtras' ProfessionXpGainMixin:
 * (ProfessionType, float gainedXpRaw, float currentXpPercentage).
 */
package julianh06.wynnextras.wtshim.models.profession.event;

import julianh06.wynnextras.wtshim.models.profession.type.ProfessionType;
import net.neoforged.bus.api.Event;

public class ProfessionXpGainEvent extends Event {
    private final ProfessionType profession;
    private final float gainedXpRaw;
    private final float currentXpPercentage;

    public ProfessionXpGainEvent(ProfessionType profession, float gainedXpRaw, float currentXpPercentage) {
        this.profession = profession;
        this.gainedXpRaw = gainedXpRaw;
        this.currentXpPercentage = currentXpPercentage;
    }

    public ProfessionType getProfession() { return profession; }
    public ProfessionType getProfessionType() { return profession; }
    public float getGainedXpRaw() { return gainedXpRaw; }
    public float getCurrentXpPercentage() { return currentXpPercentage; }
    public double getXp() { return gainedXpRaw; }
}
