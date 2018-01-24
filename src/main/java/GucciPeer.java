package com.austinjadams.circuitsim.gucci;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ra4king.circuitsim.gui.CircuitManager;
import com.ra4king.circuitsim.gui.ComponentManager.ComponentManagerInterface;
import com.ra4king.circuitsim.gui.ComponentPeer;
import com.ra4king.circuitsim.gui.Connection.PortConnection;
import com.ra4king.circuitsim.gui.GuiUtils;
import com.ra4king.circuitsim.gui.Properties;
import com.ra4king.circuitsim.gui.Properties.MemoryLine;
import com.ra4king.circuitsim.gui.Properties.Property;
import com.ra4king.circuitsim.gui.Properties.PropertyMemoryValidator;
import com.ra4king.circuitsim.simulator.CircuitState;
import com.ra4king.circuitsim.simulator.components.memory.ROM;

import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Pair;

/**
 * @author Roi Atalla
 */
public class GucciPeer extends ComponentPeer<GucciComponent> {
	public static void installComponent(ComponentManagerInterface manager) {
		manager.addComponent(new Pair<>("Misc", "Gucci"),
		                     new Image(GucciPeer.class.getResourceAsStream("/icecream.png")),
		                     new Properties());
	}

	private final Property<List<MemoryLine>> contentsProperty;

	public GucciPeer(Properties props, int x, int y) {
		super(x, y, 5, 4);

		Properties properties = new Properties();
		properties.ensureProperty(Properties.LABEL);
		properties.ensureProperty(Properties.LABEL_LOCATION);
		properties.ensureProperty(Properties.BITSIZE);
		properties.ensureProperty(Properties.ADDRESS_BITS);
		properties.mergeIfExists(props);

		int addressBits = properties.getValue(Properties.ADDRESS_BITS);
		int dataBits = properties.getValue(Properties.BITSIZE);

		contentsProperty = new Property<>("Contents", new PropertyMemoryValidator(addressBits, dataBits), null);
		String oldMemory;
		Property<?> oldContents = props.getProperty("Contents");
		if(oldContents == null) {
			oldMemory = "";
		} else if(oldContents.validator == null) {
			oldMemory = props.getValue("Contents");
		} else {
			oldMemory = oldContents.getStringValue();
		}
		properties.setValue(contentsProperty, contentsProperty.validator.parse(oldMemory));

		int[] memory = memoryToArray(properties.getValue(contentsProperty));
		GucciComponent ram = new GucciComponent(properties.getValue(Properties.LABEL), dataBits, addressBits, memory);

		List<PortConnection> connections = new ArrayList<>();
		connections.add(new PortConnection(this, ram.getPort(GucciComponent.PORT_ADDRESS), "Address", 0, 2));
		connections.add(new PortConnection(this, ram.getPort(GucciComponent.PORT_ENABLE), "Enable", 2, getHeight()));
		connections.add(new PortConnection(this, ram.getPort(GucciComponent.PORT_DATA), "Data", getWidth(), 2));

		init(ram, properties, connections);
	}

	private static int[] memoryToArray(List<MemoryLine> lines) {
		if(lines == null) {
			return new int[0];
		}

		return lines.stream()
		            .flatMap(line -> line.values.stream())
		            .mapToInt(prop -> Integer.parseUnsignedInt(prop.get(), 16))
		            .toArray();
	}

	@Override
	public List<MenuItem> getContextMenuItems(CircuitManager circuit) {
		MenuItem menuItem = new MenuItem("Edit contents");
		menuItem.setOnAction(event -> {
			Property<List<MemoryLine>> property = getProperties().getProperty(contentsProperty.name);
			PropertyMemoryValidator memoryValidator = (PropertyMemoryValidator)property.validator;

			int[] memory = getComponent().getMemory();
			List<MemoryLine> lines =
					memoryValidator.parse(memory, (address, value) -> {
						memory[address] = value;

						int addressBits = getComponent().getAddressBits();
						for(MemoryLine line : property.value) {
							if(address < line.address + 16) {
								int index = address - line.address;
								line.get(index).set(String.format("%0" + (1 + (addressBits - 1) / 4) + "x", value));
								break;
							}
						}

						for(CircuitState state : circuit.getCircuit().getCircuitStates()) {
							getComponent().valueChanged(state, null, 0);
						}

						circuit.getSimulatorWindow().runSim();
					});

			memoryValidator.createAndShowMemoryWindow(circuit.getSimulatorWindow().getStage(), lines);
		});
		return Collections.singletonList(menuItem);
	}

	@Override
	public void paint(GraphicsContext graphics, CircuitState circuitState) {
		GuiUtils.drawName(graphics, this, getProperties().getValue(Properties.LABEL_LOCATION));

		graphics.setFill(Color.WHITE);
		GuiUtils.drawShape(graphics::fillRect, this);

		graphics.setStroke(Color.BLACK);
		GuiUtils.drawShape(graphics::strokeRect, this);

		String text = "Gucci";
		Bounds bounds = GuiUtils.getBounds(graphics.getFont(), text);
		graphics.setFill(Color.BLACK);
		graphics.fillText(text,
		                    getScreenX() + (getScreenWidth() - bounds.getWidth()) * 0.5,
		                    getScreenY() + (getScreenHeight() + bounds.getHeight()) * 0.45);
	}
}
