package org.shogun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Tree Node of the Dependency Tree. We parse the output of lddtree into a Tree
 * so that we can visit the dependencies in post order
 * 
 * @author pschatzmann
 *
 */
public class DependencyTreeNode {
	private String libraryName;
	private String libraryWithPath;
	private List<DependencyTreeNode> children = new ArrayList();
	private boolean loadLibSupported = false;

	public DependencyTreeNode(String name, String path) {
		this.libraryName = name;
		this.libraryWithPath = path;
	}
	

	/**
	 * Returns the children in the defined order
	 * 
	 * @return
	 */
	public List<DependencyTreeNode> getChildren() {
		return children;
	}

	/**
	 * Returns the children in reverse order. lddtree provides the independent libraries at the end so we want
	 * to laod them first
	 * 
	 * @return
	 */
	public List<DependencyTreeNode> getChildrenReverse() {
		List result = new ArrayList(getChildren());
		Collections.reverse(result);
		return result;
	}

	public String getLibraryName() {
		return this.libraryName;
	}

	public String getLibraryWithPath() {
		return this.libraryWithPath;
	}
	
	public void setLibraryWithPath(String path) {
		this.libraryWithPath = path;
	}

	public void addChild(DependencyTreeNode node) {
		this.children.add(node);

	}

	public List<DependencyTreeNode> getAllTreeNodesPostOrder() {
		List<DependencyTreeNode> result = new ArrayList();
		postOrder(this, result);
		return result;
	}

	public List<DependencyTreeNode> getAllTreeNodes() {
		List<DependencyTreeNode> result = new ArrayList();
		preOrder(this, result);
		return result;
	}

	void postOrder(DependencyTreeNode node, List<DependencyTreeNode> result) {
		for (DependencyTreeNode child : node.getChildrenReverse()) {
			postOrder(child, result);
		}
		if (!node.getLibraryName().isEmpty()) {
			result.add(node);
		}
	}

	void preOrder(DependencyTreeNode node, List<DependencyTreeNode> result) {
		if (!node.getLibraryName().isEmpty()) {
			result.add(node);
		}
		for (DependencyTreeNode child : node.getChildrenReverse()) {
			preOrder(child, result);
		}
	}

	@Override
	public String toString() {
		return this.libraryName;
	}
	
	public boolean isLoadLoadLibSupported() {
		return loadLibSupported;
	}
	
	public void setLoadLoadLibSupported(boolean flag) {
		loadLibSupported = flag;
	}
	

}
