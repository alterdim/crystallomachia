package li.gerard.crystallomachia.golem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

public record GolemData(Map<GolemCapability, Integer> capabilities, List<GolemTrait> traits, float speed, float intelligence, GolemCombat combat, GolemAppetite appetite, long appearanceSeed) {
    public static final float DEFAULT_SPEED = 1.0f;
    public static final float DEFAULT_INTELLIGENCE = 5.0f;
    public static final float MAX_INTELLIGENCE = 10.0f;
    public static final float INTELLIGENCE_PER_TRAIT_SLOT = 2.0f;
    public static final int BASE_TRAIT_SLOTS = 2;
    public static final int MAX_TRAIT_SLOTS = 4;
    public static final float MIN_HEAD_SCALE = 0.7f;
    public static final float MAX_HEAD_SCALE = 1.3f;
    public static final GolemData EMPTY = new GolemData(Map.of(), 0L);

    public static final Codec<GolemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(GolemCapability.CODEC, Codec.INT).fieldOf("capabilities").forGetter(GolemData::capabilities),
            GolemTrait.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(GolemData::traits),
            Codec.FLOAT.optionalFieldOf("speed", DEFAULT_SPEED).forGetter(GolemData::speed),
            Codec.FLOAT.optionalFieldOf("intelligence", 0f).forGetter(GolemData::intelligence),
            GolemCombat.CODEC.optionalFieldOf("combat", GolemCombat.DEFAULT).forGetter(GolemData::combat),
            GolemAppetite.CODEC.optionalFieldOf("appetite", GolemAppetite.DEFAULT).forGetter(GolemData::appetite),
            Codec.LONG.optionalFieldOf("appearance_seed", 0L).forGetter(GolemData::appearanceSeed)
    ).apply(instance, GolemData::new));

    public static final StreamCodec<ByteBuf, GolemData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.<ByteBuf, GolemCapability, Integer, Map<GolemCapability, Integer>>map(
                    HashMap::new, GolemCapability.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            GolemData::capabilities,
            GolemTrait.STREAM_CODEC.apply(ByteBufCodecs.list()), GolemData::traits,
            ByteBufCodecs.FLOAT, GolemData::speed,
            ByteBufCodecs.FLOAT, GolemData::intelligence,
            GolemCombat.STREAM_CODEC, GolemData::combat,
            GolemAppetite.STREAM_CODEC, GolemData::appetite,
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
        this(capabilities, List.of(), DEFAULT_SPEED, DEFAULT_INTELLIGENCE, GolemCombat.DEFAULT, GolemAppetite.DEFAULT, appearanceSeed);
    }

    public static GolemData combine(List<GolemData> contributions, long appearanceSeed) {
        Map<GolemCapability, Integer> capabilities = new EnumMap<>(GolemCapability.class);
        Set<GolemTrait> traits = new LinkedHashSet<>();
        float speed = 0f;
        float intelligence = 0f;
        float stomach = 0f;
        float efficiency = 0f;
        List<GolemCombat> combat = new ArrayList<>();
        for (GolemData contribution : contributions) {
            contribution.capabilities().forEach((capability, level) -> capabilities.merge(capability, level, Integer::sum));
            traits.addAll(contribution.traits());
            speed += contribution.speed();
            intelligence += contribution.intelligence();
            stomach += contribution.appetite().stomach();
            efficiency += contribution.appetite().efficiency();
            combat.add(contribution.combat());
        }
        if (contributions.isEmpty()) {
            return new GolemData(capabilities, appearanceSeed);
        }
        int count = contributions.size();
        List<GolemTrait> kept = traits.stream().limit(traitSlots(intelligence)).toList();
        return new GolemData(capabilities, kept, speed / count, intelligence, GolemCombat.sum(combat), new GolemAppetite(stomach, efficiency / count), appearanceSeed);
    }

    public static int traitSlots(float intelligence) {
        int extra = (int) ((DEFAULT_INTELLIGENCE - intelligence) / INTELLIGENCE_PER_TRAIT_SLOT);
        return BASE_TRAIT_SLOTS + Math.clamp(extra, 0, MAX_TRAIT_SLOTS - BASE_TRAIT_SLOTS);
    }

    public int traitSlots() {
        return traitSlots(this.intelligence);
    }

    public GolemData withTraits(List<GolemTrait> traits) {
        return new GolemData(this.capabilities, traits, this.speed, this.intelligence, this.combat, this.appetite, this.appearanceSeed);
    }

    public GolemData capped(GolemTier tier) {
        Map<GolemCapability, Integer> levels = new EnumMap<>(GolemCapability.class);
        this.capabilities.forEach((capability, level) -> levels.put(capability, Math.min(level, tier.capabilityCap())));
        List<GolemTrait> allowed = this.traits.stream().filter(tier::allows).toList();
        GolemCombat cappedCombat = new GolemCombat(
                Math.min(this.combat.health(), tier.cap(GolemStat.HEALTH)),
                Math.min(this.combat.attack(), tier.cap(GolemStat.ATTACK)),
                Math.min(this.combat.armor(), tier.cap(GolemStat.ARMOR)));
        GolemAppetite cappedAppetite = new GolemAppetite(
                Math.min(this.appetite.stomach(), tier.cap(GolemStat.STOMACH)),
                Math.min(this.appetite.efficiency(), tier.cap(GolemStat.EFFICIENCY)));
        return new GolemData(levels, allowed, Math.min(this.speed, tier.cap(GolemStat.SPEED)),
                Math.min(this.intelligence, tier.cap(GolemStat.INTELLIGENCE)), cappedCombat, cappedAppetite, this.appearanceSeed);
    }

    public float headScale() {
        float fraction = Math.clamp(this.intelligence / MAX_INTELLIGENCE, 0f, 1f);
        return MIN_HEAD_SCALE + (MAX_HEAD_SCALE - MIN_HEAD_SCALE) * fraction;
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
