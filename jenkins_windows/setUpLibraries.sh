#! /bin/bash
set -e

# Run maven to download jar containing libraries
mvn -Dsnap.userdir='C:/Program Files/snap' install -DskipTests=true

export WORKSPACE_PATH=`pwd`

# Set up OpenJPEG
echo "Set up OpenJPEG"
rm -rf C:/Users/admin/setupLib/tmp
mkdir C:/Users/admin/setupLib/tmp
cd C:/Users/admin/setupLib/tmp
cp C:/Temp/snap/.m2/repository/org/esa/snap/lib-openjpeg/9.0.0-SNAPSHOT/lib-openjpeg-9.0.0-SNAPSHOT.jar .
export OPENJPEG_VERSION="9.0.0-SNAPSHOT"
unzip -o lib-openjpeg-${OPENJPEG_VERSION}.jar
mkdir -p C:/Users/admin/.snap/auxdata/openjpeg/${OPENJPEG_VERSION}
cp -r auxdata/openjpeg/* C:/Users/admin/.snap/auxdata/openjpeg/${OPENJPEG_VERSION}
chmod 755 -R C:/Users/admin/.snap/auxdata/openjpeg/${OPENJPEG_VERSION}/*/bin

# Set up GDAL
echo "Set up GDAL"
cp C:/Temp/snap/.m2/repository/org/esa/s2tbx/lib-gdal/9.0.0-SNAPSHOT/lib-gdal-9.0.0-SNAPSHOT.jar .
export GDAL_VERSION="9.0.0-SNAPSHOT"
unzip -o lib-gdal-${GDAL_VERSION}.jar
mkdir -p C:/Users/admin/.snap/auxdata/gdal
cp auxdata/gdal/Windows/x64/gdal-*-*-1.zip C:/Users/admin/.snap/auxdata/gdal
cd C:/Users/admin/.snap/auxdata/gdal/
unzip -o *.zip
# chmod 755 -R bin/*
# Update LD_LIBRARY_PATH Library
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:"C:/Program Files/snap/auxdata/gdal/gdal-3-2-1/":"C:/Users/admin/.snap/auxdata/gdal/":"C:/Users/admin/.snap/auxdata/gdal/lib/":"C:/Users/admin/.snap/auxdata/gdal/lib/jni/"

echo "end of setup libraries"