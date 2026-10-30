#!/bin/bash
set -e

echo "=== DSpace Build and Push Script ==="
echo ""

# Ask for registry type
echo "Choose your registry:"
echo "1) AWS ECR"
echo "2) Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    echo ""
    echo "=== AWS ECR Configuration ==="
    read -p "Enter AWS region: " AWS_REGION
    read -p "Enter AWS account ID: " AWS_ACCOUNT_ID
    read -p "Enter ECR repository name: " ECR_REPO
    REGISTRY_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    FULL_IMAGE_NAME="${REGISTRY_URL}/${ECR_REPO}"
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    echo ""
    echo "=== Docker Hub Configuration ==="
    read -p "Enter Docker Hub username: " DOCKER_USERNAME
    read -p "Enter Docker Hub repository name: " DOCKER_REPO
    FULL_IMAGE_NAME="${DOCKER_USERNAME}/${DOCKER_REPO}"
else
    echo "Invalid choice. Exiting."
    exit 1
fi

# Ask for image tag
read -p "Enter IMAGE_TAG (press Enter for 'latest'): " IMAGE_TAG
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

# Sanitize the image name
PROJECT_NAME="dspace"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

# Build the full image name with tag
FULL_IMAGE_NAME="${FULL_IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo "=== Building Docker Image ==="
echo "Image: ${FULL_IMAGE_NAME}"
echo ""

# Build the Docker image
docker build -f Dockerfile -t "${FULL_IMAGE_NAME}" .

echo ""
echo "=== Logging in and pushing to registry ==="

if [ "$REGISTRY_CHOICE" = "1" ]; then
    # AWS ECR login
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    # Create repository if it doesn't exist
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || \
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    # Docker Hub login
    read -s -p "Enter Docker Hub password: " DOCKER_PASSWORD
    echo ""
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
fi

echo ""
echo "=== Pushing Docker Image ==="
docker push "${FULL_IMAGE_NAME}"

echo ""
echo "=== Build and Push Complete ==="
echo "Image pushed to: ${FULL_IMAGE_NAME}"
