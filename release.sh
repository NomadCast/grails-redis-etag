#!/bin/bash

rm -rf target/release
mkdir target/release
cd target/release
git clone https://github.com/tamershahin/grails-redis-etag.git
cd grails-redis-etag
grails clean
grails compile
grails publish-plugin --stacktrace
