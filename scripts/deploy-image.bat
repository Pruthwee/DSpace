@echo off
setlocal enabledelayedexpansion

set "PROJECT_NAME=dspace"
set "IMAGE_NAME=dspace"
set "NAMESPACE=dspace"

echo === DSpace Deployment Script for AWS EKS ===
echo.

set /p AWS_REGION="Enter AWS region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS cluster name: "
set /p IMAGE_URI="Enter full image URI (registry/repo:tag): "

echo.
echo === Environment Variables ===
echo Please provide values for the environment variables required by DSpace.
echo Press Enter to skip (a placeholder will remain in the deployment).
echo.

set /p DB_URL_VALUE="Enter value for DB_URL (or press Enter to skip): "
set /p DB_USERNAME_VALUE="Enter value for DB_USERNAME (or press Enter to skip): "
set /p DB_PASSWORD_VALUE="Enter value for DB_PASSWORD (or press Enter to skip): "
set /p SOLR_SERVER_VALUE="Enter value for SOLR_SERVER (or press Enter to skip): "
set /p DSPACE_SERVER_URL_VALUE="Enter value for DSPACE_SERVER_URL (or press Enter to skip): "
set /p DSPACE_UI_URL_VALUE="Enter value for DSPACE_UI_URL (or press Enter to skip): "

echo.
echo === Configuring kubectl for EKS cluster ===
call aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl
    exit /b 1
)

echo.
echo === Checking cluster connectivity ===
call kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Cluster connectivity check failed
    exit /b 1
)

echo.
echo === Creating namespace ===
call kubectl apply -f kubernetes/namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo Namespace creation failed
    exit /b 1
)

echo.
echo === Creating secrets ===
if not "!DB_PASSWORD_VALUE!"=="" (
    call kubectl create secret generic !NAMESPACE!-secrets -n !NAMESPACE! --from-literal=db-password="!DB_PASSWORD_VALUE!" --dry-run=client -o yaml | kubectl apply -f -
    if !ERRORLEVEL! neq 0 (
        echo Secret creation failed
        exit /b 1
    )
    echo Secret created with db-password
)

echo.
echo === Deploying application ===
copy kubernetes\deployment.yaml /tmp\deployment.yaml.bak >nul

REM Replace IMAGE_URI
powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"

REM Replace namespace
powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{NAMESPACE}}', '!NAMESPACE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"

REM Replace environment variables
if not "!DB_URL_VALUE!"=="" (
    powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{DB_URL}}', '!DB_URL_VALUE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"
)
if not "!DB_USERNAME_VALUE!"=="" (
    powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{DB_USERNAME}}', '!DB_USERNAME_VALUE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"
)
if not "!SOLR_SERVER_VALUE!"=="" (
    powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{SOLR_SERVER}}', '!SOLR_SERVER_VALUE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"
)
if not "!DSPACE_SERVER_URL_VALUE!"=="" (
    powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{DSPACE_SERVER_URL}}', '!DSPACE_SERVER_URL_VALUE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"
)
if not "!DSPACE_UI_URL_VALUE!"=="" (
    powershell -Command "(gc /tmp/deployment.yaml.bak) -replace '{{DSPACE_UI_URL}}', '!DSPACE_UI_URL_VALUE!' | Out-File -Encoding utf8 /tmp/deployment.yaml.bak"
)

call kubectl apply -f /tmp/deployment.yaml.bak -n !NAMESPACE!
if !ERRORLEVEL! neq 0 (
    echo Deployment failed
    del /tmp/deployment.yaml.bak
    exit /b 1
)

del /tmp/deployment.yaml.bak

echo.
echo === Creating service ===
call kubectl apply -f kubernetes/service.yaml -n !NAMESPACE!
if !ERRORLEVEL! neq 0 (
    echo Service creation failed
    exit /b 1
)

echo.
echo === Creating ingress ===
call kubectl apply -f kubernetes/ingress.yaml -n !NAMESPACE!
if !ERRORLEVEL! neq 0 (
    echo Ingress creation failed
    exit /b 1
)

echo.
echo === Waiting for deployment rollout ===
call kubectl rollout status deployment/!IMAGE_NAME! -n !NAMESPACE!
if !ERRORLEVEL! neq 0 (
    echo Rollout failed
    exit /b 1
)

echo.
echo === Deployment status ===
call kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo === Application URL ===
echo The application will be available at: http://!IMAGE_NAME!.example.com
echo.

echo === Deployment complete ===
echo.
echo If the deployment fails, you can rollback with:
echo   kubectl rollout undo deployment/!IMAGE_NAME! -n !NAMESPACE!
echo.
echo To check logs:
echo   kubectl logs -f deployment/!IMAGE_NAME! -n !NAMESPACE!

endlocal
