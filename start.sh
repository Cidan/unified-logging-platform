#!/bin/bash
set -e
PROJECT=$1
BUCKET=$2

if [[ $PROJECT == "" || $BUCKET == "" ]]; then
	echo "Usage: run.sh <project-id> <staging-bucket>"
	exit 1
fi

function create-terraform {
	cd terraform/infrastructure
	terraform init
	terraform apply -auto-approve -var "project=$PROJECT" -var "bucket=$BUCKET"
	cd ../..
}

function start-random-logger {
	gcloud container clusters get-credentials unified-logging --zone us-central1-a --project $PROJECT
	kubectl apply -f k8s/deployment.yamnl
}

create-terraform
start-random-logger