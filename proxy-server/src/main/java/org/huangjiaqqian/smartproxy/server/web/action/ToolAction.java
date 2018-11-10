package org.huangjiaqqian.smartproxy.server.web.action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.annotation.RenderBody;
import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;
import org.huangjiaqqian.smartproxy.server.cache.ClientPool;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;
import org.huangjiaqqian.smartproxy.server.tunnel.ProxyServerTunnel;
import org.huangjiaqqian.smartproxy.server.web.interceptor.PageInterceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import net.freeutils.httpserver.HTTPServer.Request;

public class ToolAction {
	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	@RenderBody(RenderBodyType.TEXT)
	public String index(Request request, ReqModel reqModel, RespMpdel respMpdel) {
		return "";
	}

	@RenderBody(RenderBodyType.TEXT)
	public String genClientKey(Request request, ReqModel reqModel, RespMpdel respMpdel) {
		String key = UUID.randomUUID().toString();
		key = key.replaceAll("-", "") + DateUtil.format(new Date(), "yyyyMMdd");
		return key;
	}

	@RenderBody(RenderBodyType.TEXT)
	public String updateClient(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String id = reqModel.getParam("id");
		String name = reqModel.getParam("name");
		String clientKey = reqModel.getParam("clientKey");

		JSONObject jsonObject = new JSONObject();

		String sql = "select * from proxy_client where name='" + name + "'";
		if (!StrUtil.isBlank(id)) {
			sql += " and id != '" + id + "'";
		}
		Map<String, Object> map = sqliteHelper.executeQueryForMap(sql);
		if (map != null) {
			jsonObject.put("success", "false");
			jsonObject.put("msg", "名称已存在请重新输入！");
			return jsonObject.toString();
		}

		if (StrUtil.isBlank(id)) {
			// 新增
			sqliteHelper.executeUpdate(
					"insert into proxy_client(name, clientKey) values('" + name + "','" + clientKey + "')");
		} else {
			// 修改
			sqliteHelper.executeUpdate(
					"update proxy_client set name='" + name + "', clientKey='" + clientKey + "' where id='" + id + "'");
		}

		jsonObject.put("success", "true");
		return jsonObject.toString();
	}

