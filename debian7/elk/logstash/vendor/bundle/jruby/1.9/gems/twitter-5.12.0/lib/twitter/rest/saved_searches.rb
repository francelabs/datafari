require 'twitter/arguments'
require 'twitter/request'
require 'twitter/rest/utils'
require 'twitter/saved_search'
require 'twitter/utils'

module Twitter
  module REST
    module SavedSearches
      include Twitter::REST::Utils
      include Twitter::Utils

      # @rate_limited Yes
      # @authentication Requires user context
      # @raise [Twitter::Error::Unauthorized] Error raised when supplied user credentials are not valid.
      # @return [Array<Twitter::SavedSearch>] The saved searches.
      # @overload saved_search(options = {})
      #   Returns the authenticated user's saved search queries
      #
      #   @see https://dev.twitter.com/docs/api/1.1/get/saved_searches/list
      #   @param options [Hash] A customizable set of options.
      # @overload saved_search(*ids)
      #   Retrieve the data for saved searches owned by the authenticating user
      #
      #   @see https://dev.twitter.com/docs/api/1.1/get/saved_searches/show/:id
      #   @param ids [Enumerable<Integer>] A collection of saved search IDs.
      # @overload saved_search(*ids, options)
      #   Retrieve the data for saved searches owned by the authenticating user
      #
      #   @see https://dev.twitter.com/docs/api/1.1/get/saved_searches/show/:id
      #   @param ids [Enumerable<Integer>] A collection of saved search IDs.
      #   @param options [Hash] A customizable set of options.
      def saved_searches(*args)
        arguments = Twitter::Arguments.new(args)
        if arguments.empty?
          perform_with_objects(:get, '/1.1/saved_searches/list.json', arguments.options, Twitter::SavedSearch)
        else
          pmap(arguments) do |id|
            saved_search(id, arguments.options)
          end
        end
      end

      # Retrieve the data for saved searches owned by the authenticating user
      #
      # @see https://dev.twitter.com/docs/api/1.1/get/saved_searches/show/:id
      # @rate_limited Yes
      # @authentication Requires user context
      # @raise [Twitter::Error::Unauthorized] Error raised when supplied user credentials are not valid.
      # @return [Twitter::SavedSearch] The saved searches.
      # @param id [Integer] The ID of the saved search.
      # @param options [Hash] A customizable set of options.
      def saved_search(id, options = {})
        perform_with_object(:get, "/1.1/saved_searches/show/#{id}.json", options, Twitter::SavedSearch)
      end

      # Creates a saved search for the authenticated user
      #
      # @see https://dev.twitter.com/docs/api/1.1/post/saved_searches/create
      # @rate_limited No
      # @authentication Requires user context
      # @raise [Twitter::Error::Unauthorized] Error raised when supplied user credentials are not valid.
      # @return [Twitter::SavedSearch] The created saved search.
      # @param query [String] The query of the search the user would like to save.
      # @param options [Hash] A customizable set of options.
      def create_saved_search(query, options = {})
        perform_with_object(:post, '/1.1/saved_searches/create.json', options.merge(:query => query), Twitter::SavedSearch)
      end
      deprecate_alias :saved_search_create, :create_saved_search

      # Destroys saved searches for the authenticated user
      #
      # @see https://dev.twitter.com/docs/api/1.1/post/saved_searches/destroy/:id
      # @note The search specified by ID must be owned by the authenticating user.
      # @rate_limited No
      # @authentication Requires user context
      # @raise [Twitter::Error::Unauthorized] Error raised when supplied user credentials are not valid.
      # @return [Array<Twitter::SavedSearch>] The deleted saved searches.
      # @overload destroy_saved_search(*ids)
      #   @param ids [Enumerable<Integer>] A collection of saved search IDs.
      # @overload destroy_saved_search(*ids, options)
      #   @param ids [Enumerable<Integer>] A collection of saved search IDs.
      #   @param options [Hash] A customizable set of options.
      def destroy_saved_search(*args)
        arguments = Twitter::Arguments.new(args)
        pmap(arguments) do |id|
          perform_with_object(:post, "/1.1/saved_searches/destroy/#{id}.json", arguments.options, Twitter::SavedSearch)
        end
      end
      deprecate_alias :saved_search_destroy, :destroy_saved_search
    end
  end
end
