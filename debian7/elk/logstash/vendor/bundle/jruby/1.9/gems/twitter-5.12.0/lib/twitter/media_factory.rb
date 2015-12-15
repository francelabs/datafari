require 'twitter/factory'
require 'twitter/media/photo'

module Twitter
  class MediaFactory < Twitter::Factory
    class << self
      # Construct a new media object
      #
      # @param attrs [Hash]
      # @raise [IndexError] Error raised when supplied argument is missing a :type key.
      # @return [Twitter::Media]
      def new(attrs = {})
        super(:type, Media, attrs)
      end
    end
  end
end
