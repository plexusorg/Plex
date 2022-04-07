pipeline {
    agent any
    stages {
        stage("build") {
            steps {
                withGradle {
                    sh "./gradlew build javadoc --no-daemon"
                }
            }
        }
        stage("publish") {
            when {
                branch "master"
            }
            steps {
            withCredentials([usernamePassword(credentialsId: '85b7099f-2e65-461e-b2ce-124edec41ed6', passwordVariable: 'plexPassword', usernameVariable: 'plexUser')]) {
                withGradle {
                    sh "./gradlew publish --no-daemon"
                    }
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: "build/libs/*.jar", fingerprint: true
            javadoc javadocDir: 'build/docs/javadoc', keepAll: false
            discordSend description: '**Build:** ${BUILD_NUMBER}\n**Status:**: ${currentBuild.currentResult}', enableArtifactsList: true, footer: 'Built with Jenkins', link: env.BUILD_URL, result: currentBuild.currentResult, scmWebUrl: 'https://github.com/plexusorg/Plex', showChangeset: true, title: env.JOB_NAME, webhookURL: env.PLEX_WEBHOOK_URL
            cleanWs()
        }
    }
}