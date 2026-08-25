#!/usr/bin/env bash
#
# MCP Server Manager
#

set -u

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/set-mcp-env.sh"
source "${SCRIPT_DIR}/utils.sh"

validate_configuration() {
    if [[ -z "${JAVA_HOME:-}" ]]; then
        echo "Error: JAVA_HOME is not configured."
        return 1
    fi

    if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
        echo "Error: Java executable not found:"
        echo "  ${JAVA_HOME}/bin/java"
        return 1
    fi

    if [[ ! -r "${MCP_SERVER_JAR}" ]]; then
        echo "Error: MCP Server JAR not found or not readable:"
        echo "  ${MCP_SERVER_JAR}"
        return 1
    fi

    if [[ ! -r "${MCP_SERVER_APPLICATION_CONFIG}" ]]; then
        echo "Error: application.properties not found or not readable:"
        echo "  ${MCP_SERVER_APPLICATION_CONFIG}"
        return 1
    fi

    if [[ ! -r "${MCP_SERVER_LOGGING_CONFIG}" ]]; then
        echo "Error: Log4j2 configuration not found or not readable:"
        echo "  ${MCP_SERVER_LOGGING_CONFIG}"
        return 1
    fi

    if [[ "${MCP_SERVER_MEM}" == *"@"* ]]; then
        echo "Error: MCP_SERVER_MEM has not been configured:"
        echo "  ${MCP_SERVER_MEM}"
        echo "Replace @MCPSERVERMEMORY@ during installation."
        return 1
    fi

    mkdir -p "${PID_DIR}" "${MCP_SERVER_LOGS_DIR}"
}

cmd_start() {
    if is_running "${MCP_SERVER_PID_FILE}" "${MCP_SERVER_JAR}"; then
        local pid
        pid="$(cat "${MCP_SERVER_PID_FILE}")"

        echo "Error: MCP Server is already running with PID ${pid}."
        return 1
    fi

    validate_configuration || return 1

    local memory_options=()
    local extra_java_options=()

    read -r -a memory_options <<< "${MCP_SERVER_MEM}"

    if [[ -n "${MCP_SERVER_JAVA_OPTS}" ]]; then
        read -r -a extra_java_options <<< "${MCP_SERVER_JAVA_OPTS}"
    fi

    echo "Starting MCP Server..."

    nohup "${JAVA_HOME}/bin/java" \
        -Duser.timezone=UTC \
        -Dspring.config.additional-location="file:${MCP_SERVER_CONF_DIR}/" \
        -Dlogging.config="file:${MCP_SERVER_LOGGING_CONFIG}" \
        "${memory_options[@]}" \
        "${extra_java_options[@]}" \
        -jar "${MCP_SERVER_JAR}" \
        >> "${MCP_SERVER_CONSOLE_LOG}" 2>&1 &

    local pid=$!
    echo "${pid}" > "${MCP_SERVER_PID_FILE}"

    sleep "${MCP_SERVER_STARTUP_WAIT}"

    if ! is_running "${MCP_SERVER_PID_FILE}" "${MCP_SERVER_JAR}"; then
        echo "Error: MCP Server failed to start."
        echo "See ${MCP_SERVER_CONSOLE_LOG}"
        return 1
    fi

    echo "MCP Server started with PID ${pid}."
}

cmd_stop() {
    if is_running "${MCP_SERVER_PID_FILE}" "${MCP_SERVER_JAR}"; then
        echo -n "Stopping MCP Server"

        forceStopIfNecessary \
            "${MCP_SERVER_PID_FILE}" \
            "MCP Server" \
            "${MCP_SERVER_JAR}"
    else
        echo "Warning: MCP Server does not seem to be running."
    fi
}

cmd_status() {
    if is_running "${MCP_SERVER_PID_FILE}" "${MCP_SERVER_JAR}"; then
        local pid
        pid="$(cat "${MCP_SERVER_PID_FILE}")"

        echo "MCP Server is running:"
        ps -o pid,etime,cmd --width 5000 -p "${pid}"
        return 0
    fi

    echo "MCP Server is not running."
    return 3
}

cmd_restart() {
    cmd_stop || return 1
    cmd_start
}

print_usage() {
    echo "Usage: $0 {start|stop|restart|status}"
}

COMMAND="${1:-}"

case "${COMMAND}" in
    start)
        cmd_start
        ;;
    stop)
        cmd_stop
        ;;
    restart)
        cmd_restart
        ;;
    status)
        cmd_status
        ;;
    *)
        print_usage
        exit 1
        ;;
esac