package io.github.marcschlicker.blackbox.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import io.github.marcschlicker.blackbox.util.CoreUpgradeAnvilHandler;

/** JEI presentation for the server-side AnvilUpdateEvent core transformations. */
public final class CoreAnvilJeiCategory implements IRecipeCategory<CoreUpgradeAnvilHandler.AnvilUpgradeDisplay> {
	private final IDrawable background;
	private final IDrawable icon;

	public CoreAnvilJeiCategory(IGuiHelper guiHelper) {
		this.background = guiHelper.createBlankDrawable(128, 48);
		this.icon = guiHelper.createDrawableItemStack(new ItemStack(Blocks.ANVIL));
	}

	@Override
	public RecipeType<CoreUpgradeAnvilHandler.AnvilUpgradeDisplay> getRecipeType() {
		return BlackboxJeiPlugin.CORE_ANVIL_UPGRADES;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("jei.blackbox.core_anvil.title");
	}

	@Override
	public IDrawable getBackground() {
		return this.background;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, CoreUpgradeAnvilHandler.AnvilUpgradeDisplay recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 8, 12).addItemStack(recipe.core());
		builder.addSlot(RecipeIngredientRole.INPUT, 38, 12).addItemStack(recipe.upgrade());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 12).addItemStack(recipe.result());
	}

	@Override
	public void draw(CoreUpgradeAnvilHandler.AnvilUpgradeDisplay recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
		graphics.drawString(Minecraft.getInstance().font, "+", 29, 16, 0xFFD3D9E1, false);
		graphics.drawString(Minecraft.getInstance().font, "->", 68, 16, 0xFFD3D9E1, false);
		graphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.blackbox.core_anvil.cost", recipe.experienceCost()), 8, 35, 0xFFAAB4C0, false);
	}
}
