package li.gerard.crystallomachia.golem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemData(Map<GolemCapability, Integer> capabilities, long appearanceSeed) {
    public static final GolemData EMPTY = new GolemData(Map.of(), 0L);

    public static final Codec<GolemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(GolemCapability.CODEC, Codec.INT).fieldOf("capabilities").forGetter(GolemData::capabilities),
            Codec.LONG.optionalFieldOf("appearance_seed", 0L).forGetter(GolemData::appearanceSeed)
    ).apply(instance, GolemData::new));

    public static final StreamCodec<ByteBuf, GolemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, GolemCapability, Integer, Map<GolemCapability, Integer>>map(
                    HashMap::new, GolemCapability.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            GolemData::capabilities,
            ByteBufCodecs.VAR_LONG, GolemData::appearanceSeed,
            GolemData::new);

    public static final CustomDirectAccessor<GolemData> ACCESSOR = CustomDirectAccessor.builder(GolemData.class)
            .codec(CODEC)
            .streamCodec(STREAM_CODEC)
            .build();

    public GolemData {
        EnumMap<GolemCapability, Integer> copy = new EnumMap<>(GolemCapability.class);
        capabilities.forEach((capability, level) -> {
            if (level != null && level > 0) {
                copy.put(capability, level);
            }
        });
        capabilities = Collections.unmodifiableMap(copy);
    }

    public int level(GolemCapability capability) {
        return this.capabilities.getOrDefault(capability, 0);
    }

    public boolean has(GolemCapability capability, int minLevel) {
        return level(capability) >= minLevel;
    }

    public boolean isEmpty() {
        return this.capabilities.isEmpty();
    }
}
