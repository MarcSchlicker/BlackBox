
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.blackbox.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.blackbox.client.gui.OutputBlockGUIScreen;
import net.mcreator.blackbox.client.gui.EmeraldBedrockGUIScreen;
import net.mcreator.blackbox.client.gui.DimensionalWorkbenchGUIScreen;
import net.mcreator.blackbox.client.gui.BlackBoxGuiScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BlackboxModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(BlackboxModMenus.EMERALD_BEDROCK_GUI.get(), EmeraldBedrockGUIScreen::new);
		event.register(BlackboxModMenus.BLACK_BOX_GUI.get(), BlackBoxGuiScreen::new);
		event.register(BlackboxModMenus.DIMENSIONAL_WORKBENCH_GUI.get(), DimensionalWorkbenchGUIScreen::new);
		event.register(BlackboxModMenus.OUTPUT_BLOCK_GUI.get(), OutputBlockGUIScreen::new);
	}
}
