#!/bin/sh
#
# start cloud service
#
###EOF

prog=datapipeline-griffin
export JAVA_HOME=/usr/jdk1.8.0_191
export DEPLOY_PATH=/home/datapipeline_griffin_v2
export DEBUG_PORT=11523

if [ ! -d $JAVA_HOME ];then
    echo "please set right JAVA_HOME in this file"
    exit 0
fi

if [ ! -d $DEPLOY_PATH ];then
    echo "please set right DEPLOY_PATH in this file"
    exit 0
fi

function usage(){
cat << EOF
Usage: startServer.sh --port <port>  [options]

Options:
    --help | -h Print usage information.
    --port | -p Set java debug port, default value(5100).
EOF

exit $1
}

while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help) usage 0 ;;
        -p|--port) shift; DEBUG_PORT=$1 ;;
        *) shift ;; # ignore
    esac
    shift
done


export JAVA_OPTIONS="-Xmx1024m -Xms1024m -Dcom.sun.management.jmxremote.port=22399 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
export CLASSPATH=${CLASSPATH}:${DEPLOY_PATH}/resources:${DEPLOY_PATH}/lib/*
export JAVA_DEBUG="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=n"
export TIME_ZONE="-Duser.timezone=GMT+08"

${JAVA_HOME}/bin/java ${JAVA_OPTIONS} ${JAVA_DEBUG} ${TIME_ZONE}\
-Dcom.dc.install_path=${DEPLOY_PATH}/resources \
-classpath ${CLASSPATH} org.apache.griffin.core.GriffinWebApplication $prog &