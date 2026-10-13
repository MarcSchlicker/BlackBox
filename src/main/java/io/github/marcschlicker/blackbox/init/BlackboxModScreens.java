package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import io.github.marcschlicker.blackbox.client.gui.OutputBlockGUIScreen;
import io.github.marcschlicker.blackbox.client.gui.EmeraldBedrockGUIScreen;
import io.github.marcschlicker.blackbox.client.gui.DimensionalWorkbenchGUIScreen;
import io.github.marcschlicker.blackbox.client.gui.BlackBoxGuiScreen;
import io.github.marcschlicker.blackbox.client.gui.HandbookScreen;
import io.github.marcschlicker.blackbox.client.gui.AdminBookScreen;
import io.github.marcschlicker.blackbox.client.gui.CoreSplitterScreen;
import io.github.marcschlicker.blackbox.client.gui.SpawnerBlackboxScreen;
import io.github.marcschlicker.blackbox.client.gui.CoreDistributionMatrixScreen;

@EventBusSubscriber(Dist.CLIENT)
public class BlackboxModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(BlackboxModMenus.EMERALD_BEDROCK_GUI.get(), EmeraldBedrockGUIScreen::new);
		event.register(BlackboxModMenus.BLACK_BOX_GUI.get(), BlackBoxGuiScreen::new);
		event.register(BlackboxModMenus.DIMENSIONAL_WORKBENCH_GUI.get(), DimensionalWorkbenchGUIScreen::new);
		event.register(BlackboxModMenus.OUTPUT_BLOCK_GUI.get(), OutputBlockGUIScreen::new);
		event.register(BlackboxModMenus.HANDBOOK.get(), HandbookScreen::new);
		event.register(BlackboxModMenus.ADMIN_BOOK.get(), AdminBookScreen::new);
		event.register(BlackboxModMenus.CORE_SPLITTER.get(), CoreSplitterScreen::new);
		event.register(BlackboxModMenus.SPAWNER_BLACKBOX.get(), SpawnerBlackboxScreen::new);
		event.register(BlackboxModMenus.CORE_DISTRIBUTION_MATRIX.get(), CoreDistributionMatrixScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}
