import org.gametube.redisetag.helper.RedisEtagConfigurationHelper

class RedisEtagGrailsPlugin {
	def version = "1.0"
	def grailsVersion = "2.0 > *"
	def title = "Redis Etag Plugin"
	def author = "Tamer Shahin"
	def authorEmail = "tamer.shahin@gmail.com "
	def description = '''\
Works in pair with the Grails cache-headers plugin, providing a solution to generate, store
and retrieve ETag values in Redis, avoiding DB hits. Redis is thus used as a central cache for
ETags, and the plugin can be used by multiple instances of the same application
that will share the same "ETag repository".
'''
	def documentation = "https://github.com/NomadCast/grails-redis-etag/blob/master/README.md"
	def license = "APACHE"
	def loadAfter = ['redis']
	def watchedResources = ['file:./grails-app/controllers/**', 'file:./grails-app/services/**']
	def organization = [name: "GameTube SAS", url: "http://www.gametube.org/"]
	def developers = [[name: "Germán Sancho", email: "german@gametube.org"]]
	def issueManagement = [system: "GITHUB", url: "https://github.com/NomadCast/grails-redis-etag/issues"]
	def scm = [url: "https://github.com/NomadCast/grails-redis-etag"]

	def doWithDynamicMethods = { ctx ->
		redisEtagCustomization(ctx)
	}

	def doWithApplicationContext = { ctx ->
		redisEtagCustomization(ctx)
	}

	def onChange = { event ->
		redisEtagCustomization(event.application.mainContext)
	}

	def onConfigChange = { event ->
		redisEtagCustomization(event.application.mainContext)
	}

	/**
	 * (Re-)inject the redis ETag methods in services and controllers
	 *
	 * @param mainContext application main context
	 */
	static void redisEtagCustomization(mainContext) {
		RedisEtagConfigurationHelper.injectRedisETagMethods(mainContext)
	}
}
