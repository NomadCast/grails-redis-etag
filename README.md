Grails Redis ETAG Plugin
==================================

This plugin is an alternative to [redis-cache-plugin]. It is better than [redis-cache-plugin] because it gives the possibility to set the expire time in seconds
for every cached key, and provides a service, annotations and injected methods to perform entry caching and eviction.
The [redis-plugin] plugin also provides the possibility to set a TTL for a cached entry (using the provided `@Memoize` annotation), but it lacks the option to serialize
any kind of Serializable objects: you are forced to be fully aware of what is being saved into cache each time (in [redis-plugin] you can save Domain object, list of
Domain objects, List of POGOs, Scores, ecc, but not mix of that objects).

With this plugin you save *everything* you need into cache:
* if it is a domain object the plugin will save its id in the cache and rehydrate it (if requested) at each cache hit.
* if it is a plain object the plugin will cache it as it is.
* if it is a Collection or a Map instance the plugin will iterate over it, preserving keys and/or order, and check what
to save to the cache. For example you can have a Map with POGOs and Domain Classes: the plugin will save them accordingly.

This means that potentially a <b>lot</b> of data will go to redis, so pay attention to memory and bandwidth consumption!

This plugin is not an extension of [cache-plugin] plugin, it is far more simple and lighter at the same time.
The [cache-plugin] gives a deep integration with grails Controller CoC mechanism, but I think it creates too much overhead sometimes.

The cache implementation provided by this plugin is inspired by [redis-cache-plugin] and [redis-plugin] but is not based on them.
This plugins depends on [redis-plugin] for communication with redis and therefore it uses its configuration DSL.

Installation
------------
Dependency :

    compile ':redis-etag:0.1'

In order to access the redis server where cached entries are stored, the plugin uses the configuration of the [redis-plugin]. 
Typically, you'd have something like this in your `grails-app/conf/Config.groovy` file:

    grails.redisflexiblecache.connectiontouse = 'eTag' // not mandatory. If not declared 'cache' is the default value
    grails {  // example of configuration
        redis {
            poolConfig {
                // jedis pool specific tweaks here, see jedis docs & src
                // ex: testWhileIdle = true
            }
            database = 1          // each other grails env can have a private one
            port = 6379
            host = 'localhost'
            timeout = 2000 // default in milliseconds
            password = '' // defaults to no password
            connections {
                eTag {
                    //enabled = false // cache enabled by default
                    database = 2
                    host = 'localhost'  // will override the base one
                    defaultTTL = 10 * 60 // seconds (used only if no ttl are declared in the annotation/map and no expireMap is defined
                    expireMap = [ // values in seconds
                            never: -1, // negative values mean do not set any TTL
                            low: 10 * 60,
                            mid_low: 5 * 60,
                            mid: 2 * 60,
                            high: 1 * 60
                    ]
                }
            }
        }
    }

There are two additional entries in the configuration respect a standard redis plugin configuration:
 * `defaultTTL`: indicates the amount of seconds to use as TTL when no other TTL value is provided explicitly
 * `expireMap`: provides names and values (in seconds) for TTL presets that can be used in annotations/method calls

There is also a specific configuration key that allows selecting which redis connection to use:
 * `grails.redisflexiblecache.connectiontouse` : the redis connection that the plugin must use. Can be 'cache' or any other specified in the redis plugin configuration. If not found, 'cache' is the default one. If there is no 'cache' connection, the base connection will be used.

Plugin Usage
------------

## Parameters ##


## What to Cache ##

### RedisCacheETagService Bean ###

    def redisCacheETagService

The `redisFlexibleCacheService` has 2 methods: 
 * `doCache`: stores/retrieves an object from the cache using a given key.
 * `evictCache` evicts a key (or several if the key contains a wildcard) and its value from the cache.

This service is a standard Spring bean that can be injected and used in controllers, services, etc.

Example:
    
    redisFlexibleCacheService.doCache('some:key:1', 'someGroup', 120, true, {
        return somethingToCache
    })

    redisFlexibleCacheService.evictCache( 'some:key:*', {
                log.debug('evicted :' + keys);
    })

### Controllers/Services dynamic methods ###

At application startup/reload each Grails controller and service gets injected the two methods provided from redisFlexibleCacheService with the following names:
 * `cache`
 * `evictCache`

Here is an example of usage:

    def index(){
      //params validation, ecc...
      def res = redisFlexibleCache group: 'longLasting', key: 'key:1', reAttachToSession: true, {
                  def bookList = // long lasting algortitm that returns a collection of domain class instances
                  def calculated = // long lasting algoritm that returns an Integer  
                  [book: bookList, statistics: calculated]
              }
       // ..
       return [result: res]
    }


Release Notes
=============

* 0.1   - released 15/02/2015 - this is the first released revision of the plugin.

Credits
=======

This plugin is sponsored by <b>[GameTube]</b>.


[cache-headers-plugin]: http://grails.org/plugin/cache-headers
[redis]: http://redis.io
[GameTube]: http://www.gametube.org/
