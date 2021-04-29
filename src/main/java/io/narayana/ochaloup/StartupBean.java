package io.narayana.ochaloup;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
import java.sql.Statement;

@Singleton
@Startup
public class StartupBean {
    private static final Logger log = Logger.getLogger(StartupBean.class);

    @Resource(lookup = "java:jboss/datasources/xaDs")
    private DataSource datasource;

    @PostConstruct
    public void init() {
        try (Statement st = datasource.getConnection().createStatement()) {
            st.execute("create table test(name VARCHAR)");
        } catch (Exception e) {
            log.info("Trouble to create table 'test' :: " + e.getMessage());
        }
    }
}
