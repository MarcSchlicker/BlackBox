package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModMenus;
import net.mcreator.blackbox.init.BlackboxModItems;

public class BUDimWBEndProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		ItemStack Core = ItemStack.EMPTY;
		boolean inside = false;
		double n = 0;
		double Slots = 0;
		double Slotcount = 0;
		Core = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu0 ? _menu0.getSlots().get(0).getItem() : ItemStack.EMPTY).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if ((Core.getDisplayName().getString()).contains("#")) {
				n = 1;
				Slots = 0;
				while (itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y,
						entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).getCount() != 0) {
					inside = false;
					Slotcount = 0;
					for (int index1 = 0; index1 < (int) Slots; index1++) {
						if ((Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(("produces" + Slotcount)))
								.equals(BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X,
										entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).copy()).getItem()).toString())) {
							{
								final String _tagName = ("number" + Slotcount);
								final double _tagValue = (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("number" + Slotcount))
										+ itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y,
												entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).getCount());
								CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
							}
							inside = true;
						}
						Slotcount = Slotcount + 1;
					}
					if (inside == false) {
						{
							final String _tagName = ("produces" + Slots);
							final String _tagValue = (BuiltInRegistries.ITEM.getKey((itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X,
									entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).copy()).getItem()).toString());
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
						}
						{
							final String _tagName = ("number" + Slots);
							final double _tagValue = (itemFromBlockInventory(world, BlockPos.containing(entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_X, entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Y,
									entity.getData(BlackboxModVariables.PLAYER_VARIABLES).OutputBlock_Z), (int) n).getCount());
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
						}
						{
							final String _tagName = "slots";
							final double _tagValue = Slots;
							CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
						}
						Slots = Slots + 1;
					}
					n = n + 1;
				}
				Slotcount = 0;
				BlackboxModVariables.MapVariables.get(world).ProduktionTime = 120;
				BlackboxModVariables.MapVariables.get(world).markSyncDirty();
				for (int index2 = 0; index2 < (int) (Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble("slots") + 1); index2++) {
					{
						final String _tagName = ("tick" + Slotcount);
						final double _tagValue = (BlackboxModVariables.MapVariables.get(world).ProduktionTime / Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("number" + Slotcount)));
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					{
						final String _tagName = ("number" + Slotcount);
						final double _tagValue = (BlackboxModVariables.MapVariables.get(world).ProduktionTime / Core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getDouble(("number" + Slotcount)));
						CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putDouble(_tagName, _tagValue));
					}
					Slotcount = Slotcount + 1;
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
			}
		}
		PlaceBlockUnderProcedure.execute(world, x, y, z);
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