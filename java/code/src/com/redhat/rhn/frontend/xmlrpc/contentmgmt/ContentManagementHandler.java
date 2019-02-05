/**
 * Copyright (c) 2019 SUSE LLC
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

package com.redhat.rhn.frontend.xmlrpc.contentmgmt;

import com.redhat.rhn.domain.contentmgmt.ContentEnvironment;
import com.redhat.rhn.domain.contentmgmt.ContentManagementException;
import com.redhat.rhn.manager.contentmgmt.ContentManager;
import com.redhat.rhn.domain.contentmgmt.ContentProject;
import com.redhat.rhn.domain.contentmgmt.ProjectSource;
import com.redhat.rhn.domain.contentmgmt.ProjectSource.Type;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.xmlrpc.BaseHandler;
import com.redhat.rhn.frontend.xmlrpc.InvalidParameterException;

import java.util.List;
import java.util.Map;

import static com.redhat.rhn.common.util.StringUtil.nullIfEmpty;
import static java.util.Optional.ofNullable;

/**
 * Content Management XMLRPC handler
 */
public class ContentManagementHandler extends BaseHandler {

    /**
     * List Content Projects visible to user
     *
     * @param loggedInUser - the logged in user
     * @return the list of Content Projects visible to user
     *
     * @xmlrpc.doc Look up Content Project with given label
     * @xmlrpc.param #session_key()
     * @xmlrpc.returntype
     * #array()
     * $ContentProjectSerializer
     * #array_end()
     */
    public List<ContentProject> listProjects(User loggedInUser) {
        return ContentManager.listProjects(loggedInUser);
    }

    /**
     * Look up Content Project
     *
     * @param loggedInUser - the logged in user
     * @param label - the label
     * @return the Content Project with given label
     *
     * @xmlrpc.doc Look up Content Project with given label
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "label", "Content Project label")
     * @xmlrpc.returntype $ContentProjectSerializer
     */
    public ContentProject lookupProject(User loggedInUser, String label) {
        return ContentManager.lookupProject(label, loggedInUser).orElse(null);
    }

