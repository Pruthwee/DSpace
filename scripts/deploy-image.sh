#!/bin/bash
set -e
set -o pipefail

echo "=== DSpace Deploy to AWS EKS ==="
echo ""

# Ask for AWS details
read -p "Enter AWS region: " AWS_REGION
read -p "Enter EKS cluster name: " CLUSTER_NAME
read -p "Enter full image URI (registry/repo:tag): " IMAGE_URI

echo ""
echo "=== Configuring kubectl for EKS ==="
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo ""
echo "=== Verifying cluster connectivity ==="
kubectl cluster-info || exit 1

echo ""
echo "=== Collecting environment variables ==="
echo "Enter values for environment variables (press Enter to skip)."
echo "For credentials, values will be stored in a Kubernetes Secret."
echo ""

# Collect environment variables
read -p "Enter value for DB_HOST: " DB_HOST
read -p "Enter value for DB_PORT (default: 5432): " DB_PORT
DB_PORT="${DB_PORT:-5432}"
read -p "Enter value for DB_NAME: " DB_NAME
read -p "Enter value for DB_USER: " DB_USER
read -s -p "Enter value for DB_PASSWORD (will be stored in Secret): " DB_PASSWORD
echo ""

read -p "Enter value for SOLR_HOST: " SOLR_HOST
read -p "Enter value for SOLR_PORT (default: 8983): " SOLR_PORT
SOLR_PORT="${SOLR_PORT:-8983}"

read -p "Enter value for REDIS_HOST: " REDIS_HOST
read -p "Enter value for REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT="${REDIS_PORT:-6379}"

read -p "Enter value for JAVA_OPTS (default: -Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0): " JAVA_OPTS_INPUT
JAVA_OPTS="${JAVA_OPTS_INPUT:--Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0}"

read -p "Enter value for SPRING_PROFILES_ACTIVE (default: prod): " SPRING_PROFILES_ACTIVE_INPUT
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE_INPUT:-prod}"

echo ""
echo "=== Creating Kubernetes Secret for credentials ==="
kubectl create secret generic dspace-secrets -n dspace \
    --from-literal=DB_PASSWORD="$DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "=== Creating ConfigMap for non-sensitive configuration ==="
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: dspace-config
  namespace: dspace
data:
  DB_HOST: "${DB_HOST}"
  DB_PORT: "${DB_PORT}"
  DB_NAME: "${DB_NAME}"
  DB_USER: "${DB_USER}"
  SOLR_HOST: "${SOLR_HOST}"
  SOLR_PORT: "${SOLR_PORT}"
  REDIS_HOST: "${REDIS_HOST}"
  REDIS_PORT: "${REDIS_PORT}"
  JAVA_OPTS: "${JAVA_OPTS}"
  SPRING_PROFILES_ACTIVE: "${SPRING_PROFILES_ACTIVE}"
EOF

echo ""
echo "=== Applying Kubernetes manifests ==="

# Apply namespace
kubectl apply -f kubernetes/namespace.yaml

# Replace placeholders in deployment.yaml and apply
sed -i.bak 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{NAMESPACE}}|'"dspace"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_HOST}}|'"$DB_HOST"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_PORT}}|'"$DB_PORT"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_NAME}}|'"$DB_NAME"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{DB_USER}}|'"$DB_USER"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{SOLR_HOST}}|'"$SOLR_HOST"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{SOLR_PORT}}|'"$SOLR_PORT"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{REDIS_HOST}}|'"$REDIS_HOST"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{REDIS_PORT}}|'"$REDIS_PORT"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{JAVA_OPTS}}|'"$JAVA_OPTS"'|g' kubernetes/deployment.yaml
sed -i.bak 's|{{SPRING_PROFILES_ACTIVE}}|'"$SPRING_PROFILES_ACTIVE"'|g' kubernetes/deployment.yaml
rm -f kubernetes/deployment.yaml.bak

# Apply deployment
kubectl apply -f kubernetes/deployment.yaml

# Apply service
kubectl apply -f kubernetes/service.yaml

# Apply ingress
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "=== Waiting for deployment to be ready ==="
kubectl rollout status deployment/dspace -n dspace

echo ""
echo "=== Deployment Status ==="
kubectl get pods,svc,ingress -n dspace

echo ""
echo "=== Application URL ==="
kubectl get ingress dspace-ingress -n dspace -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
echo ""

echo ""
echo "=== Deployment Complete ==="
echo "Application is available at: http://dspace.example.com"
echo ""
echo "To rollback in case of failure:"
echo "  kubectl rollout undo deployment/dspace -n dspace"
