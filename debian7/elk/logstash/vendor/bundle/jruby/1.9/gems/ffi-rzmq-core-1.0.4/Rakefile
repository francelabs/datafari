require 'bundler/gem_tasks'

require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new

task :default => :spec

task :console do
  require 'irb'
  require 'irb/completion'
  require 'ffi-rzmq-core'
  ARGV.clear
  IRB.start
end

task :pryconsole do
  require 'pry'
  require 'ffi-rzmq-core'
  ARGV.clear
  Pry.start
end