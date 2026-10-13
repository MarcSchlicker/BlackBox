package io.github.marcschlicker.blackbox.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.item.ItemStack;

import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmEnvironment;

@EventBusSubscriber(Dist.CLIENT)
public final class FarmCoreColorHandler {
	private FarmCoreColorHandler() {
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register(FarmCoreColorHandler::getCoreColor, BlackboxModItems.DIMENSION_CORE.get());
	}

	private static int getCoreColor(ItemStack stack, int tintIndex) {
		if (tintIndex == 3) {
			return FarmCoreData.isSplitOutputCore(stack) ? 0xFF54D6E8 : 0x00FFFFFF;
		}
		FarmEnvironment environment = FarmCoreData.getEnvironment(stack);
		return switch (tintIndex) {
			// The permanent brass frame and yellow memory crystal are painted into layer 0.
			case 1 -> switch (environment) {
				case OVERWORLD -> 0xFF52C56B;
				case NETHER -> 0xFFE24A40;
				case END -> 0xFF9D66E6;
				case STANDARD -> 0xFFF2C94C;
			};
			case 2 -> switch (environment) {
				case OVERWORLD -> 0xFF245C35;
				case NETHER -> 0xFF5C161C;
				case END -> 0xFF171020;
				case STANDARD -> 0xFF8E6626;
			};
			default -> 0xFFFFFFFF;
		};
	}
}
