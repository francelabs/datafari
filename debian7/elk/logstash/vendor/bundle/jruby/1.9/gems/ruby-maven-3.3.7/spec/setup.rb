$LOAD_PATH << File.expand_path( '../../lib', __FILE__ )
begin
  require 'minitest'
rescue LoadError
end
require 'minitest/autorun'
