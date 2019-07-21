resource "google_dataflow_job" "unified-logging" {
    name = "unified-logging"
    template_gcs_path = "gs://${var.bucket}/dataflow-template/unified-logging"
    temp_gcs_location = "gs://${var.bucket}/unified-logging-tmp"
    zone = "us-central1-a"
}