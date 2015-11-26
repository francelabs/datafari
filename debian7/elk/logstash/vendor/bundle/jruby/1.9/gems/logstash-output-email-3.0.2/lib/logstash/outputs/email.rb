# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"

# Send email when an output is received. Alternatively, you may include or
# exclude the email output execution using conditionals. 
class LogStash::Outputs::Email < LogStash::Outputs::Base

  config_name "email"

  # The fully-qualified email address to send the email to.
  #
  # This field also accepts a comma-separated string of addresses, for example: 
  # `"me@host.com, you@host.com"`
  #
  # You can also use dynamic fields from the event with the `%{fieldname}` syntax.
  config :to, :validate => :string, :required => true

  # The fully-qualified email address for the From: field in the email.
  config :from, :validate => :string, :default => "logstash.alert@nowhere.com"

  # The fully qualified email address for the Reply-To: field.
  config :replyto, :validate => :string

  # The fully-qualified email address(es) to include as cc: address(es).
  #
  # This field also accepts a comma-separated string of addresses, for example: 
  # `"me@host.com, you@host.com"`
  config :cc, :validate => :string

  # How Logstash should send the email, either via SMTP or by invoking sendmail.
  config :via, :validate => :string, :default => "smtp"

  # The address used to connect to the mail server
  config :address, :validate => :string, :default => "localhost"

  # Port used to communicate with the mail server
  config :port, :validate => :number, :default => 25

  # Domain used to send the email messages
  config :domain, :validate => :string, :default => "localhost"

  # Username to authenticate with the server
  config :username, :validate => :string

  # Password to authenticate with the server
  config :password, :validate => :string

  # Authentication method used when identifying with the server
  config :authentication, :validate => :string

  # Enables TLS when communicating with the server
  config :use_tls, :validate => :boolean, :default => false

  # Run the mail relay in debug mode
  config :debug, :validate => :boolean, :default => false

  # Subject: for the email.
  config :subject, :validate => :string, :default => ""

  # Body for the email - plain text only.
  config :body, :validate => :string, :default => ""

  # HTML Body for the email, which may contain HTML markup.
  config :htmlbody, :validate => :string, :default => ""

  # Attachments - specify the name(s) and location(s) of the files.
  config :attachments, :validate => :array, :default => []

  # contenttype : for multipart messages, set the content-type and/or charset of the HTML part.
  # NOTE: this may not be functional (KH)
  config :contenttype, :validate => :string, :default => "text/html; charset=UTF-8"

  public
  def register
    require "mail"

    options = {
      :address              => @address,
      :port                 => @port,
      :domain               => @domain,
      :user_name            => @username,
      :password             => @password,
      :authentication       => @authentication,
      :enable_starttls_auto => @use_tls,
      :debug                => @debug
    }

    if @via == "smtp"
      Mail.defaults do
        delivery_method :smtp, options
      end
    elsif @via == 'sendmail'
      Mail.defaults do
        delivery_method :sendmail
      end
    else
      Mail.defaults do
        delivery_method :@via, options
      end
    end # @via tests
    @logger.debug("Email Output Registered!", :config => options, :via => @via)
  end # def register

  public
  def receive(event)
    

      @logger.debug? and @logger.debug("Creating mail with these settings : ", :via => @via, :options => @options, :from => @from, :to => @to, :cc => @cc, :subject => @subject, :body => @body, :content_type => @contenttype, :htmlbody => @htmlbody, :attachments => @attachments, :to => to, :to => to)
      formatedSubject = event.sprintf(@subject)
      formattedBody = event.sprintf(@body)
      formattedHtmlBody = event.sprintf(@htmlbody)
      mail = Mail.new
      mail.from = event.sprintf(@from)
      mail.to = event.sprintf(@to)
      if @replyto
        mail.reply_to = event.sprintf(@replyto)
      end
      mail.cc = event.sprintf(@cc)
      mail.subject = formatedSubject
      if @htmlbody.empty?
        formattedBody.gsub!(/\\n/, "\n") # Take new line in the email
        mail.body = formattedBody
      else
        mail.text_part = Mail::Part.new do
          content_type "text/plain; charset=UTF-8"
          formattedBody.gsub!(/\\n/, "\n") # Take new line in the email
          body formattedBody
        end
        mail.html_part = Mail::Part.new do
          content_type "text/html; charset=UTF-8"
          body formattedHtmlBody
        end
      end
      @attachments.each do |fileLocation|
        mail.add_file(fileLocation)
      end # end @attachments.each
      @logger.debug? and @logger.debug("Sending mail with these values : ", :from => mail.from, :to => mail.to, :cc => mail.cc, :subject => mail.subject)
      begin
        mail.deliver!
      rescue StandardError => e
        @logger.error("Something happen while delivering an email", :exception => e)
        @logger.debug? && @logger.debug("Processed event: ", :event => event)
      end
  end # def receive
end # class LogStash::Outputs::Email
