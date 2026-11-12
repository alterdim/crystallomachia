package li.gerard.crystallomachia.compat.jei;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.block.ForgeStationBlockEntity;
import li.gerard.crystallomachia.block.GolemAssemblerBlockEntity;
import li.gerard.crystallomachia.block.GolemFurnaceBlockEntity;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.block.StoneDelicacyBlockEntity;
import li.gerard.crystallomachia.recipe.CookingRecipe;
import li.gerard.crystallomachia.recipe.ForgingRecipe;
import li.gerard.crystallomachia.recipe.TraitSmeltingRecipe;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class CrystallomachiaJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(crystallomachia.MODID, "jei");

    public static final IRecipeType<ForgingRecipe> FORGING = IRecipeType.create(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "forging"), ForgingRecipe.class);
    public static final IRecipeType<TraitSmeltingRecipe> TRAIT_SMELTING = IRecipeType.create(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "trait_smelting"), TraitSmeltingRecipe.class);
    public static final IRecipeType<AssemblyRecipe> ASSEMBLY = IRecipeType.create(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "assembly"), AssemblyRecipe.class);

    public static final IRecipeType<CookingRecipe> COOKING = IRecipeType.create(
            Identifier.fromNamespaceAndPath(crystallomachia.MODID, "cooking"), CookingRecipe.class);

    public record AssemblyRecipe(Map<GolemPart, ItemStack> parts, ItemStack golem) {}

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        registration.addRecipeCategories(
                new WorkstationCategory<>(helpers, FORGING, CrystallomachiaJeiPlugin::forgingUI,
                        Component.translatable("block.crystallomachia.forge_station"), crystallomachia.FORGE_STATION_ITEM.get(), 150, 44),
                new WorkstationCategory<>(helpers, TRAIT_SMELTING, CrystallomachiaJeiPlugin::traitSmeltingUI,
                        Component.translatable("jei.crystallomachia.trait_smelting"), crystallomachia.GOLEM_FURNACE_ITEM.get(), 150, 44),
                new WorkstationCategory<>(helpers, COOKING, CrystallomachiaJeiPlugin::cookingUI,
                        Component.translatable("block.crystallomachia.stone_delicacy"), crystallomachia.STONE_DELICACY_ITEM.get(), 150, 44),
                new WorkstationCategory<>(helpers, ASSEMBLY, CrystallomachiaJeiPlugin::assemblyUI,
                        Component.translatable("block.crystallomachia.golem_assembler"), crystallomachia.GOLEM_ASSEMBLER_ITEM.get(), 160, 70));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FORGING, registryValues(ForgingRecipe.REGISTRY_KEY));
        registration.addRecipes(TRAIT_SMELTING, registryValues(TraitSmeltingRecipe.REGISTRY_KEY));
        registration.addRecipes(COOKING, registryValues(CookingRecipe.REGISTRY_KEY));
        registration.addRecipes(ASSEMBLY, assemblyRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(RecipeTypes.SMELTING, crystallomachia.GOLEM_FURNACE_ITEM.get());
        registration.addCraftingStation(TRAIT_SMELTING, crystallomachia.GOLEM_FURNACE_ITEM.get());
        registration.addCraftingStation(FORGING, crystallomachia.FORGE_STATION_ITEM.get());
        registration.addCraftingStation(COOKING, crystallomachia.STONE_DELICACY_ITEM.get());
        registration.addCraftingStation(ASSEMBLY, crystallomachia.GOLEM_ASSEMBLER_ITEM.get());
    }

    private static <T> List<T> registryValues(ResourceKey<Registry<T>> key) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }
        return connection.registryAccess().lookup(key).stream()
                .flatMap(registry -> registry.listElements())
                .map(holder -> holder.value())
                .toList();
    }

    private static List<AssemblyRecipe> assemblyRecipes() {
        List<AssemblyRecipe> recipes = new ArrayList<>();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return recipes;
        }
        connection.registryAccess().lookup(GolemMaterial.REGISTRY_KEY).ifPresent(materials -> materials.listElements().forEach(material -> {
            Map<GolemPart, ItemStack> parts = new EnumMap<>(GolemPart.class);
            Map<GolemPart, Holder.Reference<GolemMaterial>> materialsByPart = new EnumMap<>(GolemPart.class);
            for (GolemPart part : GolemPart.values()) {
                parts.put(part, partItem(part).create(material));
                materialsByPart.put(part, material);
            }
            recipes.add(new AssemblyRecipe(parts, GolemItem.createFromMaterials(materialsByPart, 0L)));
        }));
        return recipes;
    }

    private static GolemPartItem partItem(GolemPart part) {
        return switch (part) {
            case HEAD -> crystallomachia.GOLEM_HEAD.get();
            case TORSO -> crystallomachia.GOLEM_TORSO.get();
            case HAND_LEFT -> crystallomachia.GOLEM_HAND_LEFT.get();
            case HAND_RIGHT -> crystallomachia.GOLEM_HAND_RIGHT.get();
            case LEGS -> crystallomachia.GOLEM_LEGS.get();
        };
    }

    private static ModularUI forgingUI(ForgingRecipe recipe) {
        return JeiRecipeUI.page(
                JeiRecipeUI.roles(ForgeStationBlockEntity.GOLEM_ROLES),
                JeiRecipeUI.input(recipe.first()),
                JeiRecipeUI.input(recipe.second()),
                JeiRecipeUI.arrow(),
                JeiRecipeUI.output(recipe.result().create()));
    }

    private static ModularUI traitSmeltingUI(TraitSmeltingRecipe recipe) {
        return JeiRecipeUI.page(
                JeiRecipeUI.roles(GolemFurnaceBlockEntity.GOLEM_ROLES),
                JeiRecipeUI.trait(recipe.trait()),
                JeiRecipeUI.input(recipe.input()),
                JeiRecipeUI.arrow(),
                JeiRecipeUI.output(recipe.result().create()));
    }

    private static ModularUI cookingUI(CookingRecipe recipe) {
        return JeiRecipeUI.page(
                JeiRecipeUI.roles(List.of(StoneDelicacyBlockEntity.COOK_ROLE)),
                JeiRecipeUI.input(recipe.input()),
                JeiRecipeUI.arrow(),
                JeiRecipeUI.output(recipe.result().create()));
    }

    private static ModularUI assemblyUI(AssemblyRecipe recipe) {
        UIElement parts = JeiRecipeUI.column(1).addChildren(
                JeiRecipeUI.input(recipe.parts().get(GolemPart.HEAD)),
                JeiRecipeUI.row(1).addChildren(
                        JeiRecipeUI.input(recipe.parts().get(GolemPart.HAND_LEFT)),
                        JeiRecipeUI.input(recipe.parts().get(GolemPart.TORSO)),
                        JeiRecipeUI.input(recipe.parts().get(GolemPart.HAND_RIGHT))),
                JeiRecipeUI.input(recipe.parts().get(GolemPart.LEGS)));
        return JeiRecipeUI.page(
                JeiRecipeUI.roles(GolemAssemblerBlockEntity.GOLEM_ROLES),
                parts,
                JeiRecipeUI.input(new ItemStack(crystallomachia.GOLEM_CORE.get())),
                JeiRecipeUI.arrow(),
                JeiRecipeUI.output(recipe.golem()));
    }
}
