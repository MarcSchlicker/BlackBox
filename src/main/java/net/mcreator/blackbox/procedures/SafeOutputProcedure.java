package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.UUID;

public class SafeOutputProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		ItemStack Core = ItemStack.EMPTY;
		boolean inside = false;
		double n = 0;
		double Slots = 0;
		double Slotcount = 0;
		double SlotAmountOutputblock = 0;
		SlotAmountOutputblock = 9;
		Core = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).copy();
		if (Core.getItem() == BlackboxModItems.RED_DIMENSION_CORE.get()) {
			n = 1;
			Slots = Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots");
			while (n <= SlotAmountOutputblock) {
				inside = false;
				Slotcount = 0;
				for (int index1 = 0; index1 < (int) Slots; index1++) {
					if ((Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(("produces" + Slotcount)))
							.equals(BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) n).copy()).getItem()).toString())) {
						{
							final String _tagName = ("TotalAmount" + Slotcount);
							final double _tagValue = (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("TotalAmount" + Slotcount)) + itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) n).getCount());
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
						}
						inside = true;
					}
					Slotcount = Slotcount + 1;
				}
				if (inside == false) {
					{
						final String _tagName = ("produces" + Slots);
						final String _tagValue = (BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) n).copy()).getItem()).toString());
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
					}
					{
						final String _tagName = ("TotalAmount" + Slots);
						final double _tagValue = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) n).getCount());
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					{
						final String _tagName = "slots";
						final double _tagValue = Slots;
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					Slots = Slots + 1;
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = new ItemStack(Blocks.GRASS_BLOCK).copy();
					_setstack.setCount(0);
					_itemHandlerModifiable.setStackInSlot((int) n, _setstack);
				}
				n = n + 1;
			}
			Slotcount = 0;
			BlackboxModVariables.MapVariables.get(world).ProduktionTime = 120;
			BlackboxModVariables.MapVariables.get(world).markSyncDirty();
			if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
				ItemStack _setstack = Core.copy();
				_setstack.setCount(1);
				_itemHandlerModifiable.setStackInSlot(0, _setstack);
			}
			if (!((world instanceof ServerLevel _level18 ? getEntityFromUUID(_level18, (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("UUID"))) : null) == null)) {
				{
					BlackboxModVariables.PlayerVariables _vars = (world instanceof ServerLevel _level21 ? getEntityFromUUID(_level21, (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("UUID"))) : null)
							.getData(BlackboxModVariables.PLAYER_VARIABLES);
					_vars.RedDimensionCoreID = Core.copy();
					_vars.markSyncDirty();
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

	private static Entity getEntityFromUUID(ServerLevel level, String uuid) {
		try {
			return level.getEntity(UUID.fromString(uuid));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}