package li.gerard.crystallomachia.block;

import java.util.Map;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;

import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public abstract class AbstractWorkstationBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    protected final int golemSlots;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    public final ItemStacksResourceHandler inventory;

    @Persisted
    @DescSynced
    protected int progress;

    @Persisted
    @DescSynced
    protected int maxProgress;

    protected AbstractWorkstationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int golemSlots, int otherSlots) {
        super(type, pos, state);
        this.golemSlots = golemSlots;
        this.inventory = new ItemStacksResourceHandler(golemSlots + otherSlots) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return isItemValid(index, resource);
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return this.syncStorage;
    }

    protected boolean isItemValid(int slot, ItemResource resource) {
        return slot >= this.golemSlots || resource.getItem() instanceof GolemItem;
    }

    public abstract Map<GolemCapability, Integer> requirements();

    public abstract ModularUI createUI(BlockUIMenuType.BlockUIHolder holder);

    protected abstract void serverTick(ServerLevel level);

    public int golemSlots() {
        return this.golemSlots;
    }

    public ItemStack getStack(int slot) {
        return this.inventory.getResource(slot).toStack(this.inventory.getAmountAsInt(slot));
    }

    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, ItemResource.of(stack), stack.getCount());
    }

    public GolemData golem(int slot) {
        return GolemItem.getData(getStack(slot));
    }

    public int capabilityLevel(GolemCapability capability) {
        int level = 0;
        for (int slot = 0; slot < this.golemSlots; slot++) {
            level += golem(slot).level(capability);
        }
        return level;
    }

    public boolean meetsRequirements() {
        for (Map.Entry<GolemCapability, Integer> requirement : requirements().entrySet()) {
            if (capabilityLevel(requirement.getKey()) < requirement.getValue()) {
                return false;
            }
        }
        return true;
    }

    public int workSpeed() {
        if (!meetsRequirements()) {
            return 0;
        }
        int speed = 1;
        for (Map.Entry<GolemCapability, Integer> requirement : requirements().entrySet()) {
            speed += capabilityLevel(requirement.getKey()) - requirement.getValue();
        }
        return speed;
    }

    public int speedBonusPercent() {
        return Math.max(0, workSpeed() - 1) * 100;
    }

    public float progressFraction() {
        return this.maxProgress <= 0 ? 0f : Math.min(1f, (float) this.progress / this.maxProgress);
    }

    protected boolean advanceProgress(int duration) {
        this.maxProgress = duration;
        this.progress += workSpeed();
        setChanged();
        if (this.progress >= this.maxProgress) {
            this.progress = 0;
            return true;
        }
        return false;
    }

    protected void resetProgress() {
        if (this.progress != 0) {
            this.progress = 0;
            setChanged();
        }
    }

    protected boolean canOutput(int slot, ItemStack result) {
        ItemStack output = getStack(slot);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result) && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    protected void addOutput(int slot, ItemStack result) {
        ItemStack output = getStack(slot);
        if (output.isEmpty()) {
            setStack(slot, result.copy());
        } else {
            output.grow(result.getCount());
            setStack(slot, output);
        }
    }

    protected void consume(int slot, int amount) {
        ItemStack stack = getStack(slot);
        stack.shrink(amount);
        setStack(slot, stack);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.inventory.copyToList());
        }
    }
}
