# r2dbc-scheduler
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=klawru_r2dbc-scheduler&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=klawru_r2dbc-scheduler)
![pipeline](https://gitlab.com/klawru/r2dbc-scheduler/badges/main/pipeline.svg)
![coverage](https://gitlab.com/klawru/r2dbc-scheduler/badges/main/coverage.svg)

This is a reactive task scheduler inspired by the [db-scheduler](https://github.com/kagkarlsson/db-scheduler) project.
Tasks are stored in persistent storage using R2DBC and Spring DatabaseClient.

## Installing

### Maven configuration

Artifacts can be found in [Maven Central](https://search.maven.org/search?q=r2dbc-scheduler-spring-boot-starter):
```xml

<dependency>
    <groupId>io.gitlab.klawru</groupId>
    <artifactId>r2dbc-scheduler-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

### Usage with Spring

1) Add maven dependency
```xml

<dependency>
    <groupId>io.gitlab.klawru</groupId>
    <artifactId>r2dbc-scheduler-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

2) create table ``scheduled_job``. See table definition
   for [postgresql](r2dbc-scheduler/src/main/resources/postgres_table.sql)

3) Create bean with task

```java

@Configuration
public class TaskConfig {

    @Bean
    RecurringTask<Void> updateObjects(Service service, OneTimeTask<UUID> updateObject) {
        return Tasks.recurring("updateObjects",
                        new CronScheduler("0 0 1 * * *", ZoneId.of("UTC")))
                .execute(((taskInstance, executionContext) -> service.scheduleUpdateAll(executionContext, updateObject)));
    }

    @Bean
    OneTimeTask<UUID> updateObject(Service service) {
        return Tasks.oneTime("updateObject", UUID.class)
                .execute(service::updateById);
    }

}
```

### Manual usage

1) Add a dependency to the project

```xml

<dependency>
    <groupId>io.gitlab.klawru</groupId>
    <artifactId>r2dbc-scheduler</artifactId>
    <version>${version}</version>
</dependency>
```

2) create table ``scheduled_job``. See table definition
   for [postgresql](r2dbc-scheduler/src/main/resources/postgres_table.sql)

3) Create the necessary tasks

```java
class Tasks {
    OneTimeTask<String> task = new OneTimeTaskBuilder<>("taskName", String.class)
            .execute((taskInstance, context) -> Mono.fromRunnable(() -> {
                String data = taskInstance.getData();
                log.info(data);
            }));
}
```

4) Create a task scheduler, and register task

```java
class Scheduler {
    SchedulerClient client = SchedulerBuilder.create(connectionFactory, task, everyHourTask)
            .schedulerConfig(schedulerConfigBuilder -> schedulerConfigBuilder
                    .schedulerName("name")
                    .threads(2)
            )
            .build();
}
```

5) Start the task scheduler

```java
client.start();
```

6) And then you can schedule the task via the SchedulerClient

```java
scheduler.schedule(taskA.instance("1"));
```

## Authors

Contributor names and contact info

* Klawru@gmail.com Amir

## Version History

* 0.1.0
    * Initial Release

## License

This project is licensed under the Apache License Version 2.0 — see the LICENSE file for details

## Acknowledgments

* [db-scheduler](https://github.com/kagkarlsson/db-scheduler) by kagkarlsson