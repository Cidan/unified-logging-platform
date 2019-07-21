#!/bin/bash
PROJECT=$1
BUCKET=$2

if [[ $PROJECT == "" || $BUCKET == "" ]]; then
	echo "Usage: stop.sh <project-id> <staging-bucket>"
	exit 1
fi

cd terraform/dataflow
terraform destroy -var "project=$PROJECT" -var "bucket=$BUCKET"
cd ..
cd infrastructure
terraform destroy -var "project=$PROJECT" -var "bucket=$BUCKET"
cd ../..