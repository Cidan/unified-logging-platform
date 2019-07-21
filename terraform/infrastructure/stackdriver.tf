resource "google_logging_project_sink" "unified-logging" {
    name = "unified-logging-pubsub"
    destination = "pubsub.googleapis.com/projects/${var.project}/topics/${google_pubsub_topic.unified-logging.name}"
    filter = ""
    unique_writer_identity = true
}

resource "google_pubsub_topic_iam_binding" "binding" {
    topic       = "${google_pubsub_topic.unified-logging.name}"
    role        = "roles/pubsub.publisher"
    members     = ["serviceAccount:${google_logging_project_sink.unified-logging.writer_identity}"]
}
