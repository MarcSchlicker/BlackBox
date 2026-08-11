package net.mcreator.blackbox.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.item.ItemStack;

import net.mcreator.blackbox.init.BlackboxModItems;
import net.mcreator.blackbox.util.FarmCoreData;
import net.mcreator.blackbox.util.FarmEnvironment;

@EventBusSubscriber(Dist.CLIENT)
public final class FarmCoreColorHandler {
	private FarmCoreColorHandler() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(FarmCoreColorHandler::getCoreColor, BlackboxModItems.DIMENSION_CORE.get());
	}

	private static int getCoreColor(ItemStack stack, int tintIndex) {
		if (tintIndex != 0) {
			return 0xFFFFFFFF;
		}
		FarmEnvironment environment = FarmCoreData.getEnvironment(stack);
		return switch (environment) {
			case OVERWORLD -> 0xFF45C463;
			case NETHER -> 0xFFE44848;
			case END -> 0xFFA96BE8;
			case STANDARD -> 0xFFF2C94C;
		};
	}
}
