package org.matsim.run.scoring.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface KernelFunction {
	Map<Id<Link>, Double> calculateKernel(Link link, double distance);
}
