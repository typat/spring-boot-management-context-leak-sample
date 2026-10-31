package repro;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No test context framework here: the lifecycle calls are made by hand, so a failure
 * can only come from how the application context and Spring Boot react to them.
 */
class ManagementContextLifecycleTests {

	@BeforeEach
	void clear() {
		ManagementContextCapture.clear();
	}

	@Test
	void closeClosesManagementContext() {
		ConfigurableApplicationContext context = run();
		ConfigurableApplicationContext management = managementContext();
		context.close();
		assertThat(management.isActive()).as("management context active after close").isFalse();
	}

	@Test
	void pauseThenCloseClosesManagementContext() {
		ConfigurableApplicationContext context = run();
		ConfigurableApplicationContext management = managementContext();
		long before = ManagementContextCapture.tomcatAwaitThreads();
		context.pause();
		context.close();
		System.out.printf("pause+close: management active=%s, container-N threads %d -> %d%n",
				management.isActive(), before, ManagementContextCapture.tomcatAwaitThreads());
		assertThat(management.isActive()).as("management context active after pause() then close()").isFalse();
	}

	@Test
	void stopThenCloseClosesManagementContext() {
		ConfigurableApplicationContext context = run();
		ConfigurableApplicationContext management = managementContext();
		context.stop();
		context.close();
		assertThat(management.isActive()).as("management context active after stop() then close()").isFalse();
	}

	@Test
	void serversWhilePausedAndAfterRestart() {
		ConfigurableApplicationContext context = run();
		ConfigurableApplicationContext management = managementContext();
		int mainPort = ((WebServerApplicationContext) context).getWebServer().getPort();
		int managementPort = ((WebServerApplicationContext) management).getWebServer().getPort();
		context.pause();
		boolean mainUpWhilePaused = accepts(mainPort);
		boolean managementUpWhilePaused = accepts(managementPort);
		context.restart();
		int managementPortAfterRestart = ((WebServerApplicationContext) management).getWebServer().getPort();
		System.out.printf(
				"paused: main server port %d accepting=%s, management port %d accepting=%s; after restart management port %d accepting=%s%n",
				mainPort, mainUpWhilePaused, managementPort, managementUpWhilePaused, managementPortAfterRestart,
				accepts(managementPortAfterRestart));
		context.close();
		assertThat(managementUpWhilePaused).as("management server accepting while paused (main server is %s)",
				mainUpWhilePaused ? "accepting" : "not accepting")
			.isEqualTo(mainUpWhilePaused);
	}

	private ConfigurableApplicationContext run() {
		return new SpringApplication(ReproApplication.class).run("--server.port=0", "--management.server.port=0");
	}

	private ConfigurableApplicationContext managementContext() {
		assertThat(ManagementContextCapture.managementContexts()).hasSize(1);
		return ManagementContextCapture.managementContexts().get(0);
	}

	private static boolean accepts(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", port), 1000);
			return true;
		}
		catch (IOException ex) {
			return false;
		}
	}

}
