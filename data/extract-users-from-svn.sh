#!/bin/sh
#
# NAME: extract-users-from-svn.sh
#
# PURPOSE: to extract all the user names from a given subversion repository
#

URL=$1

if [ -z "$URL" ]; then

echo "USAGE: extract-users-from-svn.sh <svn url>";
exit 1;

fi;

svn log "$URL" | grep '^r[0-9]' | awk '{print $3}' | sort | uniq 

# EOF
