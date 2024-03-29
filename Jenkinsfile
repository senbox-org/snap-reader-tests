#!/usr/bin/env groovy

/**
 * Copyright (C) 2019 CS-SI
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */


/**
 * Launch jobs in parallel for every json file listed in jsonString separated by '\n'
 */
 
/**
 * Return the email corresponding to given classpath filter
 */
def getEmailFromClassPathFilter(classPathFilter) {

    def email = ""
    if (classPathFilter == "org.esa.s1tbx") {
        email = 'luis@skywatch.com'
    } else if (classPathFilter == "com.iceye") {
        email = 'luis@skywatch.com'
    } else if (classPathFilter == "org.csa.rstb") {
        email = 'luis@skywatch.com'
    } else if (classPathFilter == "org.esa.s2tbx") {
        email = 'diana.harosa@c-s.ro'
    } else if (classPathFilter == "org.esa.s3tbx") {
        email = 'tom.block@brockmann-consult.de'
    } else if (classPathFilter == "org.esa.smos") {
        email = 'tom.block@brockmann-consult.de'
    } else {
        email = 'stb-internal@snap-ci.ovh'
    }
    echo "classPathFilter : " + classPathFilter
    echo "email : " + email
    return email
}


pipeline {
    agent { label 'snap-test' }
    options {
            buildDiscarder(logRotator(daysToKeepStr: '10', artifactDaysToKeepStr: '10'))
            timeout(time: 20, unit: 'HOURS')
    }
    parameters {
        string(name: 'classPathFilter', defaultValue: 'org.esa.s2tbx', description: 'Class path filter of the Test class to launch')
        string(name: 'dataPath', defaultValue: '/data/ssd/testData/s2tbx', description: 'Path of the data used by the reader tests')
        string(name: 'maxMemory', defaultValue: '5G', description: 'Max memory used by JVM')
    }
    stages {
        stage('Reader Tests') {
            agent {
                docker {
                    image "snap-build-server.tilaa.cloud/snap-ci:master"
                    label "snap"
                    args "-v /data/ssd/testData/:/data/ssd/testData/ -e MAVEN_CONFIG=/var/maven/.m2 -v /opt/maven/.m2/settings.xml:/var/maven/.m2/settings.xml"
                }
            }
            steps {
                echo "Launch reader tests from ${env.JOB_NAME} from ${env.GIT_BRANCH}"
                sh script:"echo ######### Launch mvn version ######### | tee -a ./readerTest-${env.BUILD_NUMBER}.log", label: "initialize log"
                sh script: "mvn versions:update-properties -Dincludes=org.esa.* | tee -a ./readerTest-${env.BUILD_NUMBER}.log", label: "add maven version to log"
                sh script: "echo ######### Launch reader tests ######### | tee -a ./readerTest-${env.BUILD_NUMBER}.log", label: "ready to launch tests"
                sh script: "sh scripts/setUpLibraries.sh", label: "prepare libraries"
                sh script: "sh scripts/launchReaderTest.sh ${params.dataPath} ${params.classPathFilter} ${env.BUILD_NUMBER} ${params.maxMemory}", label: "execute tests"
            }
            post {
                always {
                    archiveArtifacts artifacts: "readerTest-${env.BUILD_NUMBER}.log", fingerprint: true
                    sh "rm -rf readerTest-${env.BUILD_NUMBER}.log"
                    junit "target/surefire-reports/*.xml"
                }
            }
        }
    }
    post {
        failure {
            step (
                emailext(
                    subject: "[SNAP] JENKINS-NOTIFICATION: ${currentBuild.result ?: 'SUCCESS'} : Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: """Build status : ${currentBuild.result ?: 'SUCCESS'}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
Check console output at ${env.BUILD_URL}
${env.JOB_NAME} [${env.BUILD_NUMBER}]""",
                    attachLog: true,
                    compressLog: true,
                    to: getEmailFromClassPathFilter("${params.classPathFilter}")
                )
            )
        }
    }
}
