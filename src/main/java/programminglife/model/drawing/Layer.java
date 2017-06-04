package programminglife.model.drawing;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A layer of {@link DrawableNode DrawableNodes}. Multiple Layers are used to lay out the graph.
 *
 * @see SubGraph#layout()
 * @see SubGraph#findLayers()
 */
public class Layer implements Iterable<DrawableNode> {
    private double width;
    private List<DrawableNode> nodes;

    /**
     * Default empty constructor.
     */
    public Layer() {
        this.width = 0;
        this.nodes = new ArrayList<>();
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    /**
     * Add a {@link DrawableNode} to this Layer.
     * @param node the node to add.
     */
    public void add(DrawableNode node) {
        if (node.getWidth() > width) {
            this.width = node.getWidth();
        }
        this.nodes.add(node);
    }

    /**
     * Get an iterator over the {@link DrawableNode DrawableNodes} in this Layer.
     * @return an iterator over the {@link DrawableNode DrawableNodes} in this Layer.
     */
    @NotNull
    @Override
    public Iterator<DrawableNode> iterator() {
        return nodes.iterator();
    }

    /**
     * sort the {@link DrawableNode DrawableNodes} in this layer according to the order of the {@link Comparator} c.
     * @param c A {@link Comparator} which imposes a total ordering on the nodes.
     */
    public void sort(Comparator<? super DrawableNode> c) {
        this.nodes.sort(c);
    }

    /**
     * Checks whether a node is in this layer.
     * @param node {@link DrawableNode} to check for its presence.
     * @return {@link boolean} true if it is in the layer, false otherwise.
     */
    public boolean contains(DrawableNode node) {
        return nodes.contains(node);
    }

    /**
     * Get the size of the layer.
     * @return {@link int} the size of the layer.
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * Get the index of the a {@link DrawableNode} node in the layer.
     * @param node {@link DrawableNode} to get the index of.
     * @return the index of the node.
     */
    public int indexOf(DrawableNode node) {
        return nodes.indexOf(node);
    }
}