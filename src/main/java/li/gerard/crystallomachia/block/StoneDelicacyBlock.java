package li.gerard.crystallomachia.block;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class StoneDelicacyBlock extends AbstractWorkstationBlock {
    public StoneDelicacyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends AbstractWorkstationBlockEntity> blockEntityType() {
        return crystallomachia.STONE_DELICACY_BLOCK_ENTITY.get();
    }
}
