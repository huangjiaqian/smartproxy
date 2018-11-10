package org.huangjiaqqian.smartproxy.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pmw.tinylog.Logger;

public class SqliteHelper {

	private static SqliteHelper DEFAULT_SQLITE_HELPER;

	private Connection connection;
	private Statement statement;
	private ResultSet resultSet;
	private String dbFilePath;

	/**
	 * 构造函数
	 * 
	 * @param dbFilePath
	 *            sqlite db 文件路径
	 
	 * @throws SQLException
	 */
	public SqliteHelper(String dbFilePath) throws SQLException {
		this.dbFilePath = dbFilePath;
		connection = getConnection(dbFilePath);
	}

	public static final void initDefaultSqliteHelper(String dbFilePath) throws SQLException {
		try {
			if(DEFAULT_SQLITE_HELPER == null) {
				DEFAULT_SQLITE_HELPER = new SqliteHelper(dbFilePath);				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static final SqliteHelper getDefaultSqliteHelper(String dbFilePath) {
		try {
			initDefaultSqliteHelper(dbFilePath);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return DEFAULT_SQLITE_HELPER;
	}

	/**
	 * 获取数据库连接
	 * 
	 * @param dbFilePath
	 *            db文件路径
	 * @return 数据库连接
	 
	 * @throws SQLException
	 */
	public synchronized Connection getConnection(String dbFilePath) throws SQLException {
		Connection conn = null;
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
		return conn;
	}

	/**
	 * 执行sql查询
	 * 
	 * @param sql
	 *            sql select 语句
	 * @param rse
	 *            结果集处理类对象
	 * @return 查询结果
	 * @throws SQLException
	 
	 */
	public synchronized <T> T executeQuery(String sql, ResultSetExtractor<T> rse) throws SQLException {
		try {
			resultSet = getStatement().executeQuery(sql);
			T rs = rse.extractData(resultSet);
			return rs;
		} finally {
			destroyed();
		}
	}

	public synchronized Map<String, Object> executeQueryForMap(String sql) throws SQLException {
		List<Map<String, Object>> results = executeQueryForList(sql);
		if (results == null || results.isEmpty()) {
			return null;
		}
		return results.get(0);
	}

	public synchronized List<Map<String, Object>> executeQueryForList(String sql) throws SQLException {
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

		resultSet = getStatement().executeQuery(sql);
		ResultSetMetaData md = resultSet.getMetaData(); // 获得结果集结构信息,元数据
		int columnCount = md.getColumnCount(); // 获得列数
		while (resultSet.next()) {
			Map<String, Object> rowData = new HashMap<String, Object>();
			for (int i = 1; i <= columnCount; i++) {
				rowData.put(md.getColumnLabel(i), resultSet.getObject(i));
			}
			results.add(rowData);
		}
		return results;
	}

	/**
	 * 执行select查询，返回结果列表
	 * 
	 * @param sql
	 *            sql select 语句
	 * @param rm
	 *            结果集的行数据处理类对象
	 * @return
	 * @throws SQLException
	 
	 */
	public synchronized <T> List<T> executeQuery(String sql, RowMapper<T> rm) throws SQLException {
		List<T> rsList = new ArrayList<T>();
		try {
			resultSet = getStatement().executeQuery(sql);
			while (resultSet.next()) {
				rsList.add(rm.mapRow(resultSet, resultSet.getRow()));
			}
		} finally {
			destroyed();
		}
		return rsList;
	}

	/**
	 * 执行数据库更新sql语句
	 * 
	 * @param sql
	 * @return 更新行数
	 * @throws SQLException
	 
	 */
	public synchronized int executeUpdate(String sql) throws SQLException {
		try {
			int c = getStatement().executeUpdate(sql);
			return c;
		} finally {
			destroyed();
		}

	}

	/**
	 * 执行多个sql更新语句
	 * 
	 * @param sqls
	 * @throws SQLException
	 
	 */
	public synchronized void executeUpdate(String... sqls) throws SQLException {
		try {
			for (String sql : sqls) {
				getStatement().executeUpdate(sql);
			}
		} finally {
			destroyed();
		}
	}

	/**
	 * 执行数据库更新 sql List
	 * 
	 * @param sqls
	 *            sql列表
	 * @throws SQLException
	 
	 */
	public synchronized void executeUpdate(List<String> sqls) throws SQLException {
		try {
			for (String sql : sqls) {
				getStatement().executeUpdate(sql);
			}
		} finally {
			destroyed();
		}
	}

	private Connection getConnection() throws SQLException {
		if (null == connection)
			connection = getConnection(dbFilePath);
		return connection;
	}

	private Statement getStatement() throws SQLException {
		if (null == statement)
			statement = getConnection().createStatement();
		return statement;
	}

	/**
	 * 数据库资源关闭和释放
	 */
	public void destroyed() {
		try {
			if (null != connection) {
				connection.close();
				connection = null;
			}

			if (null != statement) {
				statement.close();
				statement = null;
			}

			if (null != resultSet) {
				resultSet.close();
				resultSet = null;
			}
		} catch (SQLException e) {
			Logger.error("Sqlite数据库关闭时异常", e);
		}
	}
}
