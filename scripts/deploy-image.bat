@echo off
setlocal enabledelayedexpansion

echo === DSpace Deploy to AWS EKS ===
echo.

echo === AWS EKS Configuration ===
set /p AWS_REGION="Enter AWS region: "
set /p CLUSTER_NAME="Enter EKS cluster name: "
set /p IMAGE_URI="Enter full image URI (registry/repo:tag): "

echo.
echo === Configuring kubectl for EKS ===
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl
    exit /b 1
)

echo.
echo === Verifying cluster connectivity ===
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Cluster connection failed
    exit /b 1
)

echo.
echo === Collecting environment variables ===
echo Enter values for environment variables (press Enter to skip).
echo For credentials, values will be stored in a Kubernetes Secret.
echo.

set /p DB_HOST="Enter value for DB_HOST: "
set /p DB_PORT="Enter value for DB_PORT (default: 5432): "
if "!DB_PORT!"=="" set DB_PORT=5432
set /p DB_NAME="Enter value for DB_NAME: "
set /p DB_USER="Enter value for DB_USER: "
set /p DB_PASSWORD="Enter value for DB_PASSWORD (will be stored in Secret): "

set /p SOLR_HOST="Enter value for SOLR_HOST: "
set /p SOLR_PORT="Enter value for SOLR_PORT (default: 8983): "
if "!SOLR_PORT!"=="" set SOLR_PORT=8983

set /p REDIS_HOST="Enter value for REDIS_HOST: "
set /p REDIS_PORT="Enter value for REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p JAVA_OPTS_INPUT="Enter value for JAVA_OPTS (default: -Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0): "
if "!JAVA_OPTS_INPUT!"=="" (
    set JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
) else (
    set JAVA_OPTS=!JAVA_OPTS_INPUT!
)

set /p SPRING_PROFILES_ACTIVE_INPUT="Enter value for SPRING_PROFILES_ACTIVE (default: prod): "
if "!SPRING_PROFILES_ACTIVE_INPUT!"=="" (
    set SPRING_PROFILES_ACTIVE=prod
) else (
    set SPRING_PROFILES_ACTIVE=!SPRING_PROFILES_ACTIVE_INPUT!
)

echo.
echo === Creating Kubernetes Secret for credentials ===
kubectl create secret generic dspace-secrets -n dspace --from-literal=DB_PASSWORD="!DB_PASSWORD!" --dry-run=client -o yaml | kubectl apply -f -
if !ERRORLEVEL! neq 0 (
    echo Failed to create secret
    exit /b 1
)

echo.
echo === Creating ConfigMap for non-sensitive configuration ===
kubectl create configmap dspace-config -n dspace ^
    --from-literal=DB_HOST="!DB_HOST!" ^
    --from-literal=DB_PORT="!DB_PORT!" ^
    --from-literal=DB_NAME="!DB_NAME!" ^
    --from-literal=DB_USER="!DB_USER!" ^
    --from-literal=SOLR_HOST="!SOLR_HOST!" ^
    --from-literal=SOLR_PORT="!SOLR_PORT!" ^
    --from-literal=REDIS_HOST="!REDIS_HOST!" ^
    --from-literal=REDIS_PORT="!REDIS_PORT!" ^
    --from-literal=JAVA_OPTS="!JAVA_OPTS!" ^
    --from-literal=SPRING_PROFILES_ACTIVE="!SPRING_PROFILES_ACTIVE!" ^
    --dry-run=client -o yaml | kubectl apply -f -
if !ERRORLEVEL! neq 0 (
    echo Failed to create configmap
    exit /b 1
)

echo.
echo === Applying Kubernetes manifests ===

kubectl apply -f kubernetes\namespace.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply namespace
    exit /b 1
)

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace IMAGE_URI
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{NAMESPACE}}', 'dspace' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace NAMESPACE
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{DB_HOST}}', '!DB_HOST!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace DB_HOST
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{DB_PORT}}', '!DB_PORT!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace DB_PORT
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{DB_NAME}}', '!DB_NAME!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace DB_NAME
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{DB_USER}}', '!DB_USER!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace DB_USER
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{SOLR_HOST}}', '!SOLR_HOST!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace SOLR_HOST
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{SOLR_PORT}}', '!SOLR_PORT!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace SOLR_PORT
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace REDIS_HOST
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace REDIS_PORT
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{JAVA_OPTS}}', '!JAVA_OPTS!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace JAVA_OPTS
    exit /b 1
)
powershell -Command "(Get-Content kubernetes\deployment.yaml -Raw) -replace '{{SPRING_PROFILES_ACTIVE}}', '!SPRING_PROFILES_ACTIVE!' | Set-Content kubernetes\deployment.yaml"
if !ERRORLEVEL! neq 0 (
    echo Failed to replace SPRING_PROFILES_ACTIVE
    exit /b 1
)

kubectl apply -f kubernetes\deployment.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply deployment
    exit /b 1
)

kubectl apply -f kubernetes\service.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply service
    exit /b 1
)

kubectl apply -f kubernetes\ingress.yaml
if !ERRORLEVEL! neq 0 (
    echo Failed to apply ingress
    exit /b 1
)

echo.
echo === Waiting for deployment to be ready ===
kubectl rollout status deployment/dspace -n dspace
if !ERRORLEVEL! neq 0 (
    echo Deployment failed
    exit /b 1
)

echo.
echo === Deployment Status ===
kubectl get pods,svc,ingress -n dspace

echo.
echo === Deployment Complete ===
echo Application is available at: http://dspace.example.com
echo.
echo To rollback in case of failure:
echo   kubectl rollout undo deployment/dspace -n dspace
