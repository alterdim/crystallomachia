package li.gerard.crystallomachia.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

public record ForgingRecipe(Ingredient first, Ingredient second, ItemStackTemplate result, int work) {
    public static final ResourceKey<Registry<ForgingRecipe>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "forging"));

    public static final Codec<ForgingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("first").forGetter(ForgingRecipe::first),
            Ingredient.CODEC.fieldOf("second").forGetter(ForgingRecipe::second),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ForgingRecipe::result),
            Codec.INT.optionalFieldOf("work", 200).forGetter(ForgingRecipe::work)
    ).apply(instance, ForgingRecipe::new));

    public boolean matches(ItemStack a, ItemStack b) {
        return (this.first.test(a) && this.second.test(b)) || (this.first.test(b) && this.second.test(a));
    }
}
