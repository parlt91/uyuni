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

import static spark.Spark.halt;

import com.redhat.rhn.common.security.CSRFTokenValidator;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.taskomatic.TaskomaticApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.suse.manager.matcher.MatcherJsonIO;
import com.suse.manager.matcher.MatcherRunner;
import com.suse.manager.webui.services.subscriptionmatching.SubscriptionMatchProcessor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spark.ModelAndView;
import spark.Request;
import spark.Response;

/**
 * Controller class providing backend code for subscription-matcher pages.
 */
public class SubscriptionMatchingController {

    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(Date.class, new ECMAScriptDateAdapter())
        .serializeNulls()
        .create();

    private SubscriptionMatchingController() { }

    /**
     * Displays the subscription-matcher report page
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView show(Request request, Response response, User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("csrf_token", CSRFTokenValidator.getToken(request.session().raw()));
        return new ModelAndView(data, "subscription_matching/show.jade");
    }

    /**
     * Returns JSON data from subscription-matcher
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return JSON result of the API call
     */
    public static String data(Request request, Response response, User user) {
        MatcherJsonIO matcherJsonIO = new MatcherJsonIO();
        Object data = new SubscriptionMatchProcessor().getData(
                matcherJsonIO.getMatcherInput(),
                matcherJsonIO.getMatcherOutput());
        response.type("application/json");
        return GSON.toJson(data);
    }

    /**
     * Invokes download of a csv from the filename given in the request.
     *
     * @param request the request
     * @param response the response
     * @param user the user
     * @return the contents of the given csv file
     */
    public static String csv(Request request, Response response, User user) {
        String filename = request.params("filename");
        List<String> validFilenames = Arrays.asList("message_report.csv",
                "subscription_report.csv",
                "unmatched_system_report.csv");

        if (StringUtils.isBlank(filename) || !validFilenames.contains(filename)) {
            throw new IllegalArgumentException("Tried to download csv with illegal" +
                    " filename: " + filename);
        }

        try {
            response.raw().setContentType("application/csv");
            return FileUtils.readFileToString(new File(MatcherRunner.OUT_DIRECTORY, filename));
        } catch (IOException e) {
            halt(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    /**
     * Schedule run of gatherer-matcher-bunch.
     * @param request the request
     * @param response the response
     * @param user the user
     * @return null
     */
    public static String scheduleMatcherRun(Request request, Response response, User user) {
        new TaskomaticApi().scheduleSingleSatBunch(user, "gatherer-matcher-bunch",
                new HashMap<>());
        return "";
    }
}
