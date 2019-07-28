resource "google_bigquery_dataset" "unified-logging" {
  dataset_id = "unified_logging"
  location = "US"
  delete_contents_on_destroy = true
}

resource "google_bigquery_table" "okta" {
  dataset_id = "${google_bigquery_dataset.unified-logging.dataset_id}"
  table_id   = "okta"

  time_partitioning {
    type = "DAY"
  }

  schema = "${file("./schemas/okta.schema.json")}"
}