    /**
     * Create Content Project
     *
     * @param loggedInUser - the logged in user
     * @param label - the label
     * @param name - the name
     * @param description - the description
     * @return the created Content Project
     *
     * @xmlrpc.doc Create Content Project
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "label", "Content Project label")
     * @xmlrpc.param #param_desc("string", "name", "Content Project name")
     * @xmlrpc.param #param_desc("string", "description", "Content Project description")
     * @xmlrpc.returntype $ContentProjectSerializer
     */
    public ContentProject createProject(User loggedInUser, String label, String name, String description) {
        ensureOrgAdmin(loggedInUser);
        try {
            return ContentManager.createProject(label, name, description, loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Update Content Project
     *
     * @param loggedInUser - the logged in user
     * @param label - the new label
     * @param props - the map with the Content Project properties
     * @return the updated Content Project
     *
     * @xmlrpc.doc Update Content Project with given label
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "label", "Content Project label")
     * @xmlrpc.param
     *  #struct("data")
     *      #prop_desc("string", "name", "Content Project name")
     *      #prop_desc("string", "description", ""Content Project description")
     *  #struct_end()
     * @xmlrpc.returntype $ContentProjectSerializer
     */
    public ContentProject updateProject(User loggedInUser, String label, Map<String, Object> props) {
        ensureOrgAdmin(loggedInUser);
        try {
            return ContentManager.updateProject(label,
                    ofNullable((String) props.get("name")),
                    ofNullable((String) props.get("description")),
                    loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Remove Content Project
     *
     * @param loggedInUser - the logged in user
     * @param label - the label
     * @return the number of removed objects
     *
     * @xmlrpc.doc Remove Content Project
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "label", "Content Project label")
     * @xmlrpc.returntype int - the number of removed objects
     */
    public int removeProject(User loggedInUser, String label) {
        ensureOrgAdmin(loggedInUser);
        return ContentManager.removeProject(label, loggedInUser);
    }

    /**
     * List Environments in a Content Project with the respect to their ordering
     *
     * @param loggedInUser - the logged in user
     * @param projectLabel - the Content Project label
     * @return the List of Content Environments with respect to their ordering
     *
     * @xmlrpc.doc List Environments in a Content Project with the respect to their ordering
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.returntype
     * #array()
     * $ContentEnvironmentSerializer
     * #array_end()
     */
    public List<ContentEnvironment> listProjectEnvironments(User loggedInUser, String projectLabel) {
        try {
            return ContentManager.listProjectEnvironments(projectLabel, loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Look up Content Environment based on Content Project and Content Environment label
     *
     * @param loggedInUser - the logged in user
     * @param projectLabel - the Content Project label
     * @param envLabel - the Content Environment label
     * @return found Content Environment or null if no such environment exists
     *
     * @xmlrpc.doc Look up Content Environment based on Content Project and Content Environment label
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "envLabel", "Content Environment label")
     * @xmlrpc.returntype $ContentEnvironmentSerializer
     */
    public ContentEnvironment lookupEnvironment(User loggedInUser, String projectLabel, String envLabel) {
        return ContentManager.lookupEnvironment(envLabel, projectLabel, loggedInUser).orElse(null);
    }

    /**
     * Create a Content Environment and appends it behind given Content Environment
     *
     * @param loggedInUser - the logged in user
     * @param projectLabel - the Content Project label
     * @param predecessorLabel - the Predecessor label
     * @param label - the Content Environment Label
     * @param name - the Content Environment name
     * @param description - the Content Environment description
     * @return the created Content Environment
     *
     * @xmlrpc.doc Create a Content Environment and appends it behind given Content Environment
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "predecessorLabel", "Predecessor Environment label")
     * @xmlrpc.param #param_desc("string", "label", "new Content Environment label")
     * @xmlrpc.param #param_desc("string", "name", "new Content Environment name")
     * @xmlrpc.param #param_desc("string", "description", "new Content Environment description")
     * @xmlrpc.returntype $ContentEnvironmentSerializer
     */
    public ContentEnvironment createEnvironment(User loggedInUser, String projectLabel, String predecessorLabel,
            String label, String name, String description) {
        ensureOrgAdmin(loggedInUser);
        try {
            return ContentManager.createEnvironment(projectLabel, ofNullable(nullIfEmpty(predecessorLabel)), label,
                    name, description, loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Update Content Environment
     *
     * @param loggedInUser - the logged in user
     * @param projectLabel - the Content Project label
     * @param envLabel - the Environment label
     * @param props - the map with the Environment properties
     * @return the updated Environment
     *
     * @xmlrpc.doc Update Content Environment with given label
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "envLabel", "Content Environment label")
     * @xmlrpc.param
     *  #struct("data")
     *      #prop_desc("string", "name", "Content Environment name")
     *      #prop_desc("string", "description", "Content Environment description")
     *  #struct_end()
     * @xmlrpc.returntype $ContentEnvironmentSerializer
     */
    public ContentEnvironment updateEnvironment(User loggedInUser, String projectLabel, String envLabel,
            Map<String, Object> props) {
        ensureOrgAdmin(loggedInUser);
        try {
            return ContentManager.updateEnvironment(envLabel,
                    projectLabel,
                    ofNullable((String) props.get("name")),
                    ofNullable((String) props.get("description")),
                    loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Remove a Content Environment
     *
     * @param loggedInUser - the logged in user
     * @param projectLabel - the Content Project label
     * @param envLabel - the Content Environment label
     * @return the number of removed objects
     *
     * @xmlrpc.doc Remove a Content Environment
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "envLabel", "Content Environment label")
     * @xmlrpc.returntype int - the number of removed objects
     */
    public int removeEnvironment(User loggedInUser, String projectLabel, String envLabel) {
        ensureOrgAdmin(loggedInUser);
        return ContentManager.removeEnvironment(envLabel, projectLabel, loggedInUser);
    }

    /**
     * List Content Project Sources
     *
     * @param loggedInUser the logged in user
     * @param projectLabel the Project label
     * @throws InvalidParameterException if the Project is not found
     * @return list of Project Sources
     *
     * @xmlrpc.doc List Content Project Sources
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.returntype
     * #array()
     * $ContentProjectSourceSerializer
     * #array_end()
     */
    public List<ProjectSource> listProjectSources(User loggedInUser, String projectLabel) {
        return ContentManager.lookupProject(projectLabel, loggedInUser)
                .orElseThrow(() -> new InvalidParameterException("Content Project: " + projectLabel +
                        ", does not exist"))
                .getSources();
    }

    /**
     * Look up Content Project Source
     *
     * @param loggedInUser the logged in user
     * @param projectLabel the Project label
     * @param sourceType the Source type (e.g. "software")
     * @param sourceLabel the Source label (e.g. software channel label)
     * @throws InvalidParameterException if the Project is not found
     * @return list of Project Sources
     *
     * @xmlrpc.doc Look up Content Project Source
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "sourceType", "Project Source type, e.g. 'software'")
     * @xmlrpc.param #param_desc("string", "sourceLabel", "Project Source label")
     * @xmlrpc.returntype $ContentProjectSourceSerializer
     */
    public ProjectSource lookupProjectSource(User loggedInUser, String projectLabel, String sourceType,
            String sourceLabel) {
        Type type = Type.lookupByLabel(sourceType);
        try {
            return ContentManager.lookupProjectSource(projectLabel, type, sourceLabel, loggedInUser).orElse(null);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Attach a Source to a Project
     *
     * @param loggedInUser the logged in user
     * @param projectLabel the Project label
     * @param sourceType the Source type (e.g. "software")
     * @param sourceLabel the Source label (e.g. software channel label)
     * @throws InvalidParameterException when Source already exists or when used entities don't exist or are not
     * accessible
     * @return the created Source
     *
     * @xmlrpc.doc Attach a Source to a Project
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "sourceType", "Project Source type, e.g. 'software'")
     * @xmlrpc.param #param_desc("string", "sourceLabel", "Project Source label")
     * @xmlrpc.returntype $ContentProjectSourceSerializer
     */
    public ProjectSource attachSource(User loggedInUser, String projectLabel, String sourceType, String sourceLabel) {
        Type type = Type.lookupByLabel(sourceType);
        try {
            ContentManager.lookupProjectSource(projectLabel, type, sourceLabel, loggedInUser).ifPresent(
                    s -> {
                        throw new InvalidParameterException("Source " + sourceLabel + " of type " + sourceType +
                                " is already attached to Project " + projectLabel);
                    }
            );
            return ContentManager.attachSource(projectLabel, type, sourceLabel, loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    /**
     * Detach a Source from a Project
     *
     * @param loggedInUser the logged in user
     * @param projectLabel the Project label
     * @param sourceType the Source type (e.g. "software")
     * @param sourceLabel the Source label (e.g. software channel label)
     * @return the number of Sources removed
     *
     * @xmlrpc.doc Detach a Source from a Project
     * @xmlrpc.param #session_key()
     * @xmlrpc.param #param_desc("string", "projectLabel", "Content Project label")
     * @xmlrpc.param #param_desc("string", "sourceType", "Project Source type, e.g. 'software'")
     * @xmlrpc.param #param_desc("string", "sourceLabel", "Project Source label")
     * @xmlrpc.returntype int - the number of removed objects
     */
    public int detachSource(User loggedInUser, String projectLabel, String sourceType, String sourceLabel) {
        Type type = Type.lookupByLabel(sourceType);
        try {
            return ContentManager.detachSource(projectLabel, type, sourceLabel, loggedInUser);
        }
        catch (ContentManagementException e) {
            throw new InvalidParameterException(e.getMessage());
        }
    }
}
