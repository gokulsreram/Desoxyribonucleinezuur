package programminglife.model.drawing;

import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
import programminglife.model.GenomeGraph;
import programminglife.model.XYCoordinate;
import programminglife.utility.Console;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A part of a {@link programminglife.model.GenomeGraph}. It uses a centerNode and a radius.
 * Roughly, every node reachable within radius steps from centerNode is included in this graph.
 * When updating the centerNode or the radius, it also updates the Nodes within this SubGraph.
 */
public class SubGraph implements Iterable<DrawableNode> {
    private static final int DEFAULT_DYNAMIC_RADIUS = 50;
    private static final int DEFAULT_NODE_Y = 50;
    private static final int BORDER_BUFFER = 40;

    private static final int MIN_RADIUS_DEFAULT = 50;
    /**
     * The amount of padding between layers (horizontal padding).
     */
    private static final double LAYER_PADDING = 20;

    private static final double DIFF_LAYER_PADDING = 7;

    private double zoomLevel;

    /**
     * The amount of padding between nodes within a Layer (vertical padding).
     */
    private GenomeGraph graph;
    private LinkedHashMap<Integer, DrawableNode> nodes;
    private LinkedHashMap<Integer, DrawableNode> rootNodes;
    private LinkedHashMap<Integer, DrawableNode> endNodes;

    private ArrayList<Layer> layers;
    private Map<DrawableNode, Map<DrawableNode, Collection<Integer>>> genomes;
    private int numberOfGenomes;
    private boolean replaceSNPs;

