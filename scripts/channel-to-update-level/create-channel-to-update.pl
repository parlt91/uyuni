#!/usr/bin/perl

use Frontier::Client;
use Data::Dumper;
use CGI;


# @author Justin Sherrill jsherril@redhat.com
#TODO
#  Add better error handling
#		Add ability to print a list of packages not added due to failed package match
#  Improve rpm search feature (more efficient)
#  Check to see if an rpm already exists in a channel before adding?  (possible speed increase) (possibly include this in improved search)



#START EDITABLE OPTIONS
$SERVER="dually.rdu.redhat.com";
$USERNAME="admin";
$PASSWORD='redhat';
$packGroupSize = 50; # the number of packages to add in one go.  If you see proxy 502 errors, decrease this value.
#END EDITABLE OPTIONS




$DEBUG=0;

$new_channel_label = $ARGV[5];

@VERSIONS = ( "5", "4", "3", "2.1" );
#@VERSIONS = ( "5");
@RELEASES = ( "AS", "ES", "WS", "Server", "Client", "Desktop");
@ARCHES= ( "i386",  "ia64",  "ppc",  "s390",  "s390x",  "x86_64"); 
@UPDATE = ("GOLD", "U1", "U2", "U3", "U4", "U5", "U6", "U7", "U8", "U9" );

@SUB_REPOS = ("VT", "Cluster", "ClusterStorage", "Workstation");

@EXTRAS = ("Extras", "Supplementary");


$SOURCE_DIR = "/mnt/engarchive2/released/";
$DATA_DIR = "./data/";




if( $ARGV[0] eq "--help" || $ARGV[0] eq "help"){

####START USAGE/HELP
print <<EOF;
This script will create a channel on an RHN Satellite according to a list of rpms to a specific update level.  It can also upgrade an existing channel to a higher update level.
EOF
&usage();
print <<EOF;

Arguments:
	VERSION - Currently supported versions:  2.1, 3, 4, and 5
	UPDATE -  update level.  Currently supported GOLD & 1 - 9  
		(Note: all update levels are not applicable to all versions)
	RELEASE - Currently Supports:  AS, ES, WS, Desktop, Server, Client
	ARCH - Architecture.  ( Currently Supports:  i386, ia64, s390, s390x, ppc, and x86_64 )
	CHANNEL_LABEL - The new channel label you would like to create.  Label will be converted to lower case. 
	SUB_REPO (optional) - Sub repo/component, usually a child channel  (Currently supports VT, Cluster, 
		ClusterStorage, Workstation, & Supplementary for RHEl 5 and Extras for RHEL 4 or earlier)

Questions?  Suggestions? Bugs?   ->    jsherril\@redhat.com

EOF
############END HELP

exit(0);
}




# if gather or create isn't the first argument
if( @ARGV < 1 || ($ARGV[0] ne "gather"  &&  $ARGV[0] ne "create")   ){

	&usage();
	exit 0;

}


#gather data files.  This is only usable within Red Hat, not for external use
if ( $ARGV[0] eq "gather"){

	if( ! -d $DATA_DIR ){
		exe( "mkdir $DATA_DIR" );
	}
	
	for( my $i = 0; $i < @VERSIONS; $i++){
		for( my $j = 0; $j < @UPDATE; $j++){
			for( my $k = 0; $k < @RELEASES; $k++){
				for( my $l = 0; $l < @ARCHES; $l++){
				
				        
					$directory  = "RHEL-$VERSIONS[$i]/$UPDATE[$j]/$RELEASES[$k]/$ARCHES[$l]/";
					$full_path = "$SOURCE_DIR/$directory";	

					$directory_RHEL5 = "RHEL-$VERSIONS[$i]-$RELEASES[$k]/$UPDATE[$j]/$ARCHES[$l]/";
					$full_path_RHEL5 = "$SOURCE_DIR/$directory_RHEL5";

					#if it's RHEL 5 do special stuff
					if( $VERSIONS[$i] eq "5" && -d  $full_path_RHEL5){
						print("Creating data file for $directory_RHEL5\n");
						exe( "ls $full_path_RHEL5/os/$RELEASES[$k]/ | grep .rpm > $DATA_DIR$VERSIONS[$i]-$UPDATE[$j]-$RELEASES[$k]-$ARCHES[$l]" );
						for( my $m = 0; $m < @SUB_REPOS; $m++){
							$full_path_repo = "$full_path_RHEL5/os/$SUB_REPOS[$m]";
							if( -d $full_path_repo ) {
								print "Creating Data file for $directory_RHEL5-$SUB_REPOS[$m]\n";
								exe( "ls $full_path_RHEL5/os/$SUB_REPOS[$m]/ | grep .rpm > $DATA_DIR$VERSIONS[$i]-$UPDATE[$j]-$RELEASES[$k]-$ARCHES[$l]-$SUB_REPOS[$m]" );
							}
						}
					}
					elsif( -d $full_path){																		
						print("Creating data file for $directory\n");
						exe( "ls $full_path/tree/RedHat/RPMS/ | grep .rpm > $DATA_DIR$VERSIONS[$i]-$UPDATE[$j]-$RELEASES[$k]-$ARCHES[$l]" );
					}

					#Handle extras and supplementary
					if ( $VERSIONS[$i] eq "5") {
						$SUP = "Supplementary";
	                                        $directory_RHEL5 = "RHEL-$VERSIONS[$i]-$RELEASES[$k]-$SUP/$UPDATE[$j]/$ARCHES[$l]/";
        	                                $full_path_RHEL5 = "$SOURCE_DIR/$directory_RHEL5";
						if (-d $full_path_RHEL5) {
							print("Creating data file for $directory_RHEL5\n");
							exe( "ls $full_path_RHEL5/os/$SUP/ | grep .rpm > $DATA_DIR$VERSIONS[$i]-$UPDATE[$j]-$RELEASES[$k]-$ARCHES[$l]-$SUP" );
						}

					}
					else {
						$EXTRAS = "Extras";
        	                                $directory  = "RHEL-$VERSIONS[$i]-$EXTRAS/$UPDATE[$j]/$RELEASES[$k]/$ARCHES[$l]/";
	                                        $full_path = "$SOURCE_DIR/$directory";
						if (-d $full_path) {
							 print("Creating data file for $directory\n");
							exe( "ls $full_path/tree/RedHat/RPMS/ | grep .rpm > $DATA_DIR$VERSIONS[$i]-$UPDATE[$j]-$RELEASES[$k]-$ARCHES[$l]-$EXTRAS" );
						}
					}
				}
			}
		}
	
	}
	
	exit 0;
}



	

