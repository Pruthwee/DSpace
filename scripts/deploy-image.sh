#!/bin/bash
set -e
set -o pipefail

PROJECT_NAME="dspace"
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')
NAMESPACE="$IMAGE_NAME"

echo "=== DSpace Deployment Script for AWS EKS ==="
echo ""

# Ask for AWS region
read -p "Enter AWS region (e.g., us-east-1): " AWS_REGION

# Ask for EKS cluster name
read -p "Enter EKS cluster name: " CLUSTER_NAME

# Ask for image URI
read -p "Enter full image URI (registry/repo:tag): " IMAGE_URI

# Ask for environment variables
echo ""
echo "=== Environment Variables ==="
echo "Please provide values for the environment variables required by DSpace."
echo "Press Enter to skip (a placeholder will remain in the deployment)."
echo ""

read -p "Enter value for DB_URL (or press Enter to skip): " DB_URL_VALUE
read -p "Enter value for DB_USERNAME (or press Enter to skip): " DB_USERNAME_VALUE
read -s -p "Enter value for DB_PASSWORD (or press Enter to skip): " DB_PASSWORD_VALUE
echo ""
read -p "Enter value for SOLR_SERVER (or press Enter to skip): " SOLR_SERVER_VALUE
read -p "Enter value for DSPACE_SERVER_URL (or press Enter to skip): " DSPACE_SERVER_URL_VALUE
read -p "Enter value for DSPACE_UI_URL (or press Enter to skip): " DSPACE_UI_URL_VALUE

echo ""
echo "=== Configuring kubectl for EKS cluster ==="
aws eks update-kubeconfig --region $AWS_REGION --name $CLUSTER_NAME

echo ""
echo "=== Checking cluster connectivity ==="
kubectl cluster-info || exit 1

echo ""
echo "=== Creating namespace ==="
kubectl apply -f kubernetes/namespace.yaml

echo ""
echo "=== Creating secrets ==="
if [ -n "$DB_PASSWORD_VALUE" ]; then
    kubectl create secret generic $NAMESPACE-secrets -n $NAMESPACE \
        --from-literal=db-password="$DB_PASSWORD_VALUE" \
        --dry-run=client -o yaml | kubectl apply -f -
    echo "Secret created with db-password"
fi

echo ""
echo "=== Deploying application ==="
# Copy deployment.yaml and replace placeholders
cp kubernetes/deployment.yaml /tmp/deployment.yaml.bak

# Replace IMAGE_URI
sed -i.bak 's|{{IMAGE_URI}}|'$IMAGE_URI'|g' /tmp/deployment.yaml.bak

# Replace namespace
sed -i.bak 's|{{NAMESPACE}}|'$NAMESPACE'|g' /tmp/deployment.yaml.bak

# Replace environment variables
if [ -n "$DB_URL_VALUE" ]; then
    sed -i.bak 's|{{DB_URL}}|'$DB_URL_VALUE'|g' /tmp/deployment.yaml.bak
fi
if [ -n "$DB_USERNAME_VALUE" ]; then
    sed -i.bak 's|{{DB_USERNAME}}|'$DB_USERNAME_VALUE'|g' /tmp/deployment.yaml.bak
fi
if [ -n "$SOLR_SERVER_VALUE" ]; then
    sed -i.bak 's|{{SOLR_SERVER}}|'$SOLR_SERVER_VALUE'|g' /tmp/deployment.yaml.bak
fi
if [ -n "$DSPACE_SERVER_URL_VALUE" ]; then
    sed -i.bak 's|{{DSPACE_SERVER_URL}}|'$DSPACE_SERVER_URL_VALUE'|g' /tmp/deployment.yaml.bak
fi
if [ -n "$DSPACE_UI_URL_VALUE" ]; then
    sed -i.bak 's|{{DSPACE_UI_URL}}|'$DSPACE_UI_URL_VALUE'|g' /tmp/deployment.yaml.bak
fi

# Remove .bak files
rm -f /tmp/deployment.yaml.bak.bak

# Apply deployment
kubectl apply -f /tmp/deployment.yaml.bak -n $NAMESPACE

# Clean up
rm -f /tmp/deployment.yaml.bak

echo ""
echo "=== Creating service ==="
kubectl apply -f kubernetes/service.yaml -n $NAMESPACE

echo ""
echo "=== Creating ingress ==="
kubectl apply -f kubernetes/ingress.yaml -n $NAMESPACE

echo ""
echo "=== Waiting for deployment rollout ==="
kubectl rollout status deployment/$IMAGE_NAME -n $NAMESPACE

echo ""
echo "=== Deployment status ==="
kubectl get pods,svc,ingress -n $NAMESPACE

echo ""
echo "=== Application URL ==="
echo "The application will be available at: http://$IMAGE_NAME.example.com"
echo ""

echo "=== Deployment complete ==="
echo ""
echo "If the deployment fails, you can rollback with:"
echo "  kubectl rollout undo deployment/$IMAGE_NAME -n $NAMESPACE"
echo ""
echo "To check logs:"
echo "  kubectl logs -f deployment/$IMAGE_NAME -n $NAMESPACE"
