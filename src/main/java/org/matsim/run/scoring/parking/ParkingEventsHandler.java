package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;

public class ParkingEventsHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
	

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {

	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {

	}
}
