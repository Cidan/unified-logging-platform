#!/bin/bash
set -e
set -u

PROJECT=$1
BUCKET=$2

if [[ $PROJECT == "" || $BUCKET == "" ]]; then
	echo "Usage: run.sh <project-id> <staging-bucket>"
	exit 1
fi

# Create unique bucket name
BUCKET=${PROJECT}-${BUCKET}

function enable-apis {
  echo "Enabling required APIs for project ${PROJECT} ..."
  gcloud services enable cloudbuild.googleapis.com
  gcloud services enable storage-component.googleapis.com
  gcloud services enable containerregistry.googleapis.com
  gcloud services enable container.googleapis.com
  gcloud services enable dataflow.googleapis.com
  gcloud services enable cloudtrace.googleapis.com
}

function create-container {
	echo "Creating container image..."
	cd logger
	gcloud builds submit --config=cloudbuild.yaml --project=$PROJECT
	cd ..
}

function create-terraform {
	cd terraform/infrastructure
	terraform init
	terraform apply -auto-approve -var "project=$PROJECT" -var "bucket=$BUCKET"
	cd ../..
}

function start-dataflow {
	cd terraform/dataflow
	terraform init
	terraform apply -auto-approve -var "project=$PROJECT" -var "bucket=$BUCKET"
	cd ../..
}

function start-random-logger {
	gcloud container clusters get-credentials unified-logging --zone us-central1-a --project $PROJECT
	sed "s/{{PROJECT}}/$PROJECT/" k8s/deployment.yaml | kubectl apply -f -
}

function create-dataflow-template {
	cd beam/unified-logging
	TEMPLATE_DIR=${BUCKET}/dataflow-template
	mvn test exec:java \
	-Dexec.mainClass=com.google.UnifiedLogging \
	-Dexec.args="--runner=DataflowRunner \
  --project=$PROJECT \
  --stagingLocation=gs://$BUCKET/dataflow-staging \
  --templateLocation=gs://${TEMPLATE_DIR}/unified-logging"

  gsutil cp src/main/metadata/unified-logging.json_metadata gs://${TEMPLATE_DIR}/
	cd ../../
}

function display-data {
  echo "Pausing for two minutes before querying the data, but might be not enough..."
  sleep 2*60
  bq query 'SELECT resource_type, COUNT(*) AS number_of_logs FROM unified_logging.logs GROUP BY resource_type'
}


gcloud config set project ${PROJECT}
enable-apis
create-container
create-terraform
create-dataflow-template
start-random-logger
start-dataflow
display-data