--
-- Copyright (c) 2023 SUSE LLC
--
-- This software is licensed to you under the GNU General Public License,
-- version 2 (GPLv2). There is NO WARRANTY for this software, express or
-- implied, including the implied warranties of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
-- along with this software; if not, see
-- http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
--
-- Red Hat trademarks are not licensed under GPLv2. No permission is
-- granted to use or replicate Red Hat trademarks that are incorporated
-- in this software or its documentation.
--

CREATE TABLE rhnActionContentManagement (
    id                  NUMERIC NOT NULL
                            CONSTRAINT rhn_action_cntmgmt_id_pk PRIMARY KEY,
    action_id           NUMERIC NOT NULL
                            CONSTRAINT rhn_action_cntmgmt_aid_fk
                            REFERENCES rhnAction (id)
                            ON DELETE CASCADE,
    project_id          NUMERIC
                            CONSTRAINT rhn_action_cntmgmt_project_fk
                            REFERENCES suseContentProject (id)
                            ON DELETE CASCADE,
    env_id              NUMERIC
                            CONSTRAINT rhn_action_cntmgmt_env_fk
                            REFERENCES suseContentEnvironment (id)
                            ON DELETE CASCADE,
    created             TIMESTAMPTZ
                            DEFAULT (CURRENT_TIMESTAMP) NOT NULL,
    modified            TIMESTAMPTZ
                            DEFAULT (CURRENT_TIMESTAMP) NOT NULL
);

CREATE UNIQUE INDEX rhn_act_content_management_aid_idx
    ON rhnActionContentManagement (action_id);

CREATE SEQUENCE RHN_ACT_CONTENT_MANAGEMENT_ID_SEQ;
