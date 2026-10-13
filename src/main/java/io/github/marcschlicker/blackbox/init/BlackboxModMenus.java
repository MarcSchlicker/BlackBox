package io.github.marcschlicker.blackbox.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import io.github.marcschlicker.blackbox.world.inventory.OutputBlockGUIMenu;
import io.github.marcschlicker.blackbox.world.inventory.EmeraldBedrockGUIMenu;
import io.github.marcschlicker.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;
import io.github.marcschlicker.blackbox.world.inventory.BlackBoxGuiMenu;
import io.github.marcschlicker.blackbox.world.inventory.HandbookMenu;
import io.github.marcschlicker.blackbox.world.inventory.AdminBookMenu;
import io.github.marcschlicker.blackbox.world.inventory.CoreSplitterMenu;
import io.github.marcschlicker.blackbox.world.inventory.SpawnerBlackboxMenu;
import io.github.marcschlicker.blackbox.world.inventory.CoreDistributionMatrixMenu;
import io.github.marcschlicker.blackbox.network.MenuStateUpdateMessage;
import io.github.marcschlicker.blackbox.BlackboxMod;

import java.util.Map;

public class BlackboxModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, BlackboxMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<EmeraldBedrockGUIMenu>> EMERALD_BEDROCK_GUI = REGISTRY.register("emerald_bedrock_gui", () -> IMenuTypeExtension.create(EmeraldBedrockGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<BlackBoxGuiMenu>> BLACK_BOX_GUI = REGISTRY.register("black_box_gui", () -> IMenuTypeExtension.create(BlackBoxGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<DimensionalWorkbenchGUIMenu>> DIMENSIONAL_WORKBENCH_GUI = REGISTRY.register("dimensional_workbench_gui", () -> IMenuTypeExtension.create(DimensionalWorkbenchGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<OutputBlockGUIMenu>> OUTPUT_BLOCK_GUI = REGISTRY.register("output_block_gui", () -> IMenuTypeExtension.create(OutputBlockGUIMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<HandbookMenu>> HANDBOOK = REGISTRY.register("handbook", () -> IMenuTypeExtension.create(HandbookMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<AdminBookMenu>> ADMIN_BOOK = REGISTRY.register("admin_book", () -> IMenuTypeExtension.create(AdminBookMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CoreSplitterMenu>> CORE_SPLITTER = REGISTRY.register("core_splitter", () -> IMenuTypeExtension.create(CoreSplitterMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<SpawnerBlackboxMenu>> SPAWNER_BLACKBOX = REGISTRY.register("spawner_blackbox", () -> IMenuTypeExtension.create(SpawnerBlackboxMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<CoreDistributionMatrixMenu>> CORE_DISTRIBUTION_MATRIX = REGISTRY.register("core_distribution_matrix", () -> IMenuTypeExtension.create(CoreDistributionMatrixMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof BlackboxModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}
