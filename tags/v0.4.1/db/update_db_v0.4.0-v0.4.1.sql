begin;

alter table runs add column model_url text;
alter table runs add column model_repo_type text;
alter table runs rename code_version to model_revision;
update runs set model_url = '';
update runs set model_repo_type = 'SVN';

alter table runs alter column model_url set not null;
alter table runs alter column model_repo_type set not null;
alter table runs rename command_flags to runner_flags;

commit;