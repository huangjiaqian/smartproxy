package org.huangjiaqqian.smartproxy.common.web;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.SessionModel;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;

/**
 * 回话管理器
 * @author 黄钱钱
 *
 */
public class SessionManager {
	public static final int SESSION_TIMEOUT = 30 * 60000; //回话超时时间 30分钟 (ms)
	public static final String SESSION_COOKIE_KEY = "_HJQ_SESSIONID"; //session cookie key
	
	private static final SessionManager SESSION_MANAGER = new SessionManager();
	public static final SessionManager getSessionManager() {
		return SESSION_MANAGER;
	}
	
	//回话数据
	private static final Map<String, Map<String, Object>> SESSION_ATTR_MAP = new ConcurrentHashMap<>();
	
	//回话时间戳
	private static final Map<String, Long> SESSION_TS = new ConcurrentHashMap<>();
	
	
	private SessionManager(){}
	
	private boolean isStartSesionManage = false; //开始回话管理
	
	private void startSessionManage() {
		if(isStartSesionManage) {
			return;
		}
		isStartSesionManage = true;
		
		ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
		//30s刷新一次session状态
		ses.scheduleWithFixedDelay(()->{
			
			for (String sessionId : SESSION_TS.keySet()) {
				Long time = System.currentTimeMillis();
				if(time - SESSION_TS.get(sessionId) > SESSION_TIMEOUT) {
					System.out.println("会话超时:" + sessionId);
					SESSION_TS.remove(sessionId);
					SESSION_ATTR_MAP.remove(sessionId);
				}
				
			}
			
			
		}, 10, 30, TimeUnit.SECONDS);
	}
	
	public ReqModel serveReq(Request req, Response resp) throws IOException {
		startSessionManage();
		
		String cookieStr = req.getHeaders().get("Cookie");
		Map<String, String> cookieMap = genCookie(cookieStr);
		
		String sessionId = cookieMap.get(SESSION_COOKIE_KEY);
		
		if(StrUtil.isBlank(sessionId)) {
			sessionId = UUID.randomUUID().toString().replaceAll("-", "") + DateUtil.format(new Date(), "yyyyMMdd");			
		}
		
		Map<String, Object> sessionAttrMap = SESSION_ATTR_MAP.get(sessionId);
		
		if(sessionAttrMap == null) {
			sessionAttrMap = new HashMap<String, Object>();
			SESSION_ATTR_MAP.put(sessionId, sessionAttrMap);
		}
		SessionModel sessionModel = new SessionModel(sessionAttrMap, sessionId);
				
		SESSION_TS.put(sessionId, System.currentTimeMillis());
		ReqModel reqModel = new ReqModel(req.getParamsList(), sessionModel);
		
		resp.getHeaders().add("Set-Cookie", SESSION_COOKIE_KEY + "=" + sessionId);
		
		return reqModel;
	}
	
	private Map<String, String> genCookie(String cookieStr) {
		//token=be8b88b553f14137a7bbc8a2b06a049c; stay_login=0; KOD_SESSION_SSO=9pg5pbduij4jilaqfbam3fdeas;
		Map<String, String> cookieMap = new HashMap<String, String>();
		String[] cookies = StrUtil.split(cookieStr, ";");
		if(cookies == null || cookies.length < 1) {
			return cookieMap;
		}
		for (String cookie : cookies) {
			cookie = StrUtil.trim(cookie);
			if(StrUtil.isBlank(cookie)) {
				continue;
			}
			String key = null;
			String value = null;
			
			String[] strs = StrUtil.split(cookie, "=");
			if(strs == null || strs.length < 1) {
				continue;
			}
			
			key = strs[0];
			if(strs.length > 1) {
				value = strs[1];
			}
			
			cookieMap.put(key, value);
		}
		
		return cookieMap;
	}
}
