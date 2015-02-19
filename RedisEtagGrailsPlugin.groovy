import org.gametube.redisetag.helper.RedisEtagConfigurationHelper

class RedisEtagGrailsPlugin {
// the plugin version
	def version = "0.1-SNAPSHOT"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "2.0 > *"
	// resources that are excluded from plugin packaging
	def pluginExcludes = []

	def title = "Redis Etag Plugin" // Headline display name of the plugin
	def author = "Tamer Shahin"
	def authorEmail = "tamer.shahin@gmail.com "
	def description = '''\
This plugin is intended to work in pair with the [Grails cache-headers plugin], in
particular it's providing a solution to generate, store and retrieve ETag values
in Redis, avoiding DB hits. Redis is thus used as a central cache for
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

	def doWithWebDescriptor = { xml ->
		// TODO Implement additions to web.xml (optional), this event occurs before
	}

	def onShutdown = { event ->
		// TODO Implement code that is executed when the application shuts down (optional)
	}

	def doWithDynamicMethods = { ctx ->
		redisEtagCustomization(ctx)
	}

	def doWithApplicationContext = { applicationContext ->
		redisEtagCustomization(applicationContext)
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


