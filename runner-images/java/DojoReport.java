import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Turns the JUnit XML reports into the output of `cargo test`, the format the dojo reads:
 * one "test NAME ... ok|FAILED" line per test and a "---- NAME stdout ----" block with the
 * failure message. Exits with 1 when a test failed and 2 when none ran.
 */
public class DojoReport {
    record Result(String name, String failure) {}

    public static void main(String[] args) throws Exception {
        List<Result> results = new ArrayList<>();
        File[] reports = new File(args[0]).listFiles((dir, name) -> name.endsWith(".xml"));
        for (File report : reports == null ? new File[0] : reports) {
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report);
            NodeList cases = document.getElementsByTagName("testcase");
            for (int i = 0; i < cases.getLength(); i++) {
                Element testcase = (Element) cases.item(i);
                if (testcase.getElementsByTagName("skipped").getLength() > 0) {
                    continue;
                }
                results.add(new Result(name(testcase), failure(testcase)));
            }
        }

        System.out.println();
        System.out.println("running " + results.size() + " tests");
        for (Result result : results) {
            System.out.println("test " + result.name() + " ... " + (result.failure() == null ? "ok" : "FAILED"));
        }
        List<Result> failed = results.stream().filter(result -> result.failure() != null).toList();
        if (!failed.isEmpty()) {
            System.out.println();
            System.out.println("failures:");
            for (Result result : failed) {
                System.out.println();
                System.out.println("---- " + result.name() + " stdout ----");
                System.out.println("thread '" + result.name() + "' panicked at " + result.name() + ":");
                System.out.println(result.failure());
                System.out.println();
            }
            System.out.println("failures:");
        }
        System.out.printf("%ntest result: %s. %d passed; %d failed%n",
                failed.isEmpty() ? "ok" : "FAILED", results.size() - failed.size(), failed.size());
        System.exit(results.isEmpty() ? 2 : failed.isEmpty() ? 0 : 1);
    }

    /** "greets_a_stranger()" in class "dojo.GreeterTest" -> "GreeterTest::greets_a_stranger". */
    private static String name(Element testcase) {
        String className = testcase.getAttribute("classname");
        String simpleClass = className.substring(className.lastIndexOf('.') + 1);
        String method = testcase.getAttribute("name").replaceAll("\\(.*\\)$", "").replaceAll("\\s+", "_");
        return simpleClass + "::" + method;
    }

    /** The assertion message, without the stack trace; null when the test passed. */
    private static String failure(Element testcase) {
        for (String tag : new String[] {"failure", "error"}) {
            NodeList found = testcase.getElementsByTagName(tag);
            if (found.getLength() > 0) {
                Element problem = (Element) found.item(0);
                String message = problem.getAttribute("message");
                if (message.isBlank()) {
                    message = problem.getAttribute("type");
                }
                return message.strip();
            }
        }
        return null;
    }
}
