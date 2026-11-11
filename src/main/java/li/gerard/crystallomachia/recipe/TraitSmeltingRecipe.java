package li.gerard.crystallomachia.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemTrait;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

public record TraitSmeltingRecipe(GolemTrait trait, Ingredient input, ItemStackTemplate result, int work) {
    public static final ResourceKey<Registry<TraitSmeltingRecipe>> REGISTRY_KEY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "trait_smelting"));

    public static final Codec<TraitSmeltingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GolemTrait.CODEC.fieldOf("trait").forGetter(TraitSmeltingRecipe::trait),
            Ingredient.CODEC.fieldOf("input").forGetter(TraitSmeltingRecipe::input),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(TraitSmeltingRecipe::result),
            Codec.INT.optionalFieldOf("work", 200).forGetter(TraitSmeltingRecipe::work)
    ).apply(instance, TraitSmeltingRecipe::new));
}
