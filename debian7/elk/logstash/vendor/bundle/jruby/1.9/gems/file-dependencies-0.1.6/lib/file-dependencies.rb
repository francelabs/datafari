require 'file-dependencies/file'
require 'file-dependencies/archive'
require 'json'
require 'tmpdir'
require 'fileutils'
# :nodoc:
module FileDependencies
  def process_vendor(dir, target = 'vendor', tmpdir = Dir.tmpdir)
    vendor_file = ::File.join(dir, 'vendor.json')
    if ::File.exist?(vendor_file)
      vendor_file_content = IO.read(vendor_file)
      file_list = JSON.load(vendor_file_content)
      FileDependencies.process_downloads(file_list, ::File.join(dir, target), tmpdir)
    else
      puts "vendor.json not found, looked for the file at #{vendor_file}" if $DEBUG
    end
  end # def process_vendor
  module_function :process_vendor

  def process_downloads(files, target, tmpdir)
    FileUtils.mkdir_p(target) unless ::File.directory?(target)
    files.each do |file|
      full_target = file['target'] ? ::File.join(target, file['target']) : target
      download = FileDependencies::File.fetch_file(file['url'], file['sha1'], tmpdir)
      if (res = download.match(/(\S+?)(\.tar\.gz|\.tgz)$/))
        FileDependencies::Archive.untar(download) do |entry|
          prefix = file['include_tar_prefix'] ? '' : ::File.join(entry.full_name.split(::File::SEPARATOR).first, '')
          next unless FileDependencies::Archive.extract_file?(entry.full_name, file['extract'], file['exclude'], prefix)
          if file['flatten'] == true
            ::File.join(full_target, ::File.basename(entry.full_name))
          else
            ::File.join(full_target, entry.full_name.gsub(prefix, ''))
          end
        end
      elsif download =~ /.gz$/
        FileDependencies::Archive.ungzip(download, full_target)
      else
        FileUtils.mv(download, ::File.join(full_target, ::File.basename(download)))
      end
    end
  end # def download
  module_function :process_downloads
end
