package net.mcreator.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.mcreator.blackbox.network.AdminFarmRegistryMessage;
import net.mcreator.blackbox.network.AdminFarmRegistryMessage.FarmEntry;

import java.util.ArrayList;
import java.util.List;

public final class AdminFarmRegistryScreen extends Screen {
	private static final int PANEL_WIDTH = 360;
	private static final int PANEL_HEIGHT = 232;
	private static final int ROWS = 7;
	private final List<FarmEntry> farms;
	private final List<Button> rowButtons = new ArrayList<>();
	private int page;
	private String confirmId = "";
	private Button previous;
	private Button next;

	public AdminFarmRegistryScreen(List<FarmEntry> farms) {
		super(Component.translatable("gui.blackbox.admin.farms"));
		this.farms = List.copyOf(farms);
	}

	@Override
	protected void init() {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		this.previous = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1)).bounds(left + 12, top + 205, 30, 18).build());
		this.next = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1)).bounds(left + 318, top + 205, 30, 18).build());
		this.addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose()).bounds(left + 335, top + 5, 18, 18).build());
		rebuildRows();
	}

	private void changePage(int amount) {
		this.page = Math.max(0, Math.min(maxPage(), this.page + amount));
		this.confirmId = "";
		rebuildRows();
	}

	private void rebuildRows() {
		for (Button button : this.rowButtons) {
			this.removeWidget(button);
		}
		this.rowButtons.clear();
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		int first = this.page * ROWS;
		for (int row = 0; row < ROWS && first + row < this.farms.size(); row++) {
			FarmEntry farm = this.farms.get(first + row);
			String dimension = farm.dimension().substring(farm.dimension().indexOf(':') + 1).replace("farm_", "");
			String id = farm.coreId().substring(0, 8);
			Component label = Component.literal(farm.name() + " | " + farm.sizeChunks() + "x" + farm.sizeChunks() + " | " + dimension + " | " + farm.owner() + " | " + id);
			this.rowButtons.add(this.addRenderableWidget(Button.builder(label, ignored -> {
			}).bounds(left + 12, top + 32 + row * 23, 272, 20).build()));
			boolean confirming = this.confirmId.equals(farm.coreId());
			this.rowButtons.add(this.addRenderableWidget(Button.builder(Component.translatable(confirming ? "gui.blackbox.admin.confirm" : "gui.blackbox.admin.delete"),
					ignored -> delete(farm)).bounds(left + 288, top + 32 + row * 23, 60, 20).build()));
		}
		this.previous.active = this.page > 0;
		this.next.active = this.page < maxPage();
	}

	private void delete(FarmEntry farm) {
		if (!this.confirmId.equals(farm.coreId())) {
			this.confirmId = farm.coreId();
			rebuildRows();
			return;
		}
		PacketDistributor.sendToServer(AdminFarmRegistryMessage.delete(farm.coreId()));
	}

	private int maxPage() {
		return Math.max(0, (this.farms.size() - 1) / ROWS);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int left = (this.width - PANEL_WIDTH) / 2;
		int top = (this.height - PANEL_HEIGHT) / 2;
		graphics.fill(0, 0, this.width, this.height, 0xCC0A0D10);
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF20262B);
		graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFF8E9AA8);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 9, 0xFFF1F3F5);
		if (this.farms.isEmpty()) {
			graphics.drawCenteredString(this.font, Component.translatable("gui.blackbox.admin.no_farms"), this.width / 2, top + 104, 0xFFB5BEC3);
		}
		graphics.drawCenteredString(this.font, Component.literal((this.page + 1) + " / " + (maxPage() + 1)), this.width / 2, top + 210, 0xFFB5BEC3);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
