package li.gerard.crystallomachia.golem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemData(Map<GolemCapability, Integer> capabilities, List<GolemTrait> traits, float speed, GolemCombat combat, long appearanceSeed) {
    public static final float DEFAULT_SPEED = 1.0f;
    public static final GolemData EMPTY = new GolemData(Map.of(), 0L);

    public static final Codec<GolemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(GolemCapability.CODEC, Codec.INT).fieldOf("capabilities").forGetter(GolemData::capabilities),
            GolemTrait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(GolemData::traits),
            Codec.FLOAT.optionalFieldOf("speed", DEFAULT_SPEED).forGetter(GolemData::speed),
            GolemCombat.CODEC.optionalFieldOf("combat", GolemCombat.DEFAULT).forGetter(GolemData::combat),
            Codec.LONG.optionalFieldOf("appearance_seed", 0L).forGetter(GolemData::appearanceSeed)
    ).apply(instance, GolemData::new));

    public static final StreamCodec<ByteBuf, GolemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, GolemCapability, Integer, Map<GolemCapability, Integer>>map(
                    HashMap::new, GolemCapability.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            GolemData::capabilities,
            GolemTrait.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemData::traits,
            ByteBufCodecs.FLOAT, GolemData::speed,
            GolemCombat.STREAM_CODEC, GolemData::combat,
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
        Set<GolemTrait> distinct = EnumSet.noneOf(GolemTrait.class);
        distinct.addAll(traits);
        traits = List.copyOf(distinct);
    }

    public GolemData(Map<GolemCapability, Integer> capabilities, long appearanceSeed) {
        this(capabilities, List.of(), DEFAULT_SPEED, GolemCombat.DEFAULT, appearanceSeed);
    }

    public static GolemData combine(List<GolemData> contributions, long appearanceSeed) {
        Map<GolemCapability, Integer> capabilities = new EnumMap<>(GolemCapability.class);
        List<GolemTrait> traits = new ArrayList<>();
        float speed = 0f;
        List<GolemCombat> combat = new ArrayList<>();
        for (GolemData contribution : contributions) {
            contribution.capabilities().forEach((capability, level) -> capabilities.merge(capability, level, Integer::sum));
            traits.addAll(contribution.traits());
            speed += contribution.speed();
            combat.add(contribution.combat());
        }
        return new GolemData(capabilities, traits, contributions.isEmpty() ? DEFAULT_SPEED : speed / contributions.size(), GolemCombat.sum(combat), appearanceSeed);
    }

    public static String formatSpeed(float speed) {
        return formatStat(speed);
    }

    public static String formatStat(float value) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    public int level(GolemCapability capability) {
        return this.capabilities.getOrDefault(capability, 0);
    }

    public boolean has(GolemCapability capability, int minLevel) {
        return level(capability) >= minLevel;
    }

    public boolean hasTrait(GolemTrait trait) {
        return this.traits.contains(trait);
    }

    public boolean isEmpty() {
        return this.capabilities.isEmpty() && this.traits.isEmpty();
    }
}
