package li.gerard.crystallomachia.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import li.gerard.crystallomachia.golem.GolemCoreItem;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.golem.GolemParts;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

public class GolemEntity extends Monster implements IEntityWithComplexSpawn {
    private static final int MAX_DROPPED_PARTS = 2;
    private static final float CORE_DROP_CHANCE = 0.25f;

    private @Nullable GolemData core;
    private GolemParts parts = GolemParts.EMPTY;
    private GolemData golemData = GolemData.EMPTY;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public GolemParts parts() {
        return this.parts;
    }

    public GolemData golemData() {
        return this.golemData;
    }

    public void setGolem(GolemData data, GolemParts parts, boolean heal) {
        this.golemData = data;
        this.parts = parts;
        setBase(Attributes.MAX_HEALTH, Math.max(1f, data.combat().health()));
        setBase(Attributes.ATTACK_DAMAGE, data.combat().attack());
        setBase(Attributes.ARMOR, data.combat().armor());
        if (heal) {
            setHealth(getMaxHealth());
        }
    }

    private void setBase(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData) {
        groupData = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        if (this.parts.isEmpty()) {
            rollParts(level);
        }
        return groupData;
    }

    private void rollParts(ServerLevelAccessor level) {
        Holder<Biome> biome = level.getBiome(blockPosition());
        List<Holder.Reference<GolemMaterial>> candidates = level.registryAccess().lookup(GolemMaterial.REGISTRY_KEY)
                .map(registry -> registry.listElements().filter(material -> material.value().spawnsIn(biome)).toList())
                .orElse(List.of());
        if (candidates.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        Map<GolemPart, ResourceKey<GolemMaterial>> materials = new EnumMap<>(GolemPart.class);
        List<GolemData> contributions = new ArrayList<>();
        for (GolemPart part : GolemPart.values()) {
            Holder.Reference<GolemMaterial> material = candidates.get(random.nextInt(candidates.size()));
            materials.put(part, material.key());
            contributions.add(material.value().contribution(part));
        }
        this.core = GolemCoreItem.wild(random);
        contributions.add(this.core);
        setGolem(GolemData.combine(contributions, random.nextLong()), new GolemParts(materials), true);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (level.getRandom().nextFloat() < CORE_DROP_CHANCE) {
            spawnAtLocation(level, GolemCoreItem.create(this.core != null ? this.core : GolemCoreItem.wild(level.getRandom())));
        }
        List<GolemPart> available = new ArrayList<>(this.parts.materials().keySet());
        Collections.shuffle(available, new java.util.Random(level.getRandom().nextLong()));
        int count = Math.min(available.size(), level.getRandom().nextInt(MAX_DROPPED_PARTS + 1));
        HolderLookup.RegistryLookup<GolemMaterial> materials = level.registryAccess().lookupOrThrow(GolemMaterial.REGISTRY_KEY);
        for (int i = 0; i < count; i++) {
            GolemPart part = available.get(i);
            ResourceKey<GolemMaterial> key = this.parts.material(part);
            if (key != null) {
                materials.get(key).ifPresent(material -> spawnAtLocation(level, GolemPartItem.forPart(part).create(material)));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("golem_data", GolemData.CODEC, this.golemData);
        output.store("golem_parts", GolemParts.CODEC, this.parts);
        if (this.core != null) {
            output.store("golem_core", GolemData.CODEC, this.core);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        GolemParts savedParts = input.read("golem_parts", GolemParts.CODEC).orElse(GolemParts.EMPTY);
        this.core = input.read("golem_core", GolemData.CODEC).orElse(null);
        input.read("golem_data", GolemData.CODEC).ifPresent(data -> setGolem(data, savedParts, false));
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        GolemData.STREAM_CODEC.encode(buffer, this.golemData);
        GolemParts.STREAM_CODEC.encode(buffer, this.parts);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        this.golemData = GolemData.STREAM_CODEC.decode(buffer);
        this.parts = GolemParts.STREAM_CODEC.decode(buffer);
    }
}
