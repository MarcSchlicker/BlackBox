package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.init.BlackboxModItems;

public class ItemproductionTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		ItemStack Core = ItemStack.EMPTY;
		ItemStack CurrentItem = ItemStack.EMPTY;
		double n = 0;
		double CurrentSlot = 0;
		double counter = 0;
		double amountslots = 0;
		double amountitem = 0;
		Core = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if ((Core.getDisplayName().getString()).contains("#")) {
				amountslots = Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots");
				n = 0;
				while (n <= amountslots) {
					{
						final String _tagName = ("tick" + n);
						final double _tagValue = (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("tick" + n)) - 1);
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					if (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("tick" + n)) <= 0) {
						amountitem = 0;
						while (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("tick" + n)) <= 0) {
							{
								final String _tagName = ("tick" + n);
								final double _tagValue = (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("tick" + n))
										+ Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("number" + n)));
								CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
							}
							amountitem = amountitem + 1;
						}
						CurrentItem = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(((Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(("produces" + n)))).toLowerCase(java.util.Locale.ENGLISH))))
								.copy();
						if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
							ItemStack _setstack = CurrentItem.copy();
							_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) (n + 1)).getCount() + amountitem));
							_itemHandlerModifiable.setStackInSlot((int) (n + 1), _setstack);
						}
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