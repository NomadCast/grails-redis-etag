package org.gametube.redisetag.service

import grails.plugin.redis.RedisService
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import org.joda.time.DateTimeUtils
import redis.clients.jedis.Client
import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import spock.lang.Shared
import spock.lang.Specification
import spock.util.mop.ConfineMetaClassChanges

@TestFor(RedisETagService)
@TestMixin(ServiceUnitTestMixin)
class RedisETagServiceSpec extends Specification {

	RedisService redisService
	@Shared
	String prefix = 'ETagPrefix|'
	@Shared
	int defaultTTL = 100

	def setup() {
		redisService = Mock(RedisService)
		service.configure(prefix, defaultTTL, true, redisService)
	}

	@ConfineMetaClassChanges([DateTimeUtils, String])
	void "That generateETagValueForObject creates an ETag Value using given params and currentTime"() {
		given:
			String id = 'objId'
			String name = 'objName'
			String.metaClass.encodeAsMD5 = { -> return delegate }
			DateTimeUtils.metaClass.static.currentTimeMillis = { ->
				10000000L
			}
		when:
			def result = service.generateETagValueForObject(name, id)
		then:
			result == (name + ':' + id + ':' + 10000000L).encodeAsMD5()
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

	void "That evictEtag does nothing if the plugin is not enabled"() {
		given:
			service.enabled = false
		when:
			service.evictRedisETag('', '')
		then:
			noExceptionThrown()
			0 * _
	}

	void "That evictEtag correctly calls redis to evict the given obj"() {
		given:
			String name = 'objName'
			String id = 'objId'
			Pipeline pipeline = Mock(Pipeline)
			def keyBytes ="${prefix}${name}=${id}".bytes
			Client client = Mock(Client)
		when:
			service.evictRedisETag(name, id)

		then:
			1 * pipeline.getResponse(_)
			1 * pipeline.getClient(keyBytes) >> {client}
			1 * client.expire(keyBytes,0)
			1 * redisService.withPipeline(_)>> { Closure cl ->
				cl.call(pipeline)
			}
			0 * _
	}

	void "That evictEtag correctly calls redis to evict all objects whom keys match the wildcards"() {
		given:
			String name = 'objName'
			String id = 'objId:page=*'
			Pipeline pipeline = Mock(Pipeline)
			Jedis redis = Mock(Jedis)
			def keyBytes ="${prefix}${name}=${id}".bytes
			Client client = Mock(Client)
			Set<byte[]> redisReturnedKeys = ['objId:page=1'.bytes,'objId:page=2'.bytes] as Set
		when:
			service.evictRedisETag(name, id)

		then:
			1 * redis.keys(keyBytes) >> {
				redisReturnedKeys
			}
			1 * redisService.withRedis(_) >> { Closure cl ->
				cl.call(redis)
			}
			2 * pipeline.getResponse(_)
			2 * pipeline.getClient(_) >> {client}
			1 * client.expire(redisReturnedKeys.first(),0)
			1 * client.expire(redisReturnedKeys.last(),0)
			1 * redisService.withPipeline(_)>> { Closure cl ->
				cl.call(pipeline)
			}
			0 * _
	}

	void "That getETag returns a random UUID if the plugin is not enabled"() {
		given:
			String uuid = UUID.randomUUID()
			UUID.metaClass.static.randomUUID = { ->
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
			String.metaClass.encodeAsMD5 = { -> return delegate }
			DateTimeUtils.metaClass.static.currentTimeMillis = { ->
				10000000L
			}
			String expectedValue = (name + ':' + id + ':' + 10000000L).encodeAsMD5()

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
