%define sysv_dir       %{_sysconfdir}/rc.d/np.d
%define hb_res_dir     %{_sysconfdir}/ha.d/resource.d
%define registry_dir   %sysv_dir/registry
%define apache_registry_dir   %sysv_dir/apachereg
%define installed_dir  %sysv_dir/installed
Name:         SatConfig-general
Version:      1.216.0
Release:      1%{?dist}
Summary:      Satellite Configuration System - general setup, used by many packages
URL:          https://fedorahosted.org/spacewalk
Source0:      https://fedorahosted.org/releases/s/p/spacewalk/%{name}-%{version}.tar.gz
Group:        Development/Libraries
License:      GPLv2
BuildArch:    noarch
Buildroot:    %{_tmppath}/%{name}-%{version}-%{release}-root-%(%{__id_u} -n)
Requires:     perl(:MODULE_COMPAT_%(eval "`%{__perl} -V:version`"; echo $version))
Requires:       nocpulse-common

%description
SatConfig-general sets up directories and other items shared by many packages 
to make a monitoring work.

%prep
%setup -q

%build
#nothing to do here

%install
rm -rf $RPM_BUILD_ROOT

mkdir -p $RPM_BUILD_ROOT%sysv_dir
mkdir -p $RPM_BUILD_ROOT%hb_res_dir
mkdir -p $RPM_BUILD_ROOT%registry_dir
mkdir -p $RPM_BUILD_ROOT%apache_registry_dir
mkdir -p $RPM_BUILD_ROOT%installed_dir
install -m 644 *.pm $RPM_BUILD_ROOT%sysv_dir
install -m 755 hbResource $RPM_BUILD_ROOT%sysv_dir
install -m 755 installSysVSteps $RPM_BUILD_ROOT%sysv_dir
install -m 755 registerStep $RPM_BUILD_ROOT%sysv_dir
install -m 755 step $RPM_BUILD_ROOT%sysv_dir
install -m 755 sysvStep $RPM_BUILD_ROOT%sysv_dir
install -m 755 validateConfiguration $RPM_BUILD_ROOT%sysv_dir
install -m 755 pip $RPM_BUILD_ROOT%sysv_dir
install -m 444 SysV.ini $RPM_BUILD_ROOT%sysv_dir
ln -s ../../rc.d/np.d/hbResource $RPM_BUILD_ROOT%hb_res_dir/ClusterLeader

%files
%defattr(-,root,root,-)
%dir %sysv_dir
%dir %registry_dir
%dir %apache_registry_dir
%dir %installed_dir
%sysv_dir/*.pm
%sysv_dir/hbResource
%sysv_dir/installSysVSteps
%sysv_dir/registerStep
%sysv_dir/step
%sysv_dir/sysvStep
%sysv_dir/validateConfiguration
%sysv_dir/pip
%sysv_dir/SysV.ini
%hb_res_dir/*
%doc 1-STARTUP_SEQUENCE 2-COMMANDS_OVERVIEW 3-CONFIGURATION 4-DEVELOPMENT 5-STEPS_LEGEND

%clean
rm -rf $RPM_BUILD_ROOT

%changelog
* Fri Jan 16 2009 Miroslav Suchý <msuchy@redhat.com>
- fix path to generate_config.log

* Sat Jan 10 2009 Milan Zazrivec 1.215.47-1
- move content from under /usr/share/nocpulse to /var/www

* Wed Jan  7 2009 Milan Zazrivec 1.215.46-1
- bz #474591 - move web data to /usr/share/nocpulse

* Mon Dec  1 2008 Miroslav Suchý <msuchy@redhat.com> 1.215.44-1
- 472910 - fix paths to nofitication configs

* Wed Oct 29 2008 Miroslav Suchý <msuchy@redhat.com> 1.215.43-1
- 468537 - renaming paths with /opt in SysV.ini 

* Tue Oct 21 2008 Miroslav Suchý <msuchy@redhat.com> 1.215.42-1
- 467868 - load propper module during install

* Mon Oct 20 2008 Miroslav Suchý <msuchy@redhat.com> 1.215.41-1
- 467441 - fix namespace

* Wed Sep 24 2008 Miroslav Suchý <msuchy@redhat.com> 1.215.40-1
- spec cleanup for Fedora

