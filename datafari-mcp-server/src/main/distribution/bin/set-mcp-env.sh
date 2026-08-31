#!/usr/bin/env bash
#
# Datafari MCP Server environment
#

export DATAFARI_HOME="$(
    cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd
)"

export MCP_SERVER_HOME="$(
    cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd
)"

export MCP_SERVER_JAR="${MCP_SERVER_HOME}/bin/mcp-server.jar"
export MCP_SERVER_CONF_DIR="${MCP_SERVER_HOME}/conf"

export MCP_SERVER_APPLICATION_CONFIG="${MCP_SERVER_CONF_DIR}/application.properties"
export MCP_SERVER_LOGGING_CONFIG="${MCP_SERVER_CONF_DIR}/log4j2.xml"

export PID_DIR="${DATAFARI_HOME}/pid"
export MCP_SERVER_PID_FILE="${PID_DIR}/mcp-server.pid"

export MCP_SERVER_LOGS_DIR="${DATAFARI_HOME}/logs"
export MCP_SERVER_CONSOLE_LOG="${MCP_SERVER_LOGS_DIR}/mcp-server-console.log"

# Replaced by the Datafari installer.
export MCP_SERVER_MEM="${MCP_SERVER_MEM:--Xms128m -Xmx512m}"

# Additional JVM options can be supplied by the environment.
export MCP_SERVER_JAVA_OPTS="${MCP_SERVER_JAVA_OPTS:-}"

export MCP_SERVER_STARTUP_WAIT="${MCP_SERVER_STARTUP_WAIT:-2}"
export MCP_SERVER_STOP_TIMEOUT="${MCP_SERVER_STOP_TIMEOUT:-30}"

if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="${PATH}:${JAVA_HOME}/bin"
fi

export PATH="${PATH}:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"