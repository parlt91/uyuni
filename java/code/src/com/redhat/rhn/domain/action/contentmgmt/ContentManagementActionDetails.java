package com.redhat.rhn.domain.action.contentmgmt;

import com.redhat.rhn.domain.action.ActionChild;
import com.redhat.rhn.domain.contentmgmt.ContentEnvironment;
import com.redhat.rhn.domain.contentmgmt.ContentProject;

public class ContentManagementActionDetails extends ActionChild {

    private long id;
    private long actionId;
    private ContentProject project;
    private ContentEnvironment env;


    public long getId() {
        return id;
    }

    public void setId(long idIn) {
        id = idIn;
    }

    public ContentProject getProject() {
        return project;
    }

    public void setProject(ContentProject projectIn) {
        this.project = projectIn;
    }

    public ContentEnvironment getEnv() {
        return env;
    }

    public void setEnv(ContentEnvironment envIn) {
        this.env = envIn;
    }

    public long getActionId() {
        return actionId;
    }

    public void setActionId(long actionIdIn) {
        this.actionId = actionIdIn;
    }
}
