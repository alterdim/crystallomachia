package li.gerard.crystallomachia.client;

import li.gerard.crystallomachia.golem.CapabilityTooltip;
import li.gerard.crystallomachia.golem.GolemCapability;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class ClientCapabilityTooltip implements ClientTooltipComponent {
    private static final int LINE_HEIGHT = GolemCapability.ICON_SIZE + 1;
    private static final int TEXT_OFFSET = GolemCapability.ICON_SIZE + 3;

    private final CapabilityTooltip tooltip;

    public ClientCapabilityTooltip(CapabilityTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight(Font font) {
        return this.tooltip.lines().size() * LINE_HEIGHT + 1;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            width = Math.max(width, TEXT_OFFSET + font.width(line.text()));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int lineY = y;
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            graphics.blit(line.icon(), x, lineY, x + GolemCapability.ICON_SIZE, lineY + GolemCapability.ICON_SIZE, 0f, 1f, 0f, 1f);
            graphics.text(font, line.text(), x + TEXT_OFFSET, lineY + 2, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }
}
