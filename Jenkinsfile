pipeline {
    agent none  // Pas d'agent global — chaque stage déclare le sien

    options {
        timeout(time: 20, unit: 'MINUTES')   // Tuer un build bloqué
        disableConcurrentBuilds()             // Pas de double build sur le même branch
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        JAVA_VERSION   = '21'
        MAVEN_IMAGE    = 'maven:3.9-eclipse-temurin-21-alpine'
        APP_NAME       = 'securevault'
    }

    stages {

        // ──────────────────────────────────────────
        stage('Checkout') {
            agent any
            steps {
                // Jenkins fait le checkout automatiquement en "Pipeline from SCM"
                // On l'explicite quand même pour la clarté et les infos de build
                checkout scm
                script {
                    // Expose le SHA court pour le tagging futur des images Docker
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()
                    env.GIT_BRANCH_NAME = env.BRANCH_NAME ?: 'unknown'
                }
                echo "🔍 Branch : ${env.GIT_BRANCH_NAME} | Commit : ${env.GIT_COMMIT_SHORT}"
            }
        }

        // ──────────────────────────────────────────
        stage('Build & Test — Backend') {
            agent {
                docker {
                    image "${env.MAVEN_IMAGE}"
                    // Monte le cache Maven local pour éviter de tout re-télécharger
                    args '-v maven-cache:/root/.m2 --network=host'
                    reuseNode false  // Container éphémère propre à ce stage
                }
            }
            steps {
                dir('backend') {  // Adapte si ton pom.xml est à la racine
                    sh '''
                        echo "=== Java version ==="
                        java -version

                        echo "=== Maven build ==="
                        mvn clean verify \
                            --batch-mode \
                            --no-transfer-progress \
                            -Dmaven.test.failure.ignore=false
                    '''
                }
            }
            post {
                always {
                    // Publie les résultats de tests JUnit (Surefire)
                    junit(
                        testResults: 'backend/**/target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                    // Archive le JAR produit
                    archiveArtifacts(
                        artifacts: 'backend/target/*.jar',
                        allowEmptyArchive: true,
                        fingerprint: true
                    )
                }
            }
        }

        // ──────────────────────────────────────────
        stage('Rapport de build') {
            agent any
            steps {
                script {
                    def summary = """
╔══════════════════════════════════════════╗
║         SECUREVAULT — BUILD REPORT       ║
╠══════════════════════════════════════════╣
║  Branch  : ${env.GIT_BRANCH_NAME.padRight(29)}║
║  Commit  : ${env.GIT_COMMIT_SHORT.padRight(29)}║
║  Build # : ${env.BUILD_NUMBER.padRight(29)}║
║  Status  : EN COURS (voir étapes)        ║
╚══════════════════════════════════════════╝
                    """
                    echo summary
                }
            }
        }
    }

    // ──────────────────────────────────────────
    post {
        success {
            echo "✅ Build ${env.BUILD_NUMBER} réussi — ${env.APP_NAME}@${env.GIT_COMMIT_SHORT}"
        }
        failure {
            echo "❌ Build ${env.BUILD_NUMBER} en échec — consulte les logs Surefire"
        }
        unstable {
            echo "⚠️  Build instable — des tests ont échoué"
        }
        cleanup {
            // Nettoie le workspace pour ne pas saturer le disque Jenkins
            cleanWs()
        }
    }
}