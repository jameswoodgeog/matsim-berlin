package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.run.scoring.parking.ParkingObserver.LINK_OFF_STREET_SPOTS;
import static org.matsim.run.scoring.parking.ParkingObserver.LINK_ON_STREET_SPOTS;

public class EventBasedParkingCapacityInitializer implements ParkingCapacityInitializer {
	@Inject
	private Network network;

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize(List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents, List<VehicleLeavesTrafficEvent> vehicleLeavesTrafficEvents) {
		Map<Id<Link>, Long> initialParkingByPlans = getInitialParkingByPlans(vehicleEntersTrafficEvents);

		Map<Id<Link>, ParkingInitialCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS)).orElse(0);

			int initialParking = initialParkingByPlans.getOrDefault(link.getId(), 0L).intValue();
			res.put(link.getId(), new ParkingInitialCapacity(onStreet + offStreet, initialParking));
		}
		return res;
	}

	// Returns the number of parking spots on the link where the first car trip starts
	private Map<Id<Link>, Long> getInitialParkingByPlans(List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents) {


		Set<Id<Person>> visitedPerson = new HashSet<>();
		return vehicleEntersTrafficEvents.stream()
			.filter(e -> visitedPerson.add(e.getPersonId()))
			.collect(Collectors.groupingBy(VehicleEntersTrafficEvent::getLinkId, Collectors.counting()));
	}
}
