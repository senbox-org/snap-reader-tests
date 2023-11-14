#!/bin/bash
set -e

export 
export MAVEN_SETTINGS_FILE=$1
export SETTINGS_FILE="/var/maven/.m2/settings.xml"

if [ -z ${MAVEN_SETTINGS_FILE} ];
    then
        SETTINGS_FILE="/var/maven/.m2/settings.xml"
    else
        SETTINGS_FILE=$MAVEN_SETTINGS_FILE
fi

# Run maven to download jar containing libraries
mvn -s $SETTINGS_FILE -Duser.home=/var/maven -Dsnap.userdir=/home/snap install package \
    -DskipTests=true --no-transfer-progress --batch-mode --errors --fail-at-end --show-version -DdeployAtEnd=false

export WORKSPACE_PATH=`pwd`
echo $WORKSPACE_PATH

# Set up OpenJPEG
echo 'ls /var/tmp/repository/org/esa/snap/lib-openjpeg/'
ls /var/tmp/repository/org/esa/snap/lib-openjpeg/
export OPENJPEG_VERSION=`ls /var/tmp/repository/org/esa/snap/lib-openjpeg/ | grep SNAPSHOT`
cd /var/tmp/repository/org/esa/snap/lib-openjpeg/${OPENJPEG_VERSION}
unzip lib-openjpeg-${OPENJPEG_VERSION}.jar
mkdir -p /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
cp -r auxdata/openjpeg/* /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
chmod 755 -R /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}/*/bin

# Set up GDAL
echo 'ls /var/tmp/repository/org/esa/s2tbx/lib-gdal/'
ls /var/tmp/repository/org/esa/s2tbx/lib-gdal/
export GDAL_VERSION=`ls /var/tmp/repository/org/esa/s2tbx/lib-gdal/`
cd /var/tmp/repository/org/esa/s2tbx/lib-gdal/${GDAL_VERSION}
unzip lib-gdal-${GDAL_VERSION}.jar
mkdir -p /home/snap/auxdata/gdal
cp auxdata/gdal/Linux/x64/gdal-*-*-1.zip /home/snap/auxdata/gdal
cd /home/snap/auxdata/gdal/
unzip *.zip
chmod 755 -R bin/*

# Update LD_LIBRARY_PATH Library
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib/:/lib/x86_64-linux-gnu/:/usr/lib/:/home/snap/.snap/auxdata/gdal/:/home/snap/.snap/auxdata/gdal/lib/:/home/snap/.snap/auxdata/gdal/lib/jni/

cd ${WORKSPACE_PATH}
pwd