package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModMenus;
import net.mcreator.blackbox.init.BlackboxModItems;

public class CalcDimCoreatEndProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		boolean inside = false;
		double n = 0;
		double Slots = 0;
		double Slotcount = 0;
		ItemStack Core = ItemStack.EMPTY;
		ItemStack RedCore = ItemStack.EMPTY;
		Core = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu0 ? _menu0.getSlots().get(0).getItem() : ItemStack.EMPTY).copy();
		RedCore = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu1 ? _menu1.getSlots().get(2).getItem() : ItemStack.EMPTY).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if (RedCore.getItem() == BlackboxModItems.RED_DIMENSION_CORE.get()) {
				if ((Core.getDisplayName().getString()).contains("#")) {
					Slotcount = 0;
					BlackboxModVariables.MapVariables.get(world).ProduktionTime = 120;
					BlackboxModVariables.MapVariables.get(world).markSyncDirty();
					for (int index0 = 0; index0 < (int) (RedCore.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots") + 0); index0++) {
						{
							final String _tagName = ("produces" + Slotcount);
							final String _tagValue = (RedCore.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(("produces" + Slotcount)));
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
						}
						{
							final String _tagName = ("tick" + Slotcount);
							final double _tagValue = (BlackboxModVariables.MapVariables.get(world).ProduktionTime / RedCore.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("TotalAmount" + Slotcount)));
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
						}
						{
							final String _tagName = ("number" + Slotcount);
							final double _tagValue = (BlackboxModVariables.MapVariables.get(world).ProduktionTime / RedCore.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("TotalAmount" + Slotcount)));
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
						}
						Slotcount = Slotcount + 1;
					}
					{
						final String _tagName = "slots";
						final double _tagValue = (RedCore.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots") - 1);
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						ItemStack _setstack = Core.copy();
						_setstack.setCount(0);
						_itemHandlerModifiable.setStackInSlot(0, _setstack);
					}
					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						ItemStack _setstack = Core.copy();
						_setstack.setCount(1);
						_itemHandlerModifiable.setStackInSlot(1, _setstack);
					}
					if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
						ItemStack _setstack = RedCore.copy();
						_setstack.setCount(1);
						_itemHandlerModifiable.setStackInSlot(2, _setstack);
					}
				}
			}
		}
	}
}