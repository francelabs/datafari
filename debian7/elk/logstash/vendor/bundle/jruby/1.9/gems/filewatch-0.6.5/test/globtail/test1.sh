#!/bin/sh
# Test: basic tail support

. $(dirname $0)/framework.sh

test_init
test_start

echo a > $TEST_DIR/a.log
echo b > $TEST_DIR/b.log
echo c > $TEST_DIR/c.log
echo a >> $TEST_DIR/a.log
echo c >> $TEST_DIR/c.log

sleep 5

test_done
