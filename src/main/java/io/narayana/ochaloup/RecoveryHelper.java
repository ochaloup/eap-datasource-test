package io.narayana.ochaloup;

import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.jboss.jca.adapters.jdbc.WrapperDataSource;
import org.jboss.logging.Logger;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.xa.XAResource;

public class RecoveryHelper {
    private static final Logger log = Logger.getLogger(RecoveryHelper.class);

    public static XAResource getNewXAResource(String datasourceName) throws Exception {
        try {
            log.info("getting XA resource of " + datasourceName + " datasource");
            DataSource ds = (DataSource) new InitialContext().lookup(datasourceName);
            WrapperDataSource wds = (WrapperDataSource) ds;
            WrappedConnection connection = (WrappedConnection) wds.getConnection();

            if (!connection.isXA()) {
                throw new RuntimeException("Datasource " + datasourceName + " does not seem to be an XADataSource!");
            }
            return connection.getXAResource();
        } catch (Exception e) {
            log.warn("Cannot get an XAResource of " + datasourceName + " datasource", e);
            throw e;
        }
    }
}
