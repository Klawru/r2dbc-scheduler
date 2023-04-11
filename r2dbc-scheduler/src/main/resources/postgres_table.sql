CREATE TABLE IF NOT EXISTS scheduled_job
(
    task_name            varchar(100),
    task_instance        varchar(100),
    task_data            jsonb,
    execution_time       TIMESTAMP WITH TIME ZONE,
    picked               bool,
    picked_by            varchar(50),
    last_success         TIMESTAMP WITH TIME ZONE,
    last_failure         TIMESTAMP WITH TIME ZONE,
    consecutive_failures INT,
    last_heartbeat       TIMESTAMP WITH TIME ZONE,
    version              BIGINT,
    PRIMARY KEY (task_name, task_instance)
);
