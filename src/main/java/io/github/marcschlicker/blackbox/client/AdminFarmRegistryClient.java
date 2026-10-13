package io.github.marcschlicker.blackbox.client;

import net.minecraft.client.Minecraft;

import io.github.marcschlicker.blackbox.client.gui.AdminFarmRegistryScreen;
import io.github.marcschlicker.blackbox.network.AdminFarmRegistryMessage.FarmEntry;

import java.util.List;

public final class AdminFarmRegistryClient {
	private AdminFarmRegistryClient() {
	}

	public static void open(List<FarmEntry> entries) {
		Minecraft.getInstance().setScreen(new AdminFarmRegistryScreen(entries));
	}
}
