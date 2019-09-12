package com.markssoftware.microscheduler.config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.simpl.SimpleThreadPool;

import java.util.Optional;
import java.util.Properties;

@Value
@Builder(toBuilder = true)
public class Config {
    String instanceName = "MyClusteredScheduler";
    String instanceId = "AUTO";
    String threadPoolClass = SimpleThreadPool.class.getName();
    String threadCount = "25";
    String threadPriority = "5";
    String misfireThreshold = "60000";
    String jobStoreClass = org.quartz.impl.jdbcjobstore.JobStoreTX.class.getName();
    String driverDelegateClass = StdJDBCDelegate.class.getName();
    String jobStoreUseProperties = "false";
    String jobStoreDataSource = "myDS";
    String jobStoreTablePrefix = "QRTZ_";
    String jobStoreIsClustered = "true";
    String jobStoreClusterCheckinInterval = "20000";
    String myDsDriver = "com.mysql.cj.jdbc.Driver";
    @NonNull
    String mysqlHost = getEnvOrDefault("mysql_host", "localhost");
    String rabbitmqHost = getEnvOrDefault("rabbitmq_host", "localhost");
    String myDsUser = "quartz";
    String myDsPassword = "quartz";
    String myDsMaxConnections = "5";

    private String getEnvOrDefault(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
    }

    public String getMyDsUrl() {
        return "jdbc:mysql://" + mysqlHost + "/quartz";
    }

    public Properties getProperties() {
        final Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", instanceName);
        properties.setProperty("org.quartz.scheduler.instanceId", instanceId);
        properties.setProperty("org.quartz.threadPool.class", threadPoolClass);
        properties.setProperty("org.quartz.threadPool.threadCount", threadCount);
        properties.setProperty("org.quartz.threadPool.threadPriority", threadPriority);
        properties.setProperty("org.quartz.jobStore.misfireThreshold", misfireThreshold);
        properties.setProperty("org.quartz.jobStore.class", jobStoreClass);
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", driverDelegateClass);
        properties.setProperty("org.quartz.jobStore.useProperties", jobStoreUseProperties);
        properties.setProperty("org.quartz.jobStore.dataSource", jobStoreDataSource);
        properties.setProperty("org.quartz.jobStore.tablePrefix", jobStoreTablePrefix);
        properties.setProperty("org.quartz.jobStore.isClustered", jobStoreIsClustered);
        properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", jobStoreClusterCheckinInterval);
        properties.setProperty("org.quartz.dataSource.myDS.driver", myDsDriver);
        properties.setProperty("org.quartz.dataSource.myDS.URL", getMyDsUrl());
        properties.setProperty("org.quartz.dataSource.myDS.user", myDsUser);

        properties.setProperty("org.quartz.dataSource.myDS.password", myDsPassword);
        properties.setProperty("org.quartz.dataSource.myDS.maxConnections", myDsMaxConnections);
        properties.setProperty("org.quartz.dataSource.myDS.validationQuery", "select 0 from dual");
        return properties;
    }


}
