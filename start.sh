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

create-terraform