package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.matsim.run.scoring.parking.ParkingObserver.LINK_OFF_STREET_SPOTS;
import static org.matsim.run.scoring.parking.ParkingObserver.LINK_ON_STREET_SPOTS;

public class PlanBasedParkingCapacityInitializer implements ParkingCapacityInitializer {
	private Network network;
	private Population population;

	@Inject
	PlanBasedParkingCapacityInitializer(Network network, Population population) {
		this.network = network;
		this.population = population;
	}

	@Override
	public Map<Id<Link>, InitialParkingCapacity> initialize() {
		Map<Id<Link>, Long> initialParkingByPlans = getInitialParkingByPlans();

		Map<Id<Link>, InitialParkingCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS)).orElse(0);

			int initialParking = initialParkingByPlans.getOrDefault(link.getId(), 0L).intValue();
			res.put(link.getId(), new InitialParkingCapacity(onStreet + offStreet, initialParking));
		}
		return res;
	}

	// Returns the number of parking spots on the link where the first car trip starts
	private Map<Id<Link>, Long> getInitialParkingByPlans() {
		return population.getPersons().values().stream().map(p -> p.getSelectedPlan())
			.map(p -> TripStructureUtils.findAccessWalksWithPreviousActivity(p, TransportMode.car).stream().findFirst())
			.filter(Optional::isPresent)
			.map(Optional::get)
			.map(lap -> lap.act.getLinkId())
			.collect(Collectors.groupingBy(l -> l, Collectors.counting()));
	}
}
