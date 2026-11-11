package li.gerard.crystallomachia.block;

import java.util.List;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemTrait;
import li.gerard.crystallomachia.recipe.TraitSmeltingRecipe;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

public class GolemFurnaceBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final List<GolemRole> GOLEM_ROLES = List.of(GolemRole.of(GolemCapability.HEAT, 1));

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipeCheck = RecipeManager.createCheck(RecipeType.SMELTING);

    public GolemFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.GOLEM_FURNACE_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 2);
    }

    @Override
    public List<GolemTrait> supportedTraits() {
        return traitRecipes().map(TraitSmeltingRecipe::trait).distinct().toList();
    }

    private Stream<TraitSmeltingRecipe> traitRecipes() {
        if (this.level == null) {
            return Stream.empty();
        }
        return this.level.registryAccess().lookup(TraitSmeltingRecipe.REGISTRY_KEY).stream()
                .flatMap(registry -> registry.listElements())
                .map(holder -> holder.value());
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return GolemAnchor.inFront();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ItemStack input = getStack(INPUT_SLOT);
        if (workSpeed() <= 0 || input.isEmpty()) {
            resetProgress();
            return;
        }
        ItemStack result;
        int work;
        TraitSmeltingRecipe traitRecipe = traitRecipes().filter(recipe -> hasActiveTrait(recipe.trait()) && recipe.input().test(input))
                .findFirst().orElse(null);
        if (traitRecipe != null) {
            result = traitRecipe.result().create();
            work = traitRecipe.work();
        } else {
            SingleRecipeInput recipeInput = new SingleRecipeInput(input);
            RecipeHolder<SmeltingRecipe> recipe = this.recipeCheck.getRecipeFor(recipeInput, level).orElse(null);
            if (recipe == null) {
                resetProgress();
                return;
            }
            result = recipe.value().assemble(recipeInput);
            work = recipe.value().cookingTime();
        }
        if (result.isEmpty() || !canOutput(OUTPUT_SLOT, result)) {
            resetProgress();
            return;
        }
        if (advanceProgress(work)) {
            consume(INPUT_SLOT, 1);
            addOutput(OUTPUT_SLOT, result);
        }
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return new AbstractWorkstationUI<>(this) {
            @Override
            protected UIElement createMachineArea() {
                return row(4).addChildren(slot(INPUT_SLOT), progressArrow(), outputSlot(OUTPUT_SLOT));
            }
        }.build(holder.player);
    }
}
