#! /bin/bash
set -e

# Run maven to download jar containing libraries

mvn -s /var/maven/.m2/settings.xml -Duser.home=/var/maven -Dsnap.userdir=/home/snap install deploy -DskipTests=true

export WORKSPACE_PATH=`pwd`
echo $WORKSPACE_PATH

# Set up OpenJPEG
echo `ls /var/tmp/repository/org/esa/snap/lib-openjpeg/`
if [ -d "/var/tmp/repository/org/esa/snap/lib-openjpeg/8.0.4-SNAPSHOT" ] ; then
    echo 'delete old snapshot dir'
	rm -rf /var/tmp/repository/org/esa/snap/lib-openjpeg/8.0.4-SNAPSHOT
fi
echo `ls /var/tmp/repository/org/esa/snap/lib-openjpeg/`

export OPENJPEG_VERSION=`ls /var/tmp/repository/org/esa/snap/lib-openjpeg/`
echo "OPENJPEG_VERSION ${OPENJPEG_VERSION}"
cd /var/tmp/repository/org/esa/snap/lib-openjpeg/${OPENJPEG_VERSION}
unzip lib-openjpeg-${OPENJPEG_VERSION}.jar
mkdir -p /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
cp -r auxdata/openjpeg/* /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}
chmod 755 -R /home/snap/auxdata/openjpeg/${OPENJPEG_VERSION}/*/bin

# Set up GDAL

export GDAL_VERSION=`ls /var/tmp/repository/org/esa/s2tbx/lib-gdal/`
echo "OPENJPEG_VERSION ${GDAL_VERSION}"
cd /var/tmp/repository/org/esa/s2tbx/lib-gdal/${GDAL_VERSION}
unzip lib-gdal-${GDAL_VERSION}.jar
mkdir -p /home/snap/auxdata/gdal
cp auxdata/gdal/Linux/x64/gdal-*-*-0.zip /home/snap/auxdata/gdal
cd /home/snap/auxdata/gdal/
unzip *.zip
chmod 755 -R bin/*

# Update LD_LIBRARY_PATH Library
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib/:/lib/x86_64-linux-gnu/:/usr/lib/:/home/snap/auxdata/gdal/:/home/snap/auxdata/gdal/lib/:/home/snap/auxdata/gdal/lib/jni/
echo "LD_LIBRARY_PATH ${LD_LIBRARY_PATH}"
cd ${WORKSPACE_PATH}
pwd

echo "setup finished"