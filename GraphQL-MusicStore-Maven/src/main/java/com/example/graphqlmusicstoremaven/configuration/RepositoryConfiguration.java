package com.example.graphqlmusicstoremaven.configuration;

import com.example.graphqlmusicstoremaven.repository.TrackStore;
import com.example.graphqlmusicstoremaven.repository.jooq.JooqTrackStore;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RepositoryConfiguration {

    @Bean
    DataSource getMysqlDataSource(
            @Value("${mysql.server}") final String serverName,
            @Value("${mysql.database}") final String database,
            @Value("${mysql.port}") final Integer port,
            @Value("${spring.datasource.username}") final String username,
            @Value("${spring.datasource.password}") final String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName(serverName);
        dataSource.setDatabaseName(database);
        dataSource.setPortNumber(port);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    DSLContext getContext(final DataSource dataSource) {
        return DSL.using(dataSource, SQLDialect.MYSQL);
    }

    @Bean
    TrackStore getTrackStore(final DSLContext context) {
        return new JooqTrackStore(context);
    }

}
