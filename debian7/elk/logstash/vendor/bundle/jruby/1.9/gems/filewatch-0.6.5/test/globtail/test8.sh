#!/bin/sh
# Test: sincedb, and file truncation between globtail runs

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
echo 2 >> $TEST_DIR/a.log
echo 3 >> $TEST_DIR/a.log
sleep 8

test_stop

echo 4 > $TEST_DIR/a.log

test_start

echo 5 >> $TEST_DIR/a.log
sleep 3

test_done
