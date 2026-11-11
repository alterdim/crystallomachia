package li.gerard.crystallomachia.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemData;
import li.gerard.crystallomachia.golem.GolemItem;
import li.gerard.crystallomachia.golem.GolemMaterial;
import li.gerard.crystallomachia.golem.GolemPart;
import li.gerard.crystallomachia.golem.GolemParts;
import net.minecraft.resources.ResourceKey;
import li.gerard.crystallomachia.golem.GolemPartItem;
import li.gerard.crystallomachia.golem.GolemRole;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class GolemAssemblerBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int FIRST_PART_SLOT = 1;
    public static final int OUTPUT_SLOT = FIRST_PART_SLOT + GolemPart.values().length;
    private static final int ASSEMBLY_TIME = 200;
    private static final List<GolemRole> GOLEM_ROLES = List.of(GolemRole.of(GolemCapability.HANDWORK, 1));

    public GolemAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.GOLEM_ASSEMBLER_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, GolemPart.values().length + 1);
    }

    public static int partSlot(GolemPart part) {
        return FIRST_PART_SLOT + part.ordinal();
    }

    @Override
    protected boolean isItemValid(int slot, ItemResource resource) {
        if (slot >= FIRST_PART_SLOT && slot < OUTPUT_SLOT) {
            GolemPart part = GolemPart.values()[slot - FIRST_PART_SLOT];
            return resource.getItem() instanceof GolemPartItem item && item.part() == part;
        }
        return super.isItemValid(slot, resource);
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return GolemAnchor.inFront();
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (workSpeed() <= 0 || !hasAllParts() || !getStack(OUTPUT_SLOT).isEmpty()) {
            resetProgress();
            return;
        }
        if (advanceProgress(ASSEMBLY_TIME)) {
            ItemStack golem = GolemItem.create(assemble(level.getRandom()), partMaterials());
            for (GolemPart part : GolemPart.values()) {
                consume(partSlot(part), 1);
            }
            addOutput(OUTPUT_SLOT, golem);
        }
    }

    private boolean hasAllParts() {
        for (GolemPart part : GolemPart.values()) {
            if (getStack(partSlot(part)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private GolemParts partMaterials() {
        Map<GolemPart, ResourceKey<GolemMaterial>> materials = new EnumMap<>(GolemPart.class);
        for (GolemPart part : GolemPart.values()) {
            ResourceKey<GolemMaterial> material = getStack(partSlot(part)).get(crystallomachia.GOLEM_MATERIAL.get());
            if (material != null) {
                materials.put(part, material);
            }
        }
        return new GolemParts(materials);
    }

    protected GolemData assemble(RandomSource random) {
        Map<GolemCapability, Integer> capabilities = new EnumMap<>(GolemCapability.class);
        for (GolemPart part : GolemPart.values()) {
            GolemData contribution = GolemItem.getData(getStack(partSlot(part)));
            contribution.capabilities().forEach((capability, level) -> capabilities.merge(capability, level, Integer::sum));
        }
        return new GolemData(capabilities, random.nextLong());
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return new AbstractWorkstationUI<>(this) {
            @Override
            protected UIElement createMachineArea() {
                UIElement parts = column(2).addChildren(
                        partSlot(GolemPart.HEAD),
                        row(2).addChildren(partSlot(GolemPart.HAND_LEFT), partSlot(GolemPart.TORSO), partSlot(GolemPart.HAND_RIGHT)),
                        partSlot(GolemPart.LEGS));
                return row(4).addChildren(parts, progressArrow(), outputSlot(OUTPUT_SLOT));
            }

            private UIElement partSlot(GolemPart part) {
                return slot(GolemAssemblerBlockEntity.partSlot(part)).style(style -> style.tooltips(part.displayName()));
            }
        }.build(holder.player);
    }
}
