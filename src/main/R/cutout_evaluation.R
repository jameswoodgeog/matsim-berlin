  # Preparation script for first time users
{
  install.packages("devtools")
  devtools::install_github("matsim-vsp/matsim-r", build_vignettes = TRUE)
  library("matsim")
  library("tidyverse")
  library("sf")
}

compare_across_methods <- function(...) {
  trips_list <- list(...)
  method_names <- names(trips_list)

  if (is.null(method_names) || any(method_names == "")) {
    method_names <- paste0("method_", seq_along(trips_list))
  }

  trips_all <- bind_rows(
    lapply(seq_along(trips_list), function(i) {
      df <- trips_list[[i]]
      df$method_name <- method_names[i]
      return(df)
    })
  )

  trips_shares <- trips_all %>%
    group_by(method_name) %>%
    mutate(method_count = n()) %>%
    group_by(method_name, main_mode) %>%
    summarize(share = n()/first(method_count))

  print(trips_shares)

  ggplot(trips_shares, aes(x = share, y = method_name, fill = main_mode)) +
    geom_bar(stat = "identity", position = "stack") +  # horizontal stacked
    #scale_x_continuous(labels = percent_format())
    labs(
      title = "Verteilung der Verkehrsmittel nach Methode",
      x = "Anteil",
      y = "",
      fill = "Verkehrsmittel"
    ) +
    theme_minimal() +
    theme(text=element_text(size=18))
}

# Modal Share
{
  # Using a HPC-Cluster mount drive for the paths here
  # trips_gp_ref <- read_output_trips(input_path = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gartenfeld/output/gartenfeld-v6.4-10pct/gartenfeld-v6.4-10pct.output_trips.csv.gz") %>%
  #   filter(str_detect(person, "dng"))
  trips_gp_reference <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-reference/berlin-v6.4.output_trips.csv.gz") %>%
    filter(startsWith(person, "dng"))

  trips_cut_gp_keepCapacities <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-keepCapacities/berlin-v6.4.output_trips.csv.gz") %>%
    filter(startsWith(person, "dng"))
  trips_cut_gp_subtractLostVehiclesCapacities <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-subtractLostVehiclesCapacities/berlin-v6.4.output_trips.csv.gz") %>%
    filter(startsWith(person, "dng"))
  #trips_cut_gp_relativeAdjustmentOfCapacities <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-relativeAdjustmentOfCapacities/berlin-v6.4.output_trips.csv.gz") %>%
  #  filter(startsWith(person, "dng"))
  trips_cut_gp_proportionalFreespeedsCleaning <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-proportionalFreespeedsCleaning/berlin-v6.4.output_trips.csv.gz") %>%
    filter(startsWith(person, "dng"))
  trips_cut_gp_modeledFreespeedsCleaning <- read_output_trips(input_path = "/Users/aleksander/Cluster/scenario-cutout-improvement/simulation-runs/output/gp-modeledFreespeedsCleaning/berlin-v6.4.output_trips.csv.gz") %>%
    filter(startsWith(person, "dng"))

  compare_across_methoads(a_gp_reference=trips_gp_reference,
                         b_gp_keepCapacities=trips_cut_gp_keepCapacities,
                         c_gp_subtractLostVehiclesCapacities=trips_cut_gp_subtractLostVehiclesCapacities,
                         d_gp_proportionalFreespeedsCleaning=trips_cut_gp_proportionalFreespeedsCleaning,
                         e_gp_modeledFreespeedsCleaning=trips_cut_gp_modeledFreespeedsCleaning)
}