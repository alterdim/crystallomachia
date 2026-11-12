package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemAppetite(float stomach, float efficiency) {
    public static final GolemAppetite DEFAULT = new GolemAppetite(120f, 1f);

    public static final Codec<GolemAppetite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("stomach").forGetter(GolemAppetite::stomach),
            Codec.FLOAT.fieldOf("efficiency").forGetter(GolemAppetite::efficiency)
    ).apply(instance, GolemAppetite::new));

    public static final StreamCodec<ByteBuf, GolemAppetite> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GolemAppetite::stomach,
            ByteBufCodecs.FLOAT, GolemAppetite::efficiency,
            GolemAppetite::new);
}
