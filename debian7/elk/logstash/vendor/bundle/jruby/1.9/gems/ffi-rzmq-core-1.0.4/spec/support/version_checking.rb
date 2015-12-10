# Helper methods for figuring out which version of Ruby / ZMQ we are
# testing and working with.
module VersionChecking
  def jruby?
    RUBY_PLATFORM =~ /java/
  end
end
