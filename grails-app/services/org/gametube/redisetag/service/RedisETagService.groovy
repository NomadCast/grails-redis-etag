package org.gametube.redisetag.service

import grails.plugin.redis.RedisService

import org.gametube.redisetag.helper.RedisEtagConfigurationHelper
import org.joda.time.DateTimeUtils

/**
 * Provides the basic cache, update and evict functionality for etags stored in Redis.
 */
class RedisETagService {

	static transactional = false

	private String eTagStringPrefix = RedisEtagConfigurationHelper.DEFAULT_ETAG_STRING_PREFIX
	private int defaultTTL = RedisEtagConfigurationHelper.DEFAULT_TTL
	private boolean enabled = true
	private RedisService redisService

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
	 * Evict an entry from the cache.
	 *
	 * @param objectType the type of the object
	 * @param objectIdentifier the ID of the object
	 */
	void evictRedisETag(String objectType, String objectIdentifier) {
		if (enabled) {
			redisService.expire(getETagKeyForObject(objectType, objectIdentifier), 0)
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
