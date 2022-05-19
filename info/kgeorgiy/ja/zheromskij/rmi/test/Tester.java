package info.kgeorgiy.ja.zheromskij.rmi.test;

import java.io.PrintWriter;

import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class Tester {
    public static void main(String[] args) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectPackage("info.kgeorgiy.ja.zheromskij.rmi.test"))
            .filters(ClassNameFilter.includeClassNamePatterns(".*Test"))
            .build();
        Launcher launcher = LauncherFactory.create();
        launcher.discover(request);
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        
        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        if (summary.getFailures().isEmpty()) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
