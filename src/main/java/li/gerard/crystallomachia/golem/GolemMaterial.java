package li.gerard.crystallomachia.golem;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;

public record GolemMaterial(int color, Map<GolemCapability, Integer> capabilities, Optional<Identifier> texture,
        Map<GolemPart, List<GolemTrait>> traits, float speed, float health, float attack, float armor, float stomach, float efficiency,
        Optional<TagKey<Biome>> spawnBiomes) {
    public static final ResourceKey<Registry<GolemMaterial>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "golem_material"));

    public static final Codec<GolemMaterial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.STRING_RGB_COLOR.fieldOf("color").forGetter(GolemMaterial::color),
            Codec.unboundedMap(GolemCapability.CODEC, Codec.INT).optionalFieldOf("capabilities", Map.of()).forGetter(GolemMaterial::capabilities),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(GolemMaterial::texture),
            Codec.unboundedMap(GolemPart.CODEC, GolemTrait.CODEC.listOf()).optionalFieldOf("traits", Map.of()).forGetter(GolemMaterial::traits),
            Codec.FLOAT.optionalFieldOf("speed", GolemData.DEFAULT_SPEED).forGetter(GolemMaterial::speed),
            Codec.FLOAT.optionalFieldOf("health", 4f).forGetter(GolemMaterial::health),
            Codec.FLOAT.optionalFieldOf("attack", 1f).forGetter(GolemMaterial::attack),
            Codec.FLOAT.optionalFieldOf("armor", 0f).forGetter(GolemMaterial::armor),
            Codec.FLOAT.optionalFieldOf("stomach", 20f).forGetter(GolemMaterial::stomach),
            Codec.FLOAT.optionalFieldOf("efficiency", 1f).forGetter(GolemMaterial::efficiency),
            TagKey.hashedCodec(Registries.BIOME).optionalFieldOf("spawn_biomes").forGetter(GolemMaterial::spawnBiomes)
    ).apply(instance, GolemMaterial::new));

    public int level(GolemCapability capability) {
        return this.capabilities.getOrDefault(capability, 0);
    }

    public GolemData contribution(GolemPart part) {
        GolemCapability capability = part.capability();
        Map<GolemCapability, Integer> levels = capability == null || level(capability) <= 0 ? Map.of() : Map.of(capability, level(capability));
        return new GolemData(levels, this.traits.getOrDefault(part, List.of()), this.speed,
                new GolemCombat(this.health, this.attack, this.armor), new GolemAppetite(this.stomach, this.efficiency), 0L);
    }

    public boolean spawnsIn(Holder<Biome> biome) {
        return this.spawnBiomes.isEmpty() || biome.is(this.spawnBiomes.get());
    }

    public static Component displayName(ResourceKey<GolemMaterial> key) {
        Identifier id = key.identifier();
        return Component.translatable("golem_material." + id.getNamespace() + "." + id.getPath());
    }
}
