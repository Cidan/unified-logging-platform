resource "google_compute_instance_template" "unified-logging" {
  name        = "unified-logging-template"
  description = "VM's that output fake logs"

  metadata_startup_script = "curl -sSO https://dl.google.com/cloudagents/install-monitoring-agent.sh | sudo bash"

  instance_description = "Unified logging output server"
  machine_type         = "n1-standard-2"
  can_ip_forward       = false

  scheduling {
    automatic_restart   = true
    on_host_maintenance = "MIGRATE"
  }

  disk {
    source_image = "debian-cloud/debian-9"
    auto_delete  = false
    boot         = true
  }

  network_interface {
    subnetwork = "${google_compute_subnetwork.unified-logging.name}"
    access_config {}
  }

  service_account {
    scopes = [
      "userinfo-email",
      "compute-ro",
      "storage-ro",
      "logging.write",
      "monitoring.write",
      "trace.append"
    ]
  }
}

resource "google_compute_region_instance_group_manager" "unified-logging" {
  name = "unified-logging"

  base_instance_name         = "unified-logging"
  instance_template          = "${google_compute_instance_template.unified-logging.self_link}"
  region                     = "us-central1"

  target_size  = 5

}