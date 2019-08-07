#!/bin/bash
set -e
set -u

PROJECT=$1
BUCKET=$2

if [[ $PROJECT == "" || $BUCKET == "" ]]; then
	echo "Usage: run.sh <project-id> <staging-bucket>"
	exit 1
fi

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
	mvn compile exec:java \
	-Dexec.mainClass=com.google.UnifiedLogging \
	-Dexec.args="--runner=DataflowRunner \
  --project=$PROJECT \
  --stagingLocation=gs://$BUCKET/dataflow-staging \
  --templateLocation=gs://$BUCKET/dataflow-template/unified-logging"
	cd ../../
}

create-container
create-terraform
create-dataflow-template
start-random-logger
start-dataflow