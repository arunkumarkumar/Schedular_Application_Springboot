package com.teleapps.schedulemate.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

public class DataBaseUtility {
	

	private static final Logger logger = LogManager.getLogger(DataBaseUtility.class);

	public SortedMap<String, List<String>> getTableDetails(String connectionString, String userName, String password, String tableName) {
		try (Connection connection = DriverManager.getConnection(connectionString, userName, new Cryptography().getPlainText(password))) {
	        DatabaseMetaData metaData = connection.getMetaData();
	        try (ResultSet tableResultSet = metaData.getTables(null, null, tableName, null)) {
	            if (!tableResultSet.next()) {
	                logger.warn("Table '{}' does not exist.", tableName);
	                return Collections.emptySortedMap();
	            }
	        }
	        try (ResultSet columnResultSet = metaData.getColumns(null, null, tableName, null)) {
	        	SortedMap<String, List<String>> tableMetaData = new TreeMap<>();
	            while (columnResultSet.next()) {
	                tableMetaData.put(columnResultSet.getString(Constants.COLUMN_NAME),
	                        new ArrayList<>(Arrays.asList(columnResultSet.getString(Constants.TYPE_NAME),
	                                columnResultSet.getString(Constants.COLUMN_SIZE))));
	            }
	            return tableMetaData;
	        }
	    } catch (Exception ex) {
	        logger.error("Exception: {}", StackTrace.getMessage(ex));
	        return Collections.emptySortedMap();
	    }
	}
	
	public SortedMap<String, String> getStoredProcedureDetails(String connectionString, String userName, String password, String procedureName) {
	    try (Connection connection = DriverManager.getConnection(connectionString, userName, new Cryptography().getPlainText(password))) {
	        DatabaseMetaData databaseMetaData = connection.getMetaData();
	        ResultSet procedureResultSet = databaseMetaData.getProcedures(null, null, procedureName);
	        if (!procedureResultSet.next()) {
	            logger.warn("Stored procedure '{}' does not exist.", procedureName);
	            return Collections.emptySortedMap();
	        }
	        ResultSet resultSet = databaseMetaData.getProcedureColumns(null, null, procedureName, null);
	        SortedMap<String, String> procedureMetaData = new TreeMap<>();
	        while (resultSet.next()) {
	            procedureMetaData.put(resultSet.getString(Constants.COLUMN_NAME).replace("@", ""), resultSet.getString(Constants.TYPE_NAME));
	        }
	        return procedureMetaData;
	    } catch (Exception ex) {
	        logger.error("Exception: {}", StackTrace.getMessage(ex));
	        return Collections.emptySortedMap();
	    }
	}
	
