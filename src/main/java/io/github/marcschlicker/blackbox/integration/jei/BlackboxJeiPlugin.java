package io.github.marcschlicker.blackbox.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.util.CoreUpgradeAnvilHandler;

/** Adds the data-driven anvil core upgrades to JEI's recipe browser. */
@JeiPlugin
public final class BlackboxJeiPlugin implements IModPlugin {
	public static final RecipeType<CoreUpgradeAnvilHandler.AnvilUpgradeDisplay> CORE_ANVIL_UPGRADES = RecipeType.create(
			BlackboxMod.MODID, "core_anvil_upgrades", CoreUpgradeAnvilHandler.AnvilUpgradeDisplay.class);

	@Override
	public net.minecraft.resources.ResourceLocation getPluginUid() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(BlackboxMod.MODID, "jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new CoreAnvilJeiCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(CORE_ANVIL_UPGRADES, CoreUpgradeAnvilHandler.jeiDisplays());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(Blocks.ANVIL), CORE_ANVIL_UPGRADES);
	}
}
