package io.narayana.ochaloup;

import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Statement;

@Stateless
public class DatasourceEjb {
    private static final Logger log = Logger.getLogger(DatasourceEjb.class);
    private static final String JNDI_XA_DS = "java:jboss/datasources/xaDs";

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Resource(lookup = JNDI_XA_DS)
    private DataSource datasource;
    
    public void crashWithPreparedTxn() {
        try {
            TestXAResource testXAResource = new TestXAResource(TestXAResource.TestAction.COMMIT_CRASH_VM);
            log.info("transaction: " + tm.getTransaction());
            tm.getTransaction().enlistResource(testXAResource);
        } catch (Exception e) {
            throw new RuntimeException("Cannot enlist", e);
        }

        try (Statement st = datasource.getConnection().createStatement()) {
            st.execute("insert into test values ('TESTING')");
        } catch (Exception e) {
            throw new RuntimeException("Error on inserting data to table 'test'", e);
        }
    }

    public void recover() {
        try {
            Xid[] xids = RecoveryHelper.getNewXAResource(JNDI_XA_DS).recover(XAResource.TMSTARTRSCAN & XAResource.TMENDRSCAN);
            int i = 1;
            log.infof("Number of xids: %n", xids.length);
            for (Xid xid : xids) {
                log.infof("[%n] %s", i, xid);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error on list Xids", e);
        }
    }
}
