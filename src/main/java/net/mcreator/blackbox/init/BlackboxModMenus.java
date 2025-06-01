
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.Registries;

import net.mcreator.blackbox.world.inventory.OutputBlockGUIMenu;
import net.mcreator.blackbox.world.inventory.EmeraldBedrockGUIMenu;
import net.mcreator.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;
import net.mcreator.blackbox.world.inventory.BlackBoxGuiMenu;
import net.mcreator.blackbox.BlackboxMod;

public class BlackboxModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, BlackboxMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<EmeraldBedrockGUIMenu>> EMERALD_BEDROCK_GUI = REGISTRY.register("emerald_bedrock_gui", () -> IMenuTypeExtension.create(EmeraldBedrockGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BlackBoxGuiMenu>> BLACK_BOX_GUI = REGISTRY.register("black_box_gui", () -> IMenuTypeExtension.create(BlackBoxGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<DimensionalWorkbenchGUIMenu>> DIMENSIONAL_WORKBENCH_GUI = REGISTRY.register("dimensional_workbench_gui", () -> IMenuTypeExtension.create(DimensionalWorkbenchGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<OutputBlockGUIMenu>> OUTPUT_BLOCK_GUI = REGISTRY.register("output_block_gui", () -> IMenuTypeExtension.create(OutputBlockGUIMenu::new));
}
