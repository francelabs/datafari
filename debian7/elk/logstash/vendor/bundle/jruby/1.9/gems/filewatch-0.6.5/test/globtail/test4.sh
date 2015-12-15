#!/bin/sh
# Test: tests file truncation

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
echo 2 >> $TEST_DIR/a.log
echo 3 >> $TEST_DIR/a.log
sleep 3
echo 4 > $TEST_DIR/a.log
sleep 3

test_done
