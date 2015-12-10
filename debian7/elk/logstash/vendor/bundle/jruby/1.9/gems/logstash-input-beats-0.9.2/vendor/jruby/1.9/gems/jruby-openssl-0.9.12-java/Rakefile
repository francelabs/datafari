#-*- mode: ruby -*-

begin
  require 'maven/ruby/tasks'
rescue LoadError
  warn "ruby-maven not available - some tasks will not work " <<
       "either `gem install ruby-maven' or use mvn instead of rake"
  desc "Package jopenssl.jar with the compiled classes"
  task :jar do
    sh "mvn prepare-package -Dmaven.test.skip=true"
  end
  namespace :jar do
    desc "Package jopenssl.jar file (and dependendent jars)"
    task :all do
      sh "mvn package -Dmaven.test.skip=true"
    end
  end
else
  Rake::Task[:jar].clear
  desc "Package jopenssl.jar with the compiled classes"
  task :jar => :maven do
    maven.prepare_package '-Dmaven.test.skip=true'
  end
  namespace :jar do
    desc "Package jopenssl.jar file (and dependendent jars)"
    task :all => :maven do
      maven.package '-Dmaven.test.skip=true'
    end
  end
end

# the actual build configuration is inside the Mavenfile

task :default => :build

file('lib/jopenssl.jar') { Rake::Task['jar'].invoke }

require 'rake/testtask'
Rake::TestTask.new do |task|
  task.libs << 'lib'
  task.test_files = FileList['src/test/ruby/**/test*.rb']
  task.verbose = true
  task.loader = :direct
end
task :test => 'lib/jopenssl.jar'

namespace :integration do
  it_path = File.expand_path('../src/test/integration', __FILE__)
  task :install do
    Dir.chdir(it_path) do
      ruby "-S bundle install --gemfile '#{it_path}/Gemfile'"
    end
  end
  # desc "Run IT tests"
  task :test => 'lib/jopenssl.jar' do
    unless File.exist?(File.join(it_path, 'Gemfile.lock'))
      raise "bundle not installed, run `rake integration:install'"
    end
    loader = "ARGV.each { |f| require f }"
    test_files = FileList['src/test/integration/*_test.rb'].to_a
    ruby "-Ilib -e \"#{loader}\" #{test_files.map { |f| "\"#{f}\"" }.join(' ')}"
  end
end