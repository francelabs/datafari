Aws.add_service(:Kinesis, {
  api: "#{Aws::API_DIR}/kinesis/2013-12-02/api-2.json",
  docs: "#{Aws::API_DIR}/kinesis/2013-12-02/docs-2.json",
  paginators: "#{Aws::API_DIR}/kinesis/2013-12-02/paginators-1.json",
  waiters: "#{Aws::API_DIR}/kinesis/2013-12-02/waiters-2.json",
})
