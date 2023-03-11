package com.redhat.rhn.domain.action.contentmgmt;

import com.redhat.rhn.domain.action.Action;

public class ContentManagementAction extends Action {

    private ContentManagementActionDetails details;

    /**
     * Return the details.
     * @return details
     */
    public ContentManagementActionDetails getDetails() {
        return details;
    }

    /**
     * Set the details.
     * @param detailsIn details
     */
    public void setDetails(ContentManagementActionDetails detailsIn) {
        if (detailsIn != null) {
            detailsIn.setParentAction(this);
        }
        this.details = detailsIn;
    }
}
