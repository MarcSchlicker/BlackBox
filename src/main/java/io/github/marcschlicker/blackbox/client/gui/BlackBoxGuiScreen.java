package io.github.marcschlicker.blackbox.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import io.github.marcschlicker.blackbox.block.entity.BlackboxBlockBlockEntity;
import io.github.marcschlicker.blackbox.block.entity.SpawnerBlackboxBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModScreens;
import io.github.marcschlicker.blackbox.network.BlackboxOutputFilterMessage;
import io.github.marcschlicker.blackbox.network.BlackboxSideConfigMessage;
import io.github.marcschlicker.blackbox.network.DimensionalWorkbenchGUIButtonMessage;
import io.github.marcschlicker.blackbox.network.FarmNameUpdateMessage;
import io.github.marcschlicker.blackbox.util.BlackboxSideMode;
import io.github.marcschlicker.blackbox.util.FarmAccessMode;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmMeasurementHost;
import io.github.marcschlicker.blackbox.util.LiveMeasurementOutputs;
import io.github.marcschlicker.blackbox.util.OutputFilterData;
import io.github.marcschlicker.blackbox.world.inventory.BlackBoxGuiMenu;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlackBoxGuiScreen extends AbstractContainerScreen<BlackBoxGuiMenu> implements BlackboxModScreens.ScreenAccessor {
	private static final int FILTER_COLUMNS = 6;
	private static final int FILTER_ROWS = 4;
	private static final int FILTER_PAGE_SIZE = FILTER_COLUMNS * FILTER_ROWS;
	private static final int FILTER_FLUID_Y = 138;
	private static final int FILTER_ENERGY_Y = 172;
	private static final int MAIN_PANEL_WIDTH = 196;
	private static final int SIDE_PANEL_WIDTH = 164;
	private static final int FILTER_PANEL_WIDTH = 142;
	private static final Direction[] CONFIGURED_SIDES = {Direction.DOWN, Direction.SOUTH, Direction.UP, Direction.WEST, Direction.NORTH, Direction.EAST};

	private int filterPage;
	// Keep the normal BlackBox inventory compact. The resource/routing and filter
	// views are optional side drawers, just like the configuration panel on an
	// Absorption Hopper.
	private boolean leftPanelOpen;
	private boolean workbenchPanelOpen;
	private boolean rightPanelOpen;
	private Button leftPanelButton;
	private Button workbenchPanelButton;
	private Button rightPanelButton;
	private Button enterFarmButton;
	private Button saveFarmNameButton;
	private Button farmAccessButton;
	private EditBox farmName;
	private Button enableAllButton;
	private Button spawnerArmorFilterButton;
	private Button spawnerGemFilterButton;
	private Button previousPageButton;
	private Button nextPageButton;
	private final Button[] sideButtons = new Button[CONFIGURED_SIDES.length];
	private ItemStack cachedFilterCore = ItemStack.EMPTY;
	private FarmCoreData.Recipe cachedFilterRecipe;
	private ItemStack cachedFilterUpgrade = ItemStack.EMPTY;
	private List<ItemStack> cachedDisabledItemOutputs = List.of();
	private List<FluidStack> cachedDisabledFluidOutputs = List.of();
	private Set<String> cachedDisabledSpawnerOutputGroups = Set.of();
	private boolean cachedEnergyEnabled = true;
	private Component filterFeedback = Component.empty();
	private boolean filterFeedbackAccepted;
	private int filterFeedbackTicks;
	private boolean farmNameInitialized;
	private int measurementScroll;
	private boolean wasMeasuring;

	public BlackBoxGuiScreen(BlackBoxGuiMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = MAIN_PANEL_WIDTH;
		this.imageHeight = 223;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
	}

	/** Applies the exact server-side state immediately instead of waiting for a slot refresh. */
	public void applyConfirmedFilterState(int x, int y, int z, boolean accepted, CompoundTag filterData) {
		if (x != this.menu.x || y != this.menu.y || z != this.menu.z) {
			return;
		}
		this.filterFeedbackAccepted = accepted;
		this.filterFeedback = Component.translatable(accepted ? "gui.blackbox.filter.saved" : "gui.blackbox.filter.rejected");
		this.filterFeedbackTicks = 40;
		if (!accepted) {
			return;
		}
		ItemStack upgrade = filterUpgrade();
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get())) {
			return;
		}
		ItemStack confirmedUpgrade = upgrade.copy();
		confirmedUpgrade.set(DataComponents.CUSTOM_DATA, CustomData.of(filterData.copy()));
		this.menu.getSlot(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT).set(confirmedUpgrade);
		this.cachedFilterUpgrade = ItemStack.EMPTY;
		refreshFilterUpgradeCache();
	}

	@Override
	protected void init() {
		this.imageWidth = totalGuiWidth();
		super.init();
		repositionGuiForDrawers();
		this.leftPanelButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			this.leftPanelOpen = !this.leftPanelOpen;
			this.workbenchPanelOpen = false;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
			updatePanelWidgets();
		}).bounds(this.leftPos + 2, this.topPos + 2, 16, 16).build());
		this.leftPanelButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.routing.toggle")));
		this.workbenchPanelButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			this.workbenchPanelOpen = !this.workbenchPanelOpen;
			this.leftPanelOpen = false;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
			updatePanelWidgets();
		}).bounds(this.leftPos + 20, this.topPos + 2, 16, 16).build());
		this.workbenchPanelButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.toggle")));
		this.rightPanelButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			this.rightPanelOpen = !this.rightPanelOpen;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
			updatePanelWidgets();
		}).bounds(this.leftPos + 178, this.topPos + 2, 16, 16).build());
		this.rightPanelButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.filter.toggle")));
		this.farmName = new EditBox(this.font, sidePanelX() + 6, this.topPos + 23, 88, 18, Component.translatable("gui.blackbox.workbench.name"));
		this.farmName.setMaxLength(32);
		this.farmName.setHint(Component.translatable("gui.blackbox.workbench.name"));
		this.addRenderableWidget(this.farmName);
		this.saveFarmNameButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> saveFarmName())
				.bounds(sidePanelX() + 98, this.topPos + 23, 18, 18).build());
		this.saveFarmNameButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.save_name")));
		this.enterFarmButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.workbench.enter_short"), button -> {
			saveFarmName();
			PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(0, this.menu.x, this.menu.y, this.menu.z));
		}).bounds(sidePanelX() + 6, this.topPos + 48, 67, 18).build());
		this.enterFarmButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.start")));
		this.farmAccessButton = this.addRenderableWidget(Button.builder(Component.empty(), button ->
				PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(1, this.menu.x, this.menu.y, this.menu.z)))
				.bounds(sidePanelX() + 77, this.topPos + 48, 81, 18).build());
		this.farmAccessButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.access_tooltip")));
		this.enableAllButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.filter.enable_all"), button ->
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.ENABLE_ALL, this.menu.x, this.menu.y, this.menu.z, -1, "")))
				.bounds(filterPanelX() + 48, this.topPos + 20, 86, 20).build());
		this.spawnerArmorFilterButton = this.addRenderableWidget(Button.builder(Component.empty(), button ->
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.TOGGLE_SPAWNER_ARMOR, this.menu.x, this.menu.y, this.menu.z, -1, "")))
				.bounds(filterPanelX() + 8, this.topPos + 20, 16, 20).build());
		this.spawnerGemFilterButton = this.addRenderableWidget(Button.builder(Component.empty(), button ->
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.TOGGLE_SPAWNER_GEMS, this.menu.x, this.menu.y, this.menu.z, -1, "")))
				.bounds(filterPanelX() + 28, this.topPos + 20, 16, 20).build());
		this.previousPageButton = this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.filterPage--)
				.bounds(filterPanelX() + 108, this.topPos + 5, 13, 13).build());
		this.nextPageButton = this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.filterPage++)
				.bounds(filterPanelX() + 124, this.topPos + 5, 13, 13).build());
		for (int index = 0; index < CONFIGURED_SIDES.length; index++) {
			Direction side = CONFIGURED_SIDES[index];
			this.sideButtons[index] = this.addRenderableWidget(Button.builder(Component.empty(), button ->
					PacketDistributor.sendToServer(new BlackboxSideConfigMessage(this.menu.x, this.menu.y, this.menu.z, side.get3DDataValue())))
					.bounds(sideButtonX(index), sideButtonY(index), 44, 18).build());
		}
		layoutDrawerWidgets();
		updatePanelWidgets();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.farmName != null && this.farmName.isFocused()) {
			this.farmName.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		updateWorkbenchControls();
		super.render(graphics, mouseX, mouseY, partialTick);
		renderDrawerToggleIcons(graphics);
		renderUpgradeSlotHints(graphics);
		renderSideResourceStatus(graphics, mouseX, mouseY);
		if (this.leftPanelOpen) {
			renderResourceStatus(graphics, mouseX, mouseY);
		} else if (this.workbenchPanelOpen) {
			renderWorkbenchStatus(graphics, mouseX, mouseY);
		}
		if (this.rightPanelOpen) {
			renderSpawnerCategoryFilterButtons(graphics);
			renderFilterOutputs(graphics, mouseX, mouseY);
		}
		renderSideTooltip(graphics, mouseX, mouseY);
		renderBucketTooltip(graphics, mouseX, mouseY);
		renderUpgradeSlotHintTooltip(graphics, mouseX, mouseY);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && this.rightPanelOpen && canConfigureFilter()) {
			int index = outputIndexAt(mouseX, mouseY);
			List<FilterItemOutput> outputs = filterItemOutputs();
			if (index >= 0 && index < outputs.size()) {
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.TOGGLE_OUTPUT, this.menu.x, this.menu.y, this.menu.z, index,
						isSpawnerBlackbox() ? outputs.get(index).filterGroup() : ""));
				return true;
			}
			int fluidIndex = fluidIndexAt(mouseX, mouseY);
			if (fluidIndex >= 0 && fluidIndex < filterFluidOutputs().size()) {
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.TOGGLE_FLUID, this.menu.x, this.menu.y, this.menu.z, fluidIndex, ""));
				return true;
			}
			if (inside((int) mouseX, (int) mouseY, filterPanelX() + 8, this.topPos + FILTER_ENERGY_Y, 16, 16) && filterEnergyOutput() > 0) {
				PacketDistributor.sendToServer(new BlackboxOutputFilterMessage(BlackboxOutputFilterMessage.TOGGLE_ENERGY, this.menu.x, this.menu.y, this.menu.z, 0, ""));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.workbenchPanelOpen
				&& inside((int) mouseX, (int) mouseY, sidePanelX(), this.topPos + 118, SIDE_PANEL_WIDTH, 96) && scrollY != 0) {
			int pages = measurementPageCount();
			if (pages > 1) {
				this.measurementScroll = Math.clamp(this.measurementScroll + (scrollY > 0 ? -1 : 1), 0, pages - 1);
				return true;
			}
		}
		if (this.rightPanelOpen && canConfigureFilter()
				&& inside((int) mouseX, (int) mouseY, filterPanelX(), this.topPos, FILTER_PANEL_WIDTH, this.imageHeight)
				&& filterPageCount() > 1 && scrollY != 0) {
			int direction = scrollY > 0 ? -1 : 1;
			this.filterPage = Math.clamp(this.filterPage + direction, 0, filterPageCount() - 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawMainPanel(graphics);
		boolean spawnerMode = isSpawnerBlackbox();
		if (spawnerMode) {
			for (int column = 0; column < 5; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 20, 0xFFB06EDE);
			}
			for (int column = 0; column < SpawnerBlackboxBlockEntity.MODULE_END - SpawnerBlackboxBlockEntity.MODULE_START; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 49, 0xFFE0786A);
			}
			drawSlot(graphics, this.leftPos + 170, this.topPos + 20, 0xFF68727D);
		} else {
			drawSlot(graphics, this.leftPos + 8, this.topPos + 20, 0xFF77CC88);
			drawSlot(graphics, this.leftPos + 134, this.topPos + 20, 0xFF68727D);
			drawSlot(graphics, this.leftPos + 152, this.topPos + 20, 0xFF68727D);
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 49, 0xFF5E8FB8);
			}
		}
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 78 + row * 18, 0xFF77A96E);
			}
		}
		drawSlot(graphics, this.leftPos + 8, this.topPos + 119, 0xFF5E8FB8);
		drawSlot(graphics, this.leftPos + 26, this.topPos + 119, 0xFF77A96E);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 141 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 199, 0xFF68727D);
		}
		if (this.leftPanelOpen) {
			drawSidePanel(graphics);
		} else if (this.workbenchPanelOpen) {
			drawWorkbenchPanel(graphics);
		}
		if (this.rightPanelOpen) {
			drawFilterPanel(graphics);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		boolean spawnerMode = isSpawnerBlackbox();
		graphics.drawString(this.font, Component.translatable(spawnerMode ? "gui.blackbox.spawner_blackbox.title" : "gui.blackbox.machine.title"), spawnerMode ? 21 : 40, 6, 0xFFE8EDF2, false);
		if (!spawnerMode) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.common.core"), 29, 24, 0xFFAAB4C0, false);
		}
		if (spawnerMode) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.spawners"), 104, 24, 0xFFD7B8F3, false);
			graphics.drawString(this.font, Component.translatable("gui.blackbox.spawner_blackbox.modules"), 104, 53, 0xFFFFAAA0, false);
		} else {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.inputs"), 8, 39, 0xFF83BCE6, false);
		}
		graphics.drawString(this.font, Component.translatable(spawnerMode ? "gui.blackbox.spawner_blackbox.outputs" : "gui.blackbox.machine.outputs"), 8, 68, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 48, 130, 0xFFAAB4C0, false);

		StatusLine status = statusLine();
		String statusText = this.font.plainSubstrByWidth(status.text().getString(), 122);
		graphics.drawCenteredString(this.font, statusText, 109, 119, status.color());

		if (this.leftPanelOpen) {
			int x = sidePanelRelativeX();
			graphics.drawString(this.font, Component.translatable("gui.blackbox.routing.title"), x + 6, 7, 0xFFE8EDF2, false);
			for (int index = 0; index < CONFIGURED_SIDES.length; index++) {
				graphics.drawString(this.font, Component.translatable("gui.blackbox.side." + CONFIGURED_SIDES[index].getSerializedName()), sideLabelRelativeX(index), 20 + index / 2 * 28,
						0xFFAAB4C0, false);
			}
			graphics.drawString(this.font, Component.translatable("gui.blackbox.fluids.title"), x + 6, 102, 0xFFE8EDF2, false);
			graphics.drawString(this.font, Component.translatable("gui.blackbox.energy.title"), x + 6, 174, 0xFFE8EDF2, false);
		} else if (this.workbenchPanelOpen) {
			int x = sidePanelRelativeX();
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.title"), x + 6, 7, 0xFFE8EDF2, false);
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.live_status"), x + 6, 78, 0xFFAAB4C0, false);
			graphics.drawString(this.font, Component.translatable(measurementActive() ? "gui.blackbox.workbench.produced_so_far" : "gui.blackbox.workbench.core_profile"), x + 6, 122, 0xFFAAB4C0, false);
		}

		if (this.rightPanelOpen) {
			int x = filterPanelRelativeX();
			graphics.drawString(this.font, Component.translatable("gui.blackbox.filter.title"), x + 6, 7, 0xFFE8EDF2, false);
			if (!canConfigureFilter()) {
				graphics.drawString(this.font, Component.translatable("gui.blackbox.filter.missing"), x + 6, 29, 0xFF9AA4B0, false);
			} else {
				graphics.drawString(this.font, Component.translatable("gui.blackbox.filter.legend"), x + 6, 29, 0xFF89C9E8, false);
				graphics.drawString(this.font, Component.translatable("gui.blackbox.filter.page", this.filterPage + 1, filterPageCount()), x + 6, 36, 0xFF9AA4B0, false);
				if (this.filterFeedbackTicks > 0) {
					graphics.drawString(this.font, this.filterFeedback, x + 6, 43, this.filterFeedbackAccepted ? 0xFF8DDB9B : 0xFFFF8080, false);
				}
				graphics.drawString(this.font, Component.translatable("gui.blackbox.fluids.title"), x + 6, 126, 0xFF73C7FF, false);
				if (!spawnerMode) {
					graphics.drawString(this.font, Component.translatable("gui.blackbox.energy.title"), x + 6, 160, 0xFFE2BE62, false);
				}
			}
		}
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (this.filterFeedbackTicks > 0) {
			this.filterFeedbackTicks--;
		}
		this.filterPage = Math.max(0, Math.min(this.filterPage, Math.max(0, filterPageCount() - 1)));
		boolean ready = canConfigureFilter();
		this.enableAllButton.active = ready && OutputFilterData.hasDisabledOutputs(filterUpgrade(), Minecraft.getInstance().level.registryAccess());
		boolean spawnerFilterReady = ready && isSpawnerBlackbox();
		this.spawnerArmorFilterButton.active = spawnerFilterReady;
		this.spawnerGemFilterButton.active = spawnerFilterReady;
		this.spawnerArmorFilterButton.setTooltip(Tooltip.create(Component.translatable(OutputFilterData.isSpawnerArmorEnabled(filterUpgrade())
				? "gui.blackbox.filter.spawner.armor.enabled" : "gui.blackbox.filter.spawner.armor.disabled")));
		this.spawnerGemFilterButton.setTooltip(Tooltip.create(Component.translatable(OutputFilterData.isSpawnerGemsEnabled(filterUpgrade())
				? "gui.blackbox.filter.spawner.gems.enabled" : "gui.blackbox.filter.spawner.gems.disabled")));
		this.previousPageButton.active = ready && this.filterPage > 0;
		this.nextPageButton.active = ready && this.filterPage + 1 < filterPageCount();
		boolean measuring = measurementActive();
		if (measuring && !this.wasMeasuring) {
			this.measurementScroll = 0;
		}
		this.wasMeasuring = measuring;
		this.measurementScroll = Math.clamp(this.measurementScroll, 0, Math.max(0, measurementPageCount() - 1));
		boolean normalBlackbox = !isSpawnerBlackbox();
		this.workbenchPanelButton.active = normalBlackbox;
		this.workbenchPanelButton.visible = normalBlackbox;
		if (!normalBlackbox && this.workbenchPanelOpen) {
			this.workbenchPanelOpen = false;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
		}
		BlackboxBlockBlockEntity blackbox = blackbox();
		for (int index = 0; index < CONFIGURED_SIDES.length; index++) {
			BlackboxSideMode mode = blackbox == null ? BlackboxSideMode.DISABLED : blackbox.getSideMode(CONFIGURED_SIDES[index]);
			this.sideButtons[index].active = blackbox != null;
			this.sideButtons[index].setMessage(Component.literal(modeButtonLabel(mode)).withStyle(style -> style.withColor(modeColor(mode))));
			this.sideButtons[index].setTooltip(Tooltip.create(sideTooltip(CONFIGURED_SIDES[index], mode)));
		}
		updatePanelWidgets();
	}

	private void updatePanelWidgets() {
		if (this.enableAllButton == null) {
			return;
		}
		this.enableAllButton.visible = this.rightPanelOpen;
		this.spawnerArmorFilterButton.visible = this.rightPanelOpen && isSpawnerBlackbox();
		this.spawnerGemFilterButton.visible = this.rightPanelOpen && isSpawnerBlackbox();
		this.previousPageButton.visible = this.rightPanelOpen;
		this.nextPageButton.visible = this.rightPanelOpen;
		boolean showWorkbench = this.workbenchPanelOpen && !isSpawnerBlackbox();
		this.farmName.visible = showWorkbench;
		this.saveFarmNameButton.visible = showWorkbench;
		this.enterFarmButton.visible = showWorkbench;
		this.farmAccessButton.visible = showWorkbench;
		for (Button sideButton : this.sideButtons) {
			if (sideButton != null) {
				sideButton.visible = this.leftPanelOpen;
			}
		}
	}

	private void layoutDrawerWidgets() {
		if (this.leftPanelButton == null) {
			return;
		}
		this.leftPanelButton.setPosition(this.leftPos + 2, this.topPos + 2);
		this.workbenchPanelButton.setPosition(this.leftPos + 20, this.topPos + 2);
		this.rightPanelButton.setPosition(this.leftPos + 178, this.topPos + 2);
		this.farmName.setPosition(sidePanelX() + 6, this.topPos + 23);
		this.saveFarmNameButton.setPosition(sidePanelX() + 98, this.topPos + 23);
		this.enterFarmButton.setPosition(sidePanelX() + 6, this.topPos + 48);
		this.farmAccessButton.setPosition(sidePanelX() + 77, this.topPos + 48);
		this.enableAllButton.setPosition(filterPanelX() + 48, this.topPos + 20);
		this.spawnerArmorFilterButton.setPosition(filterPanelX() + 8, this.topPos + 20);
		this.spawnerGemFilterButton.setPosition(filterPanelX() + 28, this.topPos + 20);
		this.previousPageButton.setPosition(filterPanelX() + 108, this.topPos + 5);
		this.nextPageButton.setPosition(filterPanelX() + 124, this.topPos + 5);
		for (int index = 0; index < this.sideButtons.length; index++) {
			if (this.sideButtons[index] != null) {
				this.sideButtons[index].setPosition(sideButtonX(index), sideButtonY(index));
			}
		}
	}

	private void renderSideTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!this.leftPanelOpen) {
			return;
		}
		for (int index = 0; index < this.sideButtons.length; index++) {
			if (this.sideButtons[index].isHovered()) {
				BlackboxBlockBlockEntity blackbox = blackbox();
				BlackboxSideMode mode = blackbox == null ? BlackboxSideMode.DISABLED : blackbox.getSideMode(CONFIGURED_SIDES[index]);
				graphics.renderTooltip(this.font, sideTooltip(CONFIGURED_SIDES[index], mode), mouseX, mouseY);
				return;
			}
		}
	}

	private void renderBucketTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (inside(mouseX, mouseY, this.leftPos + 8, this.topPos + 119, 16, 16)) {
			graphics.renderTooltip(this.font, Component.translatable("gui.blackbox.bucket.input.tooltip"), mouseX, mouseY);
		} else if (inside(mouseX, mouseY, this.leftPos + 26, this.topPos + 119, 16, 16)) {
			graphics.renderTooltip(this.font, Component.translatable("gui.blackbox.bucket.output.tooltip"), mouseX, mouseY);
		}
	}

	private void renderDrawerToggleIcons(GuiGraphics graphics) {
		graphics.renderItem(new ItemStack(Items.HOPPER), this.leftPos + 2, this.topPos + 2);
		if (!isSpawnerBlackbox()) {
			graphics.renderItem(new ItemStack(Items.CRAFTING_TABLE), this.leftPos + 20, this.topPos + 2);
		}
		graphics.renderItem(new ItemStack(Items.COMPARATOR), this.leftPos + 178, this.topPos + 2);
	}

	/** Shows empty upgrade slots as muted, discoverable capability hints. */
	private void renderUpgradeSlotHints(GuiGraphics graphics) {
		if (isSpawnerBlackbox()) {
			renderUpgradeSlotHint(graphics, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, this.leftPos + 170, this.topPos + 20,
					new ItemStack(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()));
			return;
		}
		renderUpgradeSlotHint(graphics, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, this.leftPos + 134, this.topPos + 20,
				new ItemStack(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()));
		renderUpgradeSlotHint(graphics, BlackboxBlockBlockEntity.UPGRADE_SLOT, this.leftPos + 152, this.topPos + 20,
				new ItemStack(BlackboxModItems.STABILITY_UPGRADE.get()));
	}

	private void renderUpgradeSlotHint(GuiGraphics graphics, int slotIndex, int x, int y, ItemStack hint) {
		if (!this.menu.getSlot(slotIndex).getItem().isEmpty()) {
			return;
		}
		graphics.renderItem(hint, x, y);
		graphics.fill(x, y, x + 16, y + 16, 0xB0A4AAB2);
	}

	private void renderUpgradeSlotHintTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (isSpawnerBlackbox()) {
			renderUpgradeSlotHintTooltip(graphics, mouseX, mouseY, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, this.leftPos + 170, this.topPos + 20,
					"gui.blackbox.upgrade_slot.filter");
			return;
		}
		renderUpgradeSlotHintTooltip(graphics, mouseX, mouseY, BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT, this.leftPos + 134, this.topPos + 20,
					"gui.blackbox.upgrade_slot.filter");
		renderUpgradeSlotHintTooltip(graphics, mouseX, mouseY, BlackboxBlockBlockEntity.UPGRADE_SLOT, this.leftPos + 152, this.topPos + 20,
					"gui.blackbox.upgrade_slot.stability");
	}

	private void renderUpgradeSlotHintTooltip(GuiGraphics graphics, int mouseX, int mouseY, int slotIndex, int x, int y, String translationKey) {
		if (this.menu.getSlot(slotIndex).getItem().isEmpty() && inside(mouseX, mouseY, x, y, 16, 16)) {
			graphics.renderTooltip(this.font, Component.translatable(translationKey), mouseX, mouseY);
		}
	}

	private void renderSideResourceStatus(GuiGraphics graphics, int mouseX, int mouseY) {
		BlackboxBlockBlockEntity blackbox = blackbox();
		if (blackbox == null) {
			return;
		}
		int x = this.leftPos + 179;
		renderVerticalResourcePair(graphics, mouseX, mouseY, x, this.topPos + 20, 95,
				blackbox.resources().inputFluids().getFluidInTank(0).getAmount(), blackbox.resources().inputFluids().getTankCapacity(0), 0xFF5E8FB8,
				compactFluidTooltip(blackbox.resources().inputFluids().getFluidInTank(0), blackbox.resources().inputFluids().getTankCapacity(0), true),
				blackbox.resources().outputFluids().getFluidInTank(0).getAmount(), blackbox.resources().outputFluids().getTankCapacity(0), 0xFF77A96E,
				compactFluidTooltip(blackbox.resources().outputFluids().getFluidInTank(0), blackbox.resources().outputFluids().getTankCapacity(0), false));
		renderVerticalResourcePair(graphics, mouseX, mouseY, x, this.topPos + 121, 95,
				blackbox.resources().inputEnergy().getEnergyStored(), blackbox.resources().inputEnergy().getMaxEnergyStored(), 0xFFCC83FF,
				compactEnergyTooltip(blackbox.resources().inputEnergy().getEnergyStored(), blackbox.resources().inputEnergy().getMaxEnergyStored(), true),
				blackbox.resources().outputEnergy().getEnergyStored(), blackbox.resources().outputEnergy().getMaxEnergyStored(), 0xFFE2BE62,
				compactEnergyTooltip(blackbox.resources().outputEnergy().getEnergyStored(), blackbox.resources().outputEnergy().getMaxEnergyStored(), false));
	}

	private void renderVerticalResourcePair(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int height,
			int firstAmount, int firstCapacity, int firstColor, Component firstTooltip,
			int secondAmount, int secondCapacity, int secondColor, Component secondTooltip) {
		graphics.fill(x - 1, y - 1, x + 13, y + height + 1, 0xFF596572);
		renderVerticalResourceBar(graphics, x, y, height, firstAmount, firstCapacity, firstColor);
		renderVerticalResourceBar(graphics, x + 7, y, height, secondAmount, secondCapacity, secondColor);
		if (inside(mouseX, mouseY, x, y, 5, height)) {
			graphics.renderTooltip(this.font, firstTooltip, mouseX, mouseY);
		} else if (inside(mouseX, mouseY, x + 7, y, 5, height)) {
			graphics.renderTooltip(this.font, secondTooltip, mouseX, mouseY);
		}
	}

	private static void renderVerticalResourceBar(GuiGraphics graphics, int x, int y, int height, int amount, int capacity, int color) {
		graphics.fill(x, y, x + 5, y + height, 0xFF0C0F13);
		if (amount > 0) {
			int filled = Math.max(1, Math.round(height * amount / (float) Math.max(1, capacity)));
			graphics.fill(x, y + height - filled, x + 5, y + height, color);
		}
	}

	private static Component compactFluidTooltip(FluidStack fluid, int capacity, boolean input) {
		Component label = Component.translatable(input ? "gui.blackbox.fluids.input" : "gui.blackbox.fluids.output");
		if (fluid.isEmpty()) {
			return label.copy().append(Component.literal(": ")).append(Component.translatable("gui.blackbox.fluids.empty"));
		}
		return label.copy().append(Component.literal(": ")).append(fluid.getHoverName())
				.append(Component.literal(" (" + fluid.getAmount() + " / " + capacity + " mB)"));
	}

	private static Component compactEnergyTooltip(int energy, int capacity, boolean input) {
		Component label = Component.translatable(input ? "gui.blackbox.energy.input" : "gui.blackbox.energy.output");
		return label.copy().append(Component.literal(": " + energy + " / " + capacity + " FE"));
	}

	private void drawMainPanel(GuiGraphics graphics) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + 2, 0xFF4C9B70);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(this.leftPos + MAIN_PANEL_WIDTH - 1, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void drawSidePanel(GuiGraphics graphics) {
		drawExtendedPanel(graphics, sidePanelX(), SIDE_PANEL_WIDTH, 0xFF121820, 0xFF4C9B70);
	}

	private void drawWorkbenchPanel(GuiGraphics graphics) {
		drawExtendedPanel(graphics, sidePanelX(), SIDE_PANEL_WIDTH, 0xFF121820, 0xFF4C9B70);
		int x = sidePanelX() + 6;
		graphics.fill(x, this.topPos + 101, x + 152, this.topPos + 104, 0xFF303A45);
		if (measurementActive()) {
			graphics.fill(x, this.topPos + 101, x + 152 * this.menu.getCalculationProgressPercent() / 100, this.topPos + 104, 0xFF4B98D0);
		}
		for (int index = 0; index < 10; index++) {
			drawSlot(graphics, x + index % 5 * 30, this.topPos + 140 + index / 5 * 30, 0xFF77A96E);
		}
	}

	private void drawFilterPanel(GuiGraphics graphics) {
		int x = filterPanelX();
		drawExtendedPanel(graphics, x, FILTER_PANEL_WIDTH, 0xFF121820, 0xFF6B9DB7);
		for (int row = 0; row < FILTER_ROWS; row++) {
			for (int column = 0; column < FILTER_COLUMNS; column++) {
				drawSlot(graphics, x + 8 + column * 18, this.topPos + 50 + row * 18, 0xFF5E8FB8);
			}
		}
		for (int column = 0; column < FILTER_COLUMNS; column++) {
			drawSlot(graphics, x + 8 + column * 18, this.topPos + FILTER_FLUID_Y, 0xFF73C7FF);
		}
		drawSlot(graphics, x + 8, this.topPos + FILTER_ENERGY_Y, 0xFFE2BE62);
	}

	private void drawExtendedPanel(GuiGraphics graphics, int x, int width, int fillColor, int accentColor) {
		graphics.fill(x, this.topPos, x + width, this.topPos + this.imageHeight, fillColor);
		graphics.fill(x, this.topPos, x + width, this.topPos + 2, accentColor);
		graphics.fill(x, this.topPos, x + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(x + width - 1, this.topPos, x + width, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void renderResourceStatus(GuiGraphics graphics, int mouseX, int mouseY) {
		BlackboxBlockBlockEntity blackbox = blackbox();
		if (blackbox == null) {
			return;
		}
		int x = sidePanelX();
		renderFluidTank(graphics, mouseX, mouseY, x + 6, this.topPos + 114, 72, 55,
				blackbox.resources().inputFluids().getFluidInTank(0), blackbox.resources().inputFluids().getTankCapacity(0), true);
		renderFluidTank(graphics, mouseX, mouseY, x + 86, this.topPos + 114, 72, 55,
				blackbox.resources().outputFluids().getFluidInTank(0), blackbox.resources().outputFluids().getTankCapacity(0), false);
		renderEnergyBar(graphics, mouseX, mouseY, x + 6, this.topPos + 187, 152,
				blackbox.resources().inputEnergy().getEnergyStored(), blackbox.resources().inputEnergy().getMaxEnergyStored(), true);
		renderEnergyBar(graphics, mouseX, mouseY, x + 6, this.topPos + 201, 152,
				blackbox.resources().outputEnergy().getEnergyStored(), blackbox.resources().outputEnergy().getMaxEnergyStored(), false);
	}

	private void renderWorkbenchStatus(GuiGraphics graphics, int mouseX, int mouseY) {
		FarmMeasurementHost host = measurementHost();
		if (host == null) {
			return;
		}
		int x = sidePanelX() + 6;
		int phase = this.menu.getCalculationPhase();
		if (phase == 1) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.calculation_preparing", formatDuration(this.menu.getCalculationSecondsRemaining())), x, this.topPos + 88, 0xFFFFC866, false);
		} else if (phase == 2) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.calculation_running", formatDuration(this.menu.getCalculationSecondsRemaining()), this.menu.getCalculationProgressPercent()), x, this.topPos + 88, 0xFF73C7FF, false);
			Component rate = this.menu.getCalculationItemsPerMinute() > 0
					? Component.translatable("gui.blackbox.workbench.calculation_rate", this.menu.getCalculationItemsPerMinute())
					: Component.translatable("gui.blackbox.workbench.calculation_rate_waiting");
			graphics.drawString(this.font, rate, x, this.topPos + 111, 0xFF8DDB9B, false);
		} else if (currentProfile().profile()) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.profile_saved"), x, this.topPos + 88, 0xFF8DDB9B, false);
		} else {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.no_measurement"), x, this.topPos + 88, 0xFF9AA4B0, false);
		}
		WorkbenchOutputData outputData = currentProfile();
		List<WorkbenchItemOutput> items = outputData.items();
		List<FarmCoreData.FluidAmount> fluids = outputData.fluids();
		long energy = outputData.energy();
		int start = this.measurementScroll * 10;
		for (int index = 0; index < 10 && start + index < items.size(); index++) {
			WorkbenchItemOutput entry = items.get(start + index);
			int itemX = x + index % 5 * 30;
			int itemY = this.topPos + 140 + index / 5 * 30;
			graphics.renderItem(entry.stack(), itemX, itemY);
			String amount = outputData.profile() ? formatItemRate(entry, outputData.sampleTicks()) + "/m" : formatAmount(entry.amount());
			graphics.renderItemDecorations(this.font, entry.stack(), itemX, itemY, amount);
			if (inside(mouseX, mouseY, itemX, itemY, 16, 16)) {
				if (outputData.profile()) {
					graphics.renderComponentTooltip(this.font, List.of(entry.stack().getHoverName(),
							Component.translatable("gui.blackbox.workbench.profile_rate", formatItemRate(entry, outputData.sampleTicks()))), mouseX, mouseY);
				} else {
					graphics.renderTooltip(this.font, entry.stack(), mouseX, mouseY);
				}
			}
		}
		if (items.isEmpty() && fluids.isEmpty() && energy <= 0) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.no_output_yet"), x, this.topPos + 140, 0xFF718090, false);
		}
		renderMeasuredResourceOutputs(graphics, mouseX, mouseY, x, fluids, energy, outputData.sampleTicks(), outputData.profile());
		if (measurementPageCount() > 1) {
			graphics.drawString(this.font, (this.measurementScroll + 1) + "/" + measurementPageCount(), x + 125, this.topPos + 122, 0xFF9AA4B0, false);
		}
	}

	private void renderMeasuredResourceOutputs(GuiGraphics graphics, int mouseX, int mouseY, int x,
			List<FarmCoreData.FluidAmount> fluids, long energy, int sampleTicks, boolean profile) {
		int fluidY = this.topPos + 196;
		int energyY = this.topPos + 208;
		int fluidIndex = fluids.isEmpty() ? 0 : Math.min(this.measurementScroll, fluids.size() - 1);
		graphics.fill(x, fluidY + 2, x + 3, fluidY + 10, 0xFF73C7FF);
		if (fluidIndex < fluids.size()) {
			FarmCoreData.FluidAmount output = fluids.get(fluidIndex);
			String amount = profile ? formatRate(output.amount(), sampleTicks) + " mB/m" : formatAmount(output.amount()) + " mB";
			String text = this.font.plainSubstrByWidth(output.stack().getHoverName().getString() + ": " + amount, 144);
			graphics.drawString(this.font, text, x + 6, fluidY + 1, 0xFF73C7FF, false);
			if (inside(mouseX, mouseY, x, fluidY, 152, 11)) {
				graphics.renderTooltip(this.font, profile
						? Component.translatable("gui.blackbox.workbench.profile_fluid_rate", output.stack().getHoverName(), formatRate(output.amount(), sampleTicks))
						: Component.translatable("gui.blackbox.workbench.produced_fluid", output.stack().getHoverName(), output.amount()), mouseX, mouseY);
			}
		} else {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.produced_fluid_empty"), x + 6, fluidY + 1, 0xFF718090, false);
		}

		graphics.fill(x, energyY + 2, x + 3, energyY + 10, 0xFFE2BE62);
		Component energyText = profile ? Component.translatable("gui.blackbox.workbench.profile_energy_rate", formatRate(energy, sampleTicks))
				: Component.translatable("gui.blackbox.workbench.produced_energy", formatAmount(energy));
		graphics.drawString(this.font, energyText, x + 6, energyY + 1,
				energy > 0 ? 0xFFE2BE62 : 0xFF718090, false);
		if (inside(mouseX, mouseY, x, energyY, 152, 11)) {
			graphics.renderTooltip(this.font, profile
					? Component.translatable("gui.blackbox.workbench.profile_energy_rate", formatRate(energy, sampleTicks))
					: Component.translatable("gui.blackbox.workbench.produced_energy", energy), mouseX, mouseY);
		}
	}

	private void renderFluidTank(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, FluidStack fluid, int capacity, boolean input) {
		int color = input ? 0xFF5E8FB8 : 0xFF77A96E;
		graphics.fill(x, y, x + width, y + height, 0xFF596572);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0C0F13);
		if (!fluid.isEmpty()) {
			int filled = Math.max(1, Math.round((height - 2) * fluid.getAmount() / (float) Math.max(1, capacity)));
			graphics.fill(x + 1, y + height - 1 - filled, x + width - 1, y + height - 1, (color & 0x00FFFFFF) | 0x88000000);
		}
		Component label = Component.translatable(input ? "gui.blackbox.fluids.input" : "gui.blackbox.fluids.output");
		graphics.drawCenteredString(this.font, label, x + width / 2, y + 4, color);
		String name = fluid.isEmpty() ? Component.translatable("gui.blackbox.fluids.empty").getString() : this.font.plainSubstrByWidth(fluid.getHoverName().getString(), width - 6);
		graphics.drawCenteredString(this.font, name, x + width / 2, y + 15, fluid.isEmpty() ? 0xFF9AA4B0 : 0xFFE8EDF2);
		graphics.drawCenteredString(this.font, fluid.getAmount() + " mB", x + width / 2, y + 27, fluid.isEmpty() ? 0xFF718090 : color);
		graphics.drawCenteredString(this.font, "/ " + capacity + " mB", x + width / 2, y + 38, fluid.isEmpty() ? 0xFF718090 : color);
		if (inside(mouseX, mouseY, x, y, width, height)) {
			Component tooltip = fluid.isEmpty()
					? label.copy().append(Component.literal(": ")).append(Component.translatable("gui.blackbox.fluids.empty"))
					: Component.literal(fluid.getHoverName().getString() + ": " + fluid.getAmount() + " / " + capacity + " mB");
			graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	private void renderEnergyBar(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int energy, int capacity, boolean input) {
		int color = input ? 0xFFCC83FF : 0xFFE2BE62;
		graphics.fill(x, y, x + width, y + 9, 0xFF0C0F13);
		if (energy > 0) {
			int filled = Math.max(1, Math.round(width * energy / (float) Math.max(1, capacity)));
			graphics.fill(x, y, x + filled, y + 9, (color & 0x00FFFFFF) | 0x88000000);
		}
		String side = Component.translatable(input ? "gui.blackbox.energy.input" : "gui.blackbox.energy.output").getString();
		String label = this.font.plainSubstrByWidth(side + ": " + formatEnergy(energy) + " / " + formatEnergy(capacity) + " FE", width - 4);
		graphics.drawString(this.font, label, x + 2, y + 1, color, false);
		if (inside(mouseX, mouseY, x, y, width, 9)) {
			graphics.renderTooltip(this.font, Component.literal(side + ": " + energy + " / " + capacity + " FE"), mouseX, mouseY);
		}
	}

	private void renderFilterOutputs(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!canConfigureFilter()) {
			return;
		}
		List<FilterItemOutput> outputs = filterItemOutputs();
		int start = this.filterPage * FILTER_PAGE_SIZE;
		for (int localIndex = 0; localIndex < FILTER_PAGE_SIZE && start + localIndex < outputs.size(); localIndex++) {
			int column = localIndex % FILTER_COLUMNS;
			int row = localIndex / FILTER_COLUMNS;
			int x = filterPanelX() + 8 + column * 18;
			int y = this.topPos + 50 + row * 18;
			FilterItemOutput outputEntry = outputs.get(start + localIndex);
			ItemStack output = outputEntry.stack();
			boolean enabled = isItemOutputEnabled(outputEntry);
			graphics.renderItem(output, x, y);
			if (!enabled) {
				graphics.fill(x, y, x + 16, y + 16, 0xAA8D2020);
				graphics.drawString(this.font, "X", x + 4, y + 3, 0xFFFF8080, false);
			}
			if (inside(mouseX, mouseY, x, y, 16, 16)) {
				if (outputEntry.observed()) {
					graphics.renderComponentTooltip(this.font, List.of(output.getHoverName(),
							Component.translatable("gui.blackbox.spawner_blackbox.drop_total", outputEntry.amount()),
							Component.translatable("gui.blackbox.spawner_blackbox.drop_chance", formatChance(outputEntry.occurrences(), outputEntry.totalRolls()))), mouseX, mouseY);
				} else {
					graphics.renderComponentTooltip(this.font, List.of(output.getHoverName(),
							Component.translatable("gui.blackbox.filter.rate_item.total", outputEntry.amount()),
							Component.translatable("gui.blackbox.filter.rate_item.minute", formatRate(outputEntry.amount(), filterSampleTicks()))), mouseX, mouseY);
				}
			}
		}
		List<FarmCoreData.FluidAmount> fluids = filterFluidOutputs();
		int fluidStart = this.filterPage * FILTER_COLUMNS;
		for (int localIndex = 0; localIndex < FILTER_COLUMNS && fluidStart + localIndex < fluids.size(); localIndex++) {
			int x = filterPanelX() + 8 + localIndex * 18;
			int y = this.topPos + FILTER_FLUID_Y;
			FarmCoreData.FluidAmount outputEntry = fluids.get(fluidStart + localIndex);
			FluidStack output = outputEntry.stack();
			boolean enabled = isFluidOutputEnabled(output);
			graphics.renderItem(fluidIcon(output), x, y);
			if (!enabled) {
				drawDisabledOverlay(graphics, x, y);
			}
			if (inside(mouseX, mouseY, x, y, 16, 16)) {
				graphics.renderComponentTooltip(this.font, List.of(output.getHoverName(),
						Component.translatable("gui.blackbox.filter.rate_fluid.total", outputEntry.amount()),
						Component.translatable("gui.blackbox.filter.rate_fluid.minute", formatRate(outputEntry.amount(), filterSampleTicks()))), mouseX, mouseY);
			}
		}
		if (filterEnergyOutput() > 0) {
			int x = filterPanelX() + 8;
			int y = this.topPos + FILTER_ENERGY_Y;
			boolean enabled = isEnergyOutputEnabled();
			graphics.renderItem(new ItemStack(Items.REDSTONE), x, y);
			if (!enabled) {
				drawDisabledOverlay(graphics, x, y);
			}
			if (inside(mouseX, mouseY, x, y, 16, 16)) {
				graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.blackbox.energy.title"),
						Component.translatable("gui.blackbox.filter.rate_energy.total", filterEnergyOutput()),
						Component.translatable("gui.blackbox.filter.rate_energy.minute", formatRate(filterEnergyOutput(), filterSampleTicks()))), mouseX, mouseY);
			}
		}
	}

	private void drawDisabledOverlay(GuiGraphics graphics, int x, int y) {
		graphics.fill(x, y, x + 16, y + 16, 0xAA8D2020);
		graphics.drawString(this.font, "X", x + 4, y + 3, 0xFFFF8080, false);
	}

	private void renderSpawnerCategoryFilterButtons(GuiGraphics graphics) {
		if (!isSpawnerBlackbox() || !canConfigureFilter()) {
			return;
		}
		int x = filterPanelX();
		int y = this.topPos + 22;
		graphics.renderItem(new ItemStack(Items.IRON_CHESTPLATE), x + 8, y);
		if (!OutputFilterData.isSpawnerArmorEnabled(filterUpgrade())) {
			drawDisabledOverlay(graphics, x + 8, y);
		}
		graphics.renderItem(new ItemStack(Items.EMERALD), x + 28, y);
		if (!OutputFilterData.isSpawnerGemsEnabled(filterUpgrade())) {
			drawDisabledOverlay(graphics, x + 28, y);
		}
	}

	private static ItemStack fluidIcon(FluidStack fluid) {
		ItemStack bucket = new ItemStack(fluid.getFluid().getBucket());
		return bucket.isEmpty() ? new ItemStack(Items.BUCKET) : bucket;
	}

	private boolean canConfigureFilter() {
		ItemStack core = this.menu.getSlot(BlackboxBlockBlockEntity.CORE_SLOT).getItem();
		boolean validCore = isSpawnerBlackbox()
				? spawnerBlackbox() != null && spawnerBlackbox().isReady()
				: core.is(BlackboxModItems.DIMENSION_CORE.get()) && (FarmCoreData.isProgrammed(core) || measurementActive());
		return validCore && filterUpgrade().is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) && Minecraft.getInstance().level != null;
	}

	private ItemStack filterUpgrade() {
		return this.menu.getSlot(BlackboxBlockBlockEntity.FILTER_UPGRADE_SLOT).getItem();
	}

	private boolean isItemOutputEnabled(ItemStack output) {
		refreshFilterUpgradeCache();
		return this.cachedDisabledItemOutputs.stream().noneMatch(saved -> ItemStack.isSameItemSameComponents(saved, output));
	}

	private boolean isItemOutputEnabled(FilterItemOutput output) {
		if (!isItemOutputEnabled(output.stack())) {
			return false;
		}
		return output.filterGroup().isEmpty() || !this.cachedDisabledSpawnerOutputGroups.contains(output.filterGroup());
	}

	private boolean isFluidOutputEnabled(FluidStack output) {
		refreshFilterUpgradeCache();
		return this.cachedDisabledFluidOutputs.stream().noneMatch(saved -> FluidStack.isSameFluidSameComponents(saved, output));
	}

	private boolean isEnergyOutputEnabled() {
		refreshFilterUpgradeCache();
		return this.cachedEnergyEnabled;
	}

	private void refreshFilterUpgradeCache() {
		ItemStack upgrade = filterUpgrade();
		if (ItemStack.isSameItemSameComponents(this.cachedFilterUpgrade, upgrade)) {
			return;
		}
		this.cachedFilterUpgrade = upgrade.copy();
		if (!upgrade.is(BlackboxModItems.OUTPUT_FILTER_UPGRADE.get()) || Minecraft.getInstance().level == null) {
			this.cachedDisabledItemOutputs = List.of();
			this.cachedDisabledFluidOutputs = List.of();
			this.cachedDisabledSpawnerOutputGroups = Set.of();
			this.cachedEnergyEnabled = true;
			return;
		}
		this.cachedDisabledItemOutputs = OutputFilterData.disabledItemOutputs(upgrade, Minecraft.getInstance().level.registryAccess());
		this.cachedDisabledFluidOutputs = OutputFilterData.disabledFluidOutputs(upgrade, Minecraft.getInstance().level.registryAccess());
		this.cachedDisabledSpawnerOutputGroups = OutputFilterData.disabledSpawnerOutputGroups(upgrade);
		this.cachedEnergyEnabled = OutputFilterData.isEnergyEnabled(upgrade);
	}

	private List<FilterItemOutput> filterItemOutputs() {
		SpawnerBlackboxBlockEntity spawner = spawnerBlackbox();
		if (spawner != null) {
			long totalRolls = spawner.totalLootRolls();
			return spawner.observedDrops().stream()
					.map(drop -> new FilterItemOutput(drop.stack(), drop.filterGroup(), drop.amount(), drop.occurrences(), totalRolls, true)).toList();
		}
		if (measurementActive()) {
			return LiveMeasurementOutputs.items(measurementHost()).stream()
					.map(output -> new FilterItemOutput(output.stack(), "", output.amount(), 0L, 0L, false)).toList();
		}
		FarmCoreData.Recipe recipe = filterRecipe();
		return recipe == null ? List.of() : recipe.outputTemplates().stream()
				.map(output -> new FilterItemOutput(output.stack(), "", output.amount(), 0L, 0L, false)).toList();
	}

	private List<FarmCoreData.FluidAmount> filterFluidOutputs() {
		if (measurementActive()) {
			return LiveMeasurementOutputs.fluids(measurementHost());
		}
		FarmCoreData.Recipe recipe = filterRecipe();
		return recipe == null ? List.of() : recipe.fluidOutputs();
	}

	private long filterEnergyOutput() {
		if (measurementActive()) {
			return LiveMeasurementOutputs.energy(measurementHost());
		}
		FarmCoreData.Recipe recipe = filterRecipe();
		return recipe == null ? 0 : recipe.energyOutput();
	}

	private int filterSampleTicks() {
		if (measurementActive()) {
			return Math.max(1, this.menu.getCalculationTotalTicks());
		}
		FarmCoreData.Recipe recipe = filterRecipe();
		return recipe == null ? 1 : Math.max(1, recipe.sampleTicks());
	}

	/** FarmCoreData parses serialized stacks. Cache it until the core's components actually change. */
	private FarmCoreData.Recipe filterRecipe() {
		if (isSpawnerBlackbox()) {
			return null;
		}
		ItemStack core = this.menu.getSlot(BlackboxBlockBlockEntity.CORE_SLOT).getItem();
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core) || Minecraft.getInstance().level == null) {
			this.cachedFilterCore = ItemStack.EMPTY;
			this.cachedFilterRecipe = null;
			return null;
		}
		if (!ItemStack.isSameItemSameComponents(this.cachedFilterCore, core)) {
			this.cachedFilterCore = core.copy();
			this.cachedFilterRecipe = FarmCoreData.read(core, Minecraft.getInstance().level.registryAccess());
		}
		return this.cachedFilterRecipe;
	}

	private int filterPageCount() {
		int itemPages = (filterItemOutputs().size() + FILTER_PAGE_SIZE - 1) / FILTER_PAGE_SIZE;
		int fluidPages = (filterFluidOutputs().size() + FILTER_COLUMNS - 1) / FILTER_COLUMNS;
		return Math.max(1, Math.max(itemPages, fluidPages));
	}

	private int outputIndexAt(double mouseX, double mouseY) {
		int relativeX = (int) mouseX - (filterPanelX() + 8);
		int relativeY = (int) mouseY - (this.topPos + 50);
		if (relativeX < 0 || relativeY < 0) {
			return -1;
		}
		int column = relativeX / 18;
		int row = relativeY / 18;
		if (column >= FILTER_COLUMNS || row >= FILTER_ROWS || relativeX % 18 >= 16 || relativeY % 18 >= 16) {
			return -1;
		}
		return this.filterPage * FILTER_PAGE_SIZE + row * FILTER_COLUMNS + column;
	}

	private int fluidIndexAt(double mouseX, double mouseY) {
		int relativeX = (int) mouseX - (filterPanelX() + 8);
		int relativeY = (int) mouseY - (this.topPos + FILTER_FLUID_Y);
		if (relativeX < 0 || relativeY < 0 || relativeY >= 16 || relativeX % 18 >= 16) {
			return -1;
		}
		int column = relativeX / 18;
		return column >= FILTER_COLUMNS ? -1 : this.filterPage * FILTER_COLUMNS + column;
	}

	private BlackboxBlockBlockEntity blackbox() {
		return this.menu.world.getBlockEntity(new BlockPos(this.menu.x, this.menu.y, this.menu.z)) instanceof BlackboxBlockBlockEntity blackbox ? blackbox : null;
	}

	private FarmMeasurementHost measurementHost() {
		BlackboxBlockBlockEntity blackbox = blackbox();
		return !isSpawnerBlackbox() ? blackbox : null;
	}

	private void saveFarmName() {
		PacketDistributor.sendToServer(new FarmNameUpdateMessage(this.menu.x, this.menu.y, this.menu.z, this.farmName.getValue()));
	}

	private void updateWorkbenchControls() {
		if (this.farmName == null) {
			return;
		}
		ItemStack core = this.menu.getSlot(BlackboxBlockBlockEntity.CORE_SLOT).getItem();
		boolean hasCore = !isSpawnerBlackbox() && core.is(BlackboxModItems.DIMENSION_CORE.get());
		boolean archive = hasCore && FarmCoreData.isVillageArchiveCore(core);
		this.enterFarmButton.active = hasCore && !archive;
		this.saveFarmNameButton.active = hasCore && !archive;
		this.farmName.setEditable(hasCore && !archive);
		this.farmAccessButton.active = hasCore && !archive;
		this.farmAccessButton.setMessage(Component.translatable("gui.blackbox.access." + (hasCore ? FarmCoreData.getAccessMode(core) : FarmAccessMode.PRIVATE).id()));
		if (hasCore && !this.farmNameInitialized) {
			this.farmName.setValue(archive ? core.getHoverName().getString() : FarmCoreData.getFarmName(core));
			this.farmNameInitialized = true;
		} else if (!hasCore) {
			this.farmNameInitialized = false;
			this.farmName.setValue("");
		}
	}

	private boolean measurementActive() {
		return this.menu.getCalculationPhase() > 0;
	}

	private int measurementPageCount() {
		WorkbenchOutputData outputData = currentProfile();
		int itemPages = (outputData.items().size() + 9) / 10;
		int fluidPages = outputData.fluids().size();
		return Math.max(1, Math.max(itemPages, fluidPages));
	}

	private WorkbenchOutputData currentProfile() {
		FarmMeasurementHost host = measurementHost();
		if (host == null) {
			return WorkbenchOutputData.EMPTY;
		}
		if (measurementActive()) {
			List<WorkbenchItemOutput> items = host.measurementProgress().items().stream().map(entry -> new WorkbenchItemOutput(entry.stack(), entry.amount(), false, 1)).toList();
			return new WorkbenchOutputData(items, host.measurementProgress().fluids(), host.measurementProgress().energy(),
					Math.max(1, this.menu.getCalculationTotalTicks()), false);
		}
		ItemStack core = this.menu.getSlot(BlackboxBlockBlockEntity.CORE_SLOT).getItem();
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get()) || !FarmCoreData.isProgrammed(core) || this.menu.world == null) {
			return WorkbenchOutputData.EMPTY;
		}
		FarmCoreData.Recipe recipe = FarmCoreData.read(core, this.menu.world.registryAccess());
		List<WorkbenchItemOutput> items = new java.util.ArrayList<>();
		for (FarmCoreData.StackAmount output : recipe.outputs()) {
			items.add(new WorkbenchItemOutput(output.stack(), output.amount(), false, 1));
		}
		for (FarmCoreData.DurabilityAmount output : recipe.durabilityOutputs()) {
			items.add(new WorkbenchItemOutput(output.stack(), output.durability(), true, Math.max(1, output.stack().getMaxDamage())));
		}
		return new WorkbenchOutputData(List.copyOf(items), recipe.fluidOutputs(), recipe.energyOutput(), Math.max(1, recipe.sampleTicks()), true);
	}

	private boolean isSpawnerBlackbox() {
		return this.menu.isSpawnerBlackbox() && spawnerBlackbox() != null;
	}

	private SpawnerBlackboxBlockEntity spawnerBlackbox() {
		return this.menu.world.getBlockEntity(new BlockPos(this.menu.x, this.menu.y, this.menu.z)) instanceof SpawnerBlackboxBlockEntity blackbox ? blackbox : null;
	}

	private StatusLine statusLine() {
		ItemStack core = this.menu.getSlot(BlackboxBlockBlockEntity.CORE_SLOT).getItem();
		if (isSpawnerBlackbox()) {
			if (spawnerBlackbox() == null || spawnerBlackbox().spawnerCount() == 0) {
				return new StatusLine(Component.translatable("gui.blackbox.spawner_blackbox.missing_spawner"), 0xFFFFB85C);
			}
			return new StatusLine(Component.translatable("gui.blackbox.spawner_blackbox.running"), 0xFF86E7A0);
		}
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get())) {
			return new StatusLine(Component.translatable("gui.blackbox.machine.missing_core"), 0xFFFF7373);
		}
		if (!FarmCoreData.isProgrammed(core)) {
			return new StatusLine(Component.translatable("gui.blackbox.machine.empty_core"), 0xFFFFB85C);
		}
		if (this.menu.getSlot(BlackboxBlockBlockEntity.UPGRADE_SLOT).getItem().is(BlackboxModItems.STABILITY_UPGRADE.get())) {
			return new StatusLine(Component.translatable("gui.blackbox.machine.stable"), 0xFF73C7FF);
		}
		return new StatusLine(Component.translatable("gui.blackbox.machine.timeline"), 0xFF6EDC8C);
	}

	private static Component sideTooltip(Direction side, BlackboxSideMode mode) {
		return Component.translatable("gui.blackbox.side." + side.getSerializedName()).append(Component.literal(": "))
				.append(Component.translatable("gui.blackbox.routing." + mode.id()));
	}

	private static String modeButtonLabel(BlackboxSideMode mode) {
		return switch (mode) {
			case ITEM_INPUT -> "Item+";
			case ITEM_OUTPUT -> "Item-";
			case ITEM_BOTH -> "Item*";
			case FLUID_INPUT -> "Fluid+";
			case FLUID_OUTPUT -> "Fluid-";
			case FLUID_BOTH -> "Fluid*";
			case ENERGY_INPUT -> "FE+";
			case ENERGY_OUTPUT -> "FE-";
			case ENERGY_BOTH -> "FE*";
			case DISABLED -> "None";
		};
	}

	private static int modeColor(BlackboxSideMode mode) {
		return switch (mode) {
			case ITEM_INPUT, ITEM_OUTPUT, ITEM_BOTH -> 0xFFD5B85A;
			case FLUID_INPUT, FLUID_OUTPUT, FLUID_BOTH -> 0xFF73C7FF;
			case ENERGY_INPUT, ENERGY_OUTPUT, ENERGY_BOTH -> 0xFFCC83FF;
			case DISABLED -> 0xFF8A95A2;
		};
	}

	private int sidePanelX() {
		return this.leftPos + sidePanelRelativeX();
	}

	private int filterPanelX() {
		return this.leftPos + filterPanelRelativeX();
	}

	private static int sidePanelRelativeX() {
		return -SIDE_PANEL_WIDTH - 6;
	}

	private int filterPanelRelativeX() {
		return MAIN_PANEL_WIDTH + 2;
	}

	private void repositionGuiForDrawers() {
		this.imageWidth = totalGuiWidth();
		this.leftPos = (this.width - this.imageWidth) / 2 + ((this.leftPanelOpen || this.workbenchPanelOpen) ? SIDE_PANEL_WIDTH + 6 : 0);
		this.topPos = (this.height - this.imageHeight) / 2;
	}

	private int totalGuiWidth() {
		return MAIN_PANEL_WIDTH + ((this.leftPanelOpen || this.workbenchPanelOpen) ? SIDE_PANEL_WIDTH + 6 : 0) + (this.rightPanelOpen ? FILTER_PANEL_WIDTH + 2 : 0);
	}

	private int sideButtonX(int index) {
		return sidePanelX() + 32 + index % 2 * 80;
	}

	private int sideLabelRelativeX(int index) {
		return sidePanelRelativeX() + 4 + index % 2 * 80;
	}

	private int sideButtonY(int index) {
		return this.topPos + 15 + index / 2 * 28;
	}

	private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static String formatEnergy(int energy) {
		if (energy >= 1_000_000) {
			return String.format(Locale.ROOT, "%.1fM", energy / 1_000_000.0D);
		}
		if (energy >= 1_000) {
			return String.format(Locale.ROOT, "%.1fk", energy / 1_000.0D);
		}
		return Integer.toString(energy);
	}

	private static String formatRate(long amount, int sampleTicks) {
		return formatRateValue(amount * 1_200.0D / Math.max(1, sampleTicks));
	}

	private static String formatRateValue(double perMinute) {
		return String.format(Locale.ROOT, perMinute >= 100.0D ? "%.0f" : "%.2f", perMinute);
	}

	private static String formatAmount(long amount) {
		if (amount >= 1_000_000) {
			return String.format(Locale.ROOT, "%.2fM", amount / 1_000_000.0D);
		}
		if (amount >= 1_000) {
			return String.format(Locale.ROOT, "%.1fk", amount / 1_000.0D);
		}
		return Long.toString(amount);
	}

	private static String formatDuration(int seconds) {
		int safeSeconds = Math.max(0, seconds);
		return safeSeconds / 60 + ":" + (safeSeconds % 60 < 10 ? "0" : "") + safeSeconds % 60;
	}

	private static String formatChance(long occurrences, long totalRolls) {
		if (totalRolls <= 0) {
			return "0.00%";
		}
		return String.format(Locale.ROOT, "%.2f%%", occurrences * 100.0D / totalRolls);
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}

	private record StatusLine(Component text, int color) {
	}

	private String formatItemRate(WorkbenchItemOutput output, int sampleTicks) {
		double perMinute = output.amount() * 1200.0D / Math.max(1, sampleTicks);
		if (output.durabilityOutput()) {
			perMinute /= Math.max(1, output.maxDamage());
		}
		return formatRateValue(perMinute);
	}

	private record WorkbenchItemOutput(ItemStack stack, long amount, boolean durabilityOutput, int maxDamage) {
		private WorkbenchItemOutput {
			stack = stack.copyWithCount(1);
			amount = Math.max(0, amount);
		}
	}

	private record WorkbenchOutputData(List<WorkbenchItemOutput> items, List<FarmCoreData.FluidAmount> fluids, long energy, int sampleTicks, boolean profile) {
		private static final WorkbenchOutputData EMPTY = new WorkbenchOutputData(List.of(), List.of(), 0, 1, false);
	}

	private record FilterItemOutput(ItemStack stack, String filterGroup, long amount, long occurrences, long totalRolls, boolean observed) {
	}
}
