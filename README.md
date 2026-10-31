# Child management context leak after pause or stop

Spring Boot 4.1.2, actuator with `management.server.port=0`.

    ./gradlew test                 # 4 failures: the bug
    ./gradlew test -Ppause=never   # the test context framework case passes with the workaround

`ManagementContextLifecycleTests` calls `pause()`, `stop()`, `restart()` and `close()` on the
context directly, with no test context framework. `ManagementContextTcfCacheContextPausingTests`
runs five cached contexts with `spring.test.context.cache.maxSize=2` and checks that the
management contexts of the evicted ones are closed.
