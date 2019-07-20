resource "google_logging_project_sink" "unified-logging" {
    name = "unified-logging-pubsub"
    destination = "pubsub.googleapis.com/projects/${var.project}/topics/${google_pubsub_topic.unified-logging.name}"
    filter = ""
    unique_writer_identity = true
}