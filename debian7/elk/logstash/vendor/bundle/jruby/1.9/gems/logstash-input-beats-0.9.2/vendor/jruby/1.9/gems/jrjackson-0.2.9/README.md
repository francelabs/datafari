

LICENSE applicable to this library:

Apache License 2.0 see http://www.apache.org/licenses/LICENSE-2.0

### JrJackson:

a jruby library wrapping the JAVA Jackson jars`

__NOTE:__ Smile support has been temporarily dropped

The code has been refactored to use almost all Java.

There is now a MultiJson adapter added for JrJackson

***

#### NEWS

11th May 2014 - Added to_time method call for Ruby object serialization

26th October 2013 - Added support to serialize arbitary (non JSON datatypes)
ruby objects.  Normally the toJava internal method is called, but additionally
to_h, to_hash, to_a and finally to_json are tried.  Be aware that the to_json
method might invoke a new selialization session and impact performance. 

***

#### API

```
JrJackson::Json.load(string, options) -> hash like object
      aliased as parse
```
By default the load method will return Ruby objects (Hashes have string keys).
The options hash respects three symbol keys

+ :symbolize_keys

  Will return symbol keys in hashes

+ :raw

  Will return JRuby wrapped java objects that quack like ruby objects
  This is the fastest option

+ :use_bigdecimal

  Will return BigDecimal objects instead of Float
  If used with the :raw option you will get Java::JavaMath::BigDecimal objects
  otherwise they are Ruby BigDecimal

```
JrJackson::Json.dump(obj) -> json string
      aliased as generate
```
The dump method expects that the values of hashes or arrays are JSON data types,
the only exception to this is Ruby Symbol as values, they are converted to java strings
during serialization. __NOTE:__ All other objects should be converted to JSON data types before
serialization. See the wiki for more on this.

***

#### Internals

There are two Ruby sub modules of the JrJackson module

```JrJackson::Json```, this is the general external facade used by MultiJson, and is pure Ruby.

```JrJackson::Raw```, this is used by the Json module, it is defined in Java with annotations
exposing it as a Ruby module with module methods.

***

#### Benchmarks

Credit to Chuck Remes for the benchmark and initial
investigation when the jruby, json gem and the jackson
libraries were young.

I compared Json (java) 1.8, Gson 0.6.1 and jackson 2.2.3 on jruby 1.7.5 and OpenJDK 64-Bit Server VM 1.7.0_25-b30
All the benchmarks were run separately. A 727.9KB string of random json data is read from a file and handled 250 times, thereby attempting to balance invocation and parsing benchmarking.

```
generation/serialize

                                               user     system      total         real
json mri generate: 250                        12.02       0.00      12.02     ( 12.022)
oj mri generate: 250                           7.18       0.00       7.18     (  7.183)
json java generate: 250                        7.83       0.01       7.84     (  7.289)
gson generate: 250                             5.04       0.00       5.04     (  4.995)
jackson generate: 250                          4.94       0.08       5.02     (  4.811)
jackson generate: 250                          4.85       0.04       4.89     (  4.758)


parsing/deserialize - after jrjackson parsing profiling

                                               user     system      total         real
json mri parse: 250                            8.35       0.02       8.37     (  8.366)
oj mri parse: 250                              6.10       0.13       6.23     (  7.527)
      
gson parse: 250                               12.02       0.02      12.04     ( 11.774)
json java parse: 250                          10.35       0.01      10.36     ( 10.204)
jackson parse string keys: 250                 5.60       0.00       5.60     (  5.356)
jackson parse string + bigdecimal: 250         5.14       0.01       5.15     (  4.946)
jackson parse symbol keys: 250                 4.16       0.01       4.17     (  3.989)
jackson parse symbol + bigdecimal: 250         3.90       0.03       3.93     (  3.597)
jackson parse raw: 250                         3.08       0.01       3.09     (  2.924)
jackson parse raw + bigdecimal: 250            2.70       0.06       2.76     (  2.493)

```
