/**
 * Copyright (c) 2015 SUSE LLC
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

package com.suse.manager.webui.controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.redhat.rhn.domain.role.RoleFactory;
import com.redhat.rhn.domain.server.virtualhostmanager.VirtualHostManager;
import com.redhat.rhn.domain.server.virtualhostmanager.VirtualHostManagerFactory;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.taskomatic.TaskomaticApi;
import com.redhat.rhn.taskomatic.TaskomaticApiException;

import com.redhat.rhn.taskomatic.task.gatherer.GathererJob;
import com.suse.manager.gatherer.GathererRunner;
import com.suse.manager.model.gatherer.GathererModule;
import com.suse.manager.webui.utils.FlashScopeHelper;

import com.suse.manager.webui.utils.gson.JsonResult;
import org.apache.commons.lang.StringUtils;

import org.apache.http.HttpStatus;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;

import javax.persistence.NoResultException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.suse.manager.webui.utils.SparkApplicationHelper.json;
import static com.suse.utils.Json.GSON;

/**
 * Controller class providing backend code for the VHM pages.
 */
public class VirtualHostManagerController {

    private VirtualHostManagerController() { }

    private static GathererRunner gathererRunner = new GathererRunner();

    private static VirtualHostManagerFactory factory;

    /**
     * Displays a list of VHMs.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView list(Request request, Response response, User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("pageSize", user.getPageSize());
        data.put("virtualHostManagers", getFactory()
                .listVirtualHostManagers(user.getOrg()));
        data.put("modules", gathererRunner.listModules().keySet());
        data.put("info", FlashScopeHelper.flash(request));
        return new ModelAndView(data, "virtualhostmanager/list.jade");
    }

    /**
     * Processes a GET request to get a list of all vhms
     *
     * @param req the request object
     * @param res the response object
     * @param user the authorized user
     * @return the result JSON object
     */
    public static Object get(Request req, Response res, User user) {
        List<VirtualHostManager> vhms =
                getFactory().listVirtualHostManagers(user.getOrg());
        return json(res, getJsonList(vhms));
    }

