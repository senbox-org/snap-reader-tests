#!/bin/bash
set -e

if [ $# -ne 3 ]
then
    echo "Usage $0 dataPath classPathFilter buildNumber ramSize settingsPath"
    echo "dataPath: path to the data required for the test. Example : /data/ssd/testData/s2tbx"
    echo "classPathFilter: filter to tell what class should be used for reader tests. Example : org.esa.s2tbx"
    echo "buildNumber: current build number. Example : 76"
    echo "ramSize: size of the RAM to use"
fi

export DATA_PATH=$1
export CLASS_PATH_FILTER=$2
export BUILD_NUMBER=$3
export RAM_SIZE=$4
export MAVEN_SETTINGS_FILE=$5
export LIB_HDF_ARGS=$6

export LD_LIBRARY_PATH=.
export MAVEN_OPTS="-Xmx${RAM_SIZE}"
export MAX_MEMORY="-Xmx${RAM_SIZE}" 


if [ -z "${MAVEN_SETTINGS_FILE}" ];
    then
        SETTINGS_FILE="/var/maven/.m2/settings.xml"
    else
        SETTINGS_FILE="${MAVEN_SETTINGS_FILE}"
fi

# Launch tests
mvn \
    --no-transfer-progress --batch-mode --errors --fail-at-end --show-version -DdeployAtEnd=false \
    -Dmax.memory=${MAX_MEMORY} ${LIB_HDF_ARGS} \
    -s ${SETTINGS_FILE} -Duser.home=/var/maven -Dsnap.userdir=/home/snap \
    -Dsnap.reader.tests.execute=true -Dsnap.reader.tests.data.dir=${DATA_PATH} \
    -Dsnap.reader.tests.class.name=${CLASS_PATH_FILTER} \
    -Dsnap.reader.tests.failOnMissingData=true test 2>&1 | tee -a ./readerTest-${BUILD_NUMBER}.log
