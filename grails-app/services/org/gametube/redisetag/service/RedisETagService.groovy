package org.gametube.redisetag.service

import grails.plugin.redis.RedisService
import org.joda.time.DateTimeUtils

/**
 * Service providing the basic cache, update and evict functionality for etags
 * stored in Redis
 */
class RedisETagService {
	static transactional = false

	String eTagStringPrefix = 'eTag:'
	Integer defaultTTL = 60 * 60 * 24  // 1 day
	Boolean enabled = true
	RedisService redisService

	/**
	 * Returns the ETag stored in Redis for an object of the type @objectType
	 * and with with ID @objectIdentifier.
	 * If it is not yet stored or it has expired, a new key/value pair will be
	 * created in Redis.
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
	 * Evict and entry from the cache.
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 */
	void evictRedisETag(String objectType, String objectIdentifier) {
		if (enabled) {
			redisService.expire(getETagKeyForObject(objectType, objectIdentifier), 0)
		}
	}

	/**
	 * Utility method that creates the key used to manipulate the etag entries
	 * in Redis
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 * @return a String containing the created key
	 */
	private String getETagKeyForObject(String objectName, String objectIdentifier) {
		return eTagStringPrefix + "${objectName}=${objectIdentifier}"
	}

	/**
	 * Utility method that generates a unique ETag value based on the given
	 * @objectName and @objectIdentifier, as well as the current millisecond
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 * @return a String containing the created value
	 */
	private String generateETagValueForObject(String objectName, String objectIdentifier) {
		return "${objectName}:${objectIdentifier}:${DateTimeUtils.currentTimeMillis()}".encodeAsMD5()
	}

}
