require 'twitter/creatable'
require 'twitter/entities'
require 'twitter/identity'

module Twitter
  class Tweet < Twitter::Identity
    include Twitter::Creatable
    include Twitter::Entities
    # @return [String]
    attr_reader :filter_level, :in_reply_to_screen_name, :lang, :source, :text
    # @return [Integer]
    attr_reader :favorite_count, :in_reply_to_status_id, :in_reply_to_user_id,
                :retweet_count
    deprecate_alias :favorites_count, :favorite_count
    deprecate_alias :favoriters_count, :favorite_count
    alias_method :in_reply_to_tweet_id, :in_reply_to_status_id
    alias_method :reply?, :in_reply_to_user_id?
    deprecate_alias :retweeters_count, :retweet_count
    object_attr_reader :GeoFactory, :geo
    object_attr_reader :Metadata, :metadata
    object_attr_reader :Place, :place
    object_attr_reader :Tweet, :retweeted_status
    alias_method :retweeted_tweet, :retweeted_status
    alias_method :retweet?, :retweeted_status?
    alias_method :retweeted_tweet?, :retweeted_status?
    object_attr_reader :User, :user, :status
    predicate_attr_reader :favorited, :possibly_sensitive, :retweeted,
                          :truncated

    # @note May be > 140 characters.
    # @return [String]
    def full_text
      if retweet?
        prefix = text[/\A(RT @[a-z0-9_]{1,20}: )/i, 1]
        [prefix, retweeted_status.text].compact.join
      else
        text
      end
    end
    memoize :full_text

    # @return [Addressable::URI] The URL to the tweet.
    def uri
      Addressable::URI.parse("https://twitter.com/#{user.screen_name}/status/#{id}") if user?
    end
    memoize :uri
    alias_method :url, :uri
  end
  Status = Tweet # rubocop:disable ConstantName
end