my  $client = new Frontier::Client(url => "http://$SERVER/rpc/api", timeout => 10000);
my  $session = $client->call('auth.login',$USERNAME, $PASSWORD);


if( $ARGV[0] eq "create"  ){

	if( @ARGV > 7 || @ARGV < 6  ){
		&usage();
		exit 0;
	}


 	

	#find data file
	$update_level = "";
	if( $ARGV[2] eq "GOLD" || $ARGV[2] eq "gold" || $ARGV[2] eq "Gold" ){
		$FILENAME = "$DATA_DIR$ARGV[1]-GOLD-$ARGV[3]-$ARGV[4]";
	}
	else {
		$FILENAME = "$DATA_DIR$ARGV[1]-U$ARGV[2]-$ARGV[3]-$ARGV[4]";
	}

	if ($ARGV[6]){
		$FILENAME = $FILENAME."-$ARGV[6]";
	}

	if ( ! -e "$FILENAME" ){
		print "the requested data file ($FILENAME) does not exist, is this a valid version/release/update/arch combination\?\n" ;
		exit 1;
	}

	#create a base channel label from which we will pull packages from
	$base_channel_label= getBaseChannel();
	
	
	#find the channel's id & arch
	my $new_channel_label = $ARGV[5];
	$new_channel_label =~ tr/A-Z/a-z/;  #convert upper case letters to lowercase to conform to specs
	my $base_channel_id = -1;
	my $new_channel_id = -1;
	my $channels = $client->call('channel.listSoftwareChannels',$session);
	foreach my $channel (@$channels) {
		
		if( $channel->{'channel_label'} eq $base_channel_label ){ 			
		        print("Found match for your base channel - $base_channel_label\n");
			$base_channel_id = $channel->{'channel_id'};
			$new_channel_arch = $channel->{'channel_arch'};
 		}
		if( $channel->{'channel_label'} eq $new_channel_label ){ 			
		        print("Found match for your new channel - $new_channel_label, will update this channel\n");
			$new_channel_id =  $channel->{'channel_id'};
 		}

	}

	#if our base channel (to pull packages from) doesn't exist exit
	if( $base_channel_id == -1 ){
		print("Error: could not find appropriate base channel on Satellite  -  $base_channel_label\n");
		exit(1);
	}
	#if our new channel label doesn't already exist create it
	if( $new_channel_id == -1 ){
		print("Creating new channel\n");
		$client->call('channel.software.create', $session,$new_channel_label, $new_channel_label, $new_channel_label, getArchLabel($new_channel_arch,$session) ,'' );
	} 
	

	#get all the packages from the original base channel
	my $all_packages = $client->call('channel.software.listAllPackages', $session, $base_channel_label);

	#@needed_packages = `/bin/rpm -qa --qf "%{NAME}-%{VERSION}-%{RELEASE}-%{ARCH}\n" | sort`;
	@needed_packages = `cat $FILENAME | sort`;

	#get rid of new lines on end of package name
	chomp(@needed_packages);
	$pack_num = 0;
	@ids_to_add;
        
	$| = 10;  #i have this so when i print the #'s  (i.e. 343/2034), it will print every 10 #s)
	print "Finished finding channels and getting package list, now looking for package matches\n";
	foreach $line (@needed_packages) {
		$line = &fixPackageName($line);
		@elements = split /-/, $line;
		$arch = pop @elements;		
		$subver = pop @elements;
		$ver = pop @elements;
		$name = join ("-", @elements);
		chomp($line);
     	 	#print "Looking for $line (".$pack_num++."/".@needed_packages."):\n";
		foreach $package (@$all_packages){

			if ( 1 == &matches( $name, $package->{'package_name'}, $ver, $package->{'package_version'}, $subver, $package->{'package_release'}, $arch, $package->{'package_arch_label'} ) ){
				print $pack_num++."/".@needed_packages."\r";
				push(@ids_to_add,  $package->{'package_id'});
				last;
			}	    	
		}

	
	}
	print $pack_num."/".@needed_packages."\n";
	print "Requested ".@needed_packages.", and matched ".@ids_to_add." from $base_channel_label\n";
	print "Now calling on server.  This may take a few minutes.\n";
        $packGroupSize = 50; # the number of packages to add in one go

	$i = 0;
	for(; $i + $packGroupSize <= @ids_to_add; $i +=  $packGroupSize ) {
		@subarray = @ids_to_add[$i .. $i + $packGroupSize -1];
		call('channel.software.addPackages', $session, $new_channel_label, \@subarray);
		print $i."/".@ids_to_add."\r";
        }
	@subarray = @ids_to_add[$i .. @ids_to_add -1];
	call('channel.software.addPackages', $session, $new_channel_label, \@subarray);
	print @ids_to_add."/".@ids_to_add."\n";
	print "All operations completed succesfully\n";

	#call('channel.software.addPackages', $session, $new_channel_label, \@ids_to_add);	

	
}





