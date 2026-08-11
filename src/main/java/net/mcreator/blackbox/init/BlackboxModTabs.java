/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.blackbox.BlackboxMod;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.util.FarmEnvironment;

@EventBusSubscriber
public class BlackboxModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlackboxMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLACKBOX_TAB = REGISTRY.register("blackbox_tab",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.blackbox.blackbox_tab")).icon(() -> new ItemStack(BlackboxModItems.OPFERSCHWERT.get())).displayItems((parameters, tabData) -> {
				tabData.accept(BlackboxModBlocks.EMERALD_BEDROCK.get().asItem());
				tabData.accept(BlackboxModBlocks.BLACKBOX_BLOCK.get().asItem());
				tabData.accept(BlackboxModBlocks.DIMENSIONAL_WORKBENCH.get().asItem());
				tabData.accept(FarmCoreData.createEnvironmentCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), FarmEnvironment.STANDARD));
				tabData.accept(FarmCoreData.createEnvironmentCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), FarmEnvironment.OVERWORLD));
				tabData.accept(FarmCoreData.createEnvironmentCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), FarmEnvironment.NETHER));
				tabData.accept(FarmCoreData.createEnvironmentCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), FarmEnvironment.END));
				tabData.accept(FarmCoreData.createCellSizeCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), 1));
				tabData.accept(FarmCoreData.createCellSizeCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), 2));
				tabData.accept(FarmCoreData.createCellSizeCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), 3));
				tabData.accept(BlackboxModBlocks.OUTPUT_BLOCK.get().asItem());
				tabData.accept(BlackboxModBlocks.INPUTBLOCK.get().asItem());
				tabData.accept(BlackboxModItems.HANDBOOK.get());
				tabData.accept(BlackboxModItems.ADMIN_BOOK.get());
				tabData.accept(BlackboxModItems.STANDARD_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.OVERWORLD_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.NETHER_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.END_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.STABILITY_UPGRADE.get());
				tabData.accept(BlackboxModItems.MOB_SPAWN_UPGRADE.get());
				tabData.accept(BlackboxModItems.COMPACT_CELL_UPGRADE.get());
				tabData.accept(BlackboxModItems.MEDIUM_CELL_UPGRADE.get());
				tabData.accept(BlackboxModItems.LARGE_CELL_UPGRADE.get());
				tabData.accept(BlackboxModItems.BLUEPRINT.get());
				tabData.accept(FarmCoreData.createExampleIronFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
			}).withSearchBar().build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(BlackboxModItems.OPFERSCHWERT.get());
		}
	}
}
