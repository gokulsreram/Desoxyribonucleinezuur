package programminglife.gui.controller;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import programminglife.model.Dummy;
import programminglife.model.GenomeGraph;
import programminglife.model.Segment;
import programminglife.model.drawing.DrawableEdge;
import programminglife.model.drawing.DrawableNode;
import programminglife.model.drawing.SubGraph;
import programminglife.utility.Console;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import static javafx.scene.shape.StrokeType.OUTSIDE;

/**
 * Created by Martijn van Meerten on 8-5-2017.
 * Controller for drawing the graph.
 */
public class GraphController {

    private GenomeGraph graph;
    private Group grpDrawArea;
    private double locationCenterY;
    private double locationCenterX;
    private LinkedList<DrawableNode> oldMinMaxList = new LinkedList<>();
    private SubGraph subGraph;
    private AnchorPane anchorGraphInfo;
    private LinkedList<DrawableNode> oldGenomeList = new LinkedList<>();

    /**
     * Initialize controller object.
     * @param graph The genome graph to control
     * @param grpDrawArea The {@link Group} to draw in
     * @param anchorGraphInfo the {@link AnchorPane} were to show the info of a node or edge.
     */
    public GraphController(GenomeGraph graph, Group grpDrawArea, AnchorPane anchorGraphInfo) {
        this.graph = graph;
        this.grpDrawArea = grpDrawArea;
        this.anchorGraphInfo = anchorGraphInfo;
    }

    /**
     * Method to draw the subgraph decided by a center node and radius.
     * @param center the node of which the radius starts.
     * @param radius the amount of layers to be drawn.
     */
    public void draw(int center, int radius) {
        long startTimeProgram = System.nanoTime();
        Segment centerSegment = new Segment(graph, center);
        DrawableNode centerNode = new DrawableNode(centerSegment);
        subGraph = new SubGraph(centerNode, radius);

        long startLayoutTime = System.nanoTime();

        subGraph.layout();

        long startTimeDrawing = System.nanoTime();

        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            drawNode(drawableNode);
            for (DrawableNode child : subGraph.getChildren(drawableNode)) {
                drawEdge(drawableNode, child);
            }
        }

        centerOnNodeId(center);
        highlightNode(center, Color.DARKORANGE);

//        // An example of how to highlight. This is not very practical atm but it works.
//        for (Object o : grpDrawArea.getChildren()) {
//            if (o instanceof DrawableEdge) {
//                DrawableEdge edge = (DrawableEdge) o;
//                if (edge.getLink().getEnd() instanceof Dummy) {
//                    if (edge.getLink().getEnd().getLink(null).getEnd().getIdentifier() % 2 == 0)
//                        highlightEdge(edge, Color.GOLDENROD);
//                }
//                if ((int) edge.getLink().getEnd().getIdentifier() % 2 == 0)
//                    highlightEdge(edge, Color.GOLDENROD);
//            }
//            if (o instanceof DrawableNode) {
//                DrawableNode node = (DrawableNode) o;
//                if (node.getNode() instanceof Dummy) {
//                    Dummy dummy = (Dummy) node.getNode();
//                    if ((int) dummy.getLink(null).getEnd().getIdentifier() % 2 == 0)
//                        highlightDummyNode(node, Color.GOLDENROD);
//                }
//
//            }
//        }



