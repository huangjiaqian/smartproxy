package org.huangjiaqqian.smartproxy.server.web.interceptor;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.interceptor.BaseInterceptor;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;

import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;

public class PageInterceptor implements BaseInterceptor {

	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();
	
	public static boolean shouldRefreshMenu = true; //需要刷新菜单
	
	@Override
	public boolean before(Request request, Response response, ReqModel reqModel, RespMpdel respMpdel) throws SQLException, IOException {
		
		if(shouldRefreshMenu || reqModel.getSession().getAttr("_proxy_client") == null) {
			List<Map<String, Object>> list = sqliteHelper.executeQueryForList("select * from proxy_client");
			reqModel.getSession().setAttr("_proxy_client", list);
			shouldRefreshMenu = false;
		}
		
		respMpdel.put("_clientList", reqModel.getSession().getAttr("_proxy_client"));
		
		respMpdel.put("_host", request.getBaseURL().getHost());
		
		if(reqModel.getSession().getAttr("_loginUser") == null) {
			response.redirect("/login", false); //跳转到登录页面
			return false;
		}
		respMpdel.put("_loginUser", reqModel.getSession().getAttr("_loginUser"));
		return true;
	}

	@Override
	public void after(Request request, Response response, ReqModel reqModel, RespMpdel respMpdel) {
		// TODO Auto-generated method stub
		
	}

}
