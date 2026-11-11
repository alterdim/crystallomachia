package li.gerard.crystallomachia.golem;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemCombat(float health, float attack, float armor) {
    public static final GolemCombat DEFAULT = new GolemCombat(20f, 3f, 0f);
    public static final GolemCombat ZERO = new GolemCombat(0f, 0f, 0f);

    public static final Codec<GolemCombat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("health").forGetter(GolemCombat::health),
            Codec.FLOAT.fieldOf("attack").forGetter(GolemCombat::attack),
            Codec.FLOAT.fieldOf("armor").forGetter(GolemCombat::armor)
    ).apply(instance, GolemCombat::new));

    public static final StreamCodec<ByteBuf, GolemCombat> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GolemCombat::health,
            ByteBufCodecs.FLOAT, GolemCombat::attack,
            ByteBufCodecs.FLOAT, GolemCombat::armor,
            GolemCombat::new);

    public static GolemCombat sum(List<GolemCombat> parts) {
        float health = 0f;
        float attack = 0f;
        float armor = 0f;
        for (GolemCombat part : parts) {
            health += part.health();
            attack += part.attack();
            armor += part.armor();
        }
        return new GolemCombat(health, attack, armor);
    }
}
