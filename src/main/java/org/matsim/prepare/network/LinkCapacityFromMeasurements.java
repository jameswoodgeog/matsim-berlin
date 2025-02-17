package org.matsim.prepare.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

@CommandLine.Command(
	name = "link-capacity-from-measurements",
	description = "Set the capacity of links according to measurements provided via counts file."
)
public class LinkCapacityFromMeasurements implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(LinkCapacityFromMeasurements.class);

	@CommandLine.Option(names = {"--network"}, description = "Path to the network file", required = true)
	private String networkPath;

	@CommandLine.Option(names = {"--counts"}, description = "Path to the counts file in hourly resolution", required = true)
	private String countsPath;

	@CommandLine.Option(names = "--output", description = "Path to the output network file", required = true)
	private Path output;

	@CommandLine.Option(names = "--factor", description = "Factor for the maximum hourly capacity", defaultValue = "1.3")
	private double factor;

	@CommandLine.Option(names = "--min-capacity", description = "Minimum capacity", defaultValue = "600")
	private double minCapacity;

	@CommandLine.Option(names = "--pce", description = "Map containing vehicle types to passenger car equivalents", split = ";", defaultValue = "car=1;truck=3.5")
	private Map<String, Double> pce;

	public static void main(String[] args) {
		new LinkCapacityFromMeasurements().execute(args);
	}

	/**
	 * Calculate the maximum hourly volume for a given measurement location and mapping of pce for each mode.
	 */
	public static double calcMaxHourlyVolume(MeasurementLocation<Link> ms, Map<String, Double> pce) {

		double[] hourlyVolumes = new double[24];

		for (Map.Entry<String, Double> mp : pce.entrySet()) {
			Measurable values = ms.getVolumesForMode(mp.getKey());

			if (values == null)
				continue;

			for (int i = 0; i < 24; i++) {
				hourlyVolumes[i] += values.aggregateAtHour(i).orElse(0) * mp.getValue();
			}
		}

		return Arrays.stream(hourlyVolumes).max().orElse(Double.NaN);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkPath);

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(countsPath);

		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> kv : counts.getMeasureLocations().entrySet()) {

			MeasurementLocation<Link> ms = kv.getValue();

			double hourlyVolume = calcMaxHourlyVolume(ms, pce);

			Link link = network.getLinks().get(kv.getKey());
			if (link == null) {
				log.warn("Link {} not found in network", kv.getKey());
				continue;
			}

			double capacity = link.getCapacity();

			if (link.getCapacity() > hourlyVolume * factor) {
				double target = Math.max(minCapacity, hourlyVolume * factor);
				log.info("Decreasing capacity of link {} from {} to {}", link.getId(), capacity, target);
				link.setCapacity(target);
			} else if (link.getCapacity() < hourlyVolume) {
				log.info("Increasing capacity of link {} from {} to {}", link.getId(), capacity, hourlyVolume);
				link.setCapacity(hourlyVolume);
			} else
				log.info("Link {} unchanged with cap: {}, vol: {}", link.getId(), capacity, hourlyVolume);
		}

		NetworkUtils.writeNetwork(network, output.toString());


		return 0;
	}

}
