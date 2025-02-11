package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface ParkingCapacityInitializer {
	Map<Id<Link>, InitialParkingCapacity> initialize();

	record InitialParkingCapacity(int capacity, int initial) {
	}
}
