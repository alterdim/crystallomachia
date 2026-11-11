package li.gerard.crystallomachia.compat.jei;

import java.util.List;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import li.gerard.crystallomachia.golem.GolemCapability;
import li.gerard.crystallomachia.golem.GolemRole;
import li.gerard.crystallomachia.golem.GolemTrait;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class JeiRecipeUI {
    private JeiRecipeUI() {}

    public static ModularUI page(UIElement... content) {
        UIElement root = row(6).layout(layout -> layout.widthPercent(100).heightPercent(100).justifyContent(AlignContent.CENTER));
        root.addChildren(content);
        return ModularUI.of(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))));
    }

    public static UIElement row(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(gap));
    }

    public static UIElement column(int gap) {
        return new UIElement().layout(layout -> layout.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER).gapAll(gap));
    }

    public static UIElement input(ItemStack stack) {
        return new ItemSlot().setItem(stack).xeiRecipeIngredient(IngredientIO.INPUT).xeiRecipeSlot();
    }

    public static UIElement input(Ingredient ingredient) {
        List<ItemStack> stacks = ingredient.items().map(ItemStack::new).toList();
        if (stacks.isEmpty()) {
            return new ItemSlot();
        }
        return new ItemSlot().setItem(stacks.getFirst())
                .xeiRecipeIngredient(IngredientIO.INPUT, stacks::stream)
                .xeiRecipeSlot(IngredientIO.INPUT, 1, 1, stacks::stream);
    }

    public static UIElement output(ItemStack stack) {
        return new ItemSlot().setItem(stack).xeiRecipeIngredient(IngredientIO.OUTPUT).xeiRecipeSlot();
    }

    public static UIElement arrow() {
        return new UIElement().layout(layout -> layout.width(18).height(18))
                .style(style -> style.backgroundTexture(Icons.RIGHT_ARROW_NO_BAR));
    }

    public static UIElement icon(String texture) {
        return new UIElement().layout(layout -> layout.width(GolemCapability.ICON_SIZE).height(GolemCapability.ICON_SIZE))
                .style(style -> style.backgroundTexture(SpriteTexture.of(texture)));
    }

    public static UIElement roles(List<GolemRole> roles) {
        UIElement element = column(1);
        for (GolemRole role : roles) {
            for (GolemCapability capability : role.capabilities()) {
                element.addChild(row(2).addChildren(
                        icon(capability.icon().toString()),
                        new Label().setText(Integer.toString(role.required(capability)))));
            }
        }
        return element;
    }

    public static UIElement trait(GolemTrait trait) {
        return icon(trait.icon().toString()).style(style -> style.tooltips(trait.displayName(), trait.description()));
    }
}
