# r2dbc-scheduler

This is a reactive task scheduler inspired by the [db-scheduler](https://github.com/kagkarlsson/db-scheduler) project.
Tasks are stored in persistent storage using R2DBC and Spring.

### Installing

* Soon

### Usage

* Create the necessary tasks

```java
OneTimeTask<String> task = new OneTimeTaskBuilder<>("taskName",String.class)
        .execute((taskInstance, context) -> Mono.fromRunnable(() -> {
            String data = taskInstance.getData();
            log.info(data);
        }));
```

* Create a task scheduler, and register task

```java
SchedulerClient client = SchedulerBuilder.create(connectionFactory, task, everyHourTask)
        .schedulerConfig(schedulerConfigBuilder -> schedulerConfigBuilder
            .schedulerName("name")
            .threads(2)
        )
        .build();
```

* Start the task scheduler

```java
client.start();
```

* And then you can schedule the task via the SchedulerClient
```java
scheduler.schedule(taskA.instance("1"));
```

## Authors

Contributors names and contact info

* Klawru@gmail.com Amir

## Version History

* 0.1
    * Initial Release

## License

This project is licensed under the Apache License Version 2.0 — see the LICENSE file for details

## Acknowledgments

* [db-scheduler](https://github.com/kagkarlsson/db-scheduler) by kagkarlsson