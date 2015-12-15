#!/bin/sh

test_init() {
  export TEST_BASE=$(dirname $0)
  export FW_BASE="$TEST_BASE/../.."
  export RUBYLIB=$FW_BASE/lib:$RUBYLIB
  export SINCEDB=$(mktemp)
  export TAIL="$FW_BASE/bin/globtail -v -s $SINCEDB -i 5 -x skip*.log"
  export TEST_DIR=$(mktemp -d)
  export TEST_OUT=$(mktemp)
  touch $TEST_OUT
  mkdir -p $TEST_DIR
}

test_start() {
  $TAIL "$TEST_DIR/*" >>$TEST_OUT 2>&1 &
  export TEST_TAIL_PID=$!

  # let globtail get started and do it's initial glob
  sleep 3
}

test_stop() {
  kill $TEST_TAIL_PID 2>/dev/null
  count=0
  while kill -0 $TEST_TAIL_PID 2>/dev/null; do
    count=$((count+1))
    sleep 1
    if [ "$count" -eq 5 ]; then
      kill -9 $TEST_TAIL_PID
      count=0
    fi
  done
  export TEST_TAIL_PID=""
}

test_done() {
  [ -n "$TEST_TAIL_PID" ] && test_stop

  output=$(mktemp)
  output_clean=$(mktemp)
  sed -e "s,^${TEST_DIR}/,," $TEST_OUT | sort > $output
  sed -e '/^D, \[/d' < $output > $output_clean

  data_file=$(echo $0 | sed -e 's/\.sh$/.data/')

  diff $TEST_BASE/$data_file $output_clean >/dev/null
  diff_rc=$?

  if [ $diff_rc -ne 0 ]; then
    diff -u $TEST_BASE/$data_file $output_clean
    echo "$0 TEST FAILURE (output differs)"
    sed -e 's,^,output: ,' $TEST_OUT
  fi

  rm -rf $TEST_DIR $TEST_OUT $output $output_clean $SINCEDB
  exit $diff_rc
}
