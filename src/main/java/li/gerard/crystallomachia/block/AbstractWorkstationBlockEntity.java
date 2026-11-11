package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;

import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.WorkEffect;
import net.minecraft.world.phys.Vec3;
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
    public static Consumer<AbstractWorkstationBlockEntity> CLIENT_TICK = workstation -> {};

    protected final List<GolemRole> golemRoles;
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

    protected AbstractWorkstationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<GolemRole> golemRoles, int otherSlots) {
        super(type, pos, state);
        this.golemRoles = List.copyOf(golemRoles);
        this.golemSlots = this.golemRoles.size();
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

    public GolemRole golemRole(int slot) {
        return this.golemRoles.get(slot);
    }

    public abstract GolemAnchor golemAnchor(int slot);

    public boolean isWorking() {
        return this.progress > 0;
    }

    public List<WorkEffect> workEffects(int slot) {
        List<WorkEffect> effects = new ArrayList<>();
        for (GolemCapability capability : golemRole(slot).capabilities()) {
            effects.addAll(WorkEffect.forCapability(capability));
        }
        return effects;
    }

    public Vec3 effectPosition(int slot, WorkEffect.Point point) {
        double right = point.right();
        double up = point.up();
        double front = point.front();
        if (point.origin() == WorkEffect.Origin.GOLEM) {
            GolemAnchor anchor = golemAnchor(slot);
            right += anchor.right();
            up += anchor.up();
            front += anchor.front();
        }
        double angle = Math.toRadians(switch (getBlockState().getValue(AbstractWorkstationBlock.FACING)) {
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        });
        double x = right * Math.cos(angle) - front * Math.sin(angle);
        double z = -right * Math.sin(angle) - front * Math.cos(angle);
        BlockPos pos = getBlockPos();
        return new Vec3(pos.getX() + 0.5 + x, pos.getY() + up, pos.getZ() + 0.5 + z);
    }

    public void clientTick() {
        CLIENT_TICK.accept(this);
    }

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

    public boolean meetsRequirements() {
        for (int slot = 0; slot < this.golemSlots; slot++) {
            if (!golemRole(slot).isMetBy(golem(slot))) {
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
        for (int slot = 0; slot < this.golemSlots; slot++) {
            speed += golemRole(slot).surplus(golem(slot));
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
