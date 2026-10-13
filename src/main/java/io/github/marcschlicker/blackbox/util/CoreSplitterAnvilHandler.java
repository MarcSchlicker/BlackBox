package io.github.marcschlicker.blackbox.util;

import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;

/** Implements permanent core splitting through Minecraft's normal anvil screen. */
public final class CoreSplitterAnvilHandler {
	private static final String PENDING_SPLIT_TAG = "blackbox_anvil_split_result";
	private static final long SPLIT_COST_LEVELS = 5;

	private CoreSplitterAnvilHandler() {
	}

	public static void onAnvilUpdate(AnvilUpdateEvent event) {
		if (!(event.getPlayer() instanceof ServerPlayer player) || !event.getOutput().isEmpty()) {
			return;
		}
		FarmCoreData.Recipe recipe = getSplittableRecipe(player, event.getLeft(), event.getRight());
		if (recipe == null) {
			return;
		}

		ItemStack result = FarmCoreData.createSplitOutputCore(event.getLeft(), recipe, recipe.outputTemplates().getFirst().stack(), player.level().registryAccess());
		CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.putBoolean(PENDING_SPLIT_TAG, true));
		event.setOutput(result);
		event.setCost(SPLIT_COST_LEVELS);
		event.setMaterialCost(1);
	}

	public static void onAnvilRepair(AnvilRepairEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !isPendingSplit(event.getOutput())) {
			return;
		}
		FarmCoreData.Recipe recipe = getSplittableRecipe(player, event.getLeft(), event.getRight());
		if (recipe == null) {
			return;
		}

		// The anvil output is the first output core. Give the remaining cores directly to the player.
		ItemStack source = event.getLeft().copyWithCount(1);
		FarmCoreData.getCoreId(source).ifPresent(coreId -> FarmDimensionRuntime.deleteFarmCell(player.server, coreId));
		for (int index = 1; index < recipe.outputTemplates().size(); index++) {
			ItemStack split = FarmCoreData.createSplitOutputCore(source, recipe, recipe.outputTemplates().get(index).stack(), player.level().registryAccess());
			ItemHandlerHelper.giveItemToPlayer(player, split);
		}
		CustomData.update(DataComponents.CUSTOM_DATA, event.getOutput(), tag -> tag.remove(PENDING_SPLIT_TAG));
		player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.blackbox.core_splitter.completed", recipe.outputTemplates().size()));
	}

	private static FarmCoreData.Recipe getSplittableRecipe(ServerPlayer player, ItemStack core, ItemStack upgrade) {
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !upgrade.is(BlackboxModItems.CORE_SPLITTER.get())
				|| !FarmCoreData.isProgrammed(core) || FarmCoreData.isVillageArchiveCore(core) || FarmCoreData.isSplitOutputCore(core)
				|| !FarmCoreData.canManage(core, player)) {
			return null;
		}
		FarmCoreData.Recipe recipe = FarmCoreData.read(core, player.level().registryAccess());
		return recipe.isValid() && recipe.outputTemplates().size() >= 2 ? recipe : null;
	}

	private static boolean isPendingSplit(ItemStack stack) {
		CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
		return data.getBoolean(PENDING_SPLIT_TAG);
	}
}
