package li.gerard.crystallomachia.block;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class GolemFurnaceBlock extends AbstractWorkstationBlock {
    public GolemFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends AbstractWorkstationBlockEntity> blockEntityType() {
        return crystallomachia.GOLEM_FURNACE_BLOCK_ENTITY.get();
    }
}
