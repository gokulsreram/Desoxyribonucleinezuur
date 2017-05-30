package programminglife.model;

import org.junit.*;
import programminglife.InitFXThread;
import programminglife.parser.Cache;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by toinehartman on 03/05/2017.
 */
public class GenomeGraphTest {


    private static final String TEST_DB = "test.db";
    GenomeGraph graph;
    Segment node;
    String link;

    private static String TEST_PATH, TEST_FAULTY_PATH;

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
        graph = new GenomeGraph("test graph");
        node = new Segment(graph, 3, "ATCG");

        graph.addNode(node);
    }

    @After
    public void tearDown() throws Exception {
        graph.removeCache();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Cache.removeDB(TEST_DB);
    }

    @Test
    public void addNodeTest() throws Exception {
        Segment secondNode = new Segment(graph, 8);
        graph.addNode(secondNode);

        assertEquals(2, graph.size());
        assertTrue(graph.contains(3));
        assertTrue(graph.contains(8));
    }

    @Test
    public void getNodeTest2() {
        assertTrue(graph.contains(3));
    }

    @Test(expected = NullPointerException.class)
    public void getNodeTest1() {
        graph.getChildren(121);
    }

    @Test
    public void sizeTest() {
        assertEquals(1,graph.size());
        graph.addNode(new Segment(graph, 2,"AAAAT"));
        assertEquals(2,graph.size());
    }

    @Test
    public void containsTest() {
        Node node2 = new Segment(graph, 2, "ATTCTT");
        graph.addNode(node2);
        assertTrue(graph.contains(node2));
        Node node3 = new Segment(graph, 37,"AAAAAAAA");
        assertFalse(graph.contains(node3));
    }
}
