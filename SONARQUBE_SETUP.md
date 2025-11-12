# SonarQube Integration Setup Guide

## 📋 Overview

This guide will help you set up SonarQube code quality analysis for the Buy-01 E-Commerce Platform Jenkins pipeline.

## 🐳 Running SonarQube with Docker

### Step 1: Pull and Run SonarQube

```bash
# Pull the latest SonarQube image
docker pull sonarqube:latest

# Run SonarQube container
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_extensions:/opt/sonarqube/extensions \
  -v sonarqube_logs:/opt/sonarqube/logs \
  sonarqube:latest

# Check if SonarQube is running
docker ps | grep sonarqube
```

### Step 2: Access SonarQube

1. Open your browser and navigate to: `http://localhost:9000`
2. Default credentials:
   - **Username**: `admin`
   - **Password**: `admin`
3. You'll be prompted to change the password on first login

## 🔧 Jenkins Configuration

### Step 1: Install SonarQube Scanner Plugin

1. Go to **Jenkins Dashboard** → **Manage Jenkins** → **Manage Plugins**
2. Click on the **Available** tab
3. Search for "SonarQube Scanner"
4. Install the following plugins:
   - ✅ **SonarQube Scanner for Jenkins**
   - ✅ **Sonar Quality Gates Plugin** (optional, for quality gate checks)
5. Restart Jenkins if required

### Step 2: Configure SonarQube Server in Jenkins

1. Go to **Manage Jenkins** → **Configure System**
2. Scroll down to **SonarQube servers** section
3. Click **Add SonarQube**
4. Configure:
   - **Name**: `SonarQube` (must match the name in Jenkinsfile)
   - **Server URL**: `http://localhost:9000` (or your SonarQube server URL)
   - **Server authentication token**: Add credentials (see Step 3)
5. Click **Save**

### Step 3: Generate SonarQube Token

1. Log in to SonarQube at `http://localhost:9000`
2. Click on your profile icon (top right) → **My Account**
3. Go to **Security** tab
4. Under **Generate Tokens**:
   - **Name**: `jenkins`
   - **Type**: `Global Analysis Token`
   - **Expires in**: `No expiration` (or set as needed)
