package org.gametube.redisetag.service

import grails.plugin.redis.RedisService
import org.joda.time.DateTimeUtils

/**
 * Service providing the basic cache, update and evict functionality for etags stored in redis
 */
class RedisCacheETagService {
	static transactional = false

	String eTagStringPrefix = 'eTag:'
	Integer defaultTTL = 60 * 60 * 24  //1 day
	Boolean enabled = true
	RedisService redisService

	/**
	 * Returns the ETag stored in Redis for a given @objectName instance with id @objectIdentifier.
	 * If it is not yet stored or it has expired, a new key/value pair will be created in Redis.
	 *
	 * @param objectName a class that belongs to Domain Class set
	 * @param objectIdentifier the id of the instance to retrieve / create in redis
	 * @return the hashCode of the value stored in redis. If the plugin is disabled a random UUID.
	 */
	String getRedisETag(String objectName, String objectIdentifier) {
		if (!enabled) {
			return UUID.randomUUID()
		}
		String key = createETagKey(objectName, objectIdentifier)
		String cachedValue = redisService.get(key)
		if (!cachedValue) {
			cachedValue = createETagValue(objectName, objectIdentifier)
			redisService.setex(key, defaultTTL, cachedValue)
		}
		return cachedValue
	}

	/**
	 * Evict the entry from the cache.
	 * @param objectName a String containing the name of the object to evict from redis
	 * @param objectIdentifier the id of the object to evict from redis
	 */
	void evictRedisETag(String objectName, String objectIdentifier) {
		if (enabled) {
			redisService.expire(createETagKey(objectName, objectIdentifier), 0)
		}
	}

	/**
	 * Utility method that creates the key used to manipulate the etag entries in redis
	 * @param objectName
	 * @param objectIdentifier the id of the object
	 * @return the key created
	 */
	private String createETagKey(String objectName, String objectIdentifier) {
		return eTagStringPrefix + "${objectName}=${objectIdentifier}"
	}

	/**
	 * Utility method that creates a unique ETag value based on:
	 * -objectName
	 * -objectIdentifier
	 * -current millisecond
	 *
	 * @param objectName
	 * @param objectIdentifier
	 * @return a String
	 */
	private String createETagValue(String objectName, String objectIdentifier) {
		return "${objectName}:${objectIdentifier}:${DateTimeUtils.currentTimeMillis()}".encodeAsMD5()
	}

}
