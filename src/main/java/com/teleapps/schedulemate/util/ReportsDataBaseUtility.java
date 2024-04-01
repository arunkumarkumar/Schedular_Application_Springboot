package com.teleapps.schedulemate.util;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager", basePackages = {"com.teleapps.schedulemate.repository"})
public class ReportsDataBaseUtility {

	private static Logger logger = LogManager.getLogger(ReportsDataBaseUtility.class);
	
	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Value("${spring.datasource.username}")
	private String username;

	@Value("${spring.datasource.password}")
	private String password;

	@Value("${spring.datasource.driverClassName}")
	private String driver;
	
	@Value("${spring.datasource.hikari.maximum-pool-size}")
	private int maximumPoolSize;

	@Value("${spring.datasource.hikari.minimum-idle}")
	private int minimumIdle;

	@Value("${spring.datasource.hikari.connectionTimeout}")
	private int connectionTimeOut;
	
	

	HikariDataSource hikariConfig() {
		HikariDataSource dataSource = null;
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDriverClassName(driver);
		hikariConfig.setJdbcUrl(dbUrl);
		hikariConfig.setUsername(username);
		hikariConfig.setPassword(new Cryptography().getPlainText(password));

		hikariConfig.setMaximumPoolSize(maximumPoolSize);
		hikariConfig.setMinimumIdle(minimumIdle);
		hikariConfig.setPoolName("TaskX-Pool");
		hikariConfig.setAutoCommit(true);
		hikariConfig.setConnectionTimeout(connectionTimeOut);
		hikariConfig.setLeakDetectionThreshold(60 * 3 * Long.parseLong("1000"));
		try {
			
			dataSource = new HikariDataSource(hikariConfig);
			logger.info("DB Status : Connected");
		} catch (Exception ex) {
			logger.info("Primary URL : " + StackTrace.getMessage(ex));
		}

		return dataSource;
	}
	
	@Bean
	LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(hikariConfig());
		entityManagerFactoryBean.setJpaProperties(hibProperties());
		JpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
		entityManagerFactoryBean.setPackagesToScan("com.teleapps.schedulemate.domain");
		return entityManagerFactoryBean;
	}

	private Properties hibProperties() {
		Properties properties = new Properties();
		String dialect = "org.hibernate.dialect.H2Dialect";
		properties.put("hibernate.dialect", dialect);
		properties.put("hibernate.hbm2ddl.auto", "update");
		return properties;
	}
	@Bean(name = "transactionManager")
	JpaTransactionManager transactionManager() {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
		return transactionManager;
	}

}