5. Click **Generate**
6. **Copy the token** (you won't see it again!)

### Step 4: Add Token to Jenkins

1. In Jenkins, go to **Manage Jenkins** → **Manage Credentials**
2. Click on **(global)** domain
3. Click **Add Credentials**
4. Configure:
   - **Kind**: `Secret text`
   - **Scope**: `Global`
   - **Secret**: Paste the SonarQube token
   - **ID**: `sonarqube-token`
   - **Description**: `SonarQube Authentication Token`
5. Click **Create**

### Step 5: Update Jenkins SonarQube Configuration

1. Go back to **Manage Jenkins** → **Configure System**
2. In the **SonarQube servers** section
3. Under **Server authentication token**, select the credential you just created (`sonarqube-token`)
4. Click **Save**

## 🔍 Install SonarQube Scanner (Optional - Auto-installed by Pipeline)

The Jenkins pipeline automatically installs the SonarQube Scanner, but you can install it manually on the Jenkins server:

```bash
# Download SonarQube Scanner
cd /opt
wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-linux.zip

# Extract
unzip sonar-scanner-cli-5.0.1.3006-linux.zip

# Create symbolic link
ln -s /opt/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner /usr/local/bin/sonar-scanner

# Verify installation
sonar-scanner --version
```

## 📊 Configure SonarQube Analysis

The pipeline is already configured with `sonar-project.properties` which includes:

- **Project Key**: `buy-01-ecommerce`
- **Project Name**: Buy-01 E-Commerce Platform
- **Source Directories**: All microservices and frontend
- **Java Version**: 21
- **Exclusions**: node_modules, target directories, test files

## 🚦 Quality Gate Configuration

### Default Quality Gate

SonarQube comes with a default quality gate called **"Sonar way"**. You can customize it:

1. Log in to SonarQube
2. Go to **Quality Gates**
3. Create a new quality gate or modify existing one
4. Add conditions such as:
   - Code Coverage > 80%
   - Duplicated Lines < 3%
   - Maintainability Rating = A
   - Reliability Rating = A
   - Security Rating = A

### Set Quality Gate for Project

1. In SonarQube, go to **Projects**
2. Select **buy-01-ecommerce**
3. Go to **Project Settings** → **Quality Gate**
4. Select your quality gate
5. Click **Save**

## 📈 Pipeline Integration

The Jenkins pipeline now includes:

### Stage 2: SonarQube Analysis
- Automatically installs SonarQube Scanner if not available
- Runs code analysis on all services
- Sends results to SonarQube server

### Stage 2a: Quality Gate Check
- Waits for SonarQube to process the analysis
- Checks if quality gate passed
- Currently configured to **warn but continue** on failure
- Can be configured to **fail the build** (see Jenkinsfile comments)

## 🎯 Viewing Analysis Results

After running a build:

1. Go to SonarQube at `http://localhost:9000`
2. Click on **Projects**
3. Select **buy-01-ecommerce**
4. View:
   - **Overview**: Summary of code quality
   - **Issues**: Bugs, vulnerabilities, code smells
   - **Measures**: Detailed metrics
   - **Code**: Line-by-line analysis
   - **Activity**: History of analyses

## 🔧 Advanced Configuration

### Webhook for Quality Gate Status

To enable immediate quality gate feedback in Jenkins:

1. In SonarQube, go to **Administration** → **Webhooks**
2. Click **Create**
3. Configure:
   - **Name**: `Jenkins`
   - **URL**: `http://JENKINS_URL/sonarqube-webhook/`
   - **Secret**: (optional)
4. Click **Create**

### Per-Service Analysis (Optional)

If you want to analyze each service separately:

```groovy
// In each service directory
stage('SonarQube Analysis - User Service') {
    steps {
        dir('user-service') {
            withSonarQubeEnv('SonarQube') {
                sh 'mvn sonar:sonar'
            }
        }
    }
}
```

### Maven Plugin Configuration

Add to each service's `pom.xml`:

```xml
<properties>
    <sonar.projectKey>buy-01-user-service</sonar.projectKey>
    <sonar.projectName>User Service</sonar.projectName>
    <sonar.java.source>21</sonar.java.source>
    <sonar.coverage.jacoco.xmlReportPaths>target/site/jacoco/jacoco.xml</sonar.coverage.jacoco.xmlReportPaths>
</properties>
```

## 🧪 Testing the Integration

1. **Run a Jenkins build**:
   ```bash
   # Trigger a build manually or via commit
   git commit -m "Test SonarQube integration" --allow-empty
   git push
   ```

2. **Check Jenkins Console Output**:
   - Look for "SonarQube Code Quality Analysis" stage
   - Verify scanner execution
   - Check quality gate results

3. **Verify in SonarQube**:
   - Go to `http://localhost:9000`
   - Check that the project appears
   - Review the analysis results

## 📝 Metrics Explained

| Metric | Description | Good Value |
|--------|-------------|------------|
| **Bugs** | Coding errors that could break functionality | 0 |
| **Vulnerabilities** | Security issues | 0 |
| **Code Smells** | Maintainability issues | < 100 |
| **Coverage** | Percentage of code covered by tests | > 80% |
| **Duplications** | Duplicated code blocks | < 3% |
| **Technical Debt** | Estimated time to fix all issues | < 5 days |

## 🎨 SonarQube Badges (Optional)

Add SonarQube badges to your README:

```markdown
[![Quality Gate Status](http://localhost:9000/api/project_badges/measure?project=buy-01-ecommerce&metric=alert_status)](http://localhost:9000/dashboard?id=buy-01-ecommerce)
[![Bugs](http://localhost:9000/api/project_badges/measure?project=buy-01-ecommerce&metric=bugs)](http://localhost:9000/dashboard?id=buy-01-ecommerce)
[![Code Smells](http://localhost:9000/api/project_badges/measure?project=buy-01-ecommerce&metric=code_smells)](http://localhost:9000/dashboard?id=buy-01-ecommerce)
[![Coverage](http://localhost:9000/api/project_badges/measure?project=buy-01-ecommerce&metric=coverage)](http://localhost:9000/dashboard?id=buy-01-ecommerce)
```

## 🚨 Troubleshooting

### Issue: "SonarQube server not found"
**Solution**: 
- Verify SonarQube is running: `docker ps | grep sonarqube`
- Check Jenkins configuration has correct server URL
- Ensure server name matches Jenkinsfile (`SonarQube`)

### Issue: "Authentication failed"
**Solution**:
- Regenerate SonarQube token
- Update Jenkins credential with new token
- Verify credential ID is `sonarqube-token`

### Issue: "Scanner not found"
**Solution**:
- Pipeline auto-installs scanner, check Jenkins has internet access
- Manually install scanner on Jenkins server (see above)
- Add scanner to Jenkins PATH

### Issue: "Quality gate timeout"
**Solution**:
- Increase timeout in Jenkinsfile (currently 5 minutes)
- Check SonarQube server load
- Configure webhook for faster feedback

### Issue: "Analysis fails for Angular project"
**Solution**:
- Ensure `sonar.sources` points to correct directories
- Exclude `node_modules` in `sonar.exclusions`
- Consider using `sonar-scanner` instead of Maven for frontend

## 📚 Additional Resources

- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [SonarQube Scanner for Jenkins](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-jenkins/)
- [Quality Gates](https://docs.sonarqube.org/latest/user-guide/quality-gates/)
- [SonarQube Docker Image](https://hub.docker.com/_/sonarqube)

## 🎉 Success Criteria

After successful integration, you should see:

✅ SonarQube analysis runs on every Jenkins build  
✅ Code quality metrics visible in SonarQube dashboard  
✅ Quality gate status reported in Jenkins  
✅ Build continues even if quality gate fails (warning mode)  
✅ Historical trend of code quality visible  

---

**Next Steps**: Configure quality gate to fail builds, set up branch analysis, integrate with pull requests.
