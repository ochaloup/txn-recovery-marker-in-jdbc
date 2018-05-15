package org.jboss.openshift;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * DTO for gathering name of application pod and
 * the corresponding recovery pod name
 * which the recovery pod works at.
 */
@Entity
@Table(name = ApplicationRecoveryPodDTO.TABLE_NAME)
public class ApplicationRecoveryPodDTO {
    public static final String TABLE_NAME = "RECOVERY_MARKER";

    @EmbeddedId RecoveryInProgressRecordId id;

    ApplicationRecoveryPodDTO(String applicationPodName, String recoveryPodName) {
        this.id = new RecoveryInProgressRecordId()
            .setApplicationPodName(applicationPodName)
            .setRecoveryPodName(recoveryPodName);
    }

    @Override
    public String toString() {
        return String.format("app pod name: %s, recovery pod name: %s",
            this.id.getApplicationPodName(), this.id.getRecoveryPodName());
    }
}

@Embeddable
class RecoveryInProgressRecordId implements Serializable {
    private static final long serialVersionUID = 1L;

    private String applicationPodName;
    private String recoveryPodName;

    RecoveryInProgressRecordId setRecoveryPodName(String recoveryPodName) {
        this.recoveryPodName = recoveryPodName;
        return this;
    }
    RecoveryInProgressRecordId setApplicationPodName(String applicationPodName) {
        this.applicationPodName = applicationPodName;
        return this;
    }

    public String getApplicationPodName() {
        return applicationPodName;
    }
    public String getRecoveryPodName() {
        return recoveryPodName;
    }
}