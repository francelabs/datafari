# encoding: utf-8
# This file is part of ruby-flores.
# Copyright (C) 2015 Jordan Sissel
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# :nodoc:
require "flores/namespace"

# The root of the rspec helpers the Flores library provides
module Flores::RSpec
  DEFAULT_ITERATIONS = 1..1000

  # Sets up rspec with the Flores RSpec helpers. Usage looks like this:
  #
  #     RSpec.configure do |config|
  #       Flores::RSpec.configure(config)
  #     end
  def self.configure(rspec_configuration)
    require "flores/rspec/stress"
    require "flores/rspec/analyze"
    rspec_configuration.extend(Flores::RSpec::Stress)
    rspec_configuration.extend(Flores::RSpec::Analyze)
  end # def self.configure

  def self.iterations
    return @iterations if @iterations
    if ENV["ITERATIONS"]
      @iterations = 0..ENV["ITERATIONS"].to_i
    else
      @iterations = DEFAULT_ITERATIONS
    end
    @iterations
  end
end # def Flores::RSpec
