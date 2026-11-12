package li.gerard.crystallomachia.golem;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum GolemTier implements StringRepresentable {
    CRUDE("crude", 1, 1f, 5f, 24f, 6f, 3f, 120f, 1f),
    REFINED("refined", 2, 2f, 7.5f, 48f, 12f, 8f, 240f, 1.5f),
    MASTERWORK("masterwork", 3, 3f, 10f, 96f, 24f, 16f, 480f, 2f);

    public static final GolemTier DEFAULT = MASTERWORK;

    public static final Codec<GolemTier> CODEC = StringRepresentable.fromEnum(GolemTier::values);
    public static final StreamCodec<ByteBuf, GolemTier> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            index -> values()[Math.floorMod(index, values().length)], GolemTier::ordinal);

    private final String name;
    private final int capabilityCap;
    private final float speed;
    private final float intelligence;
    private final float health;
    private final float attack;
    private final float armor;
    private final float stomach;
    private final float efficiency;

    GolemTier(String name, int capabilityCap, float speed, float intelligence, float health, float attack, float armor, float stomach, float efficiency) {
        this.name = name;
        this.capabilityCap = capabilityCap;
        this.speed = speed;
        this.intelligence = intelligence;
        this.health = health;
        this.attack = attack;
        this.armor = armor;
        this.stomach = stomach;
        this.efficiency = efficiency;
    }

    public static GolemTier of(ItemStack stack) {
        return stack.getOrDefault(crystallomachia.GOLEM_TIER.get(), DEFAULT);
    }

    public static GolemTier lowest(GolemTier first, GolemTier second) {
        return first.ordinal() <= second.ordinal() ? first : second;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int capabilityCap() {
        return this.capabilityCap;
    }

    public float cap(GolemStat stat) {
        return switch (stat) {
            case SPEED -> this.speed;
            case INTELLIGENCE -> this.intelligence;
            case HEALTH -> this.health;
            case ATTACK -> this.attack;
            case ARMOR -> this.armor;
            case STOMACH -> this.stomach;
            case EFFICIENCY -> this.efficiency;
        };
    }

    public boolean allows(GolemTrait trait) {
        return trait.tier().ordinal() <= ordinal();
    }

    public Component displayName() {
        return Component.translatable("tier.crystallomachia." + this.name);
    }
}
