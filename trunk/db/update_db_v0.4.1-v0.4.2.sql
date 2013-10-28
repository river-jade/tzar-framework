begin;

update runs set cluster_name = 'default' where cluster_name = '';
update runs set runset = 'default_runset' where runset = '';

commit;