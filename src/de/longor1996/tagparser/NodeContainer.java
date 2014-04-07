package de.longor1996.tagparser;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that holds Node's.
 **/
public class NodeContainer
{
	private ArrayList<AbstractNode> nodes;
	
	public NodeContainer()
	{
		this.nodes = new ArrayList<AbstractNode>();
	}
	
	public void add(AbstractNode node)
	{
		this.nodes.add(node);
	}
	
	public List<AbstractNode> list()
	{
		return this.nodes;
	}
	
}
