package com.github.aesteve.vertx.nubes;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.aesteve.vertx.nubes.auth.AuthMethod;
import com.github.aesteve.vertx.nubes.context.RateLimit;
import com.github.aesteve.vertx.nubes.handlers.AnnotationProcessorRegistry;
import com.github.aesteve.vertx.nubes.handlers.Processor;
import com.github.aesteve.vertx.nubes.marshallers.PayloadMarshaller;
import com.github.aesteve.vertx.nubes.reflections.RouteRegistry;
import com.github.aesteve.vertx.nubes.reflections.injectors.annot.AnnotatedParamInjectorRegistry;
import com.github.aesteve.vertx.nubes.reflections.injectors.typed.TypedParamInjectorRegistry;
import com.github.aesteve.vertx.nubes.services.ServiceRegistry;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;
import io.vertx.ext.web.templ.JadeTemplateEngine;
import io.vertx.ext.web.templ.MVELTemplateEngine;
import io.vertx.ext.web.templ.TemplateEngine;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

public class Config {

	private Config() {
		bundlesByLocale = new HashMap<>();
		globalHandlers = new ArrayList<>();
		templateEngines = new HashMap<>();
		sockJSOptions = new SockJSHandlerOptions();
		marshallers = new HashMap<>();
	}

	public JsonObject json;
	public String srcPackage;
	public List<String> controllerPackages;
	public List<String> fixturePackages;
	public String verticlePackage;
	public String domainPackage;
	public RateLimit rateLimit;
	public String webroot;
	public String assetsPath;
	public String tplDir;
	public boolean displayErrors;
	public Vertx vertx;
	public AuthProvider authProvider;
	public AuthMethod authMethod;
	public String i18nDir;

	public AnnotationProcessorRegistry apRegistry;
	public Map<Class<? extends Annotation>, Set<Handler<RoutingContext>>> annotationHandlers;
	public Map<Class<?>, Processor> typeProcessors;
	public TypedParamInjectorRegistry typeInjectors;
	public AnnotatedParamInjectorRegistry annotInjectors;
	public ServiceRegistry serviceRegistry;
	public RouteRegistry routeRegistry;
	public Map<Class<?>, Handler<RoutingContext>> paramHandlers;
	public Map<String, Handler<RoutingContext>> aopHandlerRegistry;
	public Map<Locale, ResourceBundle> bundlesByLocale;
	public List<Handler<RoutingContext>> globalHandlers;
	public Map<String, TemplateEngine> templateEngines;
	public SockJSHandlerOptions sockJSOptions;
	public Map<String, PayloadMarshaller> marshallers;

	/**
	 * TODO : check config instead of throwing exceptions
	 * 
	 * @param json
	 * @return config
	 */
	@SuppressWarnings("unchecked")
	public static Config fromJsonObject(JsonObject json, Vertx vertx) {
		Config instance = new Config();
		JsonObject services;
		JsonArray templates;

		instance.json = json;
		instance.vertx = vertx;
		instance.srcPackage = json.getString("src-package");
		instance.i18nDir = json.getString("i18nDir", "web/i18n/");
		if (!instance.i18nDir.endsWith("/")) {
			instance.i18nDir = instance.i18nDir + "/";
		}
		JsonArray controllers = json.getJsonArray("controller-packages", new JsonArray().add(instance.srcPackage + ".controllers"));
		instance.controllerPackages = controllers.getList();

		instance.verticlePackage = json.getString("verticle-package", instance.srcPackage + ".verticles");

		instance.domainPackage = json.getString("domain-package", instance.srcPackage + ".domains");

		JsonArray fixtures = json.getJsonArray("fixture-packages",new JsonArray().add(instance.srcPackage + ".fixtures"));
		instance.fixturePackages = fixtures.getList();


		//register services included in config
		services = json.getJsonObject("services", new JsonObject());
		instance.serviceRegistry = new ServiceRegistry(vertx);

		for (Map.Entry<String, Object> service : services) {
			String name = service.getKey();
			String className = (String)service.getValue();
			try {

				Class<?> clazz = Class.forName(className);
				instance.serviceRegistry.registerService(name, clazz.newInstance());

			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}


		templates = json.getJsonArray("templates", new JsonArray());

		//Register templateEngines for extensions added in config
		if(templates.contains("hbs")) {
			instance.templateEngines.put("hbs", HandlebarsTemplateEngine.create());
		}
		if(templates.contains("jade")) {
			instance.templateEngines.put("jade", JadeTemplateEngine.create());
		}
		if(templates.contains("templ")){
			instance.templateEngines.put("templ", MVELTemplateEngine.create());
		}
		if(templates.contains("thymeleaf")){
			instance.templateEngines.put("html", ThymeleafTemplateEngine.create());
		}

		JsonObject rateLimitJson = json.getJsonObject("throttling");
		if (rateLimitJson != null) {
			int count = rateLimitJson.getInteger("count");
			int value = rateLimitJson.getInteger("time-frame");
			TimeUnit timeUnit = TimeUnit.valueOf(rateLimitJson.getString("time-unit"));
			instance.rateLimit = new RateLimit(count, value, timeUnit);
		}

		instance.webroot = json.getString("webroot", "web/assets");
		instance.assetsPath = json.getString("static-path", "/assets");
		instance.tplDir = json.getString("views-dir", "web/views");
		instance.displayErrors = json.getBoolean("display-errors", Boolean.FALSE);
		// TODO : read sockJSOptions from config
		return instance;
	}

	public ResourceBundle getResourceBundle(Locale loc) {
		return bundlesByLocale.get(loc);
	}
}
