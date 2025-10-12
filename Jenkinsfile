// Jenkinsfile that leverages Docker Compose for deployment
pipeline {
    agent any

    // Best practice: Define environment variables for the pipeline
    // Jenkins can inject credentials securely into these variables
    environment {
        // The URL to your configuration repository
        CONFIG_REPO_URI = 'https://github.com/mamadbah2/config-buy-01.git'
    }

    stages {
        // Stage 1: Build all source code in parallel to create JARs and JS bundles 🏗️
        // stage('Build Source Code') {
        //     parallel {
        //         stage('Build Frontend (Angular)') {
        //             when {changeset "buy-01-frontend/**" }
        //             steps {
        //                 dir('buy-01-frontend') {
        //                     sh 'echo "Building the Angular frontend..."'
        //                     sh 'npm install'
        //                     sh 'npm run build'
        //                 }
        //             }
        //         }

        //         // This builds all backend services in a single parallel step
        //         stage('API Gateway') { 
        //             when { changeset "api-gateway/**" }
        //             steps { dir('api-gateway') { sh 'mvn clean package -DskipTests' } } 
        //         }

        //         stage('Config Service') {
        //             when { changeset "config-service/**" }
        //             steps { dir('config-service') { sh 'mvn clean package -DskipTests' } } 
        //         }

        //         stage('Discovery Service') {
        //             when { changeset "discovery-service/**" }
        //             steps { dir('discovery-service') { sh 'mvn clean package -DskipTests' } }
        //         }

        //         stage('Media Service') {
        //             when { changeset "media-service/**" }
        //             steps { dir('media-service') { sh 'mvn clean package -DskipTests' } }
        //         }

        //         stage('Product Service') {
        //             when { changeset "product-service/**" }
        //             steps { dir('product-service') { sh 'mvn clean package -DskipTests' } }
        //         }

        //         stage('User Service') {
        //             when { changeset "user-service/**" }
        //             steps{ dir('user-service') { sh 'mvn clean package -DskipTests' } }
        //         }
        //     }
        // }

        // Stage 2: Run tests in parallel for maximum speed ⚡
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

        // Stage 3: Build all the Docker images using docker-compose 🐳
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

        // Stage 4: 
        stage('Push Docker Images') {
            steps {
                echo 'Pushing Docker images to Docker Hub...'
                script {
                    def commitSha = GIT_COMMIT.take(7) // Shorten the commit SHA for tagging
                    def services = ['api-gateway', 'config-service', 'discovery-service', 'media-service', 'product-service', 'user-service', 'buy-01-frontend']

                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                        sh "echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin"

                        services.each { service ->
                            def imageTag = "${DOCKERHUB_USERNAME}/${service}:local-${commitSha}"
                            sh "docker tag ${service}:latest ${imageTag}"
                            sh "docker push ${imageTag}"
                        }
                    }
                }
            }
        }

        

        // Stage 5: Deploy the entire application using Docker Compose 🚀
        stage('Deploy Application') {
            steps {
                echo 'Deploying the application stack from Docker Hub images...'
                script {
                    def commitSha = GIT_COMMIT.take(7) // Use the same commit SHA as the push stage
                    def services = ['api-gateway', 'config-service', 'discovery-service', 'media-service', 'product-service', 'user-service', 'buy-01-frontend']

                    withCredentials([
                        usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD'),
                        usernamePassword(credentialsId: 'github', usernameVariable: 'CONFIG_REPO_USERNAME', passwordVariable: 'CONFIG_REPO_PASSWORD')
                    ]) {
                        sh "echo $DOCKERHUB_PASSWORD | docker login -u $DOCKERHUB_USERNAME --password-stdin"

                        // Pull all the images from Docker Hub
                        echo 'Pulling Docker images from Docker Hub...'
                        services.each { service ->
                            def imageTag = "${DOCKERHUB_USERNAME}/${service}:local-${commitSha}"
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
        failure {
            echo 'Pipeline failed. Cleaning up...'
            sh """
                docker-compose down
            """
        }
    }
}
