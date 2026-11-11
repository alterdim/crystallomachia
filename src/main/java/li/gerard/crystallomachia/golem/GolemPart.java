package li.gerard.crystallomachia.golem;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum GolemPart implements StringRepresentable {
    HEAD("head", GolemCapability.PRECISION),
    TORSO("torso", GolemCapability.HEAT),
    HAND_LEFT("hand_left", GolemCapability.HANDWORK),
    HAND_RIGHT("hand_right", GolemCapability.HANDWORK),
    LEGS("legs", null);

    public static final Codec<GolemPart> CODEC = StringRepresentable.fromEnum(GolemPart::values);
    public static final StreamCodec<ByteBuf, GolemPart> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            index -> values()[Math.floorMod(index, values().length)], GolemPart::ordinal);

    private final String name;
    private final @Nullable GolemCapability capability;

    GolemPart(String name, @Nullable GolemCapability capability) {
        this.name = name;
        this.capability = capability;
    }

    public String id() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public @Nullable GolemCapability capability() {
        return this.capability;
    }

    public Component displayName() {
        return Component.translatable("golem_part.crystallomachia." + this.name);
    }
}
