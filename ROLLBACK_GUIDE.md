# 🔄 Automatic Rollback Strategy Guide

## Overview

This Jenkins pipeline implements an **automatic rollback system** using **environment variables** and **persistent file storage** to track successful deployments and automatically revert to the last known good version on failure.

---

## 🎯 How It Works

### Environment Variables

```groovy
LAST_SUCCESSFUL_TAG    // Tag of the last successful deployment (e.g., "build-42-54818bf")
CURRENT_BUILD_TAG      // Tag of the current build being deployed
ROLLBACK_FILE          // File path: ".last_successful_build"
```

### Build Tag Format

```
build-{BUILD_NUMBER}-{GIT_COMMIT_SHORT}

Examples:
- build-42-54818bf
- build-43-abc1234
- build-44-def5678
```

---

## 📋 Deployment Workflow

### Stage 0: Load Rollback Info
```
1. Check if .last_successful_build file exists
2. Read the last successful build tag
3. Set LAST_SUCCESSFUL_TAG environment variable
4. Generate CURRENT_BUILD_TAG for this build
```

### Stage 1-4: Normal Build Process
```
Test → Build → Push → Deploy
```

### Stage 5: Save Build Reference
```
✅ If deployment successful:
   - Save CURRENT_BUILD_TAG to .last_successful_build
   - Archive file as build artifact
   - This becomes the rollback point for next build
```

### On Failure: Automatic Rollback
```
❌ If any stage fails:
   1. Read LAST_SUCCESSFUL_TAG
   2. Pull all images with that tag from Docker Hub
   3. Re-tag as latest
   4. Stop current deployment
   5. Deploy previous version
   6. Send email with rollback details
```

---

## 🚀 Example Scenario

### Successful Deployment Flow

```
Build #42 (commit: 54818bf)
├─ Stage 0: Load Rollback Info
│  └─ No previous build found (first deployment)
├─ Stages 1-4: Test → Build → Push → Deploy
│  └─ ✅ All stages successful
└─ Stage 5: Save Build Reference
   └─ Save "build-42-54818bf" to .last_successful_build

Build #43 (commit: abc1234)
├─ Stage 0: Load Rollback Info
│  ├─ LAST_SUCCESSFUL_TAG = "build-42-54818bf"
│  └─ CURRENT_BUILD_TAG = "build-43-abc1234"
├─ Stages 1-4: Test → Build → Push → Deploy
│  └─ ✅ All stages successful
└─ Stage 5: Save Build Reference
   └─ Save "build-43-abc1234" to .last_successful_build
```

### Failed Deployment with Automatic Rollback

```
Build #44 (commit: def5678)
├─ Stage 0: Load Rollback Info
│  ├─ LAST_SUCCESSFUL_TAG = "build-43-abc1234"
│  └─ CURRENT_BUILD_TAG = "build-44-def5678"
├─ Stages 1-3: Test → Build → Push
│  └─ ✅ Successful
├─ Stage 4: Deploy
│  └─ ❌ FAILED! (e.g., Docker Hub 500 error)
└─ Post: Failure Block
   ├─ Detect LAST_SUCCESSFUL_TAG exists
   ├─ Pull images tagged "build-43-abc1234"
   ├─ Stop failed deployment
   ├─ Deploy "build-43-abc1234"
   └─ ✅ Rollback complete
   
Result: Application running on build-43-abc1234 (last successful)
```

---

## 📊 File Storage

### .last_successful_build File

**Location:** Jenkins workspace root  
**Format:** Single line with build tag  
**Example Content:**
```
build-43-abc1234
```

**Persistence:**
- Stored in workspace (survives across builds)
- Archived as build artifact (downloadable)
- Used for rollback reference

---

## 🔍 Checking Current Deployment

### Via Jenkins Console Output

Look for these messages:
```bash
✅ Build completed successfully!
📦 Current deployment: build-43-abc1234
📜 Previous deployment: build-42-54818bf
```

### Via Command Line

```bash
# On Jenkins server
cd /var/lib/jenkins/workspace/your-job

# Check last successful build
cat .last_successful_build
# Output: build-43-abc1234

# Check running containers
docker ps --format "table {{.Names}}\t{{.Image}}"

# Check specific service version
docker inspect config-service | grep -oP '(?<=tanakaiko/config-service:)[^"]+' | head -1
```

### Via Docker Hub

```bash
# List all available tags
curl -s "https://hub.docker.com/v2/repositories/tanakaiko/config-service/tags" | jq -r '.results[].name'

# Output:
# build-44-def5678
# build-43-abc1234
# build-42-54818bf
# latest
```

---

## 🛠️ Manual Rollback

If you need to manually rollback to a specific version:

### Option 1: Via Jenkins (Rebuild)

```bash
1. Go to Jenkins → Your Job → Build History
2. Find the successful build you want to rollback to
3. Click "Replay" or "Rebuild"
4. The pipeline will use the same code and redeploy
```

### Option 2: Via Command Line

```bash
# 1. Set the target build tag
TARGET_BUILD="build-42-54818bf"

# 2. Login to Docker Hub
echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin

# 3. Pull all service images with that tag
for service in api-gateway config-service discovery-service media-service product-service user-service buy-01-frontend; do
    echo "Pulling $service:$TARGET_BUILD"
    docker pull tanakaiko/$service:$TARGET_BUILD
    docker tag tanakaiko/$service:$TARGET_BUILD $service:latest
done

# 4. Redeploy
docker compose down
docker compose up -d --no-build --force-recreate --remove-orphans

# 5. Update the rollback file
echo "$TARGET_BUILD" > .last_successful_build
```

### Option 3: Edit Rollback File

