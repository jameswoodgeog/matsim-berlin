package org.matsim.run;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
Simple class to run the open berlin scenario with reduced free speed.
 */
public class RunOpenBerlinSpeedReduction extends OpenBerlinScenario {

	private static final Logger log = LoggerFactory.getLogger(RunOpenBerlinSpeedReduction.class);

	@CommandLine.Option(names = "--slowSpeed-area", description = "Path to SHP file specifying slow speed area", required = true, defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/shp/Berlin_25832.shp")
	private Path slowSpeedArea;

	@CommandLine.Option(names = "--speed reduction", description = "Define how much car speed is reduced", required = true, defaultValue = "0.6")
	private double speedReduction;

	public static void main(String[] args) {
		MATSimApplication.run(RunOpenBerlinSpeedReduction.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);
		prepareSlowSpeed(scenario.getNetwork(),
			ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(new ShpOptions(slowSpeedArea, null, null).getShapeFile().toString())),
			speedReduction);
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
	}

	private static void prepareSlowSpeed(Network network, List<PreparedGeometry> geometries, Double relativeSpeedChange) {
		Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
			//filter car links
			.filter(link -> link.getAllowedModes().contains(TransportMode.car))
			//spatial filter
			.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries))
			//we won't change motorways and motorway_links
			.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))
			.filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("trunk"))
			.collect(Collectors.toSet());

		if (relativeSpeedChange >= 0.0 && relativeSpeedChange < 1.0) {
			log.info("reduce speed relatively by a factor of: {}", relativeSpeedChange);
			//apply  all roads but motorways
			carLinksInArea.forEach(link -> link.setFreespeed(link.getFreespeed() * relativeSpeedChange));

		} else {
			log.info("reduce speed to 20 km/h");
			carLinksInArea.forEach(link ->
				//apply  to all roads but motorways
				//20 km/h --> 5.5 m/s
				link.setFreespeed(5.5)
			);
		}
	}
}
