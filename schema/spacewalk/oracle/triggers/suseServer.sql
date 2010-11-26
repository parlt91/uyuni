--
-- Copyright (c) 2010 Novell
--
-- This software is licensed to you under the GNU General Public License,
-- version 2 (GPLv2). There is NO WARRANTY for this software, express or
-- implied, including the implied warranties of MERCHANTABILITY or FITNESS
-- FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
-- along with this software; if not, see
-- http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
--
--

create or replace trigger
suseserver_mod_trig
before insert or update on suseServer
for each row
begin
    :new.modified := sysdate;
end;
/
show errors

create or replace trigger
suse_server_del_trig
before delete on suseServer
for each row
begin
    insert into suseDelServer ( guid ) values ( :old.guid );
end;
/
show errors

