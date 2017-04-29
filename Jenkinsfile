pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh '''# WebApp WAR
./gradlew clean build war;'''
        sh '''# Follow Up Module
cd followup;
./gradlew clean build fatJar;
cd ..;'''
        sh '''# Alerts Module
cd alerts;
./gradlew clean build fatJar;
cd ..;'''
      }
    }
  }
  environment {
    WELCOME_APP = 'fit_welcome_test'
  }
}