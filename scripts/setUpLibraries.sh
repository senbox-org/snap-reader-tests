#! /bin/bash

# Run maven to download jar containing libraries
mvn -s /var/maven/.m2/settings.xml -Duser.home=/var/maven -Dsnap.userdir=/home/snap install deploy -DskipTests=true

export WORKSPACE_PATH=`pwd`
echo $WORKSPACE_PATH

# Set up OpenJPEG
export OPENJPEG_VERSION=`ls /var/tmp/repository/org/esa/snap/lib-openjpeg/`
cd /var/tmp/repository/org/esa/snap/lib-openjpeg/${OPENJPEG_VERSION}
unzip lib-openjpeg-${OPENJPEG_VERSION}.jar
mkdir -p /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
cp -r auxdata/openjpeg/* /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
chmod 755 -R /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}/*/bin

# Set up GDAL

export GDAL_VERSION=`ls /var/tmp/repository/org/esa/s2tbx/lib-gdal/`
cd /var/tmp/repository/org/esa/s2tbx/lib-gdal/${GDAL_VERSION}
unzip lib-gdal-${GDAL_VERSION}.jar
mkdir -p /home/snap/auxdata/gdal
cp auxdata/gdal/Linux/x64/gdal-*-*-0.zip /home/snap/auxdata/gdal
cd /home/snap/auxdata/gdal/
unzip *.zip
chmod 755 -R bin/*

# Update LD_LIBRARY_PATH Library
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib/:/lib/x86_64-linux-gnu/:/usr/lib/:/home/snap/auxdata/gdal/:/home/snap/auxdata/gdal/lib/:/home/snap/auxdata/gdal/lib/jni/

cd ${WORKSPACE_PATH}
pwd