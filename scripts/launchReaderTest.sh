#! /bin/bash

if [ $# -ne 3 ]
then
    echo "Usage $0 dataPath classPathFilter buildNumber"
    echo "dataPath: path to the data required for the test. Example : /data/ssd/testData/s2tbx"
    echo "classPathFilter: filter to tell what class should be used for reader tests. Example : org.esa.s2tbx"
    echo "buildNumber: current build number. Example : 76"
    echo "ramSize: size of the RAM to use"
fi

export DATA_PATH=$1
export CLASS_PATH_FILTER=$2
export BUILD_NUMBER=$3
export RAM_SIZE=$4

export LD_LIBRARY_PATH=.
export MAVEN_OPTS="-Xmx${RAM_SIZE}"
export MAX_MEMORY="-Xmx${RAM_SIZE}" 

# Launch tests
mvn -Dmax.memory=${MAX_MEMORY} -Dncsa.hdf.hdflib.HDFLibrary.hdflib=/home/snap/snap/snap/modules/lib/amd64/libjhdf.so -Dncsa.hdf.hdf5lib.H5.hdf5lib=/home/snap/snap/snap/modules/lib/amd64/libjhdf5.so -s /var/maven/.m2/settings.xml -Duser.home=/var/maven -Dsnap.userdir=/home/snap -Dsnap.reader.tests.execute=true -Dsnap.reader.tests.data.dir=${DATA_PATH} -Dsnap.reader.tests.class.name=${CLASS_PATH_FILTER} -Dsnap.reader.tests.failOnMissingData=true clean test 2>&1 | tee -a ./readerTest-${BUILD_NUMBER}.log

# return mvn status
exit ${PIPESTATUS[0]}
