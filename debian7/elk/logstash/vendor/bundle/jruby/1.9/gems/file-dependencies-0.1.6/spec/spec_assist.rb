require 'stud/temporary'

# :nodoc:
module Assist
  def self.generate_tarball(files)
    tarpath = "#{Stud::Temporary.pathname}.tar.gz"
    tarfile = File.new(tarpath, "wb")
    gz = Zlib::GzipWriter.new(tarfile, Zlib::BEST_COMPRESSION)
    tar = Archive::Tar::Minitar::Output.new(gz)
    files.each do |path, value|
      opts = {
        :size => value.bytesize,
        :mode => 0666,
        :mtime => Time.new
      }
      tar.tar.add_file_simple(path, opts) do |io|
        io.write(value)
      end
    end
    tar.close
    tarpath
  end

  def self.generate_gzip(content)
    file = "#{Stud::Temporary.pathname}.gz"
    Zlib::GzipWriter.open(file) do |gz|
      gz.write(content)
    end
    file
  end

  def self.generate_file(content)
    file = Stud::Temporary.pathname
    File.write(file, content)
    file
  end
end
