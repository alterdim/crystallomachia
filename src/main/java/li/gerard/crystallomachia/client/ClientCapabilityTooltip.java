package li.gerard.crystallomachia.client;

import java.util.List;

import li.gerard.crystallomachia.golem.CapabilityTooltip;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public class ClientCapabilityTooltip implements ClientTooltipComponent {
    private static final int LINE_HEIGHT = GolemCapability.ICON_SIZE + 1;
    private static final int TEXT_OFFSET = GolemCapability.ICON_SIZE + 3;
    private static final int STAT_HEIGHT = GolemStat.ICON_SIZE + 3;
    private static final int ROW_GAP = 6;

    private final CapabilityTooltip tooltip;

    public ClientCapabilityTooltip(CapabilityTooltip tooltip) {
        this.tooltip = tooltip;
    }

    private boolean showStats() {
        return !this.tooltip.stats().isEmpty() && Minecraft.getInstance().hasShiftDown();
    }

    private boolean showCapabilities() {
        return !this.tooltip.capabilities().isEmpty();
    }

    private static int rowWidth(Font font, List<CapabilityTooltip.Line> entries, int iconSize) {
        int width = 0;
        for (CapabilityTooltip.Line entry : entries) {
            width += iconSize + 2 + font.width(entry.text()) + ROW_GAP;
        }
        return Math.max(0, width - ROW_GAP);
    }

    private static void extractRow(Font font, GuiGraphicsExtractor graphics, List<CapabilityTooltip.Line> entries, int iconSize, int x, int y) {
        int entryX = x;
        int textY = y + (iconSize - 7) / 2;
        for (CapabilityTooltip.Line entry : entries) {
            graphics.blit(entry.icon(), entryX, y, entryX + iconSize, y + iconSize, 0f, 1f, 0f, 1f);
            graphics.text(font, entry.text(), entryX + iconSize + 2, textY, 0xFFFFFFFF);
            entryX += iconSize + 2 + font.width(entry.text()) + ROW_GAP;
        }
    }

    @Override
    public int getHeight(Font font) {
        int height = this.tooltip.lines().size() * LINE_HEIGHT;
        if (showStats()) {
            height += STAT_HEIGHT;
        }
        if (showCapabilities()) {
            height += LINE_HEIGHT;
        }
        return height == 0 ? 0 : height + 1;
    }

    @Override
    public int getWidth(Font font) {
        int width = showStats() ? rowWidth(font, this.tooltip.stats(), GolemStat.ICON_SIZE) : 0;
        width = Math.max(width, rowWidth(font, this.tooltip.capabilities(), GolemCapability.ICON_SIZE));
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            width = Math.max(width, TEXT_OFFSET + font.width(line.text()));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int lineY = y;
        if (showStats()) {
            extractRow(font, graphics, this.tooltip.stats(), GolemStat.ICON_SIZE, x, lineY);
            lineY += STAT_HEIGHT;
        }
        if (showCapabilities()) {
            extractRow(font, graphics, this.tooltip.capabilities(), GolemCapability.ICON_SIZE, x, lineY);
            lineY += LINE_HEIGHT;
        }
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            graphics.blit(line.icon(), x, lineY, x + GolemCapability.ICON_SIZE, lineY + GolemCapability.ICON_SIZE, 0f, 1f, 0f, 1f);
            graphics.text(font, line.text(), x + TEXT_OFFSET, lineY + 2, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }
}
