--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: run_params; Type: TABLE; Schema: public;; Tablespace: 
--

CREATE TABLE run_params (
    run_param_id integer NOT NULL,
    run_id integer,
    param_name text,
    param_value text,
    param_type text,
    data_type text NOT NULL
);


--
-- Name: run_params_run_param_id_seq; Type: SEQUENCE; Schema: public;
--

CREATE SEQUENCE run_params_run_param_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;



--
-- Name: run_params_run_param_id_seq; Type: SEQUENCE OWNED BY; Schema: public;
--

ALTER SEQUENCE run_params_run_param_id_seq OWNED BY run_params.run_param_id;


--
-- Name: runs; Type: TABLE; Schema: public;; Tablespace: 
--

CREATE TABLE runs (
    run_id integer NOT NULL,
    run_name text NOT NULL,
    state text NOT NULL,
    seed integer,
    code_version text NOT NULL,
    command_flags text NOT NULL,
    hostname text,
    output_path text,
    output_host text,
    run_start_time timestamp without time zone,
    run_end_time timestamp without time zone,
    runset text,
    cluster_name text,
    runner_class text
);



--
-- Name: COLUMN runs.code_version; Type: COMMENT; Schema: public;
--

COMMENT ON COLUMN runs.code_version IS 'The subversion revision number of the code used to execute the run.';


--
-- Name: COLUMN runs.run_start_time; Type: COMMENT; Schema: public;
--

COMMENT ON COLUMN runs.run_start_time IS 'Time run began (UTC)';


--
-- Name: COLUMN runs.run_end_time; Type: COMMENT; Schema: public;
--

COMMENT ON COLUMN runs.run_end_time IS 'Time run finished (UTC)';


--
-- Name: runs_run_id_seq; Type: SEQUENCE; Schema: public;
--

CREATE SEQUENCE runs_run_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Name: runs_run_id_seq; Type: SEQUENCE OWNED BY; Schema: public;
--

ALTER SEQUENCE runs_run_id_seq OWNED BY runs.run_id;

--
-- Name: run_param_id; Type: DEFAULT; Schema: public;
--

ALTER TABLE run_params ALTER COLUMN run_param_id SET DEFAULT nextval('run_params_run_param_id_seq'::regclass);


--
-- Name: run_id; Type: DEFAULT; Schema: public;
--

ALTER TABLE runs ALTER COLUMN run_id SET DEFAULT nextval('runs_run_id_seq'::regclass);


--
-- Name: seed; Type: DEFAULT; Schema: public;
--

ALTER TABLE runs ALTER COLUMN seed SET DEFAULT currval('runs_run_id_seq'::regclass);


--
-- Name: PK; Type: CONSTRAINT; Schema: public;; Tablespace: 
--

ALTER TABLE ONLY runs
    ADD CONSTRAINT "PK" PRIMARY KEY (run_id);


--
-- Name: run_params_pkey; Type: CONSTRAINT; Schema: public;; Tablespace: 
--

ALTER TABLE ONLY run_params
    ADD CONSTRAINT run_params_pkey PRIMARY KEY (run_param_id);


--
-- Name: run_params_run_id_key; Type: CONSTRAINT; Schema: public;; Tablespace: 
--

ALTER TABLE ONLY run_params
    ADD CONSTRAINT run_params_run_id_key UNIQUE (run_id, param_name);


--
-- Name: fki_run_id; Type: INDEX; Schema: public;; Tablespace: 
--

CREATE INDEX fki_run_id ON run_params USING btree (run_id);


--
-- Name: run_id; Type: FK CONSTRAINT; Schema: public;
--

ALTER TABLE ONLY run_params
    ADD CONSTRAINT run_id FOREIGN KEY (run_id) REFERENCES runs(run_id) ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

