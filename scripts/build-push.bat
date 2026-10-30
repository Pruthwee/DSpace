@echo off
setlocal enabledelayedexpansion

echo === DSpace Build and Push Script ===
echo.

echo Choose your registry:
echo 1) AWS ECR
echo 2) Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo === AWS ECR Configuration ===
    set /p AWS_REGION="Enter AWS region: "
    set /p AWS_ACCOUNT_ID="Enter AWS account ID: "
    set /p ECR_REPO="Enter ECR repository name: "
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo === Docker Hub Configuration ===
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    set /p DOCKER_REPO="Enter Docker Hub repository name: "
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!DOCKER_REPO!
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

set /p IMAGE_TAG="Enter IMAGE_TAG (press Enter for 'latest'): "
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

set PROJECT_NAME=dspace
set IMAGE_NAME=dspace

echo.
echo === Building Docker Image ===
echo Image: !FULL_IMAGE_NAME!:!IMAGE_TAG!
echo.

docker build -f Dockerfile -t "!FULL_IMAGE_NAME!:!IMAGE_TAG!" .

if !ERRORLEVEL! neq 0 (
    echo Docker build failed
    exit /b 1
)

echo.
echo === Logging in and pushing to registry ===

if "!REGISTRY_CHOICE!"=="1" (
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ECR login failed
        exit /b 1
    )
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    )
) else if "!REGISTRY_CHOICE!"=="2" (
    set /p DOCKER_PASSWORD="Enter Docker Hub password: "
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Docker Hub login failed
        exit /b 1
    )
)

echo.
echo === Pushing Docker Image ===
docker push "!FULL_IMAGE_NAME!:!IMAGE_TAG!"

if !ERRORLEVEL! neq 0 (
    echo Docker push failed
    exit /b 1
)

echo.
echo === Build and Push Complete ===
echo Image pushed to: !FULL_IMAGE_NAME!:!IMAGE_TAG!
