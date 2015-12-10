#!/bin/sh
# Test: make sure a renamed file resumes it's since state

. $(dirname $0)/framework.sh

test_init
test_start

echo 1 > $TEST_DIR/a.log
echo 2 >> $TEST_DIR/a.log
echo 3 >> $TEST_DIR/a.log
sleep 6

test_stop

echo 4 >> $TEST_DIR/a.log
mv $TEST_DIR/a.log $TEST_DIR/b.log

test_start

echo 5 >> $TEST_DIR/b.log
sleep 3

test_done
