.PHONY: all install move run linear convex concave bucket

all: install move run

install:
	mvn clean install

move:
	cp Application/target/demo.jar demo/servers/server1;
	cp Application/target/demo.jar demo/servers/server2;
	cp Application/target/demo.jar demo/servers/server3;
	cp Application/target/demo.jar demo/linear/server1;
	cp Application/target/demo.jar demo/linear/server2;
	cp Application/target/demo.jar demo/linear/server3;
	cp Application/target/demo.jar demo/convex/server1;
	cp Application/target/demo.jar demo/convex/server2;
	cp Application/target/demo.jar demo/convex/server3;
	cp Application/target/demo.jar demo/concave/server1;
	cp Application/target/demo.jar demo/concave/server2;
	cp Application/target/demo.jar demo/concave/server3;
	cp Application/target/demo.jar demo/bucket/server1;
	cp Application/target/demo.jar demo/bucket/server2;
	cp Application/target/demo.jar demo/bucket/server3;

run:
	cd demo/servers/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/servers/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/servers/server1 && java -jar demo.jar  2>&1 |tee log.txt

linear:
	cd demo/linear/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/linear/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/linear/server1 && java -jar demo.jar  2>&1 |tee log.txt

convex:
	cd demo/convex/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/convex/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/convex/server1 && java -jar demo.jar  2>&1 |tee log.txt

concave:
	cd demo/concave/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/concave/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/concave/server1 && java -jar demo.jar  2>&1 |tee log.txt

bucket:
	cd demo/bucket/server3 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/bucket/server2 && java -jar demo.jar  > log.txt 2>&1 &
	cd demo/bucket/server1 && java -jar demo.jar  2>&1 |tee log.txt
