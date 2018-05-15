package org.jboss.openshift;

public enum ArgsOptions {
    CREATE("create"),
    GET("get"),
    DELETE_BY_APPLICATION_POD("delete_by_application"),
    DELETE_BY_RECOVERY_POD("delete_by_recovery");

    private String option;
    
    private ArgsOptions(String option) {
        this.option = option;
    }

    public String getOption() {
        return option;
    }
}
