package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;

import java.util.List;
import java.util.Map;

public interface ParkingCapacityInitializer {
	Map<Id<Link>, ParkingInitialCapacity> initialize(List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents, List<VehicleLeavesTrafficEvent> vehicleLeavesTrafficEvents);

	record ParkingInitialCapacity(int capacity, int initial) {
	}
}
