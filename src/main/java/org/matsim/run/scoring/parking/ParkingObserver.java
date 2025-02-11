package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.modechoice.estimators.ActivityEstimator;
import org.matsim.modechoice.estimators.LegEstimator;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class ParkingObserver implements AfterMobsimListener {
	static final String LINK_ON_STREET_SPOTS = "onstreet_spots";
	static final String LINK_OFF_STREET_SPOTS = "offstreet_spots";

	@Inject
	Network network;
	@Inject
	KernelFunction kernelFunction;
	@Inject
	PenaltyFunction penaltyFunction;
	@Inject
	EventsManager eventsManager;
	@Inject
	ActivityEstimator activityEstimator;
	@Inject
	LegEstimator legEstimator;

	@Inject
	ParkingCapacityInitializer parkingCapacityInitializer;
	@Inject
	ParkingEventsHandler parkingEventsHandler;

	//state
	Set<Id<Vehicle>> knownPtVehicles = new HashSet<>();
	Map<Id<Link>, Integer> indexByLinkId;
	int[] parkingCount;
	int[] capacity;

	//TODO
	double kernelDistance = 500;

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		parkingEventsHandler.lock();
		initializeParking();
		run();
	}

	private void run() {
		List<VehicleLeavesTrafficEvent> vehicleLeavesTrafficEvents = parkingEventsHandler.getVehicleLeavesTrafficEvents();
		List<VehicleEntersTrafficEvent> vehicleEntersTrafficEvents = parkingEventsHandler.getVehicleEntersTrafficEvents();

		double maxEntersTime = vehicleEntersTrafficEvents.stream().mapToDouble(VehicleEntersTrafficEvent::getTime).max().orElse(0.0);
		double maxLeavesTime = vehicleLeavesTrafficEvents.stream().mapToDouble(VehicleLeavesTrafficEvent::getTime).max().orElse(0.0);

		int indexEnterEvents = 0;
		int indexLeaveEvents = 0;

		for (int i = 0; i <= Math.max(maxLeavesTime, maxEntersTime); i++) {
			while (indexEnterEvents < vehicleEntersTrafficEvents.size() && vehicleEntersTrafficEvents.get(indexEnterEvents).getTime() == i) {
				handleEvent(vehicleEntersTrafficEvents.get(indexEnterEvents++));
			}

			while (indexLeaveEvents < vehicleLeavesTrafficEvents.size() && vehicleLeavesTrafficEvents.get(indexLeaveEvents).getTime() == i) {
				handleEvent(vehicleLeavesTrafficEvents.get(indexLeaveEvents++));
			}
		}

	}

	private void handleEvent(VehicleEntersTrafficEvent event) {
		if (knownPtVehicles.contains(event.getVehicleId())) {
			return;
		}
		unparkVehicle(event.getLinkId());
	}

	private void handleEvent(VehicleLeavesTrafficEvent event) {
		if (knownPtVehicles.contains(event.getVehicleId())) {
			return;
		}

		Map<Id<Link>, Double> weightedLinks = kernelFunction.calculateKernel(network.getLinks().get(event.getLinkId()), kernelDistance);
		Map<Id<Link>, ParkingCount> parkingCounts = applyWeights(weightedLinks);

		double penalty = penaltyFunction.calculatePenalty(parkingCounts);
		applyPenalty(event.getTime(), event.getPersonId(), penalty, event.getLinkId());

		parkVehicle(event.getLinkId());
	}

	private void parkVehicle(Id<Link> linkId) {
		parkingCount[indexByLinkId.get(linkId)]++;
	}

	private void unparkVehicle(Id<Link> linkId) {
		parkingCount[indexByLinkId.get(linkId)]--;
	}

	private Map<Id<Link>, ParkingCount> applyWeights(Map<Id<Link>, Double> weightedLinks) {
		Map<Id<Link>, ParkingCount> result = new HashMap<>();
		for (Map.Entry<Id<Link>, Double> entry : weightedLinks.entrySet()) {
			int index = indexByLinkId.get(entry.getKey());
			result.put(entry.getKey(), new ParkingCount(parkingCount[index], capacity[index], entry.getValue()));
		}
		return result;
	}

	private void applyPenalty(double time, Id<Person> personId, double penaltyInSec, Id<Link> linkId) {
		//TODO convert penalty to money

		double score = 0.0;

		PersonScoreEvent personScoreEvent = new PersonScoreEvent(time, personId, score, "parking");
		eventsManager.processEvent(personScoreEvent);
	}

	private void initializeParking() {
		int counter = 0;
		indexByLinkId = new HashMap<>(network.getLinks().size());

		for (Id<Link> id : network.getLinks().keySet()) {
			indexByLinkId.put(id, counter++);
		}

		int linkCount = network.getLinks().size();
		capacity = new int[linkCount];
		parkingCount = new int[linkCount];

		Map<Id<Link>, ParkingCapacityInitializer.ParkingInitialCapacity> initialize =
			parkingCapacityInitializer.initialize(parkingEventsHandler.getVehicleEntersTrafficEvents(), parkingEventsHandler.getVehicleLeavesTrafficEvents());

		for (Link link : network.getLinks().values()) {
			ParkingCapacityInitializer.ParkingInitialCapacity parkingInitialCapacity = initialize.get(link.getId());
			int index = indexByLinkId.get(link.getId());

			capacity[index] = parkingInitialCapacity.capacity();
			parkingCount[index] = parkingInitialCapacity.initial();
		}
	}
}
