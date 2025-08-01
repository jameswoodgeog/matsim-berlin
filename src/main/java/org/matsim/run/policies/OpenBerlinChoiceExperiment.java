package org.matsim.run.policies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
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
import org.matsim.core.replanning.choosers.BalancedInnovationStrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.pruning.PlanScoreThresholdPruner;
import org.matsim.run.Activities;
import org.matsim.run.OpenBerlinScenario;
import org.matsim.run.scoring.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.AdvancedScoringModule;
import org.matsim.run.scoring.PseudoRandomTripScoreEstimator;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used to run some synthetic mode choice experiments on the OpenBerlin scenario.
 */
public class OpenBerlinChoiceExperiment extends OpenBerlinScenario {

	private static final Logger log = LogManager.getLogger(OpenBerlinChoiceExperiment.class);

	@CommandLine.Option(names = "--bike-speed-offset", description = "Offset the default bike speed in km/h", defaultValue = "0")
	private double bikeSpeedOffset;

	@CommandLine.Option(names = "--imc", description = "Enable informed-mode-choice functionality")
	private boolean imc;

	@CommandLine.Option(names = "--pruning", description = "Plan pruning threshold. If 0 pruning is disabled", defaultValue = "0")
	private double pruning;

	@CommandLine.Option(names = "--top-k", description = "Top k value to use with IMC", defaultValue = "25")
	private int topK;

	@CommandLine.Option(names = "--balanced-innovation", description = "Use balanced innovation selection", defaultValue = "false")
	private boolean bi;

	@CommandLine.Option(names = "--no-act-est", description = "Include estimation of activity utilities into score", defaultValue = "true",
			negatable = true)
	private boolean actEst;

	@CommandLine.Option(names = "--inv-beta", description = "Beta inv value for selection", defaultValue = "-1")
	private double invBeta;

	@CommandLine.Option(names = "--strategy", description = "Mode choice strategy to use (imc needs to be enabled)",
		defaultValue = InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY)
	private String strategy;

	@CommandLine.Option(names = "--plan-selector",
			description = "Plan selector to use.",
			defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
	private String planSelector;

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);


	public static void main(String[] args) {
		MATSimApplication.execute(OpenBerlinChoiceExperiment.class, args);
	}

	/**
	 * Remove strategy from all subpopulations.
	 */
	private static void removeStrategy(Config config, String strategy) {

		List<ReplanningConfigGroup.StrategySettings> strategies = new ArrayList<>(config.replanning().getStrategySettings());
		strategies.removeIf(s -> s.getStrategyName().equals(strategy));

		config.replanning().clearStrategySettings();

		strategies.forEach(config.replanning()::addStrategySettings);
	}

	protected Config prepareConfig(Config config) {

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
        eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
        eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
        eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
        eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
        eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
        eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
        eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction);

        config = config;

		if (imc) {

			InformedModeChoiceConfigGroup imcConfig = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

			imcConfig.setTopK(topK);
			imcConfig.setModes(List.of(config.subtourModeChoice().getModes()));
			imcConfig.setConstraintCheck(InformedModeChoiceConfigGroup.ConstraintCheck.repair);

			if (invBeta >= 0)
				imcConfig.setInvBeta(invBeta);

			InformedModeChoiceModule.replaceReplanningStrategy(config, "person",
				DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice,
				strategy
			);

			// All imc strategies are run with best score selector
			InformedModeChoiceModule.replaceReplanningStrategy(config,
				DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta,
				DefaultPlanStrategiesModule.DefaultSelector.BestScore
			);

			// Best score requires disabling consistency checking
			config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

			// Experiments are without time mutation
			removeStrategy(config, DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator);

			if (pruning > 0)
				imcConfig.setPruning("p" + pruning);

		} else if (pruning > 0) {
			throw new IllegalArgumentException("Pruning is only available with informed-mode-choice enabled");
		}

		return config;
	}

	protected void prepareScenario(Scenario scenario) {

		// add hbefa link attributes.
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());

		// If bike speed is adjusted, we need to remove all bike routes and travel times
		// These times will be recalculated by the router
		if (bikeSpeedOffset != 0) {

			log.info("Adjusting bike speed by {} km/h", bikeSpeedOffset);

			VehicleType bike = scenario.getVehicles().getVehicleTypes().get(Id.create(TransportMode.bike, VehicleType.class));
			bike.setMaximumVelocity(bike.getMaximumVelocity() + bikeSpeedOffset / 3.6);

			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (Leg leg : TripStructureUtils.getLegs(plan)) {
						if (leg.getMode().equals(TransportMode.bike)) {
							leg.setRoute(null);
							leg.setTravelTimeUndefined();
						}
					}
				}
			}
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

		if (bi) {
			log.info("Using balanced innovation strategy chooser");
			BalancedInnovationStrategyChooser.install(controler);
		}

		if (imc) {

			InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car", "pt")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
				// Modes with fixed costs need to be considered separately
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "pt")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "walk", "bike", "ride")
				.withPruner("p" + pruning, new PlanScoreThresholdPruner(pruning))
				.withConstraint(RelaxedMassConservationConstraint.class);

			if (actEst) {
				log.info("Including activity estimation into score");
				builder.withActivityEstimator(DefaultActivityEstimator.class);
			} else {
				log.info("No activity estimation for score");
			}

			if (ConfigUtils.hasModule(controler.getConfig(), AdvancedScoringConfigGroup.class)) {
				log.info("Using pseudo-random trip score estimator");
				builder.withTripScoreEstimator(PseudoRandomTripScoreEstimator.class);
			}

			controler.addOverridingModule(builder.build());
		}

	}
}
