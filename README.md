# Micro Scheduler

The goal of this project is demonstrate a simple quartz cluster configured independently of the workers.

This may not be far off production ready.

To run this locally execute `docker-compose up --build`

Next steps
* Helm chart?
* Expose config
* Add tests
* Improve UI (maybe an HTML ui?)
* Extend amqp options to other queuing systems like SQS, JMS, etc
* Improve multistage build caching

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

