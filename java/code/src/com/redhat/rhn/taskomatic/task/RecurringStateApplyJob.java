/*
 * Copyright (c) 2020 SUSE LLC
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
package com.redhat.rhn.taskomatic.task;

import com.redhat.rhn.domain.action.contentmgmt.ContentManagementAction;
import com.redhat.rhn.domain.action.salt.ApplyStatesAction;
import com.redhat.rhn.domain.contentmgmt.ContentProject;
import com.redhat.rhn.domain.recurringactions.RecurringAction;
import com.redhat.rhn.domain.recurringactions.RecurringActionFactory;
import com.redhat.rhn.manager.action.ActionChainManager;
import com.redhat.rhn.manager.contentmgmt.ContentManager;
import com.redhat.rhn.taskomatic.TaskoXmlRpcHandler;
import com.redhat.rhn.taskomatic.TaskomaticApiException;

import com.suse.manager.maintenance.MaintenanceManager;

import org.quartz.JobExecutionContext;

import java.util.List;
import java.util.Optional;

/**
 * Used to run a scheduled Recurring Highstate Apply action
 */
public class RecurringStateApplyJob extends RhnJavaJob {

    private static MaintenanceManager maintenanceManager = new MaintenanceManager();

    @Override
    public String getConfigNamespace() {
        return "recurring_state_apply";
    }

    /**
     * Schedule highstate application.
     *
     * If the {@link RecurringAction} data is not found, clean the schedule.
     *
     * {@inheritDoc}
     */
    @Override
    public void execute(JobExecutionContext context) {
        String scheduleName = context.getJobDetail().getKey().getName();
        Optional<RecurringAction> recurringAction = RecurringActionFactory.lookupByJobName(scheduleName);

        recurringAction.ifPresentOrElse(
                action ->  {
                    if (action.isActive()) {
                        scheduleAction(context, action);
                    }
                    else {
                        log.debug(String.format("Action %s not active, skipping", action));
                    }
                },
                () -> cleanSchedule(scheduleName)
        );
    }

    private void scheduleAction(JobExecutionContext context, RecurringAction action) {
        List<Long> minionIds = maintenanceManager.systemIdsMaintenanceMode(action.computeMinions());

        try {
            // TODO: Don't use regex
            String actionType = action.getAction().getActionType().toString().split(" : ")[0];
            switch (actionType) {
                case "states.apply":
                    ActionChainManager.scheduleApplyStates(
                            action.getCreator(),
                            minionIds,
                            Optional.of(((ApplyStatesAction) action.getAction()).getDetails().isTest()),
                            context.getFireTime(),
                            null
                    );
                    break;
                case "content.management":
                    ContentManager cm = new ContentManager();
                    ContentProject project = ((ContentManagementAction) action.getAction()).getDetails().getProject();
                    cm.buildProject(project, Optional.of("Recurring project build of project" + project.getName()),
                            true, action.getCreator());
                    break;
            }
        }
        catch (TaskomaticApiException e) {
            log.error("Error scheduling states application for recurring action {}", action, e);
        }
    }

    private void cleanSchedule(String scheduleName) {
        log.warn("Can't find a recurring action data for schedule '{}'. Cleaning the schedule!", scheduleName);
        int result = new TaskoXmlRpcHandler().unscheduleBunch(null, scheduleName);
        if (result != 1) {
            log.error("Error cleaning schedule '{}'", scheduleName);
        }
    }
}
