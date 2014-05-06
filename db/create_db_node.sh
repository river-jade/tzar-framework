#!/bin/bash

password=$1

if [ -e $password]; then
  echo "Need to pass a password as the first parameter"
  exit -1
fi
# install postgresql
sudo apt-get update && sudo apt-get install postgresql

# download schema
wget https://tzar-framework.googlecode.com/svn/trunk/db/db_schema.sqls

# become postgres user
sudo su postgres

# create the tzar user (without a password)
createuser tzar -S -d -R

# set the password for the tzar user

psql -c "alter user postgres with password '$password';"

# create tzar database
createdb tzar -O tzar

# Edit postgres permissions file to allow local trusted login, remote 
# password login, and connections from internet, and restart db server
sed '/^local.*all.*all.*peer/s/peer/trust/g' -i /etc/postgresql/9.1/main/pg_hba.conf

cat >> /etc/postgresql/9.1/main/pg_hba.conf << EOF

#Allow network connections from anywhere using password auth
host    all             all             0.0.0.0/0               md5
EOF

sed '/^#listen_addresses/a\
  listen_addresses = '"'"*"'" /etc/postgresql/9.1/main/postgresql.conf -i
/etc/init.d/postgresql restart

#Execute db creation script
psql -f ~ubuntu/db_schema.sql tzar -U tzar

# exit su shell
exit
