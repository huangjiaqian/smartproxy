package org.huangjiaqqian.smartproxy.common.web.handler;

import java.io.IOException;
import java.lang.reflect.Method;

import org.huangjiaqqian.smartproxy.common.web.SessionManager;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.annotation.RenderBody;
import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;
import org.huangjiaqqian.smartproxy.common.web.interceptor.BaseInterceptor;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;

import cn.hutool.core.util.StrUtil;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;

public class ActionHandler implements ContextHandler {
	
	private Object action;
	
	private String baseTemplatePath = System.getProperty("user.dir") + "/webapps/templates";
	
	private BaseInterceptor baseInterceptor;
	
	private Engine engine = Engine.use();
	{
		engine.setDevMode(false);
		engine.setBaseTemplatePath(baseTemplatePath);
	}
	public ActionHandler(Object action) {
		this.action = action;
	}
	public ActionHandler(Object action, String baseTemplatePath) {
		this.action = action;
		this.baseTemplatePath = baseTemplatePath;
	}
	public ActionHandler(Object action, BaseInterceptor baseInterceptor, String baseTemplatePath) {
		this.action = action;
		this.baseTemplatePath = baseTemplatePath;
		this.baseInterceptor = baseInterceptor;
	}
	public ActionHandler(Object action, BaseInterceptor baseInterceptor) {
		this.action = action;
		this.baseInterceptor = baseInterceptor;
	}
	
	
	@Override
	public int serve(Request req, Response resp) throws IOException {
		
		ReqModel reqModel = SessionManager.getSessionManager().serveReq(req, resp);
		
        String methodName = reqModel.getParam("method");
		
        if(StrUtil.isBlank(methodName)) {
        	methodName = "index";
        }
        
        if(StrUtil.isBlank(methodName)) {
        	resp.sendError(404, "<h1>404 not found</h1>");
        	return 0;
        }
        
        Class<?> actionCls = action.getClass();
        
        try {
			
        	Method method = null;
        	
        	try {
        		method = actionCls.getMethod(methodName, Request.class, ReqModel.class, RespMpdel.class);        		
        	} catch (NoSuchMethodException e) {
        		resp.sendError(404, "<h1>404 not found</h1>");
            	return 0;
			}
			
			if(!method.getReturnType().isAssignableFrom(String.class)) {
				resp.sendError(404, "<h1>404 not found</h1>");
	        	return 0;
			}
			RenderBody renderBody = method.getAnnotation(RenderBody.class);
			
			RespMpdel respMpdel = new RespMpdel();
			try {
				boolean canExecuteNext = true;
				if(baseInterceptor != null) {
					canExecuteNext = baseInterceptor.before(req, resp, reqModel, respMpdel);
				}
				
				//不往下执行 
				if(!canExecuteNext) {
					return 0;
				}
				
				Object resultObj = method.invoke(action, req, reqModel, respMpdel);
				String result = resultObj == null ? "" : (String) resultObj;
				if(baseInterceptor != null) {
					baseInterceptor.after(req, resp, reqModel, respMpdel);
				}
				if(renderBody == null || renderBody.value() == RenderBodyType.TEXT) {
					resp.getHeaders().add("Content-Type", "text/plain");
			        resp.send(200, result);
				} else if(renderBody.value() == RenderBodyType.HTML) {
					resp.getHeaders().add("Content-Type", "text/html");
			        resp.send(200, result);
				} else if(renderBody.value() == RenderBodyType.JSON) {
					resp.getHeaders().add("Content-Type", "application/json");
			        resp.send(200, result);
				} else if(renderBody.value() == RenderBodyType.TEMPLATE) {
					resp.getHeaders().add("Content-Type", "text/html");
					
			    	Template template = engine.getTemplate(result);
			    	String resultHtml = template.renderToString(respMpdel);
			        resp.send(200, resultHtml);
				} else {
					resp.sendError(404, "<h1>404 not found</h1>");
		        	return 0;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				resp.sendError(500, "<h1>500 error</h1>");
	        	return 0;
			}
			
			
		} catch (SecurityException e) {
			e.printStackTrace();
		}
        
        if(StrUtil.isBlank(methodName)) {
        	resp.sendError(404, "<h1>404 not found</h1>");
        	return 0;
        }
        /*
        resp.getHeaders().add("Content-Type", "text/plain");
        resp.send(200, "Hello, World!");
        */        
		return 0;
	}

}
