#!/bin/bash -e
#
#
# Deployment script for Datafari
#
#

mvn -B clean install
ant clean-build -f linux/build.xml
