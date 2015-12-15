#!/bin/sh
# Test: ensure sincedb periodic database writing works (make sure we're not
# relying on SIGTERM handling)

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
echo 2 >> $TEST_DIR/a.log
echo 3 >> $TEST_DIR/a.log
sleep 8

echo 4 >> $TEST_DIR/a.log
sleep 3

# send a "kill -9" to test that the sincedb write interval stuff is working
kill -9 $TEST_TAIL_PID
test_stop

echo 5 >> $TEST_DIR/a.log

test_start

echo 6 >> $TEST_DIR/a.log
sleep 3

test_done
