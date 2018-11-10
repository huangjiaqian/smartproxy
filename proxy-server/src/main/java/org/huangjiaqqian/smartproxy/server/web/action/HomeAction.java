package org.huangjiaqqian.smartproxy.server.web.action;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.annotation.RenderBody;
import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;

import net.freeutils.httpserver.HTTPServer.Request;

public class HomeAction {

	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	@RenderBody(RenderBodyType.TEMPLATE)
	public String index(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		List<Map<String, Object>> list = sqliteHelper.executeQueryForList("select * from proxy_client");

		respMpdel.put("list", list);

		respMpdel.put("currentMenu", "clientList");
		return "index.html";
	}

	@RenderBody(RenderBodyType.TEMPLATE)
	public String editClient(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {

		Map<String, Object> client = sqliteHelper
				.executeQueryForMap("select * from proxy_client where id='" + reqModel.getParam("id") + "'");

		respMpdel.put("client", client);
		respMpdel.put("type", "update");
		respMpdel.put("currentMenu", "clientList");
		return "client-info.html";
	}

	@RenderBody(RenderBodyType.TEMPLATE)
	public String addClient(Request request, ReqModel reqModel, RespMpdel respMpdel) {
		respMpdel.put("type", "add");
		respMpdel.put("currentMenu", "addClient");
		return "client-info.html";
	}

	@RenderBody(RenderBodyType.TEMPLATE)
	public String clientConfigInfo(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String clientId = reqModel.getParam("clientId");
		Map<String, Object> client = sqliteHelper
				.executeQueryForMap("select * from proxy_client where id='" + clientId + "'");
		List<Map<String, Object>> list = sqliteHelper
				.executeQueryForList("select * from proxy_client_config where proxy_client_id='" + clientId + "'");

		respMpdel.put("client", client);
		respMpdel.put("list", list);
		
		respMpdel.put("currentMenu", "clientId" + clientId);
		return "client-config-list.html";
	}

	@RenderBody(RenderBodyType.TEMPLATE)
	public String editConfig(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String clientId = reqModel.getParam("clientId");
		String configId = reqModel.getParam("configId");
		
		Map<String, Object> config = sqliteHelper
				.executeQueryForMap("select * from proxy_client_config where id='" + configId + "'");

		respMpdel.put("clientId", clientId);
		respMpdel.put("config", config);
		respMpdel.put("type", "update");
		respMpdel.put("currentMenu", "clientId" + clientId);
		return "client-config-info.html";
	}
	
	@RenderBody(RenderBodyType.TEMPLATE)
	public String addConfig(Request request, ReqModel reqModel, RespMpdel respMpdel) {
		String clientId = reqModel.getParam("clientId");
		
		respMpdel.put("clientId", clientId);
		respMpdel.put("type", "add");
		respMpdel.put("currentMenu", "clientId" + clientId);
		return "client-config-info.html";
	}
	
}
