-- DDL generata dalle entity Java
-- Progetto Gryffisio - Backend Spring Boot 4.1.0

BEGIN;

-- Tabelle base (senza dipendenze)
CREATE TABLE IF NOT EXISTS public.projects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.domains (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.activities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INTEGER
);

CREATE TABLE IF NOT EXISTS public.sessions (
    id SERIAL PRIMARY KEY,
    session VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.doctors (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.subject_types (
    id SERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS public.operators (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    password_hash VARCHAR(120)
);

-- Tabelle che dipendono da quelle base
CREATE TABLE IF NOT EXISTS public.subjects (
    id BIGSERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL,
    code VARCHAR(50) NOT NULL,
    subject_type_id INTEGER NOT NULL,
    UNIQUE(project_id, code)
);

CREATE TABLE IF NOT EXISTS public.registrations (
    id BIGSERIAL PRIMARY KEY,
    project_id INTEGER NOT NULL,
    domain_id INTEGER NOT NULL,
    session_id INTEGER NOT NULL,
    doctor_id INTEGER,
    activity_date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.modification_requests (
    id BIGSERIAL PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    operator_id INTEGER NOT NULL,
    new_activity_date DATE,
    new_duration_minutes INTEGER,
    new_session_id INTEGER,
    new_doctor_id INTEGER,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_by_admin_id INTEGER,
    handled_at TIMESTAMP,
    rejection_reason TEXT
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabelle di join (M2M)
CREATE TABLE IF NOT EXISTS public.domain_activities (
    domain_id INTEGER NOT NULL,
    activity_id INTEGER NOT NULL,
    PRIMARY KEY(domain_id, activity_id)
);

CREATE TABLE IF NOT EXISTS public.registration_operators (
    registration_id BIGINT NOT NULL,
    operator_id INTEGER NOT NULL,
    PRIMARY KEY(registration_id, operator_id)
);

CREATE TABLE IF NOT EXISTS public.registration_subjects (
    registration_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY(registration_id, subject_id)
);

CREATE TABLE IF NOT EXISTS public.registration_activities (
    registration_id BIGINT NOT NULL,
    activity_id INTEGER NOT NULL,
    PRIMARY KEY(registration_id, activity_id)
);

CREATE TABLE IF NOT EXISTS public.audit_log_operators (
    audit_log_id BIGINT NOT NULL,
    operator_id INTEGER NOT NULL,
    PRIMARY KEY(audit_log_id, operator_id)
);

-- Foreign keys
ALTER TABLE IF EXISTS public.activities
    ADD CONSTRAINT activities_parent_id_fkey FOREIGN KEY (parent_id)
        REFERENCES public.activities (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.subjects
    ADD CONSTRAINT subjects_project_id_fkey FOREIGN KEY (project_id)
        REFERENCES public.projects (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.subjects
    ADD CONSTRAINT subjects_subject_type_id_fkey FOREIGN KEY (subject_type_id)
        REFERENCES public.subject_types (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.registrations
    ADD CONSTRAINT registrations_project_id_fkey FOREIGN KEY (project_id)
        REFERENCES public.projects (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.registrations
    ADD CONSTRAINT registrations_domain_id_fkey FOREIGN KEY (domain_id)
        REFERENCES public.domains (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.registrations
    ADD CONSTRAINT registrations_session_id_fkey FOREIGN KEY (session_id)
        REFERENCES public.sessions (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.registrations
    ADD CONSTRAINT registrations_doctor_id_fkey FOREIGN KEY (doctor_id)
        REFERENCES public.doctors (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.modification_requests
    ADD CONSTRAINT modification_requests_registration_id_fkey FOREIGN KEY (registration_id)
        REFERENCES public.registrations (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.modification_requests
    ADD CONSTRAINT modification_requests_operator_id_fkey FOREIGN KEY (operator_id)
        REFERENCES public.operators (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.modification_requests
    ADD CONSTRAINT modification_requests_new_session_id_fkey FOREIGN KEY (new_session_id)
        REFERENCES public.sessions (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.modification_requests
    ADD CONSTRAINT modification_requests_new_doctor_id_fkey FOREIGN KEY (new_doctor_id)
        REFERENCES public.doctors (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.modification_requests
    ADD CONSTRAINT modification_requests_handled_by_admin_id_fkey FOREIGN KEY (handled_by_admin_id)
        REFERENCES public.operators (id)
        ON DELETE NO ACTION;

ALTER TABLE IF EXISTS public.domain_activities
    ADD CONSTRAINT domain_activities_domain_id_fkey FOREIGN KEY (domain_id)
        REFERENCES public.domains (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.domain_activities
    ADD CONSTRAINT domain_activities_activity_id_fkey FOREIGN KEY (activity_id)
        REFERENCES public.activities (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_operators
    ADD CONSTRAINT registration_operators_registration_id_fkey FOREIGN KEY (registration_id)
        REFERENCES public.registrations (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_operators
    ADD CONSTRAINT registration_operators_operator_id_fkey FOREIGN KEY (operator_id)
        REFERENCES public.operators (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_subjects
    ADD CONSTRAINT registration_subjects_registration_id_fkey FOREIGN KEY (registration_id)
        REFERENCES public.registrations (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_subjects
    ADD CONSTRAINT registration_subjects_subject_id_fkey FOREIGN KEY (subject_id)
        REFERENCES public.subjects (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_activities
    ADD CONSTRAINT registration_activities_registration_id_fkey FOREIGN KEY (registration_id)
        REFERENCES public.registrations (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.registration_activities
    ADD CONSTRAINT registration_activities_activity_id_fkey FOREIGN KEY (activity_id)
        REFERENCES public.activities (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.audit_log_operators
    ADD CONSTRAINT audit_log_operators_audit_log_id_fkey FOREIGN KEY (audit_log_id)
        REFERENCES public.audit_logs (id)
        ON DELETE CASCADE;

ALTER TABLE IF EXISTS public.audit_log_operators
    ADD CONSTRAINT audit_log_operators_operator_id_fkey FOREIGN KEY (operator_id)
        REFERENCES public.operators (id)
        ON DELETE CASCADE;

END;