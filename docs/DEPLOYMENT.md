# DSpace Deployment Guide

## Overview

This document describes how to build, deploy, and manage DSpace (a Spring Boot-based institutional repository) on Amazon EKS (Elastic Kubernetes Service).

## Prerequisites

### System Requirements

- **Java**: Java 21 (JDK)
- **Maven**: 3.9.x (for building from source)
- **Docker**: 24.x or later
- **AWS CLI**: 2.x
- **kubectl**: 1.28 or later
- **eksctl**: 0.160 or later

### AWS Prerequisites

- An AWS account with permissions to create ECR repositories, EKS clusters, and IAM roles
- IAM permissions for ECR, EKS, and Kubernetes resource creation
- AWS credentials configured (`aws configure`)

### External Services

DSpace requires the following external services, which must be provisioned separately:

- **PostgreSQL Database**: DSpace supports PostgreSQL 15+
- **Apache Solr**: Solr 9.x (standalone instance)
- **Assetstore**: File system storage or S3-compatible object storage

These services are reached through environment variables and are NOT included in the container image or docker-compose.yml.

## Local Setup with Docker Compose

### Building Locally

1. Build the Docker image:
   ```bash
   ./scripts/build-push.sh
   ```
   When prompted, select "Docker Hub" as the target platform.

2. Create a `.env` file with your configuration:
   ```bash
   DB_URL=jdbc:postgresql://host.docker.internal:5432/dspace
   DB_USERNAME=dspace
   DB_PASSWORD=dspace
   SOLR_SERVER=http://host.docker.internal:8983/solr
   DSPACE_SERVER_URL=http://localhost:8080/server
   DSPACE_UI_URL=http://localhost:4000
   ```

3. Start the application:
   ```bash
   docker-compose up -d
   ```

4. Check health:
   ```bash
   curl http://localhost:8080/server/actuator/health
   ```

## Building and Pushing the Docker Image

### Using the Shell Script (Linux/macOS)

```bash
./scripts/build-push.sh
```

The script will:
1. Ask you to select the target platform (AWS ECR or Docker Hub)
2. Prompt for registry details
3. Prompt for an image tag (defaults to `latest`)
4. Build the Docker image using the multi-stage Dockerfile
5. Log in to the selected registry
6. Push the image

### Using the Batch Script (Windows)

```cmd
scripts\build-push.bat
```

## AWS EKS Deployment

### EKS Cluster Setup

1. **Install the AWS CLI and eksctl**:
   ```bash
   # Install AWS CLI
   curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   unzip awscliv2.zip
   sudo ./aws/install

   # Install eksctl
   curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" \
       | tar xz -z
   sudo mv eksctl /usr/local/bin
   ```

2. **Create an EKS cluster** (using eksctl):
   ```bash
   eksctl create cluster \
       --name dspace \
       --region us-east-1 \
       --version 1.28 \
       --nodegroup-name dspace-nodes \
       --node-type t3.medium \
       --nodes 2 \
       --nodes-min 2 \
       --nodes-max 4
   ```

3. **Configure kubectl**:
   ```bash
   aws eks update-kubeconfig --region us-east-1 --name dspace
   ```

4. **Verify cluster connectivity**:
   ```bash
   kubectl cluster-info
   ```

### Deploying the Application

1. **Run the deployment script**:
   ```bash
   ./scripts/deploy-image.sh
   ```

2. **Provide the requested information**:
   - AWS region
   - EKS cluster name
   - Full image URI (from the build-push step)
   - Environment variables for DSpace configuration
   - Database password (entered securely, not echoed)

3. **The script will**:
   - Configure kubectl for the EKS cluster
   - Create the `dspace` namespace
   - Create a Kubernetes Secret for the database password
   - Replace placeholders in the deployment manifest
   - Apply the namespace, deployment, service, and ingress resources
   - Wait for the deployment to complete
   - Display the deployment status and application URL

### Deployment Walkthrough

The Kubernetes manifests in the `kubernetes/` directory include:

- **namespace.yaml**: Creates a dedicated `dspace` namespace
- **deployment.yaml**: Defines a Deployment with 2 replicas, resource limits/requests, liveness and readiness probes, and environment variable configuration
- **service.yaml**: Creates a ClusterIP service routing traffic to the application pods
- **ingress.yaml**: Configures an AWS ALB ingress for external access

