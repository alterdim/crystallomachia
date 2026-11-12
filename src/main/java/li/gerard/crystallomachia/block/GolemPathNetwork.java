package li.gerard.crystallomachia.block;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import li.gerard.crystallomachia.crystallomachia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class GolemPathNetwork {
    public static final int CLEARANCE = 2;
    public static final int MAX_TILES = 512;
    private static final float MAX_STEP = 0.5f;

    private GolemPathNetwork() {}

    private record Search(Map<BlockPos, Integer> stations, Map<BlockPos, BlockPos> stationTiles, Map<BlockPos, BlockPos> parents) {}

    public static boolean isPath(BlockState state) {
        Block block = state.getBlock();
        return block == crystallomachia.GOLEM_PATH.get() || block == crystallomachia.GOLEM_PATH_SLAB.get() || block == crystallomachia.GOLEM_PATH_STAIRS.get();
    }

    public static boolean isWalkable(Level level, BlockPos pos) {
        return level.isLoaded(pos) && isPath(level.getBlockState(pos)) && isClearAbove(level, pos);
    }

    public static boolean isClearAbove(BlockGetter level, BlockPos pos) {
        for (int dy = 1; dy <= CLEARANCE; dy++) {
            if (hasCollision(level, pos.above(dy))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCollision(BlockGetter level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static float surfaceHeight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        float height = 1f;
        if (state.getBlock() instanceof SlabBlock) {
            height = state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? 0.5f : 1f;
        } else if (state.getBlock() instanceof StairBlock) {
            height = state.getValue(StairBlock.HALF) == Half.BOTTOM ? 0.5f : 1f;
        }
        return pos.getY() + height;
    }

    public static Map<BlockPos, Integer> reachableStations(Level level, BlockPos origin) {
        return search(level, origin).stations();
    }

    public static List<Vec3> route(Level level, BlockPos origin, BlockPos station) {
        Search search = search(level, origin);
        LinkedList<Vec3> route = new LinkedList<>();
        BlockPos tile = search.stationTiles().get(station);
        while (tile != null) {
            route.addFirst(new Vec3(tile.getX() + 0.5, surfaceHeight(level, tile), tile.getZ() + 0.5));
            tile = search.parents().get(tile);
        }
        return route;
    }

    private static Search search(Level level, BlockPos origin) {
        Map<BlockPos, Integer> stations = new LinkedHashMap<>();
        Map<BlockPos, BlockPos> stationTiles = new HashMap<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        Map<BlockPos, Integer> distances = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos beside = origin.relative(direction);
            for (BlockPos tile : new BlockPos[] {beside.below(), beside}) {
                if (!distances.containsKey(tile) && isWalkable(level, tile)) {
                    distances.put(tile, 1);
                    queue.add(tile);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos tile = queue.poll();
            int distance = distances.get(tile);
            float height = surfaceHeight(level, tile);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos beside = tile.relative(direction);
                for (BlockPos station : new BlockPos[] {beside.above(), beside}) {
                    if (!station.equals(origin) && !stations.containsKey(station) && level.getBlockEntity(station) instanceof AbstractWorkstationBlockEntity) {
                        stations.put(station, distance);
                        stationTiles.put(station, tile);
                    }
                }
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos next = beside.above(dy);
                    if (distances.size() >= MAX_TILES || distances.containsKey(next) || !isWalkable(level, next)) {
                        continue;
                    }
                    if (Math.abs(surfaceHeight(level, next) - height) <= MAX_STEP) {
                        distances.put(next, distance + 1);
                        parents.put(next, tile);
                        queue.add(next);
                    }
                }
            }
        }
        return new Search(stations, stationTiles, parents);
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        BlockPos pos = event.getPos();
        BlockState placed = event.getPlacedBlock();
        boolean blocked = false;
        if (isPath(placed)) {
            blocked = !isClearAbove(event.getLevel(), pos);
        }
        if (!blocked && !placed.getCollisionShape(event.getLevel(), pos).isEmpty()) {
            for (int dy = 1; dy <= CLEARANCE && !blocked; dy++) {
                blocked = isPath(event.getLevel().getBlockState(pos.below(dy)));
            }
        }
        if (blocked) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player) {
                player.sendOverlayMessage(Component.translatable("message.crystallomachia.path_blocked", CLEARANCE));
            }
        }
    }
}
