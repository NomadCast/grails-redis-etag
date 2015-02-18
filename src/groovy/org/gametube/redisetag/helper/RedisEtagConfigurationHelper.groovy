package org.gametube.redisetag.helper

/**
 * Created by tamer on 15/02/15.
 */
class RedisEtagConfigurationHelper {
	/**
	 * If the specified connection exists, use it. Otherwise use only base parameters
	 * @return a Map instance with config info if any, an empty Map otherwise.
	 */
	static Map mergeConfigMapsForRedisConnections(application, String connectionToUse) {

		def redisConfigMap = application.config.grails.redis ?: [:]

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
	static void injectRedisETagMethods(def mainContext) {

		def gApp = mainContext.grailsApplication
		def etagSrv = mainContext.redisETagService

		def redisETagConfigMap = mergeConfigMapsForRedisConnections(gApp, gApp.config.grails.redisEtag.connectiontouse ?: 'eTag')

		// once we re-inject one service or property, we must re-inject all of them
		etagSrv.eTagStringPrefix = redisETagConfigMap.eTagStringPrefix ?: 'eTag:'
		// default ttl: 1 day
		etagSrv.defaultTTL = redisETagConfigMap.defaultTTL as Integer ?: 60 * 60 * 24

		etagSrv.enabled = redisETagConfigMap?.enabled == false ?: true
		etagSrv.redisService = mainContext."redisService${redisETagConfigMap.connectionToUse.capitalize()}"

		def clazzes = []
		clazzes += gApp.controllerClasses*.clazz
		clazzes += gApp.serviceClasses*.clazz
		clazzes.each { cls ->
			cls.metaClass.getRedisETag = { Map args ->
				etagSrv.getRedisETag(args.type, args.id)
			}
			cls.metaClass.evictRedisETag = { Map args ->
				etagSrv.evictRedisETag(args.type, args.id)
			}
		}
	}
}