	@RenderBody(RenderBodyType.TEXT)
	public String updateConfig(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String id = reqModel.getParam("id");
		String name = reqModel.getParam("name");
		String inetPort = reqModel.getParam("inetPort");
		String lanHost = reqModel.getParam("lanHost");
		String lanPort = reqModel.getParam("lanPort");

		String clientId = reqModel.getParam("clientId");

		JSONObject jsonObject = new JSONObject();

		// 判断端口
		String sql = "select * from proxy_client_config where inetPort='" + inetPort + "'";
		if (!StrUtil.isBlank(id)) {
			sql += " and id != '" + id + "'";
		}
		Map<String, Object> map = sqliteHelper.executeQueryForMap(sql);
		if (map != null) {
			jsonObject.put("success", "false");
			jsonObject.put("msg", "公网端口已占用请重新输入！");
			return jsonObject.toString();
		}

		// 判断名称
		sql = "select * from proxy_client_config where name='" + name + "' and proxy_client_id = '" + clientId + "'";
		if (!StrUtil.isBlank(id)) {
			sql += " and id != '" + id + "'";
		}
		map = sqliteHelper.executeQueryForMap(sql);
		if (map != null) {
			jsonObject.put("success", "false");
			jsonObject.put("msg", "名称已存在请重新输入！");
			return jsonObject.toString();
		}

		ProxyServerTunnel tunnel = ClientPool.NEW_CLIENT_TUNNEL_MAP.get(Convert.toInt(clientId));
		if (StrUtil.isBlank(id)) {
			// 新增
			if (tunnel != null) {
				try {
					tunnel.startSocketListener(Convert.toInt(inetPort),
							new InetSocketAddress(lanHost, Convert.toInt(lanPort)));
				} catch (IOException | InterruptedException e) {
					Object errorMsg = e.getStackTrace();
					if (e instanceof java.net.BindException) {
						errorMsg = "端口（" + inetPort + "）已被占用！";
					}

					// e.printStackTrace();
					jsonObject.put("success", "false");
					jsonObject.put("msg", "添加失败！" + errorMsg);
					return jsonObject.toString();
				}
			}

			sqliteHelper.executeUpdate("update proxy_client_config set enable='true' where proxy_client_id='"
					+ tunnel.getProxyClientId() + "' and inetPort='" + inetPort + "'");

			sqliteHelper.executeUpdate(
					"insert into proxy_client_config(name,inetPort,lanHost,lanPort,proxy_client_id, enable) values('"
							+ name + "', '" + inetPort + "', '" + lanHost + "', '" + lanPort + "', '" + clientId
							+ "', 'true')");

		} else {
			// 修改
			map = sqliteHelper.executeQueryForMap("select * from proxy_client_config where id='" + id + "'");
			if (!Convert.toStr(map.get("inetPort")).equals(inetPort)) {
				// 外网端口已修改
				if (tunnel != null) {
					try {
						tunnel.closeListener(Convert.toInt(map.get("inetPort"))); // 关闭原来端口
						tunnel.startSocketListener(Convert.toInt(inetPort),
								new InetSocketAddress(lanHost, Convert.toInt(lanPort))); // 启用新端口
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						jsonObject.put("success", "false");
						jsonObject.put("msg", "修改失败！" + e.getStackTrace());
						return jsonObject.toString();
					}
				}
				/** 端口流量转移 **/
				if (ProxyServerTunnel.downFlowMap != null) {
					Double downFlow = ProxyServerTunnel.downFlowMap.get(Convert.toInt(map.get("inetPort")));
					Double upFlow = ProxyServerTunnel.upFlowMap.get(Convert.toInt(map.get("inetPort")));

					ProxyServerTunnel.downFlowMap.remove(Convert.toInt(map.get("inetPort")));
					ProxyServerTunnel.upFlowMap.remove(Convert.toInt(map.get("inetPort")));

					ProxyServerTunnel.downFlowMap.put(Convert.toInt(inetPort), downFlow);
					ProxyServerTunnel.upFlowMap.put(Convert.toInt(inetPort), upFlow);
				}
			}

			String enable = tunnel != null ? "true" : "false";
			sqliteHelper.executeUpdate(
					"update proxy_client_config set name='" + name + "', inetPort='" + inetPort + "', lanHost='"
							+ lanHost + "', lanPort='" + lanPort + "', enable='" + enable + "' where id='" + id + "'");
		}

		PageInterceptor.shouldRefreshMenu = true;
		jsonObject.put("success", "true");
		return jsonObject.toString();
	}

	@RenderBody(RenderBodyType.TEXT)
	public String deleteClient(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String id = reqModel.getParam("id");

		ProxyServerTunnel tunnel = ClientPool.NEW_CLIENT_TUNNEL_MAP.get(Convert.toInt(id));

		if (tunnel != null) {
			return "删除失败！用户已在登录状态!";
		}

		sqliteHelper.executeUpdate("delete from proxy_client where id='" + id + "'");
		sqliteHelper.executeUpdate("delete from proxy_client_config where proxy_client_id='" + id + "'");
		return "true";
	}

	@RenderBody(RenderBodyType.TEXT)
	public String deleteConfig(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String id = reqModel.getParam("id");

		Map<String, Object> map = sqliteHelper
				.executeQueryForMap("select * from proxy_client_config where id='" + id + "'");

		ProxyServerTunnel tunnel = ClientPool.NEW_CLIENT_TUNNEL_MAP.get(Convert.toInt(map.get("proxy_client_id")));

		if (tunnel != null) {
			tunnel.closeListener(Convert.toInt(map.get("inetPort"))); // 关闭端口
		}

		PageInterceptor.shouldRefreshMenu = true;

		ProxyServerTunnel.downFlowMap.remove(Convert.toInt(map.get("inetPort")));
		ProxyServerTunnel.upFlowMap.remove(Convert.toInt(map.get("inetPort")));

		sqliteHelper.executeUpdate("delete from proxy_client_config where id='" + id + "'");
		return "true";
	}

}
