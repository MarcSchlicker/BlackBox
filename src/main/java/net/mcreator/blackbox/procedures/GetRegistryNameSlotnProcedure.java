package net.mcreator.blackbox.procedures;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;

public class GetRegistryNameSlotnProcedure {
	public static String execute(LevelAccessor world, Entity entity, double n) {
		if (entity == null)
			return "";
		return BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world,
				BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z),
				(int) n).copy()).getItem()).toString();
	}

	private static ItemStack itemFromBlockInventory(LevelAccessor world, BlockPos pos, int slot) {
		if (world instanceof ILevelExtension ext) {
			IItemHandler itemHandler = ext.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
			if (itemHandler != null)
				return itemHandler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}
}