# DSpace Deployment Guide

This document describes how to build, push, and deploy the DSpace Server Webapp container image to AWS EKS.

## Prerequisites and System Requirements

- **Java 21** (for local builds)
- **Maven 3.9+** (for local builds)
- **Docker** (for building and pushing container images)
- **AWS CLI** (for ECR and EKS operations)
- **kubectl** (for Kubernetes operations)
- **eksctl** (for EKS cluster management, optional but recommended)

### IAM Permissions

The AWS credentials used for deployment must have permissions for:
- ECR: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:CompleteLayerUpload`, `ecr:UploadLayerPart`, `ecr:InitiateLayerUpload`, `ecr:PutImage`, `ecr:DescribeRepositories`, `ecr:CreateRepository`
- EKS: `eks:DescribeCluster`, `eks:UpdateKubeconfig` (via `aws eks update-kubeconfig`)
- Kubernetes: Full access to the target namespace

## Local Setup with Docker Compose

1. Copy `.env.example` to `.env` and fill in the required values:
   ```bash
   cp .env.example .env
   ```

2. Start the application locally:
   ```bash
   docker-compose up -d
   ```

3. The application will be available at `http://localhost:8080`.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | - |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | - |
| `DB_USER` | Database user | - |
| `DB_PASSWORD` | Database password | - |
| `SOLR_HOST` | Solr host | - |
| `SOLR_PORT` | Solr port | 8983 |
| `REDIS_HOST` | Redis host | - |
| `REDIS_PORT` | Redis port | 6379 |
| `APP_PORT` | Application port | 8080 |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | prod |

## Building and Pushing the Container Image

### Prerequisites

- Docker installed and running
- AWS CLI configured (for ECR) or Docker Hub credentials (for Docker Hub)

### Build and Push (Linux/macOS)

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will:
1. Ask for your registry choice (AWS ECR or Docker Hub)
2. Collect the necessary credentials and repository details
3. Build the Docker image using the multi-stage Dockerfile
4. Log in to the chosen registry
5. Push the image

### Build and Push (Windows)

```cmd
scripts\build-push.bat
```

## Deploying to AWS EKS

### EKS Cluster Setup

1. **Create an EKS cluster** (if not already created):
   ```bash
   eksctl create cluster --name dspace-cluster --region us-east-1 --nodegroup-name dspace-nodes --node-type t3.medium --nodes 2 --nodes-max 4
   ```

2. **Configure kubectl**:
   ```bash
   aws eks update-kubeconfig --region us-east-1 --name dspace-cluster
   ```

3. **Verify cluster connectivity**:
   ```bash
   kubectl cluster-info
   ```

### Deploy the Application

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will:
1. Ask for AWS region and EKS cluster name
2. Configure kubectl for the target cluster
3. Collect environment variables for the application
4. Create a Kubernetes Secret for credentials (DB_PASSWORD)
5. Create a ConfigMap for non-sensitive configuration
6. Apply the Kubernetes manifests (namespace, deployment, service, ingress)
7. Wait for the deployment to be ready
8. Display the application URL

### Kubernetes Manifests

The following manifests are provided under `kubernetes/`:

- **namespace.yaml**: Creates the `dspace` namespace
- **deployment.yaml**: Defines the Deployment with 2 replicas, resource limits, health probes, and environment variables
- **service.yaml**: Creates a ClusterIP service on port 80
- **ingress.yaml**: Creates an ALB ingress for external access

### Troubleshooting

#### Pod Failures

- **Check pod status**: `kubectl get pods -n dspace`
- **Check pod logs**: `kubectl logs <pod-name> -n dspace`
- **Describe pod**: `kubectl describe pod <pod-name> -n dspace`
- **Common issues**:
  - Missing environment variables: Check ConfigMap and Secret
  - Database connection failures: Verify DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
  - Solr connection failures: Verify SOLR_HOST, SOLR_PORT
  - Insufficient memory: Increase memory limits in deployment.yaml

#### Service Issues

- **Check service**: `kubectl get svc -n dspace`
- **Test service connectivity**: `kubectl exec -it <pod-name> -n dspace -- curl http://dspace-service:80/actuator/health`

#### Ingress Problems

- **Check ingress**: `kubectl get ingress -n dspace`
- **Check ingress address**: `kubectl get ingress dspace-ingress -n dspace -o wide`
- **Common issues**:
  - ALB not provisioned: Ensure the AWS Load Balancer Controller is installed
  - SSL certificate: Configure ACM certificate and add annotation to ingress
  - DNS: Update DNS records to point to the ALB address

### Scaling and Management

#### Horizontal Pod Autoscaler

To enable auto-scaling based on CPU utilization:

```bash
kubectl autoscale deployment dspace --cpu-percent=70 --min=2 --max=10 -n dspace
```

#### Rolling Updates

To update the application with a new image:

```bash
kubectl set image deployment/dspace dspace=<new-image-uri> -n dspace
kubectl rollout status deployment/dspace -n dspace
```

#### Rollback

To rollback to a previous revision:

```bash
kubectl rollout history deployment/dspace -n dspace
kubectl rollout undo deployment/dspace --to-revision=<revision> -n dspace
```

## Configuration Management

The application reads configuration from environment variables, which are injected via Kubernetes ConfigMaps (non-sensitive) and Secrets (sensitive). The `DSpaceConfigurationService` handles the mapping of environment variables to DSpace configuration properties.

### Key Configuration Properties

- `dspace.dir`: The DSpace installation directory (set via `DSPACE_HOME` env var)
- `db.url`, `db.name`, `db.user`, `db.password`: Database connection settings
- `solr.server`, `solr.search.server`, `solr.authority.server`: Solr connection settings
- `usage_statistics.solr`: Usage statistics Solr connection settings

## Security Considerations

- **Secrets**: All credentials (DB_PASSWORD) are stored in Kubernetes Secrets, never in ConfigMaps
- **Non-root user**: The container runs as a non-root user
- **Read-only root filesystem**: The application uses writable volumes for temporary files
- **TLS**: Enable TLS by configuring the ALB with an ACM certificate
- **Network policies**: Apply Kubernetes NetworkPolicies to restrict pod-to-pod communication
- **Resource limits**: CPU and memory limits are set to prevent resource exhaustion
- **Image scanning**: Scan container images for vulnerabilities before deploying

## Java Notes

### JVM Settings

The container is configured with the following JVM settings:
- `-Xmx512m`: Maximum heap size of 512MB
- `-Xms256m`: Initial heap size of 256MB
- `-XX:+UseContainerSupport`: Enables container-aware JVM settings (Java 21+)
- `-XX:MaxRAMPercentage=75.0`: JVM will use up to 75% of the container's memory limit

These settings should be tuned based on the actual memory usage of your DSpace instance. For production workloads with large datasets, consider increasing the memory limits.

### Spring Profiles

The application uses Spring profiles for environment-specific configuration. The default profile is `prod`. You can override this by setting the `SPRING_PROFILES_ACTIVE` environment variable.

### Health Check Endpoint

The application exposes a health check endpoint at `/actuator/health`. This endpoint is used by Kubernetes liveness and readiness probes:

- **Liveness probe**: Checks if the application is running (initial delay: 60s, period: 30s)
- **Readiness probe**: Checks if the application is ready to serve traffic (initial delay: 30s, period: 10s)

The health check includes checks for the database, Solr, and other critical services configured in the DSpace instance.
