from webgui.models import Run, RunParam, RunSet, Library
from django.contrib import admin
from django.contrib.admin.views.main import ChangeList

class RunParamInline(admin.TabularInline):
    model = RunParam
    extra = 0
    fieldsets = [
        (None,               {'fields': ['param_value']}),
    ]


class RunLibraryInline(admin.TabularInline):
    """
    Represents the libraries for this run on the Runs admin screen.
    """
    model = Run.libraries.through
    extra = 0


def link_results(obj):
    # remove the 'mnt/rdv' from the output host TODO make this a config or somehow more robust
    new_path = obj.output_path.replace("/mnt/rdv","")
    return '<a href="http://%s%s/" target="_blank">View results</a>' % (obj.output_host, new_path)
    # TODO - generate an index.html file and point at this instead
link_results.allow_tags=True


class RunAdmin(admin.ModelAdmin):
    save_as = True
    list_display = ('run_id','project_name','scenario_name','runset','state', 'hostname', link_results)
    # list_display = ('run_id','run_name','runset','state', 'hostname', link_results)
    list_filter = ['run_submission_time','state', 'runset']
    search_fields = ['project_name','run_id', 'runset']
    # search_fields = ['run_name','run_id', 'runset']

    date_hierarchy = 'run_submission_time'
    fieldsets = [
        # (None,               {'fields': ['run_name']}),
        (None,               {'fields': ['project_name', 'scenario_name']}),
        ('Date, host and status', {'fields': ['hostname', 'host_ip', 'run_submission_time', 'run_end_time', 'state',
                                              'runset'], 'classes': ['collapse']}),
        ('Technical details', {'fields': ['seed', 'model_revision', 'runner_flags'], 'classes': ['collapse']}),
        ('Output', {'fields': ['output_host', 'output_path'], 'classes': ['collapse']}),
    ]
    inlines = [RunParamInline, RunLibraryInline]
    actions = ['reschedule_runs']

#    def format_submission_time(self, obj):
#        return obj.run_submission_time.strftime('%d %b %Y %H:%M')
#    format_submission_time.short_description = 'Submission Time'
#    format_submission_time.admin_order_field = 'run_submission_time'

    def reschedule_runs(self, request, queryset):
        queryset.update(state='scheduled')
    reschedule_runs.short_description = "Reschedule selected runs"

admin.site.register(Run, RunAdmin)


class RunInline(admin.TabularInline):
    model = Run
    list_display = ('run_id','state')
    extra = 0
    fieldsets = [
#        (None,               {'fields': ['run_id']}),
        ('Details', {'fields': ['state'], 'classes': ['collapse']})
    ]
#    list_filter = ['state']


class RunSetAdmin(admin.ModelAdmin):
    save_as = False
    list_display = ('runset','num_runs', 'seconds_duration', 'failed','scheduled','in_progress','completed','copied', 'copy_failed')
    list_filter = ['submission_time']
    search_fields = ['runset']

    def format_submission_time(self, obj):
        return obj.submission_time.strftime('%d %b %Y %H:%M')
    format_submission_time.short_description = 'Submission Time'
    format_submission_time.admin_order_field = 'submission_time'

    def format_end_time(self, obj):
        return obj.end_time.strftime('%d %b %Y %H:%M')
    format_end_time.short_description = 'End Time'
    format_end_time.admin_order_field = 'end_time'

    def has_add_permission(self, request):
        return False

    # keeping for this for now because without it 'Runsets' disappear from the main admin menu. TODO fix with proper read-only views.
    def has_view_permission(self, request, obj=None):
        return True

    def has_change_permission(self, request, obj=None):
        return True

    def has_delete_permission(self, request, obj=None):
        return False

    def get_actions(self, request):
        actions = super(RunSetAdmin, self).get_actions(request)
        if 'delete_selected' in actions:
            del actions['delete_selected']
        return actions

    def get_changelist(self, request, **kwargs):
        return RunSetChangeList


class RunSetChangeList(ChangeList):
    def url_for_result(self, result):
        pk = getattr(result, self.pk_attname)
        return '/admin/webgui/run/?runset__runset__exact={0}'.format(pk)
        # TODO(river): the above is a hack, but urlresolvers.reverse doesn't seem to support filter; the below line
        # would list all runs, not just those in the current runset. This is a symptom of the fact that runsets are
        # not first class database entities; they are just string fields in the runs table.
        # return urlresolvers.reverse('admin:webgui_run_changelist', args=(pk,))

admin.site.register(RunSet, RunSetAdmin)
admin.site.register(Library)
