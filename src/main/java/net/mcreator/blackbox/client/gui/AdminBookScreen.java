package net.mcreator.blackbox.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.mcreator.blackbox.world.inventory.AdminBookMenu;
import net.mcreator.blackbox.network.AdminFarmRegistryMessage;
import net.mcreator.blackbox.network.AdminSettingsMessage;
import net.neoforged.neoforge.network.PacketDistributor;

public class AdminBookScreen extends AbstractContainerScreen<AdminBookMenu> {
	private EditBox measurementSeconds;
	private Button villageArchiveButton;
	private boolean durationInitialized;

	public AdminBookScreen(AdminBookMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 242;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.admin.farms_short"), button ->
				PacketDistributor.sendToServer(AdminFarmRegistryMessage.request())).bounds(this.leftPos + 112, this.topPos + 5, 56, 18).build());
		this.measurementSeconds = new EditBox(this.font, this.leftPos + 8, this.topPos + 39, 62, 20,
				Component.translatable("gui.blackbox.admin.measurement_seconds"));
		this.measurementSeconds.setMaxLength(4);
		this.measurementSeconds.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
		this.addRenderableWidget(this.measurementSeconds);
		Button saveDuration = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.admin.apply"), button -> saveDuration())
				.bounds(this.leftPos + 75, this.topPos + 39, 62, 20).build());
		saveDuration.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.admin.measurement_tooltip")));
		this.villageArchiveButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> toggleVillageArchive())
				.bounds(this.leftPos + 119, this.topPos + 64, 49, 18).build());
		this.villageArchiveButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.admin.village_archive_tooltip")));
	}

	private void saveDuration() {
		try {
			int seconds = Integer.parseInt(this.measurementSeconds.getValue());
			seconds = Math.max(10, Math.min(3600, seconds));
			this.measurementSeconds.setValue(Integer.toString(seconds));
			PacketDistributor.sendToServer(new AdminSettingsMessage(seconds, this.menu.isVillageArchiveEnabled()));
		} catch (NumberFormatException ignored) {
			this.measurementSeconds.setValue(Integer.toString(Math.max(10, this.menu.getMeasurementSeconds())));
		}
	}

	private void toggleVillageArchive() {
		PacketDistributor.sendToServer(new AdminSettingsMessage(this.menu.getMeasurementSeconds(), !this.menu.isVillageArchiveEnabled()));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (!this.durationInitialized && this.menu.getMeasurementSeconds() > 0) {
			this.measurementSeconds.setValue(Integer.toString(this.menu.getMeasurementSeconds()));
			this.durationInitialized = true;
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF202329);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF8E9AA8);
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 104 + row * 18, 0xFFB77979);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 157 + row * 18, 0xFF697582);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 215, 0xFF697582);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.title"), 8, 7, 0xFFF1F3F5, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.measurement_seconds"), 8, 27, 0xFFC9CFD6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.village_archive"), 8, 68, 0xFFC9CFD6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.admin.hint"), 8, 92, 0xFFC9CFD6, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 146, 0xFFC9CFD6, false);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (this.villageArchiveButton != null) {
			this.villageArchiveButton.setMessage(Component.translatable(this.menu.isVillageArchiveEnabled()
					? "gui.blackbox.admin.village_archive.on" : "gui.blackbox.admin.village_archive.off"));
		}
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}
}
