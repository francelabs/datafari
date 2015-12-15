module LogStash::PluginMixins::AwsConfig::Generic
  def self.included(base)
    base.extend(self)
    base.generic_aws_config
  end

  def generic_aws_config
    # The AWS Region
    config :region, :validate => LogStash::PluginMixins::AwsConfig::REGIONS_ENDPOINT, :default => LogStash::PluginMixins::AwsConfig::US_EAST_1 

    # This plugin uses the AWS SDK and supports several ways to get credentials, which will be tried in this order...
    # 1. Static configuration, using `access_key_id` and `secret_access_key` params in logstash plugin config
    # 2. External credentials file specified by `aws_credentials_file`
    # 3. Environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`
    # 4. Environment variables `AMAZON_ACCESS_KEY_ID` and `AMAZON_SECRET_ACCESS_KEY`
    # 5. IAM Instance Profile (available when running inside EC2)
    config :access_key_id, :validate => :string

    # The AWS Secret Access Key
    config :secret_access_key, :validate => :string

    # The AWS Session token for temprory credential
    config :session_token, :validate => :string

    # URI to proxy server if required
    config :proxy_uri, :validate => :string

    # Path to YAML file containing a hash of AWS credentials.
    # This file will only be loaded if `access_key_id` and
    # `secret_access_key` aren't set. The contents of the
    # file should look like this:
    #
    #     :access_key_id: "12345"
    #     :secret_access_key: "54321"
    #
    config :aws_credentials_file, :validate => :string
  end
end
