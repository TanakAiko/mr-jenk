# Quick SonarQube Setup Commands

## 🚀 Quick Start

### 1. Run SonarQube Container
```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_extensions:/opt/sonarqube/extensions \
  -v sonarqube_logs:/opt/sonarqube/logs \
  sonarqube:latest
```

### 2. Access SonarQube
```
URL: http://localhost:9000
Default Login: admin / admin
```

### 3. Generate Token
1. Login → My Account → Security → Generate Token
2. Name: `jenkins`
3. Type: `Global Analysis Token`
4. Copy the token

### 4. Add to Jenkins
- Manage Jenkins → Manage Credentials → Add Secret Text
- ID: `sonarqube-token`
- Secret: Paste the token

### 5. Configure Jenkins
- Manage Jenkins → Configure System → SonarQube servers
- Name: `SonarQube`
- URL: `http://localhost:9000`
- Token: Select `sonarqube-token`

## 🔧 Useful Commands

### Check SonarQube Status
```bash
docker ps | grep sonarqube
docker logs sonarqube
```

### Stop SonarQube
```bash
docker stop sonarqube
```

### Start SonarQube
```bash
docker start sonarqube
```

### Restart SonarQube
```bash
docker restart sonarqube
```

### Remove SonarQube (keep data)
```bash
docker stop sonarqube
docker rm sonarqube
```

### Remove Everything (including data)
```bash
docker stop sonarqube
docker rm sonarqube
docker volume rm sonarqube_data sonarqube_extensions sonarqube_logs
```

## 📊 View Results

After Jenkins build completes:
```
http://localhost:9000/dashboard?id=buy-01-ecommerce
```

---

For detailed setup instructions, see [SONARQUBE_SETUP.md](./SONARQUBE_SETUP.md)
