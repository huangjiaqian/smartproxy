package org.huangjiaqqian.smartproxy.common.web.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReqModel {
	
	private Map<String, List<String>> paramsMap = new HashMap<>();
	
	private SessionModel sessionModel;
	
	public ReqModel(List<String[]> paramList, SessionModel sessionModel) {
		this.sessionModel = sessionModel;
		
		if(paramList != null && !paramList.isEmpty()) {
			for(String[] param: paramList) {
				String key = null;
				String value = null;
				if(param.length < 1) {
					continue;
				}
				key = param[0];
				if(param.length > 1) {
					value = param[1];
				}
				
				List<String> paramsTemp = paramsMap.get(key);
				
				if(paramsTemp == null) {
					paramsTemp = new ArrayList<String>();
				}
				
				paramsTemp.add(value);
				
				paramsMap.put(key, paramsTemp);
				
			}
		}
	}
	
	public String getParam(String key) {
		if(paramsMap.get(key) == null || paramsMap.get(key).isEmpty()) {
			return null;
		}
		return paramsMap.get(key).get(0);
	}
	
	public String[] getParams(String key) {
		if(paramsMap.get(key) == null || paramsMap.get(key).isEmpty()) {
			return null;
		}
		return listToArr(paramsMap.get(key));
	}
	
	private String[] listToArr(List<String> list) {
		if(list == null || list.isEmpty()) {
			return null;
		}
		String[] arr = new String[list.size()];
		for(int i = 0;i < list.size();i++) {
			arr[i] = list.get(i);
		}
		return arr;
	}
	public SessionModel getSession() {
		return sessionModel;
	}
}
