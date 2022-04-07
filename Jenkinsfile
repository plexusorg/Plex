pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh './gradlew build javadoc publish --no-daemon'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            javadocDir 'build/docs/javadoc'
            discordSend description: "Jenkins", link: env.BUILD_URL, result: currentBuild.currentResult, title: JOB_NAME, webhookURL: env.PLEX_WEBHOOK_URL
            deleteWs()
        }
    }
}