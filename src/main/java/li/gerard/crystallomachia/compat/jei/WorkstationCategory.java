package li.gerard.crystallomachia.compat.jei;

import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;

public class WorkstationCategory<T> extends ModularUIRecipeCategory<T> {
    private final IRecipeType<T> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final int width;
    private final int height;

    public WorkstationCategory(IJeiHelpers helpers, IRecipeType<T> recipeType, IModularUIProvider<T> uiProvider, Component title, ItemLike icon,
            int width, int height) {
        super(uiProvider);
        this.recipeType = recipeType;
        this.title = title;
        this.icon = helpers.getGuiHelper().createDrawableItemLike(icon);
        this.width = width;
        this.height = height;
    }

    @Override
    public IRecipeType<T> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }
}
