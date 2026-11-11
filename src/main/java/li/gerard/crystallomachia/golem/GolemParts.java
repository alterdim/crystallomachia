package li.gerard.crystallomachia.golem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record GolemParts(Map<GolemPart, ResourceKey<GolemMaterial>> materials) {
    public static final GolemParts EMPTY = new GolemParts(Map.of());

    public static final Codec<GolemParts> CODEC = Codec.unboundedMap(GolemPart.CODEC, ResourceKey.codec(GolemMaterial.REGISTRY_KEY))
            .xmap(GolemParts::new, GolemParts::materials);

    public static final StreamCodec<ByteBuf, GolemParts> STREAM_CODEC = ByteBufCodecs
            .<ByteBuf, GolemPart, ResourceKey<GolemMaterial>, Map<GolemPart, ResourceKey<GolemMaterial>>>map(
                    HashMap::new, GolemPart.STREAM_CODEC, ResourceKey.streamCodec(GolemMaterial.REGISTRY_KEY))
            .map(GolemParts::new, GolemParts::materials);

    public GolemParts {
        EnumMap<GolemPart, ResourceKey<GolemMaterial>> copy = new EnumMap<>(GolemPart.class);
        copy.putAll(materials);
        materials = Collections.unmodifiableMap(copy);
    }

    public @Nullable ResourceKey<GolemMaterial> material(GolemPart part) {
        return this.materials.get(part);
    }

    public boolean isEmpty() {
        return this.materials.isEmpty();
    }
}
