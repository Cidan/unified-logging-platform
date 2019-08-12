resource "google_pubsub_topic" "unified-logging" {
  name = "unified-logging"
}

resource "google_pubsub_subscription" "unified-logging" {
  name = "unified-logging-sub"
  topic = "${google_pubsub_topic.unified-logging.name}"
}