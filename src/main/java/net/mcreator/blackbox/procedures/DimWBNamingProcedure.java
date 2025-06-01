package net.mcreator.blackbox.procedures;

import org.apache.logging.log4j.core.Core;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.common.extensions.ILevelExtension;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.EditBox;

import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.init.BlackboxModItems;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class DimWBNamingProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, HashMap guistate) {
		if (entity == null || guistate == null)
			return;
		ItemStack Core = ItemStack.EMPTY;
		Core = (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY).copy();
		if (Core.getItem() == BlackboxModItems.DIMENSION_CORE.get()) {
			if (!(Core.getDisplayName().getString()).contains("#")) {
				Core.set(DataComponents.CUSTOM_NAME, Component.literal((((guistate.containsKey("text:Worldname") ? ((EditBox) guistate.get("text:Worldname")).getValue() : "") + "#") + ""
						+ (entity.getDisplayName().getString() + "" + entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter))));
				{
					final String _tagName = "Name";
					final String _tagValue = (((guistate.containsKey("text:Worldname") ? ((EditBox) guistate.get("text:Worldname")).getValue() : "") + "#") + ""
							+ (entity.getDisplayName().getString() + "" + entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter));
					CustomData.update(DataComponents.CUSTOM_DATA, Core, tag -> tag.putString(_tagName, _tagValue));
				}
				{
					BlackboxModVariables.PlayerVariables _vars = entity.getData(BlackboxModVariables.PLAYER_VARIABLES);
					_vars.FarmCounter = entity.getData(BlackboxModVariables.PLAYER_VARIABLES).FarmCounter + 1;
					_vars.syncPlayerVariables(entity);
				}
				Core.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
				if (world instanceof ILevelExtension _ext && _ext.getCapability(Capabilities.ItemHandler.BLOCK, BlockPos.containing(x, y, z), null) instanceof IItemHandlerModifiable _itemHandlerModifiable) {
					ItemStack _setstack = Core.copy();
					_setstack.setCount(1);
					_itemHandlerModifiable.setStackInSlot(0, _setstack);
				}
			}
		}
	}
}
