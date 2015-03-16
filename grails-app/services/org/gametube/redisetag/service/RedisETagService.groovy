package org.gametube.redisetag.service

import grails.plugin.redis.RedisService

import org.gametube.redisetag.helper.RedisEtagConfigurationHelper
import org.joda.time.DateTimeUtils
import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline

import java.util.regex.Pattern

/**
 * Provides the basic cache, update and evict functionality for etags stored in Redis.
 */
class RedisETagService {

	static transactional = false

	private String eTagStringPrefix = RedisEtagConfigurationHelper.DEFAULT_ETAG_STRING_PREFIX
	private int defaultTTL = RedisEtagConfigurationHelper.DEFAULT_TTL
	private boolean enabled = true
	private RedisService redisService

	static final Pattern WILDCARD_PATTERN = ~/.*[\*\?]+.*/

	/**
	 * Returns the ETag stored in Redis for an object of the type @objectType and
	 * with with ID @objectIdentifier. If it is not yet stored or it has expired, a
	 * new key/value pair will be created in Redis.
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 * @return the hashCode of the value stored in Redis. If the plugin is
	 * disabled, a random UUID.
	 */
	String getRedisETag(String objectType, String objectIdentifier) {
		if (!enabled) {
			return UUID.randomUUID()
		}
		String key = getETagKeyForObject(objectType, objectIdentifier)
		String cachedValue = redisService.get(key)
		if (!cachedValue) {
			cachedValue = generateETagValueForObject(objectType, objectIdentifier)
			redisService.setex(key, defaultTTL, cachedValue)
		}
		return cachedValue
	}

	/**
	 * Evict an entry from the cache. If the provided parameters contain wildcards, all matching keys will be expired.
	 *
	 * @param objectType the type of the object. May contain wildcards such as *
	 * @param objectIdentifier the ID of the object. May contain wildcards such as *
	 */
	void evictRedisETag(String objectType, String objectIdentifier) {
		if (!enabled) {
			return
		}

		String keyString = getETagKeyForObject(objectType, objectIdentifier)

		// get the keys to evict depending on whether the key is a wildcard or not
		Set keys = []
		if (keyString.matches(WILDCARD_PATTERN)) {
			redisService.withRedis { Jedis redis ->
				keys = redis.keys(keyString.bytes)
			}
			if (log.isDebugEnabled()) {
				log.debug("found ${keys.size()} keys for wildcard string '${keyString}'")
			}
		} else {
			keys.add(keyString.bytes)
		}

		// expire all the keys
		redisService.withPipeline { Pipeline pipeline ->
			keys.each { byte[] key ->
				pipeline.expire(key, 0) // setting expire time = 0 will work for keys already with ttl and the one without ttl
				if (log.isDebugEnabled()) {
					log.debug("evicted the key : ${new String(key)}")
				}
			}
		}
	}

	void configure(String eTagStringPrefix, int defaultTTL, boolean enabled, RedisService redisService) {
		this.eTagStringPrefix = eTagStringPrefix
		this.defaultTTL = defaultTTL
		this.enabled = enabled
		this.redisService = redisService
	}

	/**
	 * Creates the key used to manipulate the etag entries in Redis.
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 * @return the created key
	 */
	private String getETagKeyForObject(String objectName, String objectIdentifier) {
		return "${eTagStringPrefix}${objectName}=${objectIdentifier}"
	}

	/**
	 * Generates a unique ETag value based on the given @objectName and
	 * @objectIdentifier, as well as the current millisecond.
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 * @return the created value
	 */
	private String generateETagValueForObject(String objectName, String objectIdentifier) {
		return "${objectName}:${objectIdentifier}:${DateTimeUtils.currentTimeMillis()}".encodeAsMD5()
	}
}
