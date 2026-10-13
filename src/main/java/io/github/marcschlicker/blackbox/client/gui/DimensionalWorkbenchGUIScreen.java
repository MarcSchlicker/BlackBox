package io.github.marcschlicker.blackbox.client.gui;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.fluids.FluidStack;

import io.github.marcschlicker.blackbox.block.entity.DimensionalWorkbenchBlockEntity;
import io.github.marcschlicker.blackbox.init.BlackboxModItems;
import io.github.marcschlicker.blackbox.init.BlackboxModScreens;
import io.github.marcschlicker.blackbox.network.DimensionalWorkbenchGUIButtonMessage;
import io.github.marcschlicker.blackbox.network.FarmNameUpdateMessage;
import io.github.marcschlicker.blackbox.util.FarmCoreData;
import io.github.marcschlicker.blackbox.util.FarmAccessMode;
import io.github.marcschlicker.blackbox.world.inventory.DimensionalWorkbenchGUIMenu;

import java.util.List;
import java.util.Locale;

public class DimensionalWorkbenchGUIScreen extends AbstractContainerScreen<DimensionalWorkbenchGUIMenu> implements BlackboxModScreens.ScreenAccessor {
	private static final int MAIN_PANEL_WIDTH = 216;
	private static final int MEASUREMENT_PANEL_WIDTH = 164;
	private static final int DETAILS_PANEL_WIDTH = 200;
	private static final int MEASUREMENT_ITEM_PAGE_SIZE = 10;
	private static final int MEASUREMENT_FLUID_PAGE_SIZE = 2;
	private Button enterButton;
	private Button saveNameButton;
	private Button accessButton;
	private Button measurementToggleButton;
	private Button detailsToggleButton;
	private EditBox farmName;
	private boolean nameInitialized;
	private boolean measurementExpanded;
	private boolean detailsExpanded;
	private int measurementScroll;
	private boolean wasMeasuring;

	public DimensionalWorkbenchGUIScreen(DimensionalWorkbenchGUIMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = MAIN_PANEL_WIDTH;
		this.imageHeight = 271;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
	}

