package io.narayana.ochaloup;

import com.arjuna.ats.internal.arjuna.FormatConstants;
import com.arjuna.ats.jta.xa.XidImple;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.sql.Statement;

@Stateless
public class DatasourceEjb {
    private static final Logger log = Logger.getLogger(DatasourceEjb.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    public void crashWithPreparedTxn() {
        try {
            TestXAResource testXAResource = new TestXAResource(TestXAResource.TestAction.COMMIT_CRASH_VM);
            log.info("transaction: " + tm.getTransaction());
            tm.getTransaction().enlistResource(testXAResource);
        } catch (Exception e) {
            throw new RuntimeException("Cannot enlist", e);
        }

        try (Statement st = DatasourceUtils.getXADs().getConnection().createStatement()) {
            st.execute("insert into test values ('TESTING')");
        } catch (Exception e) {
            throw new RuntimeException("Error on inserting data to table 'test'", e);
        }
    }

    public void recover(String datasourceJndiName) {
        XAResource xaer = null;
        try {
            xaer = RecoveryHelper.getNewXAResource(datasourceJndiName);
            Xid[] xids = xaer.recover(XAResource.TMSTARTRSCAN);
            int i = 1;
            log.infof("Number of xids: %d", xids.length);
            for (Xid xid : xids) {
                if (xid.getFormatId() == FormatConstants.JTA_FORMAT_ID) {
                    XidImple xidImple = new XidImple(xid);
                    log.infof("[%d] %s | %s | %s", i, xid, xidImple, xidImple.getXID());
                } else {
                    log.infof("[%d] %s (not an Narayana JTA xid)", i, xid);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error on list Xids", e);
        } finally {
            if (xaer != null) {
                try {
                    xaer.recover(XAResource.TMENDRSCAN);
                } catch (Exception recoverene) {
                    log.warnf(recoverene,"Trouble to end the recover on XAResource of '%s'", datasourceJndiName);
                }
            }
        }
    }
}
