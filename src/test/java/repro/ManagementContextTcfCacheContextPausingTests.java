package repro;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Five distinct contexts with spring.test.context.cache.maxSize=2 (set in build.gradle):
 * contexts one, two and three are each paused and later evicted (closed) by the time
 * context five runs.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class ManagementContextTcfCacheContextPausingTests {

	@Nested
	@Order(1)
	@TestPropertySource(properties = "context=one")
	class ContextOne {

		@Test
		void test() {
		}

	}

	@Nested
	@Order(2)
	@TestPropertySource(properties = "context=two")
	class ContextTwo {

		@Test
		void test() {
		}

	}

	@Nested
	@Order(3)
	@TestPropertySource(properties = "context=three")
	class ContextThree {

		@Test
		void test() {
		}

	}

	@Nested
	@Order(4)
	@TestPropertySource(properties = "context=four")
	class ContextFour {

		@Test
		void test() {
		}

	}

	@Nested
	@Order(5)
	@TestPropertySource(properties = "context=five")
	class ContextFive {

		@Test
		void managementContextsOfEvictedContextsAreClosed() {
			long evicted = 0;
			long leaked = 0;
			for (ConfigurableApplicationContext management : ManagementContextCapture.managementContexts()) {
				ConfigurableApplicationContext parent = (ConfigurableApplicationContext) management.getParent();
				if (parent.getEnvironment().getProperty("context") == null) {
					continue; // a context of another test class in the same JVM
				}
				if (!parent.isActive()) {
					evicted++;
					if (management.isActive()) {
						leaked++;
					}
				}
			}
			System.out.printf("evicted contexts %d, their management contexts still active %d, container-N threads %d%n",
					evicted, leaked, ManagementContextCapture.tomcatAwaitThreads());
			assertThat(evicted).isEqualTo(3);
			assertThat(leaked).as("management contexts still active after their parent was evicted").isZero();
		}

	}

}
