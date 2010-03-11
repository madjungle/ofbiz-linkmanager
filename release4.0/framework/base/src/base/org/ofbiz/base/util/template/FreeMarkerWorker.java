/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.base.util.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;

import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
//import com.clarkware.profiler.Profiler;


/**
 * FreemarkerViewHandler - Freemarker Template Engine Util
 *
 */
public class FreeMarkerWorker {
    
    public static final String module = FreeMarkerWorker.class.getName();
    
    // use soft references for this so that things from Content records don't kill all of our memory, or maybe not for performance reasons... hmmm, leave to config file...
    public static UtilCache cachedTemplates = new UtilCache("template.ftl.general", 0, 0, false);
    private static Configuration defaultOfbizConfig = null;

    public static Map ftlTransforms = new HashMap();
    
    static {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            // note: loadClass is necessary for these since this class doesn't know anything about them at compile time
            // double note: may want to make this more dynamic and configurable in the future
            ftlTransforms.put("ofbizUrl", loader.loadClass("org.ofbiz.webapp.ftl.OfbizUrlTransform").newInstance());
            ftlTransforms.put("ofbizContentUrl", loader.loadClass("org.ofbiz.webapp.ftl.OfbizContentTransform").newInstance());
            ftlTransforms.put("ofbizCurrency", loader.loadClass("org.ofbiz.webapp.ftl.OfbizCurrencyTransform").newInstance());
            ftlTransforms.put("ofbizAmount", loader.loadClass("org.ofbiz.webapp.ftl.OfbizAmountTransform").newInstance());
            ftlTransforms.put("setRequestAttribute", loader.loadClass("org.ofbiz.webapp.ftl.SetRequestAttributeMethod").newInstance());
            ftlTransforms.put("renderWrappedText", loader.loadClass("org.ofbiz.webapp.ftl.RenderWrappedTextTransform").newInstance());

            ftlTransforms.put("menuWrap", loader.loadClass("org.ofbiz.widget.menu.MenuWrapTransform").newInstance());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (IllegalAccessException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        } catch (InstantiationException e) {
            Debug.logError(e, "Could not pre-initialize dynamically loaded class: ", module);
        }

        // do the applications ones in a separate pass so the framework ones can load even if the applications are not present
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            ftlTransforms.put("editRenderSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.EditRenderSubContentTransform").newInstance());
            ftlTransforms.put("renderSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentTransform").newInstance());
            ftlTransforms.put("loopSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.LoopSubContentTransform").newInstance());
            ftlTransforms.put("traverseSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.TraverseSubContentTransform").newInstance());

            ftlTransforms.put("checkPermission", loader.loadClass("org.ofbiz.content.webapp.ftl.CheckPermissionTransform").newInstance());
            ftlTransforms.put("injectNodeTrailCsv", loader.loadClass("org.ofbiz.content.webapp.ftl.InjectNodeTrailCsvTransform").newInstance());
            
            ftlTransforms.put("editRenderSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.EditRenderSubContentCacheTransform").newInstance());
            ftlTransforms.put("renderSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentCacheTransform").newInstance());
            ftlTransforms.put("loopSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.LoopSubContentCacheTransform").newInstance());
            ftlTransforms.put("traverseSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.TraverseSubContentCacheTransform").newInstance());
            ftlTransforms.put("wrapSubContentCache", loader.loadClass("org.ofbiz.content.webapp.ftl.WrapSubContentCacheTransform").newInstance());
            ftlTransforms.put("limitedSubContent", loader.loadClass("org.ofbiz.content.webapp.ftl.LimitedSubContentCacheTransform").newInstance());
            ftlTransforms.put("renderSubContentAsText", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderSubContentAsText").newInstance());
            ftlTransforms.put("renderContentAsText", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderContentAsText").newInstance());
            ftlTransforms.put("renderContent", loader.loadClass("org.ofbiz.content.webapp.ftl.RenderContentTransform").newInstance());
        } catch (ClassNotFoundException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        } catch (IllegalAccessException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        } catch (InstantiationException e) {
            Debug.logError("Could not pre-initialize dynamically loaded class: " + e.toString(), module);
        }
    }

    /**
     * Renders a template at the specified location.
     * @param templateLocation Location of the template - file path or URL
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplateAtLocation(String templateLocation, Map context, Writer outWriter) throws MalformedURLException, TemplateException, IOException {
        renderTemplate(templateLocation, context, outWriter);
    }
    
    /**
     * Renders a template contained in a String.
     * @param templateId A unique ID for this template - used for caching
     * @param templateString The String containing the template
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplate(String templateLocation, String templateString, Map context, Writer outWriter) throws TemplateException, IOException {
        renderTemplate(templateLocation, context, outWriter);
    }
    
    /**
     * Renders a template from a Reader.
     * @param templateId A unique ID for this template - used for caching
     * @param templateReader The Reader that reads the template
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplate(String templateLocation, Map context, Writer outWriter) throws TemplateException, IOException {
        Template template = getTemplate(templateLocation);
        renderTemplate(template, context, outWriter);
    }
 
    /**
     * Renders a Template instance.
     * @param template A Template instance
     * @param context The context Map
     * @param outWriter The Writer to render to
     */
    public static void renderTemplate(Template template, Map context, Writer outWriter) throws TemplateException, IOException {
        addAllOfbizTransforms(context);
        // make sure there is no "null" string in there as FreeMarker will try to use it
        context.remove("null");
        // Since the template cache keeps a single instance of a Template that is shared among users,
        // and since that Template instance is immutable, we need to create an Environment instance and
        // use it to process the template with the user's settings.
        Environment env = template.createProcessingEnvironment(context, outWriter);
        applyUserSettings(env, context);
        env.process();
    }
    
    public static void addAllOfbizTransforms(Map context) {
        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
        TemplateHashModel staticModels = wrapper.getStaticModels();
        if (context == null) {
            context = FastMap.newInstance();
        }
        context.put("Static", staticModels);
        context.putAll(ftlTransforms);
    }

    /**
     * Apply user settings to an Environment instance.
     * @param env An Environment instance
     * @param context The context Map containing the user settings
     */
    public static void applyUserSettings(Environment env, Map context) throws TemplateException {
        Locale locale = (Locale) context.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
        }
        env.setLocale(locale);

        /* uncomment code block when framework time zone support is implemented
        TimeZone timeZone = (TimeZone) context.get("timeZone");
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        env.setTimeZone(timeZone); */
    }

    public static Configuration getDefaultOfbizConfig() throws TemplateException, IOException {
        if (defaultOfbizConfig == null) {
            synchronized (FreeMarkerWorker.class) {
                if (defaultOfbizConfig == null) {
                    Configuration config = new Configuration();            
                    config.setObjectWrapper(BeansWrapper.getDefaultInstance());
                    // the next two settings don't do anything - Freemarker will format
                    // output according to the user's locale
                    config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss.SSS");
                    config.setSetting("number_format", "0.##########");
                    config.setLocalizedLookup(false);
                    config.setTemplateLoader(new FlexibleTemplateLoader());
                    defaultOfbizConfig = config;
                }
            }
        }
        return defaultOfbizConfig;
    }
    
    /** Make sure to close the reader when you're done! That's why this method is private, BTW. */
    private static Reader makeReader(String templateLocation) throws IOException {
        if (UtilValidate.isEmpty(templateLocation)) {
            throw new IllegalArgumentException("FreeMarker template location null or empty");
        }
        
        URL locationUrl = null;
        try {
            locationUrl = FlexibleLocation.resolveLocation(templateLocation);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        if (locationUrl == null) {
            throw new IllegalArgumentException("FreeMarker file not found at location: " + templateLocation);
        }
        
        InputStream locationIs = locationUrl.openStream();
        Reader templateReader = new InputStreamReader(locationIs);
        
        String locationProtocol = locationUrl.getProtocol();
        if ("file".equals(locationProtocol) && Debug.verboseOn()) {
            String locationFile = locationUrl.getFile();
            int lastSlash = locationFile.lastIndexOf("/");
            String locationDir = locationFile.substring(0, lastSlash);
            String filename = locationFile.substring(lastSlash + 1);
            if (Debug.verboseOn()) Debug.logVerbose("FreeMarker render: filename=" + filename + ", locationDir=" + locationDir, module);
        }
        
        return templateReader;
    }

    /**
     * Gets a Template instance from the template cache. If the Template instance isn't
     * found in the cache, then one will be created.
     * @param templateLocation Location of the template - file path or URL
     */
    public static Template getTemplate(String templateLocation) throws TemplateException, IOException {
        Template template = (Template) cachedTemplates.get(templateLocation);
        if (template == null) {
            synchronized (cachedTemplates) {
                template = (Template) cachedTemplates.get(templateLocation);
                if (template == null) {
                    // only make the reader if we need it, and then close it right after!
                    Reader templateReader = makeReader(templateLocation);
                    template = new Template(templateLocation, templateReader, getDefaultOfbizConfig());
                    templateReader.close();
                    cachedTemplates.put(templateLocation, template);
                }
            }
        }
        return template;
    }
    
    public static String getArg(Map args, String key, Environment env) {
        Map templateContext = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        return getArg(args, key, templateContext);
    }

    public static String getArg(Map args, String key, Map templateContext) {
        //SimpleScalar s = null;
        Object o = null;
        String returnVal = null;
        o = args.get(key);
        returnVal = (String) unwrap(o);
        if (returnVal == null) {
            try {
                if (templateContext != null) {
                    returnVal = (String) templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                //return null;
            }
        }
        return returnVal;
    }

    public static Object getArgObject(Map args, String key, Map templateContext) {
        //SimpleScalar s = null;
        Object o = null;
        Object returnVal = null;
        o = args.get(key);
        returnVal = unwrap(o);
        if (returnVal == null) {
            try {
                if (templateContext != null) {
                    returnVal = templateContext.get(key);
                }
            } catch (ClassCastException e2) {
                //return null;
            }
        }
        return returnVal;
    }


   /**
    * Gets BeanModel from FreeMarker context and returns the object that it wraps.
    * @param varName the name of the variable in the FreeMarker context.
    * @param env the FreeMarker Environment
    */
    public static Object getWrappedObject(String varName, Environment env) {
        Object obj = null;
        try {
            obj = env.getVariable(varName);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    obj = null;
                } else if (obj instanceof BeanModel) {
                    BeanModel bean = (BeanModel) obj;
                    obj = bean.getWrappedObject();
                } else if (obj instanceof SimpleScalar) {
                    obj = obj.toString();
                }
            }
        } catch (TemplateModelException e) {
            Debug.logInfo(e.getMessage(), module);
        }
        return obj;
    }

   /**
    * Gets BeanModel from FreeMarker context and returns the object that it wraps.
    * @param varName the name of the variable in the FreeMarker context.
    * @param env the FreeMarker Environment
    */
    public static BeanModel getBeanModel(String varName, Environment env) {
        BeanModel bean = null;
        try {
            bean = (BeanModel) env.getVariable(varName);
        } catch (TemplateModelException e) {
            Debug.logInfo(e.getMessage(), module);
        }
        return bean;
    }

    public static Object get(SimpleHash args, String key) {
        Object returnObj = null;
        Object o = null;
        try {
            o = args.get(key);
        } catch(TemplateModelException e) {
            Debug.logVerbose(e.getMessage(), module);
            return returnObj;
        }

        returnObj = unwrap(o);

        if (returnObj == null) {
            Object ctxObj = null;
            try {
                ctxObj = args.get("context");
            } catch(TemplateModelException e) {
                Debug.logInfo(e.getMessage(), module);
                return returnObj;
            }
            Map ctx = null;
            if (ctxObj instanceof BeanModel) {
                ctx = (Map)((BeanModel)ctxObj).getWrappedObject();
            returnObj = ctx.get(key);
            }
            /*
            try {
                Map templateContext = (Map)FreeMarkerWorker.getWrappedObject("context", env);
                if (templateContext != null) {
                    returnObj = (String)templateContext.get(key);
                }
            } catch(ClassCastException e2) {
                //return null;
            }
            */
        }
        return returnObj;
    }

    public static Object unwrap(Object o) {
        Object returnObj = null;

        if (o == TemplateModel.NOTHING) {
            returnObj = null;
        } else if (o instanceof SimpleScalar) {
            returnObj = o.toString();
        } else if (o instanceof BeanModel) {
            returnObj = ((BeanModel)o).getWrappedObject();
        }
    
        return returnObj;
    }

    public static void checkForLoop(String path, Map ctx) throws IOException {
        List templateList = (List)ctx.get("templateList");
        if (templateList == null) {
            templateList = new ArrayList();
        } else {
            if (templateList.contains(path)) {
                throw new IOException(path + " has already been visited.");
            }
        }
        templateList.add(path);
        ctx.put("templateList", templateList);
    }

    public static Map createEnvironmentMap(Environment env) {
        Map templateRoot = new HashMap();
        Set varNames = null;
        try {
            varNames = env.getKnownVariableNames();
        } catch (TemplateModelException e1) {
            Debug.logError(e1, "Error getting FreeMarker variable names, will not put pass current context on to sub-content", module);
        }
        if (varNames != null) {
            Iterator varNameIter = varNames.iterator();
            while (varNameIter.hasNext()) {
                String varName = (String) varNameIter.next();
                //freemarker.ext.beans.StringModel varObj = (freemarker.ext.beans.StringModel ) varNameIter.next();
                //Object varObj =  varNameIter.next();
                //String varName = varObj.toString();
                templateRoot.put(varName, FreeMarkerWorker.getWrappedObject(varName, env));
            }
        }
        return templateRoot;
    }
    
    public static void saveContextValues(Map context, String [] saveKeyNames, Map saveMap ) {
        //Map saveMap = new HashMap();
        for (int i=0; i<saveKeyNames.length; i++) {
            String key = saveKeyNames[i];
            Object o = context.get(key);
            if (o instanceof Map)
                o = new HashMap((Map)o);
            else if (o instanceof List)
                o = new ArrayList((List)o);
            saveMap.put(key, o);
        }
    }

    public static Map saveValues(Map context, String [] saveKeyNames) {
        Map saveMap = new HashMap();
        for (int i=0; i<saveKeyNames.length; i++) {
            String key = saveKeyNames[i];
            Object o = context.get(key);
            if (o instanceof Map)
                o = new HashMap((Map)o);
            else if (o instanceof List)
                o = new ArrayList((List)o);
            saveMap.put(key, o);
        }
        return saveMap;
    }


    public static void reloadValues(Map context, Map saveValues, Environment env ) {
        Set keySet = saveValues.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object o = saveValues.get(key);
            if (o instanceof Map) {
                Map map = new HashMap();
                map.putAll((Map)o);
                context.put(key, map);
            } else if (o instanceof List) {
                List list = new ArrayList();
                list.addAll((List)o);
                context.put(key, list);
            } else {
                context.put(key, o);
            }
            env.setVariable(key, autoWrap(o, env));
        }
    }

    public static void removeValues(Map context, String [] removeKeyNames ) {
        for (int i=0; i<removeKeyNames.length; i++) {
            String key = removeKeyNames[i];
            context.remove(key);
        }
    }

    public static void overrideWithArgs(Map ctx, Map args) {
        Set keySet = args.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object obj = args.get(key);
            //if (Debug.infoOn()) Debug.logInfo("in overrideWithArgs, key(3):" + key + " obj:" + obj + " class:" + obj.getClass().getName() , module);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    ctx.put(key, null);
                } else {
                    Object unwrappedObj = unwrap(obj);
                    if (unwrappedObj == null)
                        unwrappedObj = obj;
                    ctx.put(key, unwrappedObj.toString());
                }
            } else {
                ctx.put(key, null);
            }
        }
    }

    public static void convertContext(Map ctx) {
        Set keySet = ctx.keySet();
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            Object obj = ctx.get(key);
            if (obj != null) {
                Object unwrappedObj = unwrap(obj);
                if (unwrappedObj != null) {
                    ctx.put(key, unwrappedObj);
                }
            }
        }
    }

    public static void getSiteParameters(HttpServletRequest request, Map ctx) {
        if (request == null) {
            return;
        }
        if (ctx == null) {
            throw new IllegalArgumentException("Error in getSiteParameters, context/ctx cannot be null");
        }
        ServletContext servletContext = request.getSession().getServletContext();
        String rootDir = (String)ctx.get("rootDir");
        String webSiteId = (String)ctx.get("webSiteId");
        String https = (String)ctx.get("https");
        if (UtilValidate.isEmpty(rootDir)) {
            rootDir = servletContext.getRealPath("/");
            ctx.put("rootDir", rootDir);
        }
        if (UtilValidate.isEmpty(webSiteId)) {
            webSiteId = (String) servletContext.getAttribute("webSiteId");
            ctx.put("webSiteId", webSiteId);
        }
        if (UtilValidate.isEmpty(https)) {
            https = (String) servletContext.getAttribute("https");
            ctx.put("https", https);
        }
    }

    public static TemplateModel autoWrap(Object obj, Environment env) {
       BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
       TemplateModel templateModelObj = null;
       try {
           templateModelObj = wrapper.wrap(obj);
       } catch(TemplateModelException e) {
           throw new RuntimeException(e.getMessage());
       }
       return templateModelObj;
    }
    
    /**
     * OFBiz Template Source. This class is used by FlexibleTemplateLoader.
     */
    static class FlexibleTemplateSource {
        protected String templateLocation = null;
        protected Date createdDate = new Date();

        protected FlexibleTemplateSource() {}
        public FlexibleTemplateSource(String templateLocation) {
            this.templateLocation = templateLocation;
        }
        
        public int hashCode() {
            return templateLocation.hashCode();
        }
        public boolean equals(Object obj) {
            return obj instanceof FlexibleTemplateSource && obj.hashCode() == this.hashCode();
        }
        public String getTemplateLocation() {
            return templateLocation;
        }
        public long getLastModified() {
            return createdDate.getTime();
        }
    }
    
    /**
     * OFBiz Template Loader. This template loader uses the FlexibleLocation
     * class to locate and load Freemarker templates.
     */
    static class FlexibleTemplateLoader implements TemplateLoader {
        public Object findTemplateSource(String name) throws IOException {
            return new FlexibleTemplateSource(name);
        }
        public long getLastModified(Object templateSource) {
            FlexibleTemplateSource fts = (FlexibleTemplateSource) templateSource;
            return fts.getLastModified();
        }
        public Reader getReader(Object templateSource, String encoding) throws IOException {
            FlexibleTemplateSource fts = (FlexibleTemplateSource) templateSource;
            return makeReader(fts.getTemplateLocation());
        }
        public void closeTemplateSource(Object templateSource) throws IOException {
            // do nothing
        }
    }
}