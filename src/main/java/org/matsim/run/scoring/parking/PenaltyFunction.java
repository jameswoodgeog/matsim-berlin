package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface PenaltyFunction {
	double calculatePenalty(Map<Id<Link>, ParkingCount> parkingCount);
}
