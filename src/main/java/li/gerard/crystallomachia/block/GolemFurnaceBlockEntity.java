package li.gerard.crystallomachia.block;

import java.util.Map;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemCapability;
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
    private static final Map<GolemCapability, Integer> REQUIREMENTS = Map.of(GolemCapability.HEAT, 1);

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipeCheck = RecipeManager.createCheck(RecipeType.SMELTING);

    public GolemFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.GOLEM_FURNACE_BLOCK_ENTITY.get(), pos, state, 1, 2);
    }

    @Override
    public Map<GolemCapability, Integer> requirements() {
        return REQUIREMENTS;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ItemStack input = getStack(INPUT_SLOT);
        if (workSpeed() <= 0 || input.isEmpty()) {
            resetProgress();
            return;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        RecipeHolder<SmeltingRecipe> recipe = this.recipeCheck.getRecipeFor(recipeInput, level).orElse(null);
        if (recipe == null) {
            resetProgress();
            return;
        }
        ItemStack result = recipe.value().assemble(recipeInput);
        if (result.isEmpty() || !canOutput(OUTPUT_SLOT, result)) {
            resetProgress();
            return;
        }
        if (advanceProgress(recipe.value().cookingTime())) {
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