    /**
     * Create a SubGraph from a graph, without any nodes initially.
     *
     * @param graph The {@link GenomeGraph} that this SubGraph is based on.
     * @param zoomLevel double of the zoomLevel.
     * @param replaceSNPs boolean for if the SNPs need to be drawn.
     */
    private SubGraph(GenomeGraph graph, double zoomLevel, boolean replaceSNPs) {
        this(graph, zoomLevel, replaceSNPs, new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Create a SubGraph with the specified nodes, rootNodes and endNodes.
     *
     * @param graph     The {@link GenomeGraph} that this SubGraph is based on.
     * @param zoomLevel double of the zoomLevel.
     * @param replaceSNPs boolean for if the SNPs need to be drawn.
     * @param nodes     The nodes of this SubGraph.
     * @param rootNodes The rootNodes of this SubGraph.
     * @param endNodes  The endNodes of this SubGraph.
     */
    private SubGraph(GenomeGraph graph, double zoomLevel, boolean replaceSNPs,
                     LinkedHashMap<Integer, DrawableNode> nodes, LinkedHashMap<Integer, DrawableNode> rootNodes,
                     LinkedHashMap<Integer, DrawableNode> endNodes) {
        this.graph = graph;
        this.zoomLevel = zoomLevel;
        this.nodes = nodes;
        this.rootNodes = rootNodes;
        this.endNodes = endNodes;
        this.genomes = new LinkedHashMap<>();
        this.numberOfGenomes = graph.getTotalGenomeNumber();
        this.replaceSNPs = replaceSNPs;

        this.replaceSNPs();
        this.calculateGenomes();
        this.createLayers();
    }

    /**
     * Create a SubGraph using a centerNode and a radius around that centerNode.
     * This SubGraph will include all Nodes within radius steps to a parent,
     * and then another 2radius steps to a child, and symmetrically the same with children / parents reversed.
     *
     * @param centerNode  The centerNode
     * @param radius      The radius
     * @param replaceSNPs flag if SNPs should be collapsed
     */
    public SubGraph(DrawableSegment centerNode, int radius, boolean replaceSNPs) {
        this(centerNode, 1, MIN_RADIUS_DEFAULT, Math.max(radius, MIN_RADIUS_DEFAULT), replaceSNPs);

        Layer firstLayer = layers.get(0);
        assert (firstLayer != null);

        firstLayer.setX(0);
        firstLayer.setDrawLocations(DEFAULT_NODE_Y, zoomLevel);
        this.setRightDrawLocations(this.layers, 0);
    }

    /**
     * Create a SubGraph using a centerNode and a radius around that centerNode.
     * This SubGraph will include all Nodes within radius steps to a parent,
     * and then another 2radius steps to a child, and symmetrically the same with children / parents reversed.
     *
     * @param centerNode  The centerNode
     * @param zoomLevel   double of the amount zoomed in/out
     * @param minRadius   The minimum radius.
     * @param radius      The radius
     * @param replaceSNPs flag if SNPs should be collapsed
     */
    private SubGraph(DrawableSegment centerNode, double zoomLevel, int minRadius, int radius, boolean replaceSNPs) {
        assert (minRadius <= radius);

        this.graph = centerNode.getGraph();
        this.zoomLevel = zoomLevel;
        this.layers = null;
        this.genomes = new LinkedHashMap<>();
        this.replaceSNPs = replaceSNPs;
        this.numberOfGenomes = graph.getTotalGenomeNumber();

        findNodes(this, Collections.singleton(centerNode), new LinkedHashMap<>(), radius);
        this.replaceSNPs();
        this.calculateGenomes();
        this.layout();
        this.colorize();
    }

    /**
     * Detect SNPs and replace them.
     */
    private void replaceSNPs() {
        if (this.replaceSNPs) {
            Map<Integer, DrawableNode> nodesCopy = new LinkedHashMap<>(this.nodes);
            for (Map.Entry<Integer, DrawableNode> entry : nodesCopy.entrySet()) {
                DrawableNode parent = entry.getValue();
                DrawableSNP snp = parent.createSNPIfPossible(this);
                if (snp != null) {
                    snp.getMutations().stream().map(DrawableNode::getIdentifier).forEach(id -> {
                        this.nodes.remove(id);
                        parent.getChildren().remove(id);
                        snp.getChild().getParents().remove(id);
                    });
                    this.nodes.put(snp.getIdentifier(), snp);
                }
            }
        }
    }

    /**
     * Find nodes within radius steps from centerNode.
     * This resets the {@link #nodes}, {@link #rootNodes} and {@link #endNodes}
     *
     * @param subGraph      The SubGraph to find these nodes for.
     * @param startNodes    The Nodes to start searching from.
     * @param excludedNodes The nodes that will not be added to this graph, even if they are found.
     * @param radius        The number of steps to search.
     */
    private static void findNodes(SubGraph subGraph, Collection<DrawableNode> startNodes,
                                  LinkedHashMap<Integer, DrawableNode> excludedNodes, int radius) {
        subGraph.nodes = new LinkedHashMap<>();
        subGraph.rootNodes = new LinkedHashMap<>();
        subGraph.endNodes = new LinkedHashMap<>();
        LinkedHashMap<Integer, DrawableNode> foundNodes = new LinkedHashMap<>();

        Queue<FoundNode> queue = new LinkedList<>();
        startNodes.forEach(node -> queue.add(new FoundNode(node, null)));
        queue.add(null);

        boolean lastRow = radius == 0;
        while (!queue.isEmpty()) {
            FoundNode current = queue.poll(); // Note: may still be null if the actual element is null!

            if (current == null) {
                radius--;
                if (radius == 0) {
                    lastRow = true;
                } else if (radius < 0) {
                    break;
                }
                queue.add(null);
                continue;
            }

            DrawableNode previous;
            if (excludedNodes.containsKey(current.node.getIdentifier())) {
                if (startNodes.contains(current.node)) {
                    previous = null; // to signify it did not exist in subGraph.nodes yet.
                } else {
                    continue; // This is an excluded node, just continue with next
                }
            } else {
                // normal (non-excluded) node, save this node, save the result to check whether we had found it earlier.
                previous = subGraph.nodes.put(current.node.getIdentifier(), current.node);
            }

            if (lastRow) {
                // last row, add this node to rootNodes / endNodes even if we already found this node
                // (for when a node is both a root and an end node)
                if (current.foundFrom == FoundNode.FoundFrom.CHILD
                        && (previous == null || subGraph.endNodes.containsKey(current.node.getIdentifier()))) {
                    subGraph.rootNodes.put(current.node.getIdentifier(), current.node);
                } else if (current.foundFrom == FoundNode.FoundFrom.PARENT
                        && (previous == null || subGraph.rootNodes.containsKey(current.node.getIdentifier()))) {
                    subGraph.endNodes.put(current.node.getIdentifier(), current.node);
                }
                // else: current.foundFrom == null, true for the centerNode.
                // Note: since radius is always at least MIN_RADIUS, we could else instead of else if.
                // But that would be premature optimization.
            } else if (previous != null) {
                // we already found this node, continue to next node.
                assert (previous.equals(current.node));
            } else {
                Collection<Integer> children = current.node.getChildren();
                Collection<Integer> parents = current.node.getParents();

                children.forEach(node -> {
                    if (node >= 0 && !foundNodes.containsKey(node)) {
                        DrawableSegment child = new DrawableSegment(subGraph.graph, node, subGraph.zoomLevel);
                        foundNodes.put(node, child);
                        queue.add(new FoundNode(child, FoundNode.FoundFrom.PARENT));
                    }
                });
                parents.forEach(node -> {
                    if (node >= 0 && !foundNodes.containsKey(node)) {
                        DrawableSegment parent = new DrawableSegment(subGraph.graph, node, subGraph.zoomLevel);
                        foundNodes.put(node, parent);
                        queue.add(new FoundNode(parent, FoundNode.FoundFrom.CHILD));
                    }
                });
            }
        }
    }

    /**
     * Checks whether a dynamic load is necessary. This includes both loading new nodes.
     * into the datastructure as well as removing nodes from the datastructure.
     *
     * @param leftBorder  The left border of the canvas.
     * @param rightBorder The right border of the canvas.
     * @return boolean to check if it is loaded.
     */
    public boolean checkDynamicLoad(int leftBorder, double rightBorder) {
        assert (leftBorder < rightBorder);

        boolean didLoad = false;

        // Note: It checks if layers.size() < BORDER_BUFFER, if so: we definitely need to load.
        // Otherwise, check that there is enough of a buffer outside the borders.
        if (layers.size() <= BORDER_BUFFER || layers.get(BORDER_BUFFER).getX() > leftBorder) {
            didLoad = this.addFromRootNodes(SubGraph.DEFAULT_DYNAMIC_RADIUS);
        }
        if (layers.size() <= BORDER_BUFFER || layers.get(layers.size() - BORDER_BUFFER - 1).getX() < rightBorder) {
            didLoad = this.addFromEndNodes(SubGraph.DEFAULT_DYNAMIC_RADIUS);
        }

        int amountOfLayersLeft = 0;
        int amountOfLayersRight = 0;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).getX() < 0) {
                amountOfLayersLeft++;
            } else if (layers.get(i).getX() > rightBorder) {
                amountOfLayersRight = layers.size() - i;
                break;
            }
        }

