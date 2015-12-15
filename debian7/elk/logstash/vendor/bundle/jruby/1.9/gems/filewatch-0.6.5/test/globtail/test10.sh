#!/bin/sh
# Test: file rename & old file name having new data

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
sleep 8

echo 2 >> $TEST_DIR/a.log
sleep 3

echo 3 >> $TEST_DIR/a.log
mv $TEST_DIR/a.log $TEST_DIR/b.log
echo 4 > $TEST_DIR/a.log
sleep 8

test_done
