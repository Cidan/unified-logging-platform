resource "google_bigquery_dataset" "unified-logging" {
  dataset_id = "unified_logging"
  location = "US"
  delete_contents_on_destroy = true
}

resource "google_bigquery_table" "logs" {
  dataset_id = "${google_bigquery_dataset.unified-logging.dataset_id}"
  table_id   = "logs"

  time_partitioning {
    type = "DAY"
  }

  schema = "${file("./schemas/logs.schema.json")}"
}