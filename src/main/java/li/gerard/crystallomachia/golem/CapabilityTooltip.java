package li.gerard.crystallomachia.golem;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record CapabilityTooltip(List<Line> lines) implements TooltipComponent {
    public record Line(GolemCapability capability, Component text) {}
}
