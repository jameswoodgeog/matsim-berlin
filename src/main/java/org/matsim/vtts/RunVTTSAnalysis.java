package org.matsim.vtts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

public class RunVTTSAnalysis implements MATSimAppCommand {

	@CommandLine.Option(names = "--events", description = "Path to events file", required = true)
	private String eventsPath;

	@CommandLine.Option(names = "--config", description = "Path to config file", required = true)
	private String configPath;

	@CommandLine.Option(names = "--plans", description = "Path to config file", required = true)
	private String plansPath;

	@CommandLine.Option(names = "--network", description = "Path to config file", required = true)
	private String networkPath;



	public static void main(String[] args) {
		new RunVTTSAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		System.out.println("VTTS analysis started");


		Config config = ConfigUtils.loadConfig(configPath);
		config.vehicles().setVehiclesFile("/Users/gregorr/Documents/work/respos/git/matsim-berlin/input/v6.2/berlin-v6.2-vehicleTypes.xml");
		config.plans().setInputFile(plansPath);
		config.network().setInputFile(networkPath);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		String[] ignoredModes = {"freight"};
		String[] ignoredActivities = {"interaction"};

		VTTSHandler vttsHandler = new VTTSHandler(scenario, ignoredModes, "interaction", null);

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(vttsHandler);
		EventsUtils.readEvents(manager, eventsPath);
		vttsHandler.printCarVTTS("/Users/gregorr/Documents/work/respos/git/matsim-berlin/output/VTTS/carVTTS.csv");
		vttsHandler.printVTTS("/Users/gregorr/Documents/work/respos/git/matsim-berlin/output/VTTS/allVTTS.csv");

		return 0;
	}
}
