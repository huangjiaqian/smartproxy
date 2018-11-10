package org.huangjiaqqian.smartproxy.p2p.client.web.action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.annotation.RenderBody;
import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;
import org.huangjiaqqian.smartproxy.p2p.client.ForwarderClientGetter;
import org.huangjiaqqian.smartproxy.p2p.client.cache.ConfigPool;
import org.huangjiaqqian.smartproxy.p2p.client.config.DbConfig;
import org.nnat.dragonite.forwarder.config.ForwarderClientConfig;
import org.nnat.dragonite.forwarder.network.client.ForwarderClient;

import com.vecsight.dragonite.forwarder.exception.IncorrectHeaderException;
import com.vecsight.dragonite.forwarder.exception.ServerRejectedException;
import com.vecsight.dragonite.sdk.exception.DragoniteException;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import net.freeutils.httpserver.HTTPServer.Request;

public class HomeAction {

	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	@RenderBody(RenderBodyType.TEMPLATE)
	public String index(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		List<Map<String, Object>> list = sqliteHelper.executeQueryForList("select * from mapping_config");

		respMpdel.put("list", list);

		respMpdel.put("currentMenu", "mappingList");
		return "index.html";
	}

	@RenderBody(RenderBodyType.TEMPLATE)
	public String addMapping(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String mappingId = reqModel.getParam("mappingId");

		if (mappingId != null) {
			Map<String, Object> map = sqliteHelper
					.executeQueryForMap("select * from mapping_config where id='" + mappingId + "'");
			respMpdel.put("mapping", map);
		}

		respMpdel.put("type", "add");
		respMpdel.put("currentMenu", "addMapping");
		return "mapping-info.html";
	}
	
	@RenderBody(RenderBodyType.TEXT)
	public String doConnectMapping(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String mappingId = reqModel.getParam("mappingId");
		Map<String, Object> map = sqliteHelper
				.executeQueryForMap("select * from mapping_config where id='" + mappingId + "'");
		
		if(map == null) {
			return "对象不存在！";
		}
		
		ForwarderClient client = ConfigPool.FORWARDER_CLIENT_MAP.get(Convert.toInt(map.get("localPort")));
		
		if(client != null) {
			if(client != null) {
				try {
					client.close();				
				} catch (Exception e) {
				}
			}
			ConfigPool.FORWARDER_CLIENT_MAP.remove(Convert.toInt(map.get("localPort")));
		}
		
		String clientKey = Convert.toStr(map.get("clientKey"));
		String remoteHost = Convert.toStr(map.get("remoteHost"));
		Integer remotePort = Convert.toInt(map.get("remotePort"));
		String natHost = Convert.toStr(map.get("natHost"));
		Integer natPort = Convert.toInt(map.get("natPort"));
		Integer localPort = Convert.toInt(map.get("localPort"));
		Integer downMbps = Convert.toInt(map.get("downMbps"));
		Integer upMbps = Convert.toInt(map.get("upMbps"));
		
		try {
			ForwarderClientConfig config = new ForwarderClientConfig(new InetSocketAddress(remoteHost, remotePort),
					new InetSocketAddress(natHost, natPort), localPort, downMbps, upMbps);
			ForwarderClientGetter getter = new ForwarderClientGetter(config, clientKey);
			client = getter.getForwarderClient();
			
			ConfigPool.FORWARDER_CLIENT_MAP.put(localPort, client);
			
			sqliteHelper.executeUpdate("update mapping_config set enable='true' where id='" + mappingId + "'");
			
		} catch (InterruptedException | IOException | DragoniteException | IncorrectHeaderException
				| ServerRejectedException e) {
			e.printStackTrace();

			return "创建失败:" + e.getMessage();
		}
		
		return "true";
	}
	
	@RenderBody(RenderBodyType.TEXT)
	public String doCloseMapping(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String mappingId = reqModel.getParam("mappingId");
		Map<String, Object> map = sqliteHelper
				.executeQueryForMap("select * from mapping_config where id='" + mappingId + "'");
		ForwarderClient client = ConfigPool.FORWARDER_CLIENT_MAP.get(Convert.toInt(map.get("localPort")));
		if(client != null) {
			try {
				client.close();				
			} catch (Exception e) {
			}
		}
		ConfigPool.FORWARDER_CLIENT_MAP.remove(Convert.toInt(map.get("localPort")));
		sqliteHelper.executeUpdate("update mapping_config set enable='false' where id='" + mappingId + "'");
		return "true";
	}
	
