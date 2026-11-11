package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum GolemCapability implements StringRepresentable {
    HEAT("heat"),
    HANDWORK("handwork");

    public static final Codec<GolemCapability> CODEC = StringRepresentable.fromEnum(GolemCapability::values);
    public static final StreamCodec<ByteBuf, GolemCapability> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            index -> values()[Math.floorMod(index, values().length)], GolemCapability::ordinal);

    private final String name;

    GolemCapability(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Component displayName() {
        return Component.translatable("capability.crystallomachia." + this.name);
    }
}
