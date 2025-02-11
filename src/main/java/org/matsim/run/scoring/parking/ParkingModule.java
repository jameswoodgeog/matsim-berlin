package org.matsim.run.scoring.parking;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.modechoice.estimators.ActivityEstimator;
import org.matsim.modechoice.estimators.DefaultActivityEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.LegEstimator;

public class ParkingModule extends AbstractModule {
	@Override
	public void install() {
		// bind classes from informed mode choice explicitly
		bind(ActivityEstimator.class).to(DefaultActivityEstimator.class);
		bind(LegEstimator.class).to(DefaultLegScoreEstimator.class);

		// bind parking classes
		bind(KernelFunction.class).to(ConstantKernelFunction.class);
		bind(PenaltyFunction.class).toInstance(new BellochePenaltyFunction(0.4, -6));
		bind(ParkingCapacityInitializer.class).to(ZeroParkingCapacityInitializer.class);

		bind(ParkingObserver.class).in(Singleton.class);
		bind(ParkingEventsHandler.class).in(Singleton.class);
		addEventHandlerBinding().to(ParkingEventsHandler.class);
		addControlerListenerBinding().to(ParkingObserver.class);
	}
}
