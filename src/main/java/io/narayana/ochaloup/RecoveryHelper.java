package io.narayana.ochaloup;

import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.jboss.jca.adapters.jdbc.WrapperDataSource;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import org.jboss.as.connector.subsystems.datasources.WildFlyDataSource;

public class RecoveryHelper {
    private static final Logger log = Logger.getLogger(RecoveryHelper.class);

    public static XAResource getNewXAResource(String datasourceName) throws Exception {
        try {
            log.info("getting XA resource of " + datasourceName + " datasource");
            DataSource ds = DatasourceUtils.getXADs(datasourceName);

            WrappedConnection connection = null;
            if (ds instanceof WrapperDataSource) {
                // IronJacamar direct stuff
                WrapperDataSource wds = (WrapperDataSource) ds;
                connection = (WrappedConnection) wds.getConnection();
            } else if (ds instanceof WildFlyDataSource) {
                WildFlyDataSource wflyds = (WildFlyDataSource) ds;
                if (wflyds.getConnection() instanceof WrappedConnection) {
                    connection = (WrappedConnection) wflyds.getConnection();
                }
                if (wflyds.getConnection().isWrapperFor(XAConnection.class)) {
                    return ((XAConnection) wflyds.getConnection()).getXAResource();
                }
            }
            if (connection.isXA()) {
                return connection.getXAResource();
            }
        } catch (Exception e) {
            log.warn("Cannot get an XAResource of " + datasourceName + " datasource", e);
            throw e;
        }
        throw new IllegalStateException("Cannot find a way how to get XAResource from the datasource " + datasourceName);
    }
}
