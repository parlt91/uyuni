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
package com.suse.manager.webui.utils.gson;

import com.redhat.rhn.domain.config.ConfigChannel;
import com.redhat.rhn.domain.recurringactions.state.InternalState;
import com.redhat.rhn.domain.recurringactions.state.RecurringConfigChannel;
import com.redhat.rhn.domain.recurringactions.state.RecurringInternalState;
import com.redhat.rhn.domain.recurringactions.state.RecurringStateConfig;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON representation of a state configuration.
 */
public class StateConfigJson {

    private Long id;
    private String name;
    private String label;
    private String type;
    private Integer position;
    private boolean assigned;

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param idIn the id
     */
    public void setId(Long idIn) {
        this.id = idIn;
    }

    /**
     * Instantiates a new unassigned state object from config channel
     *
     * @param channelIn the channel
     */
    public StateConfigJson(ConfigChannel channelIn) {
        this.id = channelIn.getId();
        this.name = channelIn.getName();
        this.label = channelIn.getLabel();
        this.type = channelIn.getConfigChannelType().getLabel();
        this.position = null;
        this.assigned = false;
    }

    /**
     * Instantiates a new unassigned state object from internal state
     *
     * @param stateIn the internal state
     */
    public StateConfigJson(InternalState stateIn) {
        this.id = stateIn.getId();
        this.name = stateIn.getName();
        this.label = stateIn.getLabel();
        this.type = "internal_state";
        this.position = null;
        this.assigned = false;
    }

    /**
     * Instantiates a new state object assigned in a specific position from a config channel
     *
     * @param channelIn the channel
     * @param positionIn the ordering of the channel
     */
    public StateConfigJson(ConfigChannel channelIn, int positionIn) {
        this(channelIn);
        this.position = positionIn;
        this.assigned = true;
    }

    /**
     * Instantiates a new state object assigned in a specific position from an internal state
     *
     * @param stateIn the internal state
     * @param positionIn the ordering of the channel
     */
    public StateConfigJson(InternalState stateIn, int positionIn) {
        this(stateIn);
        this.position = positionIn;
        this.assigned = true;
    }

    /**
     * @return the name of the state config
     */
    public String getName() {
        return name;
    }

    /**
     * @param nameIn the name of the state config
     */
    public void setName(String nameIn) {
        this.name = nameIn;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param labelIn the label
     */
    public void setLabel(String labelIn) {
        this.label = labelIn;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param typeIn the type
     */
    public void setType(String typeIn) {
        this.type = typeIn;
    }

    /**
     * @return the position
     */
    public Integer getPosition() {
        return position;
    }

    /**
     * @param positionIn the position
     */
    public void setPosition(Integer positionIn) {
        this.position = positionIn;
    }

    /**
     * @return true if the state is assigned
     */
    public boolean isAssigned() {
        return assigned;
    }

    /**
     * @param assignedIn true if state is assigned
     */
    public void setAssigned(boolean assignedIn) {
        this.assigned = assignedIn;
    }

    /**
     * Creates a list of {@link StateConfigJson} objects from a set of {@link RecurringStateConfig}
     * @param configIn set of states to be included in the list
     * @return the list of {@link StateConfigJson} objects
     */
    public static List<StateConfigJson> listOrderedStates(Set<RecurringStateConfig> configIn) {
        return configIn.stream().map(config -> {
            if (config instanceof RecurringInternalState) {
                return new StateConfigJson(
                        ((RecurringInternalState) config).getInternalState(), config.getPosition().intValue());
            }
            else {
                return new StateConfigJson(
                        ((RecurringConfigChannel) config).getConfigChannel(), config.getPosition().intValue());
            }
        }).collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StateConfigJson)) {
            return false;
        }
        StateConfigJson castOther = (StateConfigJson) other;
        return new EqualsBuilder()
                .append(name, castOther.name)
                .append(label, castOther.label)
                .append(type, castOther.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(name)
                .append(label)
                .append(type).toHashCode();
    }
}
