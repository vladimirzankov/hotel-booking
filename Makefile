.PHONY: build test check clean registry hotel booking gateway

export AUTH_JWT_SECRET ?= dev-secret-0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCD
export SPRING_PROFILES_ACTIVE ?= dev-test

MVN := mvn -q

build:
	$(MVN) clean install -DskipTests

test:
	$(MVN) test

check:
	$(MVN) verify -T1C

clean:
	$(MVN) clean

registry:
	$(MVN) -pl :service-registry spring-boot:run

hotel:
	$(MVN) -pl :hotel-api spring-boot:run

booking:
	$(MVN) -pl :booking-api spring-boot:run

gateway:
	$(MVN) -pl :api-gateway spring-boot:run
