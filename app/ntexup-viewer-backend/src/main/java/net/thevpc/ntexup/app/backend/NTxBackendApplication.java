package net.thevpc.ntexup.app.backend;

import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.app.NAppRun;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@NApp
@SpringBootApplication
public class NTxBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NTxBackendApplication.class, args);
	}

//	@Override
//	public void onInstallApplication(NSession session) {
//		session.out().println("write your business logic that will be processed when the application is being installed here...");
//	}
//
//	@Override
//	public void onUpdateApplication(NSession session) {
//		session.out().println("write your business logic that will be processed when the application is being updated/upgraded here...");
//	}
//
//	@Override
//	public void onUninstallApplication(NSession session) {
//		session.out().println("write your business logic that will be processed when the application is being uninstalled/removed here...");
//	}

	@NAppRun
	public void run() {
		//
	}
}
