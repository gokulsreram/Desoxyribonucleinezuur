package programminglife.model.drawing;

import javafx.scene.paint.Color;
import programminglife.model.XYCoordinate;

import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Created by toinehartman on 19/06/2017.
 */
public class DrawableSNP extends DrawableNode {
    private static final int SNP_WIDTH = 11;

    private DrawableNode parent;
    private DrawableNode child;
    private final Collection<DrawableSegment> mutations;

    /**
     * Construct a {@link DrawableNode}.
     * @param parent the parent Segment of the mutations/SNP
     * @param child the child Segment of the mutations/SNP
     * @param mutations the Segments in this bubble
     */
    DrawableSNP(DrawableNode parent, DrawableNode child, Collection<DrawableSegment> mutations) {
        super(mutations.iterator().next().getGraph(), -mutations.hashCode());

        this.parent = parent;
        this.child = child;
        this.mutations = mutations;

        this.parent.getChildren().add(this.getIdentifier());
        this.child.getParents().add(this.getIdentifier());

        this.setDrawDimensions();
    }

    /**
     * Get the IDs of children of this.
     *
     * @return IDs of drawable children
     */
    @Override
    public Collection<Integer> getChildren() {
        return Collections.singleton(this.child.getIdentifier());
    }

    /**
     * Get the IDs of parents of this.
     *
     * @return IDs of drawable parents.
     */
    @Override
    public Collection<Integer> getParents() {
        return Collections.singleton(this.parent.getIdentifier());
    }

    /**
     * Replace a parent with another one.
     *
     * @param oldParent the parent to replace
     * @param newParent the new parent
     */
    @Override
    void replaceParent(DrawableNode oldParent, DrawableNode newParent) {
        if (this.parent.getIdentifier() == oldParent.getIdentifier()) {
            this.parent = newParent;
        } else {
            throw new NoSuchElementException(
                    String.format("The node to be replaced (%d) is not the parent of this SNP (%d).",
                            oldParent.getIdentifier(), this.getIdentifier()));
        }
    }

    /**
     * Replace child with another one.
     *
     * @param oldChild the child to replace
     * @param newChild the new child
     */
    @Override
    void replaceChild(DrawableNode oldChild, DrawableNode newChild) {
        if (this.child.getIdentifier() == oldChild.getIdentifier()) {
            this.child = newChild;
        } else {
            throw new NoSuchElementException(
                    String.format("The node to be replaced (%d) is not the parent of this SNP (%d).",
                            oldChild.getIdentifier(), this.getIdentifier()));
        }
    }

    /**
     * Information {@link String} about this.
     *
     * @return info
     */
    @Override
    public String details() {
        return this.mutations.toString();
    }

    @Override
    public DrawableSNP createSNPIfPossible(SubGraph subGraph) {
        return null;
    }

    /**
     * Color this according to contents.
     *
     * @param sg the {@link SubGraph} this {@link DrawableNode} is in
     */
    @Override
    public void colorize(SubGraph sg) {
        this.setColors(Color.BLANCHEDALMOND, Color.DARKRED);
        this.setStrokeWidth(3.5);
    }

    /**
     * Set the size of this drawing.
     */
    @Override
    protected void setDrawDimensions() {
        this.setSize(new XYCoordinate(SNP_WIDTH, NODE_HEIGHT));
        this.setDrawDimensionsUpToDate();
    }

    /**
     * Set the size of this {@link Drawable}.
     * @param size the preferred size
     */
    private void setSize(XYCoordinate size) {
        this.setWidth(size.getX());
        this.setHeight(size.getY());
        this.setDrawDimensionsUpToDate();
    }

    @Override
    public DrawableNode getParentSegment() {
        return this;
    }

    @Override
    public DrawableNode getChildSegment() {
        return this;
    }

    @Override
    public Collection<Integer> getGenomes() {
        return this.mutations.stream()
                            .map(DrawableSegment::getGenomes)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
    }

    public Collection<DrawableSegment> getMutations() {
        return this.mutations;
    }

    public DrawableNode getChild() {
        return child;
    }
}