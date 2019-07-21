resource "google_bigquery_dataset" "unified-logging" {
  dataset_id = "unified_logging"
  location = "US"
}