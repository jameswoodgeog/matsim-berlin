package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParkingEventsHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	private List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents = new ArrayList<>();
	private List<VehicleLeavesTrafficEvent> vehicleLeavesTrafficEvents = new ArrayList<>();

	private boolean locked = false;
	private Set<Id<Vehicle>> knownPtVehicles = new HashSet<>();

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		checkLocked();
		knownPtVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		checkLocked();
		if (knownPtVehicles.contains(event.getVehicleId())) {
			return;
		}

		if (!event.getNetworkMode().equals(TransportMode.car)) {
			return;
		}

		vehicleEntersTrafficEvents.add(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		checkLocked();
		if (knownPtVehicles.contains(event.getVehicleId())) {
			return;
		}

		if (!event.getNetworkMode().equals(TransportMode.car)) {
			return;
		}

		vehicleLeavesTrafficEvents.add(event);
	}

	public List<VehicleEntersTrafficEvent> getVehicleEntersTrafficEvents() {
		return vehicleEntersTrafficEvents;
	}

	public List<VehicleLeavesTrafficEvent> getVehicleLeavesTrafficEvents() {
		return vehicleLeavesTrafficEvents;
	}

	public void lock() {
		locked = true;
	}

	private void checkLocked() {
		if (locked) {
			throw new IllegalStateException("This handler is locked. It is expected that it is only locked after processing all events.");
		}
	}
}
