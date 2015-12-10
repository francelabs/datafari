module JrJackson
  module BuildInfo
    def self.version
      '0.2.9'
    end

    def self.release_date
      '2015-06-24'
    end

    def self.files
      `git ls-files`.split($/).select{|f| f !~ /\Abenchmarking/}
    end
  end
end
