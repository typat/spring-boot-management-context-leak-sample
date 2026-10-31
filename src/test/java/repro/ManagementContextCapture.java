package repro;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Registered through META-INF/spring.factories, so every SpringApplication gets it. The
 * child management context publishes its WebServerInitializedEvent to the parent too, so
 * this listener (in the parent) sees every management child context that gets created.
 */
public class ManagementContextCapture implements ApplicationListener<WebServerInitializedEvent> {

	private static final List<ConfigurableApplicationContext> managementContexts = new CopyOnWriteArrayList<>();

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		if ("management".equals(event.getApplicationContext().getServerNamespace())) {
			ConfigurableApplicationContext context = (ConfigurableApplicationContext) event.getApplicationContext();
			if (!managementContexts.contains(context)) {
				managementContexts.add(context);
			}
		}
	}

	static List<ConfigurableApplicationContext> managementContexts() {
		return managementContexts;
	}

	static void clear() {
		managementContexts.clear();
	}

	static long tomcatAwaitThreads() {
		return Thread.getAllStackTraces()
			.keySet()
			.stream()
			.filter((thread) -> thread.getName().startsWith("container-"))
			.count();
	}

}