	@RenderBody(RenderBodyType.TEXT)
	public String doDelMapping(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String mappingId = reqModel.getParam("mappingId");
		Map<String, Object> map = sqliteHelper
				.executeQueryForMap("select * from mapping_config where id='" + mappingId + "'");
		ForwarderClient client = ConfigPool.FORWARDER_CLIENT_MAP.get(Convert.toInt(map.get("localPort")));
		if(client != null) {
			try {
				client.close();
			} catch (Exception e) {
			}
		}
		ConfigPool.FORWARDER_CLIENT_MAP.remove(Convert.toInt(map.get("localPort")));
		
		sqliteHelper.executeUpdate("delete from mapping_config where id='" + mappingId + "'");
		
		return "true";
	}

	@RenderBody(RenderBodyType.TEXT)
	public String doUpdateMapping(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String id = reqModel.getParam("id");
		String name = reqModel.getParam("name");
		String clientKey = reqModel.getParam("clientKey");
		String remoteHost = reqModel.getParam("remoteHost");
		Integer remotePort = Convert.toInt(reqModel.getParam("remotePort"));
		String natHost = reqModel.getParam("natHost");
		Integer natPort = Convert.toInt(reqModel.getParam("natPort"));
		Integer localPort = Convert.toInt(reqModel.getParam("localPort"));
		Integer downMbps = Convert.toInt(reqModel.getParam("downMbps"));
		Integer upMbps = Convert.toInt(reqModel.getParam("upMbps"));

		JSONObject jsonObject = new JSONObject();

		
		// 判断端口
		String sql = "select * from mapping_config where localPort='" + localPort + "'";
		if (!StrUtil.isBlank(id)) {
			sql += " and id != '" + id + "'";
		}
		Map<String, Object> map = sqliteHelper.executeQueryForMap(sql);
		if (map != null) {
			jsonObject.put("success", "false");
			jsonObject.put("msg", "本地端口已占用请重新输入！");
			return jsonObject.toString();
		}

		// 判断名称
		sql = "select * from mapping_config where name='" + name + "'";
		if (!StrUtil.isBlank(id)) {
			sql += " and id != '" + id + "'";
		}
		map = sqliteHelper.executeQueryForMap(sql);
		if (map != null) {
			jsonObject.put("success", "false");
			jsonObject.put("msg", "映射名称已存在请重新输入！");
			return jsonObject.toString();
		}
		
		try {
			ForwarderClientConfig config = new ForwarderClientConfig(new InetSocketAddress(remoteHost, remotePort),
					new InetSocketAddress(natHost, natPort), localPort, downMbps, upMbps);
			ForwarderClientGetter getter = new ForwarderClientGetter(config, clientKey);
			ForwarderClient client = getter.getForwarderClient();
			
			ConfigPool.FORWARDER_CLIENT_MAP.put(localPort, client);
			
		} catch (InterruptedException | IOException | DragoniteException | IncorrectHeaderException
				| ServerRejectedException e) {
			e.printStackTrace();
			jsonObject.put("success", "false");
			jsonObject.put("msg", "创建失败:" + e.getMessage());
			return jsonObject.toString();
		}

		sqliteHelper.executeUpdate(
				"insert into mapping_config(name,clientKey,remoteHost,remotePort,natHost,natPort,localPort,downMbps,upMbps,enable)"
						+ " values('" + name + "','" + clientKey + "','" + remoteHost + "','" + remotePort + "','"
						+ natHost + "','" + natPort + "','" + localPort + "','" + downMbps + "','" + upMbps + "', 'true')");

		jsonObject.put("success", "true");
		return jsonObject.toString();
	}
	@RenderBody(RenderBodyType.TEXT)
	public String doExitSystem(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		System.exit(1);
		return "true";
	}
	
}
