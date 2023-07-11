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
    }
    post {
        always {
            archiveArtifacts artifacts: "build/libs/*.jar", fingerprint: true
            javadoc javadocDir: "server/build/docs/javadoc", keepAll: false
            discordSend description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** ${currentBuild.currentResult}", enableArtifactsList: true, footer: "Built with Jenkins", link: env.BUILD_URL, result: currentBuild.currentResult, scmWebUrl: "https://github.com/plexusorg/Plex", showChangeset: true, title: env.JOB_NAME, webhookURL: env.WEBHOOK_URL
            cleanWs()
        }
    }
}
