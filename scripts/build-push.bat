@echo off
setlocal enabledelayedexpansion

set "PROJECT_NAME=dspace"
set "IMAGE_NAME=dspace"

echo === DSpace Build and Push Script ===
echo.

echo Select target platform:
echo 1. AWS ECR
echo 2. Docker Hub
echo.
set /p PLATFORM_CHOICE="Enter choice (1 or 2): "

if "!PLATFORM_CHOICE!"=="1" (
    set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
    set /p REGISTRY_URL="Enter ECR registry URL (e.g., 123456789012.dkr.ecr.us-east-1.amazonaws.com): "
    set /p ECR_REPO_INPUT="Enter ECR repository name (default: !IMAGE_NAME!): "
    if "!ECR_REPO_INPUT!"=="" (
        set "ECR_REPO=!IMAGE_NAME!"
    ) else (
        set "ECR_REPO=!ECR_REPO_INPUT!"
    )

    echo.
    echo === Logging in to AWS ECR ===
    call aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo ECR login failed
        exit /b 1
    )

    echo.
    echo === Checking if ECR repository exists ===
    call aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Creating ECR repository...
        call aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
    )

    set "FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!"
) else if "!PLATFORM_CHOICE!"=="2" (
    set /p DOCKER_USERNAME="Enter Docker Hub username: "
    echo You will be prompted for your Docker Hub password.

    echo.
    echo === Logging in to Docker Hub ===
    call docker login --username "!DOCKER_USERNAME!"
    if !ERRORLEVEL! neq 0 (
        echo Docker Hub login failed
        exit /b 1
    )

    set "FULL_IMAGE_NAME=docker.io/!DOCKER_USERNAME!/!IMAGE_NAME!"
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

set /p IMAGE_TAG_INPUT="Enter image tag (or press Enter for 'latest'): "
if "!IMAGE_TAG_INPUT!"=="" (
    set "IMAGE_TAG=latest"
) else (
    set "IMAGE_TAG=!IMAGE_TAG_INPUT!"
)

set "FULL_IMAGE_NAME=!FULL_IMAGE_NAME!:!IMAGE_TAG!"

echo.
echo === Building Docker image ===
echo Image: !FULL_IMAGE_NAME!
call docker build -f Dockerfile -t "!FULL_IMAGE_NAME!" .
if !ERRORLEVEL! neq 0 (
    echo Docker build failed
    exit /b 1
)

echo.
echo === Pushing Docker image ===
call docker push "!FULL_IMAGE_NAME!"
if !ERRORLEVEL! neq 0 (
    echo Docker push failed
    exit /b 1
)

echo.
echo === Build and push complete ===
echo Image pushed: !FULL_IMAGE_NAME!

endlocal
