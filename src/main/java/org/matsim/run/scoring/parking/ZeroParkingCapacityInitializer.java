package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.matsim.run.scoring.parking.ParkingObserver.LINK_OFF_STREET_SPOTS;
import static org.matsim.run.scoring.parking.ParkingObserver.LINK_ON_STREET_SPOTS;

public class ZeroParkingCapacityInitializer implements ParkingCapacityInitializer {
	private Network network;

	@Inject
	ZeroParkingCapacityInitializer(Network network) {
		this.network = network;
	}

	@Override
	public Map<Id<Link>, InitialParkingCapacity> initialize() {
		Map<Id<Link>, InitialParkingCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS)).orElse(0);
			res.put(link.getId(), new InitialParkingCapacity(onStreet + offStreet, 0));
		}
		return res;
	}
}