	@Override
	protected void init() {
		this.imageWidth = totalGuiWidth();
		super.init();
		repositionGuiForDrawers();
		this.farmName = new EditBox(this.font, this.leftPos + 30, this.topPos + 21, 95, 20, Component.translatable("gui.blackbox.workbench.name"));
		this.farmName.setMaxLength(32);
		this.farmName.setHint(Component.translatable("gui.blackbox.workbench.name"));
		this.addRenderableWidget(this.farmName);
		this.saveNameButton = this.addRenderableWidget(Button.builder(Component.literal("\u2713"), button -> saveName()).bounds(this.leftPos + 129, this.topPos + 21, 25, 20).build());
		this.saveNameButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.save_name")));
		this.enterButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.blackbox.workbench.enter_short"), button -> {
			saveName();
			PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(0, this.menu.x, this.menu.y, this.menu.z));
		}).bounds(this.leftPos + 158, this.topPos + 21, 30, 20).build());
		this.enterButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.start")));
		this.accessButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			PacketDistributor.sendToServer(new DimensionalWorkbenchGUIButtonMessage(1, this.menu.x, this.menu.y, this.menu.z));
		}).bounds(this.leftPos + 8, this.topPos + 47, 58, 18).build());
		this.accessButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.access_tooltip")));
		this.measurementToggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			this.measurementExpanded = !this.measurementExpanded;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
		}).bounds(this.leftPos + 2, this.topPos + 2, 16, 16).build());
		this.measurementToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.workbench.measurement_toggle")));
		this.detailsToggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			this.detailsExpanded = !this.detailsExpanded;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
		}).bounds(this.leftPos + 198, this.topPos + 2, 16, 16).build());
		this.detailsToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.blackbox.resources.toggle")));
		layoutDrawerWidgets();
	}

	private void saveName() {
		PacketDistributor.sendToServer(new FarmNameUpdateMessage(this.menu.x, this.menu.y, this.menu.z, this.farmName.getValue()));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.farmName != null && this.farmName.isFocused()) {
			this.farmName.keyPressed(keyCode, scanCode, modifiers);
			// Do not forward focused text input to the container screen: its inventory
			// shortcut would otherwise close the workbench for keys such as I or E.
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		boolean measuring = measurementActive();
		if (measuring && !this.wasMeasuring) {
			this.measurementScroll = 0;
		}
		this.wasMeasuring = measuring;
		this.measurementToggleButton.active = measuring;
		if (!measuring && this.measurementExpanded) {
			this.measurementExpanded = false;
			repositionGuiForDrawers();
			layoutDrawerWidgets();
		}
		this.measurementScroll = Math.clamp(this.measurementScroll, 0, Math.max(0, measurementPageCount() - 1));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.measurementExpanded && measurementActive()
				&& inside((int) mouseX, (int) mouseY, measurementPanelX(), this.topPos, MEASUREMENT_PANEL_WIDTH, 194)
				&& measurementPageCount() > 1 && scrollY != 0) {
			int direction = scrollY > 0 ? -1 : 1;
			this.measurementScroll = Math.clamp(this.measurementScroll + direction, 0, measurementPageCount() - 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		ItemStack core = this.menu.getSlot(0).getItem();
		boolean hasCore = core.is(BlackboxModItems.DIMENSION_CORE.get());
		boolean archiveCore = hasCore && FarmCoreData.isVillageArchiveCore(core);
		this.enterButton.active = hasCore && !archiveCore;
		this.saveNameButton.active = hasCore && !archiveCore;
		this.farmName.setEditable(hasCore && !archiveCore);
		this.accessButton.active = hasCore && !archiveCore;
		FarmAccessMode access = hasCore ? FarmCoreData.getAccessMode(core) : FarmAccessMode.PRIVATE;
		this.accessButton.setMessage(Component.translatable("gui.blackbox.access." + access.id()));
		if (hasCore && !this.nameInitialized) {
			this.farmName.setValue(archiveCore ? core.getHoverName().getString() : FarmCoreData.getFarmName(core));
			this.nameInitialized = true;
		} else if (!hasCore) {
			this.nameInitialized = false;
			this.farmName.setValue("");
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		renderDrawerToggleIcons(graphics);
		renderSideResourceStatus(graphics, mouseX, mouseY);
		if (this.detailsExpanded) {
			renderResourceStatus(graphics, mouseX, mouseY);
		}
		if (this.measurementExpanded && measurementActive()) {
			renderMeasurementProgressPanel(graphics, mouseX, mouseY);
			renderMeasurementProgressTooltip(graphics, mouseX, mouseY);
		}
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	private void renderDrawerToggleIcons(GuiGraphics graphics) {
		graphics.renderItem(new ItemStack(Items.CLOCK), this.leftPos + 2, this.topPos + 2);
		graphics.renderItem(new ItemStack(Items.HOPPER), this.leftPos + 198, this.topPos + 2);
	}

	private void renderSideResourceStatus(GuiGraphics graphics, int mouseX, int mouseY) {
		DimensionalWorkbenchBlockEntity workbench = workbench();
		if (workbench == null) {
			return;
		}
		int x = this.leftPos + 199;
		renderVerticalResourcePair(graphics, mouseX, mouseY, x, this.topPos + 20, 120,
				workbench.resources().inputFluids().getFluidInTank(0).getAmount(), workbench.resources().inputFluids().getTankCapacity(0), 0xFF5E8FB8,
				compactFluidTooltip(workbench.resources().inputFluids().getFluidInTank(0), workbench.resources().inputFluids().getTankCapacity(0), true),
				workbench.resources().outputFluids().getFluidInTank(0).getAmount(), workbench.resources().outputFluids().getTankCapacity(0), 0xFF77A96E,
				compactFluidTooltip(workbench.resources().outputFluids().getFluidInTank(0), workbench.resources().outputFluids().getTankCapacity(0), false));
		renderVerticalResourcePair(graphics, mouseX, mouseY, x, this.topPos + 148, 115,
				workbench.resources().inputEnergy().getEnergyStored(), workbench.resources().inputEnergy().getMaxEnergyStored(), 0xFFCC83FF,
				compactEnergyTooltip(workbench.resources().inputEnergy().getEnergyStored(), workbench.resources().inputEnergy().getMaxEnergyStored(), true),
				workbench.resources().outputEnergy().getEnergyStored(), workbench.resources().outputEnergy().getMaxEnergyStored(), 0xFFE2BE62,
				compactEnergyTooltip(workbench.resources().outputEnergy().getEnergyStored(), workbench.resources().outputEnergy().getMaxEnergyStored(), false));
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
		Component label = Component.translatable(input ? "gui.blackbox.workbench.energy_input_short" : "gui.blackbox.workbench.energy_output_short");
		return label.copy().append(Component.literal(": " + energy + " / " + capacity + " FE"));
	}

	private void layoutDrawerWidgets() {
		if (this.farmName == null) {
			return;
		}
		this.farmName.setPosition(this.leftPos + 30, this.topPos + 21);
		this.saveNameButton.setPosition(this.leftPos + 129, this.topPos + 21);
		this.enterButton.setPosition(this.leftPos + 158, this.topPos + 21);
		this.accessButton.setPosition(this.leftPos + 8, this.topPos + 47);
		this.measurementToggleButton.setPosition(this.leftPos + 2, this.topPos + 2);
		this.detailsToggleButton.setPosition(this.leftPos + 198, this.topPos + 2);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		drawPanel(graphics);
		drawSlot(graphics, this.leftPos + 8, this.topPos + 24, 0xFF77CC88);
		drawSlot(graphics, this.leftPos + 170, this.topPos + 51, 0xFFD5B85A);
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 75, 0xFF5E8FB8);
		}
		for (int row = 0; row < 2; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 103 + row * 18, 0xFF77A96E);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 189 + row * 18, 0xFF68727D);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawSlot(graphics, this.leftPos + 8 + column * 18, this.topPos + 247, 0xFF68727D);
		}
		if (this.detailsExpanded) {
			drawDetailsPanel(graphics);
			drawLivePanel(graphics);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.title"), 8, 6, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.upgrade_short"), 169, 42, 0xFFD8C37A, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.inputs"), 70, 64, 0xFF83BCE6, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.machine.outputs"), 8, 93, 0xFF8DDB9B, false);
		graphics.drawString(this.font, Component.translatable("container.inventory"), 8, 178, 0xFFAAB4C0, false);

		StatusLine status = statusLine();
		graphics.drawString(this.font, status.text(), 70, 51, status.color(), false);
		ItemStack core = this.menu.getSlot(0).getItem();
		if (core.is(BlackboxModItems.DIMENSION_CORE.get())) {
			int size = FarmCoreData.getCellSizeChunks(core);
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.cell_size", size, size), 130, 7, 0xFFA8B2BD, false);
		}
	}

	private void drawPanel(GuiGraphics graphics) {
		graphics.fill(this.leftPos, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF171B21);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + 2, 0xFF4C9B70);
		graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(this.leftPos + MAIN_PANEL_WIDTH - 1, this.topPos, this.leftPos + MAIN_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void drawDetailsPanel(GuiGraphics graphics) {
		int x = detailsPanelX();
		graphics.fill(x, this.topPos, x + DETAILS_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF121820);
		graphics.fill(x, this.topPos, x + DETAILS_PANEL_WIDTH, this.topPos + 2, 0xFF4C9B70);
		graphics.fill(x, this.topPos, x + 1, this.topPos + this.imageHeight, 0xFF596572);
		graphics.fill(x + DETAILS_PANEL_WIDTH - 1, this.topPos, x + DETAILS_PANEL_WIDTH, this.topPos + this.imageHeight, 0xFF080A0D);
	}

	private void drawLivePanel(GuiGraphics graphics) {
		int x = detailsPanelX() + 4;
		int width = 192;
		int phase = this.menu.getCalculationPhase();
		int y = this.topPos + 34;
		int progress = this.menu.getCalculationProgressPercent();
		String remaining = formatDuration(this.menu.getCalculationSecondsRemaining());

		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.live_status"), x, this.topPos + 7, 0xFFE8EDF2, false);
		StatusLine status = statusLine();
		graphics.drawString(this.font, status.text(), x, this.topPos + 20, status.color(), false);
		graphics.fill(x, y, x + width, y + 31, 0xFF10151B);
		if (phase <= 0) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.no_measurement"), x + 5, y + 11, 0xFF9AA4B0, false);
		} else {
			graphics.fill(x, y, x + width, y + 1, phase == 2 ? 0xFF73C7FF : 0xFFFFC866);
			if (phase == 1) {
				graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.calculation_preparing", remaining), x + 5, y + 5, 0xFFFFC866, false);
				graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.calculation_waiting"), x + 5, y + 16, 0xFFB8C2CC, false);
			} else {
				graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.calculation_running", remaining, progress), x + 5, y + 5, 0xFF73C7FF, false);
				Component rate = this.menu.getCalculationItemsPerMinute() > 0
						? Component.translatable("gui.blackbox.workbench.calculation_rate", this.menu.getCalculationItemsPerMinute())
						: Component.translatable("gui.blackbox.workbench.calculation_rate_waiting");
				graphics.drawString(this.font, rate, x + 5, y + 16, 0xFF8DDB9B, false);
			}
			graphics.fill(x + 5, y + 26, x + width - 5, y + 28, 0xFF303A45);
			graphics.fill(x + 5, y + 26, x + 5 + (width - 10) * progress / 100, y + 28, phase == 2 ? 0xFF4B98D0 : 0xFFC08C35);
		}
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.fluid_buffer"), x, this.topPos + 75, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.energy"), x, this.topPos + 177, 0xFFE8EDF2, false);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.item_buffers", itemCount(1, 10), itemCount(10, 28)),
				x, this.topPos + 252, 0xFFD5B85A, false);
	}

	private void renderResourceStatus(GuiGraphics graphics, int mouseX, int mouseY) {
		DimensionalWorkbenchBlockEntity workbench = workbench();
		if (workbench == null) {
			return;
		}
		int x = detailsPanelX() + 4;
		renderFluidTank(graphics, mouseX, mouseY, x, this.topPos + 87, 84, 80,
				workbench.resources().inputFluids().getFluidInTank(0), workbench.resources().inputFluids().getTankCapacity(0), true);
		renderFluidTank(graphics, mouseX, mouseY, x + 108, this.topPos + 87, 84, 80,
				workbench.resources().outputFluids().getFluidInTank(0), workbench.resources().outputFluids().getTankCapacity(0), false);
		renderEnergyTank(graphics, mouseX, mouseY, x, this.topPos + 188, 84, 56,
				workbench.resources().inputEnergy().getEnergyStored(), workbench.resources().inputEnergy().getMaxEnergyStored(), true);
		renderEnergyTank(graphics, mouseX, mouseY, x + 108, this.topPos + 188, 84, 56,
				workbench.resources().outputEnergy().getEnergyStored(), workbench.resources().outputEnergy().getMaxEnergyStored(), false);
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
		graphics.drawCenteredString(this.font, label, x + width / 2, y + 5, color);
		String name = fluid.isEmpty() ? Component.translatable("gui.blackbox.fluids.empty").getString() : this.font.plainSubstrByWidth(fluid.getHoverName().getString(), width - 8);
		graphics.drawCenteredString(this.font, name, x + width / 2, y + 27, fluid.isEmpty() ? 0xFF9AA4B0 : 0xFFE8EDF2);
		graphics.drawCenteredString(this.font, fluid.getAmount() + " mB", x + width / 2, y + 40, fluid.isEmpty() ? 0xFF718090 : color);
		graphics.drawCenteredString(this.font, "/ " + capacity + " mB", x + width / 2, y + 52, fluid.isEmpty() ? 0xFF718090 : color);
		if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
			Component tooltip = fluid.isEmpty()
					? label.copy().append(Component.literal(": ")).append(Component.translatable("gui.blackbox.fluids.empty"))
					: Component.literal(fluid.getHoverName().getString() + ": " + fluid.getAmount() + " / " + capacity + " mB");
			graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
		}
	}

	private void renderEnergyTank(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, int energy, int capacity, boolean input) {
		int color = input ? 0xFFCC83FF : 0xFFE2BE62;
		graphics.fill(x, y, x + width, y + height, 0xFF596572);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0C0F13);
		if (energy > 0) {
			int filled = Math.max(1, Math.round((height - 2) * energy / (float) Math.max(1, capacity)));
			graphics.fill(x + 1, y + height - 1 - filled, x + width - 1, y + height - 1, (color & 0x00FFFFFF) | 0x88000000);
		}
		Component label = Component.translatable(input ? "gui.blackbox.workbench.energy_input_short" : "gui.blackbox.workbench.energy_output_short");
		graphics.drawCenteredString(this.font, label, x + width / 2, y + 5, color);
		graphics.drawCenteredString(this.font, formatEnergy(energy), x + width / 2, y + 25, energy == 0 ? 0xFF718090 : color);
		graphics.drawCenteredString(this.font, "/ " + formatEnergy(capacity), x + width / 2, y + 37, energy == 0 ? 0xFF718090 : color);
		if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
			graphics.renderTooltip(this.font, label.copy().append(Component.literal(": " + energy + " / " + capacity + " FE")), mouseX, mouseY);
		}
	}

	private boolean measurementActive() {
		return this.menu.getCalculationPhase() > 0;
	}

	private void renderMeasurementProgressPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = measurementPanelX();
		int y = this.topPos;
		int width = MEASUREMENT_PANEL_WIDTH;
		int height = 194;
		graphics.fill(x, y, x + width, y + height, 0xFF171B21);
		graphics.fill(x, y, x + width, y + 2, this.menu.getCalculationPhase() == 2 ? 0xFF73C7FF : 0xFFFFC866);
		graphics.fill(x, y, x + 1, y + height, 0xFF596572);
		graphics.fill(x + width - 1, y, x + width, y + height, 0xFF080A0D);
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.measurement_progress"), x + 8, y + 8, 0xFFE8EDF2, false);

		DimensionalWorkbenchBlockEntity workbench = workbench();
		List<FarmCoreData.StackAmount> items = workbench == null ? List.of() : workbench.measurementProgress().items();
		List<FarmCoreData.FluidAmount> fluids = workbench == null ? List.of() : workbench.measurementProgress().fluids();
		long energy = workbench == null ? 0 : workbench.measurementProgress().energy();
		if (this.menu.getCalculationPhase() == 1) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.measurement_waiting"), x + 8, y + 24, 0xFFFFC866, false);
		}
		graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.produced_so_far"), x + 8, y + 42, 0xFFAAB4C0, false);
		int pageCount = measurementPageCount(items, fluids);
		if (pageCount > 1) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.measurement_page", this.measurementScroll + 1, pageCount), x + 109, y + 42, 0xFF9AA4B0, false);
			renderMeasurementScrollbar(graphics, x, y, pageCount);
		}
		if (items.isEmpty() && fluids.isEmpty() && energy <= 0) {
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.no_output_yet"), x + 8, y + 62, 0xFF9AA4B0, false);
		} else {
			int itemStart = this.measurementScroll * MEASUREMENT_ITEM_PAGE_SIZE;
			for (int index = 0; index < MEASUREMENT_ITEM_PAGE_SIZE && itemStart + index < items.size(); index++) {
				FarmCoreData.StackAmount entry = items.get(itemStart + index);
				int itemX = x + 8 + index % 5 * 30;
				int itemY = y + 58 + index / 5 * 30;
				graphics.renderItem(entry.stack(), itemX, itemY);
				graphics.renderItemDecorations(this.font, entry.stack(), itemX, itemY, formatAmount(entry.amount()));
			}
			int resourceY = y + 126;
			graphics.drawString(this.font, Component.translatable("gui.blackbox.fluids.title"), x + 8, resourceY, 0xFF73C7FF, false);
			int fluidStart = this.measurementScroll * MEASUREMENT_FLUID_PAGE_SIZE;
			for (int index = 0; index < MEASUREMENT_FLUID_PAGE_SIZE && fluidStart + index < fluids.size(); index++) {
				FarmCoreData.FluidAmount entry = fluids.get(fluidStart + index);
				String name = this.font.plainSubstrByWidth(entry.stack().getHoverName().getString(), 86);
				graphics.drawString(this.font, name + " " + formatAmount(entry.amount()) + " mB", x + 8, resourceY + 12 + index * 10, 0xFF8DDB9B, false);
			}
			graphics.drawString(this.font, Component.translatable("gui.blackbox.workbench.produced_energy", formatAmount(energy)), x + 8, y + 165, 0xFFCC83FF, false);
		}
	}

	private void renderMeasurementScrollbar(GuiGraphics graphics, int x, int y, int pageCount) {
		int trackX = x + 151;
		int trackY = y + 57;
		int trackHeight = 91;
		int thumbHeight = Math.max(12, trackHeight / pageCount);
		int thumbY = trackY + (trackHeight - thumbHeight) * this.measurementScroll / Math.max(1, pageCount - 1);
		graphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0xFF0C0F13);
		graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF73C7FF);
	}

	private void renderMeasurementProgressTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		DimensionalWorkbenchBlockEntity workbench = workbench();
		if (workbench == null) {
			return;
		}
		int panelX = measurementPanelX();
		int panelY = this.topPos;
		List<FarmCoreData.StackAmount> items = workbench.measurementProgress().items();
		int itemStart = this.measurementScroll * MEASUREMENT_ITEM_PAGE_SIZE;
		for (int index = 0; index < MEASUREMENT_ITEM_PAGE_SIZE && itemStart + index < items.size(); index++) {
			int itemX = panelX + 8 + index % 5 * 30;
			int itemY = panelY + 58 + index / 5 * 30;
			if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
				graphics.renderTooltip(this.font, items.get(itemStart + index).stack(), mouseX, mouseY);
				return;
			}
		}
	}

	private StatusLine statusLine() {
		ItemStack core = this.menu.getSlot(0).getItem();
		if (!core.is(BlackboxModItems.DIMENSION_CORE.get())) {
			return new StatusLine(Component.translatable("gui.blackbox.workbench.missing_core"), 0xFFFF7373);
		}
		if (FarmCoreData.isVillageArchiveCore(core)) {
			return new StatusLine(Component.translatable("gui.blackbox.workbench.archive"), 0xFFC59AEF);
		}
		if (this.menu.getCalculationPhase() == 1) {
			return new StatusLine(Component.translatable("gui.blackbox.workbench.analysing"), 0xFFFFC866);
		}
		if (this.menu.getCalculationPhase() == 2) {
			return new StatusLine(Component.translatable("gui.blackbox.workbench.measuring", this.menu.getCalculationSecondsRemaining()), 0xFF73C7FF);
		}
		if (FarmCoreData.isProgrammed(core)) {
			return this.menu.getSlot(28).getItem().is(BlackboxModItems.STABILITY_UPGRADE.get())
					? new StatusLine(Component.translatable("gui.blackbox.machine.stable"), 0xFF73C7FF)
					: new StatusLine(Component.translatable("gui.blackbox.machine.timeline"), 0xFF6EDC8C);
		}
		return new StatusLine(Component.translatable("gui.blackbox.workbench.ready"), 0xFFB8C2CC);
	}

	private DimensionalWorkbenchBlockEntity workbench() {
		return this.menu.world.getBlockEntity(new BlockPos(this.menu.x, this.menu.y, this.menu.z)) instanceof DimensionalWorkbenchBlockEntity workbench ? workbench : null;
	}

	private int measurementPanelX() {
		return this.leftPos - MEASUREMENT_PANEL_WIDTH - 6;
	}

	private int detailsPanelX() {
		return this.leftPos + MAIN_PANEL_WIDTH + 2;
	}

	private void repositionGuiForDrawers() {
		this.imageWidth = totalGuiWidth();
		this.leftPos = (this.width - this.imageWidth) / 2 + (this.measurementExpanded ? MEASUREMENT_PANEL_WIDTH + 6 : 0);
		this.topPos = (this.height - this.imageHeight) / 2;
	}

	private int totalGuiWidth() {
		return MAIN_PANEL_WIDTH + (this.measurementExpanded ? MEASUREMENT_PANEL_WIDTH + 6 : 0) + (this.detailsExpanded ? DETAILS_PANEL_WIDTH + 2 : 0);
	}

	private int measurementPageCount() {
		DimensionalWorkbenchBlockEntity workbench = workbench();
		return workbench == null ? 1 : measurementPageCount(workbench.measurementProgress().items(), workbench.measurementProgress().fluids());
	}

	private static int measurementPageCount(List<FarmCoreData.StackAmount> items, List<FarmCoreData.FluidAmount> fluids) {
		int itemPages = (items.size() + MEASUREMENT_ITEM_PAGE_SIZE - 1) / MEASUREMENT_ITEM_PAGE_SIZE;
		int fluidPages = (fluids.size() + MEASUREMENT_FLUID_PAGE_SIZE - 1) / MEASUREMENT_FLUID_PAGE_SIZE;
		return Math.max(1, Math.max(itemPages, fluidPages));
	}

	private int itemCount(int start, int end) {
		int total = 0;
		for (int slot = start; slot < end; slot++) {
			total += this.menu.getSlot(slot).getItem().getCount();
		}
		return total;
	}

	private static String formatEnergy(int energy) {
		return energy >= 1_000_000 ? String.format(Locale.ROOT, "%.2fM FE", energy / 1_000_000.0D) : energy + " FE";
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

	private record StatusLine(Component text, int color) {
	}

	private static String formatDuration(int seconds) {
		int safeSeconds = Math.max(0, seconds);
		return safeSeconds / 60 + ":" + (safeSeconds % 60 < 10 ? "0" : "") + safeSeconds % 60;
	}

	private void drawSlot(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
		graphics.fill(x, y, x + 16, y + 16, 0xFF0C0F13);
	}

	private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
