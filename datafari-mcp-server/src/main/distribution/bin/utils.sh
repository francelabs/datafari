#!/usr/bin/env bash
#
# Common process-management functions
#

is_running() {
    local pid_file="$1"
    local expected_command="${2:-}"

    if [[ ! -f "${pid_file}" ]]; then
        return 1
    fi

    local pid
    pid="$(cat "${pid_file}")"

    if [[ ! "${pid}" =~ ^[0-9]+$ ]]; then
        echo "Warning: invalid PID file ${pid_file}; removing it."
        rm -f "${pid_file}"
        return 1
    fi

    if ! kill -0 "${pid}" 2>/dev/null; then
        echo "Warning: stale PID file ${pid_file}; removing it."
        rm -f "${pid_file}"
        return 1
    fi

    # Protect against PID reuse after a crash or reboot.
    if [[ -n "${expected_command}" && -r "/proc/${pid}/cmdline" ]]; then
        if ! tr '\0' ' ' < "/proc/${pid}/cmdline" |
            grep -Fq -- "${expected_command}"; then
            echo "Warning: PID ${pid} belongs to another process; removing stale PID file."
            rm -f "${pid_file}"
            return 1
        fi
    fi

    return 0
}

waitpid() {
    local pid="$1"
    local timeout="$2"

    if [[ -z "${pid}" || -z "${timeout}" ]]; then
        return 10
    fi

    local elapsed=0

    while kill -0 "${pid}" 2>/dev/null; do
        if [[ "${elapsed}" -ge "${timeout}" ]]; then
            return 1
        fi

        sleep 1
        elapsed=$((elapsed + 1))
        echo -n "."
    done

    return 0
}

forceStopIfNecessary() {
    local pid_file="$1"
    local process_name="$2"
    local expected_command="${3:-}"

    if ! is_running "${pid_file}" "${expected_command}"; then
        return 0
    fi

    local pid
    pid="$(cat "${pid_file}")"

    kill "${pid}" 2>/dev/null || true

    if ! waitpid "${pid}" "${MCP_SERVER_STOP_TIMEOUT:-30}"; then
        echo
        echo "Warning: failed to stop ${process_name} gracefully; sending SIGKILL."
        kill -KILL "${pid}" 2>/dev/null || true
        sleep 1
    fi

    rm -f "${pid_file}"
    echo " stopped"
}