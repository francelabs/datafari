#!/bin/sh
# Test: tests glob discovery of a new file, and in-memory sincedb
# preserving file position after a rename

. $(dirname $0)/framework.sh

test_init
test_start

echo a > $TEST_DIR/a.log
sleep 5
mv $TEST_DIR/a.log $TEST_DIR/b.log
sleep 5
echo b >> $TEST_DIR/b.log
sleep 3

test_done
