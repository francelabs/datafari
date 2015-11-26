#!/bin/sh
# Test: make sure we properly write a sincedb on SIGTERM, and pick up
# new log lines that were written while globtail is being restarted.

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
echo 2 >> $TEST_DIR/a.log
echo 3 >> $TEST_DIR/a.log
sleep 6

echo 4 >> $TEST_DIR/a.log
test_stop

echo 5 >> $TEST_DIR/a.log

test_start

echo 6 >> $TEST_DIR/a.log
sleep 3

test_done
