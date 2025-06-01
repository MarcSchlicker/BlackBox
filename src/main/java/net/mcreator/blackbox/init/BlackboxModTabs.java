
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

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class BlackboxModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlackboxMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLACKBOX_TAB = REGISTRY.register("blackbox_tab",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.blackbox.blackbox_tab")).icon(() -> new ItemStack(BlackboxModItems.OPFERSCHWERT.get())).displayItems((parameters, tabData) -> {
				tabData.accept(BlackboxModBlocks.EMERALD_BEDROCK.get().asItem());
				tabData.accept(BlackboxModBlocks.BLACKBOX_BLOCK.get().asItem());
				tabData.accept(BlackboxModBlocks.DIMENSIONAL_WORKBENCH.get().asItem());
				tabData.accept(BlackboxModItems.DIMENSION_CORE.get());
				tabData.accept(BlackboxModBlocks.OUTPUT_BLOCK.get().asItem());
			}).withSearchBar().build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(BlackboxModItems.OPFERSCHWERT.get());
		}
	}
}
