package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.matsim.run.scoring.parking.ParkingTimeEstimator.LINK_OFF_STREET_SPOTS;
import static org.matsim.run.scoring.parking.ParkingTimeEstimator.LINK_ON_STREET_SPOTS;

public class ZeroParkingCapacityInitializer implements ParkingCapacityInitializer {
	private Network network;

	@Inject
	ZeroParkingCapacityInitializer(Network network) {
		this.network = network;
	}

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize(List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents, List<VehicleLeavesTrafficEvent> vehicleLeavesTrafficEvents) {
		Map<Id<Link>, ParkingInitialCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS)).orElse(0);
			res.put(link.getId(), new ParkingInitialCapacity(onStreet + offStreet, 0));
		}
		return res;
	}
}
