#!/bin/bash

echo "Building image kafka-python-inference-app:latest-cpu"
docker buildx build --platform linux/amd64 -f inference-app/Dockerfile -t us.gcr.io/gcp-bakdata-cluster/kafka-python-inference-app:latest-fastembed-cpu . --push
echo "Building image kafka-python-inference-app:latest-gpu"
docker buildx build --platform linux/amd64 -f inference-app/Dockerfile_GPU -t us.gcr.io/gcp-bakdata-cluster/kafka-python-inference-app:latest-fastembed-gpu . --push
echo "Building image kafka-python-inference-producer:latest"
docker buildx build --platform linux/amd64 -f paper-producer/Dockerfile -t us.gcr.io/gcp-bakdata-cluster/kafka-python-inference-producer:latest . --push
