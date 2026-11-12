package li.gerard.crystallomachia.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;

import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemHunger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import li.gerard.crystallomachia.golem.GolemItem;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class DiscoBallBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    public static final int GOLEM_SLOTS = 5;
    public static final float SHARD_WORK = 600f;
    private static final int FLOOR_CHECK_INTERVAL = 20;
    private static final int HUNGER_INTERVAL = 20;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    public final ItemStacksResourceHandler inventory;

    @Persisted
    private float progress;

    private int floorDistance;
    private int floorCheckCooldown;
    private int hungerTicks;

    public DiscoBallBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.DISCO_BALL_BLOCK_ENTITY.get(), pos, state);
        this.inventory = new ItemStacksResourceHandler(GOLEM_SLOTS) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return isPartyGolem(resource.toStack());
            }

            @Override
            protected void onContentsChanged(int index, ItemStack previousContents) {
                setChanged();
            }
        };
    }

    public static boolean isPartyGolem(ItemStack stack) {
        return stack.getItem() instanceof GolemItem && GolemItem.getData(stack).has(GolemCapability.PARTY, 1);
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return this.syncStorage;
    }

    public ItemStack getStack(int slot) {
        return this.inventory.getResource(slot).toStack(this.inventory.getAmountAsInt(slot));
    }

    public GolemData golem(int slot) {
        return GolemItem.getData(getStack(slot));
    }

    public @Nullable Component golemSpeech(int slot) {
        ItemStack stack = getStack(slot);
        if (!isPartyGolem(stack)) {
            return null;
        }
        if (!GolemHunger.of(stack).isFed()) {
            return Component.translatable("speech.crystallomachia.hungry");
        }
        return this.floorDistance > 0 ? Component.translatable("speech.crystallomachia.work.party") : null;
    }

    public int floorDistance() {
        return this.floorDistance;
    }

    private void updateFloor() {
        if (this.level != null && this.floorCheckCooldown-- <= 0) {
            this.floorCheckCooldown = FLOOR_CHECK_INTERVAL;
            this.floorDistance = DancefloorBlock.floorDistance(this.level, this.worldPosition);
        }
    }

    public void clientTick() {
        updateFloor();
    }

    public void serverTick(ServerLevel level) {
        updateFloor();
        if (this.floorDistance == 0) {
            return;
        }
        float speed = 0f;
        for (int slot = 0; slot < GOLEM_SLOTS; slot++) {
            ItemStack stack = getStack(slot);
            if (isPartyGolem(stack)) {
                speed += GolemHunger.workSpeed(stack);
            }
        }
        if (speed > 0 && ++this.hungerTicks >= HUNGER_INTERVAL) {
            this.hungerTicks = 0;
            for (int slot = 0; slot < GOLEM_SLOTS; slot++) {
                ItemStack stack = getStack(slot);
                if (isPartyGolem(stack) && GolemHunger.of(stack).isFed()) {
                    GolemHunger.work(stack, HUNGER_INTERVAL);
                    this.inventory.set(slot, ItemResource.of(stack), stack.getCount());
                }
            }
        }
        if (speed <= 0) {
            return;
        }
        this.progress += speed;
        setChanged();
        if (this.progress >= SHARD_WORK) {
            this.progress = 0;
            spawnShard(level);
        }
    }

    private void spawnShard(ServerLevel level) {
        RandomSource random = level.getRandom();
        double span = DancefloorBlock.RADIUS * 2 + 1;
        BlockPos floor = this.worldPosition.below(this.floorDistance);
        double x = floor.getX() + 0.5 + (random.nextDouble() - 0.5) * span;
        double z = floor.getZ() + 0.5 + (random.nextDouble() - 0.5) * span;
        ItemEntity shard = new ItemEntity(level, x, floor.getY() + 1.25, z, new ItemStack(crystallomachia.FIESTA_SHARD.get()));
        shard.setDefaultPickUpDelay();
        level.addFreshEntity(shard);
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        UIElement slots = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));
        slots.addChild(new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE))
                .style(style -> style.backgroundTexture(SpriteTexture.of(GolemCapability.PARTY.icon().toString()))
                        .tooltips(GolemCapability.PARTY.displayName())));
        for (int slot = 0; slot < GOLEM_SLOTS; slot++) {
            slots.addChild(new ItemSlot().bind(this.inventory, slot));
        }
        UIElement root = new UIElement().layout(layout -> layout.paddingAll(6).gapAll(4).alignItems(AlignItems.CENTER));
        root.addChildren(
                new Label().setText(Component.translatable("gui.crystallomachia.dancefloor")),
                slots,
                new InventorySlots());
        root.addClass("panel_bg");
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC)), holder.player);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.inventory.copyToList());
        }
    }
}
