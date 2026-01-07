# Nexus Repository Manager Integration

## 📋 Overview

Nexus Repository Manager is central to the project's CI/CD infrastructure, serving two primary roles:
1.  **Proxy & Cache**: Caches external dependencies (Maven Central, NPM Registry) to speed up builds and ensure offline availability.
2.  **Private Registry**: Stores internal build artifacts (Docker images, JAR snapshots) for deployment.

## 🏗️ Repository Architecture

The project uses a structured repository layout hosted on the Nexus server (`vps-77043236.vps.ovh.ca`).

### 1. Maven Repositories (Port 7071)
Used by backend services (`pom.xml`) for Java dependencies.

| Type | Repository Name | URL | Description |
|------|----------------|-----|-------------|
| **Group** | `buy02-group` | `.../repository/buy02-group/` | A single entry point aggregating all other repos (releases, snapshots, central-proxy). |
| **Hosted** | `buy02-releases` | `.../repository/buy02-releases/` | Stores stable release artifacts. |
| **Hosted** | `buy02-snapshots` | `.../repository/buy02-snapshots/` | Stores development snapshots (e.g., `1.0.0-SNAPSHOT`). |
| **Proxy** | `maven-central` | - | Proxies requests to Maven Central. |

#### Repository Previews
**Releases Repository (`buy02-releases`)**
![Releases Repository](https://mwkrwffquzzsvssigsup.supabase.co/storage/v1/object/public/media-service/Screenshot%20from%202026-01-07%2003-03-14.png)

**Snapshots Repository (`buy02-snapshots`)**
![Snapshots Repository](https://mwkrwffquzzsvssigsup.supabase.co/storage/v1/object/public/media-service/Screenshot%20from%202026-01-07%2003-02-52.png)

### 2. Docker Registry (Port 8089)
Used for storing container images built by Jenkins.

- **Registry URL**: `vps-77043236.vps.ovh.ca:8089`
- **Authentication**: Requires Nexus credentials (injected via Jenkins).
- **Images**: Stores all microservice images tagged with build IDs (e.g., `api-gateway:build-105-a1b2c3d`) and `latest`.

#### Docker Registry Preview
**Docker Repository**
![Docker Repository](https://mwkrwffquzzsvssigsup.supabase.co/storage/v1/object/public/media-service/Screenshot%20from%202026-01-07%2003-03-37.png)

## 🔧 Jenkins Integration

The pipeline (`Jenkinsfile`) interacts with Nexus at multiple stages:

### Stage 1: Dependency Resolution
- **Frontend**: Configures NPM to use the Nexus mirror.
  ```groovy
  sh 'npm config set registry http://vps-77043236.vps.ovh.ca:7071/repository/buy02-group/'
  ```
- **Backend**: Injects a custom `settings.xml` using the **Config File Provider** plugin to authenticate Maven requests.
  ```groovy
  configFileProvider([configFile(fileId: '...', variable: 'MAVEN_SETTINGS')]) {
      sh "mvn ... -s $MAVEN_SETTINGS"
  }
  ```

### Stage 3: Artifact Push
- Authenticates with Docker Registry using Jenkins credentials (`nexus-creds`).
- Pushes built images to the private Docker registry.

### Stage 4: Deployment
- Pulls the specific build version from the private Docker registry for deployment.

## ⚙️ Local Development Setup

To speed up your local builds, you can configure your local environment to use Nexus.

### Maven (`~/.m2/settings.xml`)
Add the Nexus server to your local Maven settings to pull dependencies through the proxy:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>http://vps-77043236.vps.ovh.ca:7071/repository/buy02-group/</url>
    </mirror>
  </mirrors>
</settings>
```

### Docker
To pull images locally:
```bash
docker login vps-77043236.vps.ovh.ca:8089
# Username/Password provided by admin
```

## 🔍 Troubleshooting

**Issue: "401 Unauthorized" during build**
- Check if Jenkins credentials (`nexus-creds`) are up to date.
- Verify that the `settings.xml` file ID in `Jenkinsfile` matches the one in Jenkins "Managed Files".

**Issue: "Connection Refused"**
- Ensure the Nexus VPS is reachable.
- Check firewall rules for ports 7071 and 8089.
