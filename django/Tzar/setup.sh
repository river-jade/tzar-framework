#!/bin/bash

# run as root.
: ${DB_PASSWORD:?Must set DB_PASSWORD}
: ${SECRET_KEY:? Must set SECRET_KEY}

# install packages
apt-get update -y && apt-get install subversion python-virtualenv supervisor nginx -y
apt-get -y build-dep python-psycopg2

# create directories
mkdir /webapps/tzar -p
cd /webapps/tzar

# create group and user
sudo groupadd --system webapps
sudo useradd --system --gid webapps --shell /bin/bash --home /webapps/tzar tzar
chown tzar:webapps /webapps/tzar/ -R

su - tzar << EOF
# get code
svn checkout http://tzar-framework.googlecode.com/svn/trunk/django/Tzar/tzar_admin
virtualenv .
source bin/activate
# install python requirements
pip install -r tzar_admin/requirements.txt

mkdir -p /webapps/tzar/logs/
touch /webapps/tzar/logs/gunicorn_supervisor.log

cd /webapps/tzar/tzar_admin
# collect static
DJANGO_SETTINGS_MODULE=webgui.settings.production DJANGO_SECRET_KEY='$SECRET_KEY' DJANGO_DB_PASSWORD='$DB_PASSWORD' python manage.py collectstatic --noinput
EOF

# configure supervisor script
cat << EOF > /etc/supervisor/conf.d/tzar_admin.conf
[program:webgui]
command = /webapps/tzar/tzar_admin/bin/gunicorn_start.sh              ; Command to start app
user = tzar                                                           ; User to run as
stdout_logfile = /webapps/tzar/logs/gunicorn_supervisor.log           ; Where to write log messages
redirect_stderr = true                                                ; Save stderr in the same log
; Set UTF-8 as default encoding
environment=LANG=en_US.UTF-8,LC_ALL=en_US.UTF-8,DJANGO_DB_PASSWORD='${DB_PASSWORD//%/%%}',DJANGO_SECRET_KEY='${SECRET_KEY//%/%%}'
EOF

# reread supervisor script and start
supervisorctl reread
supervisorctl update

cp /webapps/tzar/tzar_admin/tzar_admin.nginxconf /etc/nginx/sites-available/tzar_admin
ln -s /etc/nginx/sites-available/tzar_admin /etc/nginx/sites-enabled/tzar_admin
rm /etc/nginx/sites-enabled/default

# start nginx
service nginx start
