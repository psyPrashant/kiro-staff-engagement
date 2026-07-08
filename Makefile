.PHONY: accept-test accept-smoke accept-compile accept-build accept-install

accept-test:
	cd acceptance-tests && mvn clean test

accept-smoke:
	cd acceptance-tests && mvn clean test -Dcucumber.filter.tags="@pre-push"

accept-compile:
	cd acceptance-tests && mvn compile test-compile

accept-build:
	cd acceptance-tests && mvn clean package -DskipTests

accept-install:
	cd acceptance-tests && mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install --with-deps chromium"
