package net.mcreator.blackbox.client;

import net.minecraft.client.Minecraft;

import net.mcreator.blackbox.client.gui.AdminFarmRegistryScreen;
import net.mcreator.blackbox.network.AdminFarmRegistryMessage.FarmEntry;

import java.util.List;

public final class AdminFarmRegistryClient {
	private AdminFarmRegistryClient() {
	}

	public static void open(List<FarmEntry> entries) {
		Minecraft.getInstance().setScreen(new AdminFarmRegistryScreen(entries));
	}
}