        if (amountOfLayersLeft > 3 * BORDER_BUFFER) {
            removeLeftLayers(BORDER_BUFFER);
        }
        if (amountOfLayersRight > 3 * BORDER_BUFFER) {
            removeRightLayers(BORDER_BUFFER);
        }

        return didLoad;
    }

    /**
     * Method to update the centerNode.
     *
     * @param centerCanvasX double of the x center of the canvas.
     * @param oldCenterNode int of the old centerNode.
     * @return int of the new centerNode.
     */
    public int updateCenterNode(double centerCanvasX, int oldCenterNode) {
        int centerIndex = getLayerIndex(layers, centerCanvasX);
        Layer centerLayer = layers.get(centerIndex);
        for (DrawableNode node : centerLayer) {
            if (node instanceof DrawableSegment
                    && ((DrawableSegment) node).getSequence().length() > 1) {
                return node.getIdentifier();
            }
        }
        if (centerIndex >= layers.size() - 1) {
            for (DrawableNode node : layers.get(centerIndex - 1)) {
                if (node instanceof DrawableSegment
                        && ((DrawableSegment) node).getSequence().length() > 1) {
                    return node.getIdentifier();
                }
            }
        } else {
            for (DrawableNode node : layers.get(centerIndex + 1)) {
                if (node instanceof DrawableSegment
                        && ((DrawableSegment) node).getSequence().length() > 2) {
                    return node.getIdentifier();
                }
            }
        }
        return oldCenterNode;
    }

    /**
     * Removes layers from the right of the graph.
     *
     * @param numberOfLayers The number of layers to remove from the graph.
     */
    private void removeRightLayers(int numberOfLayers) {
        for (Layer layer : this.layers.subList(this.layers.size() - numberOfLayers, this.layers.size())) {
            layer.forEach(node -> this.nodes.remove(node.getIdentifier()));
        }
        this.layers = new ArrayList<>(this.layers.subList(0, this.layers.size() - numberOfLayers));
        this.endNodes = new LinkedHashMap<>();
        this.layers.get(this.layers.size() - 1).
                forEach(node -> endNodes.put(node.getIdentifier(), node));
    }

    /**
     * Removes layers from the left of the graph.
     *
     * @param numberOfLayers The number of layers to remove from the graph.
     */
    private void removeLeftLayers(int numberOfLayers) {
        for (Layer layer : this.layers.subList(0, numberOfLayers)) {
            layer.forEach(node -> this.nodes.remove(node.getIdentifier()));
        }
        this.layers = new ArrayList<>(this.layers.subList(numberOfLayers, this.layers.size()));
        this.rootNodes = new LinkedHashMap<>();
        this.layers.get(0).
                forEach(node -> rootNodes.put(node.getIdentifier(), node));
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    /**
     * On click method.
     *
     * @param x location
     * @param y location
     * @return Drawable on which clicked. if nothing null.
     */
    public Drawable onClick(double x, double y) {
        for (DrawableNode drawableNode : this.getNodes().values()) {
            //Divide by 2 because only half of the edge is outside the node
            //the other half falls on the inside but gets drawn over.
            double edgeThickness = drawableNode.getStrokeWidth() / 2;
            if (x >= drawableNode.getLocation().getX() - edgeThickness
                    && y >= drawableNode.getLocation().getY() - edgeThickness
                    && x <= drawableNode.getLocation().getX() + drawableNode.getWidth() + edgeThickness
                    && y <= drawableNode.getLocation().getY() + drawableNode.getHeight() + edgeThickness) {
                    return drawableNode;
            }
        }

        return onClickEdge(x, y);
    }

    /**
     * Check if clicked on an edge.
     *
     * @param x location
     * @param y location
     * @return DrawableEdge
     */
    private DrawableEdge onClickEdge(double x, double y) {

        //Find layers on the left and the right of the clicked location.
        int leftLayerIndex = getLayerIndex(layers, x);

        if (layers.get(leftLayerIndex).getX() > x) {
            if (leftLayerIndex == 0) {
                // outside graph, no edge can be clicked here.
                return null;
            } else {
                leftLayerIndex--;
            }
        }

        //get the corresponding layers.
        Layer leftLayer = layers.get(leftLayerIndex);

        //click falls in between two layers.
        if (leftLayer.getX() + leftLayer.getWidth() < x) {
            for (DrawableNode left : leftLayer.getNodes()) {
                for (DrawableNode right : this.getChildren(left)) {
                    if (calculateEdge(left, right, x, y)) {

                        return new DrawableEdge(left, right);
                    }

                }
            }


        } else if (leftLayer.getX() + leftLayer.getWidth() > x) { //Edge within a layer.
            for (DrawableNode left : leftLayer.getNodes()) {
                for (DrawableNode right : this.getChildren(left)) {
                    if (calculateEdgeInLayer(left, right, x, y)) {
                        return new DrawableEdge(left, right);
                    }

                }
            }
        }
        return null;
    }

    /**
     * Calculates the edges and see if the onclick is on a line. BUT THIS TIME INSIDE A LAYER
     *
     * @param left node of the edge.
     * @param right node of the edge.
     * @param x location
     * @param y location
     * @return true if clicked on edge, false if not
     */
    private boolean calculateEdgeInLayer(DrawableNode left, DrawableNode right, double x, double y) {
        double start = left.getRightBorderCenter().getX();
        Layer layer = left.getParentSegment().getLayer();
        double end = layer.getX() + layer.getWidth();

        double edgeY = left.getRightBorderCenter().getY(); // Since it is a horizontal line it stays on 1 Y

        double genomeFraction = getGenomesEdge(left.getParentSegment(),
                right.getChildSegment()).size() / (double) this.getNumberOfGenomes();
        double minStrokeWidth = 1.d, maxStrokeWidth = 6.5;
        double strokeWidth = minStrokeWidth + genomeFraction * (maxStrokeWidth - minStrokeWidth);

        return (edgeY - strokeWidth * zoomLevel < y && edgeY + strokeWidth * zoomLevel > y
                && x > start && x < end);
    }


    /**
     * Calculates the edges and see if the onclick is on line.
     *
     * @param left node of the edge.
     * @param right node of the edge.
     * @param x location
     * @param y location
     * @return true if clicked on edge, false if not
     */
    private boolean calculateEdge(DrawableNode left, DrawableNode right, double x, double y) {
        //calculate edge start
        double leftLayerEnd = left.getLayer().getX() + left.getLayer().getWidth();
        XYCoordinate start = left.getRightBorderCenter();
        start.setX(leftLayerEnd); //make the start the layer end since this is where the edge starts to move.
        XYCoordinate end = right.getLeftBorderCenter();

        //calculate differences.
        double differenceX = end.getX() - start.getX();
        double differenceY = end.getY() - start.getY(); //Negative if line is going up, positive if line is going down.

        //calculate a out of the ax+b formula;
        double deltaY = differenceY / differenceX;

        double startX = start.getX();
        double startY = start.getY();

        double edgeY = startY + (deltaY * (x - startX));

        double genomeFraction = getGenomesEdge(left.getParentSegment(),
                right.getChildSegment()).size() / (double) this.getNumberOfGenomes();
        double minStrokeWidth = 1.d, maxStrokeWidth = 6.5;
        double strokeWidth = minStrokeWidth + genomeFraction * (maxStrokeWidth - minStrokeWidth);

        return (edgeY - strokeWidth * zoomLevel < y && edgeY + strokeWidth * zoomLevel > y);
    }

    /**
     * Method to get the genomes of an edge.
     * @param parent  node of the edge
     * @param child node of the edge.
     * @return Collection of Integer which are the id's of the genomes.
     */
    private Collection<Integer> getGenomesEdge(DrawableNode parent, DrawableNode child) {
        Map<DrawableNode, Collection<Integer>> from = this.getGenomes().get(parent);
                if (from != null) {
                    return from.get(child);
                }
        return null;

    }

    /**
     * Getter for the collection of segments of the parent of a node.
     *
     * @param node Drawable segment to check the parents of.
     * @return Collection<DrawableNode> the collection of segment from a parent.
     */
    public Collection<DrawableNode> getParentSegments(DrawableSegment node) {
        return this.getParents(node).stream().map(DrawableNode::getParentSegment).collect(Collectors.toSet());
    }

    /**
     * Getter for the collection of segments of the child of a node.
     *
     * @param node Drawable segment to check the children of.
     * @return Collection<DrawableNode> the collection of segment from a child.
     */
    public Collection<DrawableNode> getChildSegments(DrawableSegment node) {
        return this.getChildren(node).stream().map(DrawableNode::getChildSegment).collect(Collectors.toSet());
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<DrawableNode> iterator() {
        return this.nodes.values().iterator();
    }

    /**
     * A class for keeping track of how a Node was found. Only used within {@link SubGraph#findNodes}.
     */
    private static final class FoundNode {
        /**
         * Whether a node was found from a parent or a child.
         */
        private enum FoundFrom {
            PARENT, CHILD
        }

        private final DrawableNode node;
        private final FoundFrom foundFrom;

        /**
         * simple constructor for a FoundNode.
         *
         * @param node      The node that was found.
         * @param foundFrom Whether it was found from a parent or a child.
         */
        private FoundNode(DrawableNode node, FoundFrom foundFrom) {
            this.node = node;
            this.foundFrom = foundFrom;
        }
    }

    /**
     * Find out which {@link Drawable} is at the given location.
     *
     * @param loc The location to search for Drawables.
     * @return The {@link Drawable} that is on top at the given location.
     */
    public Drawable atLocation(XYCoordinate loc) {
        return this.atLocation(loc.getX(), loc.getY());
    }

    /**
     * Find out which {@link Drawable} is at the given location.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The {@link Drawable} that is on top at the given location.
     */
    private Drawable atLocation(double x, double y) {
        int layerIndex = getLayerIndex(this.layers, x);

        // TODO: implement;
        // 1: check that x is actually within the layer.
        // 2: check nodes in layer for y coordinate.
        throw new Error("Not implemented yet");
    }

    /**
     * Get the index of the {@link Layer} closest to an x coordinate.
     * If two layers are equally close (x is exactly in the middle of end
     * of left layer and start of the right layer), the right Layer is returned, as this
     * one is likely to have nodes closer (nodes in the left layer do not necessarily extend to the end)
     *
     * @param x      The coordinate
     * @param layers The list of layers to look through.
     * @return The index of the closest layer.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    private int getLayerIndex(List<Layer> layers, double x) {
        int resultIndex = Collections.binarySearch(layers, x);
        if (resultIndex >= layers.size()) {
            // x is right outside list, last layer is closest.
            return layers.size() - 1;
        } else if (resultIndex >= 0) {
            // x is exactly at the start of a layer, that layer is closest.
            return resultIndex;
        } else {
            // x < 0, -x is the closest layer on the right, -x - 1 is the closest
            // layer on the left. (see binarySearch documentation)
            // check which of the two layers is closest.
            int insertionPoint = -(resultIndex + 1);
            int rightLayerIndex = insertionPoint;
            int leftLayerIndex = insertionPoint - 1;

            if (rightLayerIndex >= layers.size()) {
                return layers.size() - 1;
            } else if (leftLayerIndex <= 0) {
                return 0;
            }

            Layer rightLayer = layers.get(rightLayerIndex);
            Layer leftLayer = layers.get(leftLayerIndex);

            if (rightLayer.getX() - x > x - (leftLayer.getX() + leftLayer.getWidth())) {
                // distance from right layer is greater, so left layer is closer
                return leftLayerIndex;
            } else {
                return rightLayerIndex;
            }
        }
    }

    /**
     * Lay out the {@link Drawable Drawables} in this SubGraph.
     */
    private void layout() {
        createLayers();

        int minimumLayerIndex = findMinimumNodesLayerIndex(this.layers);
        sortLayersLeftFrom(minimumLayerIndex);
        sortLayersRightFrom(minimumLayerIndex);
    }

    /**
     * Assign nodes to {@link Layer layers} and create dummyNodes for edges that span multiple layers.
     */
    private void createLayers() {
        this.layers = findLayers();

        createDummyNodes(layers);
    }

    /**
     * Set the coordinates for all {@link Layer layers} to the right of the given Layer.
     *
     * @param layers        The Layers to set the coordinates for.
     * @param setLayerIndex The index of the Layer to start from (exclusive,
     *                      so coordinates are not set for this layer).
     */
    private void setRightDrawLocations(ArrayList<Layer> layers, int setLayerIndex) {
        ListIterator<Layer> layerIterator = layers.listIterator(setLayerIndex);
        Layer setLayer = layerIterator.next();
        double x = setLayer.getX() + setLayer.getWidth();
        double firstY = setLayer.getY();
        int size = setLayer.size();

        while (layerIterator.hasNext()) {
            Layer layer = layerIterator.next();

            layer.setSize(zoomLevel);

            int newSize = layer.size();
            int diff = Math.abs(newSize - size);
            x += (LAYER_PADDING * zoomLevel) + (DIFF_LAYER_PADDING * zoomLevel) * diff;

            layer.setX(x);
            layer.setDrawLocations(firstY, zoomLevel);
            x += layer.getWidth() + (LAYER_PADDING * zoomLevel) * 0.1 + newSize;
            size = newSize;
        }
    }

    /**
     * Set the coordinates for all {@link Layer layers} to the left of the given Layer.
     *
     * @param layers        The Layers to set the coordinates for.
     * @param setLayerIndex The index of the Layer to start from (exclusive,
     *                      so coordinates are not set for this layer).
     */
    private void setLeftDrawLocations(ArrayList<Layer> layers, int setLayerIndex) {
        ListIterator<Layer> layerIterator = layers.listIterator(setLayerIndex + 1);
        Layer setLayer = layerIterator.previous();
        double x = setLayer.getX();
        double firstY = setLayer.getY();
        int size = setLayer.size();

        while (layerIterator.hasPrevious()) {
            Layer layer = layerIterator.previous();

            layer.setSize(zoomLevel);

            int newSize = layer.size();
            int diff = Math.abs(newSize - size);
            x -= (LAYER_PADDING * zoomLevel) + diff * (DIFF_LAYER_PADDING * zoomLevel)
                    + layer.getWidth();

            layer.setX(x);
            layer.setDrawLocations(firstY, zoomLevel);
            x -= (LAYER_PADDING * zoomLevel) * 0.1 + newSize;
            size = newSize;
        }
    }

    /**
     * Create {@link DrawableDummy} nodes for layers to avoid more crossing edges.
     *
     * @param layers {@link List} representing all layers to be drawn.
     */
    private void createDummyNodes(List<Layer> layers) {
        Layer current = new Layer();
        for (Layer next : layers) {
            for (DrawableNode node : current) {
                for (DrawableNode child : this.getChildren(node)) {
                    if (!next.contains(child)) {
                        DrawableDummy dummy = new DrawableDummy(
                                DrawableNode.getUniqueId(), node, child, this.getGraph(), this);
                        node.replaceChild(child, dummy);
                        child.replaceParent(node, dummy);
                        dummy.setWidth(next.getWidth());
                        this.nodes.put(dummy.getIdentifier(), dummy);
                        dummy.setLayer(next);
                        next.add(dummy);
                    }
                }
            }
            current = next;
        }
    }

    /**
     * Put all nodes in {@link Layer Layers}. This method is used when {@link #layout laying out} the graph.
     * This will put each node in a Layer one higher than each of its parents.
     *
     * @return A {@link List} of Layers with all the nodes (all nodes are divided over the Layers).
     */
    private ArrayList<Layer> findLayers() {
        long startTime = System.nanoTime();
        List<DrawableNode> sorted = topoSort();
        long finishTime = System.nanoTime();
        long differenceTime = finishTime - startTime;
        long millisecondTime = differenceTime / 1000000;
        Console.println("TIME OF TOPOSORT:  " + millisecondTime);
        Console.println("Amount of nodes: " + sorted.size());
        Map<DrawableNode, Integer> nodeLevel = new LinkedHashMap<>();
        ArrayList<Layer> layerList = new ArrayList<>();

        for (DrawableNode node : sorted) {
            int maxParentLevel = -1;
            for (DrawableNode parent : this.getParents(node)) {
                Integer parentLevel = nodeLevel.get(parent);
                if (maxParentLevel < parentLevel) {
                    maxParentLevel = parentLevel;
                }
            }
            maxParentLevel++; // we want this node one level higher than the highest parent.
            nodeLevel.put(node, maxParentLevel);
            if (layerList.size() <= maxParentLevel) {
                layerList.add(new Layer());
            }
            node.setLayer(layerList.get(maxParentLevel));
            layerList.get(maxParentLevel).add(node);
        }

        return layerList;
    }

    /**
     * Get the parents of {@link DrawableNode} node.
     *
     * @param node The {@link DrawableNode} to get
     * @return A {@link Collection} of {@link DrawableNode}
     */
    Collection<DrawableNode> getParents(DrawableNode node) {
        Collection<DrawableNode> parents = new LinkedHashSet<>();
        for (int parentID : node.getParents()) {
            if (this.nodes.containsKey(parentID)) {
                parents.add(this.nodes.get(parentID));
            }
        }
        return parents;
    }

    /**
     * Get the children of {@link DrawableNode} node.
     *
     * @param node The {@link DrawableNode} to get
     * @return A {@link Collection} of {@link DrawableNode}
     */
    public Collection<DrawableNode> getChildren(DrawableNode node) {
        Collection<DrawableNode> children = new LinkedHashSet<>();
        for (int childID : node.getChildren()) {
            if (this.nodes.containsKey(childID)) {
                children.add(this.nodes.get(childID));
            }
        }
        return children;
    }

    /**
     * Find the Layer with the least number of nodes.
     *
     * @param layers The {@link Layer Layers} to search through.
     * @return The index of the Layer with the minimum number of nodes, or -1 if the list of layers is empty.
     */
    private int findMinimumNodesLayerIndex(List<Layer> layers) {
        // find a layer with a single node

        int index = -1;
        int min = Integer.MAX_VALUE;

        Iterator<Layer> layerIterator = layers.iterator();
        for (int i = 0; layerIterator.hasNext(); i++) {
            Layer currentLayer = layerIterator.next();

            int currentSize = currentLayer.size();
            if (currentSize < min) {
                min = currentSize;
                index = i;
                if (currentSize <= 1) {
                    // There should be no layers with less than 1 node,
                    // so this layer has the least amount of nodes.
                    break;
                }
            }
        }

        return index;
    }

    /**
     * Sort all {@link Layer Layers} right from a given layer.
     *
     * @param layerIndex The index of the layer to start sorting from (exclusive, so that layer is not sorted).
     */
    private void sortLayersRightFrom(int layerIndex) {
        ListIterator<Layer> iterator = layers.listIterator(layerIndex);
        Layer prev = iterator.next();

        while (iterator.hasNext()) {
            Layer layer = iterator.next();
            layer.sort(this, prev, true);
            prev = layer;
        }
    }

    /**
     * Sort all {@link Layer Layers} left from a given layer.
     *
     * @param layerIndex The index of the layer to start sorting from (exclusive, so that layer is not sorted).
     */
    private void sortLayersLeftFrom(int layerIndex) {
        ListIterator<Layer> iterator = layers.listIterator(layerIndex + 1);
        Layer prev = iterator.previous();

        while (iterator.hasPrevious()) {
            Layer layer = iterator.previous();
            layer.sort(this, prev, false);
            prev = layer;
        }
    }

    /**
     * Topologically sort the nodes from this graph.
     *
     * Assumption: graph is a DAG.
     * @return a topologically sorted list of nodes
     */
    List<DrawableNode> topoSort() {
        // topo sorted list
        ArrayList<DrawableNode> res = new ArrayList<>(this.nodes.size());

        // nodes that have not yet been added to the list.
        LinkedHashSet<DrawableNode> found = new LinkedHashSet<>();

        // tactic:
        // {
        //   take any node. see if any parents were not added yet.
        //   If so, clearly that parent needs to be added first. Continue searching from parent.
        //   If not, we found a node that can be next in the ordering. Add it to the list.
        // }
        // Repeat until all nodes are added to the list.
        for (DrawableNode n : this.nodes.values()) {
            if (!found.add(n)) {
                continue;
            }
            topoSortFromNode(res, found, n);
        }

        assert (res.size() == this.nodes.size());
        return res;
    }

    /**
     * Toposort all ancestors of a node.
     *
     * @param result The result list to which these nodes will be added,
     * @param found  The nodes that have already been found,
     * @param node   The node to start searching from.
     */
    private void topoSortFromNode(ArrayList<DrawableNode> result,
                                  LinkedHashSet<DrawableNode> found, DrawableNode node) {
        for (int parentID : node.getParents()) {
            DrawableNode drawableParent = this.nodes.get(parentID);
            if (drawableParent != null && found.add(drawableParent)) {
                topoSortFromNode(result, found, drawableParent);
            }
        }
        result.add(node);
    }

    /**
     * Calculate genomes through all outgoing edges of a parent.
     *
     * @param parent find all genomes through edges from this parent
     * @return a {@link Map} of  collections of genomes through links
     */
    private Map<DrawableNode, Collection<Integer>> calculateGenomes(DrawableNode parent) {
        Map<DrawableNode, Collection<Integer>> outgoingGenomes = new LinkedHashMap<>();

        // Create set of parent genomes
        Set<Integer> parentGenomes = new LinkedHashSet<>(parent.getParentGenomes());
        // Topo sort (= natural order) children
        Collection<DrawableNode> children = this.getChildren(parent);
        // For every child (in order); do
        children.stream()
                .map(DrawableNode::getChildSegment)
                .sorted(Comparator.comparingInt(DrawableNode::getIdentifier))
                .forEach(child -> {
                    Set<Integer> childGenomes = new LinkedHashSet<>(child.getGenomes());
                    // Find mutual genomes between parent and child
                    Set<Integer> mutualGenomes = Sets.intersect(parentGenomes, childGenomes);
                    // Add mutual genomes to edge
                    outgoingGenomes.put(child, mutualGenomes);

                    // Subtract mutual genomes from parent set
                    parentGenomes.removeAll(mutualGenomes);
                });

        return outgoingGenomes;
    }

    /**
     * Calculate genomes through edge, based on topological ordering and node-genome information.
     *
     * @return a {@link Map} of {@link Map Maps} of collections of genomes through links
     */
    Map<DrawableNode, Map<DrawableNode, Collection<Integer>>> calculateGenomes() {
        // For every node in the subGraph
        this.nodes.values().stream().map(DrawableNode::getParentSegment).forEach(parent -> {
            Map<DrawableNode, Collection<Integer>> parentGenomes = this.calculateGenomes(parent);
            this.genomes.put(parent, parentGenomes);
        });

        return this.genomes;
    }

    /**
     * Add nodes from the {@link #rootNodes}.
     *
     * @param radius The number of steps to take from the rootNodes before stopping the search.
     * @return if the rootNodes are added.
     */
    private boolean addFromRootNodes(int radius) {
        if (this.rootNodes.isEmpty()) {
            return false;
        }

        Console.println("Increasing graph with radius %d", radius);
        SubGraph subGraph = new SubGraph(graph, zoomLevel, replaceSNPs);

        this.rootNodes.forEach((id, node) -> this.endNodes.remove(id));
        findNodes(subGraph, rootNodes.values(), this.nodes, radius);
        subGraph.replaceSNPs();
        subGraph.createLayers();
        subGraph.calculateGenomes();
        this.mergeLeftSubGraphIntoThisSubGraph(subGraph);

        return true;
    }

    /**
     * Add nodes from the {@link #endNodes}.
     *
     * @param radius The number of steps to take from the endNodes before stopping the search.
     * @return boolean if the endNodes are added.
     */
    private boolean addFromEndNodes(int radius) {
        if (this.endNodes.isEmpty()) {
            return false;
        }

        Console.println("Increasing graph with radius %d", radius);
        SubGraph subGraph = new SubGraph(graph, zoomLevel, replaceSNPs);

        this.endNodes.forEach((id, node) -> System.out.print(id + " "));
        System.out.println();

        this.endNodes.forEach((id, node) -> this.rootNodes.remove(id));
        findNodes(subGraph, endNodes.values(), this.nodes, radius);
        subGraph.replaceSNPs();
        subGraph.createLayers();
        subGraph.calculateGenomes();
        this.mergeRightSubGraphIntoThisSubGraph(subGraph);

        return true;
    }

    /**
     * Method to merge subGraphs with each other.
     *
     * @param rightSubGraph SubGraph to be merged into from the right.
     */
    private void mergeRightSubGraphIntoThisSubGraph(SubGraph rightSubGraph) {
        this.nodes.putAll(rightSubGraph.nodes);
        this.endNodes = rightSubGraph.endNodes;
        rightSubGraph.rootNodes.forEach((id, node) -> {
            boolean addToEndNodes = false;
            for (Integer parentId : node.getParents()) {
                if (this.nodes.containsKey(parentId)) {
                    addToEndNodes = true;
                    break;
                }
            }
            if (!addToEndNodes) {
                for (Integer childId : node.getChildren()) {
                    if (this.nodes.containsKey(childId)) {
                        addToEndNodes = true;
                        break;
                    }
                }
            }
            if (addToEndNodes) {
                this.endNodes.put(id, node);
            }
        });

        int oldLastIndex = this.layers.size() - 1;
        this.layers.addAll(rightSubGraph.layers);


        // TODO: find DummyNodes between subgraphs. Just use findDummyNodes on full graph?
        rightSubGraph.genomes.forEach((parent, childMap) -> this.genomes
                .computeIfAbsent(parent, parentId -> new LinkedHashMap<>())
                .putAll(childMap));
        rightSubGraph.colorize();

        this.sortLayersRightFrom(oldLastIndex);
        this.setRightDrawLocations(this.layers, oldLastIndex);
    }

    /**
     * Merge another {@link SubGraph} into this SubGraph, by putting it on the left of this SubGraph,
     * and then drawing connecting edges between nodes that should have them.
     *
     * @param leftSubGraph The other SubGraph that will be merged into this one.
     */
    private void mergeLeftSubGraphIntoThisSubGraph(SubGraph leftSubGraph) {
        this.nodes.putAll(leftSubGraph.nodes);
        this.rootNodes = leftSubGraph.rootNodes;
        leftSubGraph.endNodes.forEach((id, node) -> {
            boolean addToRootNodes = false;
            for (Integer childId : node.getChildren()) {
                if (this.nodes.containsKey(childId)) {
                    addToRootNodes = true;
                    break;
                }
            }
            if (!addToRootNodes) {
                for (Integer parentId : node.getParents()) {
                    if (this.nodes.containsKey(parentId)) {
                        addToRootNodes = true;
                        break;
                    }
                }
            }
            if (addToRootNodes) {
                this.rootNodes.put(id, node);
            }
        });

        int oldFirstIndex = leftSubGraph.layers.size();
        this.layers.addAll(0, leftSubGraph.layers);


        // TODO: find DummyNodes between subgraphs. Just use findDummyNodes on full graph?
        leftSubGraph.genomes.forEach((parent, childMap) -> this.genomes
                .computeIfAbsent(parent, parentId -> new LinkedHashMap<>())
                .putAll(childMap));
        leftSubGraph.colorize();

        this.sortLayersLeftFrom(oldFirstIndex);
        this.setLeftDrawLocations(this.layers, oldFirstIndex);
    }


    public LinkedHashMap<Integer, DrawableNode> getNodes() {
        return this.nodes;
    }

    private GenomeGraph getGraph() {
        return graph;
    }

    /**
     * Method to translate the graph.
     *
     * @param xDifference difference in X direction.
     * @param yDifference difference in Y direction.
     */
    public void translate(double xDifference, double yDifference) {
        for (Layer layer : this.layers) {
            layer.setX(layer.getX() + xDifference);
            for (DrawableNode node : layer) {
                double oldX = node.getLocation().getX();
                double oldY = node.getLocation().getY();
                node.setLocation(oldX + xDifference, oldY + yDifference);
            }
        }
    }

    /**
     * Method to set the zoom amount.
     *
     * @param scale double of the amount to zoom.
     */
    public void zoom(double scale) {
        zoomLevel /= scale;
        for (Layer layer : this.layers) {
            layer.setX(layer.getX() / scale);
            for (DrawableNode node : layer) {
                double oldXLocation = node.getLocation().getX();
                double oldYLocation = node.getLocation().getY();
                node.setHeight(node.getHeight() / scale);
                node.setWidth(node.getWidth() / scale);
                node.setStrokeWidth(node.getStrokeWidth() / scale);
                node.setLocation(oldXLocation / scale, oldYLocation / scale);
            }
        }
    }

    /**
     * Method to give color to the nodes.
     */
    public void colorize() {
        for (DrawableNode drawableNode : this.nodes.values()) {
            drawableNode.colorize(this);
        }
    }


    public Map<DrawableNode, Map<DrawableNode, Collection<Integer>>> getGenomes() {
        return genomes;
    }

    int getNumberOfGenomes() {
        return numberOfGenomes;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }
}

