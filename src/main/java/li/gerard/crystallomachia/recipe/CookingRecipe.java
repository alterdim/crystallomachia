package li.gerard.crystallomachia.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

public record CookingRecipe(Ingredient input, ItemStackTemplate result, int work) {
    public static final ResourceKey<Registry<CookingRecipe>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "cooking"));

    public static final Codec<CookingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("input").forGetter(CookingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CookingRecipe::result),
            Codec.INT.optionalFieldOf("work", 200).forGetter(CookingRecipe::work)
    ).apply(instance, CookingRecipe::new));
}
