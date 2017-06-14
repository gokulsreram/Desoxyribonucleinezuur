package programminglife.parser;

import org.junit.*;
import programminglife.utility.InitFXThread;
import programminglife.model.GenomeGraph;
import programminglife.model.GenomeGraphTest;
import programminglife.model.exception.UnknownTypeException;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import static org.junit.Assert.*;

/**
 * The class that handles the tests for the parser.
 */
public class GraphParserTest implements Observer {

    private static final String TEST_DB = "test.gfa.db";
    private static final String TEST_FAULTY_DB = "test-faulty.gfa.db";
    private static final String TEST_DB2 = "test.db";
    private static final String TEST_FAULTY_DB2 = "test-faulty.db";

    private static String TEST_PATH, TEST_FAULTY_PATH;

    private String linkLine, nodeLine;
    private GraphParser graphParser, faultyGraphParser;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InitFXThread.setupClass();
        TEST_PATH = new File(GenomeGraphTest.class.getResource("/test.gfa").toURI()).getAbsolutePath();
        TEST_FAULTY_PATH = new File(
                GenomeGraphTest.class.getClass().getResource("/test-faulty.gfa").toURI()
        ).getAbsolutePath();
    }

    @Before
    public void setUp() throws Exception {
        File testFile = new File(TEST_PATH);
        graphParser = new GraphParser(testFile);
        graphParser.getGraph().addGenome("TKK_04_0031.fasta");

        File faultyTestFile = new File(TEST_FAULTY_PATH);
        faultyGraphParser = new GraphParser(faultyTestFile);

        linkLine = "L\t34\t+\t35\t+\t0M";
        nodeLine = "S\t6\tC\t*\tORI:Z:TKK_04_0031.fasta\tCRD:Z:TKK_04_0031.fasta\tCRDCTG:Z:7000000219691771\tCTG:Z:7000000219691771\tSTART:Z:3039";
    }

    @After
    public void tearDown() throws Exception {
        graphParser.getGraph().removeCache();
        faultyGraphParser.getGraph().removeCache();
    }

    @Test(expected = UnknownTypeException.class)
    public void faultyParseTest() throws Exception {
        faultyGraphParser.parse();
    }

    @Test
    public void parseTest() throws Exception {
        graphParser.parse();
        GenomeGraph graph = graphParser.getGraph();

        assertEquals(8, graph.size());
    }

    @Test
    public void parseLinkTest() {
         graphParser.parseLink(linkLine);
    }

    @Test
    public void parseSegmentTest() throws UnknownTypeException {
        graphParser.parseSegment(nodeLine);
        GenomeGraph g = graphParser.getGraph();

        assertTrue(g.contains(6));
        assertEquals("C", g.getSequence(6));
        assertEquals(0, g.getParentIDs(6).length);
        assertEquals(0, g.getChildIDs(6).length);
    }

    @Test
    public void runTestSuccess() {
        graphParser.addObserver(this);
        graphParser.run();
    }

    @Test(expected = UnknownTypeException.class)
    public void runTestFailure() throws Throwable {
        try {
            faultyGraphParser.addObserver(this);
            faultyGraphParser.run();
            fail();
        } catch (RuntimeException re) {
            throw re.getCause();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof GraphParser) {
            if (arg instanceof GenomeGraph) {
                GenomeGraph graph = (GenomeGraph) arg;

                assertEquals(new File(TEST_PATH).getName(), graph.getID());
                assertEquals("GTC", graph.getSequence(8));
            } else if (arg instanceof Exception) {
                throw new RuntimeException((Exception) arg);
            }
        }
    }

    @Test (expected = AssertionError.class)
    public void parseSegmentNotStartingS() {
        try {
            graphParser.parseSegment("Fiets");
        } catch (UnknownTypeException e) {
            e.printStackTrace();
        }
    }

    @Test (expected = AssertionError.class)
    public void parseLinkNotStartingL(){
        graphParser.parseLink("Fiets");
    }

    @Test (expected = AssertionError.class)
    public void parseHeaderNotStartingH() {
        graphParser.parseHeader("Fiets");
    }
}
