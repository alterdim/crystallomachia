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
import li.gerard.crystallomachia.golem.GolemHunger;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemPose;
import org.jspecify.annotations.Nullable;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemTrait;
import li.gerard.crystallomachia.golem.WorkEffect;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

    private static final int HUNGER_INTERVAL = 20;

    protected final List<GolemRole> golemRoles;
    protected final int golemSlots;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    public final ItemStacksResourceHandler inventory;

    @Persisted
    @DescSynced
    protected float progress;

    @Persisted
    @DescSynced
    protected float maxProgress;

    private int hungerTicks;

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

    public List<GolemTrait> supportedTraits() {
        return List.of();
    }

    public boolean hasActiveTrait(GolemTrait trait) {
        if (!supportedTraits().contains(trait)) {
            return false;
        }
        for (int slot = 0; slot < this.golemSlots; slot++) {
            if (golem(slot).hasTrait(trait)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWorking() {
        return this.progress > 0;
    }

    public @Nullable Component golemSpeech(int slot) {
        if (isStarving(slot)) {
            return Component.translatable("speech.crystallomachia.hungry");
        }
        if (!isWorking() || !isRequired(slot)) {
            return null;
        }
        List<GolemCapability> capabilities = golemRole(slot).capabilities();
        return capabilities.isEmpty() ? null : Component.translatable("speech.crystallomachia.work." + capabilities.getFirst().getSerializedName());
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
        if (point.origin() != WorkEffect.Origin.WORKSTATION) {
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

    public @Nullable GolemPose golemPose(int slot, float partialTick) {
        return null;
    }

    public double renderReach() {
        return 2;
    }

    public void playEatEffects(int slot) {
        if (this.level instanceof ServerLevel serverLevel) {
            GolemHunger.playEatEffects(serverLevel, effectPosition(slot, WorkEffect.Point.golem(0, 1.5, 0)));
        }
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

    protected boolean isRequired(int slot) {
        return true;
    }

    public boolean isStarving(int slot) {
        ItemStack stack = getStack(slot);
        return stack.getItem() instanceof GolemItem && !GolemHunger.of(stack).isFed();
    }

    public boolean hasStarvingGolem() {
        for (int slot = 0; slot < this.golemSlots; slot++) {
            if (isRequired(slot) && isStarving(slot)) {
                return true;
            }
        }
        return false;
    }

    public float golemSpeed(int slot) {
        return GolemHunger.workSpeed(getStack(slot));
    }

    public boolean meetsRequirements() {
        for (int slot = 0; slot < this.golemSlots; slot++) {
            if (isRequired(slot) && (!golemRole(slot).isMetBy(golem(slot)) || isStarving(slot))) {
                return false;
            }
        }
        return true;
    }

    public float workSpeed() {
        if (!meetsRequirements()) {
            return 0f;
        }
        float speed = 0f;
        for (int slot = 0; slot < this.golemSlots; slot++) {
            if (isRequired(slot)) {
                speed += golemSpeed(slot);
            }
        }
        return speed;
    }

    public float progressFraction() {
        return this.maxProgress <= 0 ? 0f : Math.min(1f, this.progress / this.maxProgress);
    }

    protected boolean advanceProgress(float work) {
        this.maxProgress = work;
        this.progress += workSpeed();
        if (++this.hungerTicks >= HUNGER_INTERVAL) {
            this.hungerTicks = 0;
            for (int slot = 0; slot < this.golemSlots; slot++) {
                ItemStack stack = getStack(slot);
                if (isRequired(slot) && stack.getItem() instanceof GolemItem) {
                    GolemHunger.work(stack, HUNGER_INTERVAL);
                    setStack(slot, stack);
                }
            }
        }
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
