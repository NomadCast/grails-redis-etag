import org.gametube.redisetag.helper.RedisEtagConfigurationHelper

class RedisEtagGrailsPlugin {
// the plugin version
	def version = "0.1-SNAPSHOT"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "2.0 > *"
	// resources that are excluded from plugin packaging
	def pluginExcludes = [	]

	def title = "Redis Etag Plugin" // Headline display name of the plugin
	def author = "Tamer Shahin"
	def authorEmail = "tamer.shahin@gmail.com "
	def description = '''\
This plugin is intended to work in pair with the Caching Headers Plugin, in particular it's providing a solution to generate, store and retrieve a ETAG value
that relies on Redis. This means that the ETAG generated will be stored and retrieved from Redis, thus giving the possibility to centralize ETAG handling
between multiple instance of the same application.
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
	 * Methods that will enclose all customization needed for NC-Api project.
	 * Customizations are re-applied at each change in watchedResources or in
	 * config files.
	 * @param mainContext
	 * @return
	 */
	static void redisEtagCustomization(mainContext) {
		RedisEtagConfigurationHelper.injectRedisServiceForETag(mainContext)
	}

}


