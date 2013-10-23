#!/usr/bin/bash

# Get control port from args
if [ $# -lt 2 ]; then
	CONTROL_PORT=8001
elif [ $# -eq 2 ]; then
	CONTROL_PORT=$1
else
	echo "[$0] usage: $0 [debug_port]" >&2
	exit 1
fi

# Set debug environment variable
export JAVA_OPTS="-Xrunjdwp:transport=dt_socket,address=${CONTROL_PORT},server=y,suspend=n"

# Run standalone jboss
/home/yyz/mbarkley/jboss-as-7.1.1.Final/bin/standalone.sh -c standalone-embed.xml
