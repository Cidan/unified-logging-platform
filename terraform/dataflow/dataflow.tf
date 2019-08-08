resource "google_dataflow_job" "unified-logging" {
    name = "unified-logging"
    template_gcs_path = "gs://${var.bucket}/dataflow-template/unified-logging"
    temp_gcs_location = "gs://${var.bucket}/unified-logging-tmp"
    zone = "us-central1-a"
    parameters = {
        //TODO - should come from a variable
        subscriptionName = "projects/${var.project}/subscriptions/unified-logging-sub"
        //kTODO - should come from a variable
        outputTable = "${var.project}:unified_logging.logs"
    }
}