package org.matsim.run.gartenfeld;

import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.PersonNetworkLinkCheck;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.Activities;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.scoring.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.AdvancedScoringModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.run.OpenBerlinScenario.*;

/**
 * Scenario class for Gartenfeld.
 * <p>
 * This scenario has its own files, which extend the OpenBerlin scenario files with inhabitants and road infrastructure specific to Gartenfeld.
 * See {@link org.matsim.prepare.gartenfeld.CreateGartenfeldComplete} for the creation of these files.
 */
public class GartenfeldScenario extends MATSimApplication {

	@CommandLine.Option(names = "--gartenfeld-config", description = "Path to configuration for Gartenfeld.", defaultValue = "input/gartenfeld/gartenfeld.config.xml")
	private String gartenFeldConfig;

	@CommandLine.Option(names = "--gartenfeld-shp", description = "Path to configuration for Gartenfeld.", defaultValue = "input/gartenfeld/DNG_area.gpkg")
	private String gartenFeldArea;

	@CommandLine.Option(names = "--parking-garages", description = "Enable parking garages.", defaultValue = "NO_GARAGE")
	private GarageType garageType = GarageType.NO_GARAGE;

	@CommandLine.Option(names = "--plan-selector",
			description = "Plan selector to use.",
			defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
	private String planSelector;

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);

	public static void main(String[] args) {
		MATSimApplication.run(GartenfeldScenario.class, args);
	}

	protected Config prepareConfig(Config config) {

		// Load the Gartenfeld specific part into the standard Berlin config
		ConfigUtils.loadConfig(config, gartenFeldConfig);

		// needs to be after load.config

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		if (sample.isSet()) {
			double sampleSize = sample.getSample();

			config.qsim().setFlowCapFactor(sampleSize);
			config.qsim().setStorageCapFactor(sampleSize);

			// Counts can be scaled with sample size
			config.counts().setCountsScaleFactor(sampleSize);
			sw.setSampleSize(sampleSize);

			config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
			config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
		}

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);

		// overwrite ride scoring params with values derived from car
		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);
		Activities.addScoringParams(config, true);

		// Required for all calibration strategies
		for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(planSelector)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.15)
					.setSubpopulation(subpopulation)
			);
		}

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		// Need to switch to warning for best score
		if (planSelector.equals(DefaultPlanStrategiesModule.DefaultSelector.BestScore)) {
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		}

		// Bicycle config must be present
		ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

		// Add emissions configuration
		EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
		eConfig.setDetailedColdEmissionFactorsFile(OpenBerlinScenario.HBEFA_FILE_COLD_DETAILED);
		eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
		eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction);

		return config;
	}

	protected void prepareScenario(Scenario scenario) {

		// add hbefa link attributes.
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());

		Network network = scenario.getNetwork();
		Set<String> removeModes = Set.of(TransportMode.car, TransportMode.truck, "freight", TransportMode.ride);

		if (garageType != GarageType.NO_GARAGE) {

			for (Link link : network.getLinks().values()) {

				// Make all links car free
				String linkId = link.getId().toString();

				if (linkId.startsWith("network-DNG")) {

					// First garage, a garage in any cases
					if (linkId.equals("network-DNG.1") || linkId.equals("network-DNG.1_r"))
						continue;

					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					allowedModes.removeAll(removeModes);
					link.setAllowedModes(allowedModes);
				}
			}

			MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
			removeModes.forEach(m -> cleaner.run(Set.of(m)));

			// Clean link ids that are not valid anymore
			ParallelPersonAlgorithmUtils.run(
				scenario.getPopulation(),
				Runtime.getRuntime().availableProcessors(),
				PersonNetworkLinkCheck.createPersonAlgorithm(network)
			);

		}
	}

	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());

		controler.addOverridingModule(new TravelTimeBinding());

		controler.addOverridingModule(new QsimTimingModule());

		// AdvancedScoring is specific to matsim-berlin!
		if (ConfigUtils.hasModule(controler.getConfig(), AdvancedScoringConfigGroup.class)) {
			controler.addOverridingModule(new AdvancedScoringModule());
			controler.getConfig().scoring().setExplainScores(true);
		} else {
			// if the above config group is not present we still need income dependent scoring
			// this implementation also allows for person specific asc
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
				}
			});
		}
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		// Only with the car free area, the multimodal link chooser is needed
		if (garageType == GarageType.ONE_LINK)
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind( org.matsim.core.router.MultimodalLinkChooserDefaultImpl.class );
					bind(MultimodalLinkChooser.class).toInstance(new GartenfeldLinkChooser(ShpOptions.ofLayer(gartenFeldArea, null)));
				}
			});
	}

	/**
	 * Enum for the different garage types and implicitly the car free areas.
	 */
	public enum GarageType {
		/**
		 * No garage, cars are allowed on all links.
		 */
		NO_GARAGE,
		/**
		 * One garage, cars are only allowed on the garage link at the entrance of the area.
		 */
		ONE_LINK
	}
}
