# coding: utf-8
"""
Kickstart API calls unit tests.

NOTE: This module is quite rarely used within Uyuni/SLE,
      only mostly for cloning, manual editing of the cobbler profiles
      and then deleting them.
"""
import os
from unittest.mock import MagicMock, patch
from helpers import shell, assert_expect, assert_list_args_expect, assert_args_expect
import spacecmd.kickstart


class TestSCKickStart:
    """
    Test kickstart.
    """
    def test_kickstart_clone_interactive_no_profiles(self, shell):
        """
        Test do_kickstart_clone interactive. No profiles found.

        :param shell:
        :return:
        """
        mprint = MagicMock()
        logger = MagicMock()
        with patch("spacecmd.kickstart.print", mprint) as prt, \
                patch("spacecmd.kickstart.logging", logger) as lgr:
            spacecmd.kickstart.do_kickstart_clone(shell, "")

        assert not mprint.called
        assert not shell.client.kickstart.cloneProfile.called
        assert logger.error.called
        assert_expect(logger.error.call_args_list,
                      "No kickstart profiles available")

    def test_kickstart_clone_interactive_wrong_profile_entered(self, shell):
        """
        Test do_kickstart_clone interactive. Wrong profile has been entered.

        :param shell:
        :return:
        """
        mprint = MagicMock()
        logger = MagicMock()
        prompter = MagicMock(side_effect=[
            "posix_compliance_problem", "POSIX"])
        shell.do_kickstart_list = MagicMock(return_value=[
            "default_kickstart_profile", "some_other_profile"])
        with patch("spacecmd.kickstart.print", mprint) as prt, \
                patch("spacecmd.kickstart.logging", logger) as lgr, \
                patch("spacecmd.kickstart.prompt_user", prompter) as pmt:
            spacecmd.kickstart.do_kickstart_clone(shell, "")

        assert not shell.client.kickstart.cloneProfile.called
        assert mprint.called
        assert prompter.called
        assert logger.error.called

        assert_expect(logger.error.call_args_list,
                      "Kickstart profile you've entered was not found")
        assert_list_args_expect(mprint.call_args_list,
                                ['', 'Kickstart Profiles', '------------------',
                                 'default_kickstart_profile\nsome_other_profile', ''])
        assert_args_expect(prompter.call_args_list,
                           [(('Original Profile:',), {"noblank": True}),
                            (('Cloned Profile:',), {"noblank": True})])

    def test_kickstart_clone_arg_wrong_profile_entered(self, shell):
        """
        Test do_kickstart_clone with args. Wrong profile has been entered.

        :param shell:
        :return:
        """
        mprint = MagicMock()
        logger = MagicMock()
        prompter = MagicMock()
        shell.do_kickstart_list = MagicMock(return_value=[
            "default_kickstart_profile", "some_other_profile"])
        with patch("spacecmd.kickstart.print", mprint) as prt, \
                patch("spacecmd.kickstart.logging", logger) as lgr, \
                patch("spacecmd.kickstart.prompt_user", prompter) as pmt:
            spacecmd.kickstart.do_kickstart_clone(shell, "-n posix_compliance_problem -c POSIX")

        assert not prompter.called
        assert not shell.client.kickstart.cloneProfile.called
        assert not mprint.called
        assert logger.error.called

        assert_expect(logger.error.call_args_list,
                      "Kickstart profile you've entered was not found")
