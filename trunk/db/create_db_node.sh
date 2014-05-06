#!/bin/bash
set -e # -e means exit on any error
set -u # -u means treat unset variables as errors
# set -o pipefail # handles corner case where pipe destination command fails

password=${1:?Need to pass the password for the tzar db user as the first parameter}

# install postgresql
sudo apt-get update && yes | sudo apt-get install postgresql

# download schema
wget https://tzar-framework.googlecode.com/svn/trunk/db/db_schema.sql

sudo su postgres <<EOF

# create the tzar user (without a password)
createuser tzar -S -d -R

# set the password for the tzar user

psql -c "alter user tzar with password '$password';"

# create tzar database
createdb tzar -O tzar

# Edit postgres permissions file to allow local trusted login, remote 
# password login, and connections from internet, and restart db server
sed '/^local.*all.*all.*peer/s/peer/trust/g' -i /etc/postgresql/9.1/main/pg_hba.conf

cat >> /etc/postgresql/9.1/main/pg_hba.conf << EOF_INNER

#Allow network connections from anywhere using password auth
host    all             all             0.0.0.0/0               md5
EOF_INNER

sed '/^#listen_addresses/a\
  listen_addresses = '"'"*"'" /etc/postgresql/9.1/main/postgresql.conf -i
/etc/init.d/postgresql restart

#Execute db creation script
psql -f ~ubuntu/db_schema.sql tzar -U tzar

EOF
