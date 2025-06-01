package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.function.Supplier;
import java.util.Map;

public class DimWBEndProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		ItemStack Core = ItemStack.EMPTY;
		double n = 0;
		Core = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if ((Core.getDisplayName().getString()).contains("#")) {
				n = 0;
				while (itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y,
						entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).getCount() != 0) {
					{
						final String _tagName = ("produces" + n);
						final String _tagValue = (BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X,
								entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).copy()).getItem()).toString());
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
					}
					{
						final String _tagName = ("number" + n);
						final double _tagValue = (itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y,
								entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).getCount());
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					{
						final String _tagName = "slots";
						final double _tagValue = n;
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					n = n + 1;
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = Core.copy();
					_setstack.setCount(1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			}
		}
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
