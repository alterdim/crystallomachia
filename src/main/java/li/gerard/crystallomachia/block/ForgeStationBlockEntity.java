package li.gerard.crystallomachia.block;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemRole;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import li.gerard.crystallomachia.recipe.ForgingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ForgeStationBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int FIRST_INPUT_SLOT = 2;
    public static final int SECOND_INPUT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final List<GolemRole> GOLEM_ROLES = List.of(
            GolemRole.of(GolemCapability.HEAT, 2),
            GolemRole.of(GolemCapability.HANDWORK, 2));

    public ForgeStationBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.FORGE_STATION_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 3);
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return slot == 0 ? GolemAnchor.onLeft() : GolemAnchor.onRight();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        ForgingRecipe recipe = workSpeed() <= 0 ? null : findRecipe(level, getStack(FIRST_INPUT_SLOT), getStack(SECOND_INPUT_SLOT));
        if (recipe == null) {
            resetProgress();
            return;
        }
        ItemStack result = recipe.result().create();
        if (!canOutput(OUTPUT_SLOT, result)) {
            resetProgress();
            return;
        }
        if (advanceProgress(recipe.work())) {
            consume(FIRST_INPUT_SLOT, 1);
            consume(SECOND_INPUT_SLOT, 1);
            addOutput(OUTPUT_SLOT, result);
        }
    }

    private static @Nullable ForgingRecipe findRecipe(ServerLevel level, ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return null;
        }
        return level.registryAccess().lookupOrThrow(ForgingRecipe.REGISTRY_KEY).listElements()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.matches(first, second))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return new AbstractWorkstationUI<>(this) {
            @Override
            protected UIElement createMachineArea() {
                return row(4).addChildren(slot(FIRST_INPUT_SLOT), slot(SECOND_INPUT_SLOT), progressArrow(), outputSlot(OUTPUT_SLOT));
            }
        }.build(holder.player);
    }
}
