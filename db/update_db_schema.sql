CREATE OR REPLACE FUNCTION update_schema() returns void AS $$
DECLARE
   current_db_version varchar;
   latest_db_version varchar := '0.4.3';
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

-- Update from v0.4.2 to v0.4.3
CREATE OR REPLACE FUNCTION update_schema_042() returns varchar AS $$
DECLARE
    old_db_version varchar := '0.4.2';
    new_db_version varchar := '0.4.3';
BEGIN
    CREATE TABLE libraries (
        library_id integer NOT NULL,
        repo_type character varying(16) NOT NULL,
        uri text,
        name text
    );

    CREATE SEQUENCE libraries_library_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MINVALUE
        NO MAXVALUE
        CACHE 1;

    ALTER SEQUENCE libraries_library_id_seq OWNED BY libraries.library_id;

    CREATE TABLE run_libraries (
        run_id integer,
        library_id integer
    );

    ALTER TABLE ONLY libraries ALTER COLUMN library_id SET DEFAULT nextval('libraries_library_id_seq'::regclass);
    ALTER TABLE ONLY run_params ALTER COLUMN run_param_id SET DEFAULT nextval('run_params_run_param_id_seq'::regclass);

    ALTER TABLE ONLY libraries ADD CONSTRAINT libraries_pkey PRIMARY KEY (library_id);

    CREATE INDEX fki_run_libraries_run_id_fk ON run_libraries USING btree (library_id);

    ALTER TABLE ONLY run_libraries
        ADD CONSTRAINT run_libraries_library_id_fkey FOREIGN KEY (library_id) REFERENCES libraries(library_id);
    ALTER TABLE ONLY run_libraries
        ADD CONSTRAINT run_libraries_run_id_fkey FOREIGN KEY (run_id) REFERENCES runs(run_id);
    return new_db_version;
END;
$$ LANGUAGE plpgsql;

begin;
select update_schema();
commit;
