#!/bin/bash
set -e

PROJECT_NAME="dspace"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "=== DSpace Build and Push Script ==="
echo ""

# Ask for target platform
echo "Select target platform:"
echo "1. AWS ECR"
echo "2. Docker Hub"
echo ""
read -p "Enter choice (1 or 2): " PLATFORM_CHOICE

if [ "$PLATFORM_CHOICE" = "1" ]; then
    # AWS ECR
    read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION
    read -p "Enter ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): " REGISTRY_URL
    read -p "Enter ECR repository name (default: $IMAGE_NAME): " ECR_REPO_INPUT
    ECR_REPO=${ECR_REPO_INPUT:-$IMAGE_NAME}

    echo ""
    echo "=== Logging in to AWS ECR ==="
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY_URL

    echo ""
    echo "=== Checking if ECR repository exists ==="
    aws ecr describe-repositories --repository-names $ECR_REPO --region $AWS_REGION >/dev/null 2>&1 || aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION

    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO"
elif [ "$PLATFORM_CHOICE" = "2" ]; then
    # Docker Hub
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    echo "You will be prompted for your Docker Hub password."
    echo "$DOCKER_USERNAME"

    echo ""
    echo "=== Logging in to Docker Hub ==="
    docker login --username "$DOCKER_USERNAME"

    FULL_IMAGE_NAME="docker.io/$DOCKER_USERNAME/$IMAGE_NAME"
else
    echo "Invalid choice. Exiting."
    exit 1
fi

# Ask for image tag
read -p "Enter image tag (or press Enter for 'latest'): " IMAGE_TAG_INPUT
IMAGE_TAG=${IMAGE_TAG_INPUT:-latest}

# Sanitize tag
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr -cs 'a-z0-9./-' '-' | sed 's/^-*//;s/-*$//')
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

FULL_IMAGE_NAME="$FULL_IMAGE_NAME:$IMAGE_TAG"

echo ""
echo "=== Building Docker image ==="
echo "Image: $FULL_IMAGE_NAME"
docker build -f Dockerfile -t "$FULL_IMAGE_NAME" .

echo ""
echo "=== Pushing Docker image ==="
docker push "$FULL_IMAGE_NAME"

echo ""
echo "=== Build and push complete ==="
echo "Image pushed: $FULL_IMAGE_NAME"
