resource "google_logging_project_sink" "unified-logging" {
    name = "unified-logging-pubsub"
    destination = "pubsub.googleapis.com/projects/${var.project}/topics/${google_pubsub_topic.unified-logging.name}"
    filter = ""
    unique_writer_identity = true
}

resource "google_project_iam_binding" "log-writer" {
    role = "roles/pubsub.publisher"
    members = ["${google_logging_project_sink.unified-logging.writer_identity}"]
}