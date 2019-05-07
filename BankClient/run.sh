#!/usr/bin/env bash

erl -noshell -pa ebin/ deps/*/ebin -eval "application:start(thrift)" -eval "application:start(client)"