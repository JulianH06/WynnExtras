// © Wynntils (LGPL-3.0-only) — see LICENSE-WYNNTILS
/*
 * WynnExtras — Models registry (static field surface WynnExtras reads).
 * Fields populated during init by WynntilsMod.
 */
package julianh06.wynnextras.wtshim.core.components;

import julianh06.wynnextras.wtshim.models.abilities.AbilityModel;
import julianh06.wynnextras.wtshim.models.bank.BankModel;
import julianh06.wynnextras.wtshim.models.bomb.BombModel;
import julianh06.wynnextras.wtshim.models.character.CharacterModel;
import julianh06.wynnextras.wtshim.models.containers.ContainerModel;
import julianh06.wynnextras.wtshim.models.emeralds.EmeraldModel;
import julianh06.wynnextras.wtshim.models.gear.GearModel;
import julianh06.wynnextras.wtshim.models.ingredients.IngredientModel;
import julianh06.wynnextras.wtshim.models.items.ItemEncodingModel;
import julianh06.wynnextras.wtshim.models.items.ItemModel;
import julianh06.wynnextras.wtshim.models.marker.MarkerModel;
import julianh06.wynnextras.wtshim.models.party.PartyModel;
import julianh06.wynnextras.wtshim.models.profession.ProfessionModel;
import julianh06.wynnextras.wtshim.models.raid.RaidModel;
import julianh06.wynnextras.wtshim.models.skillpoint.SkillPointModel;
import julianh06.wynnextras.wtshim.models.stats.StatModel;
import julianh06.wynnextras.wtshim.models.statuseffects.StatusEffectModel;
import julianh06.wynnextras.wtshim.models.territories.TerritoryModel;
import julianh06.wynnextras.wtshim.models.war.WarModel;
import julianh06.wynnextras.wtshim.models.worlds.WorldStateModel;

public final class Models {
    private Models() {}

    public static GearModel Gear = new GearModel();
    public static ItemModel Item = new ItemModel();
    public static ItemEncodingModel ItemEncoding = new ItemEncodingModel();
    public static WorldStateModel WorldState = new WorldStateModel();
    public static CharacterModel Character = new CharacterModel();
    public static ContainerModel Container = new ContainerModel();
    public static RaidModel Raid = new RaidModel();
    public static TerritoryModel Territory = new TerritoryModel();
    public static SkillPointModel SkillPoint = new SkillPointModel();
    public static ProfessionModel Profession = new ProfessionModel();
    public static StatusEffectModel StatusEffect = new StatusEffectModel();
    public static EmeraldModel Emerald = new EmeraldModel();
    public static BombModel Bomb = new BombModel();
    public static BankModel Bank = new BankModel();
    public static WarModel War = new WarModel();
    public static PartyModel Party = new PartyModel();
    public static IngredientModel Ingredient = new IngredientModel();
    public static MarkerModel Marker = new MarkerModel();
    public static AbilityModel Ability = new AbilityModel();
    public static StatModel Stat = new StatModel();
}
