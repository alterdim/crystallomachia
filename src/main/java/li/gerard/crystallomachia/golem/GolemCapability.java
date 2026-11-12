package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

public enum GolemCapability implements StringRepresentable {
    HEAT("heat", "sprites/heat.png"),
    HANDWORK("handwork", "sprites/handwork.png"),
    PRECISION("precision", "sprites/precision.png"),
    PARTY("party", "minecraft:textures/item/firework_rocket.png"),
    FREEZING("freezing", "minecraft:textures/item/snowball.png"),
    STRENGTH("strength", "minecraft:textures/item/mace.png"),
    COOKING("cooking", "minecraft:textures/item/cooked_beef.png"),
    TRANSPORT("transport", "minecraft:textures/item/minecart.png");

    public static final int ICON_SIZE = 12;

    public static final Codec<GolemCapability> CODEC = StringRepresentable.fromEnum(GolemCapability::values);
    public static final StreamCodec<ByteBuf, GolemCapability> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            index -> values()[Math.floorMod(index, values().length)], GolemCapability::ordinal);

    private final String name;
    private final Identifier icon;

    GolemCapability(String name, String iconPath) {
        this.name = name;
        this.icon = iconPath.contains(":") ? Identifier.parse(iconPath) : Identifier.fromNamespaceAndPath(crystallomachia.MODID, iconPath);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public Identifier icon() {
        return this.icon;
    }

    public Component displayName() {
        return Component.translatable("capability.crystallomachia." + this.name);
    }
}
