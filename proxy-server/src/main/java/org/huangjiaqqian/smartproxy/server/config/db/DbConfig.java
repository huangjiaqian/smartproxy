package org.huangjiaqqian.smartproxy.server.config.db;

import java.sql.SQLException;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;

public class DbConfig {
	public static final String DB_ROOT = "db";
	public static final String DB_ROOT_PATH = System.getProperty("user.dir") + "/" + DB_ROOT + "/";
	public static final String DEFAULT_DB_PATH = DB_ROOT_PATH + "proxyserver.db";
	
	
	public static final SqliteHelper getDefaultSqliteHelper() {
		return SqliteHelper.getDefaultSqliteHelper(DbConfig.DEFAULT_DB_PATH);
	}
	
	public static final void initSql() {
		try {
			getDefaultSqliteHelper().executeUpdate("update proxy_client set status='offline' ");
			getDefaultSqliteHelper().executeUpdate("update proxy_client_config set enable='false' ");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
