#!/usr/bin/bash

# Define function for cleaning up tmp files and folders
function clean() {
	echo "[$0] Cleaning up $JBOSS_HOME/standalone..."
	rm -fv $JBOSS_HOME/standalone/configuration/standalone-dev.xml
	rm -rfv $JBOSS_HOME/standalone/tmp
	rm -rfv $JBOSS_HOME/standalone/data
	rm -rfv $JBOSS_HOME/standalone/log
	rm -rfv $JBOSS_HOME/standalone/configuration/standalone_xml_history
	echo "[$0] JBoss AS cleaned"
}

# Remove config file on exit
trap 'clean' EXIT SIGTERM SIGINT

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

# Create new temporary config file to avoid accidentally persisting deployments between runs
cp -v "$JBOSS_HOME/standalone/configuration/standalone-full.xml" "$JBOSS_HOME/standalone/configuration/standalone-dev.xml"

# Give execute permission for standalone.sh in classpath
chmod +x "$JBOSS_HOME/bin/standalone.sh"

# Run standalone jboss
"$JBOSS_HOME/bin/standalone.sh" -c standalone-dev.xml

