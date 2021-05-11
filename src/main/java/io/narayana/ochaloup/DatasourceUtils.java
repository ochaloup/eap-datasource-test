package io.narayana.ochaloup;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public final class DatasourceUtils {
    private DatasourceUtils(){
        // no instantiate
    }

    public static final String JNDI_XA_DS = System.getProperty("xa.ds.jndi.name", "java:jboss/datasources/xaDs");

    public static DataSource getXADs(String xaDs) {
        try {
            return (DataSource) new InitialContext().lookup(JNDI_XA_DS);
        } catch (NamingException ne) {
            throw new IllegalStateException("Cannot lookup the " + JNDI_XA_DS, ne);
        }
    }

    public static DataSource getXADs() {
        return getXADs(JNDI_XA_DS);
    }
}
