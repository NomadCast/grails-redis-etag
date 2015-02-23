Grails Redis ETag Plugin
==================================

This plugin is intended to work in pair with the [Grails cache-headers plugin], in particular it's providing a solution to generate, store and retrieve ETag values in Redis, avoiding DB hits. Redis is thus used as a central cache for ETags, and the plugin can be used by multiple instances of the same application that will share the same "ETag repository".

Installation
------------
Dependency :

    compile ':redis-etag:1.0'

In order to access the Redis server where ETags are stored, the plugin uses the configuration of the [Grails redis plugin].

Typically, you'd have something like this in your `grails-app/conf/Config.groovy` file:

    grails.redisEtag.connectiontouse = 'eTag' // not mandatory. If not declared 'eTag' is the default value
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
                    //enabled = false // ETag enabled by default
                    database = 2
                    host = 'localhost'  // will override the base one
                    defaultTTL = 60 * 60 * 24 // seconds (default is one day)
                    eTagStringPrefix = 'eTag:' // prefix used in Redis keys (default is 'eTag:')
                }
            }
        }
    }

There are two additional entries in the configuration respect a standard Redis plugin configuration:

 * `defaultTTL`: indicates the amount of seconds to use as TTL for ETag keys in Redis
 * `eTagStringPrefix`: indicates the prefix that will be used for ETag keys in Redis

There is also a specific configuration key that allows selecting which Redis connection to use:

 * `grails.redisEtag.connectiontouse`: the Redis connection that the plugin must use. Can be any of those specified in the redis plugin configuration. If not found, 'eTag' is the default one (and, if there is no 'eTag' connection, the base connection will be used.)

Plugin Usage
------------

## Operations and typical usage ##

As explained below, the plugin can be used in several ways (injected methods/service), but the set of operations and parameters available are always the same. The plugin provides operations:

 * `getRedisETag(String objectType, String objectIdentifier)`: retrieves the ETag value associated with the given object ID and type.
 * `evictRedisETag(String objectType, String objectIdentifier)`: retrieves the ETag value associated with the given object ID and type.

The `objectType` parameter allows separating the entities by a 'type', while the `objectIdentifier` allows distinguishing individual objects of the same type.

Typically, a Domain Class name (e.g, 'Book') will be used as type, and a Domain Class instance ID will be used as identifier. Thus, the `getRedisETag()` operation will be called inside the `etag` (provided by the [Grails cache-headers plugin]) block of a controller action so the ETag associated with a given Domain Class instance is retrieved from Redis without hitting the DB, and the `evictRedisETag()` operation will be called from the code (typically a service) that updates or deletes the instances of that Domain Class.


### Controllers/Services dynamic methods ###

At application startup/reload each Grails controller and service gets injected the two operations described above with the same names (`getRedisETag()` and `evictRedisETag()`).

Here is an example of usage along with the [Grails cache-headers plugin]:

    def index(Long id)

        withCacheHeaders {
            etag {
                getRedisETag(name: 'Book', id: id as String)
            }
            generate {
                Book book = Book.get(id)
                render(view:'bookDisplay', model:[book:book])
            }
        }
    }

### RedisETagService Bean ###

    def redisETagService

The `redisETagService` has 2 methods corresponding to the 2 operations described above.

This service is a standard Spring bean that can be injected and used in controllers, services, etc.

Examples:

    String myBookETag = redisETagService.getRedisETag('Book', 3)

    redisETagService.evictRedisETag('book', 3)


Release Notes
=============

* 1.0   - released 23/02/2015 - this is the first released revision of the plugin.

Credits
=======

This plugin is sponsored by <b>[GameTube]</b>.


[Grails cache-headers plugin]: http://grails.org/plugin/cache-headers
[Grails redis plugin]: http://grails.org/plugin/redis
[GameTube]: http://www.gametube.org/
