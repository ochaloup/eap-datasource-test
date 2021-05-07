package io.narayana.ochaloup;

import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.sql.Statement;

@Singleton
@Startup
public class StartupBean {
    private static final Logger log = Logger.getLogger(StartupBean.class);

    @PostConstruct
    public void init() {
        try (Statement st = DatasourceUtils.getXADs().getConnection().createStatement()) {
            st.execute("create table test(name VARCHAR)");
        } catch (Exception e) {
            log.info("Trouble to create table 'test' :: " + e.getMessage());
        }
    }
}
