package li.gerard.crystallomachia.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;

import li.gerard.crystallomachia.crystallomachia;
import li.gerard.crystallomachia.golem.GolemAnchor;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemFoodItem;
import li.gerard.crystallomachia.golem.GolemHunger;
import li.gerard.crystallomachia.golem.GolemPose;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.WorkEffect;
import li.gerard.crystallomachia.recipe.CookingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class StoneDelicacyBlockEntity extends AbstractWorkstationBlockEntity {
    public static final int COOK_SLOT = 0;
    public static final int TRANSPORT_SLOT = 1;
    public static final int INPUT_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final GolemRole COOK_ROLE = GolemRole.of(GolemCapability.COOKING, 1);
    public static final GolemRole TRANSPORT_ROLE = GolemRole.of(GolemCapability.TRANSPORT, 1);
    public static final List<GolemRole> GOLEM_ROLES = List.of(COOK_ROLE, TRANSPORT_ROLE);
    public static final float WORK_PER_BLOCK = 5f;
    public static final int MEALS_PER_TRIP = 4;
    private static final int DISPATCH_INTERVAL = 100;
    private static final int HUNGER_INTERVAL = 20;
    private static final double TRIP_RENDER_REACH = 96;

    @DescSynced
    private int reachableStations;

    @DescSynced
    private boolean onTrip;

    @DescSynced
    private long tripTarget;

    @DescSynced
    private float tripLegWork;

    @DescSynced
    private float tripWork;

    private boolean tripDelivered;
    private int dispatchCooldown;
    private int transportHungerTicks;

    private List<Vec3> route = List.of();
    private long routeTarget;
    private boolean routeKnown;

    public StoneDelicacyBlockEntity(BlockPos pos, BlockState state) {
        super(crystallomachia.STONE_DELICACY_BLOCK_ENTITY.get(), pos, state, GOLEM_ROLES, 2);
    }

    @Override
    public GolemAnchor golemAnchor(int slot) {
        return slot == COOK_SLOT ? GolemAnchor.inFront() : GolemAnchor.onRight();
    }

    @Override
    protected boolean isRequired(int slot) {
        return slot == COOK_SLOT;
    }

    public boolean hasTransportGolem() {
        return TRANSPORT_ROLE.isMetBy(golem(TRANSPORT_SLOT));
    }

    public int reachableStations() {
        return this.reachableStations;
    }

    @Override
    public @Nullable Component golemSpeech(int slot) {
        if (slot == TRANSPORT_SLOT && this.onTrip && this.tripWork < this.tripLegWork && !isStarving(slot)) {
            return Component.translatable("speech.crystallomachia.carrying_food");
        }
        return super.golemSpeech(slot);
    }

    @Override
    public double renderReach() {
        return this.onTrip ? TRIP_RENDER_REACH : super.renderReach();
    }

    @Override
    public @Nullable GolemPose golemPose(int slot, float partialTick) {
        if (slot != TRANSPORT_SLOT || !this.onTrip || this.tripLegWork <= 0f) {
            this.routeKnown = this.routeKnown && this.onTrip;
            return null;
        }
        List<Vec3> points = route();
        if (points.size() < 2) {
            return null;
        }
        float work = this.tripWork + partialTick * GolemHunger.workSpeed(getStack(TRANSPORT_SLOT));
        float legs = Mth.clamp(work / this.tripLegWork, 0f, 2f);
        boolean returning = legs > 1f;
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i).distanceTo(points.get(i - 1));
        }
        double travelled = total * (returning ? 2f - legs : legs);
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            double length = from.distanceTo(to);
            if (travelled <= length || i == points.size() - 1) {
                Vec3 position = length <= 0 ? to : from.lerp(to, Math.min(1.0, travelled / length));
                Vec3 heading = returning ? from.subtract(to) : to.subtract(from);
                return new GolemPose(position, (float) Math.toDegrees(Math.atan2(-heading.x, -heading.z)));
            }
            travelled -= length;
        }
        return null;
    }

    private List<Vec3> route() {
        if (this.level == null) {
            return List.of();
        }
        if (!this.routeKnown || this.routeTarget != this.tripTarget) {
            List<Vec3> points = new ArrayList<>();
            List<Vec3> tiles = GolemPathNetwork.route(this.level, this.worldPosition, BlockPos.of(this.tripTarget));
            if (!tiles.isEmpty()) {
                points.add(effectPosition(TRANSPORT_SLOT, WorkEffect.Point.golem(0, 0, 0)));
                points.addAll(tiles);
            }
            this.route = points;
            this.routeTarget = this.tripTarget;
            this.routeKnown = true;
        }
        return this.route;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        tickDelivery(level);
        tickCooking(level);
    }

    private void tickCooking(ServerLevel level) {
        CookingRecipe recipe = workSpeed() <= 0 ? null : findRecipe(level, getStack(INPUT_SLOT));
        if (recipe == null) {
            resetProgress();
            return;
        }
        ItemStack result = recipe.result().create();
        if (!canOutput(OUTPUT_SLOT, result)) {
            resetProgress();
            return;
        }
        if (advanceProgress(recipe.work())) {
            consume(INPUT_SLOT, 1);
            addOutput(OUTPUT_SLOT, result);
        }
    }

    private void tickDelivery(ServerLevel level) {
        if (this.onTrip) {
            tickTrip(level);
            return;
        }
        if (++this.dispatchCooldown < DISPATCH_INTERVAL) {
            return;
        }
        this.dispatchCooldown = 0;
        feedGolems(this, MEALS_PER_TRIP);
        if (!hasTransportGolem()) {
            setReachableStations(0);
            return;
        }
        Map<BlockPos, Integer> stations = GolemPathNetwork.reachableStations(level, this.worldPosition);
        setReachableStations(stations.size());
        GolemFoodItem meal = meal();
        if (meal == null || GolemHunger.workSpeed(getStack(TRANSPORT_SLOT)) <= 0) {
            return;
        }
        BlockPos nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Map.Entry<BlockPos, Integer> station : stations.entrySet()) {
            if (station.getValue() < nearestDistance && level.getBlockEntity(station.getKey()) instanceof AbstractWorkstationBlockEntity workstation
                    && wantsMeal(workstation, meal)) {
                nearest = station.getKey();
                nearestDistance = station.getValue();
            }
        }
        if (nearest != null) {
            this.onTrip = true;
            this.tripTarget = nearest.asLong();
            this.tripLegWork = nearestDistance * WORK_PER_BLOCK;
            this.tripWork = 0f;
            this.tripDelivered = false;
            setChanged();
        }
    }

    private void tickTrip(ServerLevel level) {
        ItemStack transport = getStack(TRANSPORT_SLOT);
        float speed = hasTransportGolem() ? GolemHunger.workSpeed(transport) : 0f;
        if (speed <= 0) {
            endTrip();
            return;
        }
        this.tripWork += speed;
        setChanged();
        if (++this.transportHungerTicks >= HUNGER_INTERVAL) {
            this.transportHungerTicks = 0;
            GolemHunger.work(transport, HUNGER_INTERVAL);
            setStack(TRANSPORT_SLOT, transport);
        }
        if (!this.tripDelivered && this.tripWork >= this.tripLegWork) {
            this.tripDelivered = true;
            if (level.getBlockEntity(BlockPos.of(this.tripTarget)) instanceof AbstractWorkstationBlockEntity workstation) {
                feedGolems(workstation, MEALS_PER_TRIP);
            }
        }
        if (this.tripWork >= this.tripLegWork * 2f) {
            endTrip();
        }
    }

    private void endTrip() {
        this.onTrip = false;
        this.tripWork = 0f;
        setChanged();
    }

    private void setReachableStations(int count) {
        if (this.reachableStations != count) {
            this.reachableStations = count;
            setChanged();
        }
    }

    private @Nullable GolemFoodItem meal() {
        return getStack(OUTPUT_SLOT).getItem() instanceof GolemFoodItem meal ? meal : null;
    }

    private static boolean wantsMeal(AbstractWorkstationBlockEntity workstation, GolemFoodItem meal) {
        for (int slot = 0; slot < workstation.golemSlots(); slot++) {
            if (GolemHunger.wantsMeal(workstation.getStack(slot), meal)) {
                return true;
            }
        }
        return false;
    }

    private void feedGolems(AbstractWorkstationBlockEntity workstation, int meals) {
        for (int slot = 0; slot < workstation.golemSlots() && meals > 0; slot++) {
            ItemStack golem = workstation.getStack(slot);
            boolean fed = false;
            while (meals > 0) {
                GolemFoodItem meal = meal();
                if (meal == null || !GolemHunger.wantsMeal(golem, meal) || !GolemHunger.feed(golem, meal)) {
                    break;
                }
                consume(OUTPUT_SLOT, 1);
                meals--;
                fed = true;
            }
            if (fed) {
                workstation.setStack(slot, golem);
                workstation.playEatEffects(slot);
            }
        }
    }

    private static @Nullable CookingRecipe findRecipe(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) {
            return null;
        }
        return level.registryAccess().lookupOrThrow(CookingRecipe.REGISTRY_KEY).listElements()
                .map(holder -> holder.value())
                .filter(recipe -> recipe.input().test(input))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return new AbstractWorkstationUI<>(this) {
            @Override
            protected UIElement createMachineArea() {
                return column(4).addChildren(
                        row(4).addChildren(slot(INPUT_SLOT), progressArrow(), outputSlot(OUTPUT_SLOT)),
                        new Label().bindDataSource(SupplierDataSource.of(
                                () -> Component.translatable("gui.crystallomachia.kitchen.stations", reachableStations()))));
            }
        }.build(holder.player);
    }
}
