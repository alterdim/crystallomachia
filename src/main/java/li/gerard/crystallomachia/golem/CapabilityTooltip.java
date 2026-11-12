package li.gerard.crystallomachia.golem;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record CapabilityTooltip(List<Line> stats, List<Line> capabilities, List<Line> lines) implements TooltipComponent {
    public CapabilityTooltip(List<Line> lines) {
        this(List.of(), List.of(), lines);
    }

    public record Line(Identifier icon, Component text) {
        public Line(GolemCapability capability, Component text) {
            this(capability.icon(), text);
        }

        public Line(GolemTrait trait, Component text) {
            this(trait.icon(), text);
        }
    }
}
