from base import *

DEBUG = False
TEMPLATE_DEBUG = False

# Make this unique, and don't share it with anybody.
SECRET_KEY = get_env_variables("DJANGO_SECRET_KEY")


