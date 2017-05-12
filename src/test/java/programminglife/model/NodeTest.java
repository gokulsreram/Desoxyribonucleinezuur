package programminglife.model;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeTest {
    Node node;
    String line;

    @Before
    public void setUp() throws Exception {
        node = new Node(1, "ATCG");
        line = "S\t6\tC\t*\tORI:Z:TKK_04_0031.fasta\tCRD:Z:TKK_04_0031.fasta\tCRDCTG:Z:7000000219691771\tCTG:Z:7000000219691771\tSTART:Z:3039";
    }

    @Test
    public void NodeId() {
        node = new Node(8);

        assertEquals(8, node.getIdentifier());
        assertEquals("", node.getSequence());
        assertEquals(0, node.getParents().size());
        assertEquals(0, node.getChildren().size());
    }

    @Test
    public void NodeIdSequence() {
        node = new Node(8, "ATCG");

        assertEquals(8, node.getIdentifier());
        assertEquals("ATCG", node.getSequence());
        assertEquals(0, node.getParents().size());
        assertEquals(0, node.getChildren().size());
    }

    @Test
    public void NodeIdSequenceParentsChildren() {
        node = new Node(8, "ATCG", new HashSet<>(), new HashSet<>());

        assertEquals(8, node.getIdentifier());
        assertEquals("ATCG", node.getSequence());
        assertEquals(0, node.getParents().size());
        assertEquals(0, node.getChildren().size());
    }

    @Test
    public void getSequenceTest() {
        assertEquals("ATCG", node.getSequence());
    }

    @Test
    public void setSequenceTest() {
        node.setSequence("GCTA");

        assertEquals("GCTA", node.getSequence());
    }

    @Test
    public void parseSegmentTest() {
        node = Node.parseSegment(line);

        assertEquals(6, node.getIdentifier());
        assertEquals("C", node.getSequence());
        assertEquals(0, node.getParents().size());
        assertEquals(0, node.getChildren().size());
    }

    @Test
    public void childrenTest() {
        Node child1 = new Node(3);
        Node child2 = new Node(16);
        node.addChild(child1);
        node.addChild(child2);

        assertEquals(0, node.getParents().size());
        assertEquals(2, node.getChildren().size());
        assertTrue(node.getChildren().contains(child1));
        assertTrue(node.getChildren().contains(child2));
    }

    @Test
    public void parentsTest() {
        Node child1 = new Node(3);
        Node child2 = new Node(16);
        node.addParent(child1);
        node.addParent(child2);

        assertEquals(0, node.getChildren().size());
        assertEquals(2, node.getParents().size());
        assertTrue(node.getParents().contains(child1));
        assertTrue(node.getParents().contains(child2));
    }

    @Test
    public void locationTest() {
        node.setLocation(new XYCoordinate(1, 2));

        assertEquals(1, node.getLocation().getX());
        assertEquals(2, node.getLocation().getY());
    }

    @Test
    public void sizeTest() {
        node.setSize(new XYCoordinate(3, 4));

        assertEquals(3, node.getSize().getX());
        assertEquals(4, node.getSize().getY());
    }

    @Test
    public void centerTest() {
        node.setLocation(new XYCoordinate(2, 2));
        node.setSize(new XYCoordinate(4, 2));

        assertEquals(4, node.getCenter().getX());
        assertEquals(3, node.getCenter().getY());
    }

    @Test
    public void rightBorderCenterTest() {
        node.setLocation(new XYCoordinate(2, 2));
        node.setSize(new XYCoordinate(4, 2));

        assertEquals(6, node.getRightBorderCenter().getX());
        assertEquals(3, node.getRightBorderCenter().getY());
    }

    @Test
    public void leftBorderCenterTest() {
        node.setLocation(new XYCoordinate(2, 2));
        node.setSize(new XYCoordinate(4, 2));

        assertEquals(2, node.getLeftBorderCenter().getX());
        assertEquals(3, node.getLeftBorderCenter().getY());
    }
}