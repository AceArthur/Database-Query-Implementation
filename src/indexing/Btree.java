package indexing;

import java.util.ArrayList;
 
public class Btree {
	static final int D = 50; // Minimum number of indexes in a non-root node.
	private boolean isLeaf; // Whether this a leaf node
	private ArrayList<Integer> values;
	private ArrayList<Object> children;
	// In leaf nodes, the children are Integers used to lookup a tuple
	// corresponding to a value.
	// In non-leaf nodes, they are btrees used to further lookup a value.

	// Public constructor creates a new empty Btree as a leaf.
	public Btree() {
		this(true);
	}

	// Private constructor can create non-leaf nodes.
	private Btree(boolean isLeaf) {
		this.isLeaf = isLeaf;
		values = new ArrayList<Integer>(2 * D);
		children = new ArrayList<Object>(2 * D);
	}

	// Adds a value and corresponding pointer to the btree. Used to create a Btree.
	// If a new root is created, returns the new root. Used during Btree creation.
	public Btree add(int value, int ptr) {
		Btree newelement = add(new Integer(value), new Integer(ptr));
		if (newelement != null) {
			Btree newroot = new Btree(false);
			newroot.add(this.values.get(0), this);
			newroot.add(newelement.values.get(0), newelement);
			return newroot;
		} else {
			return this;
		}
	}

	// Adds value and corresponding object o to a Btree. Used during Btree creation.
	private Btree add(Integer value, Object o) {
		if (isLeaf) {
			if (values.size() == 2 * D) {
				Btree newelement = new Btree();
				for (int i = 0; i < D; i++) {
					newelement.add(values.remove(D), children.remove(D));
				}
				int index = this.valuesearch(value.intValue()) + 1;
				if (index >= 0 && index < D - 1) {
					this.add(value, o);
				} else {
					newelement.add(value, o);
				}
				return newelement;
			} else {
				if (values.size() == 0) {
					values.add(value);
					children.add(o);
					return null;
				}
				int index = valuesearch(value.intValue()) + 1;
				values.add(index, value);
				children.add(index, o);
				return null;
			}
		} else {
			if (o instanceof Integer) {
				int index = valuesearch(value.intValue());
				if (index == -1) {
					index = 0;
				}
				Btree newsubtree = ((Btree) children.get(index)).add(value, o);
				if (index == 0 && values.get(index) > value) {
					values.set(index, value);
				}
				if (newsubtree != null) {
					if (values.size() == 2 * D) {
						Btree newelement = new Btree(false);
						for (int i = 0; i < D; i++) {
							newelement.add(values.remove(D), children.remove(D));
						}
						index++;
						if (index >= 0 && index <= D - 1) {
							this.add(newsubtree.values.get(0), newsubtree);
						} else {
							newelement.add(newsubtree.values.get(0), newsubtree);
						}
						return newelement;
					} else {
						values.add(index + 1, newsubtree.values.get(0).intValue());
						children.add(index + 1, newsubtree);
						return null;
					}
				} else {
					return null;
				}
			} else { // values.size() < 2 * D to reach this point
				if (values.size() == 0) {
					values.add(value);
					children.add(o);
					return null;
				}
				int index = valuesearch(value.intValue()) + 1;
				values.add(index, value);
				children.add(index, o);
				return null;
			}
		}
	}

	// Returns index that a value can be found at in a child subtree.
	// If this is a leaf, the index returned is an index where the value was
	// found.
	private int valuesearch(int value) {
		if (value < values.get(0)) {
			return -1;
		}
		for (int i = 1; i < values.size(); i++) {
			if (value < values.get(i) && value >= values.get(i - 1)) {
				return i - 1;
			}
		}
		return values.size() - 1;
	}

	// Returns an Integer. If this Btree is for the relation's ID attribute,
	// this
	// integer is used to retrieve the tuple from a tfile. For other attributes,
	// this
	// is the ID attribute.
	public Integer lookup(int value) {
		Btree subtree = this;
		while (!subtree.isLeaf) {
			subtree = (Btree) subtree.children.get(subtree.valuesearch(value));
		}
		int index = subtree.valuesearch(value);
		if (index < 0) {
			return null;
		} else {
			return (Integer) subtree.children.get(index);
		}
	}
 
	public BtreeIterator iterator(boolean ascending) {
		return new BtreeIterator(this, ascending);
	}

	public Object getChild(int index) {
		return children.get(index);
	}

	public int getValue(int index) {
		return values.get(index);
	}

	public int numValues() {
		return values.size();
	}

	public boolean isLeaf() {
		return isLeaf;
	}
}