sub exe{
  if( !$DEBUG){
        system( $_[0]);
  }
  else{
        print "DEBUG  ".$_[0]."\n";
  }
}


sub call{
  if( !$DEBUG){
	$client->call(@_);
  }
  else{
    print @_;
  }



}


sub getBaseChannel{

	# 2.1 is special.  So we handle it in a special way
	if( $ARGV[1] eq "2.1"){
		if( $ARGV[3] eq "AS"){
			return "redhat-advanced-server-2.1";
		}
		elsif (  $ARGV[3] eq "ES"){
			return "redhat-ent-linux-i386-es-2.1";			
		}
		elsif (  $ARGV[3] eq "WS"){
			return "redhat-ent-linux-i386-ws-2.1";
			
		}
	
	}
	else{
		$lowerBase = lc( $ARGV[3] );
		if( $ARGV[6] ){
			$lowerSubRepo = lc($ARGV[6]);
			if ($ARGV[6] ne  "Extras"){
 				return "rhel-$ARGV[4]-$lowerBase-$lowerSubRepo-$ARGV[1]";
			}
			else {
				return "rhel-$ARGV[4]-$lowerBase-$ARGV[1]-$lowerSubRepo";
			}
		}
		return "rhel-$ARGV[4]-$lowerBase-$ARGV[1]";
	}

}


# converts an achname to an archlabel.  
sub getArchLabel(){

	$old_arch = $_[0];

	
 	my $arches = $client->call('channel.software.listArches', $session);
	foreach my $arch (@$arches) {
		if( $arch->{'arch_name'} eq $old_arch) {
		
			return $arch->{'arch_label'};
		}
	}

}

#checks for package matching. 
sub matches{
  #print "test: $_[0]  $_[1], $_[2]  $_[3] , $_[4]  $_[5], $_[6]  $_[7]\n";
  if( $_[0] ne $_[1] ){
  	return 0;
  }
  if( $_[2] ne $_[3] ){
  	return 0;
  }
  if( $_[4] ne $_[5] ){
  	return 0;
  }
  if( $_[6] ne $_[7] ){
  	return 0;
  }
  return 1;
}

# this converts package names from    NAME-Version-Release.ARCH.rpm to NAME-Version-Release-ARCH
# possibly convert this to a regular expression or use awk?
sub fixPackageName{
	my $name = $_[0];
	my $fixed_name;
	my @array = split( '\.', $name);

	for($i = 0; $i< @array-2; $i++){
		$fixed_name = $fixed_name.$array[$i];
		if( $i == @array -3 ){
			$fixed_name = $fixed_name."-".$array[$i+1];
		}
		else{
			$fixed_name = $fixed_name.".";
		}

	}
	return $fixed_name;

}

sub usage{
		print( "Usage:\n./create-channel-to-update.pl  create VERSION  UPDATE  RELEASE  ARCH  CHANNEL_LABEL [SUB_REPO]\n./create-channel-to-update.pl  --help\n\nFor example:   ./create-channel-to-update.pl  create 4 3 AS i386 clone-4-u3-i386\n         or   ./create-channel-to-update.pl create 5 1 Server i386 clone-5-u1-server-vt VT\n");

}
