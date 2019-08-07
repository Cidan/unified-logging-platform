resource "google_pubsub_topic" "unified-logging" {
  name = "unified-logging"
}

resource "google_pubsub_subscription" "unified-logging" {
//  TODO: probably a better subscription name is "dataflow" or "processing"
  name = "unified-logging"
  topic = "${google_pubsub_topic.unified-logging.name}"
}