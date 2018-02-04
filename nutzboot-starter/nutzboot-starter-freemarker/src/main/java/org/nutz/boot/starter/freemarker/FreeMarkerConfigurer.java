package org.nutz.boot.starter.freemarker;

import freemarker.template.*;
import org.nutz.boot.annotation.PropDoc;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.IocContext;
import org.nutz.ioc.Iocs;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Files;
import org.nutz.lang.Lang;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;
import org.nutz.lang.util.ClassTools;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.Mvcs;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

@IocBean(create = "init")
public class FreeMarkerConfigurer {

	private final static Log log = Logs.get();

	protected static final String PRE = "freemarker.";
	public static final String PRE_SUFFIX = PRE + "suffix";

	private Configuration configuration;
	private String prefix;
	private String suffix;
	private FreemarkerDirectiveFactory freemarkerDirectiveFactory;
	private Map<String, Object> tags = new HashMap<String, Object>();

	public FreeMarkerConfigurer() {
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
		Ioc ioc = Mvcs.ctx().getDefaultIoc();
		PropertiesProxy conf = ioc.get(PropertiesProxy.class,"conf");
		this.initp(configuration, Mvcs.getServletContext(), "template", conf.get(PRE_SUFFIX,".html"), new FreemarkerDirectiveFactory());
	}


	protected void initp(Configuration configuration, ServletContext sc, String prefix, String suffix, FreemarkerDirectiveFactory freemarkerDirectiveFactory) {
		this.configuration = configuration;
        URL url = ClassTools.getClassLoader().getResource(prefix);
        String path = url.getPath();
        this.prefix =path;
		this.suffix = suffix;
		this.freemarkerDirectiveFactory = freemarkerDirectiveFactory;
		if (this.prefix == null)
			this.prefix = sc.getRealPath("/") + prefix;

		this.configuration.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		this.configuration.setTemplateUpdateDelayMilliseconds(-1000);
		this.configuration.setDefaultEncoding("UTF-8");
		this.configuration.setURLEscapingCharset("UTF-8");
		this.configuration.setLocale(Locale.CHINA);
		this.configuration.setBooleanFormat("true,false");
		this.configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
		this.configuration.setDateFormat("yyyy-MM-dd");
		this.configuration.setTimeFormat("HH:mm:ss");
		this.configuration.setNumberFormat("0.######");
		this.configuration.setWhitespaceStripping(true);
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public void init() {
		try {
			initFreeMarkerConfigurer();
			Iterator<Entry<String, Object>> iterator = tags.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, Object> entry = iterator.next();
				configuration.setSharedVariable(entry.getKey(), entry.getValue());
			}
			if (freemarkerDirectiveFactory == null)
				return;
			for (FreemarkerDirective freemarkerDirective : freemarkerDirectiveFactory.getList()) {
				configuration.setSharedVariable(freemarkerDirective.getName(), freemarkerDirective.getTemplateDirectiveModel());
			}
		} catch (IOException e) {
			log.error(e);
		} catch (TemplateException e) {
			log.error(e);
		}
	}

	public String getSuffix() {
		return Strings.isBlank(freemarkerDirectiveFactory.getSuffix()) ? this.suffix : freemarkerDirectiveFactory.getSuffix();
	}

	public String getPrefix() {
		return prefix;
	}

	protected void initFreeMarkerConfigurer() throws IOException, TemplateException {
		String path = freemarkerDirectiveFactory.getFreemarker();
		File file = Files.findFile(path);
		if (!Lang.isEmpty(file)) {
			Properties p = new Properties();
			p.load(Streams.fileIn(file));
			configuration.setSettings(p);
		}
		File f = Files.findFile(prefix);
		configuration.setDirectoryForTemplateLoading(f);
	}

	public void setTags(Map<String, Object> map) {
		Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			String key = entry.getKey();
			Object obj = entry.getValue();
			tags.put(key, obj);
		}
	}

	public FreeMarkerConfigurer setSuffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public FreeMarkerConfigurer setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 *
	 * @param map
	 * @return
	 *
	 *         mapTags : { factory : "$freeMarkerConfigurer#addTags", args : [ {
	 *         'abc' : 1, 'def' : 2 } ] }
	 */
	public FreeMarkerConfigurer addTags(Map<String, Object> map) {
		if (map != null) {
			try {
				configuration.setAllSharedVariables(new SimpleHash(map, new DefaultObjectWrapper(Configuration.VERSION_2_3_26)));
			} catch (TemplateModelException e) {
				log.error(e);
			}
		}
		return this;
	}
}
