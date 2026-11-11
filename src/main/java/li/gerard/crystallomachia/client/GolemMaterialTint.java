package li.gerard.crystallomachia.client;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record GolemMaterialTint(Optional<GolemPart> part, int defaultColor) implements ItemTintSource {
    public static final MapCodec<GolemMaterialTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GolemPart.CODEC.optionalFieldOf("part").forGetter(GolemMaterialTint::part),
            ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", 0xFFFFFF).forGetter(GolemMaterialTint::defaultColor)
    ).apply(instance, GolemMaterialTint::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        ResourceKey<GolemMaterial> key = this.part.isPresent()
                ? GolemItem.getParts(stack).material(this.part.get())
                : stack.get(crystallomachia.GOLEM_MATERIAL.get());
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (key == null || connection == null) {
            return ARGB.opaque(this.defaultColor);
        }
        return connection.registryAccess().lookup(GolemMaterial.REGISTRY_KEY)
                .flatMap(registry -> registry.get(key))
                .map(material -> ARGB.opaque(material.value().color()))
                .orElse(ARGB.opaque(this.defaultColor));
    }

    @Override
    public MapCodec<GolemMaterialTint> type() {
        return MAP_CODEC;
    }
}
