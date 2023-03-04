/*
 * Copyright (c) 2023 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package com.redhat.rhn.domain.recurringactions.type;

import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Recurring Action Type for highstate implementation
 */

@Entity
@DiscriminatorValue("highstate")
public class RecurringHighstateType extends RecurringActionType {

    /**
     * Standard constructor
     */
    public RecurringHighstateType() {
    }

    /**
     * Constructor
     *
     * @param testModeIn if action is in testMode
     */
    public RecurringHighstateType(boolean testModeIn) {
        super();
        setTestMode(testModeIn);
    }

    @Override
    @Transient
    public ActionType getActionType() {
        return ActionType.HIGHSTATE;
    }

    /**
     * Gets if action is in testMode.
     *
     * @return testMode - if action is testMode
     */
    @Transient
    public boolean isTestMode() {
        return (boolean) getTypeData().get("testMode");
    }

    /**
     * Sets testMode.
     *
     * @param test - testMode
     */
    public void setTestMode(boolean test) {
        Map<String, Object> data = getTypeData();
        data.put("testMode", test);
        setTypeData(data);
    }
}
