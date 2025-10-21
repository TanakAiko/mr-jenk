pipeline {
    agent any

    // Best practice: Define environment variables for the pipeline
    // Jenkins can inject credentials securely into these variables
    environment {
        CONFIG_REPO_URI = 'https://github.com/mamadbah2/config-buy-01.git'
        
        // Rollback environment variables
        LAST_SUCCESSFUL_TAG = ''      // Will store the tag of the last successful deployment
        CURRENT_BUILD_TAG = ''        // Will store the current build's tag
        ROLLBACK_FILE = '.last_successful_build' // File to persist last successful build info
    }

    stages {
        // Stage 0: Load Last Successful Build Info
        stage('Load Rollback Info') {
            steps {
                script {
                    echo '📋 Loading last successful build information...'
                    
                    // Try to read the last successful build tag from file
                    if (fileExists(env.ROLLBACK_FILE)) {
                        def rollbackInfo = readFile(env.ROLLBACK_FILE).trim()
                        if (rollbackInfo) {
                            env.LAST_SUCCESSFUL_TAG = rollbackInfo
                            echo "✅ Found last successful build: ${env.LAST_SUCCESSFUL_TAG}"
                        } else {
                            echo "⚠️ Rollback file exists but is empty. This might be the first build."
                        }
                    } else {
                        echo "ℹ️ No rollback file found. This is the first deployment."
                    }
                    
                    // Set current build tag with fallback for GIT_COMMIT
                    def gitCommit = env.GIT_COMMIT ?: sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    env.CURRENT_BUILD_TAG = "build-${env.BUILD_NUMBER}-${gitCommit.take(7)}"
                    echo "🏗️ Current build tag: ${env.CURRENT_BUILD_TAG}"
                }
            }
        }

        // Stage 1: Run tests in parallel for maximum speed
        stage('Test') {
            parallel {
                stage('Test Frontend (Angular)') {
                    steps { dir('buy-01-frontend') { 
                        sh 'npm install'
                        sh 'npm run test:ci' 
                    } }
                }

                stage('Test API Gateway') {
                    steps { dir('api-gateway') { sh 'mvn test' } }
                }

                stage('Test Config Service') {
                    steps { dir('config-service') { sh 'mvn test' } }
                }

                stage('Test Discovery Service') {
                    steps { dir('discovery-service') { sh 'mvn test' } }
                }

                stage('Test Media Service') {
                    steps { dir('media-service') { sh 'mvn test' } }
                }

                stage('Test Product Service') {
                    steps { dir('product-service') { sh 'mvn test' } }
                }

                stage('Test User Service') {
                    steps { dir('user-service') { sh 'mvn test' } }
                }
            }
        }

        // Stage 2: Build all the Docker images using docker-compose 🐳
        // This command builds the images but does not run them.
        stage('Build Docker Images') {
            steps {
                echo 'Building all Docker images from their Dockerfiles...'
                // Pass the credentials to the docker-compose command
                withCredentials([usernamePassword(credentialsId: 'github', usernameVariable: 'CONFIG_REPO_USERNAME', passwordVariable: 'CONFIG_REPO_PASSWORD')]) {
                    sh '''
                        export CONFIG_REPO_URI=${CONFIG_REPO_URI}
                        export CONFIG_REPO_USERNAME=${CONFIG_REPO_USERNAME}
                        export CONFIG_REPO_PASSWORD=${CONFIG_REPO_PASSWORD}
                        docker-compose build --parallel
                    '''
                }
            }
        }

        // Stage 3: Push Docker Images with Current Build Tag
        stage('Push Docker Images') {
            steps {
                echo 'Pushing Docker images to Docker Hub...'
                script {
                    def services = ['api-gateway', 'config-service', 'discovery-service', 'media-service', 'product-service', 'user-service', 'buy-01-frontend']

                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                        sh "echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin"

                        services.each { service ->
                            def imageTag = "${DOCKERHUB_USERNAME}/${service}:${env.CURRENT_BUILD_TAG}"
                            sh "docker tag ${service}:latest ${imageTag}"
                            
                            // Retry push up to 3 times with exponential backoff
                            retry(3) {
                                try {
                                    echo "Pushing ${imageTag}..."
                                    sh "docker push ${imageTag}"
                                    echo "✅ Successfully pushed ${imageTag}"
                                } catch (Exception e) {
                                    echo "⚠️ Failed to push ${imageTag}. Retrying..."
                                    sleep(time: 10, unit: 'SECONDS')
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }

        

        // Stage 4: Deploy the entire application using Docker Compose 🚀
        stage('Deploy Application') {
            steps {
                echo 'Deploying the application stack from Docker Hub images...'
                script {
                    def services = ['api-gateway', 'config-service', 'discovery-service', 'media-service', 'product-service', 'user-service', 'buy-01-frontend']

                    withCredentials([
                        usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD'),
                        usernamePassword(credentialsId: 'github', usernameVariable: 'CONFIG_REPO_USERNAME', passwordVariable: 'CONFIG_REPO_PASSWORD')
                    ]) {
                        sh "echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin"

                        // Pull all the images from Docker Hub using CURRENT_BUILD_TAG
                        echo 'Pulling Docker images from Docker Hub...'
                        services.each { service ->
                            def imageTag = "${DOCKERHUB_USERNAME}/${service}:${env.CURRENT_BUILD_TAG}"
                            sh "docker pull ${imageTag}"
                            // Re-tag the pulled image as latest for docker-compose to use
                            sh "docker tag ${imageTag} ${service}:latest"
                        }

                        // Deploy using docker-compose
                        echo 'Stopping any existing containers...'
                        sh '''
                            # Stop and remove existing containers to free up ports
                            docker-compose down || true
                        '''
                        
                        echo 'Starting all services with docker-compose...'
                        // Pass credentials securely as environment variables
                        withEnv([
                            "CONFIG_REPO_URI=${env.CONFIG_REPO_URI}",
                            "CONFIG_REPO_USERNAME=${CONFIG_REPO_USERNAME}",
                            "CONFIG_REPO_PASSWORD=${CONFIG_REPO_PASSWORD}"
                        ]) {
                            sh '''
                                # Bring up the services in detached mode.
                                # --no-build: Do not build images, use existing/pulled images
                                # --force-recreate: Ensures all containers are replaced with new ones, applying all changes.
                                # --remove-orphans: Cleans up containers for services that are no longer defined in the compose file.
                                docker-compose up -d --no-build --force-recreate --remove-orphans
                            '''
                        }
                    }
                }
            }
        }
        
        // Stage 5: Save Successful Build Reference
        stage('Save Build Reference') {
            steps {
                script {
                    echo '💾 Saving current build as last successful deployment...'
                    
                    // Ensure CURRENT_BUILD_TAG is set (safety check)
                    if (!env.CURRENT_BUILD_TAG || env.CURRENT_BUILD_TAG == '') {
                        def gitCommit = env.GIT_COMMIT ?: sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                        currentTag = "build-${env.BUILD_NUMBER}-${gitCommit.take(7)}"
                        env.CURRENT_BUILD_TAG = currentTag
                        echo "DEBUG: env.CURRENT_BUILD_TAG=${env.CURRENT_BUILD_TAG ?: 'null'}"
                        echo "⚠️ CURRENT_BUILD_TAG was not set, generated: ${currentTag}"
                    }
                    
                    // Create the rollback file if it doesn't exist
                    if (!fileExists(env.ROLLBACK_FILE)) {
                        echo "📝 Creating rollback file: ${env.ROLLBACK_FILE}"
                        sh "touch ${env.ROLLBACK_FILE}"
                    }
                    
                    // Save the current build tag to file for future rollbacks
                    writeFile file: env.ROLLBACK_FILE, text: currentTag
                    
                    echo "✅ Build reference saved: ${env.CURRENT_BUILD_TAG}"
                    echo "📌 This version will be used for rollback if next deployment fails"
                    
                    // Also archive the file as a build artifact
                    archiveArtifacts artifacts: env.ROLLBACK_FILE, fingerprint: true
                }
            }
        }
    }

    // The post block runs after all stages are completed
    post {
        always {
            echo 'Pipeline completed!'
            echo 'Application is running. Access it at:'
            echo '  - Frontend: http://localhost:4200'
            echo '  - API Gateway: http://localhost:8090'
            echo '  - Eureka Dashboard: http://localhost:8761'
            
            // Optional: Clean up only dangling/unused images to save disk space
            // but keep the running containers and their images
            sh """
                docker image prune -f
            """
        }
        
        success {
            script {
                echo '✅ Build completed successfully!'
                echo "📦 Current deployment: ${env.CURRENT_BUILD_TAG}"
                if (env.LAST_SUCCESSFUL_TAG) {
                    echo "📜 Previous deployment: ${env.LAST_SUCCESSFUL_TAG}"
                }
            }
            
            // Send success email notification
            emailext (
                subject: "✅ Jenkins Build SUCCESS: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                <div style="background-color: #4CAF50; color: white; padding: 15px; border-radius: 5px 5px 0 0;">
                                    <h2 style="margin: 0;">✅ Build Successful</h2>
                                </div>
                                <div style="padding: 20px; background-color: #f9f9f9;">
                                    <h3>Build Information</h3>
                                    <table style="width: 100%; border-collapse: collapse;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Project:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${env.JOB_NAME}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Build Number:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">#${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Status:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd; color: #4CAF50;"><strong>SUCCESS</strong></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Duration:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Deployment Tag:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><code>${env.CURRENT_BUILD_TAG}</code></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Commit:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${env.GIT_COMMIT?.take(7) ?: 'N/A'}</td>
                                        </tr>
                                    </table>
                                    
                                    <h3 style="margin-top: 20px;">Application Access</h3>
                                    <ul style="list-style-type: none; padding: 0;">
                                        <li style="padding: 5px 0;">🌐 <strong>Frontend:</strong> <a href="http://localhost:4200">http://localhost:4200</a></li>
                                        <li style="padding: 5px 0;">🚪 <strong>API Gateway:</strong> <a href="http://localhost:8090">http://localhost:8090</a></li>
                                        <li style="padding: 5px 0;">🔍 <strong>Eureka Dashboard:</strong> <a href="http://localhost:8761">http://localhost:8761</a></li>
                                        <li style="padding: 5px 0;">⚙️ <strong>Config Service:</strong> <a href="http://localhost:8888">http://localhost:8888</a></li>
                                    </ul>
                                    
                                    <div style="margin-top: 20px; padding: 15px; background-color: #e7f3e7; border-left: 4px solid #4CAF50; border-radius: 3px;">
                                        <p style="margin: 0;"><strong>All services deployed successfully!</strong></p>
                                        <p style="margin: 5px 0 0 0;">The application is now running and ready for testing.</p>
                                    </div>
                                    
                                    <div style="margin-top: 20px; text-align: center;">
                                        <a href="${env.BUILD_URL}" style="display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px;">View Build Details</a>
                                    </div>
                                </div>
                                <div style="padding: 15px; background-color: #f1f1f1; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 5px 5px;">
                                    <p style="margin: 0;">Jenkins CI/CD Pipeline - Buy-01 E-Commerce Platform</p>
                                </div>
                            </div>
                        </body>
                    </html>
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html',
                attachLog: false
            )
        }
        
        failure {
            echo '❌ Build failed. Initiating automatic rollback...'
            
            script {
                // Attempt automatic rollback if we have a previous successful build
                if (env.LAST_SUCCESSFUL_TAG && env.LAST_SUCCESSFUL_TAG != '' && env.LAST_SUCCESSFUL_TAG != 'null') {
                    echo "🔄 Rolling back to last successful build: ${env.LAST_SUCCESSFUL_TAG}"
                    
                    try {
                        def services = ['api-gateway', 'config-service', 'discovery-service', 'media-service', 'product-service', 'user-service', 'buy-01-frontend']
                        
                        withCredentials([
                            usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD'),
                            usernamePassword(credentialsId: 'github', usernameVariable: 'CONFIG_REPO_USERNAME', passwordVariable: 'CONFIG_REPO_PASSWORD')
                        ]) {
                            sh "echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin"
                            
                            echo "📥 Pulling last successful images: ${env.LAST_SUCCESSFUL_TAG}"
                            services.each { service ->
                                def imageTag = "${DOCKERHUB_USERNAME}/${service}:${env.LAST_SUCCESSFUL_TAG}"
                                sh "docker pull ${imageTag} || true"
                                sh "docker tag ${imageTag} ${service}:latest || true"
                            }
                            
                            echo "🛑 Stopping failed deployment..."
                            sh 'docker-compose down || true'
                            
                            echo "🚀 Redeploying last successful version..."
                            withEnv([
                                "CONFIG_REPO_URI=${env.CONFIG_REPO_URI}",
                                "CONFIG_REPO_USERNAME=${CONFIG_REPO_USERNAME}",
                                "CONFIG_REPO_PASSWORD=${CONFIG_REPO_PASSWORD}"
                            ]) {
                                sh 'docker-compose up -d --no-build --force-recreate --remove-orphans || true'
                            }
                            
                            echo "✅ Rollback completed! Reverted to: ${env.LAST_SUCCESSFUL_TAG}"
                            echo "⚠️ Current failed build: ${env.CURRENT_BUILD_TAG}"
                        }
                    } catch (Exception e) {
                        echo "❌ Automatic rollback failed: ${e.message}"
                        echo "⚠️ Manual intervention required!"
                        sh 'docker-compose down || true'
                    }
                } else {
                    echo '⚠️ No previous successful build found. Cannot rollback.'
                    echo '🛑 Cleaning up failed deployment...'
                    sh 'docker-compose down || true'
                }
            }
            
            // Send failure email notification
            emailext (
                subject: "❌ Jenkins Build FAILED: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                <div style="background-color: #f44336; color: white; padding: 15px; border-radius: 5px 5px 0 0;">
                                    <h2 style="margin: 0;">❌ Build Failed</h2>
                                </div>
                                <div style="padding: 20px; background-color: #f9f9f9;">
                                    <h3>Build Information</h3>
                                    <table style="width: 100%; border-collapse: collapse;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Project:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${env.JOB_NAME}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Build Number:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">#${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Status:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd; color: #f44336;"><strong>FAILURE</strong></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Duration:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Failed Build Tag:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><code>${env.CURRENT_BUILD_TAG}</code></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Rolled Back To:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><code>${env.LAST_SUCCESSFUL_TAG ?: 'N/A - No previous build'}</code></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Commit:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${env.GIT_COMMIT?.take(7) ?: 'N/A'}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin-top: 20px; padding: 15px; background-color: #ffebee; border-left: 4px solid #f44336; border-radius: 3px;">
                                        <p style="margin: 0;"><strong>⚠️ Action Required</strong></p>
                                        <p style="margin: 5px 0 0 0;">The build has failed. ${env.LAST_SUCCESSFUL_TAG ? 'System automatically rolled back to the previous successful version.' : 'No rollback available - this was the first deployment.'}</p>
                                    </div>
                                    
                                    <h3 style="margin-top: 20px;">Troubleshooting Steps</h3>
                                    <ol style="padding-left: 20px;">
                                        <li>Check the build console output for error messages</li>
                                        <li>Review recent code changes</li>
                                        <li>Verify all dependencies are available</li>
                                        <li>Check Docker and container logs</li>
                                        <li>Ensure MongoDB and Supabase connections are working</li>
                                    </ol>
                                    
                                    <div style="margin-top: 20px; text-align: center;">
                                        <a href="${env.BUILD_URL}console" style="display: inline-block; padding: 10px 20px; background-color: #f44336; color: white; text-decoration: none; border-radius: 5px;">View Console Output</a>
                                    </div>
                                </div>
                                <div style="padding: 15px; background-color: #f1f1f1; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 5px 5px;">
                                    <p style="margin: 0;">Jenkins CI/CD Pipeline - Buy-01 E-Commerce Platform</p>
                                </div>
                            </div>
                        </body>
                    </html>
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html',
                attachLog: true
            )
        }
        
        unstable {
            echo '⚠️ Build unstable.'
            
            // Send unstable email notification
            emailext (
                subject: "⚠️ Jenkins Build UNSTABLE: ${env.JOB_NAME} - Build #${env.BUILD_NUMBER}",
                body: """
                    <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;">
                                <div style="background-color: #ff9800; color: white; padding: 15px; border-radius: 5px 5px 0 0;">
                                    <h2 style="margin: 0;">⚠️ Build Unstable</h2>
                                </div>
                                <div style="padding: 20px; background-color: #f9f9f9;">
                                    <h3>Build Information</h3>
                                    <table style="width: 100%; border-collapse: collapse;">
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Project:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${env.JOB_NAME}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Build Number:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">#${env.BUILD_NUMBER}</td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Status:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd; color: #ff9800;"><strong>UNSTABLE</strong></td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;"><strong>Duration:</strong></td>
                                            <td style="padding: 8px; border-bottom: 1px solid #ddd;">${currentBuild.durationString.replace(' and counting', '')}</td>
                                        </tr>
                                    </table>
                                    
                                    <div style="margin-top: 20px; padding: 15px; background-color: #fff3e0; border-left: 4px solid #ff9800; border-radius: 3px;">
                                        <p style="margin: 0;"><strong>⚠️ Build completed with warnings</strong></p>
                                        <p style="margin: 5px 0 0 0;">Some tests may have failed or there are quality issues to address.</p>
                                    </div>
                                    
                                    <div style="margin-top: 20px; text-align: center;">
                                        <a href="${env.BUILD_URL}" style="display: inline-block; padding: 10px 20px; background-color: #ff9800; color: white; text-decoration: none; border-radius: 5px;">View Build Details</a>
                                    </div>
                                </div>
                                <div style="padding: 15px; background-color: #f1f1f1; text-align: center; font-size: 12px; color: #666; border-radius: 0 0 5px 5px;">
                                    <p style="margin: 0;">Jenkins CI/CD Pipeline - Buy-01 E-Commerce Platform</p>
                                </div>
                            </div>
                        </body>
                    </html>
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html',
                attachLog: true
            )
        }
    }
}
