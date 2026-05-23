package net.mcreator.blackbox.procedures;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.component.DataComponents;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModMenus;
import net.mcreator.blackbox.init.BlackboxModItems;

public class OutputBlockPositionProcedure {
	public static void execute(double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		ItemStack RedCore = ItemStack.EMPTY;
		{
			BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
			_vars.OutputBlock_X = x;
			_vars.OutputBlock_Y = y;
			_vars.OutputBlock_Z = z;
			_vars.markSyncDirty();
		}
		RedCore = new ItemStack(BlackboxModItems.RED_DIMENSION_CORE.get()).copy();
		{
			final String _tagName = "UUID";
			final String _tagValue = (entity.getStringUUID());
			CustomData.update(DataComponents.CUSTOM_DATA, RedCore, tag -> tag.putString(_tagName, _tagValue));
		}
		{
			final String _tagName = "position";
			final String _tagValue = ((("X: " + x) + "" + ("; Y: " + y)) + "" + ("; Z: " + z));
			CustomData.update(DataComponents.CUSTOM_DATA, RedCore, tag -> tag.putString(_tagName, _tagValue));
		}
		if (entity instanceof Player _player && _player.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu) {
			ItemStack _setstack3 = RedCore.copy();
			_setstack3.setCount(1);
			_menu.getSlots().get(0).set(_setstack3);
			_player.containerMenu.broadcastChanges();
		}
		{
			BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
			_vars.RedDimensionCoreID = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu4 ? _menu4.getSlots().get(0).getItem() : ItemStack.EMPTY).copy();
			_vars.markSyncDirty();
		}
	}
}