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

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModItems;

public class ItemproductionProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		ItemStack Core = ItemStack.EMPTY;
		double time = 0;
		double n = 0;
		Core = (itemFromBlockInventory(world, BlockPos.containing(x, y, z), 0).copy()).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if ((Core.getDisplayName().getString()).contains("#")) {
				if (BlackboxModVariables.MapVariables.get(world).TimeinSec != time) {
					time = BlackboxModVariables.MapVariables.get(world).TimeinSec;
					n = 1;
					while (n <= Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots")) {
						if (BlackboxModVariables.MapVariables.get(world).TimeinSec % Math.round(120 / Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("number" + n))) == 0) {
							if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
								ItemStack _setstack = new ItemStack(
										BuiltInRegistries.ITEM.get(ResourceLocation.parse(((Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(("produces" + n)))).toLowerCase(java.util.Locale.ENGLISH)))).copy();
								_setstack.setCount((int) (itemFromBlockInventory(world, BlockPos.containing(x, y, z), (int) (n + 0)).getCount() + 1));
								_itemHandlerModifiable.setStackInSlot((int) (n + 0), _setstack);
							}
						}
						n = n + 1;
					}
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
