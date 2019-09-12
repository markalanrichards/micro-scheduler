FROM debian:buster AS build
RUN apt-get update
RUN apt-get dist-upgrade -y
RUN apt-get install apt-utils -y
RUN apt-get install openjdk-11-jdk -y
RUN apt-get install maven -y
ADD / /build-dir
WORKDIR /build-dir
RUN  mvn clean -Dmaven.repo.local=./.m2 \
     && mvn package -Dmaven.repo.local=./.m2 \
     && mvn dependency:copy-dependencies '-Dmaven.repo.local=./.m2' '-DprependGroupId=true' '-DincludeScope=runtime'

FROM debian:buster AS runtime
RUN apt-get update
RUN apt-get dist-upgrade -y
RUN apt-get install apt-utils -y
RUN apt-get install openjdk-11-jre -y
COPY --from=build /build-dir/target/dependency /classpath
COPY --from=build /build-dir/target/micro-scheduler-1.0-SNAPSHOT.jar /classpath

FROM runtime AS worker
CMD java -cp "/classpath/*" com.markssoftware.microscheduler.Worker

FROM runtime AS ui
CMD java -cp "/classpath/*" com.markssoftware.microscheduler.UI




