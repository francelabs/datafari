require 'twitter/arguments'
require 'twitter/cursor'
require 'twitter/rest/utils'
require 'twitter/tweet'
require 'twitter/user'

module Twitter
  module REST
    module Undocumented
      include Twitter::REST::Utils

      # @note Undocumented
      # @rate_limited Yes
      # @authentication Requires user context
      # @raise [Twitter::Error::Unauthorized] Error raised when supplied user credentials are not valid.
      # @return [Twitter::Cursor]
      # @overload following_followers_of(options = {})
      #   Returns users following followers of the specified user
      #
      #   @param options [Hash] A customizable set of options.
      # @overload following_followers_of(user, options = {})
      #   Returns users following followers of the authenticated user
      #
      #   @param user [Integer, String, Twitter::User] A Twitter user ID, screen name, URI, or object.
      #   @param options [Hash] A customizable set of options.
      def following_followers_of(*args)
        cursor_from_response_with_user(:users, Twitter::User, :get, '/users/following_followers_of.json', args)
      end

      # Returns Tweets count for a URI
      #
      # @note Undocumented
      # @rate_limited No
      # @authentication Not required
      # @return [Integer]
      # @param uri [String, URI] A URI.
      # @param options [Hash] A customizable set of options.
      def tweet_count(uri, options = {})
        connection = Faraday.new('https://cdn.api.twitter.com', connection_options.merge(:builder => middleware))
        connection.get('/1/urls/count.json', options.merge(:url => uri.to_s)).body[:count]
      end
    end
  end
end
