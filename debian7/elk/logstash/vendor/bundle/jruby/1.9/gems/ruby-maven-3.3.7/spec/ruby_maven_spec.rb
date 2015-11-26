require_relative 'setup'
require 'ruby_maven'
require 'maven/ruby/version'

module CatchStdout

  def self.exec
    out = $stdout
    @result = StringIO.new
    $stdout = @result
    yield
  ensure
    $stdout = out
  end

  def self.result
    @result.string
  end
end

describe RubyMaven do

  it 'displays the version info' do
    CatchStdout.exec do
      RubyMaven.exec( '--version' )
    end
    CatchStdout.result.must_match /Polyglot Maven Extension 0.1.9/
  end

  let :gem do
    v = Maven::Ruby::VERSION
    v += '-SNAPSHOT' if v =~ /[a-zA-Z]/
    "pkg/ruby-maven-#{v}.gem"
  end

  it 'pack the gem' do
    FileUtils.rm_f gem
    CatchStdout.exec do
      RubyMaven.exec( '-Dverbose', 'package' )
    end
    CatchStdout.result.must_match /mvn -Dverbose package/
    File.exists?( gem )
  end

end
