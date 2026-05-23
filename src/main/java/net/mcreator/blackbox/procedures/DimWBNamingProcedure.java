package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModMenus;
import net.mcreator.blackbox.init.BlackboxModItems;

public class DimWBNamingProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		ItemStack Core = ItemStack.EMPTY;
		ItemStack RedCore = ItemStack.EMPTY;
		Core = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu0 ? _menu0.getSlots().get(0).getItem() : ItemStack.EMPTY).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if (!(Core.getDisplayName().getString()).contains("#")) {
				Core.set(DataComponents.CUSTOM_NAME, Component.literal(((((entity instanceof Player _entity3 && _entity3.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu3) ? _menu3.getMenuState(0, "Worldname", "") : "") + "#") + ""
						+ (entity.getDisplayName().getString() + "" + entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter))));
				{
					final String _tagName = "Name";
					final String _tagValue = ((((entity instanceof Player _entity6 && _entity6.containerMenu instanceof BlackboxModMenus.MenuAccessor _menu6) ? _menu6.getMenuState(0, "Worldname", "") : "") + "#") + ""
							+ (entity.getDisplayName().getString() + "" + entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter));
					CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
				}
				Core.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = Core.copy();
					_setstack.setCount(1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
				{
					BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
					_vars.FarmCounter = entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter + 1;
					_vars.markSyncDirty();
				}
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = entity.getData(BlackboxModVariables.PLAYER_VARIABLES).RedDimensionCoreID.copy();
					_setstack.setCount(1);
					_itemHandlerModifiable.setStackInSlot(2, _setstack);
				}
			}
		}
	}
}