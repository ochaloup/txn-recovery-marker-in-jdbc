package org.jboss.openshift.txrecovery;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * DTO for gathering name of application pod and
 * the corresponding recovery pod name which the recovery pod works at.
 */
@Entity
@Table(name = ApplicationRecoveryPod.TABLE_NAME)
public class ApplicationRecoveryPod {
    public static final String TABLE_NAME = "RECOVERY_MARKER";

    @EmbeddedId private RecoveryInProgressRecordId id;

    public ApplicationRecoveryPod() {
        // constructor needed by Hibernate
    }

    public ApplicationRecoveryPod(String applicationPodName, String recoveryPodName) {
        this.id = new RecoveryInProgressRecordId();
        id.applicationPodName = applicationPodName;
        id.recoveryPodName = recoveryPodName;
    }

    ApplicationRecoveryPod setApplicationPodName(String applicationPodName) {
        this.id.applicationPodName = applicationPodName;
        return this;
    }
    ApplicationRecoveryPod setRecoveryPodName(String recoveryPodName) {
        this.id.recoveryPodName = recoveryPodName;
        return this;
    }

    public String getApplicationPodName() {
        return this.id.applicationPodName;
    }
    public String getRecoveryPodName() {
        return this.id.recoveryPodName;
    }

    @Override
    public String toString() {
        return String.format("app pod name: %s, recovery pod name: %s",
            getApplicationPodName(), getRecoveryPodName());
    }
}

@Embeddable
class RecoveryInProgressRecordId implements Serializable {
    private static final long serialVersionUID = 1L;

    String applicationPodName;
    String recoveryPodName;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((applicationPodName == null) ? 0 : applicationPodName.hashCode());
        result = prime * result + ((recoveryPodName == null) ? 0 : recoveryPodName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RecoveryInProgressRecordId other = (RecoveryInProgressRecordId) obj;
        if (applicationPodName == null) {
            if (other.applicationPodName != null)
                return false;
        } else if (!applicationPodName.equals(other.applicationPodName))
            return false;
        if (recoveryPodName == null) {
            if (other.recoveryPodName != null)
                return false;
        } else if (!recoveryPodName.equals(other.recoveryPodName))
            return false;
        return true;
    }

}