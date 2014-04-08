package de.longor1996.tagparser;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that holds instances of either the TagNode-class or the DataNode-class.
 **/
public class NodeContainer
{
	private final ArrayList<AbstractNode> nodes;
	
	/**
	 * Creates a new NodeContainer.
	 **/
	public NodeContainer()
	{
		this.nodes = new ArrayList<AbstractNode>();
	}
	
	/**
	 * Creates a new NodeContainer with the given initial capacity.
	 **/
	public NodeContainer(int initialCapacity)
	{
		this.nodes = new ArrayList<AbstractNode>(initialCapacity);
	}
	
	public final void addChild(AbstractNode node)
	{
		this.nodes.add(node);
	}
	
	public final int getChildCount()
	{
		return this.nodes.size();
	}
	
	public final AbstractNode getChild(int index)
	{
		return this.nodes.get(index);
	}
	
	public final List<AbstractNode> list()
	{
		return this.nodes;
	}
	
}
