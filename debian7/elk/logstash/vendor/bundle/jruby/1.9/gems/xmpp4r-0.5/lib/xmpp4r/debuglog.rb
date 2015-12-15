# =XMPP4R - XMPP Library for Ruby
# License:: Ruby's license (see the LICENSE file) or GNU GPL, at your option.
# Website::http://home.gna.org/xmpp4r/

require 'logger'

module Jabber
  def Jabber::logger
    @@logger ||= Logger.new($stderr)
  end
  
  # Set the logger to use for debug and warn (if enabled)
  def Jabber::logger=(logger)
    @@logger = logger
  end

  # Is debugging mode enabled ?
  @@debug = false

  # Is warnings mode enabled ?
  @@warnings = false

  # Enable/disable debugging mode. When debug mode is enabled, information
  # can be logged using Jabber::debuglog. When debug mode is disabled, calls
  # to Jabber::debuglog are just ignored.
  def Jabber::debug=(debug)
    @@debug = debug
    if @@debug
      debuglog('Debugging mode enabled.')
      #if debug is enabled, we should automatically enable warnings too
      Jabber::warnings = true
    end
  end

  # Enable/disable warnings mode.
  def Jabber::warnings=(warnings)
    @@warnings = warnings
    if @@warnings
      warnlog('Warnings mode enabled.')
    end
  end

  # returns true if debugging mode is enabled. If you just want to log
  # something if debugging is enabled, use Jabber::debuglog instead.
  def Jabber::debug
    @@debug
  end

  # Outputs a string only if debugging mode is enabled. If the string includes
  # several lines, 4 spaces are added at the beginning of each line but the
  # first one. Time is prepended to the string.
  def Jabber::debuglog(string)
    return if not @@debug
    logger.debug string.chomp.gsub("\n", "\n    ")
  end
  
  # Outputs a string only if warnings mode is enabled.
  def Jabber::warnlog(string)
    return if not @@warnings
    logger.warn string.chomp.gsub("\n", "\n    ")
  end
  
end