	public boolean insertRecordsToTable(String connectionString, String userName, String password, String query, List<JsonObject> recordsList, SortedMap<String, List<String>> tableMetaData) {
	    Connection connection = null;
	    try {
	        connection = DriverManager.getConnection(connectionString, userName, new Cryptography().getPlainText(password));
	        connection.setAutoCommit(false); // Disable auto-commit
	        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
	            preparedStatement.clearParameters();
	            Map<String, Class<?>> dataTypeMap = createDataTypeMap();
	            for (JsonObject records : recordsList) {
	                AtomicInteger counter = new AtomicInteger(1);
	                records.keySet().stream().sorted().collect(Collectors.toList()).forEach(key -> {
	                    int position = counter.getAndIncrement();
	                    try {
	                        String dataType = tableMetaData.get(key).get(0).toLowerCase();
	                        Class<?> classType = dataTypeMap.getOrDefault(dataType, null);
	                        if (String.class.equals(classType)) {
	                            preparedStatement.setString(position, records.get(key).getAsString());
	                        } else if (Integer.class.equals(classType)) {
	                            preparedStatement.setInt(position, records.get(key).getAsInt());
	                        } else if (Double.class.equals(classType) ) {
	                            preparedStatement.setDouble(position, records.get(key).getAsDouble());
	                        } else {
	                            logger.warn("Unsupported data type for key '{}' configuring fallback datatype to string", tableMetaData.get(key).get(0));
	                            preparedStatement.setString(position, records.get(key).getAsString());
	                        }
	                    } catch (SQLException ex) {
	                        logger.error("Failed to set value for key '{}': {}", key, ex.getMessage());
	                    }
	                });
	                counter.set(1);
	                preparedStatement.addBatch();
	            }
	            long[] result = preparedStatement.executeLargeBatch();
	            for (long row : result) {
	                if (row == Statement.EXECUTE_FAILED) {
	                    logger.error("Transaction failure. Initiate rollback");
	                    connection.rollback();
	                    return false;
	                }
	            }
	            connection.commit();
	            logger.info("Batch insert completed successfully.");
	            return true;
	        } catch (SQLException ex) {
	            logger.error("Error performing batch insert. Rolling back the transaction: {}", ex.getMessage());
	            return false;
	        }
	    } catch (Exception ex) {
	        logger.error("Exception while establishing a database connection: {}", ex.getMessage());
	        return false;
	    } finally {
	        if (connection != null) {
	            try {
	                connection.close();
	            } catch (SQLException ex) {
	                logger.error("Failed to close the Connection: {}", ex.getMessage());
	            }
	        }
	    }
	}
	
	public boolean executeStoreProcedure(String connectionString, String userName, String password, String query, JsonObject parameters, SortedMap<String, String> tableMetaData) {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(connectionString, userName, new Cryptography().getPlainText(password));
			try (CallableStatement callableStatement = connection.prepareCall(query)) {
				Map<String, Class<?>> dataTypeMap = createDataTypeMap();
				AtomicInteger counter = new AtomicInteger(1);
                parameters.keySet().stream().collect(Collectors.toList()).forEach(key -> {
                    int position = counter.getAndIncrement();
                    try {
                        String dataType = tableMetaData.get(key).toLowerCase();
                        ExpressionResolver expressionResolver = new ExpressionResolver();
                        Class<?> classType = dataTypeMap.getOrDefault(dataType, null);
                        if (String.class.equals(classType)) {
                        	callableStatement.setString(position, expressionResolver.resolve(parameters.get(key).getAsString()));
                        } else if (Integer.class.equals(classType)) {
                        	callableStatement.setInt(position, Integer.parseInt(expressionResolver.resolve(parameters.get(key).getAsString())));
                        } else if (Double.class.equals(classType) ) {
                        	callableStatement.setDouble(position, Double.parseDouble(expressionResolver.resolve(parameters.get(key).getAsString())));
                        } else {
                            logger.warn("Unsupported data type for key '{}' configuring fallback datatype to string", tableMetaData.get(key));
                            callableStatement.setString(position, expressionResolver.resolve(parameters.get(key).getAsString()));
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to set value for key '{}': {}", key, StackTrace.getMessage(ex));
                    }
                });
				callableStatement.execute();
				return true;
			}
		} catch (Exception ex) {
			logger.error("Exception while establishing a database connection: {}", StackTrace.getMessage(ex));
			return false;
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception ex) {
					logger.error("Failed to close the Connection: {}", StackTrace.getMessage(ex));
				}
			}
		}
	}
	
	public String buildInsertQuery(SortedMap<String, List<String>> tableMetaData, String tableName) {
		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : tableMetaData.entrySet()) {
		    String key = entry.getKey();
		    if (columns.length() > 0) {
		        columns.append(", ");
		        values.append(", ");
		    }
		    columns.append(key);
		    values.append("?");
		}
		return String.format("INSERT INTO %s (%s) VALUES (%s);", tableName, columns, values);
	}
	
	public String buildProcedureCallString(String procedureName, int parameterCount) {
	    if (parameterCount <= 0) {
	        return "{ call " + procedureName + " () }";
	    }
	    String parameters = IntStream.range(0, parameterCount).mapToObj(iterator -> "?").collect(Collectors.joining(","));
	    return "{ call " + procedureName + " (" + parameters + ") }";
	}
	
	private Map<String, Class<?>> createDataTypeMap() {
	    Map<String, Class<?>> dataTypeMap = new HashMap<>();
	    dataTypeMap.put("string", String.class);
	    dataTypeMap.put("varchar", String.class);
	    dataTypeMap.put("nvarchar", String.class);
	    dataTypeMap.put("char", String.class);
	    dataTypeMap.put("nchar", String.class);
	    dataTypeMap.put("text", String.class);
	    dataTypeMap.put("date", String.class);
	    dataTypeMap.put("time", String.class);
	    dataTypeMap.put("datetime2", String.class);
	    dataTypeMap.put("datetime", String.class);
	    dataTypeMap.put("timestamp", String.class);
	    dataTypeMap.put("int", Integer.class);
	    dataTypeMap.put("tinyint", Integer.class);
	    dataTypeMap.put("smallint", Integer.class);
	    dataTypeMap.put("integer", Integer.class);
	    dataTypeMap.put("float", Double.class);
	    dataTypeMap.put("double", Double.class);
	    return dataTypeMap;
	}
}