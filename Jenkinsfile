pipeline {
  agent any
  stages {
    stage('Build Web App') {
      steps {
        parallel(
          "Build Web App": {
            sh '''# WebApp WAR
./gradlew clean build war;'''
            fileExists 'welcome.war'
            
          },
          "Build Alerts Module": {
            dir(path: 'alerts') {
              sh './gradlew clean build fatJar;'
              fileExists 'alerts-all-1.0.jar'
            }
            
            
          },
          "Build Follow Up Module": {
            dir(path: 'followup') {
              sh './gradlew clean build fatJar;'
              fileExists 'followup-all-1.0.jar'
            }
            
            
          }
        )
      }
    }
  }
  environment {
    WELCOME_APP = 'fit_welcome_test'
  }
}