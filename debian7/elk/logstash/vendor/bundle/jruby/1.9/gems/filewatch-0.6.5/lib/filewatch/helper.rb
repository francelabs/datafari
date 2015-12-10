# code downloaded from Ruby on Rails 4.2.1
# https://raw.githubusercontent.com/rails/rails/v4.2.1/activesupport/lib/active_support/core_ext/file/atomic.rb
require 'fileutils'

class File
  # Write to a file atomically. Useful for situations where you don't
  # want other processes or threads to see half-written files.
  #
  #   File.atomic_write('important.file') do |file|
  #     file.write('hello')
  #   end
  #
  # If your temp directory is not on the same filesystem as the file you're
  # trying to write, you can provide a different temporary directory.
  #
  #   File.atomic_write('/data/something.important', '/data/tmp') do |file|
  #     file.write('hello')
  #   end
  def self.atomic_write(file_name)

    if File.exist?(file_name)
      # Get original file permissions
      old_stat = stat(file_name)
    else
      # If not possible, probe which are the default permissions in the
      # destination directory.
      old_stat = probe_stat_in(dirname(file_name))
    end

    mode = old_stat ? old_stat.mode : nil

    # Create temporary file with identical permissions
    temp_file = File.new(rand_filename(file_name), "w", mode)
    temp_file.binmode
    return_val = yield temp_file
    temp_file.close

    # Overwrite original file with temp file
    File.rename(temp_file.path, file_name)

    # Unable to get permissions of the original file => return
    return return_val if old_stat.nil?

    # Set correct uid/gid on new file
    chown(old_stat.uid, old_stat.gid, file_name) if old_stat

    return_val
  end

  # Private utility method.
  def self.probe_stat_in(dir) #:nodoc:
    basename = rand_filename(".permissions_check")
    file_name = join(dir, basename)
    FileUtils.touch(file_name)
    stat(file_name)
  rescue
    # ...
  ensure
    FileUtils.rm_f(file_name) if File.exist?(file_name)
  end

  def self.rand_filename(prefix)
    [ prefix, Thread.current.object_id, Process.pid, rand(1000000) ].join('.')
  end

  def self.device?(file_name)
    chardev?(file_name) || blockdev?(file_name)
  end
end
