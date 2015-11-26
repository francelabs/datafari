logstash-output-kafka
====================

Apache Kafka output for Logstash. This output will produce messages to a Kafka topic using the producer API exposed by Kafka. 

For more information about Kafka, refer to this [documentation](http://kafka.apache.org/documentation.html) 

Information about producer API can be found [here](http://kafka.apache.org/documentation.html#apidesign)

Logstash Configuration
====================

See http://kafka.apache.org/documentation.html#producerconfigs for details about the Kafka producer options.

    output {
        kafka {
            topic_id => ... # string (required), The topic to produce the messages to
            broker_list => ... # string (optional), default: "localhost:9092", This is for bootstrapping and the producer will only use it for getting metadata
            compression_codec => ... # string (optional), one of ["none", "gzip", "snappy"], default: "none"
            compressed_topics => ... # string (optional), default: "", This parameter allows you to set whether compression should be turned on for particular
            request_required_acks => ... # number (optional), one of [-1, 0, 1], default: 0, This value controls when a produce request is considered completed
            serializer_class => ... # string, (optional) default: "kafka.serializer.StringEncoder", The serializer class for messages. The default encoder takes a byte[] and returns the same byte[]
            partitioner_class => ... # string (optional) default: "kafka.producer.DefaultPartitioner"
            request_timeout_ms => ... # number (optional) default: 10000
            producer_type => ... # string (optional), one of ["sync", "async"] default => 'sync'
            key_serializer_class => ... # string (optional) default: kafka.serializer.StringEncoder
            message_send_max_retries => ... # number (optional) default: 3
            retry_backoff_ms => ... # number (optional) default: 100
            topic_metadata_refresh_interval_ms => ... # number (optional) default: 600 * 1000
            queue_buffering_max_ms => ... # number (optional) default: 5000
            queue_buffering_max_messages => ... # number (optional) default: 10000
            queue_enqueue_timeout_ms => ... # number (optional) default: -1
            batch_num_messages => ... # number (optional) default: 200
            send_buffer_bytes => ... # number (optional) default: 100 * 1024
            client_id => ... # string (optional) default: ""
            partition_key_format => ... # string (optional) default: nil, Provides a way to specify a partition key as a string
        }
    }

The default codec is json for outputs.  If you select a codec of plain, logstash will encode your messages with not only the message
but also with a timestamp and hostname.  If you do not want anything but your message passing through, you should make
the output configuration something like:

    output {
        kafka {
            codec => plain {
                format => "%{message}"
            }
        }
    }
    

Dependencies
====================

* Apache Kafka version 0.8.1.1
* jruby-kafka library
