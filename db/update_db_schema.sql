CREATE OR REPLACE FUNCTION update_schema() returns void AS $$
DECLARE
   current_db_version varchar;
   latest_db_version varchar := '0.4.2';
BEGIN

if not exists (SELECT * FROM pg_class where relname = 'constants' and relkind = 'r') then
    raise notice 'No constants table found. Can''t perform update. Creating constants table.';
    create table constants (
        db_version text
    );
    insert into constants (db_version) values ('unknown');
else
    LOOP
        select * from constants into current_db_version;
        raise notice 'current_db_version = %', current_db_version;
        IF current_db_version = 'unknown' THEN
            raise notice 'db version unknown. can''t update the schema';
            EXIT;
        ELSEIF current_db_version = latest_db_version THEN
            raise notice 'DB version up to latest version. Schema update complete.';
            EXIT;
        END IF;
        perform execute_update_function(current_db_version);
    END LOOP;
end if;

END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION execute_update_function(old_version varchar) returns void AS $$
DECLARE
    new_db_version varchar;
    function_name varchar;
BEGIN
    function_name = 'update_schema_' || replace(old_version, '.', '');
    execute ('select ' || function_name || '();') into new_db_version;
    raise notice 'Schema updated to v%', new_db_version;
    update constants set db_version = new_db_version;
END;
$$ LANGUAGE plpgsql;

-- Update from v0.4.0 to v0.4.1
CREATE OR REPLACE FUNCTION update_schema_040() returns varchar AS $$
DECLARE
    old_db_version varchar := '0.4.0';
    new_db_version varchar := '0.4.1';
BEGIN
    alter table runs add column model_url text;
    alter table runs add column model_repo_type text;
    alter table runs rename code_version to model_revision;
    update runs set model_url = '';
    update runs set model_repo_type = 'SVN';

    alter table runs alter column model_url set not null;
    alter table runs alter column model_repo_type set not null;
    alter table runs rename command_flags to runner_flags;
    return new_db_version;
END;
$$ LANGUAGE plpgsql;

-- Update from v0.4.0 to v0.4.2
CREATE OR REPLACE FUNCTION update_schema_041() returns varchar AS $$
DECLARE
    old_db_version varchar := '0.4.1';
    new_db_version varchar := '0.4.2';
BEGIN
    update runs set cluster_name = 'default' where cluster_name = '';
    update runs set runset = 'default_runset' where runset = '';
    alter table runs add column host_ip text;
    return new_db_version;
END;
$$ LANGUAGE plpgsql;

begin;
select update_schema();
commit;
