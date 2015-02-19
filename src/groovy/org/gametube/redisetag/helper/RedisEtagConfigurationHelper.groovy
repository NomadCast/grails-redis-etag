package org.gametube.redisetag.helper

import org.gametube.redisetag.service.RedisETagService

/**
 * @author tamer
 */
class RedisEtagConfigurationHelper {

	static final String DEFAULT_ETAG_STRING_PREFIX = 'eTag:'
	static final int DEFAULT_TTL = 60 * 60 * 24  // 1 day

	/**
	 * If the specified connection exists, use it. Otherwise use only base parameters
	 * @return a Map instance with config info if any, an empty Map otherwise.
	 */
	static Map mergeConfigMapsForRedisConnections(application, String connectionToUse) {

		def redisConfigMap = application.config.grails.redis ?: [connections: [:].withDefault { [:] }]

		if (!redisConfigMap.connections[connectionToUse]) {
			connectionToUse = ''
		}

		redisConfigMap.connectionToUse = connectionToUse
		return redisConfigMap + redisConfigMap.connections[connectionToUse]
	}

	/**
	 * Injects ETag handling methods: getRedisETag and evictRedisETag in
	 * controllers and services
	 *
	 * @param mainContext bean with application's main Context
	 */
	static void injectRedisETagMethods(mainContext) {

		def gApp = mainContext.grailsApplication
		RedisETagService etagSrv = mainContext.redisETagService

		def redisETagConfigMap = mergeConfigMapsForRedisConnections(gApp, gApp.config.grails.redisEtag.connectiontouse ?: 'eTag')

		// once we re-inject one service or property, we must re-inject all of them
		String eTagStringPrefix = redisETagConfigMap.eTagStringPrefix ?: DEFAULT_ETAG_STRING_PREFIX
		int defaultTTL = redisETagConfigMap.defaultTTL as Integer ?: DEFAULT_TTL
		boolean enabled = redisETagConfigMap?.enabled == false ?: true
		def redisService = mainContext."redisService${redisETagConfigMap.connectionToUse.capitalize()}"
		etagSrv.configure(eTagStringPrefix, defaultTTL, enabled, redisService)

		def metaClasses = []
		metaClasses.addAll gApp.controllerClasses*.clazz*.metaClass
		metaClasses.addAll gApp.serviceClasses*.clazz*.metaClass
		metaClasses.each { mc ->
			mc.getRedisETag = { Map args ->
				etagSrv.getRedisETag(args.type, args.id)
			}
			mc.evictRedisETag = { Map args ->
				etagSrv.evictRedisETag(args.type, args.id)
			}
		}
	}
}
