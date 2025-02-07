package org.matsim.run.scoring.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParkingObserver implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	static final String LINK_ON_STREET_SPOTS = "onstreet_spots";
	static final String LINK_OFF_STREET_SPOTS = "offstreet_spots";

	//injected
	Network network;
	KernelFunction kernelFunction;
	PenaltyFunction penaltyFunction;
	EventsManager eventsManager;


	//state
	Set<Id<Vehicle>> knownPtVehicles = new HashSet<>();
	Map<Id<Link>, Integer> indexByLinkId;
	int[] parkingCount;
	int[] capacity;

	//TODO
	double kernelDistance = 0;

	@Inject
	public ParkingObserver(Network network, KernelFunction kernelFunction, PenaltyFunction penaltyFunction, EventsManager eventsManager) {
		this.indexByLinkId = new HashMap<>(network.getLinks().size());
		this.network = network;
		this.kernelFunction = kernelFunction;
		this.penaltyFunction = penaltyFunction;
		this.eventsManager = eventsManager;
		initCapacity(network);
	}

	private void initCapacity(Network network) {
		int counter = 0;
		for (Id<Link> id : network.getLinks().keySet()) {
			indexByLinkId.put(id, counter++);
		}

		int linkCount = network.getLinks().size();
		capacity = new int[linkCount];
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS);
			int offStreet = (int) link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS);
			capacity[indexByLinkId.get(link.getId())] = onStreet + offStreet;
		}
		parkingCount = new int[linkCount];
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		knownPtVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (knownPtVehicles.contains(event.getVehicleId())) {
			return;
		}

		unparkVehicle(event.getLinkId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
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
}
