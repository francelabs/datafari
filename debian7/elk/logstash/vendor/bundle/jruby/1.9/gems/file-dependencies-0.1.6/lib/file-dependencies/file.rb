require "digest/sha1"
require "net/http"
require "uri"
require 'fileutils'
require 'tmpdir'

module FileDependencies
  # :nodoc:
  module File
    SHA1_REGEXP = /(\b[0-9a-f]{40}\b)/

    def fetch_sha1(remote_sha1)
      if URI(remote_sha1.to_s).scheme.nil?
        sha1 = remote_sha1
      else
        file = download(remote_sha1, Dir.tmpdir)
        sha1 = IO.read(file).gsub("\n", '')
      end
      raise("invalid SHA1 signature. Got '#{sha1}'") unless sha1.match(SHA1_REGEXP)
      sha1
    end
    module_function :fetch_sha1

    def validate_sha1(local_file, remote_sha1)
      return true if remote_sha1 == 'none'
      sha1 = fetch_sha1(remote_sha1)
      local_sha1 = calculate_sha1(local_file)

      raise("SHA1 did not match. Expected #{sha1} but computed #{local_sha1}") if sha1 != local_sha1
      true
    end # def validate_sha1
    module_function :validate_sha1

    def calculate_sha1(path)
      Digest::SHA1.file(path).hexdigest
    end # def calc__sha1
    module_function :calculate_sha1

    def fetch_file(url, sha1, target)
      puts "Downloading #{url}" if $DEBUG

      file = download(url, target)
      return file if validate_sha1(file, sha1)
    end # def fetch_file
    module_function :fetch_file

    def download(url, target)
      uri = URI(url)
      output = ::File.join(target, ::File.basename(uri.path))
      tmp = "#{output}.tmp"
      Net::HTTP.start(uri.host, uri.port, :use_ssl => (uri.scheme == "https")) do |http|
        request = Net::HTTP::Get.new(uri.path)
        http.request(request) do |response|
          raise("HTTP fetch failed for #{url}. #{response}") unless [200, 301].include?(response.code.to_i)
          size = (response["content-length"].to_i || -1).to_f
          count = 0
          ::File.open(tmp, "w") do |fd|
            response.read_body do |chunk|
              fd.write(chunk)
              if size > 0 && $stdout.tty?
                count += chunk.bytesize
                $stdout.write(sprintf("\r%0.2f%%", count / size * 100))
              end
            end
          end
          $stdout.write("\r      \r") if $stdout.tty?
        end
      end

      ::File.rename(tmp, output)

      return output
    rescue SocketError => e
      puts "Failure while downloading #{url}: #{e}"
      raise
    ensure
      ::File.unlink(tmp) if ::File.exist?(tmp)
    end # def download
    module_function :download
  end
end
