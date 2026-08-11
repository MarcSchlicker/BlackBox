package net.mcreator.blackbox.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import net.neoforged.neoforge.network.PacketDistributor;

import net.mcreator.blackbox.client.gui.BlueprintLibraryScreen;
import net.mcreator.blackbox.client.gui.BlueprintNameScreen;
import net.mcreator.blackbox.item.BlueprintItem.StorageScope;
import net.mcreator.blackbox.network.BlackboxModVariables;
import net.mcreator.blackbox.network.BlueprintLibraryMessage;
import net.mcreator.blackbox.network.BlueprintTransferMessage;
import net.mcreator.blackbox.util.BlueprintLibrary.BlueprintSummary;
import net.mcreator.blackbox.util.FarmEnvironment;

import java.util.List;

public final class BlueprintLibraryClient {
	private BlueprintLibraryClient() {
	}

	public static void open(List<BlueprintSummary> entries, String selectedId, String selectedName, StorageScope selectedScope,
			StorageScope preferredStorage, InteractionHand hand) {
		setClientSelection(selectedId, selectedName, selectedScope);
		setClientPreferredStorage(preferredStorage);
		Minecraft.getInstance().setScreen(new BlueprintLibraryScreen(entries, LocalBlueprintLibrary.list(), selectedId, selectedScope, preferredStorage, hand));
	}

	public static void openNamePrompt(InteractionHand hand, StorageScope storageScope) {
		Minecraft.getInstance().setScreen(new BlueprintNameScreen(hand, storageScope));
	}

	public static void createNew(StorageScope storageScope, InteractionHand hand) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		if (!FarmEnvironment.isFarmDimension(Minecraft.getInstance().player.level().dimension())) {
			Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.blackbox.blueprint.create_in_farm"), true);
			return;
		}
		setClientPreferredStorage(storageScope);
		PacketDistributor.sendToServer(BlueprintLibraryMessage.setScope(hand, storageScope));
		openNamePrompt(hand, storageScope);
	}

	public static void select(String id, String name, StorageScope scope, InteractionHand hand) {
		setClientSelection(id, name, scope);
		PacketDistributor.sendToServer(BlueprintLibraryMessage.select(hand, id, name, scope));
	}

	public static void saveLocal(String name, byte[] data) {
		boolean saved = LocalBlueprintLibrary.save(data);
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(Component.translatable(saved ? "message.blackbox.blueprint.saved_local" : "message.blackbox.blueprint.save_failed", name), true);
		}
	}

	public static void applyLocal(String blueprintId, InteractionHand hand) {
		byte[] data = LocalBlueprintLibrary.load(blueprintId);
		if (data.length == 0) {
			showMissingLocal();
			return;
		}
		PacketDistributor.sendToServer(BlueprintTransferMessage.applyLocal(hand, blueprintId, data));
	}

	public static void publishLocal(String blueprintId, InteractionHand hand) {
		byte[] data = LocalBlueprintLibrary.load(blueprintId);
		if (data.length == 0) {
			showMissingLocal();
			return;
		}
		PacketDistributor.sendToServer(BlueprintTransferMessage.publishServer(hand, blueprintId, data));
	}

	private static void showMissingLocal() {
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.blackbox.blueprint.not_local"), true);
		}
	}

	private static void setClientSelection(String id, String name, StorageScope scope) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		BlackboxModVariables.PlayerVariables variables = Minecraft.getInstance().player.getData(BlackboxModVariables.PLAYER_VARIABLES);
		variables.BlueprintSelectionId = id;
		variables.BlueprintSelectionName = name;
		variables.BlueprintSelectionScope = scope.id();
	}

	private static void setClientPreferredStorage(StorageScope scope) {
		if (Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.getData(BlackboxModVariables.PLAYER_VARIABLES).BlueprintPreferredStorage = scope.id();
		}
	}
}