### Troubleshooting

#### Pod Failures

- **CrashLoopBackOff**: Check pod logs with `kubectl logs <pod-name> -n dspace`
- **ImagePullBackOff**: Verify the image URI and registry credentials
- **OOMKilled**: The container exceeded its memory limit; increase the memory limit in the deployment

#### Service Issues

- **Service not reachable**: Verify the service selector matches the pod labels
- **Port mismatch**: Ensure the service targetPort matches the container port (8080)

#### Ingress Problems

- **Ingress not creating ALB**: Verify the AWS Load Balancer Controller is installed in the cluster
- **404 on ingress**: Check that the ingress annotations are correct and the host matches

### Scaling and Management

#### Horizontal Pod Autoscaler

To enable auto-scaling based on CPU utilization:

```bash
kubectl autoscale deployment dspace --cpu-percent=70 --min=2 --max=10 -n dspace
```

#### Rolling Updates

To update the application image:

```bash
# Update the image in the deployment
kubectl set image deployment/dspace dspace=<new-image-uri> -n dspace

# Monitor the rollout
kubectl rollout status deployment/dspace -n dspace

# Rollback if needed
kubectl rollout undo deployment/dspace -n dspace
```

#### Rollback

To rollback to a previous revision:

```bash
# View rollout history
kubectl rollout history deployment/dspace -n dspace

# Rollback to the previous revision
kubectl rollout undo deployment/dspace -n dspace

# Rollback to a specific revision
kubectl rollout undo deployment/dspace --to-revision=2 -n dspace
```

## Configuration Management

DSpace reads configuration from environment variables using the DSpace Configuration Service. The following environment variables are used:

| Variable | Description | Default |
|----------|-------------|---------|
| `dspace__P__dir` | DSpace installation directory | `/dspace` |
| `db__P__url` | Database JDBC URL | `jdbc:postgresql://localhost:5432/dspace` |
| `db__P__username` | Database username | `dspace` |
| `db__P__password` | Database password | (from Kubernetes Secret) |
| `solr__P__server` | Solr server URL | `http://localhost:8983/solr` |
| `dspace__P__server__P__url` | DSpace server URL | `http://localhost:8080/server` |
| `dspace__P__ui__P__url` | DSpace UI URL | `http://localhost:4000` |

### Kubernetes Secrets

Sensitive values (such as database passwords) are stored in a Kubernetes Secret named `dspace-secrets` in the `dspace` namespace. The deployment script creates this secret automatically.

## Security Considerations

- The container runs as a non-root user (`dspace`)
- The JVM is configured with container-aware memory settings
- Health checks use the Spring Boot Actuator endpoint
- Database credentials are stored in Kubernetes Secrets, never in plain text
- The container image is based on Amazon Corretto 21 (minimal)
- Resource limits prevent resource exhaustion

## Java Notes

### JVM Settings

The container uses the following JVM settings optimized for containerized deployment:

- `-Xmx512m`: Maximum heap size of 512MB
- `-Xms256m`: Initial heap size of 256MB
- `-XX:+UseContainerSupport`: Enables container-aware JVM features
- `-XX:MaxRAMPercentage=75.0`: Limits heap to 75% of container memory limit

These settings are configured via the `JAVA_OPTS` environment variable and can be overridden at deployment time. The memory requests (512Mi) and limits (1Gi) in the Kubernetes deployment are aligned with these JVM settings.

### Spring Profiles

The application runs with the `prod` Spring profile by default (`SPRING_PROFILES_ACTIVE=prod`). Additional profiles can be activated by setting the `SPRING_PROFILES_ACTIVE` environment variable.

### Health Endpoints

The application exposes health endpoints via Spring Boot Actuator:

- **Health**: `GET /server/actuator/health` - Returns application health status
- **Info**: `GET /server/actuator/info` - Returns application information

The Kubernetes deployment uses the health endpoint for both liveness and readiness probes:
- **Liveness probe**: Checks if the application is running (30s initial delay, 30s interval)
- **Readiness probe**: Checks if the application is ready to serve traffic (10s initial delay, 10s interval)

Both probes expect a 200 HTTP response from the health endpoint.
