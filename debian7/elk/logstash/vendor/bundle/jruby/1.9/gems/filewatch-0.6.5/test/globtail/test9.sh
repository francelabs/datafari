#!/bin/sh
# Test: exclude support

. $(dirname $0)/framework.sh

test_init
test_start

echo a1 > $TEST_DIR/a.log
echo b1 > $TEST_DIR/b.log
echo nope1 > $TEST_DIR/skip1.log

sleep 8

mv $TEST_DIR/b.log $TEST_DIR/skip2.log
echo b2 > $TEST_DIR/b.log
sleep 8

echo nope2 >> $TEST_DIR/skip2.log
sleep 3

test_done
