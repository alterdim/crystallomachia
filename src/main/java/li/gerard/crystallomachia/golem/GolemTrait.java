package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

public enum GolemTrait implements StringRepresentable {
    OVERHEAT("overheat", "sprites/overheat.png");

    public static final Codec<GolemTrait> CODEC = StringRepresentable.fromEnum(GolemTrait::values);
    public static final StreamCodec<ByteBuf, GolemTrait> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            index -> values()[Math.floorMod(index, values().length)], GolemTrait::ordinal);

    private final String name;
    private final Identifier icon;

    GolemTrait(String name, String iconPath) {
        this.name = name;
        this.icon = Identifier.fromNamespaceAndPath(crystallomachia.MODID, iconPath);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Identifier icon() {
        return this.icon;
    }

    public Component displayName() {
        return Component.translatable("trait.crystallomachia." + this.name);
    }

    public Component description() {
        return Component.translatable("trait.crystallomachia." + this.name + ".description");
    }
}
