package li.gerard.crystallomachia.client;

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
    private static final int STAT_TEXT_OFFSET = GolemStat.ICON_SIZE + 2;
    private static final int STAT_GAP = 6;

    private final CapabilityTooltip tooltip;

    public ClientCapabilityTooltip(CapabilityTooltip tooltip) {
        this.tooltip = tooltip;
    }

    private boolean showStats() {
        return !this.tooltip.stats().isEmpty() && Minecraft.getInstance().hasShiftDown();
    }

    private int statsWidth(Font font) {
        int width = 0;
        for (CapabilityTooltip.Line stat : this.tooltip.stats()) {
            width += STAT_TEXT_OFFSET + font.width(stat.text()) + STAT_GAP;
        }
        return width - STAT_GAP;
    }

    @Override
    public int getHeight(Font font) {
        int height = this.tooltip.lines().size() * LINE_HEIGHT;
        if (showStats()) {
            height += STAT_HEIGHT;
        }
        return height == 0 ? 0 : height + 1;
    }

    @Override
    public int getWidth(Font font) {
        int width = showStats() ? statsWidth(font) : 0;
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            width = Math.max(width, TEXT_OFFSET + font.width(line.text()));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int lineY = y;
        if (showStats()) {
            int statX = x;
            for (CapabilityTooltip.Line stat : this.tooltip.stats()) {
                graphics.blit(stat.icon(), statX, lineY, statX + GolemStat.ICON_SIZE, lineY + GolemStat.ICON_SIZE, 0f, 1f, 0f, 1f);
                graphics.text(font, stat.text(), statX + STAT_TEXT_OFFSET, lineY + 1, 0xFFFFFFFF);
                statX += STAT_TEXT_OFFSET + font.width(stat.text()) + STAT_GAP;
            }
            lineY += STAT_HEIGHT;
        }
        for (CapabilityTooltip.Line line : this.tooltip.lines()) {
            graphics.blit(line.icon(), x, lineY, x + GolemCapability.ICON_SIZE, lineY + GolemCapability.ICON_SIZE, 0f, 1f, 0f, 1f);
            graphics.text(font, line.text(), x + TEXT_OFFSET, lineY + 2, 0xFFFFFFFF);
            lineY += LINE_HEIGHT;
        }
    }
}
