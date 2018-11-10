package org.huangjiaqqian.smartproxy.server.web.action;

import java.sql.SQLException;
import java.util.Map;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;
import org.huangjiaqqian.smartproxy.common.web.action.ReqModel;
import org.huangjiaqqian.smartproxy.common.web.action.RespMpdel;
import org.huangjiaqqian.smartproxy.common.web.annotation.RenderBody;
import org.huangjiaqqian.smartproxy.common.web.enums.RenderBodyType;
import org.huangjiaqqian.smartproxy.server.config.db.DbConfig;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import net.freeutils.httpserver.HTTPServer.Request;

public class LoginAction {
	private SqliteHelper sqliteHelper = DbConfig.getDefaultSqliteHelper();

	@RenderBody(RenderBodyType.TEMPLATE)
	public String index(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		return "login.html";
	}

	@RenderBody(RenderBodyType.TEXT)
	public String doLogin(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		String username = Convert.toStr(reqModel.getParam("username"));
		String password = Convert.toStr(reqModel.getParam("password"));

		Map<String, Object> map = sqliteHelper.executeQueryForMap(
				"select * from admin where username='" + username + "' and password='" + password + "'");

		if (map == null) {
			return "用户名或密码错误!";
		}

		reqModel.getSession().setAttr("_loginUser", map);

		return "true";
	}

	@RenderBody(RenderBodyType.TEXT)
	public String doExitLogin(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		reqModel.getSession().removeAttr("_loginUser");
		return "true";
	}

	@SuppressWarnings("unchecked")
	@RenderBody(RenderBodyType.TEXT)
	public String editUser(Request request, ReqModel reqModel, RespMpdel respMpdel) throws SQLException {
		Map<String, Object> map = (Map<String, Object>) reqModel.getSession().getAttr("_loginUser");
		if(map == null) {
			return "false";
		}
		Integer id = Convert.toInt(map.get("id"));
		String username = Convert.toStr(reqModel.getParam("username"));
		String password = Convert.toStr(reqModel.getParam("password"));
		if (StrUtil.isEmpty(username) || StrUtil.isEmpty(password)) {
			return "false";
		}

		sqliteHelper.executeUpdate(
				"update admin set username='" + username + "', password='" + password + "' where id='" + id + "'");

		map = sqliteHelper.executeQueryForMap("select * from admin where id='" + id + "'");
		reqModel.getSession().setAttr("_loginUser", map); //更新登录信息
		
		return "true";
	}

}
