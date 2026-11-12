package li.gerard.crystallomachia.block;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.CrudeProduct;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.recipe.ForgingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ForgeStationBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int FIRST_INPUT_SLOT = 2;
    public static final int SECOND_INPUT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final List<GolemRole> GOLEM_ROLES = List.of(
            GolemRole.of(GolemCapability.HEAT, 2),
            GolemRole.of(GolemCapability.HANDWORK, 2));
    public static final List<GolemRole> CRUDE_ROLES = List.of(
            GolemRole.of(GolemCapability.HEAT, 1),
            GolemRole.of(GolemCapability.HANDWORK, 1));
    private static final int[] INPUT_SLOTS = {FIRST_INPUT_SLOT, SECOND_INPUT_SLOT};

    @Persisted
    @DescSynced
    private int product;

    public ForgeStationBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.FORGE_STATION_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 3);
    }

    public @Nullable CrudeProduct product() {
        return this.product <= 0 || this.product > CrudeProduct.values().length ? null : CrudeProduct.values()[this.product - 1];
    }

    public void cycleProduct() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.product = (this.product + 1) % (CrudeProduct.values().length + 1);
        resetProgress();
        setChanged();
    }

    private Component productText() {
        CrudeProduct selected = product();
        return selected == null
                ? Component.translatable("gui.crystallomachia.forge.none")
                : Component.translatable("gui.crystallomachia.forge.crude", selected.displayName(), selected.cost());
    }

    @Override
    public GolemRole golemRole(int slot) {
        return product() == null ? super.golemRole(slot) : CRUDE_ROLES.get(slot);
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return slot == 0 ? GolemAnchor.onLeft() : GolemAnchor.onRight();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        CrudeProduct selected = product();
        if (selected != null) {
            tickProduct(level, selected);
            return;
        }
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

    private void tickProduct(ServerLevel level, CrudeProduct selected) {
        Holder.Reference<GolemMaterial> material = workSpeed() <= 0 || !getStack(OUTPUT_SLOT).isEmpty() ? null : findMaterial(level, selected.cost());
        if (material == null) {
            resetProgress();
            return;
        }
        if (advanceProgress(selected.work())) {
            consumeMaterial(material.value(), selected.cost());
            setStack(OUTPUT_SLOT, selected.create(material));
        }
    }

    private Holder.@Nullable Reference<GolemMaterial> findMaterial(ServerLevel level, int cost) {
        return level.registryAccess().lookupOrThrow(GolemMaterial.REGISTRY_KEY).listElements()
                .filter(material -> materialCount(material.value()) >= cost)
                .findFirst()
                .orElse(null);
    }

    private boolean isMaterial(GolemMaterial material, ItemStack stack) {
        return !stack.isEmpty() && material.ingredient().isPresent() && material.ingredient().get().test(stack);
    }

    private int materialCount(GolemMaterial material) {
        int count = 0;
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = getStack(slot);
            if (isMaterial(material, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void consumeMaterial(GolemMaterial material, int cost) {
        int remaining = cost;
        for (int slot : INPUT_SLOTS) {
            ItemStack stack = getStack(slot);
            if (remaining > 0 && isMaterial(material, stack)) {
                int taken = Math.min(remaining, stack.getCount());
                consume(slot, taken);
                remaining -= taken;
            }
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
                return column(4).addChildren(
                        row(4).addChildren(slot(FIRST_INPUT_SLOT), slot(SECOND_INPUT_SLOT), progressArrow(), outputSlot(OUTPUT_SLOT)),
                        row(4).addChildren(
                                new Button().setText(Component.translatable("gui.crystallomachia.forge.product")).setOnServerClick(event -> cycleProduct()),
                                new Label().bindDataSource(SupplierDataSource.of(ForgeStationBlockEntity.this::productText))));
            }
        }.build(holder.player);
    }
}
