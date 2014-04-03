import os
import sys
path = '/home/ubuntu/django_projects'
if path not in sys.path:
    sys.path.append(path)
path2 = '/home/ubuntu/django_projects/Tzar'
if path2 not in sys.path:
    sys.path.append(path2)

os.environ['DJANGO_SETTINGS_MODULE'] = 'Tzar.settings'

import django.core.handlers.wsgi
application = django.core.handlers.wsgi.WSGIHandler()