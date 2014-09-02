from base import *

DEBUG = False
TEMPLATE_DEBUG = False

# Make this unique, and don't share it with anybody.
SECRET_KEY = get_env_variables("DJANGO_SECRET_KEY")

#   Updated to point at the glass database, not arcs
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql_psycopg2',
        'HOST': 'glass.eres.rmit.edu.au',     # Set to empty string for localhost.
        'NAME': 'tzar',
        'USER': 'tzar',
        'PASSWORD': get_env_variables('DJANGO_DB_PASSWORD'),
        'PORT': '8080'                      # Glass runs on 8080
    }
}

ALLOWED_HOSTS=['*']