package org.huangjiaqqian.smartproxy.p2p.client.config;

import java.sql.SQLException;

import org.huangjiaqqian.smartproxy.common.db.SqliteHelper;

public class DbConfig {
	public static final String DB_ROOT = "db";
	public static final String DB_ROOT_PATH = System.getProperty("user.dir") + "/" + DB_ROOT + "/";
	public static final String DEFAULT_DB_PATH = DB_ROOT_PATH + "proxy_p2p_client.db";
	
	
	public static final SqliteHelper getDefaultSqliteHelper() {
		return SqliteHelper.getDefaultSqliteHelper(DbConfig.DEFAULT_DB_PATH);
	}
	
	public static final void initSql() {
		try {
			getDefaultSqliteHelper().executeUpdate("update mapping_config set enable='false'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
