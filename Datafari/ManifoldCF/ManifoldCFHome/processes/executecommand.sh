#!/bin/bash -e

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [[ $OSTYPE == "cygwin" ]] ; then
    PATHSEP=";"
else
    PATHSEP=":"
fi

#Make sure environment variables are properly set
if [ -e "$JAVA_HOME"/bin/java ] ; then
    if [ -f "$MCF_HOME"/properties.xml ] ; then
    
        # Build the classpath
        CLASSPATH=""
        for filename in $(ls -1 "$MCF_HOME"/processes/lib) ; do
            if [ -n "$CLASSPATH" ] ; then
                CLASSPATH="$CLASSPATH""$PATHSEP""$MCF_HOME"/processes/lib/"$filename"
            else
                CLASSPATH="$MCF_HOME"/processes/lib/"$filename"
            fi
        done

        # Build the options
	OPTIONS=$(cat "$MCF_HOME"/processes/options.env)
        
        # Build the defines
        DEFINES="-Dorg.apache.manifoldcf.configfile=$MCF_HOME/properties.xml"
        if [ -e "$MCF_HOME/processes/define" ] ; then
            for filename in $(ls -1 "$MCF_HOME"/processes/define) ; do
                DEFINEVAR=-D"$filename"=$(cat "$MCF_HOME"/processes/define/"$filename")
                DEFINES="$DEFINES $DEFINEVAR"
            done
        fi

        "$JAVA_HOME/bin/java" $OPTIONS $DEFINES -cp "$CLASSPATH" "$@"
        exit $?
        
    else
        echo "Environment variable MCF_HOME is not properly set." 1>&2
        exit 1
    fi
    
else
    echo "Environment variable JAVA_HOME is not properly set." 1>&2
    exit 1
fi
