// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/* WynnExtras — SavableSkillPointSet stub. */
package julianh06.wynnextras.wtshim.models.character.type;

public class SavableSkillPointSet {
    private final int[] points;

    public SavableSkillPointSet() { this.points = new int[5]; }

    public SavableSkillPointSet(int[] points) {
        this.points = points == null || points.length < 5 ? new int[5] : points.clone();
    }

    public int getStrength() { return points[0]; }
    public int getDexterity() { return points[1]; }
    public int getIntelligence() { return points[2]; }
    public int getDefence() { return points[3]; }
    public int getAgility() { return points[4]; }

    public int[] getSkillPointsAsArray() {
        return points.clone();
    }
}
