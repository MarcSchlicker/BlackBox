package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import io.github.marcschlicker.blackbox.BlackboxMod;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmEnvironment;
import io.github.marcschlicker.blackbox.util.SpawnerIntegration;

public class BlackboxModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlackboxMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLACKBOX_TAB = REGISTRY.register("blackbox_tab",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.blackbox.blackbox_tab")).icon(() -> new ItemStack(BlackboxModBlocks.BLACKBOX_BLOCK.get())).displayItems((parameters, tabData) -> {
				tabData.accept(BlackboxModBlocks.EMERALD_BEDROCK.get().asItem());
				tabData.accept(BlackboxModBlocks.BLACKBOX_BLOCK.get().asItem());
				if (SpawnerIntegration.isAvailable()) {
					tabData.accept(BlackboxModBlocks.SPAWNER_BLACKBOX.get().asItem());
				}
				for (FarmEnvironment environment : FarmEnvironment.values()) {
					for (int size : new int[] {1, 2, 3}) {
						ItemStack core = FarmCoreData.createEnvironmentCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get()), environment);
						FarmCoreData.setCellSizeChunks(core, size);
						tabData.accept(core);
					}
				}
				tabData.accept(BlackboxModBlocks.OUTPUT_BLOCK.get().asItem());
				tabData.accept(BlackboxModBlocks.INPUTBLOCK.get().asItem());
				tabData.accept(BlackboxModBlocks.FARM_ACTOR.get().asItem());
				tabData.accept(BlackboxModBlocks.CORE_DISTRIBUTION_MATRIX.get().asItem());
				tabData.accept(BlackboxModItems.HANDBOOK.get());
				tabData.accept(BlackboxModItems.ADMIN_BOOK.get());
				tabData.accept(BlackboxModItems.STANDARD_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.OVERWORLD_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.NETHER_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.END_ENVIRONMENT_UPGRADE.get());
				tabData.accept(BlackboxModItems.STABILITY_UPGRADE.get());
				tabData.accept(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get());
				tabData.accept(BlackboxModItems.CORE_SPLITTER.get());
				tabData.accept(BlackboxModItems.MOB_SPAWN_UPGRADE.get());
				tabData.accept(BlackboxModItems.MEDIUM_CELL_UPGRADE.get());
				tabData.accept(BlackboxModItems.LARGE_CELL_UPGRADE.get());
				tabData.accept(BlackboxModItems.BLUEPRINT.get());
				tabData.accept(FarmCoreData.createExampleIronFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
				tabData.accept(FarmCoreData.createArchiveIronFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
				tabData.accept(FarmCoreData.createArchiveSugarCaneFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
				tabData.accept(FarmCoreData.createArchiveNetherWartFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
				tabData.accept(FarmCoreData.createArchiveBlazeFarmCore(new ItemStack(BlackboxModItems.DIMENSION_CORE.get())));
			}).withSearchBar().build());
}
