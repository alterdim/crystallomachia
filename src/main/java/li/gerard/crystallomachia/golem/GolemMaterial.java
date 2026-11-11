package li.gerard.crystallomachia.golem;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;

public record GolemMaterial(int color, Map<GolemCapability, Integer> capabilities, Optional<Identifier> texture) {
    public static final ResourceKey<Registry<GolemMaterial>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "golem_material"));

    public static final Codec<GolemMaterial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.STRING_RGB_COLOR.fieldOf("color").forGetter(GolemMaterial::color),
            Codec.unboundedMap(GolemCapability.CODEC, Codec.INT).optionalFieldOf("capabilities", Map.of()).forGetter(GolemMaterial::capabilities),
            Identifier.CODEC.optionalFieldOf("texture").forGetter(GolemMaterial::texture)
    ).apply(instance, GolemMaterial::new));

    public int level(GolemCapability capability) {
        return this.capabilities.getOrDefault(capability, 0);
    }

    public GolemData contribution(GolemPart part) {
        GolemCapability capability = part.capability();
        if (capability == null || level(capability) <= 0) {
            return GolemData.EMPTY;
        }
        return new GolemData(Map.of(capability, level(capability)), 0L);
    }

    public static Component displayName(ResourceKey<GolemMaterial> key) {
        Identifier id = key.identifier();
        return Component.translatable("golem_material." + id.getNamespace() + "." + id.getPath());
    }
}