        long finishTime = System.nanoTime();
        long differenceTimeProgram = finishTime - startTimeProgram;
        long differenceTimeDrawing = finishTime - startTimeDrawing;
        long differenceTimeLayout = finishTime - startLayoutTime;
        long msdifferenceTimeProgram = differenceTimeProgram / 1000000;
        long milisecondTimeDrawing = differenceTimeDrawing   / 1000000;
        long msdifferenceTimeLayout = differenceTimeLayout   / 1000000;
        Console.println("Time of Drawing:  " + milisecondTimeDrawing);
        Console.println("Time of layout:  " + msdifferenceTimeLayout);
        Console.println("Time of Total Program:  " + msdifferenceTimeProgram);

    }

    /**
     * Fill the rectangles with the color.
     * @param nodes the Collection of {@link DrawableNode} to highlight.
     * @param color the {@link Color} to highlight with.
     */
    private void highlightNodesByID(Collection<Integer> nodes, Color color) {
        for (int i : nodes) {
            highlightNode(i, color);
        }
    }

    /**
     * Method to highlight a collection of nodes.
     * @param nodes The nodes to highlight.
     * @param color The color to highligh with.
     */
    private void highlightNodes(Collection<DrawableNode> nodes, Color color) {
        for (DrawableNode drawNode: nodes) {
            highlightNode(drawNode, color);
        }
    }

    /**
     * Fill the rectangle with the color.
     * @param nodeID the nodeID of the node to highlight.
     * @param color the {@link Color} to highlight with.
     */
    public void highlightNode(int nodeID, Color color) {
        DrawableNode node = subGraph.getNodes().get(new Segment(graph, nodeID));
        highlightNode(node, color);
    }

    /**
     * Highlights a single node.
     * @param node {@link DrawableNode} to highlight.
     * @param color {@link Color} to color with.
     */
    public void highlightNode(DrawableNode node, Color color) {
        node.setStroke(color);
        node.setStrokeWidth(3);
        node.setStrokeType(OUTSIDE);
    }

    /**
     * Method to highlight a Edge. Changes the stroke color of the Edge.
     * @param edge {@link DrawableEdge} is the edge to highlight.
     * @param color {@link Color} is the color in which the Edge node needs to highlight.
     */
    private void highlightEdge(DrawableEdge edge, Color color) {
        edge.setStroke(color);
    }

    /**
     * Method to highlight a dummy node. Changes the stroke color of the node.
     * @param node {@link DrawableNode} is the dummy node that needs highlighting.
     * @param color {@link Color} is the color in which the dummy node needs a highlight.
     */
    private void highlightDummyNode(DrawableNode node, Color color) {
        node.setStroke(color);
    }

    /**
     * Draws a edge on the location it has.
     * @param parent {@link DrawableNode} is the node to be draw from.
     * @param child {@link DrawableNode} is the node to draw to.
     */
    private void drawEdge(DrawableNode parent, DrawableNode child) {
        DrawableEdge edge = new DrawableEdge(parent, child);
        // If either parent or child are dummy nodes make on click use the link in that dummy.
        if (parent.getNode() instanceof Dummy) {
            edge.setOnMouseClicked(event -> {
                Console.println(parent.getNode().getLink(null).toString());
            });
        } else if (child.getNode() instanceof  Dummy) {
            edge.setOnMouseClicked(event -> {
                Console.println(child.getNode().getLink(null).toString());
            });
        } else {
            edge.setOnMouseClicked(event -> Console.println(edge.toString()));
        }
        edge.setOnMouseClicked(event -> {
            if (event.isShiftDown()) {
                showInfoEdge(edge, 250);
            } else {
                showInfoEdge(edge, 10);
            }
        });
        edge.colorize(graph);
        edge.setStartLocation(edge.getStart().getRightBorderCenter());
        edge.setEndLocation(edge.getEnd().getLeftBorderCenter());
        this.grpDrawArea.getChildren().add(edge);
        edge.toBack();
    }

    /**
     * Draws a node on the location it has.
     * @param drawableNode {@link DrawableNode} is the node to be drawn.
     */
    public void drawNode(DrawableNode drawableNode) {
        if (!(drawableNode.getNode() instanceof Dummy)) {
            drawableNode.setOnMouseClicked(event -> {
                Console.println(drawableNode.getSequence());
                Console.println(drawableNode.toString());
                Console.println("Genomes: " + graph.getGenomeNames(drawableNode.getGenomes()));
            });
        } else {
            Dummy node = (Dummy) drawableNode.getNode();
        }
        drawableNode.setOnMouseClicked(event -> {
            if (event.isShiftDown()) {
                showInfoNode(drawableNode, 250);
            } else {
                showInfoNode(drawableNode, 10);
            }
        });
        drawableNode.colorize(graph);
        this.grpDrawArea.getChildren().add(drawableNode);
    }

    /**
     * Getter for the graph.
     * @return - The graph
     */
    public GenomeGraph getGraph() {
        return graph;
    }

    /**
     * Setter for the graph.
     * @param graph The graph
     */
    void setGraph(GenomeGraph graph) {
        this.graph = graph;
    }

    /**
     * Clear the draw area.
     */
    public void clear() {
        this.grpDrawArea.getChildren().clear();
    }

    public double getLocationCenterX() {
        return locationCenterX;
    }


    public double getLocationCenterY() {
        return locationCenterY;
    }

    /**
     * Centers on the given node.
     * @param nodeId is the node to center on.
     */
    public void centerOnNodeId(int nodeId) {
        DrawableNode drawableCenterNode = subGraph.getNodes().get(new Segment(graph, nodeId));
        double xCoord = drawableCenterNode.getX();

        Bounds bounds = grpDrawArea.getParent().getLayoutBounds();
        double boundsHeight = bounds.getHeight();
        double boundsWidth = bounds.getWidth();

        locationCenterY = boundsHeight / 4;
        locationCenterX = boundsWidth / 2 - xCoord;

        grpDrawArea.setTranslateX(locationCenterX);
        grpDrawArea.setTranslateY(locationCenterY);
    }

    /**
     * Method to show the information of an edge.
     * @param edge DrawableEdge the edge which has been clicked on.
     * @param x int the x location of the TextField.
     */
    private void showInfoEdge(DrawableEdge edge, int x) {
        anchorGraphInfo.getChildren().removeIf(node1 -> node1.getLayoutX() == x);

        Text idText = new Text("Genomes: "); idText.setLayoutX(x); idText.setLayoutY(65);
        Text parentsText = new Text("Parent: "); parentsText.setLayoutX(x); parentsText.setLayoutY(115);
        Text childrenText = new Text("Child: "); childrenText.setLayoutX(x); childrenText.setLayoutY(165);

        TextField id = getTextField("Genomes: ", x, 70, Arrays.toString(edge.getLink().getGenomes()));
        TextField parent = getTextField("Parent Node: ", x, 120, edge.getStart().getNode().getIdentifier() + "");
        TextField child = getTextField("Child Node: ", x, 170, edge.getEnd().getNode().getIdentifier() + "");

        anchorGraphInfo.getChildren().addAll(idText, parentsText, childrenText, id, parent, child);
    }

    /**
     * Method to show the information of a node.
     * @param node DrawableNode the node which has been clicked on.
     * @param x int the x location of the TextField.
     */
    private void showInfoNode(DrawableNode node, int x) {
        Text idText = new Text("ID: "); idText.setLayoutX(x); idText.setLayoutY(65);
        Text parentText = new Text("Parents: "); parentText.setLayoutX(x); parentText.setLayoutY(115);
        Text childText = new Text("Children: "); childText.setLayoutX(x); childText.setLayoutY(165);
        Text inEdgeText = new Text("Incoming Edges: "); inEdgeText.setLayoutX(x); inEdgeText.setLayoutY(215);
        Text outEdgeText = new Text("Outgoing Edges: "); outEdgeText.setLayoutX(x); outEdgeText.setLayoutY(265);
        Text genomeText = new Text("Genomes: "); genomeText.setLayoutX(x); genomeText.setLayoutY(315);
        Text seqLengthText = new Text("Sequence Length: "); seqLengthText.setLayoutX(x); seqLengthText.setLayoutY(365);
        Text seqText = new Text("Sequence: "); seqText.setLayoutX(x); seqText.setLayoutY(415);

        anchorGraphInfo.getChildren().removeIf(node1 -> node1.getLayoutX() == x);

        TextField id = getTextField("ID: ", x, 70, node.getNode().getIdentifier() + "");

        StringBuilder parentSB = new StringBuilder();
        node.getNode().getParents().forEach(o -> parentSB.append(o.getIdentifier()).append(", "));
        TextField parents;
        if (parentSB.length() > 2) {
            parentSB.setLength(parentSB.length() - 2);
            parents = getTextField("Parents: ", x, 120, parentSB.toString());
        } else {
            parentSB.replace(0, parentSB.length(), "This node has no parent(s)");
            parents = getTextField("Parents: ", x, 120, parentSB.toString());
        }

        StringBuilder childSB = new StringBuilder();
        node.getNode().getChildren().forEach(o -> childSB.append(o.getIdentifier()).append(", "));
        TextField children;
        if (childSB.length() > 2) {
            childSB.setLength(childSB.length() - 2);
            children = getTextField("Children: ", x, 170, childSB.toString());
        } else {
            childSB.replace(0, childSB.length(), "This node has no child(ren)");
            children = getTextField("Children: ", x, 170, childSB.toString());
        }

        TextField inEdges = getTextField("Incoming Edges: ", x, 220, node.getNode().getParentEdges().size() + "");
        TextField outEdges = getTextField("Outgoing Edges: ", x, 270, node.getNode().getChildEdges().size() + "");
        TextField genome = getTextField("Genome: ", x, 320, Arrays.toString(node.getNode().getGenomes()));
        TextField seqLength = getTextField("Sequence Length: ", x, 370, node.getNode().getSequence().length() + "");
        TextField seq = getTextField(x + " Sequence: ", x, 420, node.getNode().getSequence());

        anchorGraphInfo.getChildren().addAll(idText, parentText, childText, inEdgeText,
                outEdgeText, genomeText, seqLengthText, seqText);
        anchorGraphInfo.getChildren().addAll(id, parents, children, inEdges, outEdges, genome, seqLength, seq);
    }

    /**
     * Returns a textField to be used by the edge and node information show panel.
     * @param id String the id of the textField.
     * @param x int the x coordinate of the textField inside the anchorPane.
     * @param y int the y coordinate of the textField inside the anchorPane.
     * @param text String the text to be shown by the textField.
     * @return TextField the created textField.
     */
    private TextField getTextField(String id, int x, int y, String text) {
        TextField textField = new TextField();
        textField.setId(id);
        textField.setText(text);
        textField.setLayoutX(x);
        textField.setLayoutY(y);
        textField.setEditable(false);
        textField.setStyle("-fx-text-box-border: transparent;-fx-background-color: none; -fx-background-insets: 0;"
                + " -fx-padding: 1 3 1 3; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        textField.setPrefSize(220, 25);

        return textField;
    }

    /**
     * Method to do highlighting based on a min and max amount of genomes in a node.
     * @param min The minimal amount of genomes
     * @param max The maximal amount of genomes
     * @param color the {@link Color} in which the highlight has to be done.
     */
    public void highlightMinMax(int min, int max, Color color) {
        LinkedList<DrawableNode> drawNodeList = new LinkedList<>();

        removeHighlight(oldMinMaxList);
        removeHighlight(oldGenomeList);
        for (DrawableNode drawableNode: subGraph.getNodes().values()) {
            if (drawableNode != null && !(drawableNode.getNode() instanceof Dummy)) {
                int genomeCount = drawableNode.getNode().getGenomes().length;
                if (genomeCount >= min && genomeCount <= max) {
                    drawNodeList.add(drawableNode);
                }
            }
        }
        oldMinMaxList = drawNodeList;
        highlightNodes(drawNodeList, color);
    }

    /**
     * Resets the node highlighting to remove highlights.
     * @param nodes are the nodes to remove the highlight from.
     */
    private void removeHighlight(Collection<DrawableNode> nodes) {
        for (DrawableNode node: nodes) {
            node.colorize(graph);
        }
    }


    /**
     * Highlights based on genomeID
     * @param genomeID the GenomeID to highlight on.
     */
    public void highlightByGenome(int genomeID) {
        LinkedList<DrawableNode> drawNodeList = new LinkedList<>();
        removeHighlight(oldGenomeList);
        removeHighlight(oldMinMaxList);
        for (DrawableNode drawableNode: subGraph.getNodes().values()) {
            int[] genomes = drawableNode.getGenomes();
            for (int i = 0; i < genomes.length; i++){
               if (genomes[i] == genomeID && !(drawableNode.getNode() instanceof Dummy)) {
                  drawNodeList.add(drawableNode);
               }
            }
        }
        oldGenomeList = drawNodeList;
        highlightNodes(drawNodeList, Color.YELLOW);
    }
}
