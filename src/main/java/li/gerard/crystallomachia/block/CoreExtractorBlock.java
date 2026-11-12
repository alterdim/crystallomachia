package li.gerard.crystallomachia.block;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class CoreExtractorBlock extends AbstractWorkstationBlock {
    public CoreExtractorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntityType<? extends AbstractWorkstationBlockEntity> blockEntityType() {
        return crystallomachia.CORE_EXTRACTOR_BLOCK_ENTITY.get();
    }
}
