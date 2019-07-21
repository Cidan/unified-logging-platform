resource "google_bigquery_dataset" "unified-logging" {
  dataset_id = "unified_logging"
  location = "US"
  delete_contents_on_destroy = true
}