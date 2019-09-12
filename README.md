# Micro Scheduler

The goal of this project is demonstrate a simple quartz cluster configured independently of the workers.

This may not be far off production ready.

## How to use in your cloud?
1. You need a task identifiable by a UUID
2. Run a server that listens to a queue named by the task UUID
3. Define a job in with the same UUID and a Quartz Cron (see curl below)

You will likely want to store metadata about the job (maybe name, description, version, etc) in another service.
There might be other systems that wish to trigger the same task, perhaps on user request and this separation lends well to this. 

There is a source field, which might be uesful to store a URN or similar.


## Next steps
* Helm chart?
* Expose config
* Add tests
* Improve UI (maybe an HTML ui?)
* Extend amqp options to other queuing systems like SQS, JMS, etc
* Improve multistage build caching
* Metrics
* Not happy with `source` field




## Running
To run this locally execute `docker-compose up --build`


get jobs
```
curl localhost:8080/jobs
```

Add a job

```
curl -X POST \
  http://localhost:8080/job \
  -H 'Content-Type: application/json' \
  -H 'Host: localhost:8080' \
  -d '{
	"cron": "0 * * * * ?",
	"source": "minute",
	"uuid": "95449054-d3d7-11e9-babc-df8c2d598a5a"
}'
```

