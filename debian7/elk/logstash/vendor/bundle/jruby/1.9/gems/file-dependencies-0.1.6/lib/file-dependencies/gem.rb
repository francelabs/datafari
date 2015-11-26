require 'json'
require 'file-dependencies'
module FileDependencies
  # :nodoc:
  module Gem
    def hook
      ::Gem.post_install do |gem_installer|
        next if ENV['VENDOR_SKIP'] == 'true'
        FileDependencies.process_vendor(gem_installer.gem_dir)
      end
    end # def hook
    module_function :hook
  end
end
