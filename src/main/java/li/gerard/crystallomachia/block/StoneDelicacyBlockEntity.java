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
import li.gerard.crystallomachia.recipe.CookingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class StoneDelicacyBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int COOK_SLOT = 0;
    public static final int TRANSPORT_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final GolemRole COOK_ROLE = GolemRole.of(GolemCapability.COOKING, 1);
    public static final GolemRole TRANSPORT_ROLE = GolemRole.of(GolemCapability.TRANSPORT, 1);
    public static final List<GolemRole> GOLEM_ROLES = List.of(COOK_ROLE, TRANSPORT_ROLE);

    public StoneDelicacyBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.STONE_DELICACY_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 2);
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return slot == COOK_SLOT ? GolemAnchor.inFront() : GolemAnchor.onRight();
    }

    @Override
    protected boolean isRequired(int slot) {
        return slot == COOK_SLOT;
    }

    public boolean hasTransportGolem() {
        return TRANSPORT_ROLE.isMetBy(golem(TRANSPORT_SLOT));
    }

    @Override
    protected void serverTick(ServerLevel level) {
        CookingRecipe recipe = workSpeed() <= 0 ? null : findRecipe(level, getStack(INPUT_SLOT));
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
            consume(INPUT_SLOT, 1);
            addOutput(OUTPUT_SLOT, result);
        }
    }

    private static @Nullable CookingRecipe findRecipe(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        return level.registryAccess().lookupOrThrow(CookingRecipe.REGISTRY_KEY).listElements()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.input().test(input))
                .findFirst()
                .orElse(null);
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
