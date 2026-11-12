package li.gerard.crystallomachia.block;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class DancefloorBlock extends Block {
    public static final int RADIUS = 4;
    public static final int MAX_BALL_HEIGHT = 10;

    public DancefloorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos ball = findDiscoBall(level, pos);
        if (ball == null) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, ball);
        }
        return InteractionResult.SUCCESS;
    }

    public static @Nullable BlockPos findDiscoBall(Level level, BlockPos floorPos) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                BlockPos center = floorPos.offset(dx, 0, dz);
                for (int dy = 1; dy <= MAX_BALL_HEIGHT; dy++) {
                    BlockPos ball = center.above(dy);
                    if (level.getBlockState(ball).getBlock() instanceof DiscoBallBlock && isFloor(level, center)) {
                        return ball;
                    }
                }
            }
        }
        return null;
    }

    public static boolean hasFloorBelow(Level level, BlockPos ball) {
        return floorDistance(level, ball) > 0;
    }

    public static int floorDistance(Level level, BlockPos ball) {
        for (int dy = 1; dy <= MAX_BALL_HEIGHT; dy++) {
            if (isFloor(level, ball.below(dy))) {
                return dy;
            }
        }
        return 0;
    }

    public static boolean isFloor(Level level, BlockPos center) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (!(level.getBlockState(center.offset(dx, 0, dz)).getBlock() instanceof DancefloorBlock)) {
                    return false;
                }
            }
        }
        return true;
    }
}
