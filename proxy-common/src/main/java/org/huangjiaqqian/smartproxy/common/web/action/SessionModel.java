package org.huangjiaqqian.smartproxy.common.web.action;

import java.util.Map;

public class SessionModel {
	
	private Map<String, Object> attrMap = null;
	
	private String sessionId;
	
	public SessionModel(Map<String, Object> attrMap, String sessionId) {
		super();
		this.attrMap = attrMap;
		this.sessionId = sessionId;
	}

	public void setAttr(String key, Object value) {
		attrMap.put(key, value);
	}
	
	public void removeAttr(String key) {
		attrMap.remove(key);
	}
	
	public Object getAttr(String key) {
		return attrMap.get(key);
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
}
