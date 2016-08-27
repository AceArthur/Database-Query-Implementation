package indexing;

import java.util.Stack;

public class BtreeIterator {
	private Stack<Btree> s;
	private Btree leaf;
	private int leafindex;
	private boolean ascending;

	public BtreeIterator(Btree root, boolean ascending) {
		leaf = null;
		s = new Stack<Btree>();
		s.push(root);
		this.ascending = ascending;
	}
 
	public boolean hasNext() {
		return (!s.isEmpty() || leaf != null);
	}

	// Iteration over the Btree uses a Stack
	public ValueAndPtr next() {
		while (hasNext()) {
			if (leaf != null) {
				ValueAndPtr result = new ValueAndPtr(leaf.getValue(leafindex),
						((Integer) leaf.getChild(leafindex)).intValue());
				if (ascending) {
					leafindex++;
					if (leafindex >= leaf.numValues()) {
						leaf = null;
					}
				} else {
					leafindex--;
					if (leafindex < 0) {
						leaf = null;
					}
				}
				return result;
			} else {
				Btree subtree = s.pop();
				if (subtree.isLeaf()) {
					if (ascending) {
						leafindex = 0;
					} else {
						leafindex = subtree.numValues() - 1;
					}
					leaf = subtree;
				} else {
					if (ascending) {
						for (int i = subtree.numValues() - 1; i >= 0; i--) {
							s.push((Btree) subtree.getChild(i));
						}
					} else {
						for (int i = 0; i < subtree.numValues(); i++) {
							s.push((Btree) subtree.getChild(i));
						}
					}
				}
			}
		}
		return null;
	}

	public class ValueAndPtr {
		public int value;
		public int ptr;

		public ValueAndPtr(int value, int ptr) {
			this.value = value;
			this.ptr = ptr;
		}
	}
}