    /**
     * Processes a GET request to get a single image store object
     *
     * @param req the request object
     * @param res the response object
     * @param user the authorized user
     * @return the result JSON object
     */
    public static Object getSingle(Request req, Response res, User user) {
        Long storeId;
        try {
            storeId = Long.parseLong(req.params("id"));
        }
        catch (NumberFormatException e) {
            Spark.halt(HttpStatus.SC_NOT_FOUND);
            return null;
        }

        try {
            VirtualHostManager vhm = getFactory().lookupByIdAndOrg(storeId, user.getOrg());
            return json(res, getJsonDetails(vhm));
        }
        catch (NoResultException e) {
            Spark.halt(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    /**
     * Displays a certain VHM.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView show(Request request, Response response, User user) {
        Long id = Long.parseLong(request.params("id"));

        Map<String, Object> data = new HashMap<>();
        data.put("virtualHostManager", getFactory().lookupByIdAndOrg(id,
                user.getOrg()));
        data.put("satAdmin", user.hasRole(RoleFactory.SAT_ADMIN));

        return new ModelAndView(data, "virtualhostmanager/show.jade");
    }

    /**
     * Displays a page to add a VHM.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView add(Request request, Response response, User user) {
        String module = request.queryParams("module");
        GathererModule gathererModule = gathererRunner.listModules().get(module);

        Map<String, Object> data = new HashMap<>();
        data.put("virtualHostManager",
                getFactory().createVirtualHostManager("", user.getOrg(), module,
                        gathererModule.getParameters()));
        return new ModelAndView(data, "virtualhostmanager/add.jade");
    }

    /**
     * Creates a new VHM.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView create(Request request, Response response, User user) {
        List<String> errors = new LinkedList<>();

        String label = request.queryParams("label");
        String gathererModule = request.queryParams("module");
        Map<String, String> gathererModuleParams =
                paramsFromQueryMap(request.queryMap().toMap());

        if (StringUtils.isEmpty(label)) {
            errors.add("Label must be specified.");
        }
        if (getFactory().lookupByLabel(label) != null) {
            errors.add("Virtual Host Manager with given label already exists.");
        }
        if (!getFactory().isConfigurationValid(gathererModule, gathererModuleParams)) {
            errors.add("All fields are mandatory.");
        }

        VirtualHostManager vhm = getFactory().createVirtualHostManager(
                label, user.getOrg(), gathererModule, gathererModuleParams);

        if (errors.isEmpty()) {
            getFactory().save(vhm);
            response.redirect("/rhn/manager/vhms");
            Spark.halt();
            return null;
        }
        else {
            Map<String, Object> data = new HashMap<>();
            data.put("virtualHostManager", vhm);
            data.put("errors", errors);
            return new ModelAndView(data, "virtualhostmanager/add.jade");
        }
    }

    /**
     * Processes a POST request to delete multiple vhms
     *
     * @param req the request object
     * @param res the response object
     * @param user the authorized user
     * @return the result JSON object
     */
    public static Object delete(Request req, Response res, User user) {
        List<Long> ids;
        try {
            ids = Arrays.asList(GSON.fromJson(req.body(), Long[].class));
        }
        catch (JsonParseException e) {
            Spark.halt(HttpStatus.SC_BAD_REQUEST);
            return null;
        }

        List<VirtualHostManager> vhms =
                getFactory().lookupByIdsAndOrg(ids, user.getOrg());
        if (vhms.size() < ids.size()) {
            return json(res, new JsonResult<>(false, "not_found"));
        }

        vhms.forEach(getFactory()::delete);
        return json(res, new JsonResult<>(true, vhms.size()));
    }

    /**
     * Schedule a refresh to a VHM.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return dummy string to satisfy spark
     */
    public static Object refresh(Request request, Response response, User user) {
        Long id = Long.parseLong(request.params("id"));
        VirtualHostManager virtualHostManager = getFactory().lookupByIdAndOrg(id,
                user.getOrg());
        String label = virtualHostManager.getLabel();
        String message = null;
        Map<String, String> params = new HashMap<>();
        params.put(GathererJob.VHM_LABEL, label);
        try {
            new TaskomaticApi()
                    .scheduleSingleSatBunch(user, "gatherer-matcher-bunch", params);
        }
        catch (TaskomaticApiException e) {
            message  = "Problem when running Taskomatic job: " + e.getMessage();
        }
        if (message == null) {
            message = "Refresh for Virtual Host Manager with label '" +
                    label + "' was triggered.";
        }
        FlashScopeHelper.flash(request, message);
        response.redirect("/rhn/manager/vhms");
        return "";
    }

    /**
     * Creates VHM gatherer module params from the query map.
     *
     * @param queryMap the query map
     * @return the map
     */
    private static Map<String, String> paramsFromQueryMap(Map<String, String[]> queryMap) {
        return queryMap.entrySet().stream()
                .filter(keyVal -> keyVal.getKey().startsWith("module_"))
                .collect(Collectors.toMap(
                        keyVal -> keyVal.getKey().replaceFirst("module_", ""),
                        keyVal -> keyVal.getValue()[0]));
    }

    /**
     * Gets the VHM factory.
     *
     * @return the factory
     */
    private static VirtualHostManagerFactory getFactory() {
        if (factory != null) {
            return factory;
        }
        return VirtualHostManagerFactory.getInstance();
    }

    /**
     * For testing only!
     * @param mockFactory - the factory
     */
    public static void setMockFactory(VirtualHostManagerFactory mockFactory) {
        factory = mockFactory;
    }

    /**
     * Changes the gatherer runner instance, used in tests.
     * @param gathererRunnerIn a new gatherer runner
     */
    public static void setGathererRunner(GathererRunner gathererRunnerIn) {
        gathererRunner = gathererRunnerIn;
    }

    /**
     * Creates a list of JSON objects for a list of {@link VirtualHostManager} instances
     *
     * @param vhmList the vhm list
     * @return the list of JSON objects
     */
    private static List<JsonObject> getJsonList(List<VirtualHostManager> vhmList) {
        return vhmList.stream().map(imageStore -> {
            JsonObject json = new JsonObject();
            json.addProperty("id", imageStore.getId());
            json.addProperty("label", imageStore.getLabel());
            json.addProperty("orgName", imageStore.getOrg().getName());
            return json;
        }).collect(Collectors.toList());
    }

    private static JsonObject getJsonDetails(VirtualHostManager vhm) {
        JsonObject json = new JsonObject();
        json.addProperty("id", vhm.getId());
        json.addProperty("label", vhm.getLabel());
        json.addProperty("orgName", vhm.getOrg().getName());
        json.addProperty("gathererModule", vhm.getGathererModule());

        JsonObject config = new JsonObject();
        vhm.getConfigs().forEach(c -> config.addProperty(c.getParameter(), c.getValue()));
        json.add("config", config);
        return json;
    }
}
