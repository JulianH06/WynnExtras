package julianh06.wynnextras.features.crafting.model;

import julianh06.wynnextras.utils.Pair;

import java.util.List;
import java.util.Optional;

public record GearRequirements(int level, Optional<ClassType> classType,
                               List<Pair<Skill, Integer>> skills, Optional<Object> quest) {}
