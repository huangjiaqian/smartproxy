package org.huangjiaqqian.smartproxy.common.web.interceptor;

import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;

import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;

public interface BaseInterceptor {
	public boolean before(Request request, Response response, ReqModel reqModel, RespMpdel respMpdel) throws Exception;
	public void after(Request request, Response response, ReqModel reqModel, RespMpdel respMpdel) throws Exception;
}
