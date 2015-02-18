package org.gametube.redisetag.service

import grails.plugin.redis.RedisService
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import org.joda.time.DateTimeUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(RedisETagService)
@TestMixin(ServiceUnitTestMixin)
class RedisETagServiceSpec extends Specification {


	RedisService redisService
	@Shared
	String prefix = 'ETagPrefix|'
	@Shared
	Integer defaultTTL = 100

	def setup() {
		service.redisService = redisService = Mock(RedisService)
		service.eTagStringPrefix = prefix
		service.defaultTTL = defaultTTL
	}

	def cleanup() {
	}

	@ConfineMetaClassChanges([DateTimeUtils, String])
	void "That generateETagValueForObject creates an ETag Value using given params and currentTime"() {
		given:
			String id = 'objId'
			String name = 'objName'
			String.metaClass.encodeAsMD5 = { return delegate }
			DateTimeUtils.metaClass.'static'.currentTimeMillis = {
				10000000l
			}
		when:
			def result = service.generateETagValueForObject(name, id)
		then:
			result == (name + ':' + id + ':' + 10000000l.toString()).encodeAsMD5()
	}

	void "That getETagKeyForObject creates the redis object key with the proper format"() {
		given:
			String name = 'objName'
			String id = 'objId'
		when:
			def result = service.getETagKeyForObject(name, id)
		then:
			result == "${prefix}${name}=${id}"
	}

	void "That evicEtag does nothing if the plugin is not enabled"() {
		given:
			service.enabled = false
		when:
			service.evictRedisETag('', '')
		then:
			noExceptionThrown()
			0 * _
	}

	void "That evicEtag correctly calls redis to evict the given obj"() {
		given:
			String name = 'objName'
			String id = 'objId'
		when:
			service.evictRedisETag(name, id)

		then:
			1 * redisService.methodMissing('expire', ["${prefix}${name}=${id}", 0])
	}

	void "That getETag returns a random UUID if the plugin is not enabled"() {
		given:
			String uuid = UUID.randomUUID()
			UUID.metaClass.'static'.randomUUID = {
				uuid
			}
			service.enabled = false
		when:
			def result = service.getRedisETag('', '')
		then:
			noExceptionThrown()
			0 * _
			result == uuid
	}

	void "That getEtag returns the ETag value if it's already stored in redis"() {
		given:
			String name = 'objName'
			String id = 'objId'
		when:
			def result = service.getRedisETag(name, id)
		then:
			1 * redisService.methodMissing('get', ["${prefix}${name}=${id}"]) >> { 'ETagValueInRedis' }
			0 * _
			result == 'ETagValueInRedis'
	}

	@ConfineMetaClassChanges([DateTimeUtils, String])
	void "That getEtag generates and returns the ETag value if it's NOT already stored in redis"() {
		given:
			String name = 'objName'
			String id = 'objId'
			String.metaClass.encodeAsMD5 = { return delegate }
			DateTimeUtils.metaClass.'static'.currentTimeMillis = {
				10000000l
			}
			String expectedValue = (name + ':' + id + ':' + 10000000l.toString()).encodeAsMD5()

		when:
			def result = service.getRedisETag(name, id)
		then:
			1 * redisService.methodMissing('get', ["${prefix}${name}=${id}"]) >> null
			1 * redisService.methodMissing('setex', ["${prefix}${name}=${id}", defaultTTL, expectedValue])
			0 * _
			noExceptionThrown()
			result == expectedValue
	}
}
