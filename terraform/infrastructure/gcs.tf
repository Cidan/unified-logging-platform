resource "google_storage_bucket" "staging-bucket" {
  name = "${var.bucket}"
  location = "US"
  force_destroy = true
}