# This is an auto-generated Django model module.
# You'll have to do the following manually to clean this up:
#     * Rearrange models' order
#     * Make sure each model has one field with primary_key=True
# Feel free to rename the models, but don't rename db_table values or field names.
#
# Also note: You'll have to insert the output of 'django-admin.py sqlcustom [appname]'
# into your database.

from django.db import models

# this manager is not yet in use
class FailedRunSetsManager(models.Manager):
    def get_query_set(self):
        return super(FailedRunSetsManager, self).get_query_set().filter(failed != 0)


class RunSet(models.Model):

    def __unicode__(self):
        return self.runset

    runsets = models.Manager()
    # failed_runsets = FailedRunSetsManager()

    runset = models.CharField(max_length=50, primary_key=True, editable=False)    
    num_runs = models.IntegerField(editable=False)
    submission_time = models.DateTimeField(editable=False)
    end_time  = models.DateTimeField(editable=False)
    seconds_duration = models.IntegerField(editable=False)
    # when this was an interval it caused type problems - django doesn't yet support the postgres Interval
    failed = models.IntegerField(editable=False)
    scheduled = models.IntegerField(editable=False)
    in_progress = models.IntegerField(editable=False)
    completed = models.IntegerField(editable=False)
    copied = models.IntegerField(editable=False)
    copy_failed = models.IntegerField(editable=False)

    class Meta:
        db_table = u'lucy_runset_view'
        managed = False


class Library(models.Model):
    def __unicode__(self):
        return ' | '.join((self.name, self.repo_type, self.uri, self.revision))

    library_id = models.AutoField(primary_key=True)
    repo_type = models.CharField(max_length=16)
    uri = models.TextField()
    name = models.TextField()
    revision = models.CharField(max_length=16)
    force_download = models.BooleanField()

    class Meta:
        db_table = u'libraries'


class Run(models.Model):

    def __unicode__(self):
        return str(self.run_id)
	 # return self.project_name+" "+self.scenario_name

    run_id = models.AutoField(primary_key=True)
    # run_name = models.CharField(max_length=50)
    state = models.CharField(max_length=70)
    seed = models.IntegerField()
    model_revision = models.CharField(max_length=30)
    runner_flags = models.CharField(max_length=200)
    hostname = models.CharField(max_length=50)
    host_ip = models.CharField(max_length=20)
    output_path = models.CharField(max_length=200, blank=True)
    output_host = models.CharField(max_length=50, blank=True)
    run_start_time = models.DateTimeField()
    run_end_time = models.DateTimeField()
    # runset = models.CharField(max_length=80)
    runset = models.ForeignKey(RunSet, db_column='runset')
    cluster_name = models.CharField(max_length=50)
    runner_class = models.CharField(max_length=30)
    run_submission_time = models.DateTimeField()
    project_name = models.CharField(max_length=50)
    scenario_name = models.CharField(max_length=50)
    libraries = models.ManyToManyField(Library, through='RunLibrary')
    
    class Meta:
        db_table = u'runs'


class RunParam(models.Model):

    def __unicode__(self):
        return self.param_name

    run_param_id = models.AutoField(primary_key=True)
    run = models.ForeignKey(Run)
    param_name = models.TextField()
    param_value = models.CharField(max_length=200)
    param_type = models.CharField(max_length=70)
    data_type = models.CharField(max_length=70)
    class Meta:
        db_table = u'run_params'


class RunLibrary(models.Model):
    run = models.ForeignKey(Run)
    library = models.ForeignKey(Library)

    class Meta:
        db_table = u'run_libraries'
        unique_together = ('run', 'library')
