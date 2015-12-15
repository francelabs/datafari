require 'rake'
require 'file-dependencies'

namespace "vendor" do

  desc "Process vendor files"
  task "files" do
    FileDependencies.process_vendor(Dir.pwd)
  end

end