```bash
# Manually set the rollback target
echo "build-42-54818bf" > .last_successful_build

# Trigger a new build that will fail (to test rollback)
# Or use this as reference for next deployment
```

---

## 📧 Email Notifications

### Success Email Includes:
```
✅ Build Successful
- Deployment Tag: build-43-abc1234
- Previous Tag: build-42-54818bf
- All service links
```

### Failure Email Includes:
```
❌ Build Failed
- Failed Build Tag: build-44-def5678
- Rolled Back To: build-43-abc1234
- Rollback status
- Troubleshooting steps
```

---

## 🎯 Best Practices

### 1. **Monitor Build History**
```bash
# Keep track of recent successful builds
cat .last_successful_build
git log --oneline -5
```

### 2. **Tag Management in Docker Hub**
```bash
# Keep at least the last 10 successful builds
# Delete old tags periodically to save space

# List all tags older than 30 days (manual cleanup)
# Use Docker Hub UI or API to manage tags
```

### 3. **Testing Rollback**
```bash
# Periodically test the rollback mechanism
# Simulate a failure to ensure rollback works

# Create a test branch that intentionally fails
git checkout -b test-rollback
# Make a breaking change
git push
# Trigger build
# Verify automatic rollback occurs
```

### 4. **Backup Rollback File**
```bash
# Jenkins automatically archives this, but you can also:
cp .last_successful_build .last_successful_build.backup

# Or track in git (optional)
git add .last_successful_build
git commit -m "Update last successful build reference"
```

---

## 🚨 Troubleshooting

### Problem: "No previous successful build found"

**Cause:** First deployment or rollback file missing

**Solution:**
```bash
# This is expected on first deployment
# After first successful build, file will be created
# You can manually create it for testing:
echo "build-42-54818bf" > .last_successful_build
```

### Problem: Rollback fails - images not found

**Cause:** Docker Hub images were deleted or never pushed

**Solution:**
```bash
# Check Docker Hub for available tags
docker search tanakaiko/config-service

# Manually push a known good version
docker pull config-service:latest  # if you have it locally
docker tag config-service:latest tanakaiko/config-service:build-42-54818bf
docker push tanakaiko/config-service:build-42-54818bf
```

### Problem: Rollback file corrupted

**Cause:** Manual editing or file system issues

**Solution:**
```bash
# Check file contents
cat .last_successful_build

# If corrupted, restore from Jenkins build artifacts
# Or manually create from known good build
echo "build-42-54818bf" > .last_successful_build
```

### Problem: Multiple builds running simultaneously

**Cause:** Concurrent builds can overwrite rollback file

**Solution:**
```groovy
// Add to Jenkinsfile (already implemented):
options {
    disableConcurrentBuilds()
}
```

---

## 🔐 Security Considerations

### 1. **File Permissions**
```bash
# Ensure rollback file is readable by Jenkins
chmod 644 .last_successful_build
chown jenkins:jenkins .last_successful_build
```

### 2. **Prevent Tampering**
```bash
# Rollback file should be in .gitignore (already is)
# Only Jenkins should modify it
# Archive as artifact for audit trail
```

### 3. **Access Control**
```bash
# Only authorized users can trigger builds
# Rollback happens automatically - no manual intervention
# All rollback events logged in Jenkins
```

---

## 📈 Monitoring & Metrics

### Track These Metrics:

```bash
# 1. Rollback frequency
grep "Rollback completed" jenkins-logs.txt | wc -l

# 2. Success rate
total_builds=$(ls -1 builds/ | wc -l)
successful_builds=$(grep "Build completed successfully" */console.txt | wc -l)
echo "Success rate: $((successful_builds * 100 / total_builds))%"

# 3. Average rollback time
# Check email timestamps for failure → rollback completion

# 4. Most common failure points
grep "stage failed" */console.txt | cut -d':' -f2 | sort | uniq -c | sort -nr
```

---

## 🎓 Advanced Usage

### Custom Build Tags

If you want to use custom tags instead of `build-{number}-{commit}`:

```groovy
// In Jenkinsfile environment block
env.CURRENT_BUILD_TAG = "v1.2.${env.BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
// Result: v1.2.43-abc1234
```

### Multiple Rollback Points

To keep multiple rollback points:

```bash
# Save last 3 successful builds
echo "build-43-abc1234" > .last_successful_build
echo "build-42-54818bf" > .last_successful_build.1
echo "build-41-xyz9876" > .last_successful_build.2
```

### Conditional Rollback

Rollback only for specific failure types:

```groovy
if (currentBuild.result == 'FAILURE' && currentStage == 'Deploy') {
    // Rollback only for deployment failures
}
```

---

## 📞 Quick Reference Commands

### Check Status
```bash
cat .last_successful_build
docker ps
docker compose logs --tail=50
```

### Manual Rollback
```bash
TARGET="build-42-54818bf"
docker pull tanakaiko/config-service:$TARGET
docker compose down && docker compose up -d
```

### Emergency Stop
```bash
docker compose down
```

### View All Available Versions
```bash
docker images | grep tanakaiko
```

---

## ✅ Summary

| Feature | Status | Description |
|---------|--------|-------------|
| **Auto Rollback** | ✅ Enabled | Automatic on any build failure |
| **Version Tracking** | ✅ Enabled | Persistent file + environment variables |
| **Build Tagging** | ✅ Enabled | Format: build-{number}-{commit} |
| **Email Alerts** | ✅ Enabled | Includes rollback status |
| **Artifact Archive** | ✅ Enabled | Rollback file saved per build |
| **Manual Rollback** | ✅ Supported | Via CLI or Jenkins rebuild |

---

**Last Updated:** October 20, 2025  
**Version:** 2.0  
**Maintained By:** DevOps Team
