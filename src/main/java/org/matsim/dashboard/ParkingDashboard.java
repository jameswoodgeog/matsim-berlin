package org.matsim.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.ScatterTrace;

public class ParkingDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Parking";
		header.description = "Parking analysis";

		layout.row("dist-dist").el(Plotly.class, (viz, data) -> {

			viz.title = "Parking search time distribution";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Time [s]").build())
				.yAxis(Axis.builder().title("Density").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(data.compute(ParkingAnalysis.class, "parking_search_times.csv"));

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE)
					.build(),
				ds.mapping()
					.x("search_time")
					.y("density")
			);

		});
	}
}
