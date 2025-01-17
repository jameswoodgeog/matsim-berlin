/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityFromEvents;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.dashboard.*;
import org.matsim.vehicles.MatsimVehicleWriter;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "simwrapper",
	description = "Run additional analysis and create SimWrapper dashboard for existing run output."
)
public final class BerlinSimWrapperRunner implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(BerlinSimWrapperRunner.class);

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which dashboards are to be generated.")
	private List<Path> inputPaths;

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@CommandLine.Option(names = "--noise", defaultValue = "false", description = "create noise dashboard")
	private boolean noise;
	@CommandLine.Option(names = "--trips", defaultValue = "false", description = "create trips dashboard")
	private boolean trips;
	@CommandLine.Option(names = "--emissions", defaultValue = "false", description = "create emission dashboard")
	private boolean emissions;

	@CommandLine.Option(names = "--accessibility", defaultValue = "false", description = "create accessibility dashboard")
	private boolean accessibility;

	@CommandLine.Option(names = "--population", defaultValue = "false", description = "create population dashboard")
	private boolean population;


	public BerlinSimWrapperRunner(){
//		public constructor needed for testing purposes.
	}

	@Override
	public Integer call() throws Exception {

		if (!noise && !trips && !emissions && !accessibility){
			throw new IllegalArgumentException("you have not configured any dashboard to be created! Please use command line parameters!");
		}

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			//both noise and accessibility analyses require area.shp
			if(!runDirectory.resolve("area/area.shp").toFile().exists()) {

				FileUtils.copyDirectory(new File("input/v6.3/area"),runDirectory.resolve("area").toFile());
			}

			String configPath = ApplicationUtils.matchInput("config.xml", runDirectory).toString();
			Config config = ConfigUtils.loadConfig(configPath);
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
			if (shp.isDefined()){
				simwrapperCfg.defaultParams().shp = shp.getShapeFile();
			}
			//skip default dashboards
			simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.disabled;

			//add dashboards according to command line parameters
//			if more dashboards are to be added here, we need to check if noise==true before adding noise dashboard here
			if (accessibility){


				Set<String> activityOptions = new HashSet<>();

				{
					//CONFIG
					// set the input files for acc analysis as output files from simulation:
					Config configForAcc = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", runDirectory).toAbsolutePath().toString());
					configForAcc.controller().setOutputDirectory(runDirectory.toString());
					configForAcc.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
					configForAcc.network().setInputFile(ApplicationUtils.matchInput("output_network.xml.gz", runDirectory).toAbsolutePath().toString());
					configForAcc.transit().setTransitScheduleFile(ApplicationUtils.matchInput("output_transitSchedule.xml.gz", runDirectory).toAbsolutePath().toString());
					configForAcc.transit().setVehiclesFile(null);
					configForAcc.vehicles().setVehiclesFile(ApplicationUtils.matchInput("output_vehicles.xml.gz", runDirectory).toAbsolutePath().toString());
					configForAcc.plans().setInputFile(ApplicationUtils.matchInput("output_plans.xml.gz", runDirectory).toAbsolutePath().toString());


					// misc config settings (are these neccessary?)
					configForAcc.eventsManager().setNumberOfThreads(null);
					configForAcc.eventsManager().setEstimatedNumberOfEvents(null);
					configForAcc.global().setNumberOfThreads(1);
					configForAcc.routing().setRoutingRandomness(0);

					// set facilities mgmt (exact facilities are set later)
					configForAcc.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.setInScenario);
//					configForAcc.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.fromFile);
//					configForAcc.facilities().setInputFile(ApplicationUtils.matchInput("output_facilities.xml.gz", runDirectory).toAbsolutePath().toString());

					// following would be neccessary if we examine drt

//					for (DrtConfigGroup drtConfig : ConfigUtils.addOrGetModule(configForAcc, MultiModeDrtConfigGroup.class).getModalElements()) {
//						drtConfig.transitStopFile = "/Users/jakob/git/matsim-libs/examples/scenarios/kelheim/drt-stops.xml"; // todo!!!
//						drtConfig.removeParameterSet(drtConfig.getZonalSystemParams().get());
//						drtConfig.plotDetailedCustomerStats = false;
//					}
//					ConfigUtils.addOrGetModule(configForAcc, DvrpConfigGroup.class);

					//accessibility config:
					double mapCenterX = 798766.73;
					double mapCenterY = 5828215.73;

					double tileSize = 500;
					double num_rows = 20;

					final AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(configForAcc, AccessibilityConfigGroup.class);
					acg.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox);
//					acg.setShapeFileCellBasedAccessibility(runDirectory.resolve("area/area.shp").toString());
//
					acg.setBoundingBoxLeft(mapCenterX - num_rows * tileSize - tileSize / 2);
					acg.setBoundingBoxRight(mapCenterX + num_rows * tileSize + tileSize / 2);
					acg.setBoundingBoxBottom(mapCenterY - num_rows * tileSize - tileSize / 2);
					acg.setBoundingBoxTop(mapCenterY + num_rows * tileSize + tileSize / 2);
					acg.setTileSize_m((int) tileSize);

					acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
					acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
					acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
					acg.setComputingAccessibilityForMode(Modes4Accessibility.estimatedDrt, false);
					acg.setUseParallelization(false);

					//SCENARIO
					Scenario scenario = ScenarioUtils.loadScenario(configForAcc);

					//add facilities
					ActivityFacilitiesFactory af = scenario.getActivityFacilities().getFactory();
					// hbf
					double westkreuzX = 790721.334967369;
					double westkreuzY = 5825371.781091197;
					ActivityFacility fac1 = af.createActivityFacility(Id.create("xxx", ActivityFacility.class), new Coord(westkreuzX, westkreuzY));
					ActivityOption ao = af.createActivityOption("westkreuz");
					fac1.addActivityOption(ao);
					scenario.getActivityFacilities().addActivityFacility(fac1);
					activityOptions.add("westkreuz");

					// ostkreuz
					double ostkreuzX = 803276.0103003506;
					double ostkreuzY = 5826382.082920058;
					ActivityFacility fac2 = af.createActivityFacility(Id.create("yyy", ActivityFacility.class), new Coord(ostkreuzX, ostkreuzY));
					ActivityOption ao2 = af.createActivityOption("ostkreuz");
					fac2.addActivityOption(ao2);
					scenario.getActivityFacilities().addActivityFacility(fac2);
					activityOptions.add("ostkreuz");

					String eventsFile = ApplicationUtils.matchInput("output_events.xml.gz", runDirectory).toString();

//					activityOptions = scenario.getActivityFacilities().getFacilities().values().stream().flatMap(fac -> fac.getActivityOptions().values().stream()).map(ActivityOption::getType).collect(Collectors.toSet());
					AccessibilityFromEvents.Builder builder = new AccessibilityFromEvents.Builder(scenario, eventsFile, new ArrayList<>(activityOptions));

					builder.build().run();
				}

				sw.addDashboard(new AccessibilityDashboard(config.global().getCoordinateSystem(), new ArrayList<>(activityOptions)));

			}

			if (population){
				sw.addDashboard(new PopulationAttributeDashboard());
			}

			if (noise) {

				sw.addDashboard(Dashboard.customize(new NoiseDashboard(config.global().getCoordinateSystem())).context("noise"));
			}



			try {
				sw.generate(runDirectory, true);
				sw.run(runDirectory);
			} catch (IOException e) {
				throw new InterruptedIOException();
			}
		}

		return 0;
	}

	public static void main(String[] args) {
		new BerlinSimWrapperRunner().execute(args);

	}

}
