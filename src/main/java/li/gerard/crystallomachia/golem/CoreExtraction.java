package li.gerard.crystallomachia.golem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public record CoreExtraction(GolemData base, Map<GolemCapability, List<Integer>> capabilitySources, Map<GolemTrait, Integer> traitSources) {
    public static final float STAT_VARIANCE = 0.1f;
    public static final float CAPABILITY_CHANCE = 0.15f;
    public static final float TRAIT_CHANCE = 0.03f;

    public static @Nullable CoreExtraction of(ItemStack golem, HolderLookup.Provider registries) {
        if (!(golem.getItem() instanceof GolemItem)) {
            return null;
        }
        GolemData core = golem.get(crystallomachia.GOLEM_CORE_DATA.get());
        List<GolemData> parts = new ArrayList<>();
        HolderLookup.RegistryLookup<GolemMaterial> materials = registries.lookup(GolemMaterial.REGISTRY_KEY).orElse(null);
        if (materials != null) {
            for (Map.Entry<GolemPart, ResourceKey<GolemMaterial>> entry : GolemItem.getParts(golem).materials().entrySet()) {
                materials.get(entry.getValue()).ifPresent(material -> parts.add(material.value().contribution(entry.getKey())));
            }
        }
        if (parts.isEmpty() && core == null && golem.has(crystallomachia.GOLEM_DATA.get())) {
            parts.add(asSinglePart(GolemItem.getData(golem)));
        }
        List<GolemData> components = new ArrayList<>(parts);
        if (core != null) {
            components.add(core);
        }
        if (components.isEmpty()) {
            return null;
        }

        float speed = 0f;
        float stomach = 0f;
        float efficiency = 0f;
        List<GolemCombat> combat = new ArrayList<>();
        for (GolemData component : components) {
            speed += component.speed();
            stomach += component.appetite().stomach();
            efficiency += component.appetite().efficiency();
            combat.add(component.combat());
        }
        int count = components.size();
        GolemCombat total = GolemCombat.sum(combat);
        GolemData base = new GolemData(
                core == null ? Map.of() : core.capabilities(),
                core == null ? List.of() : core.traits(),
                speed / count,
                0f,
                new GolemCombat(total.health() / count, total.attack() / count, total.armor() / count),
                new GolemAppetite(stomach / count, efficiency / count),
                0L);

        Map<GolemCapability, List<Integer>> capabilitySources = new EnumMap<>(GolemCapability.class);
        Map<GolemTrait, Integer> traitSources = new EnumMap<>(GolemTrait.class);
        for (GolemData part : parts) {
            part.capabilities().forEach((capability, level) -> capabilitySources.computeIfAbsent(capability, key -> new ArrayList<>()).add(level));
            for (GolemTrait trait : part.traits()) {
                if (!base.hasTrait(trait)) {
                    traitSources.merge(trait, 1, Integer::sum);
                }
            }
        }
        return new CoreExtraction(base, capabilitySources, traitSources);
    }

    private static GolemData asSinglePart(GolemData golem) {
        int partCount = GolemPart.values().length;
        GolemCombat combat = golem.combat();
        return new GolemData(golem.capabilities(), golem.traits(), golem.speed(), 0f,
                new GolemCombat(combat.health() / partCount, combat.attack() / partCount, combat.armor() / partCount),
                new GolemAppetite(golem.appetite().stomach() / partCount, golem.appetite().efficiency()), 0L);
    }

    public float min(GolemStat stat) {
        return GolemCoreItem.round(stat.value(this.base) * (1f - STAT_VARIANCE));
    }

    public float max(GolemStat stat) {
        return GolemCoreItem.round(stat.value(this.base) * (1f + STAT_VARIANCE));
    }

    public int minLevel(GolemCapability capability) {
        return this.base.level(capability);
    }

    public int maxLevel(GolemCapability capability) {
        int level = minLevel(capability);
        for (int source : this.capabilitySources.getOrDefault(capability, List.of())) {
            level += source;
        }
        return level;
    }

    public float capabilityChance(GolemCapability capability) {
        return anyOf(CAPABILITY_CHANCE, this.capabilitySources.getOrDefault(capability, List.of()).size());
    }

    public float traitChance(GolemTrait trait) {
        return this.base.hasTrait(trait) ? 1f : anyOf(TRAIT_CHANCE, this.traitSources.getOrDefault(trait, 0));
    }

    private static float anyOf(float chance, int tries) {
        return 1f - (float) Math.pow(1f - chance, tries);
    }

    public GolemData roll(RandomSource random) {
        Map<GolemCapability, Integer> capabilities = new EnumMap<>(GolemCapability.class);
        capabilities.putAll(this.base.capabilities());
        this.capabilitySources.forEach((capability, levels) -> {
            for (int level : levels) {
                if (random.nextFloat() < CAPABILITY_CHANCE) {
                    capabilities.merge(capability, level, Integer::sum);
                }
            }
        });
        List<GolemTrait> traits = new ArrayList<>(this.base.traits());
        this.traitSources.forEach((trait, sources) -> {
            for (int i = 0; i < sources; i++) {
                if (random.nextFloat() < TRAIT_CHANCE) {
                    traits.add(trait);
                    break;
                }
            }
        });
        GolemCombat combat = this.base.combat();
        GolemAppetite appetite = this.base.appetite();
        return new GolemData(capabilities, traits, vary(random, this.base.speed()), 0f,
                new GolemCombat(vary(random, combat.health()), vary(random, combat.attack()), vary(random, combat.armor())),
                new GolemAppetite(vary(random, appetite.stomach()), vary(random, appetite.efficiency())), 0L);
    }

    private static float vary(RandomSource random, float value) {
        return GolemCoreItem.round(value * (1f + (random.nextFloat() * 2f - 1f) * STAT_VARIANCE));
    }
}
